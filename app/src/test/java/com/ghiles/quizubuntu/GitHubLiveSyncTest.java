package com.ghiles.quizubuntu;

import android.content.Context;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** Real HTTPS round trip through GitHub; only enabled in the scoped CI workflow. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class GitHubLiveSyncTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Test public void phoneCommandsAndComputerExchangeRealFiles() throws Exception {
        String token = System.getenv("GH_SYNC_TEST_TOKEN");
        String run = System.getenv("GH_SYNC_TEST_RUN");
        assumeTrue(token != null && !token.isEmpty() && run != null && run.matches("[0-9]+"));
        String branch = "live-sync-" + run;
        String remote = "https://github.com/ghilesaimeur951-creator/new-git.git";
        Context context = RuntimeEnvironment.getApplication();
        RealGitClient phone = new RealGitClient(context, new SecureTokenStore(context),
            temp.newFolder("phone"), "live-" + run, () -> token);
        UsernamePasswordCredentialsProvider auth = new UsernamePasswordCredentialsProvider("x-access-token", token);
        boolean published = false;
        try {
            phone.execute("mkdir project");
            phone.execute("cd project");
            phone.execute("git init");
            phone.execute("git config --global user.name \"Phone Test\"");
            phone.execute("git config --global user.email phone@example.com");
            assertTrue(phone.execute("ls -la").contains(".git/"));
            phone.execute("touch phone.txt");
            phone.execute("echo 'Depuis Android' > phone.txt");
            phone.execute("git add phone.txt");
            phone.execute("git commit -m \"Téléphone initial\"");
            phone.execute("git branch -M " + branch);
            phone.execute("git remote add origin " + remote);
            assertTrue(phone.execute("git remote -v").contains(remote));
            String first = phone.execute("git push -u origin " + branch);
            assertTrue(first, first.contains("envoyé"));
            published = true;

            File computerDir = new File(temp.getRoot(), "computer");
            try (Git computer = Git.cloneRepository().setURI(remote).setBranch(branch)
                .setDirectory(computerDir).setCredentialsProvider(auth).call()) {
                assertEquals("Depuis Android\n", Files.readString(new File(computerDir,"phone.txt").toPath()));
                File computerFile = new File(computerDir,"computer.txt");
                Files.write(computerFile.toPath(), "Depuis GitHub\n".getBytes(StandardCharsets.UTF_8));
                computer.getRepository().getConfig().setString("user",null,"name","Computer Test");
                computer.getRepository().getConfig().setString("user",null,"email","computer@example.com");
                computer.add().addFilepattern("computer.txt").call();
                computer.commit().setMessage("Ordinateur distant").call();
                RealSyncResults.requireAccepted(computer.push().setRemote("origin")
                    .setCredentialsProvider(auth).add(branch).call());

                String pulled = phone.execute("git pull origin " + branch);
                assertTrue(pulled, pulled.contains("computer.txt"));
                assertEquals("Depuis GitHub\n", phone.execute("cat computer.txt"));
                phone.execute("echo 'Retour depuis Android' >> phone.txt");
                assertTrue(phone.execute("git status").contains("phone.txt"));
                phone.execute("git add phone.txt");
                phone.execute("git commit -m Retour");
                String second = phone.execute("git push");
                assertTrue(second, second.contains("envoyé"));
                assertTrue(computer.pull().setRemote("origin").setRemoteBranchName(branch)
                    .setCredentialsProvider(auth).call().isSuccessful());
                assertEquals("Depuis Android\nRetour depuis Android\n",
                    Files.readString(new File(computerDir,"phone.txt").toPath()));
            }
        } finally {
            if (published) try (Git local = Git.open(phone.workTree())) {
                RealSyncResults.requireAccepted(local.push().setRemote("origin")
                    .setCredentialsProvider(auth).setRefSpecs(new RefSpec(":refs/heads/"+branch)).call());
            }
        }
    }
}
