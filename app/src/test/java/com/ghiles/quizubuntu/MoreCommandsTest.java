package com.ghiles.quizubuntu;
import org.junit.Test;
import static org.junit.Assert.*;
public class MoreCommandsTest {
    @Test public void amendReplacesHeadPreservesParentAndUsesIndex() {
        VirtualMachine vm=new VirtualMachine();
        vm.execute("git init; echo first > a; git add a; git commit -m first");
        String first=vm.execute("git rev-parse HEAD").text;
        vm.execute("echo second > a; git add a; git commit -m second; echo staged > a; git add a; echo working > a");
        assertEquals(0,vm.execute("git commit --amend -m revised").exitCode);
        assertEquals("working\n",vm.execute("cat a").text);
        vm.execute("git restore a");
        assertEquals("staged\n",vm.execute("cat a").text);
        String log=vm.execute("git log --oneline").text;
        assertTrue(log.contains("revised"));assertFalse(log.contains("second"));
        assertEquals(2,log.split("\n").length);
        vm.execute("git reset --hard HEAD~1");assertEquals(first,vm.execute("git rev-parse HEAD").text);
    }
    @Test public void amendRootDoesNotIntroduceParentAndKeepsOtherBranch() {
        VirtualMachine vm=new VirtualMachine();
        vm.execute("git init; echo first > a; git add a; git commit -m first; git branch original");
        String first=vm.execute("git rev-parse HEAD").text;
        assertEquals(0,vm.execute("git commit --amend --no-edit").exitCode);
        assertEquals(1,vm.execute("git log --oneline").text.split("\n").length);
        assertNotEquals(first,vm.execute("git rev-parse HEAD").text);
        vm.execute("git switch original");assertEquals(first,vm.execute("git rev-parse HEAD").text);
    }
    @Test public void initDestinationPreservesCwdAndClearsPreviousRemote() {
        VirtualMachine vm=new VirtualMachine();
        vm.execute("git init; git remote add origin https://github.com/example/old.git");
        String cwd=vm.cwd();
        assertEquals(0,vm.execute("git init -b trunk 'new project'").exitCode);
        assertEquals(cwd,vm.cwd());
        vm.execute("cd 'new project'");
        assertEquals("trunk\n",vm.execute("git branch --show-current").text);
        assertEquals("",vm.execute("git remote").text);
        assertTrue(vm.execute("ls -a").entries.stream().anyMatch(e -> e.name.equals(".git")));
        vm.execute("echo data > a; git add a; git commit -m base");
        String hash=vm.execute("git rev-parse HEAD").text;
        vm.execute("git init -b ignored .");assertEquals(hash,vm.execute("git rev-parse HEAD").text);
        assertEquals("trunk\n",vm.execute("git branch --show-current").text);
    }
    @Test public void listingPipelinesRetainFlagsAndHiddenDirectories() {
        VirtualMachine vm=new VirtualMachine();
        vm.execute("mkdir -p listing/sub; touch listing/a; chmod 755 listing/a");
        assertEquals("a*\nsub/\n",vm.execute("ls -F listing | cat").text);
        assertEquals("sub\na\n",vm.execute("ls -r listing | cat").text);
        assertTrue(vm.execute("ls -l listing | cat").text.contains("-rwxr-xr-x  a"));
        assertEquals("listing:\na\nsub\n\nlisting/sub:\n",vm.execute("ls -R listing").text);
        assertEquals(2,vm.execute("ls absent").exitCode);
        assertNotEquals(0,vm.execute("ls --unknown").exitCode);
    }
}
