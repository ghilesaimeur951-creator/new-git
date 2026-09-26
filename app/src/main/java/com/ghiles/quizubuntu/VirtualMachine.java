package com.ghiles.quizubuntu;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Stateful, sandboxed Ubuntu/Git learning model.
 *
 * It never calls Android's shell. Every file and Git object lives only in memory,
 * which makes free practice safe while still preserving state between commands.
 */
public final class VirtualMachine {

    public enum Kind {
        NORMAL,
        ERROR,
        SUCCESS,
        LS,
        CLEAR,
        EDIT
    }

    public static final class FsEntry {
        public final String name;
        public final boolean directory;

        FsEntry(String name, boolean directory) {
            this.name = name;
            this.directory = directory;
        }
    }

    public static final class Result {
        public final Kind kind;
        public final String text;
        public final List<FsEntry> entries;
        public final String editPath;

        private Result(Kind kind, String text, List<FsEntry> entries, String editPath) {
            this.kind = kind;
            this.text = text == null ? "" : text;
            this.entries = entries == null ? Collections.emptyList() : entries;
            this.editPath = editPath;
        }

        public static Result normal(String text) {
            return new Result(Kind.NORMAL, text, null, null);
        }

        public static Result success(String text) {
            return new Result(Kind.SUCCESS, text, null, null);
        }

        public static Result error(String text) {
            return new Result(Kind.ERROR, text, null, null);
        }

        public static Result ls(List<FsEntry> entries, String prefix) {
            return new Result(Kind.LS, prefix, entries, null);
        }

        public static Result clear() {
            return new Result(Kind.CLEAR, "", null, null);
        }

        public static Result edit(String path) {
            return new Result(Kind.EDIT, "", null, path);
        }
    }

    private static final class Commit {
        final String hash;
        final String message;
        final String parent;
        final String branch;
        final long when;
        final Map<String,String> snapshot;

        Commit(
            String hash,
            String message,
            String parent,
            String branch,
            Map<String,String> snapshot
        ) {
            this.hash = hash;
            this.message = message;
            this.parent = parent;
            this.branch = branch;
            this.when = System.currentTimeMillis();
            this.snapshot = new LinkedHashMap<>(snapshot);
        }
    }

    private final Set<String> directories = new LinkedHashSet<>();
    private final Map<String,String> files = new LinkedHashMap<>();
    private final List<String> commandHistory = new ArrayList<>();
    private final Map<String,String> environment = new LinkedHashMap<>();
    private final Map<String,String> aliases = new LinkedHashMap<>();
    private final List<Map<String,String>> stashSnapshots = new ArrayList<>();

    private String cwd = "/home/ubuntu";
    private String previousCwd = "/home/ubuntu";

    private boolean gitInitialized = false;
    private String repoRoot = "";
    private String headBranch = "main";
    private final Map<String,String> branches = new LinkedHashMap<>();
    private final Map<String,Commit> commits = new LinkedHashMap<>();
    private final Set<String> staged = new LinkedHashSet<>();
    private Map<String,String> headSnapshot = new LinkedHashMap<>();
    private int commitCounter = 1;

    private String remoteOrigin = "";
    private final Map<String,String> remoteBranches = new LinkedHashMap<>();
    private String originMain = "";

    private boolean conflictActive = false;
    private String conflictFile = "";
    private String conflictBackup = "";
    private boolean conflictOnNextPull = false;

    private final Map<String,String> gitConfig = new LinkedHashMap<>();

    private boolean sshPrivateKeyExists = false;
    private boolean sshAgentRunning = false;
    private boolean sshKeyLoaded = false;

    public VirtualMachine() {
        reset();
    }

    public void reset() {
        directories.clear();
        files.clear();
        commandHistory.clear();
        environment.clear();
        aliases.clear();
        stashSnapshots.clear();

        directories.add("/");
        directories.add("/home");
        directories.add("/home/ubuntu");
        directories.add("/home/ubuntu/Documents");
        directories.add("/home/ubuntu/Téléchargements");
        directories.add("/home/ubuntu/Projets");

        files.put("/home/ubuntu/notes.txt", "Notes Ubuntu & Git\n");

        cwd = "/home/ubuntu";
        previousCwd = cwd;

        environment.put("HOME", "/home/ubuntu");
        environment.put("USER", "ubuntu");
        environment.put("LOGNAME", "ubuntu");
        environment.put("HOSTNAME", "academy");
        environment.put("SHELL", "/bin/bash");
        environment.put("TERM", "xterm-256color");
        environment.put("LANG", "fr_FR.UTF-8");
        environment.put("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin");
        environment.put("PWD", cwd);

        aliases.put("ll", "ls -la");
        aliases.put("la", "ls -A");
        aliases.put("l", "ls -CF");

        gitInitialized = false;
        repoRoot = "";
        headBranch = "main";
        branches.clear();
        commits.clear();
        staged.clear();
        headSnapshot = new LinkedHashMap<>();
        commitCounter = 1;

        remoteOrigin = "";
        remoteBranches.clear();
        originMain = "";

        conflictActive = false;
        conflictFile = "";
        conflictBackup = "";
        conflictOnNextPull = false;

        gitConfig.clear();
        gitConfig.put("user.name", "Ubuntu Student");
        gitConfig.put("user.email", "student@example.com");

        sshPrivateKeyExists = false;
        sshAgentRunning = false;
        sshKeyLoaded = false;
    }

    public String cwd() {
        return cwd;
    }

    public String shortCwd() {
        if ("/home/ubuntu".equals(cwd)) return "~";

        if (cwd.startsWith("/home/ubuntu/")) {
            return "~" + cwd.substring("/home/ubuntu".length());
        }

        return cwd;
    }

    public List<String> history() {
        return new ArrayList<>(commandHistory);
    }

    public String readFileForEditor(String absolutePath) {
        return files.getOrDefault(absolutePath, "");
    }

    public void saveEditedFile(String absolutePath, String content) {
        ensureParentDirectories(absolutePath);
        files.put(absolutePath, content == null ? "" : content);
    }

    public void prepareScenario(String key) {
        if ("fresh-git".equals(key)) {
            ensureDir("/home/ubuntu/projet");
            cwd = "/home/ubuntu/projet";
            files.putIfAbsent("/home/ubuntu/projet/README.md", "# Projet\n");
            gitInitialized = false;
            repoRoot = "";
            return;
        }

        if ("git-ready".equals(key)) {
            ensureGitRepo("/home/ubuntu/projet");
            return;
        }

        if ("remote-ready".equals(key)) {
            ensureGitRepo("/home/ubuntu/projet");
            remoteOrigin = "https://github.com/USER/REPO.git";
            return;
        }

        if ("conflict-pull".equals(key)) {
            ensureGitRepo("/home/ubuntu/projet");

            if (commits.isEmpty()) {
                files.put("/home/ubuntu/projet/README.md", "# merge-conflict\n");
                staged.add("README.md");
                createCommit("Base commune");
            }

            files.put("/home/ubuntu/projet/README.md", "LOCAL\n");
            staged.add("README.md");
            createCommit("Modification locale");

            remoteOrigin = "https://github.com/USER/REPO.git";
            remoteBranches.put("main", "remote123");
            originMain = "remote123";
            conflictOnNextPull = true;
            conflictActive = false;
        }
    }

    public Result execute(String raw) {
        String command = raw == null ? "" : raw.trim();

        if (command.isEmpty()) return Result.normal("");

        if (command.contains("&&")) {
            StringBuilder out = new StringBuilder();

            for (String part : command.split("&&")) {
                Result result = execute(part.trim());

                if (result.kind == Kind.ERROR) return result;
                if (result.kind == Kind.CLEAR) return result;
                if (result.kind == Kind.EDIT) return result;

                if (!result.text.isEmpty()) {
                    if (out.length() > 0) out.append('\n');
                    out.append(result.text);
                }
            }

            return Result.normal(out.toString());
        }

        commandHistory.add(command);
        String n = normalize(command);

        if ("help".equals(n)) return Result.normal(helpText());

        if (n.startsWith("help ")) {
            String query = command.substring(5).trim();
            String matches = CommandCatalog.suggestions(query, 24);
            return Result.normal(matches.isEmpty() ? "Aucune entrée pour : " + query : matches);
        }

        if (n.startsWith("apropos ")) {
            String query = command.substring(8).trim();
            String matches = CommandCatalog.suggestions(query, 30);
            return Result.normal(matches.isEmpty() ? query + ": rien d'approprié" : matches);
        }

        if (n.startsWith("man ")) {
            String query = command.substring(4).trim();
            String matches = CommandCatalog.suggestions(query, 18);
            return Result.normal(
                "UBUNTU LAB MANUAL\n\n" +
                (matches.isEmpty() ? "No manual entry for " + query : matches) +
                "\n\nCatalogue : " + CommandCatalog.count() + "+ signatures d'entraînement."
            );
        }

        if ("compgen -c".equals(n)) {
            StringBuilder out = new StringBuilder();
            Set<String> names = new LinkedHashSet<>();
            for (CommandCatalog.Entry entry : CommandCatalog.all()) {
                String first = entry.command.split("\\s+")[0];
                if (names.add(first)) out.append(first).append('\n');
            }
            return Result.normal(out.toString().trim());
        }

        if ("clear".equals(n)) return Result.clear();

        if ("history".equals(n)) {
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < commandHistory.size(); i++) {
                out.append(i + 1)
                    .append("  ")
                    .append(commandHistory.get(i))
                    .append('\n');
            }
            return Result.normal(out.toString().trim());
        }

        if (aliases.containsKey(n)) {
            return execute(aliases.get(n));
        }

        if ("pwd".equals(n)) return Result.normal(cwd);
        if ("whoami".equals(n)) return Result.normal("ubuntu");
        if ("hostname".equals(n)) return Result.normal("academy");
        if ("uname".equals(n)) return Result.normal("Linux");
        if ("uname -a".equals(n)) {
            return Result.normal(
                "Linux academy 6.8.0-sim #1 SMP PREEMPT_DYNAMIC aarch64 GNU/Linux"
            );
        }
        if ("id".equals(n)) {
            return Result.normal(
                "uid=1000(ubuntu) gid=1000(ubuntu) groupes=1000(ubuntu)"
            );
        }
        if ("date".equals(n)) {
            return Result.normal(
                new SimpleDateFormat(
                    "EEE dd MMM yyyy HH:mm:ss",
                    Locale.FRANCE
                ).format(new Date())
            );
        }

        if ("ls -al ~/.ssh".equals(n) || "ls -la ~/.ssh".equals(n)) {
            return listSsh();
        }

        if ("ls".equals(n) || n.startsWith("ls -")) {
            boolean longFormat = n.contains("l");
            boolean showHidden = n.contains("a") || n.contains("A");
            return list(longFormat, showHidden);
        }

        if ("cd".equals(n) || "cd ~".equals(n)) {
            previousCwd = cwd;
            cwd = "/home/ubuntu";
            environment.put("PWD", cwd);
            return Result.normal("");
        }

        if ("cd -".equals(n)) {
            String next = previousCwd;
            previousCwd = cwd;
            cwd = next;
            environment.put("PWD", cwd);
            return Result.normal(cwd);
        }

        if ("cd ..".equals(n)) {
            previousCwd = cwd;
            cwd = parent(cwd);
            environment.put("PWD", cwd);
            return Result.normal("");
        }

        if (n.startsWith("cd ")) {
            String target = command.substring(3).trim();
            String path = resolve(target);

            if (!directories.contains(path)) {
                return Result.error(
                    "bash: cd: " + target + ": Aucun fichier ou dossier de ce type"
                );
            }

            previousCwd = cwd;
            cwd = path;
            environment.put("PWD", cwd);
            return Result.normal("");
        }

        if (n.startsWith("mkdir -p ")) {
            String target = command.substring(
                command.toLowerCase(Locale.ROOT).indexOf("-p") + 2
            ).trim();

            ensureDir(resolve(target));
            return Result.normal("");
        }

        if (n.startsWith("mkdir ")) {
            String target = command.substring(6).trim();
            String path = resolve(target);
            String p = parent(path);

            if (!directories.contains(p)) {
                return Result.error(
                    "mkdir: impossible de créer le répertoire '" + target +
                    "': Aucun fichier ou dossier de ce type"
                );
            }

            directories.add(path);
            return Result.normal("");
        }

        if (n.startsWith("touch ")) {
            String target = command.substring(6).trim();
            String path = resolve(target);

            if (!directories.contains(parent(path))) {
                return Result.error(
                    "touch: impossible de faire un touch '" + target +
                    "': Aucun fichier ou dossier de ce type"
                );
            }

            files.putIfAbsent(path, "");
            return Result.normal("");
        }

        if ("cat -a ~/.ssh/id_ed25519.pub".equals(n)) {
            return sshPrivateKeyExists
                ? Result.normal("ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAA... student@example.com$")
                : Result.error(
                    "cat: /home/ubuntu/.ssh/id_ed25519.pub: Aucun fichier ou dossier de ce type"
                );
        }

        if (n.startsWith("cat ")) {
            String target = command.substring(4).trim();

            if ("~/.ssh/id_ed25519.pub".equals(target)) {
                return sshPrivateKeyExists
                    ? Result.normal("ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAA... student@example.com")
                    : Result.error(
                        "cat: /home/ubuntu/.ssh/id_ed25519.pub: Aucun fichier ou dossier de ce type"
                    );
            }

            String path = resolve(target);

            if (!files.containsKey(path)) {
                return Result.error(
                    "cat: " + target + ": Aucun fichier ou dossier de ce type"
                );
            }

            return Result.normal(files.get(path));
        }

        if (n.startsWith("nano ")) {
            String target = command.substring(5).trim();
            String path = resolve(target);
            ensureParentDirectories(path);
            files.putIfAbsent(path, "");
            return Result.edit(path);
        }

        if (n.startsWith("echo ")) {
            return echo(command);
        }

        if (n.startsWith("cp -r ")) {
            return copyDirectory(command.substring(6).trim());
        }

        if (n.startsWith("cp ")) {
            return copyFile(command.substring(3).trim());
        }

        if (n.startsWith("mv ")) {
            return move(command.substring(3).trim());
        }

        if (n.startsWith("rm -r ") || n.startsWith("rm -rf ")) {
            int offset = n.startsWith("rm -rf ") ? 7 : 6;
            return removeRecursive(command.substring(offset).trim());
        }

        if (n.startsWith("rm ")) {
            String target = command.substring(3).trim();
            String path = resolve(target);

            if (!files.containsKey(path)) {
                return Result.error(
                    "rm: impossible de supprimer '" + target +
                    "': Aucun fichier ou dossier de ce type"
                );
            }

            files.remove(path);
            staged.remove(relativeToRepo(path));
            return Result.normal("");
        }

        if (n.startsWith("rmdir ")) {
            String target = command.substring(6).trim();
            String path = resolve(target);

            if (!directories.contains(path)) {
                return Result.error(
                    "rmdir: échec de suppression de '" + target +
                    "': Aucun fichier ou dossier de ce type"
                );
            }

            if (hasChildren(path)) {
                return Result.error(
                    "rmdir: échec de suppression de '" + target +
                    "': Le dossier n'est pas vide"
                );
            }

            directories.remove(path);
            return Result.normal("");
        }

        if ("xclip -selection clipboard < ~/.ssh/id_ed25519.pub".equals(n)) {
            return sshPrivateKeyExists
                ? Result.success("[simulation] clé publique copiée dans le presse-papiers X11.")
                : Result.error("bash: ~/.ssh/id_ed25519.pub: Aucun fichier ou dossier de ce type");
        }

        if ("wl-copy < ~/.ssh/id_ed25519.pub".equals(n)) {
            return sshPrivateKeyExists
                ? Result.success("[simulation] clé publique copiée dans le presse-papiers Wayland.")
                : Result.error("bash: ~/.ssh/id_ed25519.pub: Aucun fichier ou dossier de ce type");
        }

        if (n.startsWith("git ") || "gh auth login".equals(n)) {
            return executeGit(command);
        }

        if (n.startsWith("ssh-keygen ") ||
            n.startsWith("ssh-add ") ||
            n.startsWith("ssh -t ") ||
            n.startsWith("eval ")) {
            return executeSsh(command);
        }

        Result extended = executeExtendedShell(command);
        if (extended != null) return extended;

        return Result.error(
            command.split("\\s+")[0] +
            ": commande introuvable dans cet environnement pédagogique" +
            (CommandCatalog.suggestions(command, 4).isEmpty()
                ? ""
                : "\nSuggestions :\n" + CommandCatalog.suggestions(command, 4))
        );
    }

    private Result executeExtendedShell(String command) {
        String n = normalize(command);
        String first = n.split("\\s+")[0];

        if ("alias".equals(n)) {
            StringBuilder out = new StringBuilder();
            for (Map.Entry<String,String> entry : aliases.entrySet()) {
                out.append("alias ")
                    .append(entry.getKey())
                    .append("='")
                    .append(entry.getValue())
                    .append("'\n");
            }
            return Result.normal(out.toString().trim());
        }

        if (n.startsWith("alias ") && command.contains("=")) {
            String definition = command.substring(6).trim();
            int eq = definition.indexOf('=');
            if (eq > 0) {
                String name = definition.substring(0, eq).trim();
                String value = stripQuotes(definition.substring(eq + 1).trim());
                aliases.put(name, value);
                return Result.normal("");
            }
        }

        if (n.startsWith("unalias ")) {
            aliases.remove(command.substring(8).trim());
            return Result.normal("");
        }

        if ("env".equals(n) || "printenv".equals(n) || "set".equals(n)) {
            StringBuilder out = new StringBuilder();
            for (Map.Entry<String,String> entry : environment.entrySet()) {
                out.append(entry.getKey()).append("=").append(entry.getValue()).append('\n');
            }
            return Result.normal(out.toString().trim());
        }

        if (n.startsWith("printenv ")) {
            String key = command.substring(9).trim();
            return Result.normal(environment.getOrDefault(key, ""));
        }

        if (n.startsWith("export ")) {
            String assignment = command.substring(7).trim();
            int eq = assignment.indexOf('=');
            if (eq > 0) {
                environment.put(
                    assignment.substring(0, eq).trim(),
                    stripQuotes(assignment.substring(eq + 1).trim())
                );
                return Result.normal("");
            }
        }

        if (n.startsWith("echo $")) {
            String key = command.substring(6).trim();
            return Result.normal(environment.getOrDefault(key, ""));
        }

        if (n.startsWith("printf ")) {
            String value = stripQuotes(command.substring(7).trim())
                .replace("\\n", "\n")
                .replace("\\t", "\t");
            return Result.normal(value);
        }

        if (n.startsWith("head ") || n.startsWith("tail ")) {
            boolean head = n.startsWith("head ");
            String[] parts = command.split("\\s+");
            int count = 10;
            String target = parts[parts.length - 1];

            for (int i = 1; i < parts.length - 1; i++) {
                if ("-n".equals(parts[i]) && i + 1 < parts.length) {
                    try {
                        count = Integer.parseInt(parts[i + 1]);
                    } catch (Exception ignored) {
                    }
                }
            }

            String path = resolve(target);
            if (!files.containsKey(path)) {
                return Result.error(first + ": impossible d'ouvrir '" + target + "'");
            }

            String[] lines = files.get(path).split("\\n", -1);
            StringBuilder out = new StringBuilder();

            if (head) {
                for (int i = 0; i < Math.min(count, lines.length); i++) {
                    out.append(lines[i]).append('\n');
                }
            } else {
                int start = Math.max(0, lines.length - count);
                for (int i = start; i < lines.length; i++) {
                    out.append(lines[i]).append('\n');
                }
            }

            return Result.normal(out.toString().trim());
        }

        if (n.startsWith("wc ")) {
            String[] parts = command.split("\\s+");
            String target = parts[parts.length - 1];
            String path = resolve(target);

            if (!files.containsKey(path)) {
                return Result.error("wc: " + target + ": Aucun fichier");
            }

            String value = files.get(path);
            int lines = value.isEmpty() ? 0 : value.split("\\n", -1).length;
            int words = value.trim().isEmpty() ? 0 : value.trim().split("\\s+").length;
            int chars = value.length();

            if (n.startsWith("wc -l")) return Result.normal(lines + " " + target);
            if (n.startsWith("wc -w")) return Result.normal(words + " " + target);
            if (n.startsWith("wc -c") || n.startsWith("wc -m")) return Result.normal(chars + " " + target);

            return Result.normal(lines + " " + words + " " + chars + " " + target);
        }

        if (n.startsWith("grep ")) {
            String[] parts = command.split("\\s+");
            if (parts.length < 3) return Result.error("Usage: grep MOT FICHIER");

            String pattern = stripQuotes(parts[parts.length - 2]);
            String target = parts[parts.length - 1];
            String path = resolve(target);

            if (!files.containsKey(path)) {
                return Result.error("grep: " + target + ": Aucun fichier");
            }

            boolean ignoreCase = n.contains(" -i ");
            boolean invert = n.contains(" -v ");
            boolean number = n.contains(" -n ");

            StringBuilder out = new StringBuilder();
            String[] lines = files.get(path).split("\\n", -1);

            for (int i = 0; i < lines.length; i++) {
                String hay = ignoreCase ? lines[i].toLowerCase(Locale.ROOT) : lines[i];
                String needle = ignoreCase ? pattern.toLowerCase(Locale.ROOT) : pattern;
                boolean match = hay.contains(needle);
                if (invert) match = !match;

                if (match) {
                    if (number) out.append(i + 1).append(":");
                    out.append(lines[i]).append('\n');
                }
            }

            return Result.normal(out.toString().trim());
        }

        if (n.startsWith("find ")) {
            StringBuilder out = new StringBuilder();
            boolean wantFiles = n.contains("-type f");
            boolean wantDirs = n.contains("-type d");
            String namePattern = "";

            int namePos = n.indexOf("-name ");
            if (namePos >= 0) {
                namePattern = stripQuotes(command.substring(namePos + 6).trim());
            }

            for (String dir : directories) {
                if (dir.startsWith(cwd) && !wantFiles) {
                    if (namePattern.isEmpty() || simpleGlob(namePattern, baseName(dir))) {
                        out.append(displayRelative(dir)).append('\n');
                    }
                }
            }

            for (String file : files.keySet()) {
                if (file.startsWith(cwd) && !wantDirs) {
                    if (namePattern.isEmpty() || simpleGlob(namePattern, baseName(file))) {
                        out.append(displayRelative(file)).append('\n');
                    }
                }
            }

            return Result.normal(out.toString().trim());
        }

        if (n.startsWith("which ") ||
            n.startsWith("command -v ") ||
            n.startsWith("whereis ") ||
            n.startsWith("type ")) {

            String name = command.substring(command.lastIndexOf(' ') + 1).trim();

            if (aliases.containsKey(name)) {
                return Result.normal(name + " is aliased to '" + aliases.get(name) + "'");
            }

            String desc = CommandCatalog.describe(name);
            return desc.isEmpty()
                ? Result.error(name + " not found")
                : Result.normal("/usr/bin/" + name);
        }

        if (n.startsWith("basename ")) {
            return Result.normal(baseName(resolve(command.substring(9).trim())));
        }

        if (n.startsWith("dirname ")) {
            return Result.normal(parent(resolve(command.substring(8).trim())));
        }

        if ("realpath .".equals(n)) return Result.normal(cwd);

        if (n.startsWith("file ")) {
            String target = command.substring(5).trim();
            String path = resolve(target);

            if (directories.contains(path)) return Result.normal(target + ": directory");
            if (files.containsKey(path)) return Result.normal(target + ": UTF-8 Unicode text");

            return Result.error(target + ": cannot open");
        }

        if (n.startsWith("stat ")) {
            String target = command.substring(5).trim();
            String path = resolve(target);

            if (directories.contains(path)) {
                return Result.normal(
                    "  Fichier : " + target +
                    "\n  Type : dossier" +
                    "\n  Accès : (0755/drwxr-xr-x)"
                );
            }

            if (files.containsKey(path)) {
                return Result.normal(
                    "  Fichier : " + target +
                    "\n  Taille : " + files.get(path).length() +
                    "\n  Accès : (0644/-rw-r--r--)"
                );
            }

            return Result.error("stat: impossible d'évaluer '" + target + "'");
        }

        if (n.startsWith("du")) return Result.normal("4.0K\t.");
        if (n.startsWith("df")) {
            return Result.normal(
                "Sys. de fichiers Taille Utilisé Dispo Uti% Monté sur\n" +
                "/dev/virtual       32G    4G   28G  13% /"
            );
        }

        if ("lsblk".equals(n) || "lsblk -f".equals(n)) {
            return Result.normal("NAME   SIZE TYPE MOUNTPOINT\nvda     32G disk /");
        }

        if (n.startsWith("chmod ") ||
            n.startsWith("chown ") ||
            n.startsWith("chgrp ") ||
            n.startsWith("umask")) {

            if ("umask".equals(n)) return Result.normal("0022");
            return Result.success("[simulation] métadonnées mises à jour.");
        }

        if (n.startsWith("ln -s ")) {
            String[] parts = command.substring(6).trim().split("\\s+");
            if (parts.length >= 2) {
                String destination = resolve(parts[1]);
                files.put(destination, "-> " + parts[0]);
                return Result.normal("");
            }
        }

        if (n.startsWith("diff ")) {
            String clean = command.replace("diff -u ", "diff ");
            String[] parts = clean.substring(5).trim().split("\\s+");

            if (parts.length >= 2) {
                String a = files.getOrDefault(resolve(parts[0]), "");
                String b = files.getOrDefault(resolve(parts[1]), "");

                if (a.equals(b)) return Result.normal("");

                return Result.normal(
                    "--- " + parts[0] +
                    "\n+++ " + parts[1] +
                    "\n-" + firstLine(a) +
                    "\n+" + firstLine(b)
                );
            }
        }

        if (n.startsWith("md5sum ") ||
            n.startsWith("sha1sum ") ||
            n.startsWith("sha256sum ") ||
            n.startsWith("sha512sum ") ||
            n.startsWith("cksum ")) {

            String target = command.substring(command.indexOf(' ') + 1).trim();
            String content = files.getOrDefault(resolve(target), "");

            long hash = 1125899906842597L;
            for (char c : content.toCharArray()) hash = 31 * hash + c;

            return Result.normal(Long.toHexString(hash) + "  " + target);
        }

        if ("groups".equals(n)) return Result.normal("ubuntu adm sudo");
        if ("users".equals(n)) return Result.normal("ubuntu");
        if ("who".equals(n)) return Result.normal("ubuntu   pts/0   2026-09-25 18:00");
        if ("w".equals(n)) return Result.normal("ubuntu   pts/0   bash");
        if ("hostname -f".equals(n)) return Result.normal("academy.local");
        if ("hostnamectl".equals(n)) {
            return Result.normal(
                "Static hostname: academy\n" +
                "Operating System: Ubuntu 24.04 LTS\n" +
                "Kernel: Linux 6.8.0-sim"
            );
        }
        if ("uname -r".equals(n)) return Result.normal("6.8.0-sim");
        if (n.startsWith("uptime")) return Result.normal("18:00:00 up 1 day, 2:14, 1 user, load average: 0.08, 0.06, 0.05");
        if (n.startsWith("free")) return Result.normal("               total        used        free\nMem:           7.8Gi       2.1Gi       5.7Gi");
        if (n.startsWith("cal")) return Result.normal("   septembre 2026\nlu ma me je ve sa di\n       1  2  3  4  5  6");
        if ("lscpu".equals(n)) return Result.normal("Architecture: aarch64\nCPU(s): 8\nModèle: Ubuntu Lab Virtual CPU");
        if ("lsmem".equals(n)) return Result.normal("RANGE                                  SIZE  STATE\n0x0000000000000000-0x00000001ffffffff   8G online");

        if (n.startsWith("ps")) return Result.normal("  PID TTY          TIME CMD\n 1234 pts/0    00:00:00 bash\n 1250 pts/0    00:00:00 ps");
        if (n.startsWith("pgrep ")) return Result.normal("1234");
        if (n.startsWith("pidof ")) return Result.normal("1234");
        if ("top".equals(n)) return Result.normal("top - Ubuntu Lab simulation\nTasks: 4 total, 1 running\n%Cpu(s): 2.0 us, 98.0 id");

        if (n.startsWith("kill ") ||
            n.startsWith("pkill ") ||
            n.startsWith("nice ") ||
            n.startsWith("renice ")) {

            return Result.success("[simulation] signal/priority appliqué.");
        }

        if (n.startsWith("ip addr") || n.startsWith("ip -br addr")) {
            return Result.normal("lo       UNKNOWN 127.0.0.1/8\nwlan0    UP      192.168.1.42/24");
        }
        if (n.startsWith("ip link")) return Result.normal("1: lo: <LOOPBACK,UP>\n2: wlan0: <BROADCAST,MULTICAST,UP>");
        if (n.startsWith("ip route")) return Result.normal("default via 192.168.1.1 dev wlan0");
        if (n.startsWith("ss")) return Result.normal("Netid State  Local Address:Port Peer Address:Port\ntcp   LISTEN 127.0.0.1:22      0.0.0.0:*");
        if (n.startsWith("ping ")) return Result.normal("64 bytes from github.com: icmp_seq=1 ttl=54 time=18.4 ms\n--- ping statistics ---\n1 packets transmitted, 1 received");
        if (n.startsWith("curl ")) return Result.normal("[simulation réseau] HTTP/2 200\ncontent-type: text/html");
        if (n.startsWith("wget ")) return Result.success("[simulation réseau] fichier téléchargé.");
        if (n.startsWith("getent hosts ")) return Result.normal("140.82.121.4   github.com");
        if ("resolvectl status".equals(n)) return Result.normal("Global\n       Protocols: -LLMNR -mDNS\nCurrent DNS Server: 1.1.1.1");

        if (n.startsWith("apt ") || n.startsWith("dpkg ")) {
            if (n.startsWith("apt install ") ||
                n.startsWith("apt remove ") ||
                n.startsWith("apt upgrade") ||
                n.startsWith("apt update")) {

                return Result.success(
                    "[simulation] gestionnaire de paquets Ubuntu : aucune modification réelle du téléphone."
                );
            }

            return Result.normal(
                "[simulation] paquet Git/Ubuntu disponible dans le catalogue pédagogique."
            );
        }

        if (n.startsWith("systemctl ")) {
            return Result.normal("[simulation] service ssh.service : active (running)");
        }

        if (n.startsWith("journalctl")) {
            return Result.normal("Sep 25 18:00:00 academy systemd[1]: Ubuntu Lab journal simulé");
        }

        if (n.startsWith("tar ") ||
            n.startsWith("gzip ") ||
            n.startsWith("gunzip ") ||
            n.startsWith("zip ") ||
            n.startsWith("unzip ")) {

            return Result.success("[simulation] opération d'archive terminée.");
        }

        if ("jobs".equals(n) || "jobs -l".equals(n)) return Result.normal("[1]+  Running                 demo &");
        if ("bg".equals(n)) return Result.normal("[1]+ demo &");
        if ("fg".equals(n)) return Result.normal("demo");
        if (n.startsWith("sleep ")) return Result.normal("");
        if ("true".equals(n)) return Result.normal("");
        if ("false".equals(n)) return Result.error("");

        String catalog = CommandCatalog.describe(command);
        if (!catalog.isEmpty()) {
            return Result.normal("[simulation documentaire] " + catalog);
        }

        return null;
    }

    private boolean simpleGlob(String pattern, String value) {
        String regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".");

        return value.matches(regex);
    }

    private String baseName(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) return path;
        int i = path.lastIndexOf('/');
        return i < 0 ? path : path.substring(i + 1);
    }

    private String displayRelative(String path) {
        if (path.equals(cwd)) return ".";
        if (path.startsWith(cwd + "/")) return "." + path.substring(cwd.length());
        return path;
    }

    private Result executeGit(String command) {
        String n = normalize(command);

        if ("git --version".equals(n)) {
            return Result.normal("git version 2.43.0");
        }

        if ("gh auth login".equals(n)) {
            return Result.normal(
                "GitHub CLI (simulation) : authentification HTTPS pédagogique. " +
                "Utilise le mode GITHUB RÉEL pour une connexion réseau."
            );
        }

        if (n.startsWith("git config ")) {
            return gitConfig(command);
        }

        if (n.startsWith("git clone ")) {
            String url = command.substring("git clone ".length()).trim();
            String name = repoNameFromUrl(url);
            String newRoot = resolve(name);

            ensureDir(newRoot);
            files.put(newRoot + "/README.md", "# " + name + "\n");

            cwd = newRoot;
            gitInitialized = true;
            repoRoot = newRoot;
            headBranch = "main";
            branches.clear();
            commits.clear();
            staged.clear();
            headSnapshot.clear();
            remoteOrigin = url;
            remoteBranches.clear();
            originMain = "";

            staged.add("README.md");
            createCommit("Initial commit");
            originMain = branches.get("main");
            remoteBranches.put("main", originMain);

            cwd = parent(newRoot);

            return Result.success(
                "Clonage dans '" + name + "'...\n" +
                "remote origin configuré."
            );
        }

        if ("git init".equals(n)) {
            gitInitialized = true;
            repoRoot = cwd;
            headBranch = "main";
            branches.clear();
            commits.clear();
            staged.clear();
            headSnapshot = new LinkedHashMap<>();
            branches.put("main", "");

            return Result.success(
                "Dépôt Git vide initialisé dans " + repoRoot + "/.git/"
            );
        }

        if (!inGitRepo()) {
            return Result.error(
                "fatal: not a git repository (or any of the parent directories): .git"
            );
        }

        if ("git status".equals(n)) return gitStatus();

        if (n.startsWith("git add ")) {
            String arg = command.substring("git add ".length()).trim();

            if (".".equals(arg)) {
                for (String path : files.keySet()) {
                    if (path.equals(repoRoot) || path.startsWith(repoRoot + "/")) {
                        staged.add(relativeToRepo(path));
                    }
                }
            } else {
                String path = resolve(arg);
                if (!path.startsWith(repoRoot)) {
                    return Result.error("fatal: pathspec outside repository");
                }

                staged.add(relativeToRepo(path));
            }

            if (conflictActive && arg.endsWith("README.md")) {
                conflictActive = false;
            }

            return Result.normal("");
        }

        if (n.startsWith("git commit -m ")) {
            String message = stripQuotes(
                command.substring("git commit -m ".length()).trim()
            );

            if (staged.isEmpty()) {
                return Result.normal("nothing added to commit");
            }

            Commit commit = createCommit(message);

            return Result.success(
                "[" + headBranch + " " + commit.hash + "] " + message
            );
        }

        if (n.startsWith("git log")) {
            return gitLog(command);
        }

        if ("git diff".equals(n)) return gitDiff(false);
        if ("git diff --staged".equals(n) || "git diff --cached".equals(n)) {
            return gitDiff(true);
        }

        if ("git branch".equals(n)) return gitBranch(false);
        if ("git branch -a".equals(n)) return gitBranch(true);

        if (n.startsWith("git branch -D ")) {
            String name = command.substring("git branch -D ".length()).trim();

            if (name.equals(headBranch)) {
                return Result.error("error: Cannot delete branch '" + name + "' checked out");
            }

            branches.remove(name);
            return Result.success("Deleted branch " + name + " (forced).");
        }

        if (n.startsWith("git branch ") &&
            !n.startsWith("git branch -") &&
            command.trim().split("\\s+").length >= 3) {

            String[] args = command.trim().split("\\s+");
            String name = args[2];

            if (branches.containsKey(name)) {
                return Result.error("fatal: a branch named '" + name + "' already exists");
            }

            String start = branches.getOrDefault(headBranch, "");
            if (args.length >= 4) {
                Commit startCommit = findCommit(args[3]);
                if (startCommit != null) start = startCommit.hash;
            }

            branches.put(name, start);
            return Result.success("Branch '" + name + "' created.");
        }

        if (n.startsWith("git branch -m ")) {
            String[] args = command.trim().split("\\s+");

            if (args.length < 4) {
                return Result.error("fatal: branch name required");
            }

            String newName = args[3];
            String hash = branches.remove(headBranch);

            headBranch = newName;
            branches.put(headBranch, hash == null ? "" : hash);

            return Result.normal("");
        }

        if (n.startsWith("git branch -d ")) {
            String name = command.substring("git branch -d ".length()).trim();

            if (name.equals(headBranch)) {
                return Result.error(
                    "error: Cannot delete branch '" + name + "' checked out"
                );
            }

            if (!branches.containsKey(name)) {
                return Result.error(
                    "error: branch '" + name + "' not found."
                );
            }

            branches.remove(name);
            return Result.success("Deleted branch " + name + ".");
        }

        if (command.startsWith("git switch -C ")) {
            String name = command.substring("git switch -C ".length()).trim();
            branches.put(name, branches.getOrDefault(headBranch, ""));
            headBranch = name;
            checkoutHeadSnapshot();
            return Result.success("Switched to and reset branch '" + name + "'");
        }

        if (n.startsWith("git switch -c ")) {
            String name = command.substring("git switch -c ".length()).trim();

            if (branches.containsKey(name)) {
                return Result.error(
                    "fatal: a branch named '" + name + "' already exists"
                );
            }

            branches.put(name, branches.getOrDefault(headBranch, ""));
            headBranch = name;

            return Result.success(
                "Switched to a new branch '" + name + "'"
            );
        }

        if (n.startsWith("git switch ")) {
            String name = command.substring("git switch ".length()).trim();

            if (!branches.containsKey(name)) {
                return Result.error(
                    "fatal: invalid reference: " + name
                );
            }

            headBranch = name;
            checkoutHeadSnapshot();

            return Result.success("Switched to branch '" + name + "'");
        }

        if (command.startsWith("git checkout -B ")) {
            String name = command.substring("git checkout -B ".length()).trim();
            branches.put(name, branches.getOrDefault(headBranch, ""));
            headBranch = name;
            checkoutHeadSnapshot();
            return Result.success("Switched to and reset branch '" + name + "'");
        }

        if (n.startsWith("git checkout -b ")) {
            String name = command.substring("git checkout -b ".length()).trim();

            if (branches.containsKey(name)) {
                return Result.error("fatal: a branch named '" + name + "' already exists");
            }

            branches.put(name, branches.getOrDefault(headBranch, ""));
            headBranch = name;
            checkoutHeadSnapshot();

            return Result.success("Switched to a new branch '" + name + "'");
        }

        if (n.startsWith("git checkout -B ")) {
            String name = command.substring("git checkout -B ".length()).trim();
            branches.put(name, branches.getOrDefault(headBranch, ""));
            headBranch = name;
            checkoutHeadSnapshot();
            return Result.success("Switched to and reset branch '" + name + "'");
        }

        if (n.startsWith("git checkout --detach ")) {
            String ref = command.substring("git checkout --detach ".length()).trim();
            Commit commit = findCommit(ref);
            if (commit == null && "HEAD".equalsIgnoreCase(ref)) {
                commit = findCommit(branches.getOrDefault(headBranch, ""));
            }
            return commit == null
                ? Result.error("fatal: invalid reference: " + ref)
                : Result.success("HEAD is now detached at " + commit.hash + " " + commit.message);
        }

        if ("git checkout -".equals(n) || "git switch -".equals(n)) {
            return Result.normal("Déplacement vers la branche précédente simulé.");
        }

        if (n.startsWith("git checkout -- ")) {
            String path = command.substring("git checkout -- ".length()).trim();
            return restoreFromHead(path);
        }

        if (n.startsWith("git checkout head -- ")) {
            String path = command.substring(command.indexOf("--") + 2).trim();
            return restoreFromHead(path);
        }

        if (n.startsWith("git checkout ") &&
            !n.startsWith("git checkout --detach") &&
            !n.contains(" -- ")) {

            String name = command.substring("git checkout ".length()).trim();

            if (branches.containsKey(name)) {
                headBranch = name;
                checkoutHeadSnapshot();
                return Result.success("Switched to branch '" + name + "'");
            }

            Commit commit = findCommit(name);
            if (commit != null) {
                headSnapshot = new LinkedHashMap<>(commit.snapshot);
                return Result.success("HEAD is now at " + commit.hash + " " + commit.message);
            }

            return Result.error("error: pathspec '" + name + "' did not match any branch or commit");
        }

        if (n.startsWith("git restore --staged ")) {
            String path = command.substring("git restore --staged ".length()).trim();
            staged.remove(path);
            return Result.normal("");
        }

        if (n.startsWith("git restore ")) {
            String path = command.substring("git restore ".length()).trim();

            if (path.startsWith("--source=")) {
                int space = path.indexOf(' ');
                path = space < 0 ? "" : path.substring(space + 1).trim();
            }

            return restoreFromHead(path);
        }

        if (n.startsWith("git reset --hard")) {
            checkoutHeadSnapshot();
            staged.clear();
            conflictActive = false;
            return Result.success("HEAD is now at " + branches.getOrDefault(headBranch, ""));
        }

        if (n.startsWith("git reset --soft ") || n.startsWith("git reset --mixed ")) {
            return Result.success("[simulation] HEAD déplacé ; working tree conservé.");
        }

        if (n.startsWith("git reset ")) {
            String arg = command.substring("git reset ".length()).trim();

            if ("HEAD".equalsIgnoreCase(arg)) {
                staged.clear();
                return Result.normal("Unstaged changes after reset.");
            }

            staged.remove(arg);
            return Result.normal("Unstaged '" + arg + "'");
        }

        if ("git stash".equals(n) ||
            "git stash push".equals(n) ||
            n.startsWith("git stash push -m ")) {

            stashSnapshots.add(new LinkedHashMap<>(files));
            checkoutHeadSnapshot();
            staged.clear();

            return Result.success("Saved working directory and index state WIP on " + headBranch);
        }

        if ("git stash list".equals(n)) {
            StringBuilder out = new StringBuilder();

            for (int i = stashSnapshots.size() - 1, nstash = 0; i >= 0; i--, nstash++) {
                out.append("stash@{")
                    .append(nstash)
                    .append("}: WIP on ")
                    .append(headBranch)
                    .append('\n');
            }

            return Result.normal(out.toString().trim());
        }

        if ("git stash pop".equals(n) || "git stash apply".equals(n)) {
            if (stashSnapshots.isEmpty()) return Result.normal("No stash entries found.");

            Map<String,String> snapshot = stashSnapshots.get(stashSnapshots.size() - 1);
            files.clear();
            files.putAll(snapshot);

            if ("git stash pop".equals(n)) {
                stashSnapshots.remove(stashSnapshots.size() - 1);
            }

            return Result.success("On branch " + headBranch + "\nChanges restored from stash.");
        }

        if ("git stash drop".equals(n)) {
            if (!stashSnapshots.isEmpty()) {
                stashSnapshots.remove(stashSnapshots.size() - 1);
            }

            return Result.success("Dropped refs/stash@{0}");
        }

        if ("git stash clear".equals(n)) {
            stashSnapshots.clear();
            return Result.normal("");
        }

        if ("git reflog".equals(n) || n.startsWith("git reflog -")) {
            StringBuilder out = new StringBuilder();
            List<Commit> list = new ArrayList<>(commits.values());
            Collections.reverse(list);

            int i = 0;
            for (Commit commit : list) {
                out.append(commit.hash)
                    .append(" HEAD@{")
                    .append(i++)
                    .append("}: commit: ")
                    .append(commit.message)
                    .append('\n');
            }

            return Result.normal(out.toString().trim());
        }

        if ("git rev-parse --abbrev-ref head".equals(n)) return Result.normal(headBranch);
        if ("git rev-parse head".equals(n)) return Result.normal(padHash(branches.getOrDefault(headBranch, "")));
        if ("git rev-parse --show-toplevel".equals(n)) return Result.normal(repoRoot);

        if (n.startsWith("git rm ")) {
            String path = command.substring("git rm ".length())
                .replace("--cached ", "")
                .trim();

            String absolute = repoRoot + "/" + path;

            if (!command.contains("--cached")) {
                files.remove(absolute);
            }

            staged.add(path);
            return Result.normal("rm '" + path + "'");
        }

        if (n.startsWith("git mv ")) {
            String[] args = command.substring("git mv ".length()).trim().split("\\s+");

            if (args.length >= 2) {
                Result moved = move(args[0] + " " + args[1]);

                if (moved.kind != Kind.ERROR) {
                    staged.add(args[0]);
                    staged.add(args[1]);
                }

                return moved;
            }
        }

        if (n.startsWith("git clean -n") || n.startsWith("git clean -nd")) {
            StringBuilder out = new StringBuilder();

            for (String path : repoFiles()) {
                if (!headSnapshot.containsKey(path) && !staged.contains(path)) {
                    out.append("Would remove ").append(path).append('\n');
                }
            }

            return Result.normal(out.toString().trim());
        }

        if (n.startsWith("git clean -fd")) {
            List<String> remove = new ArrayList<>();

            for (String path : repoFiles()) {
                if (!headSnapshot.containsKey(path) && !staged.contains(path)) {
                    remove.add(path);
                }
            }

            for (String path : remove) {
                files.remove(repoRoot + "/" + path);
            }

            return Result.success("Removing " + remove.size() + " untracked path(s).");
        }

        if (n.startsWith("git merge ") && !"git merge --abort".equals(n)) {
            String name = command.substring("git merge ".length())
                .replace("--no-ff ", "")
                .replace("--ff-only ", "")
                .trim();

            if (!branches.containsKey(name)) {
                return Result.error("merge: " + name + " - not something we can merge");
            }

            String incoming = branches.get(name);
            branches.put(headBranch, incoming);
            checkoutHeadSnapshot();

            return Result.success("Updating " + headBranch + ".." + incoming + "\nFast-forward");
        }

        if (n.startsWith("git cherry-pick ")) {
            String ref = command.substring("git cherry-pick ".length())
                .replace("--abort", "")
                .trim();

            if (ref.isEmpty()) {
                return Result.success("[simulation] cherry-pick annulé.");
            }

            Commit original = findCommit(ref);
            if (original == null) return Result.error("fatal: bad revision '" + ref + "'");

            for (Map.Entry<String,String> entry : original.snapshot.entrySet()) {
                files.put(repoRoot + "/" + entry.getKey(), entry.getValue());
                staged.add(entry.getKey());
            }

            Commit copy = createCommit(original.message);

            return Result.success("[" + headBranch + " " + copy.hash + "] " + copy.message);
        }

        if (n.startsWith("git revert ")) {
            String ref = command.substring("git revert ".length())
                .replace("--no-edit ", "")
                .trim();

            Commit original = findCommit(ref);
            if (original == null) return Result.error("fatal: bad revision '" + ref + "'");

            Commit revert = createCommit("Revert: " + original.message);

            return Result.success("[" + headBranch + " " + revert.hash + "] " + revert.message);
        }

        if ("git status -s".equals(n) ||
            "git status --short".equals(n) ||
            "git status -sb".equals(n) ||
            "git status --porcelain".equals(n)) {

            return Result.normal(gitStatus().text);
        }

        if ("git remote".equals(n)) {
            return Result.normal(remoteOrigin.isEmpty() ? "" : "origin");
        }

        if ("git remote show origin".equals(n)) {
            return remoteOrigin.isEmpty()
                ? Result.error("fatal: 'origin' does not appear to be a git repository")
                : Result.normal(
                    "* remote origin\n" +
                    "  Fetch URL: " + remoteOrigin + "\n" +
                    "  Push URL: " + remoteOrigin + "\n" +
                    "  HEAD branch: main"
                );
        }

        if (n.startsWith("git remote rename ")) {
            return Result.success("[simulation] remote renommé.");
        }

        if ("git remote -v".equals(n)) {
            if (remoteOrigin.isEmpty()) return Result.normal("");

            return Result.normal(
                "origin  " + remoteOrigin + " (fetch)\n" +
                "origin  " + remoteOrigin + " (push)"
            );
        }

        if ("git remote get-url origin".equals(n)) {
            return remoteOrigin.isEmpty()
                ? Result.error("error: No such remote 'origin'")
                : Result.normal(remoteOrigin);
        }

        if (n.startsWith("git remote add origin ")) {
            if (!remoteOrigin.isEmpty()) {
                return Result.error(
                    "error: remote origin already exists."
                );
            }

            remoteOrigin = command.substring(
                "git remote add origin ".length()
            ).trim();

            return Result.normal("");
        }

        if (n.startsWith("git remote set-url origin ")) {
            remoteOrigin = command.substring(
                "git remote set-url origin ".length()
            ).trim();

            return Result.normal("");
        }

        if ("git remote remove origin".equals(n)) {
            remoteOrigin = "";
            remoteBranches.clear();
            originMain = "";
            return Result.normal("");
        }

        if ("git fetch".equals(n) ||
            n.startsWith("git fetch origin") ||
            "git fetch --all".equals(n) ||
            "git fetch --prune".equals(n) ||
            "git fetch --tags".equals(n) ||
            "git fetch --dry-run".equals(n) ||
            "git fetch --verbose".equals(n)) {

            if (remoteOrigin.isEmpty()) {
                return Result.error(
                    "fatal: 'origin' does not appear to be a git repository"
                );
            }

            if (remoteBranches.containsKey("main")) {
                originMain = remoteBranches.get("main");
            } else if (!branches.getOrDefault("main", "").isEmpty()) {
                originMain = branches.get("main");
                remoteBranches.put("main", originMain);
            }

            return Result.success(
                "From " + remoteOrigin + "\n" +
                " * branch main -> origin/main"
            );
        }

        if ("git pull".equals(n) ||
            n.startsWith("git pull origin main") ||
            n.startsWith("git pull --rebase origin main") ||
            n.startsWith("git pull --ff-only origin main") ||
            n.startsWith("git pull --no-rebase origin main") ||
            n.startsWith("git pull --autostash origin main")) {
            return gitPull(command);
        }

        if ("git push".equals(n) ||
            n.startsWith("git push origin ") ||
            n.startsWith("git push -u origin ") ||
            n.startsWith("git push --set-upstream origin ") ||
            n.startsWith("git push --force-with-lease origin ")) {

            return gitPush(command);
        }

        if ("git ls-remote origin".equals(n) ||
            "git ls-remote --heads origin".equals(n)) {
            if (remoteOrigin.isEmpty()) {
                return Result.error(
                    "fatal: 'origin' does not appear to be a git repository"
                );
            }

            StringBuilder out = new StringBuilder();

            for (Map.Entry<String,String> entry : remoteBranches.entrySet()) {
                out.append(padHash(entry.getValue()))
                    .append("\trefs/heads/")
                    .append(entry.getKey())
                    .append('\n');
            }

            return Result.normal(out.toString().trim());
        }

        if ("git merge --abort".equals(n)) {
            if (!conflictActive) {
                return Result.error(
                    "fatal: There is no merge to abort (MERGE_HEAD missing)."
                );
            }

            if (!conflictFile.isEmpty()) {
                files.put(conflictFile, conflictBackup);
            }

            conflictActive = false;
            conflictOnNextPull = false;
            staged.remove(relativeToRepo(conflictFile));

            return Result.success("Merge annulé.");
        }

        if (n.startsWith("git show ")) {
            String[] parts = command.split("\\s+");
            String ref = parts[parts.length - 1];
            Commit commit = findCommit(ref);

            if (commit == null) {
                return Result.error("fatal: bad object " + ref);
            }

            return Result.normal(
                commit.hash + " " + commit.message + "\n" +
                "  README.md | état simulé"
            );
        }

        String catalogue = CommandCatalog.describe(command);

        if (!catalogue.isEmpty()) {
            return Result.normal("[simulation documentaire Git] " + catalogue);
        }

        return Result.error(
            "git: commande ou variante non prise en charge : " + command +
            "\nEssaie « help git », « help checkout » ou « man checkout »."
        );
    }

    private Result restoreFromHead(String relative) {
        String path = relative == null ? "" : relative.trim();

        if (path.isEmpty()) {
            return Result.error("fatal: pathspec vide");
        }

        String content = headSnapshot.get(path);

        if (content == null) {
            return Result.error(
                "error: pathspec '" + path + "' did not match any file known to git"
            );
        }

        files.put(repoRoot + "/" + path, content);
        staged.remove(path);

        return Result.normal("");
    }

    private Result executeSsh(String command) {
        String n = normalize(command);

        if (n.startsWith("ssh-keygen -t ed25519")) {
            sshPrivateKeyExists = true;
            ensureDir("/home/ubuntu/.ssh");
            files.put(
                "/home/ubuntu/.ssh/id_ed25519",
                "[clé privée simulée — jamais affichée]"
            );
            files.put(
                "/home/ubuntu/.ssh/id_ed25519.pub",
                "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAA... student@example.com\n"
            );

            return Result.success(
                "Your identification has been saved in /home/ubuntu/.ssh/id_ed25519\n" +
                "Your public key has been saved in /home/ubuntu/.ssh/id_ed25519.pub"
            );
        }

        if (n.equals("eval \"$(ssh-agent -s)\"") ||
            n.equals("eval '$(ssh-agent -s)'")) {

            sshAgentRunning = true;
            return Result.normal("Agent pid 4242");
        }

        if ("ssh-add ~/.ssh/id_ed25519".equals(n)) {
            if (!sshPrivateKeyExists) {
                return Result.error(
                    "/home/ubuntu/.ssh/id_ed25519: No such file or directory"
                );
            }

            if (!sshAgentRunning) {
                return Result.error(
                    "Could not open a connection to your authentication agent."
                );
            }

            sshKeyLoaded = true;
            return Result.success("Identity added: /home/ubuntu/.ssh/id_ed25519");
        }

        if ("ssh-keygen -lf ~/.ssh/id_ed25519.pub".equals(n)) {
            return sshPrivateKeyExists
                ? Result.normal("256 SHA256:SIMULATEDFINGERPRINT student@example.com (ED25519)")
                : Result.error("/home/ubuntu/.ssh/id_ed25519.pub is not a public key file.");
        }

        if ("ssh -t git@github.com".equals(n)) {
            return sshPrivateKeyExists
                ? Result.success(
                    "Hi USER! You've successfully authenticated, but GitHub does not provide shell access."
                )
                : Result.error("git@github.com: Permission denied (publickey).");
        }

        return Result.error("ssh: commande simulée non prise en charge");
    }

    private Result gitConfig(String command) {
        String n = normalize(command);

        if ("git config --list".equals(n) ||
            "git config --global --list".equals(n)) {

            StringBuilder out = new StringBuilder();

            for (Map.Entry<String,String> entry : gitConfig.entrySet()) {
                out.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue())
                    .append('\n');
            }

            return Result.normal(out.toString().trim());
        }

        if ("git config --list --show-origin".equals(n)) {
            StringBuilder out = new StringBuilder();

            for (Map.Entry<String,String> entry : gitConfig.entrySet()) {
                out.append("file:/home/ubuntu/.gitconfig\t")
                    .append(entry.getKey())
                    .append("=")
                    .append(entry.getValue())
                    .append('\n');
            }

            return Result.normal(out.toString().trim());
        }

        if (n.startsWith("git config --global --unset ")) {
            String key = command.substring(
                "git config --global --unset ".length()
            ).trim();

            gitConfig.remove(key);
            return Result.normal("");
        }

        if (n.startsWith("git config --global user.name ")) {
            gitConfig.put(
                "user.name",
                stripQuotes(
                    command.substring(
                        "git config --global user.name ".length()
                    ).trim()
                )
            );
            return Result.normal("");
        }

        if (n.startsWith("git config --global user.email ")) {
            gitConfig.put(
                "user.email",
                stripQuotes(
                    command.substring(
                        "git config --global user.email ".length()
                    ).trim()
                )
            );
            return Result.normal("");
        }

        return Result.error("git config: variante non prise en charge");
    }

    private Result gitStatus() {
        StringBuilder out = new StringBuilder();

        out.append("On branch ")
            .append(headBranch)
            .append('\n');

        if (conflictActive) {
            out.append("You have unmerged paths.\n")
                .append("  both modified:   ")
                .append(relativeToRepo(conflictFile))
                .append('\n')
                .append("\nfix conflicts and run \"git add <file>\"");

            return Result.error(out.toString());
        }

        Set<String> current = repoFiles();
        Set<String> untracked = new LinkedHashSet<>();
        Set<String> modified = new LinkedHashSet<>();

        for (String relative : current) {
            String absolute = repoRoot + "/" + relative;

            if (!headSnapshot.containsKey(relative)) {
                if (!staged.contains(relative)) untracked.add(relative);
            } else if (!files.getOrDefault(absolute, "")
                .equals(headSnapshot.get(relative))) {

                if (!staged.contains(relative)) modified.add(relative);
            }
        }

        if (!staged.isEmpty()) {
            out.append("\nChanges to be committed:\n");
            for (String path : staged) {
                out.append("  modified:   ").append(path).append('\n');
            }
        }

        if (!modified.isEmpty()) {
            out.append("\nChanges not staged for commit:\n");
            for (String path : modified) {
                out.append("  modified:   ").append(path).append('\n');
            }
        }

        if (!untracked.isEmpty()) {
            out.append("\nUntracked files:\n");
            for (String path : untracked) {
                out.append("  ").append(path).append('\n');
            }
        }

        if (staged.isEmpty() && modified.isEmpty() && untracked.isEmpty()) {
            out.append("nothing to commit, working tree clean");
        }

        if (!originMain.isEmpty()) {
            String local = branches.getOrDefault(headBranch, "");

            if (local.equals(originMain)) {
                out.append("\nYour branch is up to date with 'origin/main'.");
            } else {
                out.append("\nYour branch and 'origin/main' have diverged.");
            }
        }

        return Result.normal(out.toString().trim());
    }

    private Result gitLog(String command) {
        boolean oneline = command.contains("--oneline");
        boolean decorate = command.contains("--decorate");
        boolean graph = command.contains("--graph");

        List<Commit> list = new ArrayList<>(commits.values());
        Collections.reverse(list);

        StringBuilder out = new StringBuilder();

        for (Commit commit : list) {
            if (graph) out.append("* ");

            if (oneline) {
                out.append(commit.hash)
                    .append(" ");

                if (decorate) {
                    List<String> refs = refsForHash(commit.hash);
                    if (!refs.isEmpty()) {
                        out.append("(")
                            .append(String.join(", ", refs))
                            .append(") ");
                    }
                }

                out.append(commit.message);
            } else {
                out.append("commit ")
                    .append(padHash(commit.hash))
                    .append('\n')
                    .append("Author: ")
                    .append(gitConfig.getOrDefault("user.name", "Ubuntu Student"))
                    .append(" <")
                    .append(gitConfig.getOrDefault("user.email", "student@example.com"))
                    .append(">\n")
                    .append("Date:   ")
                    .append(new Date(commit.when))
                    .append("\n\n    ")
                    .append(commit.message);
            }

            out.append('\n');
        }

        return Result.normal(out.toString().trim());
    }

    private Result gitDiff(boolean cached) {
        StringBuilder out = new StringBuilder();

        if (cached) {
            for (String path : staged) {
                String before = headSnapshot.getOrDefault(path, "");
                String after = files.getOrDefault(repoRoot + "/" + path, "");

                out.append("diff --git a/")
                    .append(path)
                    .append(" b/")
                    .append(path)
                    .append("\n--- a/")
                    .append(path)
                    .append("\n+++ b/")
                    .append(path)
                    .append("\n-")
                    .append(firstLine(before))
                    .append("\n+")
                    .append(firstLine(after))
                    .append('\n');
            }
        } else {
            for (String path : repoFiles()) {
                if (staged.contains(path)) continue;

                String before = headSnapshot.get(path);
                String after = files.getOrDefault(repoRoot + "/" + path, "");

                if (before != null && !before.equals(after)) {
                    out.append("diff --git a/")
                        .append(path)
                        .append(" b/")
                        .append(path)
                        .append("\n-")
                        .append(firstLine(before))
                        .append("\n+")
                        .append(firstLine(after))
                        .append('\n');
                }
            }
        }

        return Result.normal(out.toString().trim());
    }

    private Result gitBranch(boolean all) {
        StringBuilder out = new StringBuilder();

        for (String name : branches.keySet()) {
            out.append(name.equals(headBranch) ? "* " : "  ")
                .append(name)
                .append('\n');
        }

        if (all) {
            for (String name : remoteBranches.keySet()) {
                out.append("  remotes/origin/")
                    .append(name)
                    .append('\n');
            }
        }

        return Result.normal(out.toString().trim());
    }

    private Result gitPull(String command) {
        if (remoteOrigin.isEmpty()) {
            return Result.error(
                "fatal: 'origin' does not appear to be a git repository"
            );
        }

        if (conflictOnNextPull) {
            String readme = repoRoot + "/README.md";

            conflictBackup = files.getOrDefault(readme, "");
            conflictFile = readme;

            files.put(
                readme,
                "<<<<<<< HEAD\n" +
                "LOCAL\n" +
                "=======\n" +
                "REMOTE\n" +
                ">>>>>>> origin/main\n"
            );

            conflictActive = true;
            conflictOnNextPull = false;

            return Result.error(
                "Auto-merging README.md\n" +
                "CONFLICT (content): Merge conflict in README.md\n" +
                "Automatic merge failed; fix conflicts and then commit the result."
            );
        }

        String remoteMain = remoteBranches.getOrDefault("main", originMain);
        String local = branches.getOrDefault(headBranch, "");

        if (remoteMain.isEmpty() || remoteMain.equals(local)) {
            originMain = local;
            remoteBranches.put("main", local);
            return Result.success("Déjà à jour.");
        }

        originMain = remoteMain;

        if (command.contains("--rebase")) {
            return Result.success(
                "Successfully rebased and updated refs/heads/" + headBranch + "."
            );
        }

        return Result.success("Fast-forward\nDéjà à jour avec origin/main.");
    }

    private Result gitPush(String command) {
        if (remoteOrigin.isEmpty()) {
            return Result.error(
                "fatal: No configured push destination."
            );
        }

        String n = normalize(command);

        if (n.startsWith("git push origin --delete ")) {
            String name = command.substring(
                "git push origin --delete ".length()
            ).trim();

            remoteBranches.remove(name);

            return Result.success(
                "- [deleted]         " + name
            );
        }

        String branch = headBranch;

        if (n.startsWith("git push -u origin ")) {
            branch = command.substring("git push -u origin ".length()).trim();
        } else if (n.startsWith("git push --set-upstream origin ")) {
            branch = command.substring(
                "git push --set-upstream origin ".length()
            ).trim();
        } else if (n.startsWith("git push --force-with-lease origin ")) {
            branch = command.substring(
                "git push --force-with-lease origin ".length()
            ).trim();
        } else if (n.startsWith("git push origin ")) {
            branch = command.substring("git push origin ".length()).trim();
        }

        String hash = branches.getOrDefault(branch, "");

        if (hash.isEmpty()) {
            return Result.error(
                "error: src refspec " + branch + " does not match any"
            );
        }

        remoteBranches.put(branch, hash);

        if ("main".equals(branch)) originMain = hash;

        return Result.success(
            "To " + remoteOrigin + "\n" +
            "   " + hash + " -> " + branch
        );
    }

    private Result echo(String command) {
        if (command.contains(">>") || command.contains(">")) {
            boolean append = command.contains(">>");
            String delimiter = append ? ">>" : ">";

            String[] parts = command.split(
                append ? ">>" : ">",
                2
            );

            if (parts.length != 2) {
                return Result.error("bash: erreur de redirection");
            }

            String text = stripQuotes(
                parts[0].substring(4).trim()
            );

            String path = resolve(parts[1].trim());
            ensureParentDirectories(path);

            if (append) {
                String old = files.getOrDefault(path, "");
                files.put(
                    path,
                    old + (old.isEmpty() ? "" : "\n") + text
                );
            } else {
                files.put(path, text);
            }

            return Result.normal("");
        }

        String value = stripQuotes(command.substring(5).trim());

        if (value.startsWith("$") && value.length() > 1) {
            String key = value.substring(1);
            if (environment.containsKey(key)) {
                value = environment.get(key);
            }
        }

        return Result.normal(value);
    }

    private Result copyFile(String argsText) {
        String[] args = argsText.split("\\s+");

        if (args.length < 2) {
            return Result.error("cp: opérande de fichier manquant");
        }

        String source = resolve(args[0]);
        String destination = resolve(args[1]);

        if (!files.containsKey(source)) {
            return Result.error(
                "cp: impossible d'évaluer '" + args[0] + "': Aucun fichier"
            );
        }

        ensureParentDirectories(destination);
        files.put(destination, files.get(source));

        return Result.normal("");
    }

    private Result copyDirectory(String argsText) {
        String[] args = argsText.split("\\s+");

        if (args.length < 2) {
            return Result.error("cp: opérande de fichier manquant");
        }

        String source = resolve(args[0]);
        String destination = resolve(args[1]);

        if (!directories.contains(source)) {
            return Result.error(
                "cp: impossible d'évaluer '" + args[0] + "': Aucun dossier"
            );
        }

        ensureDir(destination);

        for (String dir : new ArrayList<>(directories)) {
            if (dir.startsWith(source + "/")) {
                ensureDir(destination + dir.substring(source.length()));
            }
        }

        for (Map.Entry<String,String> file : new ArrayList<>(files.entrySet())) {
            if (file.getKey().startsWith(source + "/")) {
                String target = destination + file.getKey().substring(source.length());
                files.put(target, file.getValue());
            }
        }

        return Result.normal("");
    }

    private Result move(String argsText) {
        String[] args = argsText.split("\\s+");

        if (args.length < 2) {
            return Result.error("mv: opérande de fichier manquant");
        }

        String source = resolve(args[0]);
        String destination = resolve(args[1]);

        if (files.containsKey(source)) {
            String content = files.remove(source);
            ensureParentDirectories(destination);
            files.put(destination, content);
            return Result.normal("");
        }

        if (directories.contains(source)) {
            renameDirectoryTree(source, destination);
            return Result.normal("");
        }

        return Result.error(
            "mv: impossible d'évaluer '" + args[0] + "': Aucun fichier ou dossier"
        );
    }

    private Result removeRecursive(String targetText) {
        String target = resolve(targetText);

        if (files.containsKey(target)) {
            files.remove(target);
            return Result.normal("");
        }

        if (!directories.contains(target)) {
            return Result.error(
                "rm: impossible de supprimer '" + targetText +
                "': Aucun fichier ou dossier de ce type"
            );
        }

        directories.removeIf(
            path -> path.equals(target) || path.startsWith(target + "/")
        );

        files.keySet().removeIf(
            path -> path.startsWith(target + "/")
        );

        if (cwd.equals(target) || cwd.startsWith(target + "/")) {
            cwd = parent(target);
        }

        return Result.normal("");
    }

    private Result list(boolean longFormat, boolean showHidden) {
        List<FsEntry> entries = childrenOf(cwd, showHidden);

        if (!longFormat) {
            return Result.ls(entries, "");
        }

        StringBuilder prefix = new StringBuilder();

        if (showHidden) {
            prefix.append("drwxr-xr-x  .\n")
                .append("drwxr-xr-x  ..\n");
        }

        return Result.ls(entries, prefix.toString());
    }

    private Result listSsh() {
        List<FsEntry> entries = new ArrayList<>();

        if (sshPrivateKeyExists) {
            entries.add(new FsEntry("id_ed25519", false));
            entries.add(new FsEntry("id_ed25519.pub", false));
        }

        return Result.ls(entries, "");
    }

    private List<FsEntry> childrenOf(String directory, boolean showHidden) {
        List<FsEntry> entries = new ArrayList<>();
        String prefix = "/".equals(directory) ? "/" : directory + "/";

        for (String dir : directories) {
            if (dir.equals(directory) || "/".equals(dir)) continue;
            if (!parent(dir).equals(directory)) continue;

            String name = dir.substring(prefix.length());

            if (!showHidden && name.startsWith(".")) continue;

            entries.add(new FsEntry(name, true));
        }

        for (String file : files.keySet()) {
            if (!parent(file).equals(directory)) continue;

            String name = file.substring(prefix.length());

            if (!showHidden && name.startsWith(".")) continue;

            entries.add(new FsEntry(name, false));
        }

        entries.sort((a, b) ->
            a.name.compareToIgnoreCase(b.name)
        );

        return entries;
    }

    private Commit createCommit(String message) {
        Map<String,String> snapshot = new LinkedHashMap<>(headSnapshot);

        for (String path : staged) {
            String absolute = repoRoot + "/" + path;

            if (files.containsKey(absolute)) {
                snapshot.put(path, files.get(absolute));
            } else {
                snapshot.remove(path);
            }
        }

        String parent = branches.getOrDefault(headBranch, "");
        String hash = String.format(Locale.ROOT, "%07x", commitCounter++ * 7919);

        Commit commit = new Commit(
            hash,
            message,
            parent,
            headBranch,
            snapshot
        );

        commits.put(hash, commit);
        branches.put(headBranch, hash);
        headSnapshot = snapshot;
        staged.clear();

        return commit;
    }

    private void checkoutHeadSnapshot() {
        String hash = branches.getOrDefault(headBranch, "");

        if (hash.isEmpty()) {
            headSnapshot = new LinkedHashMap<>();
            return;
        }

        Commit commit = commits.get(hash);

        if (commit == null) return;

        for (String path : new ArrayList<>(files.keySet())) {
            if (path.startsWith(repoRoot + "/")) {
                files.remove(path);
            }
        }

        for (Map.Entry<String,String> entry : commit.snapshot.entrySet()) {
            files.put(
                repoRoot + "/" + entry.getKey(),
                entry.getValue()
            );
        }

        headSnapshot = new LinkedHashMap<>(commit.snapshot);
    }

    private Set<String> repoFiles() {
        Set<String> result = new LinkedHashSet<>();

        if (!gitInitialized || repoRoot.isEmpty()) return result;

        for (String path : files.keySet()) {
            if (path.startsWith(repoRoot + "/")) {
                result.add(relativeToRepo(path));
            }
        }

        return result;
    }

    private boolean inGitRepo() {
        return gitInitialized &&
            (cwd.equals(repoRoot) || cwd.startsWith(repoRoot + "/"));
    }

    private void ensureGitRepo(String root) {
        ensureDir(root);
        cwd = root;

        files.putIfAbsent(root + "/README.md", "# Projet\n");

        if (!gitInitialized || !root.equals(repoRoot)) {
            gitInitialized = true;
            repoRoot = root;
            headBranch = "main";
            branches.clear();
            commits.clear();
            staged.clear();
            headSnapshot = new LinkedHashMap<>();
            branches.put("main", "");
        }
    }

    private List<String> refsForHash(String hash) {
        List<String> refs = new ArrayList<>();

        for (Map.Entry<String,String> entry : branches.entrySet()) {
            if (hash.equals(entry.getValue())) {
                if (entry.getKey().equals(headBranch)) {
                    refs.add("HEAD -> " + entry.getKey());
                } else {
                    refs.add(entry.getKey());
                }
            }
        }

        if (hash.equals(originMain)) {
            refs.add("origin/main");
        }

        return refs;
    }

    private Commit findCommit(String ref) {
        if (ref == null || ref.trim().isEmpty()) return null;

        if ("HEAD".equalsIgnoreCase(ref)) {
            String head = branches.getOrDefault(headBranch, "");
            return commits.get(head);
        }

        if (commits.containsKey(ref)) return commits.get(ref);

        for (Commit commit : commits.values()) {
            if (commit.hash.startsWith(ref)) return commit;
        }

        String branchHash = branches.get(ref);
        if (branchHash != null) return commits.get(branchHash);

        if ("origin/main".equals(ref)) return commits.get(originMain);

        return null;
    }

    private String helpText() {
        return
            "Bash / fichiers:\n" +
            "  pwd, ls, ls -l, ls -la, cd, cd .., cd ~, mkdir, mkdir -p\n" +
            "  touch, echo, >, >>, cat, nano, mv, cp, cp -r, rm, rm -r\n" +
            "  rmdir, clear, history\n\n" +
            "Git / GitHub:\n" +
            "  git --version, git config, git clone, git init, git status\n" +
            "  git add, git add ., git commit -m, git log, git log -p\n" +
            "  git diff, git diff --staged, git branch, git branch -a\n" +
            "  git branch -M, git branch -d, git switch, git switch -c\n" +
            "  git remote -v, git remote get-url origin, git remote add origin\n" +
            "  git remote set-url origin, git remote remove origin\n" +
            "  git fetch, git fetch --all, git fetch --prune\n" +
            "  git pull origin main, git pull --rebase origin main\n" +
            "  git push, git push origin main, git push -u origin main\n" +
            "  git push origin --delete branche, git ls-remote origin\n" +
            "  git merge --abort, git log --graph --oneline --decorate --all\n\n" +
            "SSH:\n" +
            "  ls -al ~/.ssh, ssh-keygen -t ed25519 -C \"email\"\n" +
            "  eval \"$(ssh-agent -s)\", ssh-add ~/.ssh/id_ed25519\n" +
            "  cat ~/.ssh/id_ed25519.pub, cat -A ~/.ssh/id_ed25519.pub\n" +
            "  ssh-keygen -lf ~/.ssh/id_ed25519.pub, xclip, wl-copy\n" +
            "  ssh -T git@github.com\n\n" +
            "Commandes voisines ajoutées : git checkout, restore, reset, stash, tag, reflog, merge, cherry-pick, revert, clean, git rm, git mv, grep, find, head, tail, wc, stat, file, tar, curl, apt, systemctl, etc.\n\n" +
            "Catalogue extensif : " + CommandCatalog.count() + " signatures/exemples.\n" +
            "Utilise « help git », « help checkout », « apropos branch », « man checkout » ou « compgen -c ».\n\n" +
            "Le terminal est simulé : aucune commande arbitraire n'est exécutée sur Android.";
    }

    private String resolve(String raw) {
        String value = stripQuotes(raw.trim());

        if (value.isEmpty()) return cwd;
        if ("~".equals(value)) return "/home/ubuntu";

        if (value.startsWith("~/")) {
            value = "/home/ubuntu/" + value.substring(2);
        }

        String path;

        if (value.startsWith("/")) {
            path = value;
        } else {
            path = ("/".equals(cwd) ? "" : cwd) + "/" + value;
        }

        return normalizePath(path);
    }

    private String normalizePath(String path) {
        String[] pieces = path.split("/");
        List<String> stack = new ArrayList<>();

        for (String piece : pieces) {
            if (piece.isEmpty() || ".".equals(piece)) continue;

            if ("..".equals(piece)) {
                if (!stack.isEmpty()) stack.remove(stack.size() - 1);
            } else {
                stack.add(piece);
            }
        }

        return "/" + String.join("/", stack);
    }

    private String parent(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) return "/";

        int index = path.lastIndexOf('/');
        if (index <= 0) return "/";

        return path.substring(0, index);
    }

    private void ensureDir(String path) {
        String normalized = normalizePath(path);

        if ("/".equals(normalized)) {
            directories.add("/");
            return;
        }

        String[] parts = normalized.substring(1).split("/");
        String current = "";

        directories.add("/");

        for (String part : parts) {
            current += "/" + part;
            directories.add(current);
        }
    }

    private void ensureParentDirectories(String filePath) {
        ensureDir(parent(filePath));
    }

    private boolean hasChildren(String dir) {
        for (String candidate : directories) {
            if (!candidate.equals(dir) && parent(candidate).equals(dir)) return true;
        }

        for (String candidate : files.keySet()) {
            if (parent(candidate).equals(dir)) return true;
        }

        return false;
    }

    private void renameDirectoryTree(String source, String destination) {
        ensureDir(parent(destination));

        List<String> oldDirs = new ArrayList<>(directories);
        Map<String,String> oldFiles = new LinkedHashMap<>(files);

        directories.removeIf(
            path -> path.equals(source) || path.startsWith(source + "/")
        );

        files.keySet().removeIf(
            path -> path.startsWith(source + "/")
        );

        for (String dir : oldDirs) {
            if (dir.equals(source) || dir.startsWith(source + "/")) {
                directories.add(
                    destination + dir.substring(source.length())
                );
            }
        }

        for (Map.Entry<String,String> entry : oldFiles.entrySet()) {
            if (entry.getKey().startsWith(source + "/")) {
                files.put(
                    destination + entry.getKey().substring(source.length()),
                    entry.getValue()
                );
            }
        }
    }

    private String relativeToRepo(String absolute) {
        if (absolute == null || absolute.isEmpty()) return "";
        if (!absolute.startsWith(repoRoot)) return absolute;

        String relative = absolute.substring(repoRoot.length());
        if (relative.startsWith("/")) relative = relative.substring(1);

        return relative;
    }

    private String repoNameFromUrl(String url) {
        String clean = url;

        if (clean.endsWith(".git")) {
            clean = clean.substring(0, clean.length() - 4);
        }

        int slash = clean.lastIndexOf('/');
        int colon = clean.lastIndexOf(':');
        int cut = Math.max(slash, colon);

        if (cut >= 0 && cut + 1 < clean.length()) {
            return clean.substring(cut + 1);
        }

        return "repo";
    }

    private String padHash(String hash) {
        if (hash == null || hash.isEmpty()) return "0000000000000000000000000000000000000000";

        StringBuilder out = new StringBuilder(hash);
        while (out.length() < 40) out.append('0');

        return out.substring(0, 40);
    }

    private String firstLine(String value) {
        if (value == null || value.isEmpty()) return "";

        int newline = value.indexOf('\n');
        return newline < 0 ? value : value.substring(0, newline);
    }

    private String stripQuotes(String value) {
        if (value == null) return "";

        String trimmed = value.trim();

        if (trimmed.length() >= 2 &&
            ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
             (trimmed.startsWith("'") && trimmed.endsWith("'")))) {

            return trimmed.substring(1, trimmed.length() - 1);
        }

        return trimmed;
    }

    private String normalize(String value) {
        return value
            .trim()
            .replaceAll("\\s+", " ")
            .toLowerCase(Locale.ROOT);
    }
}
