package com.ghiles.quizubuntu;

import android.content.Context;
import android.content.SharedPreferences;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Real Git operations inside the application's private storage.
 *
 * The class never invokes a system shell. JGit reads/writes a repository under
 * Context#getFilesDir(), and network access is HTTPS only.
 */
public final class RealGitClient {

    private static final String PREFS = "real_git";
    private static final String REPO_URL = "repo_url";
    private static final String REPO_NAME = "repo_name";
    private static final String DEFAULT_BRANCH = "default_branch";

    private final Context context;
    private final SecureTokenStore tokenStore;
    private final SharedPreferences prefs;

    public RealGitClient(Context context, SecureTokenStore tokenStore) {
        this.context = context.getApplicationContext();
        this.tokenStore = tokenStore;
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void selectRepository(String fullName, String cloneUrl, String defaultBranch) {
        prefs.edit()
            .putString(REPO_NAME, fullName == null ? "" : fullName)
            .putString(REPO_URL, cloneUrl == null ? "" : cloneUrl)
            .putString(DEFAULT_BRANCH, defaultBranch == null ? "main" : defaultBranch)
            .apply();
    }

    public String selectedRepositoryName() {
        return prefs.getString(REPO_NAME, "");
    }

    public String selectedRepositoryUrl() {
        return prefs.getString(REPO_URL, "");
    }

    public String defaultBranch() {
        return prefs.getString(DEFAULT_BRANCH, "main");
    }

    public File workTree() {
        return new File(context.getFilesDir(), "github-real");
    }

    public boolean hasLocalRepository() {
        return new File(workTree(), ".git").isDirectory();
    }

    public String displayPath() {
        return hasLocalRepository() ? "~/github-real" : "~";
    }

    public String currentBranch() {
        if (!hasLocalRepository()) return "";

        try (Git git = Git.open(workTree())) {
            return git.getRepository().getBranch();
        } catch (Exception e) {
            return "";
        }
    }

    public synchronized String cloneSelectedRepository(boolean replaceExisting) throws Exception {
        String url = selectedRepositoryUrl();

        if (url.isEmpty()) {
            throw new IllegalStateException("Aucun dépôt GitHub n'est sélectionné.");
        }

        File target = workTree();

        if (hasLocalRepository()) {
            try (Git git = Git.open(target)) {
                String current = git.getRepository()
                    .getConfig()
                    .getString(
                        ConfigConstants.CONFIG_REMOTE_SECTION,
                        "origin",
                        ConfigConstants.CONFIG_KEY_URL
                    );

                if (url.equals(current)) {
                    return "Dépôt déjà présent dans " + displayPath();
                }
            }

            if (!replaceExisting) {
                throw new IllegalStateException(
                    "Un autre dépôt existe déjà dans l'espace Git réel de l'application."
                );
            }

            deleteRecursively(target);
        }

        if (target.exists() && !target.isDirectory()) {
            throw new IllegalStateException("Le chemin de travail Git réel est invalide.");
        }

        try (Git ignored = Git.cloneRepository()
            .setURI(url)
            .setDirectory(target)
            .setCredentialsProvider(credentials())
            .call()) {
            return "Clonage terminé : " + selectedRepositoryName();
        }
    }

    public synchronized String execute(String rawCommand) throws Exception {
        String command = rawCommand == null ? "" : rawCommand.trim();

        if (command.isEmpty()) return "";

        if (command.startsWith("git clone ")) {
            String url = command.substring("git clone ".length()).trim();
            if (!url.startsWith("https://")) {
                return "Mode GitHub réel : utilise une URL HTTPS pour l'authentification sécurisée.";
            }

            selectRepository(
                inferFullName(url),
                url,
                "main"
            );

            return cloneSelectedRepository(false);
        }

        if (!command.startsWith("git ")) {
            return executeFileCommand(command);
        }

        if ("git --version".equals(command)) {
            return "JGit 6.10.1 (moteur Git Java embarqué)";
        }

        if (!hasLocalRepository()) {
            if ("git init".equals(command)) {
                File target = workTree();
                if (!target.exists() && !target.mkdirs()) {
                    throw new IllegalStateException("Impossible de créer le dossier Git privé.");
                }

                try (Git ignored = Git.init().setDirectory(target).call()) {
                    return "Dépôt Git réel initialisé dans l'espace privé de l'application.";
                }
            }

            return "fatal: not a git repository — clone ou initialise d'abord un dépôt.";
        }

        try (Git git = Git.open(workTree())) {
            Repository repository = git.getRepository();

            if ("git status".equals(command)) {
                return status(git.status().call());
            }

            if ("git remote -v".equals(command)) {
                return remoteVerbose(repository);
            }

            if ("git remote get-url origin".equals(command)) {
                String url = repository.getConfig().getString(
                    ConfigConstants.CONFIG_REMOTE_SECTION,
                    "origin",
                    ConfigConstants.CONFIG_KEY_URL
                );

                return url == null ? "" : url;
            }

            if (command.startsWith("git remote add origin ")) {
                String url = command.substring("git remote add origin ".length()).trim();
                setOrigin(repository, url);
                return "";
            }

            if (command.startsWith("git remote set-url origin ")) {
                String url = command.substring("git remote set-url origin ".length()).trim();
                setOrigin(repository, url);
                return "";
            }

            if ("git remote remove origin".equals(command)) {
                repository.getConfig().unsetSection(
                    ConfigConstants.CONFIG_REMOTE_SECTION,
                    "origin"
                );
                repository.getConfig().save();
                return "";
            }

            if ("git branch".equals(command)) {
                StringBuilder out = new StringBuilder();
                String active = repository.getBranch();

                for (Ref ref : git.branchList().call()) {
                    String name = Repository.shortenRefName(ref.getName());
                    out.append(name.equals(active) ? "* " : "  ")
                       .append(name)
                       .append('\n');
                }

                return out.toString().trim();
            }

            if ("git branch -a".equals(command)) {
                StringBuilder out = new StringBuilder();
                String active = repository.getBranch();

                for (Ref ref : git.branchList()
                    .setListMode(org.eclipse.jgit.api.ListBranchCommand.ListMode.ALL)
                    .call()) {
                    String name = Repository.shortenRefName(ref.getName());
                    out.append(name.equals(active) ? "* " : "  ")
                       .append(name)
                       .append('\n');
                }

                return out.toString().trim();
            }

            if (command.startsWith("git branch -M ")) {
                String name = command.substring("git branch -M ".length()).trim();
                git.branchRename().setNewName(name).call();
                return "";
            }

            if (command.startsWith("git branch -d ")) {
                String name = command.substring("git branch -d ".length()).trim();
                List<String> deleted = git.branchDelete()
                    .setBranchNames(name)
                    .setForce(false)
                    .call();

                return deleted.isEmpty()
                    ? "Aucune branche supprimée."
                    : "Branche supprimée : " + String.join(", ", deleted);
            }

            if (command.startsWith("git checkout -b ")) {
                String name = command.substring("git checkout -b ".length()).trim();

                git.checkout()
                    .setCreateBranch(true)
                    .setName(name)
                    .call();

                return "Switched to a new branch '" + name + "'";
            }

            if (command.startsWith("git checkout -B ")) {
                String name = command.substring("git checkout -B ".length()).trim();

                try {
                    git.branchDelete()
                        .setBranchNames(name)
                        .setForce(true)
                        .call();
                } catch (Exception ignored) {
                }

                git.checkout()
                    .setCreateBranch(true)
                    .setName(name)
                    .call();

                return "Switched to and reset branch '" + name + "'";
            }

            if (command.startsWith("git checkout -- ")) {
                String path = command.substring("git checkout -- ".length()).trim();

                git.checkout()
                    .addPath(path)
                    .call();

                return "";
            }

            if (command.startsWith("git checkout ")) {
                String name = command.substring("git checkout ".length()).trim();

                git.checkout()
                    .setName(name)
                    .call();

                return "Switched to branch '" + name + "'";
            }

            if (command.startsWith("git switch -c ")) {
                String name = command.substring("git switch -c ".length()).trim();

                git.checkout()
                    .setCreateBranch(true)
                    .setName(name)
                    .call();

                return "Switched to a new branch '" + name + "'";
            }

            if (command.startsWith("git switch ")) {
                String name = command.substring("git switch ".length()).trim();
                git.checkout().setName(name).call();
                return "Switched to branch '" + name + "'";
            }

            if (command.startsWith("git add ")) {
                String pattern = command.substring("git add ".length()).trim();

                if (".".equals(pattern)) {
                    git.add().addFilepattern(".").call();
                } else {
                    git.add().addFilepattern(pattern).call();
                }

                return "";
            }

            if (command.startsWith("git commit -m ")) {
                String message = stripQuotes(
                    command.substring("git commit -m ".length()).trim()
                );

                RevCommit commit = git.commit()
                    .setMessage(message)
                    .call();

                return "[" + repository.getBranch() + " " +
                    commit.getId().abbreviate(7).name() + "] " + message;
            }

            if (command.startsWith("git log")) {
                return log(git, command);
            }

            if ("git diff".equals(command)) {
                return diff(git, false);
            }

            if ("git diff --staged".equals(command) || "git diff --cached".equals(command)) {
                return diff(git, true);
            }

            if ("git fetch".equals(command) ||
                "git fetch origin".equals(command) ||
                "git fetch --all".equals(command) ||
                "git fetch --prune".equals(command)) {

                git.fetch()
                    .setRemote("origin")
                    .setRemoveDeletedRefs(command.contains("--prune"))
                    .setCredentialsProvider(credentials())
                    .call();

                return "Fetch terminé depuis origin.";
            }

            if ("git pull origin main".equals(command)) {
                PullResult result = git.pull()
                    .setRemote("origin")
                    .setRemoteBranchName("main")
                    .setCredentialsProvider(credentials())
                    .call();

                return describePull(result);
            }

            if ("git pull --rebase origin main".equals(command)) {
                PullResult result = git.pull()
                    .setRemote("origin")
                    .setRemoteBranchName("main")
                    .setRebase(true)
                    .setCredentialsProvider(credentials())
                    .call();

                return describePull(result);
            }

            if ("git push".equals(command)) {
                git.push()
                    .setCredentialsProvider(credentials())
                    .call();

                return "Push terminé.";
            }

            if (command.startsWith("git push -u origin ")) {
                String branch = command.substring("git push -u origin ".length()).trim();

                git.push()
                    .setRemote("origin")
                    .add(branch)
                    .setCredentialsProvider(credentials())
                    .call();

                repository.getConfig().setString(
                    ConfigConstants.CONFIG_BRANCH_SECTION,
                    branch,
                    ConfigConstants.CONFIG_KEY_REMOTE,
                    "origin"
                );
                repository.getConfig().setString(
                    ConfigConstants.CONFIG_BRANCH_SECTION,
                    branch,
                    ConfigConstants.CONFIG_KEY_MERGE,
                    "refs/heads/" + branch
                );
                repository.getConfig().save();

                return "Push terminé ; upstream configuré pour origin/" + branch + ".";
            }

            if (command.startsWith("git push origin --delete ")) {
                String branch = command.substring("git push origin --delete ".length()).trim();

                git.push()
                    .setRemote("origin")
                    .setRefSpecs(new RefSpec(":refs/heads/" + branch))
                    .setCredentialsProvider(credentials())
                    .call();

                return "Branche distante supprimée : " + branch;
            }

            if (command.startsWith("git push origin ")) {
                String branch = command.substring("git push origin ".length()).trim();

                git.push()
                    .setRemote("origin")
                    .add(branch)
                    .setCredentialsProvider(credentials())
                    .call();

                return "Push terminé vers origin/" + branch + ".";
            }

            if ("git ls-remote origin".equals(command) ||
                "git ls-remote --heads origin".equals(command)) {

                String origin = repository.getConfig().getString(
                    ConfigConstants.CONFIG_REMOTE_SECTION,
                    "origin",
                    ConfigConstants.CONFIG_KEY_URL
                );

                if (origin == null || origin.isEmpty()) {
                    return "fatal: aucun remote origin configuré";
                }

                Collection<Ref> refs = Git.lsRemoteRepository()
                    .setRemote(origin)
                    .setHeads(command.contains("--heads"))
                    .setCredentialsProvider(credentials())
                    .call();

                StringBuilder out = new StringBuilder();

                for (Ref ref : refs) {
                    if (ref.getObjectId() == null) continue;

                    out.append(ref.getObjectId().name())
                       .append('\t')
                       .append(ref.getName())
                       .append('\n');
                }

                return out.toString().trim();
            }

            if (command.startsWith("git config ")) {
                return config(repository, command);
            }

            if (command.startsWith("git show ")) {
                return showCommit(repository, command);
            }

            if ("git merge --abort".equals(command)) {
                return "Cette opération réelle n'est lancée que lorsqu'un merge JGit est effectivement en cours. " +
                    "Si tu as un conflit réel, utilise d'abord git status et résous les fichiers dans l'espace privé de l'app.";
            }

            if (command.startsWith("git push --force-with-lease")) {
                return "Sécurité : force-with-lease réel n'est pas exécuté automatiquement dans l'app.";
            }

            return "Commande Git réelle non encore prise en charge : " + command;
        }
    }

    private String executeFileCommand(String command) throws Exception {
        File root = workTree();

        if ("pwd".equals(command)) {
            return displayPath();
        }

        if ("ls".equals(command) || "ls -la".equals(command) || "ls -l".equals(command)) {
            if (!root.exists()) return "";

            File[] files = root.listFiles();
            if (files == null) return "";

            StringBuilder out = new StringBuilder();

            for (File file : files) {
                if (!command.contains("-a") && file.getName().startsWith(".")) continue;

                if (command.contains("-l")) {
                    out.append(file.isDirectory() ? "drwxr-xr-x  " : "-rw-r--r--  ");
                }

                out.append(file.getName());
                if (file.isDirectory()) out.append("/");
                out.append('\n');
            }

            return out.toString().trim();
        }

        return "En mode GitHub réel, les commandes shell arbitraires restent désactivées. " +
            "Utilise les commandes Git prises en charge ou repasse en SIMULATION.";
    }

    private CredentialsProvider credentials() {
        String token = tokenStore.loadToken();

        if (token.isEmpty()) {
            throw new IllegalStateException(
                "Aucun jeton GitHub sécurisé n'est enregistré."
            );
        }

        // GitHub accepts a non-empty username and the token as the HTTPS password.
        return new UsernamePasswordCredentialsProvider("git", token);
    }

    private String status(Status status) {
        StringBuilder out = new StringBuilder();

        if (status.isClean()) {
            return "nothing to commit, working tree clean";
        }

        appendSet(out, "Changes to be committed", status.getAdded());
        appendSet(out, "Changes staged", status.getChanged());
        appendSet(out, "Modified", status.getModified());
        appendSet(out, "Missing", status.getMissing());
        appendSet(out, "Removed", status.getRemoved());
        appendSet(out, "Untracked files", status.getUntracked());
        appendSet(out, "Conflicting files", status.getConflicting());

        return out.toString().trim();
    }

    private void appendSet(StringBuilder out, String title, Collection<String> values) {
        if (values == null || values.isEmpty()) return;

        out.append(title).append(":\n");

        for (String value : values) {
            out.append("  ").append(value).append('\n');
        }
    }

    private String remoteVerbose(Repository repository) {
        String url = repository.getConfig().getString(
            ConfigConstants.CONFIG_REMOTE_SECTION,
            "origin",
            ConfigConstants.CONFIG_KEY_URL
        );

        if (url == null || url.isEmpty()) return "";

        String push = repository.getConfig().getString(
            ConfigConstants.CONFIG_REMOTE_SECTION,
            "origin",
            "pushurl"
        );

        if (push == null || push.isEmpty()) push = url;

        return "origin  " + url + " (fetch)\n" +
               "origin  " + push + " (push)";
    }

    private void setOrigin(Repository repository, String url) throws Exception {
        repository.getConfig().setString(
            ConfigConstants.CONFIG_REMOTE_SECTION,
            "origin",
            ConfigConstants.CONFIG_KEY_URL,
            url
        );

        repository.getConfig().save();
    }

    private String log(Git git, String command) throws Exception {
        boolean oneline = command.contains("--oneline");
        boolean decorate = command.contains("--decorate");
        boolean graph = command.contains("--graph");

        StringBuilder out = new StringBuilder();

        int count = 0;
        for (RevCommit commit : git.log().setMaxCount(40).call()) {
            if (count++ > 0) out.append('\n');

            String id = commit.getId().abbreviate(7).name();

            if (graph) out.append("* ");

            if (oneline) {
                out.append(id)
                   .append(" ")
                   .append(commit.getShortMessage());
            } else {
                out.append("commit ")
                   .append(commit.getId().name())
                   .append('\n')
                   .append("Author: ")
                   .append(commit.getAuthorIdent().getName())
                   .append('\n')
                   .append('\n')
                   .append("    ")
                   .append(commit.getFullMessage());
            }

            if (decorate) {
                // Keep output compact; refs can still be inspected with git branch -a.
                out.append("  [").append(id).append("]");
            }
        }

        return out.toString().trim();
    }

    private String diff(Git git, boolean cached) throws Exception {
        List<DiffEntry> entries = git.diff()
            .setCached(cached)
            .call();

        if (entries.isEmpty()) return "";

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (DiffFormatter formatter = new DiffFormatter(bytes)) {
            formatter.setRepository(git.getRepository());

            for (DiffEntry entry : entries) {
                formatter.format(entry);
            }
        }

        return bytes.toString(StandardCharsets.UTF_8.name()).trim();
    }

    private String describePull(PullResult result) {
        if (result == null) return "Pull terminé.";

        if (result.isSuccessful()) {
            return "Pull terminé avec succès.";
        }

        if (result.getMergeResult() != null) {
            return "Pull terminé avec état de merge : " +
                result.getMergeResult().getMergeStatus();
        }

        if (result.getRebaseResult() != null) {
            return "Pull terminé avec état de rebase : " +
                result.getRebaseResult().getStatus();
        }

        return "Pull terminé : " + result;
    }

    private String config(Repository repository, String command) throws Exception {
        String normalized = command.trim();

        if ("git config --list".equals(normalized) ||
            "git config --global --list".equals(normalized) ||
            "git config --list --show-origin".equals(normalized)) {

            StringBuilder out = new StringBuilder();

            for (String section : repository.getConfig().getSections()) {
                for (String subsection : repository.getConfig().getSubsections(section)) {
                    for (String name : repository.getConfig().getNames(section, subsection)) {
                        String value = repository.getConfig().getString(section, subsection, name);
                        out.append(section)
                           .append(".")
                           .append(subsection)
                           .append(".")
                           .append(name)
                           .append("=")
                           .append(value)
                           .append('\n');
                    }
                }

                for (String name : repository.getConfig().getNames(section)) {
                    String value = repository.getConfig().getString(section, null, name);
                    out.append(section)
                       .append(".")
                       .append(name)
                       .append("=")
                       .append(value)
                       .append('\n');
                }
            }

            return out.toString().trim();
        }

        String prefixName = "git config --global user.name ";
        String prefixEmail = "git config --global user.email ";

        if (normalized.startsWith(prefixName)) {
            repository.getConfig().setString(
                ConfigConstants.CONFIG_USER_SECTION,
                null,
                ConfigConstants.CONFIG_KEY_NAME,
                stripQuotes(normalized.substring(prefixName.length()).trim())
            );
            repository.getConfig().save();
            return "Configuration enregistrée localement dans ce dépôt Android.";
        }

        if (normalized.startsWith(prefixEmail)) {
            repository.getConfig().setString(
                ConfigConstants.CONFIG_USER_SECTION,
                null,
                ConfigConstants.CONFIG_KEY_EMAIL,
                stripQuotes(normalized.substring(prefixEmail.length()).trim())
            );
            repository.getConfig().save();
            return "Configuration enregistrée localement dans ce dépôt Android.";
        }

        return "Cette variante de git config n'est pas encore prise en charge en mode réel.";
    }

    private String showCommit(Repository repository, String command) throws Exception {
        String[] parts = command.split("\\s+");
        String ref = parts[parts.length - 1];

        org.eclipse.jgit.lib.ObjectId id = repository.resolve(ref);
        if (id == null) return "fatal: bad object " + ref;

        try (org.eclipse.jgit.revwalk.RevWalk walk =
                 new org.eclipse.jgit.revwalk.RevWalk(repository)) {

            RevCommit commit = walk.parseCommit(id);

            return commit.getId().abbreviate(7).name() + " " +
                commit.getShortMessage() + "\nAuthor: " +
                commit.getAuthorIdent().getName();
        }
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2 &&
            value.startsWith("\"") &&
            value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }

    private String inferFullName(String url) {
        String clean = url;

        if (clean.endsWith(".git")) {
            clean = clean.substring(0, clean.length() - 4);
        }

        int slash = clean.indexOf("github.com/");
        if (slash >= 0) {
            return clean.substring(slash + "github.com/".length());
        }

        return clean;
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;

        if (file.isDirectory()) {
            File[] children = file.listFiles();

            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }

        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
