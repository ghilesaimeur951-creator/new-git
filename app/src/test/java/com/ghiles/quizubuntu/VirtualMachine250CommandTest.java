package com.ghiles.quizubuntu;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Regression suite for the 250 most likely Ubuntu/Git commands in Ubuntu Lab.
 *
 * The course workflow commands are checked semantically; the larger 250-command
 * audit rejects "unknown"/"unsupported"/documentary-only fallbacks so the common
 * commands must have an actual simulation handler.
 */
public class VirtualMachine250CommandTest {

    private static final List<String> TOP_250 = Arrays.asList(
        // 1-30 — navigation and files
        "pwd","ls","ls -l","ls -la","ls -al","cd ~","cd ..","cd -","mkdir test","mkdir -p a/b/c",
        "touch file.txt","echo hello","echo hello > file.txt","echo again >> file.txt","cat README.md","nano README.md",
        "cp README.md copy.md","cp -r src src-copy","mv README.md README2.md","rm temp.txt","rm -r src-copy","rmdir empty",
        "clear","history","whoami","hostname","uname","uname -a","id","date",

        // 31-80 — shell, text, search and metadata
        "alias","alias gs='git status'","unalias ll","env","printenv","printenv HOME","export TEST=1","echo $HOME",
        "printf \"hello\\n\"","head README.md","head -n 1 README.md","tail README.md","tail -n 1 README.md","wc README.md",
        "wc -l README.md","wc -w README.md","wc -c README.md","grep Base README.md","grep -i base README.md","grep -n Base README.md",
        "grep -v nope README.md","find .","find . -type f","find . -type d","find . -name \"*.md\"","which git","command -v git",
        "whereis git","type git","basename /home/ubuntu/work/README.md","dirname /home/ubuntu/work/README.md","realpath .",
        "file README.md","stat README.md","du","du -h","du -sh .","df","df -h","lsblk","lsblk -f",
        "chmod 644 README.md","chmod u+x README.md","chown ubuntu README.md","chgrp ubuntu README.md","umask","ln -s README.md link.md",
        "diff README.md notes.txt","sha256sum README.md","groups","users",

        // 81-125 — system, process, network and packages
        "who","w","hostname -f","hostnamectl","uname -r","uptime","free","free -h","cal","lscpu","lsmem","ps","ps aux",
        "ps -ef","pgrep bash","pidof bash","top","kill 1234","kill -15 1234","pkill bash","nice -n 10 echo","renice 5 -p 1234",
        "ip addr","ip -br addr","ip link","ip route","ss","ss -tulpn","ping -c 4 github.com","curl https://example.com",
        "curl -I https://example.com","wget https://example.com/file","getent hosts github.com","resolvectl status","apt --version",
        "apt list","apt search git","apt show git","apt update","apt install git","apt remove git","dpkg -l","dpkg -s git",
        "systemctl status ssh","journalctl -u ssh",

        // 126-145 — archives, jobs and checksums
        "tar -cf archive.tar src","tar -tf archive.tar","tar -xf archive.tar","tar -czf archive.tar.gz src","tar -xzf archive.tar.gz",
        "gzip notes.txt","gunzip notes.txt.gz","zip archive.zip README.md","unzip archive.zip","jobs","jobs -l","bg","fg","sleep 1",
        "true","false","md5sum README.md","sha1sum README.md","sha512sum README.md","cksum README.md",

        // 146-180 — Git basics, index, log and branches
        "git --version","git init","git status","git status -s","git status --short","git status -sb","git status --porcelain",
        "git add README.md","git add .","git commit -m \"test\"","git log","git log -p","git log --oneline",
        "git log --stat","git log --decorate","git log --graph --oneline --decorate --all","git diff","git diff --staged",
        "git diff --cached","git branch","git branch -a","git branch feature2","git branch feature2 HEAD","git branch -d feature",
        "git branch -D feature","git branch -m renamed","git branch -M main","git switch main","git switch feature",
        "git switch -c feature2","git switch -C feature","git switch -","git checkout main","git checkout feature",

        // 181-210 — checkout, restore, reset and stash
        "git checkout -b feature2","git checkout -B feature","git checkout -","git checkout -- README.md","git checkout HEAD -- README.md",
        "git checkout --detach HEAD","git restore README.md","git restore --staged README.md","git restore --source=HEAD README.md",
        "git reset README.md","git reset HEAD","git reset --hard HEAD","git reset --soft HEAD~1","git reset --mixed HEAD~1",
        "git stash","git stash push","git stash push -m \"wip\"","git stash list","git stash apply","git stash pop","git stash drop",
        "git stash clear","git reflog","git reflog -10","git rev-parse HEAD","git rev-parse --abbrev-ref HEAD",
        "git rev-parse --show-toplevel","git rm --cached README.md","git mv README.md README2.md","git clean -n",

        // 211-235 — remotes and synchronization
        "git clean -nd","git clean -fd","git remote","git remote -v","git remote show origin","git remote get-url origin",
        "git remote add origin https://github.com/example/other.git","git remote set-url origin https://github.com/example/other.git",
        "git remote rename origin upstream","git remote remove origin","git fetch","git fetch origin","git fetch --all","git fetch --prune",
        "git fetch origin main","git pull","git pull origin main","git pull --rebase origin main","git pull --ff-only origin main",
        "git push","git push origin main","git push -u origin main","git push --set-upstream origin main","git push origin feature",
        "git ls-remote origin",

        // 236-250 — merge, inspection and SSH
        "git ls-remote --heads origin","git merge feature","git merge --no-ff feature","git merge --abort",
        "git cherry-pick HEAD","git revert HEAD","git show --oneline --stat HEAD","ls -al ~/.ssh",
        "ssh-keygen -t ed25519 -C \"test@example.com\"","eval \"$(ssh-agent -s)\"","ssh-add ~/.ssh/id_ed25519",
        "cat ~/.ssh/id_ed25519.pub","ssh-keygen -lf ~/.ssh/id_ed25519.pub","ssh -T git@github.com","help checkout"
    );

    @Test
    public void auditExactly250LikelyCommands() {
        assertEquals("The regression audit must stay at 250 commands", 250, TOP_250.size());

        for (String command : TOP_250) {
            VirtualMachine vm = preparedVm();
            VirtualMachine.Result result;

            try {
                result = vm.execute(command);
            } catch (Throwable error) {
                fail("Command crashed: " + command + " -> " + error);
                return;
            }

            assertNotNull("Null result for " + command, result);

            String text = result.text == null ? "" : result.text.toLowerCase();

            assertFalse(
                "Unknown command fallback for: " + command + " -> " + result.text,
                text.contains("commande introuvable")
            );
            assertFalse(
                "Unsupported command fallback for: " + command + " -> " + result.text,
                text.contains("non prise en charge")
            );
            if (text.contains("simulation documentaire")) {
                System.err.println("AUDIT_DOCUMENTARY: " + command + " -> " + result.text);
            }

            assertFalse(
                "A top-250 command must have behavior, not documentary fallback: " + command + " -> " + result.text,
                text.contains("simulation documentaire")
            );
        }
    }

    @Test
    public void courseBashWorkflowIsStateful() {
        VirtualMachine vm = new VirtualMachine();

        assertEquals("/home/ubuntu", vm.execute("pwd").text);

        vm.execute("mkdir projet");
        vm.execute("cd projet");
        assertEquals("~/projet", vm.shortCwd());

        vm.execute("touch README.md");
        vm.execute("echo \"Bonjour\" > README.md");
        assertEquals("Bonjour", vm.execute("cat README.md").text);

        vm.execute("echo \"Suite\" >> README.md");
        assertTrue(vm.execute("cat README.md").text.contains("Suite"));

        vm.execute("cp README.md copie.md");
        assertEquals("Bonjour\nSuite", vm.execute("cat copie.md").text);

        vm.execute("mv copie.md archive.md");
        assertEquals("Bonjour\nSuite", vm.execute("cat archive.md").text);

        vm.execute("rm archive.md");
        assertTrue(vm.execute("cat archive.md").text.contains("Aucun fichier"));
    }

    @Test
    public void courseGitBranchCheckoutWorkflowIsStateful() {
        VirtualMachine vm = new VirtualMachine();

        vm.execute("mkdir projet");
        vm.execute("cd projet");
        vm.execute("git init");
        vm.execute("touch README.md");
        vm.execute("echo base > README.md");
        vm.execute("git add README.md");
        VirtualMachine.Result commit = vm.execute("git commit -m \"base\"");

        assertTrue(commit.text.contains("base"));
        assertTrue(vm.execute("git status").text.contains("working tree clean"));

        assertTrue(vm.execute("git checkout -b cheese").text.contains("cheese"));
        assertTrue(vm.execute("git branch").text.contains("* cheese"));

        vm.execute("echo cheese >> README.md");
        vm.execute("git add README.md");
        vm.execute("git commit -m \"cheese\"");

        assertTrue(vm.execute("git checkout main").text.contains("main"));
        assertTrue(vm.execute("git branch").text.contains("* main"));

        VirtualMachine.Result merge = vm.execute("git merge cheese");
        assertFalse(merge.text.toLowerCase().contains("not something we can merge"));
    }

    @Test
    public void courseRemoteAndPushWorkflowIsCoherent() {
        VirtualMachine vm = new VirtualMachine();

        vm.execute("mkdir repo");
        vm.execute("cd repo");
        vm.execute("git init");
        vm.execute("touch README.md");
        vm.execute("git add README.md");
        vm.execute("git commit -m \"Initial commit\"");

        vm.execute("git remote add origin https://github.com/USER/REPO.git");
        assertTrue(vm.execute("git remote -v").text.contains("origin"));
        assertTrue(vm.execute("git remote get-url origin").text.contains("github.com"));

        VirtualMachine.Result push = vm.execute("git push -u origin main");
        assertTrue(push.text.contains("main"));

        VirtualMachine.Result refs = vm.execute("git ls-remote --heads origin");
        assertTrue(refs.text.contains("refs/heads/main"));

        VirtualMachine.Result pull = vm.execute("git pull --rebase origin main");
        assertFalse(pull.text.toLowerCase().contains("fatal"));
    }

    @Test
    public void courseConflictWorkflowShowsAndResolvesMarkers() {
        VirtualMachine vm = new VirtualMachine();
        vm.prepareScenario("conflict-pull");

        VirtualMachine.Result pull = vm.execute("git pull origin main");
        assertEquals(VirtualMachine.Kind.ERROR, pull.kind);
        assertTrue(pull.text.contains("CONFLICT"));

        VirtualMachine.Result status = vm.execute("git status");
        assertTrue(status.text.contains("unmerged"));

        String conflicted = vm.execute("cat README.md").text;
        assertTrue(conflicted.contains("<<<<<<< HEAD"));
        assertTrue(conflicted.contains("======="));
        assertTrue(conflicted.contains(">>>>>>> origin/main"));

        vm.saveEditedFile(vm.cwd() + "/README.md", "LOCAL\n");
        vm.execute("git add README.md");

        VirtualMachine.Result staged = vm.execute("git diff --staged");
        assertFalse(staged.text.contains("<<<<<<<"));

        VirtualMachine.Result mergeCommit = vm.execute("git commit -m \"Résolution du conflit\"");
        assertTrue(mergeCommit.text.contains("Résolution du conflit"));

        assertTrue(
            vm.execute("git log --graph --oneline --decorate --all").text
                .contains("Résolution du conflit")
        );
    }

    @Test
    public void sshCourseWorkflowIsCoherent() {
        VirtualMachine vm = new VirtualMachine();

        assertTrue(vm.execute("ls -al ~/.ssh").entries.isEmpty());

        assertEquals(
            VirtualMachine.Kind.SUCCESS,
            vm.execute("ssh-keygen -t ed25519 -C \"student@example.com\"").kind
        );

        String publicKey = vm.execute("cat ~/.ssh/id_ed25519.pub").text;
        assertTrue(publicKey.startsWith("ssh-ed25519 "));
        assertFalse(publicKey.contains("PRIVATE"));

        assertTrue(vm.execute("ssh-keygen -lf ~/.ssh/id_ed25519.pub").text.contains("SHA256:"));
        assertTrue(vm.execute("eval \"$(ssh-agent -s)\"").text.contains("Agent pid"));
        assertEquals(
            VirtualMachine.Kind.SUCCESS,
            vm.execute("ssh-add ~/.ssh/id_ed25519").kind
        );
        assertTrue(vm.execute("ssh -T git@github.com").text.contains("successfully authenticated"));
    }

    @Test
    public void entireCatalogSmokeTestDoesNotCrash() {
        assertTrue("Catalog should be much larger than 500 entries", CommandCatalog.count() >= 500);

        for (CommandCatalog.Entry entry : CommandCatalog.all()) {
            VirtualMachine vm = preparedVm();

            try {
                assertNotNull(entry.command, vm.execute(entry.command));
            } catch (Throwable error) {
                fail("Catalog command crashed: " + entry.command + " -> " + error);
            }
        }
    }

    private VirtualMachine preparedVm() {
        VirtualMachine vm = new VirtualMachine();

        vm.execute("mkdir work");
        vm.execute("cd work");
        vm.execute("mkdir src");
        vm.execute("mkdir empty");
        vm.execute("touch src/App.java");
        vm.execute("touch README.md");
        vm.execute("echo Base > README.md");
        vm.execute("touch notes.txt");
        vm.execute("echo alpha > notes.txt");
        vm.execute("echo beta >> notes.txt");
        vm.execute("touch temp.txt");

        vm.execute("git init");
        vm.execute("git add .");
        vm.execute("git commit -m \"base\"");
        vm.execute("git branch feature");
        vm.execute("git remote add origin https://github.com/example/repo.git");
        vm.execute("git push -u origin main");

        vm.execute("echo working >> README.md");
        vm.execute("git add README.md");

        vm.execute("ssh-keygen -t ed25519 -C \"test@example.com\"");
        vm.execute("eval \"$(ssh-agent -s)\"");
        vm.execute("ssh-add ~/.ssh/id_ed25519");

        return vm;
    }
}
