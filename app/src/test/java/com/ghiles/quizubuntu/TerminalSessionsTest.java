package com.ghiles.quizubuntu;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;
public class TerminalSessionsTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    @Test public void restartPreservesFilesAndFullSnapshotAndIsolatesSessions() throws Exception {
        File root = temp.newFolder();
        TerminalSessions sessions = new TerminalSessions(root);
        RealWorkspace one = new RealWorkspace(sessions.workspace("1"));
        one.execute("mkdir projet"); one.execute("cd projet"); one.execute("echo 'été téléphone' > note.txt");
        Properties p = sessions.load("1"); p.setProperty("transcript", "git init\nréussi\n"); p.setProperty("draft", "git sta"); p.setProperty("scenario", "12"); sessions.save("1", p);
        String second = sessions.create("Mes branches");
        RealWorkspace two = new RealWorkspace(sessions.workspace(second)); two.execute("touch autre.txt");
        TerminalSessions reopened = new TerminalSessions(root);
        assertEquals("été téléphone\n", new RealWorkspace(reopened.workspace("1")).read("projet/note.txt"));
        assertEquals(p, reopened.load("1")); assertEquals("Mes branches", reopened.load(second).getProperty("name"));
        assertFalse(new File(reopened.workspace(second), "projet/note.txt").exists());
        assertFalse(new File(reopened.workspace("1"), "autre.txt").exists());
        assertEquals(new File(root, "github-real"), reopened.workspace("1"));
    }
    @Test public void deletionOnlyRemovesTheChosenSession() throws Exception {
        TerminalSessions sessions = new TerminalSessions(temp.newFolder());
        String id = sessions.create("jetable");
        new RealWorkspace(sessions.workspace(id)).execute("touch test.txt");
        new RealWorkspace(sessions.workspace("1")).execute("touch conservé.txt");
        sessions.delete(id);
        assertFalse(sessions.workspace(id).exists());
        assertTrue(new File(sessions.workspace("1"), "conservé.txt").exists());
        assertEquals(Arrays.asList("1"), sessions.list());
    }
    @Test public void invalidOrMissingSessionCannotEscapeStorage() throws Exception {
        TerminalSessions sessions = new TerminalSessions(temp.newFolder());
        for(String id : new String[]{"../outside", "0", "-1", "/tmp/a"}) try { sessions.workspace(id); fail(id); } catch(IllegalArgumentException expected) { }
        try { sessions.load("999"); fail(); } catch(FileNotFoundException expected) { }
        assertEquals(Arrays.asList("1"), sessions.list());
    }
}
