package com.ghiles.quizubuntu;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SimulatorActivity extends Activity {

    static class Scenario {
        final String title;
        final String objective;
        final List<String> accepted;
        final String[] choices;
        final String hint;

        Scenario(String title, String objective, String hint, String[] choices, String... accepted) {
            this.title = title;
            this.objective = objective;
            this.hint = hint;
            this.choices = choices;
            this.accepted = Arrays.asList(accepted);
        }
    }

    private final List<Scenario> scenarios = Arrays.asList(
        new Scenario(
            "Se repérer",
            "Tu viens d'ouvrir un terminal. Affiche le chemin absolu du dossier courant.",
            "La commande signifie Print Working Directory.",
            new String[]{"pwd", "ls", "cd ~", "history"},
            "pwd"
        ),
        new Scenario(
            "Inspecter le dossier",
            "Affiche les fichiers cachés et les détails du dossier courant.",
            "Combine l'affichage détaillé et les fichiers cachés.",
            new String[]{"ls", "ls -l", "ls -la", "pwd"},
            "ls -la"
        ),
        new Scenario(
            "Créer une arborescence",
            "Crée les dossiers projet/src en une seule commande, même si projet n'existe pas.",
            "L'option -p crée les répertoires parents nécessaires.",
            new String[]{"mkdir projet/src", "mkdir -p projet/src", "touch projet/src", "cp -r projet/src"},
            "mkdir -p projet/src"
        ),
        new Scenario(
            "Créer un fichier",
            "Crée un fichier vide README.md.",
            "Cette commande sert à créer un fichier vide.",
            new String[]{"touch README.md", "mkdir README.md", "cat README.md", "nano -r README.md"},
            "touch README.md"
        ),
        new Scenario(
            "Initialiser Git",
            "Tu es dans ton projet. Transforme le dossier courant en dépôt Git local.",
            "Il faut initialiser les métadonnées Git.",
            new String[]{"git init", "git add .", "git clone", "git status"},
            "git init"
        ),
        new Scenario(
            "Vérifier Git",
            "Tu as modifié des fichiers. Vérifie l'état du working directory et de la staging area.",
            "Cette commande indique les fichiers modifiés, staged et non suivis.",
            new String[]{"git log", "git status", "git push", "git remote -v"},
            "git status"
        ),
        new Scenario(
            "Préparer un commit",
            "Prépare README.md pour le prochain commit.",
            "Place l'état actuel du fichier dans la staging area.",
            new String[]{"git add README.md", "git commit README.md", "git push README.md", "git log README.md"},
            "git add README.md"
        ),
        new Scenario(
            "Créer un commit",
            "Crée un commit avec le message Ajoute le README.",
            "L'option -m permet de fournir le message.",
            new String[]{"git commit -m \"Ajoute le README\"", "git add -m \"Ajoute le README\"", "git push -m \"Ajoute le README\"", "git status -m \"Ajoute le README\""},
            "git commit -m \"Ajoute le README\""
        ),
        new Scenario(
            "Pas encore relié à GitHub",
            "Tu veux vérifier si ton dépôt local possède déjà un remote GitHub. Quelle commande l'affiche ?",
            "Cette commande affiche les remotes et leurs URL.",
            new String[]{"git status", "git remote -v", "git branch -a", "git log -p"},
            "git remote -v"
        ),
        new Scenario(
            "Relier le dépôt",
            "Aucun remote n'est configuré. Ajoute origin vers git@github.com:USER/REPO.git.",
            "La syntaxe est : git remote add NOM URL.",
            new String[]{"git remote add origin git@github.com:USER/REPO.git", "git remote -v origin", "git push origin", "git init origin"},
            "git remote add origin git@github.com:USER/REPO.git"
        ),
        new Scenario(
            "Créer une clé SSH",
            "Tu n'as pas encore de clé SSH. Crée une paire de clés Ed25519 avec un commentaire email.",
            "La clé publique aura l'extension .pub ; la clé privée ne doit pas être partagée.",
            new String[]{"ssh-keygen -t ed25519 -C \"email\"", "ssh-add -t ed25519", "git keygen ed25519", "ssh -T ed25519"},
            "ssh-keygen -t ed25519 -C \"email\""
        ),
        new Scenario(
            "Tester GitHub en SSH",
            "Teste l'authentification SSH auprès de GitHub.",
            "La connexion de test utilise l'utilisateur git sur github.com.",
            new String[]{"ssh -T git@github.com", "git status github.com", "ssh-add github.com", "git remote -T"},
            "ssh -T git@github.com"
        ),
        new Scenario(
            "Publier main",
            "Publie pour la première fois la branche main sur origin en configurant le suivi.",
            "L'option -u configure l'upstream.",
            new String[]{"git push -u origin main", "git push main origin", "git remote main", "git add origin main"},
            "git push -u origin main"
        ),
        new Scenario(
            "Synchroniser avant de pousser",
            "Le dépôt distant possède des commits absents localement. Récupère-les puis rejoue tes commits locaux au-dessus.",
            "Le support utilise pull avec rebase sur origin/main.",
            new String[]{"git pull --rebase origin main", "git push --force origin main", "git clone origin main", "git status --rebase"},
            "git pull --rebase origin main"
        ),
        new Scenario(
            "Créer une branche",
            "Crée une branche feature et bascule immédiatement dessus.",
            "switch -c crée la branche puis place HEAD dessus.",
            new String[]{"git switch feature", "git switch -c feature", "git branch -d feature", "git push feature"},
            "git switch -c feature"
        ),
        new Scenario(
            "Voir les changements",
            "Affiche les changements non staged du working directory.",
            "Cette commande compare le working directory à la staging area.",
            new String[]{"git diff", "git diff --staged", "git log -p", "git status -s"},
            "git diff"
        ),
        new Scenario(
            "Voir ce qui est staged",
            "Tu as préparé des changements avec git add. Affiche ce qui est déjà prêt pour le prochain commit.",
            "Cette commande compare la staging area à HEAD.",
            new String[]{"git diff --staged", "git diff", "git log", "git remote -v"},
            "git diff --staged"
        ),
        new Scenario(
            "Résoudre un conflit",
            "Tu as édité README.md et supprimé les marqueurs de conflit. Marque maintenant le fichier comme résolu dans l'index.",
            "Après la résolution manuelle, git add enregistre la décision dans l'index.",
            new String[]{"git add README.md", "git push README.md", "git status README.md", "git rm README.md"},
            "git add README.md"
        )
    );

    private SharedPreferences prefs;
    private TextView objectiveView;
    private TextView terminalView;
    private TextView scoreView;
    private TextView modeView;
    private LinearLayout choicesBox;
    private EditText commandInput;
    private Button executeButton;

    private int scenarioIndex = 0;
    private int labPoints = 0;
    private boolean typingMode = true;
    private final List<String> history = new ArrayList<>();

    private String cwd = "/home/ubuntu";
    private final Set<String> files = new HashSet<>();
    private final Set<String> dirs = new HashSet<>();
    private final Map<String,String> contents = new HashMap<>();
    private boolean gitInit = false;
    private boolean staged = false;
    private boolean committed = false;
    private String remoteOrigin = "";
    private boolean sshKey = false;
    private String branch = "main";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("quiz_progress", MODE_PRIVATE);
        labPoints = prefs.getInt("labPoints", 0);
        typingMode = prefs.getBoolean("simTypingMode", true);
        resetMachine();
        buildUi();
        showScenario();
    }

    private void resetMachine() {
        cwd = "/home/ubuntu";
        files.clear();
        dirs.clear();
        contents.clear();
        dirs.add("/home/ubuntu");
        files.add("notes.txt");
        contents.put("notes.txt", "Notes Ubuntu & Git");
        gitInit = false;
        staged = false;
        committed = false;
        remoteOrigin = "";
        sshKey = false;
        branch = "main";
        history.clear();
        appendTerminal("Ubuntu 24.04 LTS — simulation pédagogique");
        appendTerminal("Tape une commande. Rien n'est exécuté sur ton vrai téléphone.");
    }

    private void buildUi() {
        getWindow().setStatusBarColor(Color.rgb(48, 10, 36));
        getWindow().setNavigationBarColor(Color.rgb(17, 17, 20));

        ScrollView outer = new ScrollView(this);
        outer.setBackgroundColor(Color.rgb(34, 8, 28));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(10), dp(14), dp(28));
        outer.addView(root);

        LinearLayout topbar = new LinearLayout(this);
        topbar.setOrientation(LinearLayout.HORIZONTAL);
        topbar.setGravity(Gravity.CENTER_VERTICAL);
        topbar.setPadding(dp(8), dp(5), dp(8), dp(8));

        TextView activities = label("Activités", 13, true, Color.WHITE);
        topbar.addView(activities, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView clock = label("Ubuntu  •  Terminal  •  Wi-Fi  •  100%", 12, false, Color.rgb(230,230,230));
        topbar.addView(clock);
        root.addView(topbar);

        LinearLayout header = panel(Color.rgb(68, 18, 52), 18);
        TextView title = label(">_  PC Ubuntu simulé", 24, true, Color.WHITE);
        header.addView(title);
        TextView subtitle = label("Objectifs guidés • QCM ou commandes libres • terminal sécurisé", 14, false, Color.rgb(226,210,222));
        subtitle.setPadding(0, dp(6), 0, 0);
        header.addView(subtitle);
        root.addView(header, marginTop(4));

        scoreView = label("", 14, true, Color.WHITE);
        scoreView.setPadding(dp(4), dp(12), dp(4), dp(8));
        root.addView(scoreView);

        LinearLayout objectivePanel = panel(Color.rgb(247, 244, 247), 18);
        TextView objectiveLabel = label("OBJECTIF", 12, true, Color.rgb(226,83,45));
        objectivePanel.addView(objectiveLabel);
        objectiveView = label("", 18, true, Color.rgb(30,30,34));
        objectiveView.setPadding(0, dp(7), 0, dp(7));
        objectivePanel.addView(objectiveView);

        Button hint = smallButton("Indice");
        hint.setOnClickListener(v -> {
            Scenario s = scenarios.get(scenarioIndex);
            appendTerminal("[indice] " + s.hint);
        });
        objectivePanel.addView(hint);
        root.addView(objectivePanel, marginTop(6));

        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        modeRow.setGravity(Gravity.CENTER_VERTICAL);
        modeRow.setPadding(0, dp(10), 0, dp(7));

        modeView = label("", 13, true, Color.WHITE);
        modeRow.addView(modeView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button switchMode = smallButton("Changer de mode");
        switchMode.setOnClickListener(v -> {
            typingMode = !typingMode;
            prefs.edit().putBoolean("simTypingMode", typingMode).apply();
            renderInteraction();
        });
        modeRow.addView(switchMode);
        root.addView(modeRow);

        LinearLayout terminalWindow = panel(Color.rgb(24, 24, 27), 16);

        LinearLayout terminalTitle = new LinearLayout(this);
        terminalTitle.setOrientation(LinearLayout.HORIZONTAL);
        terminalTitle.setGravity(Gravity.CENTER_VERTICAL);
        TextView dots = label("●  ●  ●", 12, true, Color.rgb(226,83,45));
        terminalTitle.addView(dots);
        TextView tty = label("  ubuntu@academy: " + cwd, 12, false, Color.rgb(190,190,194));
        terminalTitle.addView(tty);
        terminalWindow.addView(terminalTitle);

        terminalView = label("", 14, false, Color.rgb(226,226,230));
        terminalView.setTypeface(Typeface.MONOSPACE);
        terminalView.setText(historyText());
        terminalView.setPadding(0, dp(10), 0, dp(8));
        terminalWindow.addView(terminalView);
        root.addView(terminalWindow);

        choicesBox = new LinearLayout(this);
        choicesBox.setOrientation(LinearLayout.VERTICAL);
        choicesBox.setPadding(0, dp(10), 0, 0);
        root.addView(choicesBox);

        LinearLayout commandRow = new LinearLayout(this);
        commandRow.setOrientation(LinearLayout.HORIZONTAL);
        commandRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView prompt = label("$", 18, true, Color.WHITE);
        prompt.setPadding(dp(4), 0, dp(7), 0);
        commandRow.addView(prompt);

        commandInput = new EditText(this);
        commandInput.setSingleLine(true);
        commandInput.setTextColor(Color.WHITE);
        commandInput.setHintTextColor(Color.rgb(155,155,160));
        commandInput.setHint("écris ta commande...");
        commandInput.setTypeface(Typeface.MONOSPACE);
        commandInput.setTextSize(15f);
        commandInput.setInputType(InputType.TYPE_CLASS_TEXT);
        commandInput.setBackground(rounded(Color.rgb(42,42,47), 12, Color.rgb(85,85,92)));
        commandInput.setPadding(dp(12), dp(9), dp(12), dp(9));
        commandInput.setImeOptions(EditorInfo.IME_ACTION_GO);
        commandInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                executeTyped();
                return true;
            }
            return false;
        });
        commandRow.addView(commandInput, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        executeButton = accentButton("Exécuter");
        executeButton.setOnClickListener(v -> executeTyped());
        LinearLayout.LayoutParams execParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        execParams.leftMargin = dp(7);
        commandRow.addView(executeButton, execParams);
        root.addView(commandRow, marginTop(8));

        Button next = accentButton("Passer à l'objectif suivant");
        next.setOnClickListener(v -> {
            scenarioIndex = (scenarioIndex + 1) % scenarios.size();
            showScenario();
        });
        root.addView(next, marginTop(12));

        Button reset = smallFullButton("Réinitialiser le PC simulé");
        reset.setOnClickListener(v -> {
            resetMachine();
            terminalView.setText(historyText());
            showScenario();
        });
        root.addView(reset);

        Button back = smallFullButton("← Retour à l'Academy");
        back.setOnClickListener(v -> finish());
        root.addView(back);

        setContentView(outer);
    }

    private void showScenario() {
        Scenario s = scenarios.get(scenarioIndex);
        objectiveView.setText(s.title + "\n\n" + s.objective);
        scoreView.setText("Lab XP : " + labPoints + "   •   Mission " + (scenarioIndex + 1) + " / " + scenarios.size());
        appendTerminal("");
        appendTerminal("[mission] " + s.title);
        renderInteraction();
    }

    private void renderInteraction() {
        modeView.setText(typingMode ? "MODE : commande libre" : "MODE : 4 propositions");
        commandInput.setVisibility(typingMode ? View.VISIBLE : View.GONE);
        executeButton.setVisibility(typingMode ? View.VISIBLE : View.GONE);
        choicesBox.removeAllViews();

        if (!typingMode) {
            Scenario s = scenarios.get(scenarioIndex);
            List<String> shuffled = new ArrayList<>(Arrays.asList(s.choices));
            Collections.shuffle(shuffled);
            for (String choice : shuffled) {
                Button b = choiceButton(choice);
                b.setOnClickListener(v -> runForMission(choice));
                choicesBox.addView(b);
            }
        }
    }

    private void executeTyped() {
        String cmd = commandInput.getText().toString().trim();
        if (cmd.isEmpty()) return;
        commandInput.setText("");
        runForMission(cmd);
    }

    private void runForMission(String command) {
        String normalized = normalize(command);
        appendTerminal("ubuntu@academy:" + shortCwd() + "$ " + command);
        String output = executeSimulated(command);
        if (!output.isEmpty()) appendTerminal(output);

        Scenario s = scenarios.get(scenarioIndex);
        boolean success = false;
        for (String accepted : s.accepted) {
            if (normalized.equals(normalize(accepted))) {
                success = true;
                break;
            }
        }

        if (success) {
            appendTerminal("✓ Objectif réussi : +150 Lab XP");
            labPoints += 150;
            prefs.edit()
                .putInt("labPoints", labPoints)
                .putInt("totalPoints", prefs.getInt("totalPoints", 0) + 150)
                .apply();
            scoreView.setText("Lab XP : " + labPoints + "   •   Mission réussie");
            scenarioIndex = (scenarioIndex + 1) % scenarios.size();
            objectiveView.postDelayed(this::showScenario, 350);
        } else {
            appendTerminal("↳ La commande a été simulée, mais ce n'est pas encore la réponse attendue pour l'objectif.");
        }

        terminalView.setText(historyText());
    }

    private String executeSimulated(String raw) {
        String command = raw.trim();
        String n = normalize(command);
        history.add(command);

        if ("clear".equals(n)) {
            history.clear();
            return "";
        }
        if ("history".equals(n)) {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < history.size(); i++) b.append(i + 1).append("  ").append(history.get(i)).append("\n");
            return b.toString().trim();
        }
        if ("pwd".equals(n)) return cwd;
        if ("ls".equals(n)) return listBasic(false);
        if ("ls -l".equals(n)) return listLong(false);
        if ("ls -la".equals(n) || "ls -al".equals(n)) return listLong(true);

        if (n.equals("cd ~")) {
            cwd = "/home/ubuntu";
            return "";
        }
        if (n.equals("cd ..")) {
            if (!"/".equals(cwd)) {
                int cut = cwd.lastIndexOf('/');
                cwd = cut <= 0 ? "/" : cwd.substring(0, cut);
            }
            return "";
        }
        if (n.startsWith("cd ")) {
            String target = command.substring(3).trim();
            if (dirs.contains(target) || dirs.contains(cwd + "/" + target) || "projet".equals(target) || "src".equals(target)) {
                cwd = target.startsWith("/") ? target : ("/".equals(cwd) ? "" : cwd) + "/" + target;
                dirs.add(cwd);
                return "";
            }
            return "bash: cd: " + target + ": Aucun fichier ou dossier de ce type";
        }

        if (n.startsWith("mkdir -p ")) {
            String p = command.substring(command.toLowerCase(Locale.ROOT).indexOf("-p") + 2).trim();
            dirs.add(p);
            String[] parts = p.split("/");
            String acc = "";
            for (String part : parts) {
                if (part.isEmpty()) continue;
                acc = acc.isEmpty() ? part : acc + "/" + part;
                dirs.add(acc);
            }
            return "";
        }
        if (n.startsWith("mkdir ")) {
            dirs.add(command.substring(6).trim());
            return "";
        }
        if (n.startsWith("touch ")) {
            files.add(command.substring(6).trim());
            return "";
        }
        if (n.startsWith("cat ")) {
            String f = command.substring(4).trim();
            if ("~/.ssh/id_ed25519.pub".equals(f)) {
                return sshKey ? "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAA... email" :
                    "cat: /home/ubuntu/.ssh/id_ed25519.pub: Aucun fichier ou dossier de ce type";
            }
            return files.contains(f) ? contents.getOrDefault(f, "") :
                "cat: " + f + ": Aucun fichier ou dossier de ce type";
        }
        if (n.startsWith("echo ") && (command.contains(">>") || command.contains(">"))) {
            boolean append = command.contains(">>");
            String sep = append ? ">>" : ">";
            String[] parts = command.split(append ? ">>" : ">", 2);
            if (parts.length == 2) {
                String text = parts[0].substring(4).trim().replaceAll("^\\\"|\\\"$", "");
                String f = parts[1].trim();
                files.add(f);
                contents.put(f, append ? contents.getOrDefault(f, "") + (contents.containsKey(f) ? "\n" : "") + text : text);
            }
            return "";
        }
        if (n.startsWith("rm -r ")) {
            String target = command.substring(command.toLowerCase(Locale.ROOT).indexOf("-r") + 2).trim();
            dirs.remove(target);
            return "";
        }

        if ("git init".equals(n)) {
            gitInit = true;
            return "Dépôt Git vide initialisé dans " + cwd + "/.git/";
        }
        if ("git status".equals(n)) {
            if (!gitInit) return "fatal: not a git repository";
            String state = staged ? "Changes to be committed:" : "On branch " + branch + "\nworking tree simulé";
            return "On branch " + branch + "\n" + state;
        }
        if (n.startsWith("git add ")) {
            if (!gitInit) return "fatal: not a git repository";
            staged = true;
            return "";
        }
        if (n.startsWith("git commit -m ")) {
            if (!gitInit) return "fatal: not a git repository";
            if (!staged) return "nothing added to commit";
            committed = true;
            staged = false;
            return "[" + branch + " a1b2c3d] " + command.substring(command.indexOf("-m") + 2).trim().replace("\"", "");
        }
        if ("git log".equals(n) || "git log -p".equals(n)) {
            return committed ? "commit a1b2c3d\nAuthor: ubuntu\n\n    Commit simulé" : "fatal: your current branch has no commits yet";
        }
        if ("git remote -v".equals(n)) {
            return remoteOrigin.isEmpty() ? "" :
                "origin  " + remoteOrigin + " (fetch)\norigin  " + remoteOrigin + " (push)";
        }
        if (n.startsWith("git remote add origin ")) {
            remoteOrigin = command.substring("git remote add origin ".length()).trim();
            return "";
        }
        if (n.startsWith("git remote set-url origin ")) {
            remoteOrigin = command.substring("git remote set-url origin ".length()).trim();
            return "";
        }
        if (n.startsWith("git switch -c ")) {
            branch = command.substring("git switch -c ".length()).trim();
            return "Switched to a new branch '" + branch + "'";
        }
        if ("git diff".equals(n)) return "- ancienne ligne\n+ nouvelle ligne";
        if ("git diff --staged".equals(n)) return staged ? "- ancienne version\n+ version staged" : "";
        if ("git pull --rebase origin main".equals(n)) {
            return "From github.com:USER/REPO\nRebase réussi (simulation).";
        }
        if ("git push -u origin main".equals(n)) {
            if (remoteOrigin.isEmpty()) return "fatal: 'origin' does not appear to be a git repository";
            return "branch 'main' set up to track 'origin/main'.\nEverything up-to-date (simulation)";
        }
        if ("git push".equals(n)) {
            return remoteOrigin.isEmpty() ? "fatal: aucun remote configuré" : "Everything up-to-date (simulation)";
        }

        if (n.startsWith("ssh-keygen -t ed25519")) {
            sshKey = true;
            return "Your identification has been saved in /home/ubuntu/.ssh/id_ed25519\nYour public key has been saved in /home/ubuntu/.ssh/id_ed25519.pub";
        }
        if ("ssh -t git@github.com".equals(n)) {
            return sshKey ? "Hi USER! You've successfully authenticated, but GitHub does not provide shell access." :
                "Permission denied (publickey).";
        }

        return "Commande non prise en charge dans ce simulateur pédagogique.";
    }

    private String listBasic(boolean hidden) {
        List<String> all = new ArrayList<>();
        all.addAll(dirs);
        all.addAll(files);
        StringBuilder b = new StringBuilder();
        if (hidden && gitInit) b.append(".git  ");
        for (String x : all) {
            if (x.startsWith("/")) continue;
            b.append(x).append("  ");
        }
        return b.toString().trim();
    }

    private String listLong(boolean hidden) {
        StringBuilder b = new StringBuilder();
        if (hidden) b.append("drwxr-xr-x  .\ndrwxr-xr-x  ..\n");
        if (hidden && gitInit) b.append("drwxr-xr-x  .git\n");
        for (String d : dirs) {
            if (!d.startsWith("/")) b.append("drwxr-xr-x  ").append(d).append("\n");
        }
        for (String f : files) b.append("-rw-r--r--  ").append(f).append("\n");
        return b.toString().trim();
    }

    private String normalize(String s) {
        return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String shortCwd() {
        if ("/home/ubuntu".equals(cwd)) return "~";
        if (cwd.startsWith("/home/ubuntu")) return "~" + cwd.substring("/home/ubuntu".length());
        return cwd;
    }

    private void appendTerminal(String line) {
        if (!line.isEmpty()) history.add("__OUT__" + line);
        if (terminalView != null) terminalView.setText(historyText());
    }

    private String historyText() {
        StringBuilder b = new StringBuilder();
        for (String line : history) {
            if (line.startsWith("__OUT__")) b.append(line.substring(7));
            else b.append(line);
            b.append("\n");
        }
        return b.toString();
    }

    private LinearLayout panel(int color, int radius) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(15), dp(14), dp(15), dp(14));
        l.setBackground(rounded(color, radius, 0));
        return l;
    }

    private TextView label(String text, int size, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), Typeface.BOLD);
        return t;
    }

    private Button accentButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackground(rounded(Color.rgb(226,83,45), 14, 0));
        return b;
    }

    private Button smallButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.rgb(45,45,49));
        b.setBackground(rounded(Color.rgb(232,232,236), 12, 0));
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(12), dp(7), dp(12), dp(7));
        return b;
    }

    private Button smallFullButton(String text) {
        Button b = smallButton(text);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.topMargin = dp(7);
        b.setLayoutParams(p);
        return b;
    }

    private Button choiceButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(14f);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        b.setTypeface(Typeface.MONOSPACE);
        b.setBackground(rounded(Color.rgb(50,50,56), 12, Color.rgb(90,90,98)));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.bottomMargin = dp(7);
        b.setLayoutParams(p);
        return b;
    }

    private GradientDrawable rounded(int fill, int radiusDp, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private LinearLayout.LayoutParams marginTop(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.topMargin = dp(top);
        return p;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
