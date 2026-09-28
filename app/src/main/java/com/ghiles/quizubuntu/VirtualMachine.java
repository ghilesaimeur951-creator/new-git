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
        public String permissions;

        FsEntry(String name, boolean directory) {
            this.name = name;
            this.directory = directory;
            this.permissions = directory ? "drwxr-xr-x" : "-rw-r--r--";
        }
    }

    public static final class Result {
        public final Kind kind;
        public final String text;
        public final List<FsEntry> entries;
        public final String editPath;
        public final int exitCode;
        public final String stdout;
        public final String stderr;

        private Result(Kind kind, String text, List<FsEntry> entries, String editPath) {
            this(kind, text, entries, editPath, kind == Kind.ERROR ? 1 : 0);
        }

        private Result(Kind kind, String text, List<FsEntry> entries, String editPath, int exitCode) {
            this(kind, kind == Kind.ERROR ? "" : text, kind == Kind.ERROR ? text : "", entries, editPath, exitCode);
        }

        private Result(Kind kind, String stdout, String stderr, List<FsEntry> entries, String editPath, int exitCode) {
            this.exitCode = exitCode;
            this.kind = kind;
            this.stdout = stdout == null ? "" : stdout;
            this.stderr = stderr == null ? "" : stderr;
            this.text = this.stderr + this.stdout;
            this.entries = entries == null ? Collections.emptyList() : entries;
            this.editPath = editPath;
        }

        public static Result status(String text, int code) {
            return new Result(Kind.NORMAL, text, null, null, code);
        }
        public Result withStatus(int code) { return new Result(kind, stdout, stderr, entries, editPath, code); }

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

    private final List<String> directoryStack = new ArrayList<>();
    private final Map<String,String> permissionModes = new LinkedHashMap<>();
    private final Map<String,String> owners = new LinkedHashMap<>();
    private final Map<String,String> groups = new LinkedHashMap<>();
    private final Map<String,String> symbolicLinks = new LinkedHashMap<>();
    private final Map<String,Map<String,String>> archives = new LinkedHashMap<>();
    private String umask = "0022";

    private final Set<String> installedPackages = new LinkedHashSet<>(java.util.Arrays.asList("git", "bash", "coreutils", "openssh-client"));
    private final Map<String,Boolean> serviceRunning = new LinkedHashMap<>();
    private final Set<String> enabledServices = new LinkedHashSet<>();
    private final Map<Integer,String> processes = new LinkedHashMap<>();

    private String cwd = "/home/ubuntu";
    private String previousCwd = "/home/ubuntu";

    private boolean gitInitialized = false;
    private String repoRoot = "";
    private String headBranch = "main";
    private String previousBranch = "main";
    private String detachedHead;
    private final Map<String,String> branches = new LinkedHashMap<>();
    private final Map<String,String> tags = new LinkedHashMap<>();
    private final Map<String,Commit> commits = new LinkedHashMap<>();
    private final Map<String,String> indexSnapshot = new LinkedHashMap<>();
    private final Set<String> staged = new LinkedHashSet<String>() {
        @Override public boolean add(String path) {
            indexSnapshot.put(path, files.get(repoRoot + "/" + path));
            return super.add(path);
        }
        @Override public boolean remove(Object path) {
            indexSnapshot.remove(path);
            return super.remove(path);
        }
        @Override public void clear() { indexSnapshot.clear(); super.clear(); }
    };
    private Map<String,String> headSnapshot = new LinkedHashMap<>();
    private int commitCounter = 1;

    private String remoteOrigin = "";
    private String remoteName = "origin";
    private final Map<String,String> remoteBranches = new LinkedHashMap<>();
    private String originMain = "";

    private boolean conflictActive = false;
    private boolean mergePending = false;
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
        installedPackages.clear(); installedPackages.addAll(java.util.Arrays.asList("git", "bash", "coreutils", "openssh-client"));
        serviceRunning.clear(); serviceRunning.put("ssh", true); enabledServices.clear(); enabledServices.add("ssh");
        processes.clear(); processes.put(1234,"bash");
        directoryStack.clear(); permissionModes.clear(); owners.clear(); groups.clear(); symbolicLinks.clear(); archives.clear(); umask = "0022";
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
        detachedHead = null;
        previousBranch = "main";
        branches.clear();
        tags.clear();
        commits.clear();
        staged.clear();
        headSnapshot = new LinkedHashMap<>();
        commitCounter = 1;

        remoteOrigin = "";
        remoteBranches.clear();
        originMain = "";

        conflictActive = false;
        mergePending = false;
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

    private int executionDepth;
    private String standardInput;
    private int lastExitCode;

    public Result execute(String raw) {
        if (executionDepth >= 32) return Result.error("Expansion récursive : alias ou commande trop profonde.");
        if (executionDepth == 0 && raw != null && !raw.trim().isEmpty()) commandHistory.add(raw.trim());
        executionDepth++;
        try {
            Result result = executeInternal(raw);
            lastExitCode = result.exitCode;
            return result;
        }
        catch (IllegalArgumentException ex) { lastExitCode = 1; return Result.error(ex.getMessage()); }
        catch (IndexOutOfBoundsException ex) { lastExitCode = 1; return Result.error("Arguments incomplets : consulte help pour la syntaxe."); }
        finally { executionDepth--; }
    }

    private Result executeInternal(String raw) {
        String command = raw == null ? "" : raw.trim();

        if (command.isEmpty()) return Result.normal("");

        List<String> sequence = ShellSyntax.operators(command, ";");
        if (sequence.size() > 1) return executeSequence(sequence, false);
        List<String> conditional = ShellSyntax.operators(command, "&&", "||");
        if (conditional.size() > 1) return executeSequence(conditional, true);
        List<String> pipeline = ShellSyntax.operators(command, "|");
        if (pipeline.size() > 1) {
            String oldInput = standardInput;
            Result result = Result.normal("");
            StringBuilder errors = new StringBuilder();
            try {
                for (int i = 0; i < pipeline.size(); i += 2) {
                    if (pipeline.get(i).trim().isEmpty()) return Result.error("bash: pipeline incomplet");
                    if (i > 0) standardInput = outputOf(result);
                    result = execute(pipeline.get(i));
                    errors.append(result.stderr);
                    if (result.kind == Kind.EDIT || result.kind == Kind.CLEAR) return Result.error("Cette commande interactive ne peut pas être utilisée dans un pipeline.");
                }
                return new Result(result.kind, outputOf(result), errors.toString(), null, null, result.exitCode);
            } finally { standardInput = oldInput; }
        }
        // Preserve the special ssh-agent expression; no shell substitution is performed.
        if (!command.startsWith("eval ")) {
            List<String> redirects = ShellSyntax.operators(command, ">>", ">", "<");
            if (redirects.size() > 1) return executeRedirected(redirects);
        }
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

        if ("ctrl+c".equals(n) || "^c".equals(n)) return Result.normal("^C");
        if (command.startsWith(">")) {
            List<String> target = ShellSyntax.words(command.substring(1));
            if (target.size() != 1) return Result.error("bash: redirection invalide");
            String path = resolve(target.get(0));
            if (!directories.contains(parent(path))) return Result.error("bash: dossier parent absent");
            files.put(path, "");
            return Result.normal("");
        }
        if ("clear".equals(n)) return Result.clear();

        if ("id -u".equals(n) || "id -g".equals(n)) return Result.normal("1000\n");
        if ("date +%f".equals(n)) return Result.normal(new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date())+"\n");
        if ("history".equals(n) || n.startsWith("history ")) {
            StringBuilder out = new StringBuilder();
            int count = n.equals("history") ? commandHistory.size() : Integer.parseInt(command.substring(8).trim());
            if(count<0) return Result.error("history: nombre positif requis");
            for (int i = Math.max(0,commandHistory.size()-count); i < commandHistory.size(); i++) {
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

        List<String> words = ShellSyntax.words(command);
        Result archive = executeArchive(words);
        if (archive != null) return archive;
        Result system = executeVirtualSystem(words);
        if (system != null) return system;
        Result filter = executeFilter(words);
        if (filter != null) return filter;
        Result filesystem = executeFileCommand(words);
        if (filesystem != null) return filesystem;
        if (!words.isEmpty() && TextCommands.handles(words.get(0)) && !command.contains("~/.ssh/")) {
            return TextCommands.run(words, path -> readVirtual(path), standardInput);
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

        if ("ls".equals(n) || n.startsWith("ls ")) {
            boolean all = ShellSyntax.hasShortOption(command, 'a');
            boolean almostAll = ShellSyntax.hasShortOption(command, 'A');
            List<String> lsWords = ShellSyntax.words(command);
            String target = cwd;
            boolean optionsEnded = false;
            for (int i = 1; i < lsWords.size(); i++) {
                String arg = lsWords.get(i);
                if (arg.equals("--")) { optionsEnded = true; continue; }
                if (!optionsEnded && arg.startsWith("-")) continue;
                target = resolve(arg);
            }
            if (!directories.contains(target)) return Result.error("ls: " + target + ": Aucun dossier");
            return list(target, all || almostAll, all);
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

        if ("echo".equals(n) || n.startsWith("echo ")) {
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
            n.startsWith("ssh ") ||
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

    private String outputOf(Result result) {
        if (result.kind != Kind.LS) return result.stdout;
        StringBuilder out = new StringBuilder(result.stdout);
        for (FsEntry entry : result.entries) out.append(entry.name).append('\n');
        return out.toString();
    }

    private Result executeSequence(List<String> parts, boolean conditional) {
        Result result = Result.normal("");
        StringBuilder out = new StringBuilder();
        StringBuilder errors = new StringBuilder();
        for (int i = 0; i < parts.size(); i += 2) {
            String part = parts.get(i).trim();
            if (part.isEmpty()) {
                if (i == parts.size()-1 && !conditional) break;
                return Result.error("bash: opérateur sans commande");
            }
            if (i > 0 && conditional) {
                String operator = parts.get(i-1);
                if (operator.equals("&&") && result.exitCode != 0) continue;
                if (operator.equals("||") && result.exitCode == 0) continue;
            }
            result = execute(part);
            if (result.kind == Kind.EDIT || result.kind == Kind.CLEAR) return result;
            String value = outputOf(result);
            out.append(value);
            errors.append(result.stderr);
        }
        return new Result(result.kind == Kind.ERROR ? Kind.ERROR : Kind.NORMAL, out.toString(), errors.toString(), null, null, result.exitCode);
    }

    private Result executeRedirected(List<String> parts) {
        String oldInput = standardInput;
        String outputPath = null;
        boolean append = false;
        try {
            for (int i = 1; i < parts.size(); i += 2) {
                List<String> paths = ShellSyntax.words(parts.get(i+1));
                if (paths.size() != 1) return Result.error("bash: redirection ambiguë ou cible manquante");
                String path = resolve(paths.get(0));
                if (parts.get(i).equals("<")) {
                    if (!files.containsKey(path)) return Result.error("bash: fichier d'entrée absent");
                    standardInput = files.get(path);
                } else {
                    if (!directories.contains(parent(path)) || directories.contains(path)) return Result.error("bash: destination invalide");
                    outputPath = path;
                    append = parts.get(i).equals(">>");
                    if (!append || !files.containsKey(path)) files.put(path, "");
                }
            }
            Result result = parts.get(0).trim().isEmpty() ? Result.normal("") : execute(parts.get(0));
            if (outputPath == null) return result;
            files.put(outputPath, (append ? files.get(outputPath) : "") + outputOf(result));
            return new Result(result.kind, "", result.stderr, null, null, result.exitCode);
        } finally { standardInput = oldInput; }
    }

    private Result executeArchive(List<String> args) {
        if(args.isEmpty() || !java.util.Arrays.asList("tar","gzip","gunzip","zip","unzip").contains(args.get(0))) return null;
        String name=args.get(0),archive=null; int sourceStart=0;
        boolean create=false,extract=false,list=false,compressed=false;
        if(name.equals("tar")) {
            if(args.size()<3) return Result.error("tar: options et archive requises");
            String flags=args.get(1); create=flags.contains("c"); extract=flags.contains("x"); list=flags.contains("t");
            if((create?1:0)+(extract?1:0)+(list?1:0)!=1 || !flags.contains("f")) return Result.error("tar: utiliser -cf, -tf ou -xf");
            archive=resolve(args.get(2)); sourceStart=3;
        } else if(name.equals("zip")) {
            int index=args.size()>1&&args.get(1).startsWith("-")?2:1;
            if(args.size()<=index) return Result.error("zip: archive requise");
            archive=resolve(args.get(index)); sourceStart=index+1; create=true;
        } else if(name.equals("unzip")) {
            list=args.contains("-l"); extract=!list;
            if(args.size()<(list?3:2)) return Result.error("unzip: archive requise");
            archive=resolve(args.get(list?2:1));
        } else {
            extract=name.equals("gunzip")||args.contains("-d"); create=!extract; compressed=true;
            if(args.size()<2) return Result.error(name+": fichier requis");
            archive=resolve(args.get(args.size()-1));
            if(create) { sourceStart=args.size()-1; archive += ".gz"; }
        }
        if(create) {
            if(sourceStart>=args.size()) return Result.error(name+": source requise");
            if(!directories.contains(parent(archive))) return Result.error(name+": dossier archive absent");
            Map<String,String> snapshot=new LinkedHashMap<>();
            for(int i=sourceStart;i<args.size();i++) {
                String source=resolve(args.get(i));
                if(files.containsKey(source)) snapshot.put(baseName(source),files.get(source));
                else if(directories.contains(source)) {
                    snapshot.put(baseName(source)+"/",null);
                    for(String dir:directories) if(dir.startsWith(source+"/")) snapshot.put(baseName(source)+dir.substring(source.length())+"/",null);
                    for(String file:files.keySet()) if(file.startsWith(source+"/")) snapshot.put(baseName(source)+file.substring(source.length()),files.get(file));
                } else return Result.error(name+": source absente : "+args.get(i));
            }
            archives.put(archive,snapshot);
            files.put(archive,"Archive virtuelle Ubuntu Lab : "+String.join(", ",snapshot.keySet())+"\n");
            if(compressed) files.remove(resolve(args.get(sourceStart)));
            return Result.normal("");
        }
        if(!files.containsKey(archive)||!archives.containsKey(archive)) return Result.error(name+": archive virtuelle absente ou format externe non pris en charge");
        Map<String,String> snapshot=archives.get(archive);
        if(list) return Result.normal(String.join("\n",snapshot.keySet())+"\n");
        if(extract && compressed) {
            if(!archive.endsWith(".gz")) return Result.error(name+": suffixe .gz requis");
            String destination=archive.substring(0,archive.length()-3);
            files.put(destination,snapshot.values().iterator().next()); files.remove(archive); archives.remove(archive);
            return Result.normal("");
        }
        if(extract) {
            for(Map.Entry<String,String> entry:snapshot.entrySet()) {
                String path=resolve(entry.getKey());
                if(entry.getValue()==null) ensureDir(path); else { ensureParentDirectories(path); files.put(path,entry.getValue()); }
            }
            if(compressed) { files.remove(archive); archives.remove(archive); }
        }
        return Result.normal("");
    }

    private Result executeVirtualSystem(List<String> args) {
        if(args.isEmpty()) return null;
        String name=args.get(0);
        if(name.equals("apt")||name.equals("dpkg")) {
            String action=args.size()>1?args.get(1):"";
            String target=args.size()>2?args.get(args.size()-1):"";
            if(action.equals("install")) { if(target.isEmpty()) return Result.error("apt: paquet manquant"); installedPackages.add(target); return Result.success("Paquet installé dans le modèle virtuel : "+target); }
            if(action.equals("remove")) { installedPackages.remove(target); return Result.success("Paquet retiré du modèle virtuel : "+target); }
            if(action.equals("list")||action.equals("-l")) return Result.normal(String.join("\n",installedPackages)+"\n");
            if(action.equals("search")) return Result.normal(installedPackages.stream().filter(p->p.contains(target)).collect(java.util.stream.Collectors.joining("\n"))+"\n");
            if(action.equals("show")||action.equals("-s")) return Result.normal("Package: "+target+"\nStatus: "+(installedPackages.contains(target)?"install ok installed":"not-installed")+"\n");
            if(action.equals("-L")) return installedPackages.contains(target)?Result.normal("/usr/bin/"+target+"\n"):Result.error("dpkg: paquet non installé");
            if(action.equals("--version")) return Result.normal("apt 2.7 (modèle Ubuntu Lab)\n");
            if(java.util.Arrays.asList("update","upgrade","autoremove").contains(action)) return Result.normal("Modèle virtuel : aucun téléchargement ni paquet système Android modifié.\n");
            return Result.error(name+": opération non prise en charge");
        }
        if(name.equals("systemctl")) {
            String action=args.size()>1?args.get(1):"list-units";
            String service=args.size()>2?args.get(2).replace(".service",""):"ssh";
            if(action.equals("list-units")||action.equals("list-unit-files")) { StringBuilder out=new StringBuilder(); for(String key:serviceRunning.keySet())out.append(key).append(".service ").append(serviceRunning.get(key)?"active":"inactive").append('\n'); return Result.normal(out.toString()); }
            if(!serviceRunning.containsKey(service)) return Result.error("Unit "+service+".service not found (modèle virtuel)");
            if(action.equals("start")||action.equals("restart")) serviceRunning.put(service,true);
            else if(action.equals("stop")) serviceRunning.put(service,false);
            else if(action.equals("enable")) enabledServices.add(service);
            else if(action.equals("disable")) enabledServices.remove(service);
            else if(action.equals("is-enabled")) return Result.status(enabledServices.contains(service)?"enabled\n":"disabled\n",enabledServices.contains(service)?0:1);
            else if(action.equals("status")||action.equals("is-active")) return Result.status(serviceRunning.get(service)?"active\n":"inactive\n",serviceRunning.get(service)?0:3);
            else if(!action.equals("reload")) return Result.error("systemctl: opération non prise en charge");
            return Result.normal("");
        }
        if(name.equals("ps")) { StringBuilder out=new StringBuilder("  PID CMD\n"); for(Map.Entry<Integer,String> p:processes.entrySet())out.append(p.getKey()).append(' ').append(p.getValue()).append('\n'); return Result.normal(out.toString()); }
        if(name.equals("pgrep")||name.equals("pidof")) {
            String pattern=args.get(args.size()-1); StringBuilder out=new StringBuilder();
            for(Map.Entry<Integer,String> p:processes.entrySet()) if(p.getValue().contains(pattern)) out.append(p.getKey()).append(args.contains("-a")?" "+p.getValue():"").append('\n');
            return Result.status(out.toString(),out.length()==0?1:0);
        }
        if(name.equals("kill")) {
            try { int id=Integer.parseInt(args.get(args.size()-1)); return processes.remove(id)!=null?Result.normal(""):Result.error("kill: aucun processus "+id); }
            catch(RuntimeException e) { return Result.error("kill: PID manquant ou invalide"); }
        }
        if(name.equals("pkill")) { String target=args.get(args.size()-1); boolean removed=processes.values().removeIf(p->p.contains(target)); return Result.status("",removed?0:1); }
        if(name.equals("jobs")||name.equals("bg")||name.equals("fg")) return name.equals("jobs")?Result.normal(""):Result.error(name+": aucun job suspendu");
        if(name.equals("nice")) {
            int start=args.size()>2&&args.get(1).equals("-n")?3:1;
            if(start>=args.size()) return Result.error("nice: commande manquante");
            return execute(String.join(" ",args.subList(start,args.size())));
        }
        if(name.equals("renice")) {
            try { int id=Integer.parseInt(args.get(args.size()-1)); return processes.containsKey(id)?Result.normal("Priorité virtuelle ajustée pour "+id+"\n"):Result.error("renice: processus absent"); }
            catch(RuntimeException e) { return Result.error("renice: PID invalide"); }
        }
        return null;
    }

    private Result executeFilter(List<String> words) {
        if(words.isEmpty()) return null;
        String name=words.get(0);
        if(name.equals("printf")) return Result.normal(TextFilters.printf(words));
        if(name.equals("tee")) {
            String input=standardInput==null?"":standardInput;
            for(int i=1;i<words.size();i++) {
                if(words.get(i).equals("-a")) continue;
                String path=resolve(words.get(i));
                if(!directories.contains(parent(path))) return Result.error("tee: dossier parent absent");
                files.put(path,(words.contains("-a")?files.getOrDefault(path,""):"")+input);
            }
            return Result.normal(input);
        }
        if(name.matches("(?:md5sum|sha1sum|sha256sum|sha512sum|cksum)")) {
            if(words.size()!=2) return Result.error(name+": fichier requis");
            String value=readVirtual(words.get(1));
            if(value==null) return Result.error(name+": fichier absent");
            byte[] data=value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if(name.equals("cksum")) {
                int crc=0;
                for(byte octet:data) crc=crcStep(crc,octet&255);
                for(long size=data.length;size!=0;size>>>=8) crc=crcStep(crc,(int)(size&255));
                return Result.normal(Integer.toUnsignedLong(~crc)+" "+data.length+" "+words.get(1)+"\n");
            }
            try {
                String algorithm=name.equals("md5sum")?"MD5":name.equals("sha1sum")?"SHA-1":name.equals("sha256sum")?"SHA-256":"SHA-512";
                byte[] digest=java.security.MessageDigest.getInstance(algorithm).digest(data);
                StringBuilder hash=new StringBuilder(); for(byte octet:digest) hash.append(String.format(Locale.ROOT,"%02x",octet&255));
                return Result.normal(hash+"  "+words.get(1)+"\n");
            } catch(java.security.NoSuchAlgorithmException error) { return Result.error(name+": algorithme indisponible"); }
        }
        if(!java.util.Arrays.asList("tac","less","more","uniq","cut","tr","sed","awk").contains(name)) return null;
        String input=standardInput==null?"":standardInput;
        if(!name.equals("tr")&&words.size()>1) {
            String last=words.get(words.size()-1);
            boolean operand=!last.startsWith("-")&&!((name.equals("sed")||name.equals("awk"))&&words.size()<=(words.contains("-n")?3:2));
            if(operand) { input=readVirtual(last); if(input==null) return Result.error(name+": fichier absent"); }
        }
        return Result.normal(TextFilters.transform(name,words,input));
    }

    private int crcStep(int crc,int value) {
        crc ^= value<<24;
        for(int bit=0;bit<8;bit++) crc=(crc&0x80000000)!=0?(crc<<1)^0x04c11db7:crc<<1;
        return crc;
    }

    private String readVirtual(String name) {
        String path = resolve(name);
        Set<String> visited = new LinkedHashSet<>();
        while (symbolicLinks.containsKey(path)) {
            if (!visited.add(path)) throw new IllegalArgumentException("Lien symbolique circulaire");
            String destination = symbolicLinks.get(path);
            path = resolve(destination.startsWith("/") ? destination : parent(path) + "/" + destination);
        }
        return files.get(path);
    }

    private Result executeFileCommand(List<String> words) {
        if (words.isEmpty()) return null;
        String name = words.get(0);
        if (!java.util.Arrays.asList("mkdir","rmdir","touch","cp","mv","rm","pushd","popd","dirs","chmod","chown","chgrp","umask","stat","readlink","ln").contains(name)) return null;
        List<String> args = new ArrayList<>();
        String flags = "";
        boolean options = true;
        for (int i = 1; i < words.size(); i++) {
            String word = words.get(i);
            if (word.equals("--") && options) { options = false; continue; }
            if (options && word.startsWith("-")) flags += word.substring(1);
            else args.add(word);
        }
        if (name.equals("dirs")) return Result.normal(cwd + (directoryStack.isEmpty() ? "" : " " + String.join(" ", directoryStack)) + "\n");
        if (name.equals("popd")) {
            if (directoryStack.isEmpty()) return Result.error("popd: pile de dossiers vide");
            previousCwd = cwd; cwd = directoryStack.remove(0); environment.put("PWD", cwd);
            return Result.normal(cwd + "\n");
        }
        if (name.equals("umask")) {
            if (args.isEmpty()) return Result.normal(umask + "\n");
            if (!args.get(0).matches("[0-7]{3,4}")) return Result.error("umask: masque octal invalide");
            umask = String.format(Locale.ROOT, "%04o", Integer.parseInt(args.get(0),8));
            return Result.normal("");
        }
        if (args.isEmpty()) return Result.error(name + ": opérande manquant");
        if (name.equals("pushd")) {
            String path = resolve(args.get(0));
            if (!directories.contains(path)) return Result.error("pushd: dossier absent");
            directoryStack.add(0,cwd); previousCwd = cwd; cwd = path; environment.put("PWD",cwd);
            return Result.normal(cwd + " " + String.join(" ",directoryStack) + "\n");
        }
        if (name.equals("stat")) {
            String path = resolve(args.get(0));
            if (!files.containsKey(path) && !directories.contains(path)) return Result.error("stat: fichier absent");
            return Result.normal("  Fichier : " + args.get(0) + "\n  Taille : " + (directories.contains(path) ? 0 : files.get(path).getBytes(java.nio.charset.StandardCharsets.UTF_8).length) +
                "\n  Accès : (0" + permissionModes.getOrDefault(path,directories.contains(path)?"755":"644") + ")" +
                "\n  Propriétaire : " + owners.getOrDefault(path,"ubuntu") + "\n  Groupe : " + groups.getOrDefault(path,"ubuntu") + "\n");
        }
        if (name.equals("readlink")) {
            String path = resolve(args.get(0));
            if (flags.contains("f")) {
                Set<String> visited = new LinkedHashSet<>();
                while (symbolicLinks.containsKey(path)) {
                    if (!visited.add(path)) return Result.error("readlink: lien circulaire");
                    String next = symbolicLinks.get(path); path = resolve(next.startsWith("/") ? next : parent(path)+"/"+next);
                }
                return Result.normal(path + "\n");
            }
            return symbolicLinks.containsKey(path) ? Result.normal(symbolicLinks.get(path)+"\n") : Result.status("",1);
        }
        if (name.equals("chmod") || name.equals("chown") || name.equals("chgrp")) {
            if (args.size()<2) return Result.error(name + ": fichier manquant");
            String value = args.get(0);
            for (String target : args.subList(1,args.size())) {
                String path = resolve(target);
                if (!files.containsKey(path) && !directories.contains(path)) return Result.error(name+": fichier absent");
                Set<String> targets = new LinkedHashSet<>(); targets.add(path);
                if (flags.contains("R")) { for (String f:files.keySet()) if(f.startsWith(path+"/")) targets.add(f); for(String d:directories) if(d.startsWith(path+"/")) targets.add(d); }
                for (String item:targets) {
                    if(name.equals("chmod")) {
                        int mode = Integer.parseInt(permissionModes.getOrDefault(item,directories.contains(item)?"755":"644"),8);
                        if(value.matches("[0-7]{3,4}")) mode = Integer.parseInt(value,8);
                        else if(value.equals("u+x")) mode |= 0100;
                        else return Result.error("chmod: mode non pris en charge");
                        permissionModes.put(item,Integer.toOctalString(mode));
                    } else if(name.equals("chgrp")) groups.put(item,value);
                    else { String[] split=value.split(":",2); owners.put(item,split[0]); if(split.length>1) groups.put(item,split[1]); }
                }
            }
            return Result.normal("");
        }
        if (name.equals("cp") || name.equals("mv") || name.equals("ln")) {
            if(args.size()!=2) return Result.error(name+": indique une source et une destination");
            String source=resolve(args.get(0)), target=resolve(args.get(1));
            if(directories.contains(target)) target += "/"+baseName(source);
            if(!directories.contains(parent(target))) return Result.error(name+": dossier destination absent");
            if(flags.contains("i") && (files.containsKey(target)||directories.contains(target))) return Result.error(name+": destination existante ; retire -i après vérification pour remplacer");
            if(name.equals("ln") && flags.contains("s")) {
                if((files.containsKey(target)||directories.contains(target))&&!flags.contains("f")) return Result.error("ln: destination existante");
                symbolicLinks.put(target,args.get(0)); files.put(target,""); return Result.normal("");
            }
            if(name.equals("ln")) return Result.error("ln: liens physiques non pris en charge ; utiliser ln -s");
            if(!files.containsKey(source)&&!directories.contains(source)) return Result.error(name+": source absente");
            if(source.equals(target)) return Result.error(name+": source et destination identiques");
            if(directories.contains(source)) {
                if(target.startsWith(source+"/")) return Result.error(name+": copie récursive dans elle-même interdite");
                if(name.equals("cp") && !flags.contains("r")&&!flags.contains("R")&&!flags.contains("a")) return Result.error("cp: -r requis pour un dossier");
                ensureDir(target);
                for(String d:new ArrayList<>(directories)) if(d.startsWith(source+"/")) ensureDir(target+d.substring(source.length()));
                for(String f:new ArrayList<>(files.keySet())) if(f.startsWith(source+"/")) files.put(target+f.substring(source.length()),files.get(f));
                if(name.equals("mv")) removeRecursive(args.get(0));
            } else {
                files.put(target,readVirtual(source));
                if(archives.containsKey(source)) archives.put(target,new LinkedHashMap<>(archives.get(source)));
                if(name.equals("mv")) { files.remove(source); symbolicLinks.remove(source); }
            }
            return Result.normal(flags.contains("v") ? "'"+args.get(0)+"' -> '"+args.get(1)+"'\n" : "");
        }
        StringBuilder output=new StringBuilder();
        for(String arg:args) {
            String path=resolve(arg);
            if(name.equals("mkdir")) {
                if(files.containsKey(path)||directories.contains(path)&&!flags.contains("p")) return Result.error("mkdir: le chemin existe déjà");
                if(!directories.contains(parent(path))&&!flags.contains("p")) return Result.error("mkdir: dossier parent absent");
                ensureDir(path);
                permissionModes.put(path,Integer.toOctalString(0777 & ~Integer.parseInt(umask,8)));
                if(flags.contains("v")) output.append("mkdir: dossier créé '").append(arg).append("'\n");
            } else if(name.equals("touch")) {
                if(!directories.contains(parent(path))) return Result.error("touch: dossier parent absent");
                if(!directories.contains(path)) { files.putIfAbsent(path,""); permissionModes.putIfAbsent(path,Integer.toOctalString(0666 & ~Integer.parseInt(umask,8))); }
            } else if(name.equals("rm")) {
                if(!files.containsKey(path)&&!directories.contains(path)) { if(flags.contains("f")) continue; return Result.error("rm: Aucun fichier ou dossier"); }
                if(flags.contains("i")) return Result.error("rm: confirmation interactive requise ; retire -i après vérification");
                if(directories.contains(path)) { if(!flags.contains("r")&&!flags.contains("R")) return Result.error("rm: est un dossier, option -r requise"); removeRecursive(arg); }
                else { files.remove(path); symbolicLinks.remove(path); }
                if(flags.contains("v")) output.append("supprimé '").append(arg).append("'\n");
            } else if(name.equals("rmdir")) {
                do {
                    if(!directories.contains(path)) return Result.error("rmdir: dossier absent");
                    if(!childrenOf(path,true).isEmpty()) return Result.error("rmdir: dossier non vide");
                    directories.remove(path); path=parent(path);
                } while(flags.contains("p")&&!path.equals(cwd)&&path.startsWith(cwd+"/"));
            }
        }
        return Result.normal(output.toString());
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

        if (n.equals("find") || n.startsWith("find ")) {
            List<String> args=ShellSyntax.words(command);
            String start=args.size()>1&&!args.get(1).startsWith("-")?args.get(1):".";
            String root=resolve(start),pattern=null,type=null; boolean ignoreCase=false,empty=false;
            int minimum=0,maximum=Integer.MAX_VALUE; String size=null,permissions=null;
            for(int i=start.equals(".")&&args.size()>1&&args.get(1).startsWith("-")?1:2;i<args.size();i++) {
                String flag=args.get(i);
                if(flag.equals("-type")) type=args.get(++i);
                else if(flag.equals("-name")||flag.equals("-iname")) { ignoreCase=flag.equals("-iname"); pattern=args.get(++i); }
                else if(flag.equals("-maxdepth")) maximum=Integer.parseInt(args.get(++i));
                else if(flag.equals("-mindepth")) minimum=Integer.parseInt(args.get(++i));
                else if(flag.equals("-empty")) empty=true;
                else if(flag.equals("-size")) size=args.get(++i);
                else if(flag.equals("-perm")) permissions=args.get(++i);
                else return Result.error("find: filtre non pris en charge : "+flag);
            }
            if(!directories.contains(root)&&!files.containsKey(root)) return Result.error("find: chemin absent");
            Set<String> paths=new LinkedHashSet<>(); paths.addAll(directories); paths.addAll(files.keySet());
            StringBuilder out=new StringBuilder();
            for(String path:paths) {
                if(!path.equals(root)&&!path.startsWith(root+"/")) continue;
                String relative=path.equals(root)?"":path.substring(root.length()+1);
                int depth=relative.isEmpty()?0:relative.split("/").length;
                if(depth<minimum||depth>maximum) continue;
                boolean directory=directories.contains(path);
                if(type!=null&&((type.equals("f")&&directory)||(type.equals("d")&&!directory))) continue;
                if(pattern!=null&&!simpleGlob(ignoreCase?pattern.toLowerCase(Locale.ROOT):pattern,ignoreCase?baseName(path).toLowerCase(Locale.ROOT):baseName(path))) continue;
                if(empty&&(directory?!childrenOf(path,true).isEmpty():!files.get(path).isEmpty())) continue;
                if(permissions!=null&&!permissionModes.getOrDefault(path,directory?"755":"644").equals(permissions)) continue;
                if(size!=null) {
                    java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("([+-]?)([0-9]+)([ck]?)").matcher(size);
                    if(!matcher.matches()) return Result.error("find: taille invalide");
                    long unit=matcher.group(3).equals("k")?1024:matcher.group(3).equals("c")?1:512;
                    long bytes=directory?0:files.get(path).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                    long amount=(bytes+unit-1)/unit,limit=Long.parseLong(matcher.group(2));
                    if(matcher.group(1).equals("+")?amount<=limit:matcher.group(1).equals("-")?amount>=limit:amount!=limit) continue;
                }
                out.append(start).append(relative.isEmpty()?"":"/"+relative).append('\n');
            }
            return Result.normal(out.toString());
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
        if (n.equals("ss") || n.startsWith("ss ")) return Result.normal("Netid State  Local Address:Port Peer Address:Port\ntcp   LISTEN 127.0.0.1:22      0.0.0.0:*");
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
            return Result.error("Non implémenté dans ce simulateur : " + command + "\nDocumentation : " + catalog);
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
            List<String> cloneArgs = ShellSyntax.words(command);
            if (cloneArgs.size() < 3 || cloneArgs.size() > 4) return Result.error("git clone: URL et destination optionnelle attendues");
            String url = cloneArgs.get(2);
            String name = cloneArgs.size() == 4 ? cloneArgs.get(3) : repoNameFromUrl(url);
            String newRoot = resolve(name);

            if (files.containsKey(newRoot) || (directories.contains(newRoot) && !childrenOf(newRoot, true).isEmpty())) return Result.error("git clone: destination non vide");
            ensureDir(newRoot);
            files.put(newRoot + "/README.md", "# " + name + "\n");

            cwd = newRoot;
            gitInitialized = true;
            repoRoot = newRoot;
            ensureDir(repoRoot + "/.git");
            headBranch = "main";
        detachedHead = null;
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
            if (inGitRepo()) return Result.success("Dépôt Git existant réinitialisé sans modifier son historique.");
            gitInitialized = true;
            repoRoot = cwd;
            ensureDir(repoRoot + "/.git");
            headBranch = "main";
        detachedHead = null;
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

        List<String> gitWords=ShellSyntax.words(command);
        if(gitWords.size()>=3 && (gitWords.get(1).equals("switch")||gitWords.get(1).equals("checkout"))) {
            String option=gitWords.get(2);
            if(option.equals("--detach")) {
                if(gitWords.size()!=4)return Result.error("Référence requise après --detach");
                Commit target=findCommit(gitWords.get(3));if(target==null)return Result.error("Référence inconnue");
                if(!target.hash.equals(currentHeadHash())&&!gitShortStatus(false).text.isEmpty())return Result.error("Modifications locales à enregistrer avant le détachement");
                boolean same=target.hash.equals(currentHeadHash()); detachedHead=target.hash;
                if(!same)checkoutHeadSnapshot();
                return Result.success("HEAD is now at "+target.hash+" "+target.message);
            }
            if(gitWords.size()==3 && !option.startsWith("--") && (branches.containsKey(option)||option.equals("-"))) {
                String target=option.equals("-")?previousBranch:option;
                return switchBranchSafely(target,false);
            }
            if(option.equals("--discard-changes")&&gitWords.size()==4) return switchBranchSafely(gitWords.get(3),true);
        }
        if (n.equals("git tag") || n.startsWith("git tag ")) {
            List<String> args=ShellSyntax.words(command);
            if(args.size()==2) return Result.normal(String.join("\n",new java.util.TreeSet<>(tags.keySet()))+(tags.isEmpty()?"":"\n"));
            if(args.get(2).equals("-d")) {
                if(args.size()!=4||tags.remove(args.get(3))==null) return Result.error("git tag: tag absent");
                return Result.normal("");
            }
            String name=args.get(2).equals("-a")&&args.size()>3?args.get(3):args.get(2);
            if(name.startsWith("-")) return Result.error("git tag: option non prise en charge");
            if(tags.containsKey(name)) return Result.error("git tag: tag existant");
            Commit head=findCommit("HEAD"); if(head==null)return Result.error("git tag: aucun commit");
            tags.put(name,head.hash); return Result.normal("");
        }
        if(n.equals("git branch --show-current")) return Result.normal(headBranch+"\n");
        if(n.equals("git branch -r")) { StringBuilder out=new StringBuilder(); for(String branch:remoteBranches.keySet())out.append("  origin/").append(branch).append('\n'); return Result.normal(out.toString()); }
        if(n.equals("git branch -v")||n.equals("git branch -vv")) {
            StringBuilder out=new StringBuilder(); for(String branch:branches.keySet()) { Commit c=findCommit(branch); out.append(branch.equals(headBranch)?"* ":"  ").append(branch).append(' ').append(c==null?"":c.hash+" "+c.message).append('\n'); } return Result.normal(out.toString());
        }
        if(n.equals("git count-objects")) return Result.normal(commits.size()+" commits virtuels\n");
        if(n.equals("git fsck")) {
            for(Commit c:commits.values()) if(!c.parent.isEmpty()&&!commits.containsKey(c.parent)) return Result.error("parent manquant : "+c.hash);
            return Result.normal("");
        }
        if(n.startsWith("git grep ")) {
            List<String> args=ShellSyntax.words(command); List<String> grep=new ArrayList<>(); grep.add("grep"); grep.add("-H");
            grep.addAll(args.subList(2,args.size())); for(String path:headSnapshot.keySet())grep.add(path);
            return TextCommands.run(grep,path->files.get(repoRoot+"/"+path));
        }
        if(n.equals("git shortlog")) return Result.normal(gitConfig.getOrDefault("user.name","Ubuntu Student")+" ("+commits.size()+")\n"+commits.values().stream().map(c->"      "+c.message).collect(java.util.stream.Collectors.joining("\n"))+"\n");
        if(n.startsWith("git reset ")) return gitResetCommand(command);
        if(n.startsWith("git restore ")) return gitRestoreCommand(command);

        if ("git status".equals(n)) return gitStatus();

        if (n.startsWith("git add ")) {
            List<String> arguments=ShellSyntax.words(command).subList(2,ShellSyntax.words(command).size());
            if(arguments.contains("-p")) return Result.error("git add -p : sélection interactive de hunks non disponible ; utilise git add fichier");
            boolean update=arguments.contains("-u"), all=arguments.contains("-A")||arguments.contains("--all")||arguments.contains(".");
            Set<String> candidates=new LinkedHashSet<>();
            if(all||update) { candidates.addAll(headSnapshot.keySet()); candidates.addAll(staged); if(!update) candidates.addAll(repoFiles()); }
            else for(String arg:arguments) {
                if(arg.startsWith("-")) return Result.error("git add: option inconnue "+arg);
                String path=resolve(arg);
                if(!path.startsWith(repoRoot+"/")) return Result.error("git add: chemin hors du dépôt");
                if(directories.contains(path)) {
                    for(String file:repoFiles()) if((repoRoot+"/"+file).startsWith(path+"/")) candidates.add(file);
                    for(String file:headSnapshot.keySet()) if((repoRoot+"/"+file).startsWith(path+"/")) candidates.add(file);
                } else {
                    String relative=relativeToRepo(path);
                    if(!files.containsKey(path)&&!headSnapshot.containsKey(relative)&&!staged.contains(relative)) return Result.error("fatal: pathspec does not match any files");
                    candidates.add(relative);
                }
            }
            for(String path:candidates) {
                if(path.equals(".git")||path.startsWith(".git/")) continue;
                if(java.util.Objects.equals(headSnapshot.get(path),files.get(repoRoot+"/"+path))) staged.remove(path); else staged.add(path);
            }
            if(conflictActive&&candidates.contains(relativeToRepo(conflictFile))) conflictActive=false;
            return Result.normal("");
        }
        if (n.startsWith("git commit -am ")) {
            executeGit("git add -u");
            return executeGit("git commit -m " + command.substring("git commit -am ".length()));
        }

        if (n.startsWith("git commit -m ")) {
            String message = stripQuotes(
                command.substring("git commit -m ".length()).trim()
            );

            if (staged.isEmpty() && !mergePending) {
                return Result.error("nothing added to commit");
            }

            Commit commit = createCommit(message);
            mergePending = false;

            return Result.success(
                "[" + headBranch + " " + commit.hash + "] " + message
            );
        }

        if (n.startsWith("git log")) {
            return gitLog(command);
        }

        if ("git diff".equals(n) || n.startsWith("git diff ")) return gitDiffCommand(command);
        if ("git diff --staged".equals(n) || "git diff --cached".equals(n)) {
            return gitDiff(true);
        }

        if ("git branch".equals(n)) return gitBranch(false);
        if ("git branch -a".equals(n)) return gitBranch(true);

        if (command.startsWith("git branch -D ")) {
            String name = command.substring("git branch -D ".length()).trim();

            if (name.equals(headBranch)) {
                return Result.error("error: Cannot delete branch '" + name + "' checked out");
            }

            if (!branches.containsKey(name)) return Result.error("error: branch '" + name + "' not found.");
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

            String start = currentHeadHash();
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
            branches.put(name, currentHeadHash());
            previousBranch = headBranch;
            headBranch = name;
            detachedHead = null;
            checkoutHeadSnapshot();
            return Result.success("Switched to and reset branch '" + name + "'");
        }

        if ("git switch -".equals(n)) {
            if (previousBranch == null ||
                previousBranch.isEmpty() ||
                !branches.containsKey(previousBranch)) {

                return Result.error("fatal: no previous branch");
            }

            String next = previousBranch;
            previousBranch = headBranch;
            headBranch = next;
            checkoutHeadSnapshot();

            return Result.success("Switched to branch '" + headBranch + "'");
        }

        if (n.startsWith("git switch -c ")) {
            String name = command.substring("git switch -c ".length()).trim();

            if (branches.containsKey(name)) {
                return Result.error(
                    "fatal: a branch named '" + name + "' already exists"
                );
            }

            branches.put(name, currentHeadHash());
            previousBranch = headBranch;
            headBranch = name;
            detachedHead = null;

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

            previousBranch = headBranch;
            headBranch = name;
            detachedHead = null;
            checkoutHeadSnapshot();

            return Result.success("Switched to branch '" + name + "'");
        }

        if (command.startsWith("git checkout -B ")) {
            String name = command.substring("git checkout -B ".length()).trim();
            branches.put(name, currentHeadHash());
            headBranch = name;
            detachedHead = null;
            checkoutHeadSnapshot();
            return Result.success("Switched to and reset branch '" + name + "'");
        }

        if (n.startsWith("git checkout -b ")) {
            String name = command.substring("git checkout -b ".length()).trim();

            if (branches.containsKey(name)) {
                return Result.error("fatal: a branch named '" + name + "' already exists");
            }

            branches.put(name, currentHeadHash());
            previousBranch = headBranch;
            headBranch = name;
            detachedHead = null;

            return Result.success("Switched to a new branch '" + name + "'");
        }

        if (command.startsWith("git checkout -B ")) {
            String name = command.substring("git checkout -B ".length()).trim();
            branches.put(name, currentHeadHash());
            headBranch = name;
            detachedHead = null;
            checkoutHeadSnapshot();
            return Result.success("Switched to and reset branch '" + name + "'");
        }

        if (n.startsWith("git checkout --detach ")) {
            String ref = command.substring("git checkout --detach ".length()).trim();
            Commit commit = findCommit(ref);
            if (commit == null && "HEAD".equalsIgnoreCase(ref)) {
                commit = findCommit(currentHeadHash());
            }
            return commit == null
                ? Result.error("fatal: invalid reference: " + ref)
                : Result.success("HEAD is now detached at " + commit.hash + " " + commit.message);
        }

        if ("git checkout -".equals(n)) {
            if (previousBranch == null ||
                previousBranch.isEmpty() ||
                !branches.containsKey(previousBranch)) {

                return Result.error("fatal: no previous branch");
            }

            String next = previousBranch;
            previousBranch = headBranch;
            headBranch = next;
            checkoutHeadSnapshot();

            return Result.success("Switched to branch '" + headBranch + "'");
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
                previousBranch = headBranch;
                headBranch = name;
            detachedHead = null;
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
            return Result.success("HEAD is now at " + currentHeadHash());
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

            Set<String> tracked = new LinkedHashSet<>(headSnapshot.keySet());
            tracked.addAll(staged);
            Map<String,String> saved = new LinkedHashMap<>();
            for (String path : tracked) saved.put(path, files.get(repoRoot + "/" + path));
            if (staged.isEmpty() && saved.equals(headSnapshot)) return Result.normal("No local changes to save");
            stashSnapshots.add(saved);
            for (String path : tracked) if (!headSnapshot.containsKey(path)) files.remove(repoRoot + "/" + path);
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
            if (stashSnapshots.isEmpty()) return Result.error("No stash entries found.");

            Map<String,String> snapshot = stashSnapshots.get(stashSnapshots.size() - 1);
            for (String path : snapshot.keySet()) {
                if (staged.contains(path) || !java.util.Objects.equals(files.get(repoRoot + "/" + path), headSnapshot.get(path)))
                    return Result.error("git stash: modifications locales écrasées dans " + path);
            }
            for (Map.Entry<String,String> entry : snapshot.entrySet()) {
                if (entry.getValue() == null) files.remove(repoRoot + "/" + entry.getKey());
                else files.put(repoRoot + "/" + entry.getKey(), entry.getValue());
            }

            if ("git stash pop".equals(n)) {
                stashSnapshots.remove(stashSnapshots.size() - 1);
            }

            return Result.success("On branch " + headBranch + "\nChanges restored from stash.");
        }

        if ("git stash drop".equals(n)) {
            if (stashSnapshots.isEmpty()) return Result.error("No stash entries found.");
            stashSnapshots.remove(stashSnapshots.size() - 1);
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

        if ("git rev-parse --abbrev-ref head".equals(n)) return Result.normal(detachedHead==null?headBranch:"HEAD");
        if ("git rev-parse head".equals(n)) return Result.normal(padHash(currentHeadHash()));
        if ("git rev-parse --show-toplevel".equals(n)) return Result.normal(repoRoot);

        if (n.startsWith("git rm ")) {
            String path = command.substring("git rm ".length())
                .replace("--cached ", "")
                .trim();

            String absolute = repoRoot + "/" + path;

            if (!command.contains("--cached")) {
                files.remove(absolute);
            }

            if(!headSnapshot.containsKey(path)&&!staged.contains(path)) return Result.error("git rm: chemin non suivi");
            staged.add(path);
            indexSnapshot.put(path,null);
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
            List<String> args=ShellSyntax.words(command); String name=args.get(args.size()-1);
            Commit incoming=findCommit(name),head=findCommit("HEAD");
            if(incoming==null)return Result.error("merge: référence inconnue");
            if(head!=null&&isAncestor(incoming.hash,head.hash))return Result.normal("Already up to date.\n");
            if(head!=null&&!isAncestor(head.hash,incoming.hash))return Result.error("Fusion divergente non implémentée automatiquement : utiliser le scénario de conflit ou le mode GitHub réel.");
            if(args.contains("--no-ff"))return Result.error("Merge commit forcé non implémenté dans ce simulateur ; utiliser le mode GitHub réel.");
            if(!gitShortStatus(false).text.isEmpty())return Result.error("merge: enregistrer ou remiser les modifications avant la fusion");
            updateHead(incoming.hash);checkoutHeadSnapshot();
            return Result.normal("Fast-forward\n");
        }

        if (n.startsWith("git cherry-pick ")||n.startsWith("git revert ")) {
            boolean revert=n.startsWith("git revert ");
            List<String> args=ShellSyntax.words(command); String ref=args.get(args.size()-1);
            if(ref.startsWith("--"))return Result.error("Aucune opération de cherry-pick/revert en cours.");
            Commit original=findCommit(ref);if(original==null)return Result.error("fatal: bad revision '"+ref+"'");
            Commit parent=commits.get(original.parent);
            Map<String,String> base=parent==null?Collections.emptyMap():parent.snapshot;
            Map<String,String> before=revert?original.snapshot:base,after=revert?base:original.snapshot;
            Set<String> changed=new LinkedHashSet<>(before.keySet());changed.addAll(after.keySet());
            for(String path:changed) {
                if(java.util.Objects.equals(before.get(path),after.get(path)))continue;
                String current=files.get(repoRoot+"/"+path);
                if(!java.util.Objects.equals(current,before.get(path))&&!java.util.Objects.equals(current,after.get(path)))return Result.error("Conflit dans "+path+" : aucune modification appliquée");
            }
            for(String path:changed) {
                if(java.util.Objects.equals(before.get(path),after.get(path)))continue;
                if(after.containsKey(path))files.put(repoRoot+"/"+path,after.get(path));else files.remove(repoRoot+"/"+path);
                if(!java.util.Objects.equals(headSnapshot.get(path),after.get(path)))staged.add(path);
            }
            if(staged.isEmpty())return Result.normal("nothing to commit\n");
            Commit result=createCommit(revert?"Revert: "+original.message:original.message);
            return Result.success("["+headBranch+" "+result.hash+"] "+result.message);
        }

        if ("git status -s".equals(n) ||
            "git status --short".equals(n) ||
            "git status -sb".equals(n) ||
            "git status --porcelain".equals(n)) {

            return gitShortStatus(n.equals("git status -sb"));
        }

        if ("git remote".equals(n)) {
            return Result.normal(remoteOrigin.isEmpty() ? "" : remoteName);
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
            List<String> args=ShellSyntax.words(command);
            if(args.size()!=5||!args.get(3).equals(remoteName)||remoteOrigin.isEmpty())return Result.error("git remote rename: remote absent ou arguments invalides");
            remoteName=args.get(4);return Result.normal("");
        }

        if ("git remote -v".equals(n)) {
            if (remoteOrigin.isEmpty()) return Result.normal("");

            return Result.normal(
                remoteName + "  " + remoteOrigin + " (fetch)\n" +
                remoteName + "  " + remoteOrigin + " (push)"
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
            return Result.error("Non implémenté dans ce simulateur Git : " + command + "\nDocumentation : " + catalogue);
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

        if ("ssh-add -l".equals(n)) {
            if (!sshAgentRunning) return Result.error("Could not open a connection to your authentication agent.");
            return sshKeyLoaded ? Result.normal("256 SHA256:SIMULATEDFINGERPRINT student@example.com (ED25519)\n") : Result.error("The agent has no identities.");
        }
        if ("ssh -t git@github.com".equals(n) || "ssh -vt git@github.com".equals(n)) {
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

        List<String> configArgs = ShellSyntax.words(command);
        List<String> operands = new ArrayList<>();
        for (int i=2;i<configArgs.size();i++) if(!configArgs.get(i).startsWith("-")) operands.add(configArgs.get(i));
        if(configArgs.contains("--unset") && operands.size()==1) return gitConfig.remove(operands.get(0))==null ? Result.status("",5) : Result.normal("");
        if(operands.size()==2 && operands.get(0).contains(".")) { gitConfig.put(operands.get(0),operands.get(1)); return Result.normal(""); }
        if(operands.size()==1 && operands.get(0).contains(".")) return gitConfig.containsKey(operands.get(0)) ? Result.normal(gitConfig.get(operands.get(0))+"\n") : Result.status("",1);

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

        if (detachedHead != null) out.append("HEAD detached at ").append(detachedHead).append('\n');
        else out.append("On branch ").append(headBranch).append('\n');

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

        Set<String> paths = new LinkedHashSet<>(current);
        paths.addAll(headSnapshot.keySet());
        paths.addAll(staged);
        for (String relative : paths) {
            String work = files.get(repoRoot + "/" + relative);
            String index = staged.contains(relative) ? indexSnapshot.get(relative) : headSnapshot.get(relative);
            if (!headSnapshot.containsKey(relative) && !staged.contains(relative)) {
                if (work != null) untracked.add(relative);
            } else if (!java.util.Objects.equals(work, index)) modified.add(relative);
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
            String local = currentHeadHash();

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

        List<String> args = ShellSyntax.words(command);
        int limit = Integer.MAX_VALUE;
        for (int i = 2; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.equals("-n")) {
                if (++i >= args.size()) return Result.error("git log : -n attend un nombre");
                try { limit = Integer.parseInt(args.get(i)); }
                catch (NumberFormatException e) { return Result.error("git log : nombre invalide"); }
            } else if (arg.matches("-[0-9]+")) limit = Integer.parseInt(arg.substring(1));
        }
        if (limit < 0) return Result.error("git log : nombre négatif");
        List<Commit> list = new ArrayList<>();
        if (args.contains("--all")) {
            list.addAll(commits.values());
            Collections.reverse(list);
        } else {
            Commit cursor = findCommit("HEAD");
            while (cursor != null) { list.add(cursor); cursor = commits.get(cursor.parent); }
        }
        for (String arg : args) {
            if (arg.startsWith("--grep=")) {
                String pattern = arg.substring(7);
                list.removeIf(c -> !java.util.regex.Pattern.compile(pattern).matcher(c.message).find());
            }
            if (arg.startsWith("--author=")) {
                String author = gitConfig.getOrDefault("user.name", "Ubuntu Student") + " <" + gitConfig.getOrDefault("user.email", "student@example.com") + ">";
                if (!java.util.regex.Pattern.compile(arg.substring(9)).matcher(author).find()) list.clear();
            }
            if (arg.startsWith("--since=")) return Result.error("git log: filtre de date non implémenté dans le simulateur");
        }
        if (list.size() > limit) list = new ArrayList<>(list.subList(0, limit));
        if (args.contains("--reverse")) Collections.reverse(list);
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
            if (args.contains("-p") || args.contains("--stat") || args.contains("--name-only") || args.contains("--name-status")) {
                Commit parent = commits.get(commit.parent);
                Map<String,String> before = parent == null ? Collections.emptyMap() : parent.snapshot;
                Set<String> changed = new LinkedHashSet<>(before.keySet());
                changed.addAll(commit.snapshot.keySet());
                for (String path : changed) {
                    String old = before.get(path), next = commit.snapshot.get(path);
                    if (java.util.Objects.equals(old, next)) continue;
                    if (args.contains("-p")) out.append("diff --git a/").append(path).append(" b/").append(path)
                        .append("\n-").append(old == null ? "" : old).append("\n+").append(next == null ? "" : next).append('\n');
                    if (args.contains("--stat")) out.append(" ").append(path).append(" | modifié\n");
                    if (args.contains("--name-status")) out.append(old == null ? "A\t" : next == null ? "D\t" : "M\t").append(path).append('\n');
                    else if (args.contains("--name-only")) out.append(path).append('\n');
                }
            }
        }

        return Result.normal(out.toString().trim());
    }

    private String currentHeadHash() { return detachedHead!=null?detachedHead:branches.getOrDefault(headBranch, ""); }
    private void updateHead(String hash) { if(detachedHead!=null)detachedHead=hash;else branches.put(headBranch,hash); }

    private boolean isAncestor(String ancestor,String descendant) {
        Set<String> visited=new LinkedHashSet<>();
        for(Commit c=commits.get(descendant);c!=null&&visited.add(c.hash);c=commits.get(c.parent))if(c.hash.equals(ancestor))return true;
        return ancestor.isEmpty();
    }

    private Result switchBranchSafely(String target,boolean discard) {
        if(!branches.containsKey(target))return Result.error("git switch: branche inconnue");
        if(conflictActive)return Result.error("git switch: résoudre le conflit avant de changer de branche");
        Commit commit=findCommit(target);Map<String,String> next=commit==null?Collections.emptyMap():commit.snapshot;
        Set<String> paths=new LinkedHashSet<>(headSnapshot.keySet());paths.addAll(next.keySet());
        if(!discard)for(String path:paths) {
            String work=files.get(repoRoot+"/"+path),old=headSnapshot.get(path),value=next.get(path);
            if(!java.util.Objects.equals(old,value)&&(!java.util.Objects.equals(work,old)||staged.contains(path)))return Result.error("git switch: modifications locales écrasées dans "+path);
        }
        for(String path:paths)if(discard||!java.util.Objects.equals(headSnapshot.get(path),next.get(path))) {
            if(next.containsKey(path))files.put(repoRoot+"/"+path,next.get(path));else files.remove(repoRoot+"/"+path);
        }
        previousBranch=headBranch;headBranch=target;detachedHead=null;headSnapshot=new LinkedHashMap<>(next);
        if(discard)staged.clear();
        return Result.success("Switched to branch '"+target+"'");
    }

    private Result gitShortStatus(boolean branch) {
        Set<String> paths=new java.util.TreeSet<>(repoFiles()); paths.addAll(headSnapshot.keySet()); paths.addAll(staged);
        StringBuilder out=new StringBuilder(branch?"## "+headBranch+"\n":"");
        for(String path:paths) {
            String head=headSnapshot.get(path),index=staged.contains(path)?indexSnapshot.get(path):head,work=files.get(repoRoot+"/"+path);
            if(head==null&&index==null&&work!=null) { out.append("?? ").append(path).append('\n'); continue; }
            char x=java.util.Objects.equals(head,index)?' ':head==null?'A':index==null?'D':'M';
            char y=java.util.Objects.equals(index,work)?' ':work==null?'D':'M';
            if(x!=' '||y!=' ')out.append(x).append(y).append(' ').append(path).append('\n');
        }
        return Result.normal(out.toString());
    }

    private Result gitRestoreCommand(String command) {
        List<String> args=ShellSyntax.words(command);
        boolean index=args.contains("--staged"),work=args.contains("--worktree")||!index;
        String ref=null;
        for(String arg:args)if(arg.startsWith("--source="))ref=arg.substring(9);
        Map<String,String> source=new LinkedHashMap<>(headSnapshot);
        if(ref!=null) { Commit c=findCommit(ref); if(c==null)return Result.error("git restore: référence inconnue"); source=c.snapshot; }
        else if(!index) for(String path:staged) { if(indexSnapshot.get(path)==null)source.remove(path);else source.put(path,indexSnapshot.get(path)); }
        List<String> paths=new ArrayList<>(); for(int i=2;i<args.size();i++)if(!args.get(i).startsWith("-"))paths.add(args.get(i));
        if(paths.isEmpty())return Result.error("git restore: chemin requis");
        if(paths.contains("."))paths=new ArrayList<>(source.keySet());
        for(String path:paths) {
            if(!source.containsKey(path)&&!headSnapshot.containsKey(path))return Result.error("git restore: chemin inconnu");
            if(work) { if(source.containsKey(path))files.put(repoRoot+"/"+path,source.get(path));else files.remove(repoRoot+"/"+path); }
            if(index) { if(java.util.Objects.equals(source.get(path),headSnapshot.get(path)))staged.remove(path);else {staged.add(path);indexSnapshot.put(path,source.get(path));} }
        }
        return Result.normal("");
    }

    private Result gitResetCommand(String command) {
        List<String> args=ShellSyntax.words(command);
        String mode="--mixed",ref="HEAD"; List<String> operands=new ArrayList<>();
        for(int i=2;i<args.size();i++) { String arg=args.get(i); if(arg.startsWith("--")) { if(!arg.equals("--"))mode=arg; }else operands.add(arg); }
        if(!java.util.Arrays.asList("--soft","--mixed","--hard").contains(mode))return Result.error("git reset: option non prise en charge");
        if(!operands.isEmpty()&&findCommit(operands.get(0))!=null)ref=operands.remove(0);
        else if(!operands.isEmpty()&&(operands.get(0).contains("~")||operands.get(0).contains("^"))) return Result.error("git reset: référence inconnue");
        if(!operands.isEmpty()) { for(String path:operands)staged.remove(path);return Result.normal(""); }
        Commit target=findCommit(ref); if(target==null)return Result.error("git reset: référence inconnue");
        Map<String,String> oldIndex=new LinkedHashMap<>(headSnapshot);
        for(String path:staged){ if(indexSnapshot.get(path)==null)oldIndex.remove(path);else oldIndex.put(path,indexSnapshot.get(path)); }
        Set<String> oldTracked=new LinkedHashSet<>(headSnapshot.keySet());oldTracked.addAll(staged);
        updateHead(target.hash); headSnapshot=new LinkedHashMap<>(target.snapshot);staged.clear();
        if(mode.equals("--soft")) {
            Set<String> paths=new LinkedHashSet<>(oldIndex.keySet());paths.addAll(target.snapshot.keySet());
            for(String path:paths)if(!java.util.Objects.equals(oldIndex.get(path),target.snapshot.get(path))){staged.add(path);indexSnapshot.put(path,oldIndex.get(path));}
        } else if(mode.equals("--hard")) {
            for(String path:oldTracked)if(!target.snapshot.containsKey(path))files.remove(repoRoot+"/"+path);
            for(Map.Entry<String,String> entry:target.snapshot.entrySet())files.put(repoRoot+"/"+entry.getKey(),entry.getValue());
            conflictActive=false; mergePending=false;
        }
        return Result.normal("");
    }

    private Result gitDiffCommand(String command) {
        List<String> args=ShellSyntax.words(command);
        boolean cached=args.contains("--cached")||args.contains("--staged");
        Map<String,String> index=new LinkedHashMap<>(headSnapshot);
        for(String path:staged) { if(indexSnapshot.get(path)==null) index.remove(path); else index.put(path,indexSnapshot.get(path)); }
        Map<String,String> work=new LinkedHashMap<>();
        Set<String> tracked=new LinkedHashSet<>(headSnapshot.keySet()); tracked.addAll(index.keySet());
        for(String path:tracked) if(files.containsKey(repoRoot+"/"+path)) work.put(path,files.get(repoRoot+"/"+path));
        Map<String,String> before=cached?headSnapshot:index, after=cached?index:work;
        List<String> refs=new ArrayList<>();
        for(int i=2;i<args.size();i++) if(!args.get(i).startsWith("-")) refs.add(args.get(i));
        if(refs.size()==1&&refs.get(0).contains("..")) refs=java.util.Arrays.asList(refs.get(0).split("\\.\\.",2));
        if(!refs.isEmpty()) {
            Commit left=findCommit(refs.get(0)); if(left==null) return Result.error("git diff: référence inconnue "+refs.get(0));
            before=left.snapshot;
            if(refs.size()>1) { Commit right=findCommit(refs.get(1)); if(right==null)return Result.error("git diff: référence inconnue"); after=right.snapshot; }
        }
        Set<String> paths=new java.util.TreeSet<>(before.keySet()); paths.addAll(after.keySet());
        StringBuilder out=new StringBuilder();
        for(String path:paths) {
            String old=before.get(path),value=after.get(path);
            if(java.util.Objects.equals(old,value)) continue;
            if(args.contains("--name-only")) out.append(path).append('\n');
            else if(args.contains("--name-status")) out.append(old==null?"A\t":value==null?"D\t":"M\t").append(path).append('\n');
            else if(args.contains("--stat")) out.append(" ").append(path).append(" | modifié\n");
            else if(args.contains("--word-diff")||args.contains("--color-words")) return Result.error("git diff: le rendu mot à mot n'est pas pris en charge");
            else out.append("diff --git a/").append(path).append(" b/").append(path).append("\n--- a/").append(path).append("\n+++ b/").append(path).append('\n')
                .append(old==null?"":"-"+old.replace("\n","\n-")).append('\n').append(value==null?"":"+"+value.replace("\n","\n+")).append('\n');
        }
        return Result.normal(out.toString());
    }

    private Result gitDiff(boolean cached) {
        StringBuilder out = new StringBuilder();

        if (cached) {
            for (String path : staged) {
                String before = headSnapshot.getOrDefault(path, "");
                String after = indexSnapshot.get(path) == null ? "" : indexSnapshot.get(path);
                if (before.equals(after)) continue;

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
                String before = staged.contains(path) ? indexSnapshot.get(path) : headSnapshot.get(path);
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
            mergePending = true;
            conflictOnNextPull = false;

            return Result.error(
                "Auto-merging README.md\n" +
                "CONFLICT (content): Merge conflict in README.md\n" +
                "Automatic merge failed; fix conflicts and then commit the result."
            );
        }

        String remoteMain = remoteBranches.getOrDefault("main", originMain);
        String local = currentHeadHash();

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
        boolean append = ShellSyntax.split(command, ">>").size() > 1;
        List<String> parts = ShellSyntax.split(command, append ? ">>" : ">");
        List<String> words = ShellSyntax.words(parts.get(0));
        boolean newline = words.size() < 2 || !words.get(1).equals("-n");
        int start = newline ? 1 : 2;
        String value = String.join(" ", words.subList(Math.min(start, words.size()), words.size()));
        if (value.equals("$?")) value = String.valueOf(lastExitCode);
        else if (value.startsWith("$") && environment.containsKey(value.substring(1)) && !parts.get(0).contains("'")) value = environment.get(value.substring(1));
        if (newline) value += "\n";
        if (parts.size() == 1) return Result.normal(value);
        if (parts.size() != 2) return Result.error("bash: redirection invalide");
        List<String> target = ShellSyntax.words(parts.get(1));
        if (target.size() != 1) return Result.error("bash: cible de redirection manquante ou ambiguë");
        String path = resolve(target.get(0));
        if (!directories.contains(parent(path))) return Result.error("bash: dossier parent absent");
        files.put(path, (append ? files.getOrDefault(path, "") : "") + value);
        return Result.normal("");
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

    private Result list(String directory, boolean showHidden, boolean includeDots) {
        List<FsEntry> entries = childrenOf(directory, showHidden);
        if (includeDots) {
            entries.add(0, new FsEntry("..", true));
            entries.add(0, new FsEntry(".", true));
        }
        return Result.ls(entries, "");
    }

    private Result listSsh() {
        List<FsEntry> entries = new ArrayList<>();

        if (sshPrivateKeyExists) {
            entries.add(new FsEntry("id_ed25519", false));
            entries.add(new FsEntry("id_ed25519.pub", false));
        }

        return Result.ls(entries, "");
    }

    private FsEntry fsEntry(String name, String path, boolean directory) {
        FsEntry entry = new FsEntry(name, directory);
        int mode = Integer.parseInt(permissionModes.getOrDefault(path, directory ? "755" : "644"), 8);
        StringBuilder bits = new StringBuilder(directory ? "d" : "-");
        for (int bit = 8; bit >= 0; bit--) bits.append((mode & (1 << bit)) == 0 ? '-' : "xwr".charAt(bit % 3));
        entry.permissions = bits.toString();
        return entry;
    }

    private List<FsEntry> childrenOf(String directory, boolean showHidden) {
        List<FsEntry> entries = new ArrayList<>();
        String prefix = "/".equals(directory) ? "/" : directory + "/";

        for (String dir : directories) {
            if (dir.equals(directory) || "/".equals(dir)) continue;
            if (!parent(dir).equals(directory)) continue;

            String name = dir.substring(prefix.length());

            if (!showHidden && name.startsWith(".")) continue;

            entries.add(fsEntry(name, dir, true));
        }

        for (String file : files.keySet()) {
            if (!parent(file).equals(directory)) continue;

            String name = file.substring(prefix.length());

            if (!showHidden && name.startsWith(".")) continue;

            entries.add(fsEntry(name, file, false));
        }

        entries.sort((a, b) ->
            a.name.compareToIgnoreCase(b.name)
        );

        return entries;
    }

    private Commit createCommit(String message) {
        Map<String,String> snapshot = new LinkedHashMap<>(headSnapshot);

        for (String path : staged) {
            if (indexSnapshot.get(path) != null) {
                snapshot.put(path, indexSnapshot.get(path));
            } else {
                snapshot.remove(path);
            }
        }

        String parent = currentHeadHash();
        String hash = String.format(Locale.ROOT, "%07x", commitCounter++ * 7919);

        Commit commit = new Commit(
            hash,
            message,
            parent,
            headBranch,
            snapshot
        );

        commits.put(hash, commit);
        updateHead(hash);
        headSnapshot = snapshot;
        staged.clear();

        return commit;
    }

    private void checkoutHeadSnapshot() {
        String hash = currentHeadHash();

        if (hash.isEmpty()) {
            headSnapshot = new LinkedHashMap<>();
            return;
        }

        Commit commit = commits.get(hash);

        if (commit == null) return;

        for (String path : headSnapshot.keySet()) {
            if (!commit.snapshot.containsKey(path)) files.remove(repoRoot + "/" + path);
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
            if (path.startsWith(repoRoot + "/") && !path.startsWith(repoRoot+"/.git/")) {
                result.add(relativeToRepo(path));
            }
        }

        return result;
    }

    private boolean inGitRepo() {
        return gitInitialized && directories.contains(repoRoot + "/.git") &&
            (cwd.equals(repoRoot) || cwd.startsWith(repoRoot + "/"));
    }

    private void ensureGitRepo(String root) {
        ensureDir(root);
        cwd = root;

        files.putIfAbsent(root + "/README.md", "# Projet\n");

        if (!gitInitialized || !root.equals(repoRoot) || !directories.contains(root + "/.git")) {
            gitInitialized = true;
            repoRoot = root;
            ensureDir(repoRoot + "/.git");
            headBranch = "main";
        detachedHead = null;
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

        java.util.regex.Matcher ancestry=java.util.regex.Pattern.compile("(.+?)(?:~([0-9]+)|\\^)").matcher(ref);
        if(ancestry.matches()) {
            Commit commit=findCommit(ancestry.group(1));
            int count=ancestry.group(2)==null?1:Integer.parseInt(ancestry.group(2));
            while(commit!=null&&count-->0) commit=commits.get(commit.parent);
            return commit;
        }
        if ("HEAD".equalsIgnoreCase(ref)) {
            String head = currentHeadHash();
            return commits.get(head);
        }

        if (tags.containsKey(ref)) return commits.get(tags.get(ref));
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
