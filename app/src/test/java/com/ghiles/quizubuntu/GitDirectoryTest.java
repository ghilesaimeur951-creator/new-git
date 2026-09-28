package com.ghiles.quizubuntu;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;

public class GitDirectoryTest {
    private List<String> names(VirtualMachine vm, String command) {
        VirtualMachine.Result result = vm.execute(command);
        assertEquals(command, VirtualMachine.Kind.LS, result.kind);
        return result.entries.stream().map(e -> e.name).collect(Collectors.toList());
    }
    @Test public void initCreatesHiddenDirectoryInAnEmptyProject() {
        VirtualMachine vm = new VirtualMachine();
        vm.execute("mkdir project");
        vm.execute("cd project");
        assertEquals(2, names(vm, "ls -la").size());
        assertEquals(VirtualMachine.Kind.SUCCESS, vm.execute("git init").kind);
        assertEquals(java.util.Arrays.asList(".", "..", ".git"), names(vm, "ls -la"));
        assertTrue(vm.execute("ls -la").entries.stream().filter(e -> e.name.equals(".git")).allMatch(e -> e.directory));
        assertFalse(names(vm, "ls").contains(".git"));
        assertFalse(names(vm, "ls -l").contains(".git"));
        assertEquals(names(vm, "ls -la"), names(vm, "ls -al"));
        assertTrue(names(vm, "ls -a").contains(".git"));
        assertEquals(java.util.Arrays.asList(".git"), names(vm, "ls -A"));
        vm.execute("git init");
        assertEquals(3, names(vm, "ls -la").size());
        assertTrue(vm.execute("git status").text.contains("working tree clean"));
        assertEquals(VirtualMachine.Kind.NORMAL, vm.execute("cd .git").kind);
        assertEquals("/home/ubuntu/project/.git", vm.cwd());
    }
    @Test public void clonedAndGuidedRepositoriesAlsoHaveGitDirectory() {
        VirtualMachine vm = new VirtualMachine();
        vm.execute("git clone https://github.com/example/demo.git");
        assertTrue(names(vm, "ls -la demo").contains(".git"));
        vm.execute("cd demo");
        assertTrue(names(vm, "ls -la").contains(".git"));
        VirtualMachine guided = new VirtualMachine();
        guided.prepareScenario("remote-ready");
        assertTrue(names(guided, "ls -la").contains(".git"));
        VirtualMachine basic = new VirtualMachine();
        basic.prepareScenario("git-ready");
        assertTrue(names(basic, "ls -la").contains(".git"));
    }
    @Test public void deletingGitDirectoryStopsRepositoryDetection() {
        VirtualMachine vm = new VirtualMachine();
        vm.execute("git init");
        vm.execute("rm -r .git");
        assertFalse(names(vm, "ls -la").contains(".git"));
        assertEquals(VirtualMachine.Kind.ERROR, vm.execute("git status").kind);
        vm.execute("git init");
        assertTrue(names(vm, "ls -la").contains(".git"));
    }
    @Test public void longFormatRecognizesBothFlagOrders() {
        assertTrue(ShellSyntax.hasShortOption("ls -la", 'l'));
        assertTrue(ShellSyntax.hasShortOption("ls -al", 'l'));
        assertTrue(ShellSyntax.hasShortOption("ls -a -l", 'l'));
        assertFalse(ShellSyntax.hasShortOption("ls -a", 'l'));
        assertFalse(ShellSyntax.hasShortOption("ls", 'l'));
    }
}
