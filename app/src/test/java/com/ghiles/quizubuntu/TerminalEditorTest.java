package com.ghiles.quizubuntu;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest=Config.NONE)
public class TerminalEditorTest {
    @Test public void softwareKeyboardEnterSubmitsExactlyOnce() {
        TerminalEditor editor=new TerminalEditor(RuntimeEnvironment.getApplication());
        editor.render("","$ ");editor.setCommand("pwd");
        int[] count={0};editor.setOnSubmit(() -> count[0]++);
        android.view.inputmethod.InputConnection connection=editor.onCreateInputConnection(new android.view.inputmethod.EditorInfo());
        assertNotNull(connection);connection.commitText("\n",1);
        assertEquals(1,count[0]);assertEquals("pwd",editor.command());
    }
    @Test public void transcriptIsProtectedAndCommandRemainsEditable() {
        TerminalEditor editor=new TerminalEditor(RuntimeEnvironment.getApplication());
        editor.render("résultat\n","ubuntu:~$ ");
        editor.setCommand("git status");
        editor.getText().delete(0,4);
        assertTrue(editor.getText().toString().startsWith("résultat\n"));
        assertEquals("git status",editor.command());
        editor.setSelection(0);
        assertEquals("résultat\nubuntu:~$ ".length(),editor.getSelectionStart());
        editor.setCommand("pwd");assertEquals("pwd",editor.command());
    }
    @Test public void newOutputAndPromptPreserveDraftAndPasteDoesNotExecute() {
        TerminalEditor editor=new TerminalEditor(RuntimeEnvironment.getApplication());
        editor.render("","$ ");editor.setCommand("echo brouillon");
        editor.render("nouvelle sortie\n","ubuntu:~/repo$ ");
        assertEquals("echo brouillon",editor.command());
        editor.setCommand("git status\ngit push");
        assertEquals("git status git push",editor.command());
        editor.setCommand("");assertEquals("",editor.command());
        assertTrue(editor.getText().toString().contains("nouvelle sortie"));
    }
}
