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

    private TextView terminalView;
    private TextView objectiveView;
    private TextView scoreView;
    private TextView interactionLabel;
    private TextView realStatusView;
    private TextView commandPromptView;
    private TextView sectionTitleView;

    private EditText commandInput;

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

        appendSystem("Ubuntu 24.04 LTS — Ubuntu & Git Academy");
        appendSystem("Environnement pédagogique sécurisé. Tape help pour l'aide.");

        if (realEnvironment) {
            setEnvironment(true);
        } else {
            setEnvironment(false);
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
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

        TextView indicators = terminalText("●  Wi-Fi  100%", 10, false, Color.rgb(226,226,230));
        indicators.setGravity(Gravity.END);
        bar.addView(
            indicators,
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        );

        root.addView(bar);
    }

    private void addCompactHeader() {
        LinearLayout header = panel(Color.rgb(67, 18, 51), 14);

        TextView title = terminalText(">_ Ubuntu & Git Academy", 18, true, Color.WHITE);
        header.addView(title);

        TextView subtitle = terminalText(
            "Terminal Ubuntu simulé + GitHub réel HTTPS",
            11,
            false,
            Color.rgb(221,204,217)
        );
        subtitle.setPadding(0, dp(3), 0, 0);
        header.addView(subtitle);

        root.addView(header, topMargin(3));

        scoreView = terminalText("", 11, true, Color.WHITE);
        scoreView.setPadding(dp(2), dp(7), dp(2), dp(5));
        root.addView(scoreView);
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
        realControls = panel(Color.rgb(55, 24, 36), 13);

        realStatusView = terminalText("", 11, false, Color.WHITE);
        realStatusView.setPadding(0, 0, 0, dp(6));
        realControls.addView(realStatusView);

        LinearLayout first = new LinearLayout(this);
        first.setOrientation(LinearLayout.HORIZONTAL);

        Button connect = smallButton("Connexion GitHub");
        connect.setOnClickListener(v -> showTokenDialog());
        first.addView(
            connect,
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        );

        Button repos = smallButton("Choisir dépôt");
        repos.setOnClickListener(v -> chooseRepository());
        LinearLayout.LayoutParams reposParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        );
        reposParams.leftMargin = dp(6);
        first.addView(repos, reposParams);

        realControls.addView(first);

        LinearLayout second = new LinearLayout(this);
        second.setOrientation(LinearLayout.HORIZONTAL);
        second.setPadding(0, dp(6), 0, 0);

        Button clone = smallButton("Cloner / ouvrir");
        clone.setOnClickListener(v -> cloneSelectedRepository(false));
        second.addView(
            clone,
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        );

        Button disconnect = smallButton("Déconnexion");
        disconnect.setOnClickListener(v -> disconnectGitHub());
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        );
        dp.leftMargin = this.dp(6);
        second.addView(disconnect, dp);

        realControls.addView(second);
        root.addView(realControls, topMargin(5));
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

        terminalView = new TextView(this);
        terminalView.setTypeface(Typeface.MONOSPACE);
        terminalView.setTextSize(12f);
        terminalView.setTextColor(TERMINAL_TEXT);
        terminalView.setTextIsSelectable(true);
        terminalView.setLineSpacing(0f, 1.03f);
        terminalView.setPadding(0, dp(6), 0, dp(5));
        terminalView.setMinLines(8);
        terminalView.setText(terminal);

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

        Button reset = smallButton("Reset VM");
        reset.setOnClickListener(v -> confirmReset());
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
        commandBar.setPadding(dp(9), dp(6), dp(9), dp(7));
        commandBar.setBackgroundColor(Color.rgb(18,18,20));

        commandPromptView = new TextView(this);
        commandPromptView.setTypeface(Typeface.MONOSPACE);
        commandPromptView.setTextSize(10.5f);
        commandPromptView.setPadding(dp(2), 0, dp(2), dp(3));
        commandBar.addView(commandPromptView);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);

        Button previous = compactButton("↑");
        previous.setOnClickListener(v -> historyPrevious());
        inputRow.addView(previous);

        Button next = compactButton("↓");
        next.setOnClickListener(v -> historyNext());
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        nextParams.leftMargin = dp(3);
        inputRow.addView(next, nextParams);

        commandInput = new EditText(this);
        commandInput.setSingleLine(true);
        commandInput.setFocusable(true);
        commandInput.setFocusableInTouchMode(true);
        commandInput.setTextColor(Color.WHITE);
        commandInput.setHintTextColor(Color.rgb(135,135,142));
        commandInput.setHint("commande…");
        commandInput.setTypeface(Typeface.MONOSPACE);
        commandInput.setTextSize(12.5f);
        commandInput.setInputType(
            InputType.TYPE_CLASS_TEXT |
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        );
        commandInput.setBackground(
            rounded(
                Color.rgb(39,39,43),
                8,
                Color.rgb(74,74,82)
            )
        );
        commandInput.setPadding(dp(9), dp(7), dp(9), dp(7));
        commandInput.setImeOptions(EditorInfo.IME_ACTION_GO);

        commandInput.setOnFocusChangeListener((v, focused) -> {
            if (focused) {
                scroll.postDelayed(this::scrollBottom, 150);
            }
        });

        commandInput.setOnClickListener(v -> {
            if (!commandInput.hasFocus()) {
                commandInput.requestFocus();
            }
            scroll.postDelayed(this::scrollBottom, 120);
        });

        commandInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter =
                actionId == EditorInfo.IME_ACTION_GO ||
                (event != null &&
                 event.getAction() == KeyEvent.ACTION_DOWN &&
                 event.getKeyCode() == KeyEvent.KEYCODE_ENTER);

            if (enter) {
                executeInput();
                return true;
            }

            return false;
        });

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        );
        inputParams.leftMargin = dp(5);
        inputParams.rightMargin = dp(5);
        inputRow.addView(commandInput, inputParams);

        Button execute = accentButton("Exécuter");
        execute.setTextSize(11f);
        execute.setOnClickListener(v -> executeInput());
        inputRow.addView(execute);

        commandBar.addView(inputRow);

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
        String[] items = {
            "Terminal libre",
            "Missions guidées",
            "GitHub réel",
            "Aide & commandes",
            "Retour à l'Academy"
        };

        new AlertDialog.Builder(this)
            .setTitle("Ubuntu Lab — rubriques")
            .setItems(items, (dialog, which) -> {
                if (which == 0) {
                    setEnvironment(false);
                    setGuidedMode(false);
                } else if (which == 1) {
                    setEnvironment(false);
                    setGuidedMode(true);
                } else if (which == 2) {
                    setEnvironment(true);
                } else if (which == 3) {
                    showHelpSections();
                } else {
                    finish();
                }
            })
            .show();
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
            sectionTitleView.setText("GitHub réel");
            realControls.setVisibility(View.VISIBLE);
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
            realControls.setVisibility(View.VISIBLE);
            choicesBox.removeAllViews();
            nextMissionButton.setVisibility(View.GONE);
            interactionButton.setVisibility(View.GONE);

            Button hint = findHintButton();
            if (hint != null) hint.setVisibility(View.GONE);

            interactionLabel.setText("MODE : terminal Git réel");
            objectiveView.setText(
                "GITHUB RÉEL\n\n" +
                "Les commandes Git prises en charge utilisent réellement Internet et un dépôt stocké dans l'espace privé de l'application. " +
                "Aucune opération réelle n'est lancée depuis le mode SIMULATION."
            );

            appendSystem("[GITHUB RÉEL] Les opérations réseau seront clairement signalées.");
            refreshRealStatus();
        } else {
            realControls.setVisibility(View.GONE);
            simulationModes.setVisibility(View.VISIBLE);

            appendSystem("[SIMULATION] Machine Ubuntu virtuelle locale.");
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
        if (realEnvironment) return;

        guidedMode = guided;
        prefs.edit().putBoolean("simGuidedMode", guided).apply();

        styleLearningButtons();

        if (guided) showScenario();
        else showFreeSimulation();
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

        if (!scenario.setupKey.isEmpty()) {
            vm.prepareScenario(scenario.setupKey);
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
        String command = commandInput.getText().toString().trim();

        if (command.isEmpty()) return;

        commandInput.setText("");
        inputHistory.add(command);
        historyCursor = inputHistory.size();

        runCommand(command);

        commandInput.requestFocus();
        scroll.postDelayed(this::scrollBottom, 100);
    }

    private void runCommand(String command) {
        if (command == null || command.trim().isEmpty()) return;

        appendPrompt(command);

        if (realEnvironment) {
            if (isDangerousRealCommand(command)) {
                confirmDangerousRealCommand(command);
            } else {
                runRealCommandAsync(command);
            }
            return;
        }

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

            boolean longFormat = command.contains("-l");

            for (int i = 0; i < result.entries.size(); i++) {
                VirtualMachine.FsEntry entry = result.entries.get(i);

                if (longFormat) {
                    appendPlain(
                        entry.directory ? "drwxr-xr-x  " : "-rw-r--r--  ",
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

        objectiveView.postDelayed(this::showScenario, 550);
    }

    private void runRealCommandAsync(String command) {
        if (!tokenStore.hasToken() &&
            needsNetworkCredentials(command)) {

            appendPlain(
                "GitHub réel : connecte d'abord ton compte avec « Connexion GitHub ».\n",
                ERROR_RED
            );
            refreshTerminal();
            return;
        }

        appendPlain("[réel] exécution…\n", Color.rgb(238,176,96));
        refreshTerminal();

        executor.submit(() -> {
            try {
                String output = realGit.execute(command);

                runOnUiThread(() -> {
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

    private void showNanoEditor(String path) {
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
        editor.setText(vm.readFileForEditor(path));
        editor.setSelection(editor.getText().length());
        editor.setPadding(dp(14), dp(12), dp(14), dp(12));

        new AlertDialog.Builder(this)
            .setTitle("GNU nano — " + path)
            .setView(editor)
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Enregistrer", (dialog, which) -> {
                vm.saveEditedFile(path, editor.getText().toString());
                appendPlain("[nano] fichier enregistré\n", SUCCESS_GREEN);
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
            "Pour pull : Contents en lecture. Pour push : Contents en lecture/écriture.",
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
                    "[GitHub réel] origin = " + selected.fullName + "\n",
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
        String repo = realGit.selectedRepositoryName();
        String branch = realGit.currentBranch();

        StringBuilder text = new StringBuilder();

        text.append(tokenStore.hasToken()
            ? "Compte : @" + (login.isEmpty() ? "connecté" : login)
            : "Compte : non connecté");

        text.append("\norigin : ")
            .append(repo.isEmpty() ? "aucun dépôt sélectionné" : repo);

        if (!branch.isEmpty()) {
            text.append("\nbranche locale : ").append(branch);
        }

        text.append("\nmode réseau : HTTPS/JGit");

        realStatusView.setText(text.toString());
        scoreView.setText(
            "GITHUB RÉEL  •  " +
            (repo.isEmpty() ? "aucun origin" : repo)
        );
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
        if (inputHistory.isEmpty()) return;

        historyCursor = Math.max(0, historyCursor - 1);
        commandInput.setText(inputHistory.get(historyCursor));
        commandInput.setSelection(commandInput.getText().length());
    }

    private void historyNext() {
        if (inputHistory.isEmpty()) return;

        historyCursor = Math.min(inputHistory.size(), historyCursor + 1);

        if (historyCursor >= inputHistory.size()) {
            commandInput.setText("");
        } else {
            commandInput.setText(inputHistory.get(historyCursor));
            commandInput.setSelection(commandInput.getText().length());
        }
    }

    private void updateCommandPrompt() {
        if (commandPromptView == null) return;

        String userHost = realEnvironment
            ? "github@academy"
            : "ubuntu@academy";

        String path = realEnvironment
            ? realGit.displayPath()
            : vm.shortCwd();

        SpannableStringBuilder line = new SpannableStringBuilder();

        appendSpan(line, userHost, realEnvironment ? REAL_RED : PROMPT_GREEN, true);
        appendSpan(line, ":", TERMINAL_TEXT, false);
        appendSpan(line, path, PATH_BLUE, true);
        appendSpan(line, "$", TERMINAL_TEXT, false);

        commandPromptView.setText(line);
    }

    private void appendPrompt(String command) {
        String userHost = realEnvironment
            ? "github@academy"
            : "ubuntu@academy";

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
            terminalView.setText(terminal);
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
