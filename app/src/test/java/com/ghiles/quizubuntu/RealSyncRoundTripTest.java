package com.ghiles.quizubuntu;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class RealSyncRoundTripTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private void identity(Git git) throws Exception {
        StoredConfig config=git.getRepository().getConfig();
        config.setString("user",null,"name","Phone Test");
        config.setString("user",null,"email","phone@example.com");config.save();
    }
    private void commit(Git git,String message) throws Exception {
        git.add().addFilepattern(".").call();git.add().addFilepattern(".").setUpdate(true).call();
        git.commit().setMessage(message).call();
    }
    @Test public void realFilesRoundTripThroughBareRemoteAndOtherComputer() throws Exception {
        File remote=temp.newFolder("remote.git");
        try(Git bare=Git.init().setBare(true).setInitialBranch("main").setDirectory(remote).call()) {
            RealWorkspace phone=new RealWorkspace(temp.newFolder("phone"));
            phone.execute("mkdir 'mon projet'");phone.execute("cd 'mon projet'");
            try(Git local=Git.init().setInitialBranch("main").setDirectory(phone.currentDirectory()).call()) {
                identity(local);
                phone.execute("mkdir documents");phone.execute("touch 'documents/été notes.txt'");
                phone.execute("echo 'Bonjour depuis le téléphone 🙂' > 'documents/été notes.txt'");
                phone.execute("echo 'Deuxième ligne' >> 'documents/été notes.txt'");
                String original=phone.read("documents/été notes.txt");
                commit(local,"Depuis téléphone");
                String confirmation=RealSyncResults.requireAccepted(local.push().setRemote(remote.toURI().toString()).add("main").call());
                assertTrue(confirmation.contains("envoyé"));
                File computerDir=new File(temp.getRoot(),"computer");
                try(Git computer=Git.cloneRepository().setURI(remote.toURI().toString()).setDirectory(computerDir).call()) {
                    identity(computer);
                    File actual=new File(computerDir,"documents/été notes.txt");
                    assertEquals(original,new String(Files.readAllBytes(actual.toPath()),StandardCharsets.UTF_8));
                    Files.write(actual.toPath(),"Modification depuis ordinateur\n".getBytes(StandardCharsets.UTF_8));
                    commit(computer,"Depuis ordinateur");
                    RealSyncResults.requireAccepted(computer.push().setRemote("origin").add("main").call());
                    ObjectId before=local.getRepository().resolve("HEAD");
                    assertTrue(local.pull().setRemote(remote.toURI().toString()).setRemoteBranchName("main").call().isSuccessful());
                    assertEquals("Modification depuis ordinateur\n",phone.execute("cat 'documents/été notes.txt'"));
                    assertTrue(RealSyncResults.changedFiles(local.getRepository(),before,local.getRepository().resolve("HEAD")).contains("documents/été notes.txt"));
                    phone.write("documents/été notes.txt","Sauvegarde éditeur réel\n",false);
                    // A push cannot send an uncommitted edit.
                    assertTrue(RealSyncResults.requireAccepted(local.push().setRemote(remote.toURI().toString()).add("main").call()).contains("déjà à jour"));
                    assertEquals("Modification depuis ordinateur\n",new String(Files.readAllBytes(actual.toPath()),StandardCharsets.UTF_8));
                    commit(local,"Éditeur téléphone");
                    RealSyncResults.requireAccepted(local.push().setRemote(remote.toURI().toString()).add("main").call());
                    assertTrue(computer.pull().setRemote("origin").setRemoteBranchName("main").call().isSuccessful());
                    assertEquals("Sauvegarde éditeur réel\n",new String(Files.readAllBytes(actual.toPath()),StandardCharsets.UTF_8));
                    // Divergence must not be reported as a successful push.
                    Files.write(actual.toPath(),"Autre commit distant\n".getBytes(StandardCharsets.UTF_8));
                    commit(computer,"Distant avance");RealSyncResults.requireAccepted(computer.push().setRemote("origin").add("main").call());
                    phone.write("documents/été notes.txt","Local divergent\n",false);commit(local,"Local avance");
                    try {
                        RealSyncResults.requireAccepted(local.push().setRemote(remote.toURI().toString()).add("main").call());
                        fail("A non-fast-forward push must be rejected");
                    } catch(IllegalStateException expected) { assertTrue(expected.getMessage().contains("REFUSÉ")); }
                    assertFalse(local.pull().setRemote(remote.toURI().toString()).setRemoteBranchName("main").call().isSuccessful());
                    assertFalse(local.status().call().getConflicting().isEmpty());
                }
            }
        }
    }
    @Test public void workspacePathsAreConfinedAndEditorMatchesTerminal() throws Exception {
        RealWorkspace workspace=new RealWorkspace(temp.newFolder("files"));
        workspace.execute("mkdir -p 'dossier espace/sub'");workspace.execute("cd 'dossier espace/sub'");
        String editor=workspace.resolve("notes.txt",true).getPath();
        workspace.write(editor,"Première ligne\n",false);
        workspace.execute("echo suite >> notes.txt");assertEquals("Première ligne\nsuite\n",workspace.read(editor));
        assertEquals(workspace.currentDirectory(),workspace.resolve(workspace.displayPath(),false));
        for(String path:new String[]{"../../../../outside","../../.git/config"}) {
            try {workspace.write(path,"forbidden",false);fail(path);}catch(java.io.IOException expected) { }
        }
        File outside=temp.newFolder("outside");
        Files.createSymbolicLink(new File(workspace.currentDirectory(),"escape").toPath(),outside.toPath());
        try {workspace.write("escape/file","forbidden",false);fail("symlink escape");}catch(java.io.IOException expected) { }
    }
    @Test public void missingServerConfirmationNeverMeansSuccess() {
        try {RealSyncResults.requireAccepted(java.util.Collections.emptyList());fail("empty result");}
        catch(IllegalStateException expected) {assertTrue(expected.getMessage().contains("non confirmé"));}
    }
}
