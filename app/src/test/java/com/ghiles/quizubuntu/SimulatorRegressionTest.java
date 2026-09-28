package com.ghiles.quizubuntu;

import org.junit.Test;
import static org.junit.Assert.*;

public class SimulatorRegressionTest {
    private VirtualMachine vm() {
        VirtualMachine vm = new VirtualMachine();
        vm.saveEditedFile("/home/ubuntu/README.md", "Git\ngit notes\nGitHub\n10\n2\n\n\n");
        return vm;
    }
    @Test public void executableCatalogueHasOverThousandVariants() {
        int count = 0;
        VirtualMachine vm = vm();
        for (CommandCatalog.Entry e : CommandCatalog.all()) {
            if (!e.category.equals("Texte exécutable")) continue;
            VirtualMachine.Result r = vm.execute(e.command);
            assertEquals(e.command + ": " + r.text, VirtualMachine.Kind.NORMAL, r.kind);
            assertFalse(r.text.contains("documentaire"));
            count++;
        }
        System.out.println("Executable text variants: " + count + "; catalogue total: " + CommandCatalog.count());
        assertTrue(count >= 1000);
    }
    @Test public void grepFlagsAffectActualContents() {
        VirtualMachine vm = vm();
        assertEquals("1:Git\n2:git notes\n3:GitHub\n", vm.execute("grep -in git README.md").text);
        assertEquals("Git\ngit notes\n", vm.execute("grep -iw git README.md").text);
        assertEquals("1\n", vm.execute("grep -ixc Git README.md").text);
        assertEquals("README.md:1:Git\n", vm.execute("grep -Hnx Git README.md").text);
        assertEquals("git notes\n", vm.execute("grep -F 'git notes' README.md").text);
    }
    @Test public void unicodeCountsAndTrailingNewlines() {
        VirtualMachine vm = vm();
        vm.saveEditedFile("/home/ubuntu/texte.txt", "é🙂\n");
        assertEquals("1 texte.txt\n", vm.execute("wc -l texte.txt").text);
        assertEquals("3 texte.txt\n", vm.execute("wc -m texte.txt").text);
        assertEquals("7 texte.txt\n", vm.execute("wc -c texte.txt").text);
        assertEquals("2\n\n\n", vm.execute("tail -n 3 README.md").text);
        assertEquals("Git\n", vm.execute("head -n 1 README.md").text);
    }
    @Test public void sortingAndCatOptions() {
        VirtualMachine vm = vm();
        vm.saveEditedFile("/home/ubuntu/numbers", "10\n2\n2\n-1\n");
        assertEquals("10\n2\n-1\n", vm.execute("sort -nru numbers").text);
        vm.saveEditedFile("/home/ubuntu/text", "a\t\n\n\n");
        assertEquals("     1\ta^I$\n$\n", vm.execute("cat -bsET text").text);
    }
    @Test public void quoteAwareCommandsAndAliasRecursion() {
        VirtualMachine vm = vm();
        assertEquals("a && b\n", vm.execute("echo 'a && b'").text);
        assertEquals("x > y\n", vm.execute("echo 'x > y'").text);
        vm.execute("alias loop='loop'");
        assertEquals(VirtualMachine.Kind.ERROR, vm.execute("loop").kind);
    }
    @Test public void indexCapturesAddTimeNotCommitTime() {
        VirtualMachine vm = vm();
        vm.execute("git init");
        vm.execute("echo staged > README.md");
        vm.execute("git add README.md");
        vm.execute("echo unstaged > README.md");
        assertTrue(vm.execute("git diff --staged").text.contains("+staged"));
        assertFalse(vm.execute("git diff --staged").text.contains("+unstaged"));
        assertTrue(vm.execute("git diff").text.contains("+unstaged"));
        assertTrue(vm.execute("git status").text.contains("Changes not staged"));
        vm.execute("git commit -m snapshot");
        assertTrue(vm.execute("git diff").text.contains("+unstaged"));
    }
    @Test public void redirectsAppendAndTruncate() {
        VirtualMachine vm = vm();
        vm.execute("echo first > text");
        vm.execute("echo second >> text");
        assertEquals("first\nsecond\n", vm.execute("cat text").text);
        vm.execute("> text");
        assertEquals("", vm.execute("cat text").text);
    }
    @Test public void logLimitsAndInitPreserveHistory() {
        VirtualMachine vm = vm();
        vm.execute("git init");
        vm.execute("git add README.md");
        vm.execute("git commit -m first");
        vm.execute("echo second > README.md");
        vm.execute("git add README.md");
        vm.execute("git commit -m second");
        assertFalse(vm.execute("git log --oneline -n 1").text.contains("first"));
        assertEquals(VirtualMachine.Kind.ERROR, vm.execute("git log -n l0").kind);
        assertTrue(vm.execute("git log -p -n 1").text.contains("+second"));
        vm.execute("git init");
        assertTrue(vm.execute("git log").text.contains("first"));
    }
}
