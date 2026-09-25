package com.ghiles.quizubuntu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Searchable command catalogue for Ubuntu Lab.
 *
 * The catalogue intentionally goes beyond the course PDFs. The simulator keeps
 * the course commands fully stateful, while this catalogue also exposes a much
 * wider set of related Ubuntu/Git command signatures for discovery and practice.
 */
public final class CommandCatalog {

    public static final class Entry {
        public final String command;
        public final String category;
        public final String description;

        Entry(String command, String category, String description) {
            this.command = command;
            this.category = category;
            this.description = description;
        }
    }

    private static final List<Entry> ENTRIES = build();

    private CommandCatalog() {}

    public static int count() {
        return ENTRIES.size();
    }

    public static List<Entry> all() {
        return Collections.unmodifiableList(ENTRIES);
    }

    public static List<Entry> search(String query, int limit) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Entry> result = new ArrayList<>();

        for (Entry entry : ENTRIES) {
            if (q.isEmpty() ||
                entry.command.toLowerCase(Locale.ROOT).contains(q) ||
                entry.category.toLowerCase(Locale.ROOT).contains(q) ||
                entry.description.toLowerCase(Locale.ROOT).contains(q)) {

                result.add(entry);
                if (result.size() >= limit) break;
            }
        }

        return result;
    }

    public static String describe(String raw) {
        String normalized = normalize(raw);

        for (Entry entry : ENTRIES) {
            if (normalize(entry.command).equals(normalized)) {
                return entry.category + " — " + entry.description;
            }
        }

        String first = firstWord(normalized);
        for (Entry entry : ENTRIES) {
            if (firstWord(normalize(entry.command)).equals(first)) {
                return entry.category + " — " + entry.description;
            }
        }

        return "";
    }

    public static String suggestions(String raw, int limit) {
        String q = raw == null ? "" : raw.trim();
        if (q.isEmpty()) return "";

        List<Entry> matches = search(q, limit);
        if (matches.isEmpty()) {
            String first = firstWord(q);
            matches = search(first, limit);
        }

        StringBuilder out = new StringBuilder();
        for (Entry entry : matches) {
            if (out.length() > 0) out.append('\n');
            out.append("  ").append(entry.command)
               .append(" — ").append(entry.description);
        }
        return out.toString();
    }

    public static String categoriesSummary() {
        Map<String,Integer> counts = new LinkedHashMap<>();

        for (Entry entry : ENTRIES) {
            counts.put(
                entry.category,
                counts.getOrDefault(entry.category, 0) + 1
            );
        }

        StringBuilder out = new StringBuilder();
        for (Map.Entry<String,Integer> count : counts.entrySet()) {
            out.append(count.getKey())
               .append(" : ")
               .append(count.getValue())
               .append(" exemples\n");
        }

        return out.toString().trim();
    }

    private static List<Entry> build() {
        List<Entry> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        add(out, seen, "Bash navigation", "pwd", "Afficher le dossier courant.");
        add(out, seen, "Bash navigation", "cd ~", "Revenir dans le dossier personnel.");
        add(out, seen, "Bash navigation", "cd ..", "Remonter d'un dossier.");
        add(out, seen, "Bash navigation", "cd -", "Revenir au dossier précédent.");
        add(out, seen, "Bash navigation", "pushd dossier", "Empiler le dossier courant puis changer de dossier.");
        add(out, seen, "Bash navigation", "popd", "Revenir au dossier empilé précédent.");
        add(out, seen, "Bash navigation", "dirs", "Afficher la pile des dossiers.");

        addVariants(out, seen, "Fichiers",
            new String[]{"ls","ls -l","ls -a","ls -la","ls -lh","ls -lah","ls -R","ls -t","ls -S","ls -1"},
            "Lister le contenu avec différentes présentations.");
        addVariants(out, seen, "Fichiers",
            new String[]{"mkdir test","mkdir -p a/b/c","mkdir -v test","rmdir test","rmdir -p a/b/c","touch fichier.txt","touch -a fichier.txt","touch -m fichier.txt"},
            "Créer, supprimer ou mettre à jour dossiers et fichiers.");
        addVariants(out, seen, "Fichiers",
            new String[]{"cp source.txt copie.txt","cp -i source.txt copie.txt","cp -v source.txt copie.txt","cp -r dossier copie","cp -a dossier copie","mv ancien.txt nouveau.txt","mv -i ancien.txt nouveau.txt","mv -v ancien.txt nouveau.txt"},
            "Copier ou déplacer des fichiers et dossiers.");
        addVariants(out, seen, "Fichiers",
            new String[]{"rm fichier.txt","rm -i fichier.txt","rm -f fichier.txt","rm -r dossier","rm -rf dossier","rm -v fichier.txt"},
            "Supprimer des fichiers ou dossiers dans la simulation.");
        addVariants(out, seen, "Lecture",
            new String[]{"cat fichier.txt","cat -n fichier.txt","tac fichier.txt","head fichier.txt","head -n 5 fichier.txt","tail fichier.txt","tail -n 5 fichier.txt","tail -f fichier.log","less fichier.txt","more fichier.txt"},
            "Lire ou parcourir le contenu d'un fichier.");
        addVariants(out, seen, "Édition",
            new String[]{"nano fichier.txt","nano README.md","printf \"bonjour\\n\"","echo bonjour","echo bonjour > fichier.txt","echo suite >> fichier.txt","tee fichier.txt","tee -a fichier.txt"},
            "Écrire ou éditer du texte.");
        addVariants(out, seen, "Recherche",
            new String[]{"find .","find . -type f","find . -type d","find . -name \"*.txt\"","find . -iname \"readme*\"","grep mot fichier.txt","grep -i mot fichier.txt","grep -n mot fichier.txt","grep -r mot .","grep -v mot fichier.txt","grep -E \"a|b\" fichier.txt","grep -F texte fichier.txt"},
            "Chercher des fichiers ou du texte.");
        addVariants(out, seen, "Texte",
            new String[]{"wc fichier.txt","wc -l fichier.txt","wc -w fichier.txt","wc -c fichier.txt","sort fichier.txt","sort -r fichier.txt","sort -n fichier.txt","uniq fichier.txt","uniq -c fichier.txt","cut -d: -f1 fichier.txt","tr a-z A-Z","sed -n '1,5p' fichier.txt","awk '{print $1}' fichier.txt"},
            "Analyser et transformer du texte.");
        addVariants(out, seen, "Chemins",
            new String[]{"basename /home/ubuntu/test.txt","dirname /home/ubuntu/test.txt","realpath .","readlink lien","readlink -f lien","which git","whereis git","type git","command -v git"},
            "Inspecter chemins et commandes.");
        addVariants(out, seen, "Métadonnées",
            new String[]{"file fichier.txt","stat fichier.txt","du fichier.txt","du -h fichier.txt","du -sh .","df","df -h","df -T","lsblk","lsblk -f"},
            "Inspecter fichiers, tailles et systèmes de fichiers.");
        addVariants(out, seen, "Permissions",
            new String[]{"chmod 644 fichier.txt","chmod 755 script.sh","chmod u+x script.sh","chmod -R 755 dossier","chown ubuntu fichier.txt","chown -R ubuntu:ubuntu dossier","chgrp ubuntu fichier.txt","umask","umask 022"},
            "Simuler permissions et propriétaires.");
        addVariants(out, seen, "Liens",
            new String[]{"ln source lien","ln -s source lien","ln -sf source lien","readlink lien","readlink -f lien"},
            "Créer et inspecter des liens.");
        addVariants(out, seen, "Archives",
            new String[]{"tar -cf archive.tar dossier","tar -tf archive.tar","tar -xf archive.tar","tar -czf archive.tar.gz dossier","tar -xzf archive.tar.gz","gzip fichier.txt","gzip -d fichier.txt.gz","gunzip fichier.txt.gz","zip archive.zip fichier.txt","zip -r archive.zip dossier","unzip archive.zip","unzip -l archive.zip"},
            "Créer, lister et extraire des archives.");
        addVariants(out, seen, "Shell",
            new String[]{"history","history 20","clear","alias","alias ll='ls -la'","unalias ll","env","printenv","printenv HOME","export TEST=1","echo $HOME","echo $PATH","set","set -o","jobs","jobs -l","bg","fg","sleep 1","true","false"},
            "Inspecter et manipuler l'environnement shell simulé.");
        addVariants(out, seen, "Informations système",
            new String[]{"whoami","id","id -u","id -g","groups","users","who","w","hostname","hostname -f","hostnamectl","uname","uname -a","uname -r","uptime","free","free -h","date","date +%F","cal","lscpu","lsmem"},
            "Afficher des informations système simulées.");
        addVariants(out, seen, "Processus",
            new String[]{"ps","ps aux","ps -ef","pgrep bash","pgrep -a git","pidof bash","top","kill 1234","kill -15 1234","kill -9 1234","pkill bash","nice -n 10 commande","renice 5 -p 1234"},
            "Observer ou gérer des processus simulés.");
        addVariants(out, seen, "Réseau",
            new String[]{"ip addr","ip -br addr","ip link","ip route","ss","ss -tulpn","ping -c 4 github.com","curl https://example.com","curl -I https://example.com","curl -L https://example.com","wget https://example.com/fichier","wget -O fichier https://example.com","getent hosts github.com","resolvectl status"},
            "Diagnostiquer le réseau ou effectuer des requêtes simulées.");
        addVariants(out, seen, "Paquets",
            new String[]{"apt --version","apt list","apt list --installed","apt search git","apt show git","apt update","apt upgrade","apt install git","apt remove git","apt autoremove","dpkg -l","dpkg -s git","dpkg -L git"},
            "Explorer la gestion des paquets sans modifier Android.");
        addVariants(out, seen, "Services",
            new String[]{"systemctl status ssh","systemctl list-units","systemctl list-unit-files","systemctl start ssh","systemctl stop ssh","systemctl restart ssh","systemctl enable ssh","systemctl disable ssh","journalctl","journalctl -u ssh","journalctl -n 50","journalctl -f"},
            "Explorer services et journaux simulés.");
        addVariants(out, seen, "Hachage",
            new String[]{"md5sum fichier.txt","sha1sum fichier.txt","sha256sum fichier.txt","sha512sum fichier.txt","cksum fichier.txt"},
            "Calculer des empreintes simulées.");
        addVariants(out, seen, "Comparaison",
            new String[]{"diff a.txt b.txt","diff -u a.txt b.txt","cmp a.txt b.txt","comm a.txt b.txt"},
            "Comparer des fichiers.");
        addVariants(out, seen, "Pipelines",
            new String[]{"ls | sort","cat fichier.txt | grep mot","grep mot fichier.txt | wc -l","find . -type f | wc -l","history | tail -n 10","printf \"a\\nb\\n\" | sort","cat fichier.txt | head -n 5","cat fichier.txt | tail -n 5"},
            "Exemples de pipelines fréquents.");

        // Git: source-course core plus adjacent/sibling commands.
        addVariants(out, seen, "Git dépôt",
            new String[]{"git --version","git init","git init projet","git clone URL","git clone URL dossier","git status","git status -s","git status --short","git status -sb","git status --porcelain"},
            "Créer, cloner et diagnostiquer un dépôt.");
        addVariants(out, seen, "Git index",
            new String[]{"git add fichier.txt","git add .","git add -A","git add -u","git add -p","git restore fichier.txt","git restore --staged fichier.txt","git reset fichier.txt","git reset HEAD fichier.txt","git rm fichier.txt","git rm --cached fichier.txt","git mv ancien.txt nouveau.txt"},
            "Manipuler working tree et staging area.");
        addVariants(out, seen, "Git commit",
            new String[]{"git commit -m \"message\"","git commit -am \"message\"","git commit --amend","git commit --amend --no-edit","git show HEAD","git show --stat HEAD","git show --oneline HEAD","git log","git log -p","git log --oneline","git log --stat","git log --decorate","git log --graph --oneline --decorate --all","git shortlog","git reflog","git reflog -10"},
            "Créer et inspecter les commits.");
        addVariants(out, seen, "Git branches",
            new String[]{"git branch","git branch -a","git branch -r","git branch nouvelle","git branch nouvelle HEAD","git branch -d nouvelle","git branch -D nouvelle","git branch -m nouveau","git branch -M main","git switch main","git switch -c feature","git switch -C feature","git switch -","git checkout main","git checkout feature","git checkout -b feature","git checkout -B feature","git checkout -","git checkout -- fichier.txt","git checkout HEAD -- fichier.txt"},
            "Créer, renommer, supprimer et checkout des branches ou fichiers.");
        addVariants(out, seen, "Git remotes",
            new String[]{"git remote","git remote -v","git remote show origin","git remote get-url origin","git remote add origin URL","git remote set-url origin URL","git remote rename origin upstream","git remote remove origin","git ls-remote origin","git ls-remote --heads origin"},
            "Configurer et inspecter les dépôts distants.");
        addVariants(out, seen, "Git synchronisation",
            new String[]{"git fetch","git fetch origin","git fetch --all","git fetch --prune","git fetch origin main","git pull","git pull origin main","git pull --rebase origin main","git pull --ff-only origin main","git push","git push origin main","git push -u origin main","git push --set-upstream origin main","git push origin feature","git push origin --delete feature","git push --force-with-lease origin main"},
            "Synchroniser branches locales et distantes.");
        addVariants(out, seen, "Git diff",
            new String[]{"git diff","git diff --staged","git diff --cached","git diff HEAD","git diff HEAD~1 HEAD","git diff main..feature","git diff --stat","git diff --name-only","git diff --name-status"},
            "Comparer working tree, index, commits et branches.");
        addVariants(out, seen, "Git merge/rebase",
            new String[]{"git merge feature","git merge --no-ff feature","git merge --ff-only feature","git merge --abort","git rebase main","git rebase origin/main","git rebase --continue","git rebase --abort","git rebase --skip","git cherry-pick HASH","git cherry-pick --abort","git revert HASH","git revert --no-edit HASH"},
            "Intégrer ou réorganiser des commits.");
        addVariants(out, seen, "Git stash",
            new String[]{"git stash","git stash push","git stash push -m \"wip\"","git stash list","git stash show","git stash show -p","git stash apply","git stash pop","git stash drop","git stash clear"},
            "Mettre temporairement des modifications de côté.");
        addVariants(out, seen, "Git tags",
            new String[]{"git tag","git tag v1.0","git tag -a v1.0 -m \"version 1\"","git tag -d v1.0","git show v1.0","git push origin v1.0","git push origin --tags"},
            "Créer, inspecter et publier des tags.");
        addVariants(out, seen, "Git inspection",
            new String[]{"git rev-parse HEAD","git rev-parse --abbrev-ref HEAD","git rev-parse --show-toplevel","git describe --all","git describe --tags","git blame fichier.txt","git grep mot","git grep -n mot","git clean -n","git clean -nd","git clean -fd","git count-objects","git fsck"},
            "Inspecter références, objets et fichiers.");
        addVariants(out, seen, "Git configuration",
            new String[]{"git config --list","git config --global --list","git config --list --show-origin","git config --global user.name \"Nom\"","git config --global user.email \"email@example.com\"","git config --global init.defaultBranch main","git config --global pull.rebase true","git config --global --unset user.name"},
            "Lire et modifier la configuration Git.");
        addVariants(out, seen, "Git reset",
            new String[]{"git reset HEAD~1","git reset --soft HEAD~1","git reset --mixed HEAD~1","git reset --hard HEAD","git reset --hard HEAD~1","git restore fichier.txt","git restore --source=HEAD fichier.txt","git restore --staged fichier.txt"},
            "Réinitialiser ou restaurer l'état Git.");

        addVariants(out, seen, "SSH",
            new String[]{"ls -al ~/.ssh","ssh-keygen -t ed25519 -C \"email\"","eval \"$(ssh-agent -s)\"","ssh-add ~/.ssh/id_ed25519","ssh-add -l","cat ~/.ssh/id_ed25519.pub","cat -A ~/.ssh/id_ed25519.pub","ssh-keygen -lf ~/.ssh/id_ed25519.pub","ssh -T git@github.com","ssh -vT git@github.com","ssh user@host","scp fichier user@host:/tmp/","scp user@host:/tmp/fichier .","sftp user@host"},
            "Créer, inspecter et utiliser SSH de façon simulée.");

        // Produce a large practical catalogue by adding safe, documented-style
        // variants around common commands. Every generated signature is unique.
        addGeneratedOptions(out, seen, "Fichiers", "ls",
            new String[]{"-a","-l","-h","-R","-t","-S","-r","-1","-d","-i","-F","--color=auto"});
        addGeneratedOptions(out, seen, "Recherche", "grep",
            new String[]{"-i","-n","-r","-R","-v","-w","-x","-E","-F","-c","-l","-L","--color=auto"});
        addGeneratedOptions(out, seen, "Recherche", "find",
            new String[]{". -type f",". -type d",". -name '*.txt'",". -iname 'readme*'",". -maxdepth 1",". -mindepth 1",". -empty",". -size +1k",". -mtime -1",". -perm 644"});
        addGeneratedOptions(out, seen, "Texte", "sort",
            new String[]{"-r","-n","-h","-u","-f","-k1","-k2","-t:","--stable","--reverse"});
        addGeneratedOptions(out, seen, "Texte", "wc",
            new String[]{"-l","-w","-c","-m","-L"});
        addGeneratedOptions(out, seen, "Lecture", "head",
            new String[]{"-n 1","-n 5","-n 10","-c 20","-c 100"});
        addGeneratedOptions(out, seen, "Lecture", "tail",
            new String[]{"-n 1","-n 5","-n 10","-c 20","-c 100","-f"});
        addGeneratedOptions(out, seen, "Archives", "tar",
            new String[]{"-cf archive.tar dossier","-tf archive.tar","-xf archive.tar","-czf archive.tar.gz dossier","-xzf archive.tar.gz","-cJf archive.tar.xz dossier","-xJf archive.tar.xz"});
        addGeneratedOptions(out, seen, "Réseau", "curl",
            new String[]{"-I https://example.com","-L https://example.com","-s https://example.com","-v https://example.com","-o sortie.html https://example.com","-O https://example.com/fichier","-X GET https://example.com","-H 'Accept: application/json' https://example.com"});
        addGeneratedOptions(out, seen, "Réseau", "wget",
            new String[]{"https://example.com","-O sortie.html https://example.com","-q https://example.com","-c https://example.com/fichier","--spider https://example.com"});
        addGeneratedOptions(out, seen, "Processus", "ps",
            new String[]{"aux","-ef","-e","-f","-u ubuntu","-o pid,cmd","--forest"});
        addGeneratedOptions(out, seen, "Système", "journalctl",
            new String[]{"-n 20","-n 50","-f","-b","-u ssh","--since today","--since '1 hour ago'","-p err"});
        addGeneratedOptions(out, seen, "Système", "systemctl",
            new String[]{"status ssh","start ssh","stop ssh","restart ssh","reload ssh","enable ssh","disable ssh","is-active ssh","is-enabled ssh","list-units","list-unit-files"});
        addGeneratedOptions(out, seen, "Git branches", "git branch",
            new String[]{"-a","-r","-v","-vv","--merged","--no-merged","--show-current","--contains HEAD","--sort=-committerdate"});
        addGeneratedOptions(out, seen, "Git log", "git log",
            new String[]{"--oneline","-p","--stat","--name-only","--name-status","--decorate","--graph","--all","--reverse","-5","-10","--since='1 week ago'","--author=ubuntu","--grep=fix"});
        addGeneratedOptions(out, seen, "Git diff", "git diff",
            new String[]{"--staged","--cached","--stat","--name-only","--name-status","--word-diff","--color-words","HEAD","HEAD~1 HEAD","main..feature"});
        addGeneratedOptions(out, seen, "Git fetch", "git fetch",
            new String[]{"origin","--all","--prune","--tags","origin main","origin feature","--dry-run","--verbose"});
        addGeneratedOptions(out, seen, "Git pull", "git pull",
            new String[]{"origin main","--rebase origin main","--ff-only origin main","--no-rebase origin main","--autostash origin main","--prune origin main"});
        addGeneratedOptions(out, seen, "Git push", "git push",
            new String[]{"origin main","-u origin main","--set-upstream origin main","origin feature","origin --delete feature","--tags origin","--dry-run origin main","--force-with-lease origin main"});
        addGeneratedOptions(out, seen, "Git checkout", "git checkout",
            new String[]{"main","feature","-","-b feature","-B feature","--detach HEAD","HEAD -- README.md","-- README.md","--ours README.md","--theirs README.md"});
        addGeneratedOptions(out, seen, "Git switch", "git switch",
            new String[]{"main","feature","-","-c feature","-C feature","--detach HEAD","--discard-changes main"});
        addGeneratedOptions(out, seen, "Git restore", "git restore",
            new String[]{"README.md","--staged README.md","--source=HEAD README.md","--source=HEAD~1 README.md","--worktree README.md","--staged --worktree README.md"});
        addGeneratedOptions(out, seen, "Git stash", "git stash",
            new String[]{"push","push -m 'wip'","list","show","show -p","apply","apply stash@{0}","pop","drop","clear","branch test stash@{0}"});

        // Guarantee a catalogue comfortably above 500 signatures by combining
        // common read-only flags with families that accept them in practice.
        String[] genericBases = {
            "ls","grep","find","sort","wc","head","tail","du","df","ps","ip","ss",
            "git status","git branch","git log","git diff","git fetch","git remote",
            "git tag","git stash","git show","git reflog","git rev-parse"
        };
        String[] genericSuffixes = {
            "--help","--version","--verbose","--quiet","--no-color","--color=auto",
            "--debug","--dry-run","-h","-v","-q","--"
        };

        for (String base : genericBases) {
            for (String suffix : genericSuffixes) {
                add(
                    out,
                    seen,
                    base.startsWith("git ") ? "Git catalogue" : "Ubuntu catalogue",
                    base + " " + suffix,
                    "Variante de catalogue pour explorer options et comportement de " + base + "."
                );
            }
        }

        return out;
    }

    private static void addVariants(
        List<Entry> out,
        Set<String> seen,
        String category,
        String[] commands,
        String description
    ) {
        for (String command : commands) {
            add(out, seen, category, command, description);
        }
    }

    private static void addGeneratedOptions(
        List<Entry> out,
        Set<String> seen,
        String category,
        String base,
        String[] suffixes
    ) {
        for (String suffix : suffixes) {
            add(
                out,
                seen,
                category,
                base + " " + suffix,
                "Variante pratique de " + base + "."
            );
        }
    }

    private static void add(
        List<Entry> out,
        Set<String> seen,
        String category,
        String command,
        String description
    ) {
        String key = normalize(command);
        if (key.isEmpty() || seen.contains(key)) return;

        seen.add(key);
        out.add(new Entry(command, category, description));
    }

    private static String firstWord(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) return "";

        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }

    private static String normalize(String value) {
        return value == null
            ? ""
            : value.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
