package com.ghiles.quizubuntu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ubuntu Lab V4.
 *
 * - SIMULATION: safe in-memory Ubuntu/Git machine based on the user's course PDFs.
 * - GITHUB RÉEL: JGit repository in the app-private sandbox, HTTPS authenticated.
 *
 * No arbitrary Android shell command is ever executed.
 */
public class SimulatorActivity extends Activity {

    private static final int UBUNTU_BG = Color.rgb(48, 10, 36);
    private static final int TERMINAL_BG = Color.rgb(28, 28, 30);
    private static final int TERMINAL_PANEL = Color.rgb(35, 35, 38);
    private static final int TERMINAL_TEXT = Color.rgb(238, 238, 238);
    private static final int TERMINAL_MUTED = Color.rgb(170, 170, 176);
    private static final int UBUNTU_ORANGE = Color.rgb(233, 84, 32);
    private static final int PROMPT_GREEN = Color.rgb(78, 201, 109);
    private static final int PATH_BLUE = Color.rgb(94, 161, 255);
    private static final int DIRECTORY_BLUE = Color.rgb(92, 160, 255);
    private static final int ERROR_RED = Color.rgb(255, 99, 105);
    private static final int SUCCESS_GREEN = Color.rgb(82, 201, 111);
    private static final int REAL_RED = Color.rgb(242, 86, 86);

    static class Scenario {
        final String title;
        final String objective;
        final String hint;
        final String setupKey;
        final List<String> accepted;
        final String[] choices;

        Scenario(
            String title,
            String objective,
            String hint,
            String setupKey,
            String[] choices,
            String... accepted
        ) {
            this.title = title;
            this.objective = objective;
            this.hint = hint;
            this.setupKey = setupKey == null ? "" : setupKey;
            this.choices = choices;
            this.accepted = Arrays.asList(accepted);
        }
    }

    private final List<Scenario> scenarios = Arrays.asList(
        new Scenario(
            "Où suis-je ?",
            "Affiche le chemin absolu du dossier courant.",
            "Print Working Directory.",
            "",
            new String[]{"pwd", "ls", "cd ~", "history"},
            "pwd"
        ),
        new Scenario(
            "Voir les fichiers cachés",
            "Liste le dossier courant avec détails et fichiers cachés.",
            "Combine -l et -a.",
            "",
            new String[]{"ls", "ls -l", "ls -la", "pwd"},
            "ls -la", "ls -al"
        ),
        new Scenario(
            "Arborescence",
            "Crée Projets/demo/src en une seule commande.",
            "L'option -p crée les parents nécessaires.",
            "",
            new String[]{
                "mkdir Projets/demo/src",
                "mkdir -p Projets/demo/src",
                "touch Projets/demo/src",
                "cp -r Projets/demo/src"
            },
            "mkdir -p Projets/demo/src"
        ),
        new Scenario(
            "Créer un fichier",
            "Crée un fichier vide demo.txt.",
            "La commande ne crée pas un dossier.",
            "",
            new String[]{"touch demo.txt", "mkdir demo.txt", "cat demo.txt", "rm demo.txt"},
            "touch demo.txt"
        ),
        new Scenario(
            "Ajouter du contenu",
            "Ajoute le mot suite à la fin de notes.txt sans remplacer son contenu.",
            "La double redirection ajoute à la fin.",
            "",
            new String[]{
                "echo \"suite\" >> notes.txt",
                "echo \"suite\" > notes.txt",
                "cat suite >> notes.txt",
                "touch suite"
            },
            "echo \"suite\" >> notes.txt"
        ),
        new Scenario(
            "Initialiser Git",
            "Dans ~/projet, transforme le dossier en dépôt Git local.",
            "Initialise les métadonnées .git.",
            "fresh-git",
            new String[]{"git init", "git status", "git clone .", "git add ."},
            "git init"
        ),
        new Scenario(
            "État du dépôt",
            "Vérifie l'état du working directory et de la staging area.",
            "Cette commande est le premier diagnostic Git.",
            "git-ready",
            new String[]{"git log", "git status", "git diff --staged", "git push"},
            "git status"
        ),
        new Scenario(
            "Staging",
            "Prépare README.md pour le prochain commit.",
            "Copie l'état choisi du fichier dans l'index.",
            "git-ready",
            new String[]{"git add README.md", "git commit README.md", "git push README.md", "git log README.md"},
            "git add README.md"
        ),
        new Scenario(
            "Commit",
            "Crée un commit avec le message Initial commit.",
            "Utilise l'option -m.",
            "git-ready",
            new String[]{
                "git commit -m \"Initial commit\"",
                "git add -m \"Initial commit\"",
                "git log -m \"Initial commit\"",
                "git push -m \"Initial commit\""
            },
            "git commit -m \"Initial commit\""
        ),
        new Scenario(
            "Lire les changements",
            "Affiche les changements non staged du working directory.",
            "Compare working directory et index.",
            "git-ready",
            new String[]{"git diff", "git diff --staged", "git log -p", "git status -s"},
            "git diff"
        ),
        new Scenario(
            "Lire le staging",
            "Affiche ce qui est déjà préparé pour le prochain commit.",
            "Compare index et HEAD.",
            "git-ready",
            new String[]{"git diff --staged", "git diff", "git branch", "git remote -v"},
            "git diff --staged"
        ),
        new Scenario(
            "Créer une branche",
            "Crée la branche cheese et bascule immédiatement dessus.",
            "switch -c crée puis checkout.",
            "git-ready",
            new String[]{"git switch cheese", "git switch -c cheese", "git branch -d cheese", "git push cheese"},
            "git switch -c cheese"
        ),
        new Scenario(
            "Voir les branches",
            "Affiche les branches locales et marque la branche active avec *.",
            "La sous-commande branch sans argument est suffisante.",
            "git-ready",
            new String[]{"git branch", "git log", "git status", "git remote -v"},
            "git branch"
        ),
        new Scenario(
            "Vérifier origin",
            "Affiche les URL fetch et push du remote origin.",
            "L'option -v signifie verbose.",
            "remote-ready",
            new String[]{"git remote -v", "git status", "git branch -a", "git log -p"},
            "git remote -v"
        ),
        new Scenario(
            "Ajouter origin",
            "Configure origin vers https://github.com/USER/REPO.git.",
            "remote add origin URL.",
            "git-ready",
            new String[]{
                "git remote add origin https://github.com/USER/REPO.git",
                "git remote -v origin",
                "git push origin",
                "git init origin"
            },
            "git remote add origin https://github.com/USER/REPO.git"
        ),
        new Scenario(
            "Premier push",
            "Publie main sur origin et configure le suivi distant.",
            "Utilise -u ou --set-upstream.",
            "remote-ready",
            new String[]{
                "git push -u origin main",
                "git push main origin",
                "git remote main",
                "git pull -u main"
            },
            "git push -u origin main",
            "git push --set-upstream origin main"
        ),
        new Scenario(
            "Synchroniser par rebase",
            "Récupère main depuis origin puis rejoue tes commits locaux au-dessus.",
            "Le support utilise pull --rebase.",
            "remote-ready",
            new String[]{
                "git pull --rebase origin main",
                "git push --force origin main",
                "git fetch main",
                "git status --rebase"
            },
            "git pull --rebase origin main"
        ),
        new Scenario(
            "Créer une clé SSH",
            "Crée une paire de clés Ed25519 avec un commentaire email.",
            "La clé publique finit par .pub.",
            "",
            new String[]{
                "ssh-keygen -t ed25519 -C \"email\"",
                "ssh-add -t ed25519",
                "git keygen ed25519",
                "ssh -T ed25519"
            },
            "ssh-keygen -t ed25519 -C \"email\""
        ),
        new Scenario(
            "Démarrer ssh-agent",
            "Démarre ssh-agent dans le shell courant.",
            "Le support utilise eval avec ssh-agent -s.",
            "",
            new String[]{
                "eval \"$(ssh-agent -s)\"",
                "ssh-add ~/.ssh/id_ed25519",
                "ssh -T git@github.com",
                "git status"
            },
            "eval \"$(ssh-agent -s)\""
        ),
        new Scenario(
            "Tester GitHub en SSH",
            "Teste l'authentification SSH auprès de GitHub.",
            "Utilisateur git, option -T.",
            "",
            new String[]{"ssh -T git@github.com", "ssh github.com", "git ssh -T", "ssh-add github.com"},
            "ssh -T git@github.com"
        ),
        new Scenario(
            "Déclencher un conflit",
            "Le local et origin/main ont modifié la même zone. Lance le pull qui tente l'intégration.",
            "pull = fetch + intégration.",
            "conflict-pull",
            new String[]{
                "git pull origin main",
                "git push origin main",
                "git fetch --all",
                "git diff --staged"
            },
            "git pull origin main"
        ),
        new Scenario(
            "Observer le conflit",
            "Un merge est en conflit. Affiche l'état du dépôt.",
            "Commence par le diagnostic.",
            "",
            new String[]{"git status", "git push", "git branch -d main", "git log"},
            "git status"
        ),
        new Scenario(
            "Lire les marqueurs",
            "Affiche README.md pour voir <<<<<<<, ======= et >>>>>>>.",
            "Lis le fichier directement.",
            "",
            new String[]{"cat README.md", "git status README.md", "git push README.md", "pwd README.md"},
            "cat README.md"
        ),
        new Scenario(
            "Marquer la résolution",
            "Après avoir corrigé README.md, marque le fichier comme résolu dans l'index.",
            "En conflit, git add marque aussi la résolution.",
            "",
            new String[]{"git add README.md", "git push README.md", "git diff README.md", "git rm README.md"},
            "git add README.md"
        ),
        new Scenario(
            "Vérifier avant commit",
            "Vérifie la version staged qui sera enregistrée.",
            "Après git add, git diff peut être vide ; utilise --staged.",
            "",
            new String[]{"git diff --staged", "git diff", "git status -s", "git pull"},
            "git diff --staged"
        ),
        new Scenario(
            "Lire le graphe",
            "Affiche une vue synthétique des commits, branches et merges.",
            "Combine graph, oneline, decorate et all.",
            "git-ready",
            new String[]{
                "git log --graph --oneline --decorate --all",
                "git log -p",
                "git status --graph",
                "git branch --graph"
            },
            "git log --graph --oneline --decorate --all"
        )
    );

    private SharedPreferences prefs;
    private VirtualMachine vm;
    private SecureTokenStore tokenStore;
    private RealGitClient realGit;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ScrollView scroll;
    private LinearLayout root;
    private LinearLayout environmentSection;
    private LinearLayout simulationModes;
    private LinearLayout realControls;
    private LinearLayout choicesBox;
    private LinearLayout objectivePanel;
    private LinearLayout interactionRow;
    private LinearLayout terminalToolsRow;
    private LinearLayout commandBar;

    private TerminalEditor terminalView;
    private TextView objectiveView;
    private TextView scoreView;
    private TextView interactionLabel;
    private TextView realStatusView;
    private TextView commandPromptView;
    private TextView sectionTitleView;

    private TerminalEditor commandInput;

    private Button simulationButton;
    private Button realButton;
    private Button guidedButton;
    private Button freeButton;
    private Button interactionButton;
    private Button nextMissionButton;

    private final SpannableStringBuilder terminal = new SpannableStringBuilder();
    private final List<String> inputHistory = new ArrayList<>();
    private int historyCursor = 0;

    private boolean realEnvironment = false;
    private boolean guidedMode = true;
    private boolean typingMode = true;
    private int scenarioIndex = 0;
    private int labPoints = 0;

    private TerminalSessions sessions;
    private String sessionId = "1", sessionName = "Session 1";
    private boolean restoringSession = true, commandBusy, awaitingToken;
    private int preparedScenario = -1;
    private final List<String> missionReplay = new ArrayList<>();

    private String encode(String text) { return java.util.Base64.getEncoder().encodeToString(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    private String decode(String text) { return new String(java.util.Base64.getDecoder().decode(text), java.nio.charset.StandardCharsets.UTF_8); }

    private void saveSession() {
        if (restoringSession || sessions == null || terminalView == null) return;
        java.util.Properties p = new java.util.Properties();
        p.setProperty("name", sessionName);
        p.setProperty("transcript", terminal.toString());
        p.setProperty("draft", awaitingToken ? "" : commandInput.command());
        p.setProperty("guided", Boolean.toString(guidedMode));
        p.setProperty("scenario", Integer.toString(scenarioIndex));
        p.setProperty("prepared", Integer.toString(preparedScenario));
        p.setProperty("replay", String.join("\n", missionReplay));
        List<String> history = new ArrayList<>();
        for (String entry : inputHistory) history.add(encode(entry));
        p.setProperty("history", String.join("\n", history));
        try { sessions.save(sessionId, p); }
        catch (Exception e) { android.widget.Toast.makeText(this, "Sauvegarde de session impossible : " + safeMessage(e), android.widget.Toast.LENGTH_LONG).show(); }
    }

    private void openSession(String id) throws Exception {
        if (commandBusy) throw new IllegalStateException("Attends la fin de la commande en cours.");
        java.util.Properties p = sessions.load(id);
        saveSession();
        restoringSession = true;
        try {
            awaitingToken = false;
            commandInput.setCommand(""); commandInput.setSecret(false);
            sessionId = id; sessionName = p.getProperty("name", "Session " + id);
            realGit = new RealGitClient(this, tokenStore, sessions.workspace(id), id);
            vm = new VirtualMachine(); missionReplay.clear();
            String replay = p.getProperty("replay", "");
            if (!replay.isEmpty()) for (String event : replay.split("\n")) {
                String[] fields = event.split(":", -1);
                if (fields[0].equals("P")) vm.prepareScenario(decode(fields[1]));
                else if (fields[0].equals("C")) vm.execute(decode(fields[1]));
                else if (fields[0].equals("E")) vm.saveEditedFile(decode(fields[1]), decode(fields[2]));
                missionReplay.add(event);
            }
            scenarioIndex = Math.floorMod(Integer.parseInt(p.getProperty("scenario", "0")), scenarios.size());
            preparedScenario = Integer.parseInt(p.getProperty("prepared", "-1"));
            terminal.clear(); terminal.append(p.getProperty("transcript", ""));
            inputHistory.clear();
            String history = p.getProperty("history", "");
            if (!history.isEmpty()) for (String entry : history.split("\n")) inputHistory.add(decode(entry));
            historyCursor = inputHistory.size();
            guidedMode = Boolean.parseBoolean(p.getProperty("guided", "false"));
            realEnvironment = !guidedMode;
            if (guidedMode) showScenario();
            else setEnvironment(true);
            appendSystem("[" + sessionName + " · " + sessionId + "] Sauvegarde automatique. help : aide · session : sessions · gh auth login : GitHub.");
            commandInput.setCommand(p.getProperty("draft", ""));
            prefs.edit().putString("terminalSession", id).apply();
        } finally { restoringSession = false; }
        updateCommandPrompt();
    }

    private void showSessions() {
        if (commandBusy || awaitingToken) { appendSystem("Termine la commande en cours avant de changer de session."); refreshTerminal(); return; }
        try {
            List<String> ids = sessions.list(); List<String> labels = new ArrayList<>();
            for (String id : ids) labels.add((id.equals(sessionId) ? "✓ " : "") + id + " — " + sessions.load(id).getProperty("name"));
            labels.add("+ Nouvelle session");
            new AlertDialog.Builder(this).setTitle("Sessions sauvegardées").setItems(labels.toArray(new String[0]), (dialog, which) -> {
                try { openSession(which == ids.size() ? sessions.create("") : ids.get(which)); }
                catch (Exception e) { appendSystem(safeMessage(e)); refreshTerminal(); }
            }).show();
        } catch (Exception e) { appendSystem(safeMessage(e)); refreshTerminal(); }
    }

    private boolean terminalCommand(String command) {
        try {
            List<String> args = ShellSyntax.words(command);
            if (args.isEmpty()) return true;
            if (args.get(0).equals("session")) {
                String action = args.size() > 1 ? args.get(1) : "list";
                if (action.equals("new")) openSession(sessions.create(args.size() > 2 ? String.join(" ", args.subList(2, args.size())) : ""));
                else if (action.equals("open") && args.size() == 3) openSession(args.get(2));
                else if (action.equals("rename") && args.size() > 2) { sessionName = String.join(" ", args.subList(2, args.size())); appendSystem("Session renommée : " + sessionName); }
                else if (action.equals("save") || action.equals("close")) { saveSession(); appendSystem("Session sauvegardée. Tu peux fermer l'application et la reprendre ensuite."); }
                else if (action.equals("list")) { for (String id : sessions.list()) appendSystem((id.equals(sessionId) ? "* " : "  ") + id + " — " + sessions.load(id).getProperty("name")); }
                else appendSystem("session list | session new [nom] | session open ID | session rename nom | session save | session close");
                refreshTerminal(); return true;
            }
            if (command.equals("gh auth login")) {
                awaitingToken = true; commandInput.setSecret(true);
                appendSystem("Crée un jeton GitHub autorisé sur ton dépôt (Contents : lecture et écriture), puis colle-le ici et valide. Saisie masquée, exclue de l'historique. Tape cancel pour annuler.\nhttps://github.com/settings/personal-access-tokens/new");
                refreshTerminal(); return true;
            }
            if (command.startsWith("gh auth login ")) { appendSystem("Utilise gh auth login, sans jeton dans la commande."); refreshTerminal(); return true; }
            if (command.equals("gh auth status")) { appendSystem(tokenStore.hasToken() ? "GitHub connecté : @" + prefs.getString("realGitHubLogin", "") : "GitHub non connecté. Tape gh auth login."); refreshTerminal(); return true; }
            if (command.equals("gh auth logout")) { tokenStore.clearToken(); prefs.edit().remove("realGitHubLogin").apply(); appendSystem("Identifiants GitHub effacés. Les fichiers et sessions restent sauvegardés."); refreshTerminal(); return true; }
            if (command.equals("help")) {
                appendSystem("TERMINAL LIBRE : vrais fichiers dans le stockage privé du téléphone, conservés après fermeture. Git échange avec le dépôt distant.\nFichiers : mkdir, cd, pwd, touch, echo, cat, ls -la, nano.\nGitHub : gh auth login | gh auth status | gh auth logout\nSessions : session list | session new [nom] | session open ID | session rename nom | session save | session close\nExemple : mkdir projet, puis cd projet, git init, nano note.txt, git config --global user.name \"Ton nom\", git config --global user.email \"ton@email\", git add ., git commit -m \"Premier fichier\", git remote add origin URL, git push -u origin main.\nPour récupérer une modification GitHub : git pull origin main, puis cat note.txt. Un push envoie les commits ; pense à git add et git commit après chaque modification.\nMissions guidées : exercices isolés du dépôt réel. commandes : référence des commandes et limites.");
                refreshTerminal(); return true;
            }
            if (command.equals("commandes")) { showCommandCatalogDialog(""); return true; }
            if (command.equals("clear")) { terminal.clear(); refreshTerminal(); return true; }
        } catch (Exception e) { appendSystem("Erreur : " + safeMessage(e)); refreshTerminal(); return true; }
        return false;
    }

    private void submitToken(String value) {
        commandInput.setCommand(""); commandInput.setSecret(false); awaitingToken = false;
        if (value.equals("cancel")) { appendSystem("Connexion annulée."); refreshTerminal(); return; }
        commandBusy = true; appendSystem("Vérification GitHub…"); refreshTerminal();
        executor.submit(() -> {
            try {
                String login = new GitHubApiClient(value).getLogin();
                tokenStore.saveToken(value); prefs.edit().putString("realGitHubLogin", login).apply();
                runOnUiThread(() -> { commandBusy = false; appendSystem("GitHub connecté : @" + login + ". Configure le dépôt avec git remote add origin URL ou git clone URL."); refreshTerminal(); });
            } catch (Exception e) {
                runOnUiThread(() -> { commandBusy = false; appendSystem("Connexion refusée ou réseau indisponible. Vérifie les droits et la validité du jeton puis relance gh auth login."); refreshTerminal(); });
            }
        });
    }

    @Override protected void onPause() { saveSession(); super.onPause(); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("quiz_progress", MODE_PRIVATE);
        vm = new VirtualMachine();
        tokenStore = new SecureTokenStore(this);
        realGit = new RealGitClient(this, tokenStore);

        getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );

        labPoints = prefs.getInt("labPoints", 0);
        guidedMode = prefs.getBoolean("simGuidedMode", true);
        typingMode = prefs.getBoolean("simTypingMode", true);
        realEnvironment = prefs.getBoolean("labRealEnvironment", false);

        buildUi();

        try {
            sessions = new TerminalSessions(getFilesDir());
            String active = prefs.getString("terminalSession", "1");
            if (!sessions.list().contains(active)) active = "1";
            openSession(active);
        } catch (Exception e) {
            restoringSession = false;
            setEnvironment(true);
            appendSystem("Impossible de restaurer la session : " + safeMessage(e));
            refreshTerminal();
        }
    }

    @Override
    protected void onDestroy() {
        saveSession();
        executor.shutdown();
        super.onDestroy();
    }

    private void buildUi() {
        getWindow().setStatusBarColor(UBUNTU_BG);
        getWindow().setNavigationBarColor(Color.rgb(18,18,20));

        // Android 11+ / Android 15 edge-to-edge:
        // receive IME + system-bar insets ourselves so the command bar can
        // never be covered by the software keyboard.
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        }

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(UBUNTU_BG);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(UBUNTU_BG);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(6), dp(12), dp(14));
        scroll.addView(root);

        screen.addView(
            scroll,
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        );

        addUbuntuTopBar();
        addCompactHeader();
        addEnvironmentSelector();
        addSimulationModeSelector();
        addRealControls();
        addObjectivePanel();
        addInteractionRow();
        addTerminal();
        addTerminalTools();

        choicesBox = new LinearLayout(this);
        choicesBox.setOrientation(LinearLayout.VERTICAL);
        choicesBox.setPadding(0, dp(7), 0, 0);
        root.addView(choicesBox);

        nextMissionButton = accentButton("Objectif suivant");
        nextMissionButton.setOnClickListener(v -> {
            scenarioIndex = (scenarioIndex + 1) % scenarios.size();
            showScenario();
        });
        root.addView(nextMissionButton, topMargin(9));

        Button academy = smallFullButton("← Retour à l'Academy");
        academy.setOnClickListener(v -> finish());
        root.addView(academy, topMargin(8));

        buildFixedCommandBar(screen);
        protectFromSystemBars(screen);

        if (environmentSection != null) environmentSection.setVisibility(View.GONE);
        if (simulationModes != null) simulationModes.setVisibility(View.GONE);

        setContentView(screen);
    }

    private void addUbuntuTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(2), dp(2), dp(2), dp(5));

        Button menu = compactButton("☰");
        menu.setTextColor(Color.WHITE);
        menu.setBackgroundColor(Color.TRANSPARENT);
        menu.setContentDescription("Ouvrir les rubriques Ubuntu Lab");
        menu.setOnClickListener(v -> showLabMenu());
        bar.addView(
            menu,
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        );

        sectionTitleView = terminalText("Terminal libre", 11, true, Color.WHITE);
        sectionTitleView.setGravity(Gravity.CENTER);
        bar.addView(
            sectionTitleView,
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        );

        TextView indicators = terminalText("Sessions locales", 10, false, Color.rgb(226,226,230));
        indicators.setGravity(Gravity.END);
        bar.addView(
            indicators,
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        );

        root.addView(bar);
    }

    private void addCompactHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(3), dp(2), dp(3), dp(5));

        TextView title = terminalText("Ubuntu Academy", 11, true, Color.WHITE);
        header.addView(
            title,
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        );

        scoreView = terminalText("", 10, false, Color.rgb(221,204,217));
        scoreView.setGravity(Gravity.END);
        header.addView(
            scoreView,
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        );

        root.addView(header);
    }

    private void addEnvironmentSelector() {
        environmentSection = new LinearLayout(this);
        environmentSection.setOrientation(LinearLayout.VERTICAL);

        TextView label = terminalText("ENVIRONNEMENT", 10, true, Color.rgb(230,210,225));
        environmentSection.addView(label);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(6));

        simulationButton = smallButton("SIMULATION");
        simulationButton.setOnClickListener(v -> setEnvironment(false));
        row.addView(
            simulationButton,
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        );

        realButton = smallButton("GITHUB RÉEL");
        realButton.setOnClickListener(v -> setEnvironment(true));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        );
        rp.leftMargin = dp(6);
        row.addView(realButton, rp);

        environmentSection.addView(row);
        root.addView(environmentSection);
    }

    private void addSimulationModeSelector() {
        simulationModes = new LinearLayout(this);
        simulationModes.setOrientation(LinearLayout.HORIZONTAL);

        guidedButton = smallButton("Missions guidées");
        guidedButton.setOnClickListener(v -> setGuidedMode(true));
        simulationModes.addView(
            guidedButton,
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        );

        freeButton = smallButton("Terminal libre");
        freeButton.setOnClickListener(v -> setGuidedMode(false));
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        );
        fp.leftMargin = dp(6);
        simulationModes.addView(freeButton, fp);

        root.addView(simulationModes);
    }

    private void addRealControls() {
        // Kept detached for legacy status helpers; authentication is terminal-only.
        realControls = new LinearLayout(this);
        realStatusView = terminalText("", 11, false, Color.WHITE);
    }

    private void addObjectivePanel() {
        objectivePanel = panel(Color.rgb(247, 244, 247), 13);

        TextView label = terminalText("OBJECTIF", 10, true, UBUNTU_ORANGE);
        objectivePanel.addView(label);

        objectiveView = terminalText("", 13, true, Color.rgb(30,30,34));
        objectiveView.setPadding(0, dp(5), 0, dp(5));
        objectivePanel.addView(objectiveView);

        Button hint = smallButton("Indice");
        hint.setId(View.generateViewId());
        hint.setTag("hint");
        hint.setOnClickListener(v -> {
            Scenario scenario = scenarios.get(scenarioIndex);
            appendSystem("[indice] " + scenario.hint);
            refreshTerminal();
            scrollBottom();
        });
        objectivePanel.addView(hint);

        root.addView(objectivePanel, topMargin(5));
    }

    private Button findHintButton() {
        for (int i = 0; i < objectivePanel.getChildCount(); i++) {
            View child = objectivePanel.getChildAt(i);
            if ("hint".equals(child.getTag()) && child instanceof Button) {
                return (Button) child;
            }
        }
        return null;
    }

    private void addInteractionRow() {
        interactionRow = new LinearLayout(this);
        interactionRow.setOrientation(LinearLayout.HORIZONTAL);
        interactionRow.setGravity(Gravity.CENTER_VERTICAL);
        interactionRow.setPadding(0, dp(7), 0, dp(5));

        interactionLabel = terminalText("", 10, true, Color.WHITE);
        interactionRow.addView(
            interactionLabel,
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        );

        interactionButton = smallButton("QCM / saisie");
        interactionButton.setOnClickListener(v -> {
            typingMode = !typingMode;
            prefs.edit().putBoolean("simTypingMode", typingMode).apply();
            renderInteraction();
        });
        interactionRow.addView(interactionButton);

        root.addView(interactionRow);
    }

    private void addTerminal() {
        LinearLayout window = panel(TERMINAL_BG, 10);
        window.setPadding(dp(10), dp(7), dp(10), dp(9));

        LinearLayout titleBar = new LinearLayout(this);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView dots = terminalText("● ● ●", 9, true, UBUNTU_ORANGE);
        titleBar.addView(dots);

        TextView title = terminalText(
            "  ubuntu@academy — bash",
            10,
            false,
            TERMINAL_MUTED
        );
        titleBar.addView(title);

        window.addView(titleBar);

        terminalView = new TerminalEditor(this);
        commandInput = terminalView;
        terminalView.setOnSubmit(this::executeInput);
        terminalView.setBackgroundColor(Color.TRANSPARENT);
        terminalView.setGravity(Gravity.TOP | Gravity.START);
        terminalView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        terminalView.setImeOptions(EditorInfo.IME_ACTION_GO | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        terminalView.setContentDescription("Terminal. Touchez puis saisissez après le dernier prompt. Entrée exécute la commande.");
        terminalView.setOnFocusChangeListener((v,focused) -> { if(focused)scroll.postDelayed(this::scrollBottom,120); });
        terminalView.setOnClickListener(v -> {
            terminalView.requestFocus();
            ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(terminalView, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        });
        terminalView.setOnKeyListener((v,key,event) -> {
            if(key!=KeyEvent.KEYCODE_ENTER)return false;
            if(event.getAction()==KeyEvent.ACTION_DOWN && event.getRepeatCount()==0)executeInput();
            return true;
        });
        terminalView.setOnEditorActionListener((v,action,event) -> {
            if(event==null&&(action==EditorInfo.IME_ACTION_GO||action==EditorInfo.IME_ACTION_DONE||action==EditorInfo.IME_ACTION_SEND)) {executeInput();return true;}
            return false;
        });
        terminalView.setTypeface(Typeface.MONOSPACE);
        terminalView.setTextSize(11.5f);
        terminalView.setTextColor(TERMINAL_TEXT);

        terminalView.setLineSpacing(0f, 1.03f);
        terminalView.setPadding(0, dp(6), 0, dp(5));
        terminalView.setMinLines(12);
        terminalView.render(terminal, awaitingToken ? "Jeton GitHub (masqué) : " : commandPromptView == null ? "$ " : commandPromptView.getText());
            saveSession();

        window.addView(terminalView);
        root.addView(window);
    }

    private void addTerminalTools() {
        terminalToolsRow = new LinearLayout(this);
        terminalToolsRow.setOrientation(LinearLayout.HORIZONTAL);
        terminalToolsRow.setPadding(0, dp(6), 0, 0);

        Button clear = smallButton("Vider écran");
        clear.setOnClickListener(v -> {
            terminal.clear();
            appendSystem(realEnvironment ? "[GITHUB RÉEL]" : "[SIMULATION]");
            refreshTerminal();
        });
        terminalToolsRow.addView(
            clear,
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        );

        Button copy = smallButton("Copier sortie");
        copy.setOnClickListener(v -> {
            ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            manager.setPrimaryClip(
                ClipData.newPlainText("Ubuntu Lab", terminal.toString())
            );
            appendSystem("[copié dans le presse-papiers]");
            refreshTerminal();
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        );
        cp.leftMargin = dp(5);
        terminalToolsRow.addView(copy, cp);

        Button reset = smallButton("Sessions");
        reset.setOnClickListener(v -> showSessions());
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        );
        rp.leftMargin = dp(5);
        terminalToolsRow.addView(reset, rp);

        root.addView(terminalToolsRow);
    }

    private void buildFixedCommandBar(LinearLayout screen) {
        commandBar = new LinearLayout(this);
        commandBar.setOrientation(LinearLayout.VERTICAL);
        commandBar.setPadding(dp(8), dp(5), dp(8), dp(7));
        commandBar.setBackgroundColor(TERMINAL_BG);

        commandPromptView = new TextView(this); // Holds styled prompt; drawn in the terminal surface.
        LinearLayout utilityRow = new LinearLayout(this);
        utilityRow.setOrientation(LinearLayout.HORIZONTAL);
        utilityRow.setGravity(Gravity.CENTER_VERTICAL);
        utilityRow.setPadding(dp(2), dp(4), dp(2), 0);

        TextView hint = terminalText(
            "Écris directement après le prompt • Entrée = exécuter",
            9.5f,
            false,
            TERMINAL_MUTED
        );
        utilityRow.addView(
            hint,
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        );

        Button execute = compactButton("↵");
        execute.setContentDescription("Exécuter la commande du terminal");
        execute.setOnClickListener(v -> executeInput());
        utilityRow.addView(execute);
        Button previous = compactButton("↑");
        previous.setContentDescription("Commande précédente");
        previous.setOnClickListener(v -> historyPrevious());
        utilityRow.addView(previous);

        Button next = compactButton("↓");
        next.setContentDescription("Commande suivante");
        next.setOnClickListener(v -> historyNext());
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        nextParams.leftMargin = dp(3);
        utilityRow.addView(next, nextParams);

        commandBar.addView(utilityRow);

        screen.addView(
            commandBar,
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        );

        updateCommandPrompt();
    }

    private void showLabMenu() {
        String[] items = { "Terminal libre", "Missions guidées", "Help", "Commandes", "Retour à l’Académie" };
        new AlertDialog.Builder(this).setTitle("Ubuntu — rubriques").setItems(items, (dialog, which) -> {
            if (commandBusy || awaitingToken) return;
            if (which == 0) setGuidedMode(false);
            else if (which == 1) setGuidedMode(true);
            else if (which == 2) runCommand("help");
            else if (which == 3) showCommandCatalogDialog("");
            else finish();
        }).show();
    }

    private void showCommandCatalogDialog(String initialQuery) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(6), dp(16), dp(6));

        TextView counter = terminalText(
            CommandCatalog.count() + " exemples pédagogiques. En terminal libre : fichiers et Git réels ; seules les commandes implémentées sont exécutées. Les missions utilisent un environnement d’exercice isolé.",
            11,
            true,
            Color.rgb(45,45,49)
        );
        box.addView(counter);

        EditText search = new EditText(this);
        search.setHint("Ex. checkout, branch, grep, ssh, archive…");
        search.setSingleLine(true);
        search.setText(initialQuery);
        search.setTextSize(14f);
        box.addView(search);

        TextView results = new TextView(this);
        results.setTypeface(Typeface.MONOSPACE);
        results.setTextSize(11.5f);
        results.setTextColor(Color.rgb(40,40,44));
        results.setText(buildCatalogText(initialQuery));
        results.setTextIsSelectable(true);
        results.setPadding(0, dp(8), 0, dp(6));

        ScrollView resultScroll = new ScrollView(this);
        resultScroll.addView(results);
        box.addView(
            resultScroll,
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(360)
            )
        );

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Commandes")
            .setView(box)
            .setNegativeButton("Fermer", null)
            .setPositiveButton("Rechercher", null)
            .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    results.setText(buildCatalogText(search.getText().toString()));
                    resultScroll.post(() -> resultScroll.scrollTo(0, 0));
                });
        });

        dialog.show();
    }

    private String buildCatalogText(String query) {
        List<CommandCatalog.Entry> entries = CommandCatalog.search(query, 80);

        if (entries.isEmpty()) {
            return "Aucune commande trouvée.";
        }

        StringBuilder out = new StringBuilder();

        for (CommandCatalog.Entry entry : entries) {
            out.append(entry.command)
                .append("\n  ")
                .append(entry.category)
                .append(" — ")
                .append(entry.description)
                .append("\n\n");
        }

        if (CommandCatalog.search(query, 81).size() > 80) {
            out.append("… résultats supplémentaires. Affine la recherche.");
        }

        return out.toString().trim();
    }

    private void showHelpSections() {
        String[] topics = {
            "Navigation Bash",
            "Fichiers & redirections",
            "Git quotidien",
            "Branches & remotes",
            "SSH",
            "Synchronisation & conflits"
        };

        new AlertDialog.Builder(this)
            .setTitle("Aide & commandes")
            .setItems(topics, (dialog, which) -> {
                String text;

                if (which == 0) {
                    text = "pwd\nls\nls -l\nls -la\ncd dossier\ncd ..\ncd ~\nhistory\nclear";
                } else if (which == 1) {
                    text = "mkdir\nmkdir -p\ntouch\necho\n>\n>>\ncat\nnano\nmv\ncp\ncp -r\nrm\nrm -r\nrmdir";
                } else if (which == 2) {
                    text = "git --version\ngit config\ngit init\ngit status\ngit add\ngit add .\ngit commit -m\ngit log\ngit log -p\ngit diff\ngit diff --staged";
                } else if (which == 3) {
                    text = "git branch\ngit branch -a\ngit branch -M main\ngit branch -d\ngit switch\ngit switch -c\ngit remote -v\ngit remote get-url origin\ngit remote add origin URL\ngit remote set-url origin URL\ngit remote remove origin";
                } else if (which == 4) {
                    text = "ls -al ~/.ssh\nssh-keygen -t ed25519 -C \"email\"\neval \"$(ssh-agent -s)\"\nssh-add ~/.ssh/id_ed25519\ncat ~/.ssh/id_ed25519.pub\nssh-keygen -lf ~/.ssh/id_ed25519.pub\nssh -T git@github.com";
                } else {
                    text = "git fetch\ngit fetch --all\ngit fetch --prune\ngit pull origin main\ngit pull --rebase origin main\ngit push\ngit push -u origin main\ngit ls-remote origin\ngit merge --abort\ngit log --graph --oneline --decorate --all";
                }

                new AlertDialog.Builder(this)
                    .setTitle(topics[which])
                    .setMessage(text)
                    .setPositiveButton("Fermer", null)
                    .show();
            })
            .show();
    }

    private void applySectionLayout() {
        if (environmentSection != null) {
            environmentSection.setVisibility(View.GONE);
        }

        if (simulationModes != null) {
            simulationModes.setVisibility(View.GONE);
        }

        if (realEnvironment) {
            sectionTitleView.setText("Terminal libre · " + sessionName);
            realControls.setVisibility(View.GONE);
            objectivePanel.setVisibility(View.GONE);
            interactionRow.setVisibility(View.GONE);
            choicesBox.setVisibility(View.GONE);
            nextMissionButton.setVisibility(View.GONE);
            commandBar.setVisibility(View.VISIBLE);
        } else if (guidedMode) {
            sectionTitleView.setText("Missions");
            realControls.setVisibility(View.GONE);
            objectivePanel.setVisibility(View.VISIBLE);
            interactionRow.setVisibility(View.VISIBLE);
            choicesBox.setVisibility(View.VISIBLE);
            nextMissionButton.setVisibility(View.VISIBLE);
        } else {
            sectionTitleView.setText("Terminal libre");
            realControls.setVisibility(View.GONE);
            objectivePanel.setVisibility(View.GONE);
            interactionRow.setVisibility(View.GONE);
            choicesBox.setVisibility(View.GONE);
            nextMissionButton.setVisibility(View.GONE);
            commandBar.setVisibility(View.VISIBLE);
        }

        if (terminalToolsRow != null) {
            terminalToolsRow.setVisibility(View.VISIBLE);
        }
    }

    private void setEnvironment(boolean real) {
        realEnvironment = real;
        prefs.edit().putBoolean("labRealEnvironment", real).apply();

        styleEnvironmentButtons();

        if (real) {
            guidedMode = false;
            simulationModes.setVisibility(View.GONE);
            realControls.setVisibility(View.GONE);
            choicesBox.removeAllViews();
            nextMissionButton.setVisibility(View.GONE);
            interactionButton.setVisibility(View.GONE);

            Button hint = findHintButton();
            if (hint != null) hint.setVisibility(View.GONE);

            interactionLabel.setText("MODE : terminal Git réel");
            objectiveView.setText(
                "GIT / SSH RÉEL\n\n" +
                "ssh-keygen crée une vraie clé Ed25519. cat ~/.ssh/id_ed25519.pub affiche la vraie clé publique. " +
                "ssh -T git@github.com vérifie réellement l'authentification, et git fetch/pull/push utilisent Internet " +
                "dans l'espace Git privé de l'application. Aucune commande shell arbitraire n'est exécutée sur Android."
            );

            scoreView.setText(sessionName + " · fichiers sauvegardés");
            refreshRealStatusTextOnly();
        } else {
            realControls.setVisibility(View.GONE);
            simulationModes.setVisibility(View.VISIBLE);


            styleLearningButtons();

            if (guidedMode) showScenario();
            else showFreeSimulation();
        }

        commandBar.setVisibility(View.VISIBLE);
        commandInput.setVisibility(View.VISIBLE);
        updateCommandPrompt();
        applySectionLayout();
        refreshTerminal();
        scrollBottom();
    }

    private void styleEnvironmentButtons() {
        if (realEnvironment) {
            simulationButton.setBackground(rounded(Color.rgb(232,232,236), 9, 0));
            simulationButton.setTextColor(Color.rgb(45,45,49));

            realButton.setBackground(rounded(REAL_RED, 9, 0));
            realButton.setTextColor(Color.WHITE);
        } else {
            simulationButton.setBackground(rounded(UBUNTU_ORANGE, 9, 0));
            simulationButton.setTextColor(Color.WHITE);

            realButton.setBackground(rounded(Color.rgb(232,232,236), 9, 0));
            realButton.setTextColor(Color.rgb(45,45,49));
        }
    }

    private void setGuidedMode(boolean guided) {
        if (commandBusy || awaitingToken) return;
        guidedMode = guided;
        realEnvironment = !guided;
        prefs.edit().putBoolean("simGuidedMode", guided).apply();

        styleLearningButtons();

        if (guided) showScenario();
        else setEnvironment(true);
        saveSession();
    }

    private void styleLearningButtons() {
        if (guidedMode) {
            guidedButton.setBackground(rounded(UBUNTU_ORANGE, 9, 0));
            guidedButton.setTextColor(Color.WHITE);

            freeButton.setBackground(rounded(Color.rgb(232,232,236), 9, 0));
            freeButton.setTextColor(Color.rgb(45,45,49));
        } else {
            freeButton.setBackground(rounded(UBUNTU_ORANGE, 9, 0));
            freeButton.setTextColor(Color.WHITE);

            guidedButton.setBackground(rounded(Color.rgb(232,232,236), 9, 0));
            guidedButton.setTextColor(Color.rgb(45,45,49));
        }
    }

    private void showScenario() {
        if (realEnvironment) return;

        guidedMode = true;
        styleLearningButtons();

        Scenario scenario = scenarios.get(scenarioIndex);

        if (preparedScenario != scenarioIndex) {
            missionReplay.clear();
            vm.reset();
            if (!scenario.setupKey.isEmpty()) {
                vm.prepareScenario(scenario.setupKey);
                missionReplay.add("P:" + encode(scenario.setupKey));
            }
            preparedScenario = scenarioIndex;
        }

        objectivePanel.setVisibility(View.VISIBLE);
        objectiveView.setText(
            scenario.title + "\n\n" + scenario.objective
        );

        Button hint = findHintButton();
        if (hint != null) hint.setVisibility(View.VISIBLE);

        interactionButton.setVisibility(View.VISIBLE);
        nextMissionButton.setVisibility(View.VISIBLE);

        scoreView.setText(
            "Lab XP : " + labPoints +
            "  •  Mission " + (scenarioIndex + 1) + "/" + scenarios.size()
        );

        appendSystem("[mission] " + scenario.title);
        renderInteraction();
        updateCommandPrompt();
        applySectionLayout();
        refreshTerminal();
    }

    private void showFreeSimulation() {
        if (realEnvironment) return;

        guidedMode = false;
        styleLearningButtons();

        objectivePanel.setVisibility(View.VISIBLE);
        objectiveView.setText(
            "TERMINAL LIBRE\n\n" +
            "Tape librement les commandes de tes supports Bash, Git, GitHub, SSH, branches, synchronisation et conflits. " +
            "La machine conserve son état jusqu'au Reset VM."
        );

        Button hint = findHintButton();
        if (hint != null) hint.setVisibility(View.GONE);

        interactionButton.setVisibility(View.GONE);
        choicesBox.removeAllViews();
        nextMissionButton.setVisibility(View.GONE);

        commandBar.setVisibility(View.VISIBLE);
        interactionLabel.setText("MODE : terminal libre simulé");

        scoreView.setText(
            "Lab XP : " + labPoints +
            "  •  Ubuntu virtuel"
        );

        appendSystem("[terminal libre] Tape help pour la liste des commandes.");
        updateCommandPrompt();
        applySectionLayout();
        refreshTerminal();
        scrollBottom();
    }

    private void renderInteraction() {
        choicesBox.removeAllViews();

        if (realEnvironment || !guidedMode) {
            commandBar.setVisibility(View.VISIBLE);
            return;
        }

        if (typingMode) {
            interactionLabel.setText("MISSION : écris la commande");
            commandBar.setVisibility(View.VISIBLE);
            commandInput.setVisibility(View.VISIBLE);
        } else {
            interactionLabel.setText("MISSION : 4 propositions");
            commandInput.clearFocus();
            commandBar.setVisibility(View.GONE);

            Scenario scenario = scenarios.get(scenarioIndex);
            List<String> shuffled = new ArrayList<>(
                Arrays.asList(scenario.choices)
            );
            Collections.shuffle(shuffled);

            for (String choice : shuffled) {
                Button button = choiceButton(choice);
                button.setOnClickListener(v -> runCommand(choice));
                choicesBox.addView(button);
            }
        }
    }

    private void executeInput() {
        String command = commandInput.command().trim();

        if (command.isEmpty() || commandBusy) return;
        if (awaitingToken) { submitToken(command); return; }
        if (command.startsWith("gh auth login ")) command = "gh auth login";
        commandInput.setCommand("");
        inputHistory.add(command);
        historyCursor = inputHistory.size();

        runCommand(command);

        commandInput.requestFocus();
        scroll.postDelayed(this::scrollBottom, 100);
    }

    private void runCommand(String command) {
        if (command == null || command.trim().isEmpty()) return;

        if (commandBusy || awaitingToken) return;
        appendPrompt(command);
        if (terminalCommand(command)) return;

        if (realEnvironment) {
            if (command.equals("nano") || command.startsWith("nano ")) {
                try {
                    List<String> args = ShellSyntax.words(command);
                    if (args.size() != 2) throw new IllegalArgumentException("Usage : nano fichier");
                    showNanoEditor(realGit.editorPath(args.get(1)), true);
                } catch (Exception e) { appendPlain("Erreur fichier réel : " + safeMessage(e) + "\n", ERROR_RED); refreshTerminal(); }
                return;
            }
            if (isDangerousRealCommand(command)) {
                confirmDangerousRealCommand(command);
            } else {
                runRealCommandAsync(command);
            }
            return;
        }

        if (command.startsWith("git push") || command.startsWith("git pull") || command.startsWith("git fetch") || command.startsWith("git clone")) {
            appendPlain("[SIMULATION] Aucun échange avec GitHub. Les fichiers de ce mode sont virtuels. Pour synchroniser le téléphone, ouvre Terminal libre.\n", Color.rgb(238,176,96));
        }
        missionReplay.add("C:" + encode(command));
        VirtualMachine.Result result = vm.execute(command);
        renderVirtualResult(command, result);

        if (guidedMode) {
            evaluateMission(command);
        }

        updateCommandPrompt();
        refreshTerminal();
        scrollBottom();
    }

    private void renderVirtualResult(
        String command,
        VirtualMachine.Result result
    ) {
        if (result.kind == VirtualMachine.Kind.CLEAR) {
            terminal.clear();
            return;
        }

        if (result.kind == VirtualMachine.Kind.EDIT) {
            showNanoEditor(result.editPath);
            return;
        }

        if (result.kind == VirtualMachine.Kind.LS) {
            if (!result.text.isEmpty()) {
                appendPlain(result.text, TERMINAL_TEXT);
                if (!result.text.endsWith("\n")) appendRaw("\n");
            }

            boolean longFormat = ShellSyntax.hasShortOption(command, 'l');

            for (int i = 0; i < result.entries.size(); i++) {
                VirtualMachine.FsEntry entry = result.entries.get(i);

                if (longFormat) {
                    appendPlain(
                        entry.permissions + "  ",
                        TERMINAL_MUTED
                    );
                }

                appendPlain(
                    entry.name,
                    entry.directory ? DIRECTORY_BLUE : TERMINAL_TEXT
                );

                if (longFormat) {
                    appendRaw("\n");
                } else if (i < result.entries.size() - 1) {
                    appendRaw("  ");
                }
            }

            if (!result.entries.isEmpty() && !longFormat) appendRaw("\n");
            return;
        }

        if (result.text.isEmpty()) return;

        if (result.kind == VirtualMachine.Kind.ERROR) {
            appendPlain(result.text + "\n", ERROR_RED);
        } else if (result.kind == VirtualMachine.Kind.SUCCESS) {
            appendPlain(result.text + "\n", SUCCESS_GREEN);
        } else {
            appendPlain(result.text + "\n", TERMINAL_TEXT);
        }
    }

    private void evaluateMission(String command) {
        Scenario scenario = scenarios.get(scenarioIndex);
        String normalized = normalize(command);

        boolean correct = false;

        for (String accepted : scenario.accepted) {
            if (normalized.equals(normalize(accepted))) {
                correct = true;
                break;
            }
        }

        if (!correct) {
            appendPlain(
                "↳ La commande a été exécutée, mais elle ne valide pas encore l'objectif.\n",
                Color.rgb(236,184,91)
            );
            refreshTerminal();
            return;
        }

        labPoints += 150;

        prefs.edit()
            .putInt("labPoints", labPoints)
            .putInt(
                "totalPoints",
                prefs.getInt("totalPoints", 0) + 150
            )
            .apply();

        appendPlain("✓ Objectif réussi : +150 Lab XP\n", SUCCESS_GREEN);

        scenarioIndex = (scenarioIndex + 1) % scenarios.size();

        scoreView.setText(
            "Lab XP : " + labPoints + "  •  Mission réussie"
        );

        String completedSession = sessionId;
        int nextScenario = scenarioIndex;
        objectiveView.postDelayed(() -> { if (sessionId.equals(completedSession) && guidedMode && scenarioIndex == nextScenario) showScenario(); }, 550);
    }

    private void runRealCommandAsync(String command) {
        if (needsNetworkCredentials(command)) {
            boolean sshReady =
                realGit.activeTransportIsSsh() &&
                realGit.sshKeyStore().hasKey();

            if (!sshReady && !tokenStore.hasToken()) {
                appendPlain(
                    "Authentification requise : tape gh auth login, puis relance la commande.\n",
                    ERROR_RED
                );
                refreshTerminal();
                return;
            }
        }

        commandBusy = true;
        appendPlain("[réel] exécution…\n", Color.rgb(238,176,96));
        refreshTerminal();

        executor.submit(() -> {
            try {
                String output = realGit.execute(command);

                runOnUiThread(() -> {
                    commandBusy = false;
                    if (!output.isEmpty()) {
                        int color =
                            output.startsWith("fatal") ||
                            output.startsWith("error") ||
                            output.contains("non encore prise en charge")
                                ? ERROR_RED
                                : TERMINAL_TEXT;

                        appendPlain(output + "\n", color);
                    }

                    updateCommandPrompt();
                    refreshTerminal();
                    refreshRealStatusTextOnly();
                    scrollBottom();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    commandBusy = false;
                    appendPlain(
                        "Erreur Git réelle : " + safeMessage(e) + "\n",
                        ERROR_RED
                    );
                    refreshTerminal();
                    scrollBottom();
                });
            }
        });
    }

    private boolean needsNetworkCredentials(String command) {
        String n = normalize(command);

        return n.startsWith("git clone ") ||
            n.startsWith("git fetch") ||
            n.startsWith("git pull") ||
            n.startsWith("git push") ||
            n.startsWith("git ls-remote");
    }

    private boolean isDangerousRealCommand(String command) {
        String n = normalize(command);

        return n.startsWith("git push --force-with-lease") ||
            n.startsWith("git push origin --delete ");
    }

    private void confirmDangerousRealCommand(String command) {
        String repo = realGit.selectedRepositoryName();
        if (repo.isEmpty()) repo = "(dépôt non identifié)";

        final String target = repo;

        new AlertDialog.Builder(this)
            .setTitle("Confirmer l'opération distante")
            .setMessage(
                "Environnement : GITHUB RÉEL\n" +
                "origin = " + target + "\n\n" +
                command + "\n\n" +
                "Cette opération peut modifier ou supprimer l'historique distant."
            )
            .setNegativeButton("Annuler", (d, w) -> {
                appendPlain("[annulé] " + command + "\n", TERMINAL_MUTED);
                refreshTerminal();
            })
            .setPositiveButton(
                "Je confirme",
                (d, w) -> runRealCommandAsync(command)
            )
            .show();
    }

    private void showNanoEditor(String path) { showNanoEditor(path, false); }

    private void showNanoEditor(String path, boolean realFile) {
        String content;
        try { content = realFile ? realGit.readEditorFile(path) : vm.readFileForEditor(path); }
        catch (Exception e) { appendPlain("Erreur fichier réel : " + safeMessage(e) + "\n", ERROR_RED); refreshTerminal(); return; }
        EditText editor = new EditText(this);
        editor.setMinLines(10);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setTypeface(Typeface.MONOSPACE);
        editor.setTextSize(13f);
        editor.setInputType(
            InputType.TYPE_CLASS_TEXT |
            InputType.TYPE_TEXT_FLAG_MULTI_LINE |
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        );
        editor.setText(content);
        editor.setSelection(editor.getText().length());
        editor.setPadding(dp(14), dp(12), dp(14), dp(12));

        new AlertDialog.Builder(this)
            .setTitle((realFile ? "Fichier réel — " : "GNU nano — ") + path)
            .setView(editor)
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Enregistrer", (dialog, which) -> {
                try {
                    if (realFile) realGit.saveEditorFile(path, editor.getText().toString());
                    else { vm.saveEditedFile(path, editor.getText().toString()); missionReplay.add("E:" + encode(path) + ":" + encode(editor.getText().toString())); }
                    appendPlain(realFile ? "[réel] Fichier enregistré sur le téléphone. Pour l'envoyer : git add, git commit, git push.\n" : "[nano] fichier enregistré\n", SUCCESS_GREEN);
                } catch (Exception e) { appendPlain("Échec de l'enregistrement : " + safeMessage(e) + "\n", ERROR_RED); }
                updateCommandPrompt();
                refreshTerminal();
                commandInput.requestFocus();
            })
            .show();
    }

    private void showTokenDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(6), dp(20), 0);

        TextView info = terminalText(
            "Utilise un jeton GitHub à permissions fines. " +
            "Le jeton est chiffré par Android Keystore et n'est jamais enregistré dans le code source.\n\n" +
            "Pour pull : Contents en lecture. Pour push : Contents en lecture/écriture. " +
            "Pour ajouter automatiquement la clé SSH : permission utilisateur Git SSH keys en écriture.",
            12,
            false,
            Color.rgb(45,45,49)
        );
        box.addView(info);

        EditText token = new EditText(this);
        token.setHint("github_pat_…");
        token.setSingleLine(true);
        token.setInputType(
            InputType.TYPE_CLASS_TEXT |
            InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        box.addView(token);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Connexion GitHub réelle")
            .setView(box)
            .setNegativeButton("Annuler", null)
            .setNeutralButton("Créer un token", null)
            .setPositiveButton("Valider", null)
            .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener(v -> {
                    Intent browser = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/settings/personal-access-tokens/new")
                    );
                    startActivity(browser);
                });

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String value = token.getText().toString().trim();

                    if (value.isEmpty()) {
                        token.setError("Jeton requis");
                        return;
                    }

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    validateAndStoreToken(value, dialog);
                });
        });

        dialog.show();
    }

    private void validateAndStoreToken(String token, AlertDialog dialog) {
        realStatusView.setText("Vérification du compte GitHub…");

        executor.submit(() -> {
            try {
                GitHubApiClient api = new GitHubApiClient(token);
                String login = api.getLogin();

                tokenStore.saveToken(token);
                prefs.edit().putString("realGitHubLogin", login).apply();

                runOnUiThread(() -> {
                    dialog.dismiss();
                    appendPlain(
                        "[GitHub réel] connecté en tant que @" + login + "\n",
                        SUCCESS_GREEN
                    );
                    refreshTerminal();
                    refreshRealStatusTextOnly();
                    if (realGit.selectedRepositoryName().isEmpty()) chooseRepository();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    realStatusView.setText("Connexion GitHub non validée.");
                    appendPlain(
                        "Connexion GitHub échouée : " + safeMessage(e) + "\n",
                        ERROR_RED
                    );
                    refreshTerminal();
                });
            }
        });
    }

    private void chooseRepository() {
        String token = tokenStore.loadToken();

        if (token.isEmpty()) {
            showTokenDialog();
            return;
        }

        realStatusView.setText("Chargement des dépôts GitHub…");

        executor.submit(() -> {
            try {
                GitHubApiClient api = new GitHubApiClient(token);
                List<GitHubApiClient.RepoInfo> repos = api.listRepositories();

                runOnUiThread(() -> showRepositoryPicker(repos));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    realStatusView.setText("Impossible de charger les dépôts.");
                    appendPlain(
                        "GitHub : " + safeMessage(e) + "\n",
                        ERROR_RED
                    );
                    refreshTerminal();
                });
            }
        });
    }

    private void showRepositoryPicker(List<GitHubApiClient.RepoInfo> repos) {
        if (repos.isEmpty()) {
            realStatusView.setText("Aucun dépôt accessible avec ce jeton.");
            return;
        }

        String[] labels = new String[repos.size()];

        for (int i = 0; i < repos.size(); i++) {
            labels[i] = repos.get(i).toString();
        }

        new AlertDialog.Builder(this)
            .setTitle("Choisir un dépôt GitHub")
            .setItems(labels, (dialog, which) -> {
                GitHubApiClient.RepoInfo selected = repos.get(which);

                realGit.selectRepository(
                    selected.fullName,
                    selected.cloneUrl,
                    selected.defaultBranch
                );

                appendPlain(
                    "[GitHub réel] Dépôt sélectionné pour clonage : " + selected.fullName + "\n",
                    SUCCESS_GREEN
                );

                refreshRealStatusTextOnly();
                refreshTerminal();

                new AlertDialog.Builder(this)
                    .setTitle("Cloner ce dépôt ?")
                    .setMessage(
                        selected.fullName +
                        "\n\nLe clone sera placé uniquement dans l'espace privé de l'application."
                    )
                    .setNegativeButton("Plus tard", null)
                    .setPositiveButton(
                        "Cloner maintenant",
                        (d, w) -> cloneSelectedRepository(false)
                    )
                    .show();
            })
            .show();
    }

    private void cloneSelectedRepository(boolean replaceExisting) {
        if (realGit.selectedRepositoryUrl().isEmpty()) {
            appendPlain(
                "Choisis d'abord un dépôt GitHub.\n",
                ERROR_RED
            );
            refreshTerminal();
            chooseRepository();
            return;
        }

        appendPlain(
            "[réel] clonage de " + realGit.selectedRepositoryName() + "…\n",
            Color.rgb(238,176,96)
        );
        refreshTerminal();

        executor.submit(() -> {
            try {
                String result = realGit.cloneSelectedRepository(replaceExisting);

                runOnUiThread(() -> {
                    appendPlain(result + "\n", SUCCESS_GREEN);
                    updateCommandPrompt();
                    refreshRealStatusTextOnly();
                    refreshTerminal();
                    scrollBottom();
                });
            } catch (Exception e) {
                String message = safeMessage(e);

                runOnUiThread(() -> {
                    if (message.contains("Un autre dépôt existe déjà") && !replaceExisting) {
                        confirmReplaceLocalRepo();
                    } else {
                        appendPlain(
                            "Clonage échoué : " + message + "\n",
                            ERROR_RED
                        );
                        refreshTerminal();
                    }
                });
            }
        });
    }

    private void confirmReplaceLocalRepo() {
        new AlertDialog.Builder(this)
            .setTitle("Remplacer le dépôt local ?")
            .setMessage(
                "L'espace Git réel privé contient un autre dépôt. " +
                "Le remplacer supprimera uniquement cette copie interne à l'application, pas le dépôt GitHub."
            )
            .setNegativeButton("Annuler", null)
            .setPositiveButton(
                "Remplacer",
                (d, w) -> cloneSelectedRepository(true)
            )
            .show();
    }

    private void disconnectGitHub() {
        new AlertDialog.Builder(this)
            .setTitle("Se déconnecter de GitHub")
            .setMessage(
                "Le jeton chiffré sera supprimé du téléphone. " +
                "La copie locale du dépôt restera dans l'espace privé de l'application."
            )
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Déconnecter", (d, w) -> {
                tokenStore.clearToken();
                prefs.edit().remove("realGitHubLogin").apply();

                appendPlain(
                    "[GitHub réel] jeton supprimé du stockage sécurisé.\n",
                    SUCCESS_GREEN
                );

                refreshRealStatusTextOnly();
                refreshTerminal();
            })
            .show();
    }

    private void refreshRealStatus() {
        refreshRealStatusTextOnly();

        if (!tokenStore.hasToken()) return;

        executor.submit(() -> {
            try {
                String login = new GitHubApiClient(tokenStore.loadToken()).getLogin();
                prefs.edit().putString("realGitHubLogin", login).apply();

                runOnUiThread(this::refreshRealStatusTextOnly);
            } catch (Exception ignored) {
            }
        });
    }

    private void refreshRealStatusTextOnly() {
        if (realStatusView == null) return;

        String login = prefs.getString("realGitHubLogin", "");
        String repo = realGit.activeRepositoryName();
        String branch = realGit.currentBranch();

        StringBuilder text = new StringBuilder();

        text.append(tokenStore.hasToken()
            ? "Compte : @" + (login.isEmpty() ? "connecté" : login)
            : "Compte : non connecté");

        text.append("\norigin : ")
            .append(repo.isEmpty() ? "aucun remote dans ce dossier" : repo);

        text.append("\ndossier : ").append(realGit.displayPath());
        if (!branch.isEmpty()) {
            text.append("\nbranche locale : ").append(branch);
        }

        text.append("\ntransport Git : ")
            .append(realGit.activeTransportIsSsh() ? "SSH/JGit" : "HTTPS/JGit");

        if (realGit.sshKeyStore().hasKey()) {
            RealSshKeyStore.KeyInfo key = realGit.sshKeyStore().info();
            text.append("\nclé SSH : ")
                .append(key.algorithm)
                .append(" • ")
                .append(key.fingerprint);
        } else {
            text.append("\nclé SSH : non créée");
        }

        realStatusView.setText(text.toString());
        scoreView.setText(
            "GITHUB RÉEL  •  " +
            (repo.isEmpty() ? "aucun origin" : repo)
        );
    }

    private void generateRealSshKey() {
        String login = prefs.getString("realGitHubLogin", "");
        String comment = login.isEmpty()
            ? "ubuntu-git-academy"
            : login + "@ubuntu-git-academy";

        appendPlain("[réel] génération d'une clé SSH…\n", Color.rgb(238,176,96));
        refreshTerminal();

        executor.submit(() -> {
            try {
                RealSshKeyStore.KeyInfo info =
                    realGit.sshKeyStore().generate(comment);

                runOnUiThread(() -> {
                    appendPlain(
                        "Clé SSH réelle créée : " +
                        info.algorithm +
                        "\n" +
                        info.fingerprint +
                        "\n",
                        SUCCESS_GREEN
                    );

                    showPublicSshKey(info);
                    refreshRealStatusTextOnly();
                    refreshTerminal();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    appendPlain(
                        "Création SSH échouée : " + safeMessage(e) + "\n",
                        ERROR_RED
                    );
                    refreshTerminal();
                });
            }
        });
    }

    private void showPublicSshKey(RealSshKeyStore.KeyInfo info) {
        TextView keyView = new TextView(this);
        keyView.setTypeface(Typeface.MONOSPACE);
        keyView.setTextSize(11f);
        keyView.setTextColor(Color.rgb(30,30,34));
        keyView.setTextIsSelectable(true);
        keyView.setPadding(dp(16), dp(10), dp(16), dp(10));
        keyView.setText(
            info.publicKey +
            "\n\nEmpreinte : " +
            info.fingerprint +
            "\n\nLa clé privée reste chiffrée dans l'application et n'est jamais affichée."
        );

        new AlertDialog.Builder(this)
            .setTitle("Clé publique SSH")
            .setView(keyView)
            .setNegativeButton("Fermer", null)
            .setNeutralButton("Copier", (dialog, which) -> {
                ClipboardManager manager =
                    (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

                manager.setPrimaryClip(
                    ClipData.newPlainText(
                        "GitHub SSH public key",
                        info.publicKey
                    )
                );
            })
            .setPositiveButton(
                "Ajouter à GitHub",
                (dialog, which) -> uploadRealSshKey()
            )
            .show();
    }

    private void uploadRealSshKey() {
        if (!realGit.sshKeyStore().hasKey()) {
            appendPlain(
                "Crée d'abord une clé SSH réelle.\n",
                ERROR_RED
            );
            refreshTerminal();
            return;
        }

        if (!tokenStore.hasToken()) {
            appendPlain(
                "L'ajout automatique de la clé SSH nécessite une connexion GitHub avec la permission « Git SSH keys: write ».\n",
                ERROR_RED
            );
            refreshTerminal();
            showTokenDialog();
            return;
        }

        appendPlain("[réel] ajout de la clé publique au compte GitHub…\n", Color.rgb(238,176,96));
        refreshTerminal();

        executor.submit(() -> {
            try {
                RealSshKeyStore.KeyInfo info = realGit.sshKeyStore().info();
                String id = new GitHubApiClient(tokenStore.loadToken())
                    .addAuthenticationSshKey(
                        "Ubuntu Git Academy Android",
                        info.publicKey
                    );

                runOnUiThread(() -> {
                    appendPlain(
                        "Clé SSH ajoutée au compte GitHub (id " + id + ").\n",
                        SUCCESS_GREEN
                    );
                    refreshTerminal();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    appendPlain(
                        "Ajout GitHub échoué : " +
                        safeMessage(e) +
                        "\nVérifie que le token autorise « Git SSH keys: write ».\n",
                        ERROR_RED
                    );
                    refreshTerminal();
                });
            }
        });
    }

    private void toggleRealGitTransport() {
        boolean next = !realGit.isUseSsh();

        if (next && !realGit.sshKeyStore().hasKey()) {
            new AlertDialog.Builder(this)
                .setTitle("Clé SSH requise")
                .setMessage(
                    "Crée d'abord une clé SSH réelle. La clé privée restera chiffrée par Android Keystore."
                )
                .setNegativeButton("Annuler", null)
                .setPositiveButton(
                    "Créer la clé",
                    (d, w) -> generateRealSshKey()
                )
                .show();
            return;
        }

        realGit.setUseSsh(next);

        executor.submit(() -> {
            try {
                realGit.applySelectedTransportToOrigin();

                runOnUiThread(() -> {
                    appendPlain(
                        "Transport Git réel : " +
                        (realGit.isUseSsh() ? "SSH" : "HTTPS") +
                        "\n",
                        SUCCESS_GREEN
                    );
                    refreshRealStatusTextOnly();
                    refreshTerminal();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    appendPlain(
                        "Impossible de changer le transport : " +
                        safeMessage(e) +
                        "\n",
                        ERROR_RED
                    );
                    refreshTerminal();
                });
            }
        });
    }

    private void confirmReset() {
        if (realEnvironment) {
            new AlertDialog.Builder(this)
                .setTitle("Reset VM")
                .setMessage(
                    "Le bouton Reset VM ne supprime pas le dépôt GitHub réel. " +
                    "Repasse en SIMULATION pour réinitialiser la machine virtuelle."
                )
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("Réinitialiser la simulation")
            .setMessage(
                "Tous les fichiers, commits et états Git simulés seront remis à zéro."
            )
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Réinitialiser", (d, w) -> {
                vm.reset();
                terminal.clear();
                appendSystem("[SIMULATION] Machine virtuelle réinitialisée.");

                if (guidedMode) showScenario();
                else showFreeSimulation();

                updateCommandPrompt();
                refreshTerminal();
            })
            .show();
    }

    private void historyPrevious() {
        if (awaitingToken || inputHistory.isEmpty()) return;

        historyCursor = Math.max(0, historyCursor - 1);
        commandInput.setCommand(inputHistory.get(historyCursor));
        commandInput.setSelection(commandInput.getText().length());
    }

    private void historyNext() {
        if (awaitingToken || inputHistory.isEmpty()) return;

        historyCursor = Math.min(inputHistory.size(), historyCursor + 1);

        if (historyCursor >= inputHistory.size()) {
            commandInput.setCommand("");
        } else {
            commandInput.setCommand(inputHistory.get(historyCursor));
            commandInput.setSelection(commandInput.getText().length());
        }
    }

    private void updateCommandPrompt() {
        if (commandPromptView == null) return;

        String userHost = "ubuntu@academy";

        String path = realEnvironment
            ? realGit.displayPath()
            : vm.shortCwd();

        SpannableStringBuilder line = new SpannableStringBuilder();

        appendSpan(line, userHost, realEnvironment ? REAL_RED : PROMPT_GREEN, true);
        appendSpan(line, ":", TERMINAL_TEXT, false);
        appendSpan(line, path, PATH_BLUE, true);
        appendSpan(line, "$ ", TERMINAL_TEXT, false);

        commandPromptView.setText(line);
        refreshTerminal();
    }

    private void appendPrompt(String command) {
        String userHost = "ubuntu@academy";

        String path = realEnvironment
            ? realGit.displayPath()
            : vm.shortCwd();

        appendStyled(
            userHost,
            realEnvironment ? REAL_RED : PROMPT_GREEN,
            true
        );
        appendStyled(":", TERMINAL_TEXT, false);
        appendStyled(path, PATH_BLUE, true);
        appendStyled("$ ", TERMINAL_TEXT, false);
        appendStyled(command, TERMINAL_TEXT, false);
        appendRaw("\n");
    }

    private void appendSystem(String text) {
        appendStyled(text + "\n", Color.rgb(207,171,199), false);
    }

    private void appendPlain(String text, int color) {
        appendStyled(text, color, false);
    }

    private void appendRaw(String text) {
        terminal.append(text);
    }

    private void appendStyled(String text, int color, boolean bold) {
        int start = terminal.length();
        terminal.append(text);
        int end = terminal.length();

        terminal.setSpan(
            new ForegroundColorSpan(color),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        if (bold) {
            terminal.setSpan(
                new StyleSpan(Typeface.BOLD),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }

    private void appendSpan(
        SpannableStringBuilder target,
        String text,
        int color,
        boolean bold
    ) {
        int start = target.length();
        target.append(text);
        int end = target.length();

        target.setSpan(
            new ForegroundColorSpan(color),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        if (bold) {
            target.setSpan(
                new StyleSpan(Typeface.BOLD),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }

    private void refreshTerminal() {
        if (terminalView != null) {
            terminalView.render(terminal, awaitingToken ? "Jeton GitHub (masqué) : " : commandPromptView == null ? "$ " : commandPromptView.getText());
            saveSession();
        }
    }

    private void scrollBottom() {
        if (scroll == null || root == null) return;

        // Do not use fullScroll(FOCUS_DOWN) here: ScrollView treats that as
        // keyboard focus navigation and can immediately steal focus from the
        // command EditText. Scroll only by coordinates so the text field keeps
        // focus while the user types.
        scroll.post(() -> {
            int target = Math.max(0, root.getHeight() - scroll.getHeight());
            scroll.smoothScrollTo(0, target);
        });
    }

    private boolean normalizeEquals(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    private String normalize(String value) {
        return value == null
            ? ""
            : value.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.trim().isEmpty()
            ? e.getClass().getSimpleName()
            : message;
    }

    private void protectFromSystemBars(View view) {
        final int baseLeft = view.getPaddingLeft();
        final int baseTop = view.getPaddingTop();
        final int baseRight = view.getPaddingRight();
        final int baseBottom = view.getPaddingBottom();

        view.setFitsSystemWindows(false);

        view.setOnApplyWindowInsetsListener((v, insets) -> {
            int left;
            int top;
            int right;
            int bottom;

            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars()
                );
                android.graphics.Insets ime = insets.getInsets(
                    WindowInsets.Type.ime()
                );

                left = bars.left;
                top = bars.top;
                right = bars.right;

                // Critical keyboard fix: reserve whichever is taller,
                // navigation bar or IME. With edge-to-edge enabled, this
                // physically lifts the complete command bar (prompt + input +
                // Exécuter button) above the keyboard instead of letting the
                // keyboard draw over it.
                bottom = Math.max(bars.bottom, ime.bottom);

                if (insets.isVisible(WindowInsets.Type.ime())) {
                    scroll.postDelayed(this::scrollBottom, 80);
                }
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }

            v.setPadding(
                baseLeft + left,
                baseTop + top + dp(4),
                baseRight + right,
                baseBottom + bottom + dp(3)
            );

            return insets;
        });

        // Device/manufacturer fallback. Some keyboards on older Android
        // versions overlay the app instead of reporting a useful IME inset.
        // When that happens, measure the visible window and translate only
        // the fixed command bar above the obscured area.
        view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (Build.VERSION.SDK_INT >= 30) return;
            if (commandBar == null || commandBar.getHeight() == 0) return;

            Rect visible = new Rect();
            view.getWindowVisibleDisplayFrame(visible);

            int[] location = new int[2];
            commandBar.getLocationOnScreen(location);

            int commandBottom = location[1] + commandBar.getHeight();
            int overlap = commandBottom - visible.bottom;

            if (overlap > dp(2)) {
                commandBar.setTranslationY(-overlap - dp(4));
                scroll.postDelayed(this::scrollBottom, 50);
            } else if (commandBar.getTranslationY() != 0f) {
                commandBar.setTranslationY(0f);
            }
        });

        view.requestApplyInsets();
    }

    private LinearLayout panel(int color, int radius) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(11), dp(9), dp(11), dp(9));
        layout.setBackground(rounded(color, radius, 0));
        return layout;
    }

    private TextView terminalText(
        String text,
        float size,
        boolean bold,
        int color
    ) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(
            Typeface.MONOSPACE,
            bold ? Typeface.BOLD : Typeface.NORMAL
        );
        return view;
    }

    private TextView terminalText(
        String text,
        int size,
        boolean bold,
        int color
    ) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(
            Typeface.MONOSPACE,
            bold ? Typeface.BOLD : Typeface.NORMAL
        );
        return view;
    }

    private Button accentButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(11f);
        button.setTextColor(Color.WHITE);
        button.setBackground(rounded(UBUNTU_ORANGE, 9, 0));
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(10), dp(7), dp(10), dp(7));
        return button;
    }

    private Button smallButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(10.5f);
        button.setTextColor(Color.rgb(45,45,49));
        button.setBackground(rounded(Color.rgb(232,232,236), 9, 0));
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(9), dp(6), dp(9), dp(6));
        return button;
    }

    private Button compactButton(String text) {
        Button button = smallButton(text);
        button.setTextSize(14f);
        button.setPadding(dp(7), dp(5), dp(7), dp(5));
        return button;
    }

    private Button smallFullButton(String text) {
        Button button = smallButton(text);
        button.setLayoutParams(
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        );
        return button;
    }

    private Button choiceButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(11.5f);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setTypeface(Typeface.MONOSPACE);
        button.setBackground(
            rounded(
                TERMINAL_PANEL,
                8,
                Color.rgb(88,88,96)
            )
        );
        button.setPadding(dp(11), dp(8), dp(11), dp(8));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(5);
        button.setLayoutParams(params);

        return button;
    }

    private GradientDrawable rounded(
        int fill,
        int radiusDp,
        int stroke
    ) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));

        if (stroke != 0) {
            drawable.setStroke(dp(1), stroke);
        }

        return drawable;
    }

    private LinearLayout.LayoutParams topMargin(int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(top);
        return params;
    }

    private int dp(int value) {
        return (int) (
            value * getResources().getDisplayMetrics().density
        );
    }
}
