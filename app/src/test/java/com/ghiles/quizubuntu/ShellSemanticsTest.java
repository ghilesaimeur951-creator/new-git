package com.ghiles.quizubuntu;
import org.junit.Test;
import static org.junit.Assert.*;
public class ShellSemanticsTest {
    private VirtualMachine committed() {
        VirtualMachine vm=new VirtualMachine();
        vm.execute("mkdir project; cd project; git init; echo base > a; git add a; git commit -m first");
        return vm;
    }
    @Test public void detachedCommitsDoNotMoveMain() {
        VirtualMachine vm=committed();String original=vm.execute("git rev-parse HEAD").text;
        assertEquals(0,vm.execute("git switch --detach HEAD").exitCode);
        assertEquals("HEAD",vm.execute("git rev-parse --abbrev-ref HEAD").text.trim());
        vm.execute("echo detached > a; git add a; git commit -m detached");
        assertNotEquals(original,vm.execute("git rev-parse HEAD").text);
        assertEquals(0,vm.execute("git switch main").exitCode);
        assertEquals(original,vm.execute("git rev-parse HEAD").text);
        assertEquals("base\n",vm.execute("cat a").text);
    }
    @Test public void stashPreservesOtherFiles() {
        VirtualMachine vm=committed();
        vm.execute("echo changed > a; echo before > untracked; git stash");
        assertEquals("base\n",vm.execute("cat a").text);
        vm.execute("echo after > untracked; echo outside > ../outside");
        assertEquals(0,vm.execute("git stash pop").exitCode);
        assertEquals("changed\n",vm.execute("cat a").text);
        assertEquals("after\n",vm.execute("cat untracked").text);
        assertEquals("outside\n",vm.execute("cat ../outside").text);
        assertNotEquals(0,vm.execute("git stash pop").exitCode);
    }
    @Test public void stderrIsNotPipedOrRedirectedAsStdout() {
        VirtualMachine vm=new VirtualMachine();
        VirtualMachine.Result result=vm.execute("cat absent | wc -l");
        assertEquals("0\n",result.stdout);
        assertFalse(result.stderr.isEmpty());
        assertEquals(0,result.exitCode);
        vm.execute("cat absent > output");
        assertEquals("",vm.execute("cat output").text);
        assertEquals("ab",vm.execute("printf a; printf b").text);
        assertEquals("a\"b",vm.execute("printf '%s' \"a\\\"b\"").text);
    }
    @Test public void pipelinesAndConditionalsRespectExitCodes() {
        VirtualMachine vm=new VirtualMachine();
        assertEquals("1\n2\n",vm.execute("printf '%s\\n' 3 1 2 | sort -n | head -n 2").text);
        assertEquals("GOOD\n",vm.execute("printf hi | grep absent && echo BAD || echo GOOD").text);
        assertEquals("GOOD\n",vm.execute("false && echo BAD; echo GOOD").text);
        assertEquals("0\n",vm.execute("true; echo $?").text);
        assertEquals("1\n",vm.execute("false; echo $?").text);
    }
    @Test public void redirectsAndQuotedPathsPreserveContent() {
        VirtualMachine vm=new VirtualMachine();
        vm.execute("printf 'a\\nb\\n' > 'a b.txt'");
        assertEquals("2\n",vm.execute("wc -l < 'a b.txt'").text);
        vm.execute("cat 'a b.txt' | grep a >> out.txt");
        assertEquals("a\n",vm.execute("cat out.txt").text);
        assertEquals("x > y\n",vm.execute("echo 'x > y'").text);
    }
    @Test public void archivesActuallyRestoreFiles() {
        VirtualMachine vm=new VirtualMachine();
        vm.execute("mkdir dossier");vm.execute("echo payload > dossier/file");
        assertEquals(0,vm.execute("tar -cf archive.tar dossier").exitCode);
        vm.execute("rm -r dossier");
        assertEquals(0,vm.execute("tar -xf archive.tar").exitCode);
        assertEquals("payload\n",vm.execute("cat dossier/file").text);
        vm.execute("gzip dossier/file");
        assertEquals(0,vm.execute("gunzip dossier/file.gz").exitCode);
        assertEquals("payload\n",vm.execute("cat dossier/file").text);
        assertNotEquals(0,vm.execute("unzip absent.zip").exitCode);
    }
    @Test public void packageServiceAndProcessChangesPersist() {
        VirtualMachine vm=new VirtualMachine();
        vm.execute("systemctl stop ssh");
        assertEquals("inactive\n",vm.execute("systemctl is-active ssh").text);
        assertEquals(3,vm.execute("systemctl is-active ssh").exitCode);
        vm.execute("systemctl start ssh");assertEquals(0,vm.execute("systemctl is-active ssh").exitCode);
        vm.execute("apt install tree");assertTrue(vm.execute("apt list --installed").text.contains("tree"));
        vm.execute("apt remove tree");assertFalse(vm.execute("apt list --installed").text.contains("tree"));
        vm.execute("kill 1234");assertEquals(1,vm.execute("pgrep bash").exitCode);
    }
    @Test public void stagingDeletionAndSoftResetKeepCorrectStates() {
        VirtualMachine vm=new VirtualMachine();vm.execute("mkdir project");vm.execute("cd project");vm.execute("git init");
        vm.execute("echo base > a");vm.execute("git add a");vm.execute("git commit -m first");
        vm.execute("echo second > a");vm.execute("git add -A");vm.execute("git commit -m second");
        vm.execute("git reset --soft HEAD~1");
        assertEquals("M  a\n",vm.execute("git status --porcelain").text);
        assertEquals("second\n",vm.execute("cat a").text);
        vm.execute("git restore --staged a");assertEquals(" M a\n",vm.execute("git status --porcelain").text);
        vm.execute("git restore a");assertEquals("base\n",vm.execute("cat a").text);
        vm.execute("git rm --cached a");assertEquals("base\n",vm.execute("cat a").text);
        assertTrue(vm.execute("git diff --cached --name-status").text.contains("D\ta"));
    }
    @Test public void realHashAlgorithmsAndNoMatchStatus() {
        VirtualMachine vm=new VirtualMachine();vm.execute("printf abc > a");
        assertEquals("900150983cd24fb0d6963f7d28e17f72  a\n",vm.execute("md5sum a").text);
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad  a\n",vm.execute("sha256sum a").text);
        assertEquals(1,vm.execute("grep absent a").exitCode);
        assertNotEquals(0,vm.execute("md5sum absent").exitCode);
    }
}
