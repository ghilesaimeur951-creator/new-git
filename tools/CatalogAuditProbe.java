package com.ghiles.quizubuntu;
import java.util.*;
import java.nio.charset.StandardCharsets;
public final class CatalogAuditProbe {
    static String b64(String text) { return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)); }
    static VirtualMachine fixture() {
        VirtualMachine vm = new VirtualMachine();
        vm.prepareScenario("git-ready");
        for (String directory : new String[]{"dossier", "dossier/sub", "src", "empty", "test", "a/b/c"}) vm.execute("mkdir -p " + directory);
        String sample = "Git\ngit notes\nGitHub\nalpha:10\nbeta:2\nmot\nMOT\n\n\n2\n10\n2\n";
        for (String name : new String[]{"README.md", "fichier.txt", "source.txt", "ancien.txt", "script.sh", "a.txt", "b.txt", "fichier.log", "source", "dossier/file.txt"}) vm.saveEditedFile(vm.cwd() + "/" + name, sample);
        vm.execute("git add ."); vm.execute("git commit -m base"); vm.execute("git branch feature");
        vm.execute("git remote add origin https://github.com/example/repo.git");
        vm.execute("git push -u origin main");
        vm.execute("echo change >> README.md");
        vm.execute("ssh-keygen -t ed25519 -C test@example.com");
        return vm;
    }
    public static void main(String[] args) {
        for (CommandCatalog.Entry entry : CommandCatalog.all()) {
            VirtualMachine vm = fixture();
            try {
                VirtualMachine.Result r = vm.execute(entry.command);
                StringBuilder text = new StringBuilder(r.text);
                for (VirtualMachine.FsEntry f : r.entries) text.append(f.name).append(f.directory ? "/" : "").append('\n');
                System.out.println(b64(entry.command) + "\t" + b64(entry.category) + "\t" + r.kind + "\t" + b64(text.toString()) + "\t" + r.exitCode);
            } catch (Throwable e) { System.out.println(b64(entry.command) + "\t" + b64(entry.category) + "\tCRASH\t" + b64(e.toString())); }
        }
    }
}
