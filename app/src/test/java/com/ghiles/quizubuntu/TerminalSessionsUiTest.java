package com.ghiles.quizubuntu;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import java.lang.reflect.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest=Config.NONE)
public class TerminalSessionsUiTest {
    private Object field(Object obj,String name) throws Exception {Field f=obj.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(obj);}
    private void call(Object obj,String name,String arg) throws Exception {Method m=obj.getClass().getDeclaredMethod(name,String.class);m.setAccessible(true);m.invoke(obj,arg);}
    @Test public void freeTerminalUsesDiskAndReopensSessionAfterActivityDestruction() throws Exception {
        ActivityController<SimulatorActivity> controller=Robolectric.buildActivity(SimulatorActivity.class).setup();
        SimulatorActivity activity=controller.get();
        assertEquals(true,field(activity,"realEnvironment"));
        RealGitClient client=(RealGitClient)field(activity,"realGit");
        client.execute("mkdir travail");client.execute("cd travail");client.execute("git init");
        assertTrue(client.execute("ls -la").contains(".git"));
        client.execute("echo 'contenu réel' > note.txt");
        ((TerminalEditor)field(activity,"commandInput")).setCommand("git status");
        controller.pause().stop().destroy();
        ActivityController<SimulatorActivity> next=Robolectric.buildActivity(SimulatorActivity.class).setup();
        assertEquals("contenu réel\n",((RealGitClient)field(next.get(),"realGit")).execute("cat note.txt"));
        assertEquals("git status",((TerminalEditor)field(next.get(),"commandInput")).command());
        call(next.get(),"runCommand","session new essais");
        assertEquals("2",field(next.get(),"sessionId"));
        assertFalse(((RealGitClient)field(next.get(),"realGit")).execute("ls").contains("note.txt"));
        call(next.get(),"runCommand","session open 1");
        assertEquals("contenu réel\n",((RealGitClient)field(next.get(),"realGit")).execute("cat note.txt"));
        next.pause().stop().destroy();
    }
    @Test public void guidedFilesSurviveTabChangesAndRestart() throws Exception {
        ActivityController<SimulatorActivity> controller=Robolectric.buildActivity(SimulatorActivity.class).setup();
        SimulatorActivity activity=controller.get();
        Method mode=SimulatorActivity.class.getDeclaredMethod("setGuidedMode",boolean.class);mode.setAccessible(true);
        mode.invoke(activity,true);
        call(activity,"runCommand","echo sauvegarde > exemple-session.txt");
        mode.invoke(activity,false);mode.invoke(activity,true);
        assertTrue(((VirtualMachine)field(activity,"vm")).execute("cat exemple-session.txt").text.contains("sauvegarde"));
        controller.pause().stop().destroy();
        ActivityController<SimulatorActivity> next=Robolectric.buildActivity(SimulatorActivity.class).setup();
        assertEquals(true,field(next.get(),"guidedMode"));
        assertTrue(((VirtualMachine)field(next.get(),"vm")).execute("cat exemple-session.txt").text.contains("sauvegarde"));
        next.pause().stop().destroy();
    }
    @Test public void tokenDraftNeverEntersSessionSnapshot() throws Exception {
        ActivityController<SimulatorActivity> controller=Robolectric.buildActivity(SimulatorActivity.class).setup();
        SimulatorActivity activity=controller.get();
        call(activity,"runCommand","gh auth login");
        TerminalEditor editor=(TerminalEditor)field(activity,"commandInput");
        editor.setCommand("secret-example-value");
        assertFalse(editor.getTransformationMethod().getTransformation(editor.getText(),editor).toString().contains("secret-example-value"));
        controller.pause();
        TerminalSessions sessions=(TerminalSessions)field(activity,"sessions");
        assertFalse(sessions.load("1").toString().contains("secret-example-value"));
        controller.stop().destroy();
    }
}
