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
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
            "ls -la", "ls -al"
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
            new String[]{
                "git commit -m \"Ajoute le README\"",
                "git add -m \"Ajoute le README\"",
                "git push -m \"Ajoute le README\"",
                "git status -m \"Ajoute le README\""
            },
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
            new String[]{
                "git remote add origin git@github.com:USER/REPO.git",
                "git remote -v origin",
                "git push origin",
                "git init origin"
            },
            "git remote add origin git@github.com:USER/REPO.git"
        ),
        new Scenario(
            "Créer une clé SSH",
            "Tu n'as pas encore de clé SSH. Crée une paire de clés Ed25519 avec un commentaire email.",
            "La clé publique aura l'extension .pub ; la clé privée ne doit pas être partagée.",
            new String[]{
                "ssh-keygen -t ed25519 -C \"email\"",
                "ssh-add -t ed25519",
                "git keygen ed25519",
                "ssh -T ed25519"
            },
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
            new String[]{
                "git pull --rebase origin main",
                "git push --force origin main",
                "git clone origin main",
                "git status --rebase"
            },
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

    private ScrollView contentScroll;
    private LinearLayout contentRoot;
    private LinearLayout choicesBox;
    private LinearLayout objectivePanel;
    private LinearLayout commandBar;

    private TextView objectiveView;
    private TextView terminalView;
    private TextView scoreView;
    private TextView modeView;

    private EditText commandInput;
    private Button executeButton;
    private Button hintButton;
    private Button nextMissionButton;
    private Button interactionModeButton;
    private Button guidedModeButton;
    private Button freeModeButton;

    private int scenarioIndex = 0;
    private int labPoints = 0;

    private boolean typingMode = true;
    private boolean guidedMode = true;

    private final List<String> history = new ArrayList<>();
    private final List<String> commandHistory = new ArrayList<>();

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

        getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );

        labPoints = prefs.getInt("labPoints", 0);
        typingMode = prefs.getBoolean("simTypingMode", true);
        guidedMode = prefs.getBoolean("simGuidedMode", true);

        resetMachine();
        buildUi();

        if (guidedMode) {
            showScenario();
        } else {
            showFreeTerminal();
        }
    }

    private void resetMachine() {
        cwd = "/home/ubuntu";

        files.clear();
        dirs.clear();
        contents.clear();

        dirs.add("/home/ubuntu");
        dirs.add("Documents");
        dirs.add("Téléchargements");
        dirs.add("projet");

        files.add("notes.txt");
        files.add("README.md");

        contents.put("notes.txt", "Notes Ubuntu & Git");
        contents.put("README.md", "# Projet de démonstration");

        gitInit = false;
        staged = false;
        committed = false;
        remoteOrigin = "";
        sshKey = false;
        branch = "main";

        history.clear();
        commandHistory.clear();
        appendTerminal("Ubuntu 24.04 LTS — terminal simulé");
        appendTerminal("Tape help pour voir les commandes prises en charge.");
        appendTerminal("Aucune commande n'est exécutée sur le vrai téléphone.");
    }

    private void buildUi() {
        getWindow().setStatusBarColor(Color.rgb(48, 10, 36));
        getWindow().setNavigationBarColor(Color.rgb(17, 17, 20));

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(Color.rgb(34, 8, 28));

        contentScroll = new ScrollView(this);
        contentScroll.setFillViewport(true);
        contentScroll.setBackgroundColor(Color.rgb(34, 8, 28));

        contentRoot = new LinearLayout(this);
        contentRoot.setOrientation(LinearLayout.VERTICAL);
        contentRoot.setPadding(dp(14), dp(10), dp(14), dp(18));
        contentScroll.addView(contentRoot);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        );
        screen.addView(contentScroll, scrollParams);

        addTopBar();
        addHeader();
        addLabModeSelector();
        addMissionPanel();
        addInteractionModeRow();
        addTerminalWindow();

        choicesBox = new LinearLayout(this);
        choicesBox.setOrientation(LinearLayout.VERTICAL);
        choicesBox.setPadding(0, dp(10), 0, 0);
        contentRoot.addView(choicesBox);

        nextMissionButton = accentButton("Passer à l'objectif suivant");
        nextMissionButton.setOnClickListener(v -> {
            scenarioIndex = (scenarioIndex + 1) % scenarios.size();
            showScenario();
        });
        contentRoot.addView(nextMissionButton, marginTop(12));

        Button reset = smallFullButton("Réinitialiser le PC simulé");
        reset.setOnClickListener(v -> {
            resetMachine();
            refreshTerminal();
            if (guidedMode) showScenario();
            else showFreeTerminal();
        });
        contentRoot.addView(reset);

        Button back = smallFullButton("← Retour à l'Academy");
        back.setOnClickListener(v -> finish());
        contentRoot.addView(back);

        buildFixedCommandBar(screen);

        protectFromSystemBars(screen);
        setContentView(screen);
    }

    private void addTopBar() {
        LinearLayout topbar = new LinearLayout(this);
        topbar.setOrientation(LinearLayout.HORIZONTAL);
        topbar.setGravity(Gravity.CENTER_VERTICAL);
        topbar.setPadding(dp(8), dp(5), dp(8), dp(8));

        TextView activities = label("Activités", 13, true, Color.WHITE);
        topbar.addView(
            activities,
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        );

        TextView clock = label(
            "Ubuntu  •  Terminal  •  Wi-Fi  •  100%",
            12,
            false,
            Color.rgb(230,230,230)
        );
        topbar.addView(clock);

        contentRoot.addView(topbar);
    }

    private void addHeader() {
        LinearLayout header = panel(Color.rgb(68, 18, 52), 18);

        TextView title = label(">_  Ubuntu Lab", 24, true, Color.WHITE);
        header.addView(title);

        TextView subtitle = label(
            "Missions guidées ou terminal libre • environnement simulé et sécurisé",
            14,
            false,
            Color.rgb(226,210,222)
        );
        subtitle.setPadding(0, dp(6), 0, 0);
        header.addView(subtitle);

        contentRoot.addView(header, marginTop(4));

        scoreView = label("", 14, true, Color.WHITE);
        scoreView.setPadding(dp(4), dp(12), dp(4), dp(8));
        contentRoot.addView(scoreView);
    }

    private void addLabModeSelector() {
        LinearLayout selector = new LinearLayout(this);
        selector.setOrientation(LinearLayout.HORIZONTAL);
        selector.setPadding(0, 0, 0, dp(9));

        guidedModeButton = smallButton("Missions guidées");
        guidedModeButton.setOnClickListener(v -> setGuidedMode(true));
        selector.addView(
            guidedModeButton,
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        );

        freeModeButton = smallButton("Terminal libre");
        freeModeButton.setOnClickListener(v -> setGuidedMode(false));
        LinearLayout.LayoutParams freeParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        );
        freeParams.leftMargin = dp(7);
        selector.addView(freeModeButton, freeParams);

        contentRoot.addView(selector);
    }

    private void addMissionPanel() {
        objectivePanel = panel(Color.rgb(247, 244, 247), 18);

        TextView objectiveLabel = label("OBJECTIF", 12, true, Color.rgb(226,83,45));
        objectivePanel.addView(objectiveLabel);

        objectiveView = label("", 18, true, Color.rgb(30,30,34));
        objectiveView.setPadding(0, dp(7), 0, dp(7));
        objectivePanel.addView(objectiveView);

        hintButton = smallButton("Indice");
        hintButton.setOnClickListener(v -> {
            Scenario scenario = scenarios.get(scenarioIndex);
            appendTerminal("[indice] " + scenario.hint);
            refreshTerminal();
            scrollTerminalToBottom();
        });
        objectivePanel.addView(hintButton);

        contentRoot.addView(objectivePanel, marginTop(6));
    }

    private void addInteractionModeRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(7));

        modeView = label("", 13, true, Color.WHITE);
        row.addView(
            modeView,
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        );

        interactionModeButton = smallButton("Changer : QCM / saisie");
        interactionModeButton.setOnClickListener(v -> {
            typingMode = !typingMode;
            prefs.edit().putBoolean("simTypingMode", typingMode).apply();
            renderInteraction();
        });
        row.addView(interactionModeButton);

        contentRoot.addView(row);
    }

    private void addTerminalWindow() {
        LinearLayout terminalWindow = panel(Color.rgb(24, 24, 27), 16);

        LinearLayout terminalTitle = new LinearLayout(this);
        terminalTitle.setOrientation(LinearLayout.HORIZONTAL);
        terminalTitle.setGravity(Gravity.CENTER_VERTICAL);

        TextView dots = label("●  ●  ●", 12, true, Color.rgb(226,83,45));
        terminalTitle.addView(dots);

        TextView tty = label(
            "  ubuntu@academy",
            12,
            false,
            Color.rgb(190,190,194)
        );
        terminalTitle.addView(tty);

        terminalWindow.addView(terminalTitle);

        terminalView = label("", 14, false, Color.rgb(226,226,230));
        terminalView.setTypeface(Typeface.MONOSPACE);
        terminalView.setTextIsSelectable(true);
        terminalView.setText(historyText());
        terminalView.setPadding(0, dp(10), 0, dp(8));

        terminalWindow.addView(terminalView);
        contentRoot.addView(terminalWindow);
    }

    private void buildFixedCommandBar(LinearLayout screen) {
        commandBar = new LinearLayout(this);
        commandBar.setOrientation(LinearLayout.HORIZONTAL);
        commandBar.setGravity(Gravity.CENTER_VERTICAL);
        commandBar.setPadding(dp(12), dp(9), dp(12), dp(9));
        commandBar.setBackgroundColor(Color.rgb(17,17,20));

        TextView prompt = label("$", 18, true, Color.WHITE);
        prompt.setPadding(0, 0, dp(8), 0);
        commandBar.addView(prompt);

        commandInput = new EditText(this);
        commandInput.setSingleLine(true);
        commandInput.setTextColor(Color.WHITE);
        commandInput.setHintTextColor(Color.rgb(155,155,160));
        commandInput.setHint("écris une commande...");
        commandInput.setTypeface(Typeface.MONOSPACE);
        commandInput.setTextSize(15f);
        commandInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        commandInput.setBackground(
            rounded(Color.rgb(42,42,47), 12, Color.rgb(85,85,92))
        );
        commandInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        commandInput.setImeOptions(EditorInfo.IME_ACTION_GO);

        commandInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                contentScroll.postDelayed(this::scrollTerminalToBottom, 180);
            }
        });

        commandInput.setOnClickListener(v ->
            contentScroll.postDelayed(this::scrollTerminalToBottom, 180)
        );

        commandInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter =
                actionId == EditorInfo.IME_ACTION_GO ||
                (event != null &&
                 event.getAction() == KeyEvent.ACTION_DOWN &&
                 event.getKeyCode() == KeyEvent.KEYCODE_ENTER);

            if (enter) {
                executeTyped();
                return true;
            }
            return false;
        });

        commandBar.addView(
            commandInput,
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        );

        executeButton = accentButton("Exécuter");
        executeButton.setOnClickListener(v -> executeTyped());

        LinearLayout.LayoutParams executeParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        executeParams.leftMargin = dp(7);
        commandBar.addView(executeButton, executeParams);

        screen.addView(
            commandBar,
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        );
    }

    private void setGuidedMode(boolean guided) {
        guidedMode = guided;
        prefs.edit().putBoolean("simGuidedMode", guidedMode).apply();

        if (guidedMode) {
            showScenario();
        } else {
            showFreeTerminal();
        }
    }

    private void showScenario() {
        guidedMode = true;
        prefs.edit().putBoolean("simGuidedMode", true).apply();

        Scenario scenario = scenarios.get(scenarioIndex);

        objectivePanel.setVisibility(View.VISIBLE);
        interactionModeButton.setVisibility(View.VISIBLE);
        nextMissionButton.setVisibility(View.VISIBLE);

        objectiveView.setText(
            scenario.title + "\n\n" + scenario.objective
        );

        scoreView.setText(
            "Lab XP : " + labPoints +
            "   •   Mission " + (scenarioIndex + 1) + " / " + scenarios.size()
        );

        appendTerminal("");
        appendTerminal("[mission] " + scenario.title);

        styleLabModeButtons();
        renderInteraction();
        refreshTerminal();
    }

    private void showFreeTerminal() {
        guidedMode = false;
        prefs.edit().putBoolean("simGuidedMode", false).apply();

        objectivePanel.setVisibility(View.VISIBLE);
        objectiveView.setText(
            "Terminal libre\n\n" +
            "Écris librement des commandes. Le simulateur reproduit un sous-ensemble de Bash, Git et SSH sans toucher au vrai téléphone."
        );

        hintButton.setVisibility(View.GONE);
        interactionModeButton.setVisibility(View.GONE);
        choicesBox.removeAllViews();
        nextMissionButton.setVisibility(View.GONE);

        commandBar.setVisibility(View.VISIBLE);
        commandInput.setVisibility(View.VISIBLE);
        executeButton.setVisibility(View.VISIBLE);

        modeView.setText("MODE : terminal libre");
        scoreView.setText(
            "Lab XP : " + labPoints +
            "   •   environnement libre"
        );

        appendTerminal("");
        appendTerminal("[mode libre] Tape help pour afficher les commandes disponibles.");

        styleLabModeButtons();
        refreshTerminal();
        scrollTerminalToBottom();
    }

    private void styleLabModeButtons() {
        if (guidedMode) {
            guidedModeButton.setBackground(rounded(Color.rgb(226,83,45), 12, 0));
            guidedModeButton.setTextColor(Color.WHITE);

            freeModeButton.setBackground(rounded(Color.rgb(232,232,236), 12, 0));
            freeModeButton.setTextColor(Color.rgb(45,45,49));
        } else {
            freeModeButton.setBackground(rounded(Color.rgb(226,83,45), 12, 0));
            freeModeButton.setTextColor(Color.WHITE);

            guidedModeButton.setBackground(rounded(Color.rgb(232,232,236), 12, 0));
            guidedModeButton.setTextColor(Color.rgb(45,45,49));
        }
    }

    private void renderInteraction() {
        if (!guidedMode) {
            choicesBox.removeAllViews();
            commandBar.setVisibility(View.VISIBLE);
            commandInput.setVisibility(View.VISIBLE);
            executeButton.setVisibility(View.VISIBLE);
            modeView.setText("MODE : terminal libre");
            return;
        }

        hintButton.setVisibility(View.VISIBLE);
        interactionModeButton.setVisibility(View.VISIBLE);

        choicesBox.removeAllViews();

        if (typingMode) {
            modeView.setText("MISSION : commande à écrire");
            commandBar.setVisibility(View.VISIBLE);
            commandInput.setVisibility(View.VISIBLE);
            executeButton.setVisibility(View.VISIBLE);
        } else {
            modeView.setText("MISSION : 4 propositions");
            commandInput.clearFocus();
            commandBar.setVisibility(View.GONE);

            Scenario scenario = scenarios.get(scenarioIndex);
            List<String> shuffled = new ArrayList<>(Arrays.asList(scenario.choices));
            Collections.shuffle(shuffled);

            for (String choice : shuffled) {
                Button button = choiceButton(choice);
                button.setOnClickListener(v -> runCommand(choice));
                choicesBox.addView(button);
            }
        }
    }

    private void executeTyped() {
        String command = commandInput.getText().toString().trim();
        if (command.isEmpty()) return;

        commandInput.setText("");
        runCommand(command);
        commandInput.requestFocus();

        contentScroll.postDelayed(this::scrollTerminalToBottom, 120);
    }

    private void runCommand(String command) {
        if (command.trim().isEmpty()) return;

        appendTerminal("ubuntu@academy:" + shortCwd() + "$ " + command);
        commandHistory.add(command);

        String output = executeSimulated(command);
        if (!output.isEmpty()) appendTerminal(output);

        if (guidedMode) {
            evaluateMission(command);
        }

        refreshTerminal();
        scrollTerminalToBottom();
    }

    private void evaluateMission(String command) {
        Scenario scenario = scenarios.get(scenarioIndex);
        String normalized = normalize(command);

        boolean success = false;
        for (String accepted : scenario.accepted) {
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

            scoreView.setText(
                "Lab XP : " + labPoints + "   •   Mission réussie"
            );

            scenarioIndex = (scenarioIndex + 1) % scenarios.size();

            objectiveView.postDelayed(this::showScenario, 450);
        } else {
            appendTerminal(
                "↳ La commande a été simulée, mais elle ne valide pas encore l'objectif."
            );
        }
    }

    private String executeSimulated(String raw) {
        String command = raw.trim();

        if (command.isEmpty()) return "";

        if (command.contains("&&")) {
            StringBuilder combined = new StringBuilder();
            String[] parts = command.split("&&");

            for (String part : parts) {
                String out = executeSimulated(part.trim());
                if (!out.isEmpty()) {
                    if (combined.length() > 0) combined.append("\n");
                    combined.append(out);
                }
            }
            return combined.toString();
        }

        String n = normalize(command);

        if ("help".equals(n)) {
            return
                "Commandes simulées :\n" +
                "pwd, ls, ls -l, ls -la, cd, mkdir, mkdir -p, touch, cat, echo, cp, mv, rm, rmdir, clear, history\n" +
                "whoami, hostname, uname, uname -a, date, id\n" +
                "git --version, git init, git status, git add, git commit -m, git log, git diff, git branch, git switch -c\n" +
                "git remote -v, git remote add origin, git remote set-url origin, git fetch, git pull --rebase, git push\n" +
                "ssh-keygen -t ed25519, ssh -T git@github.com\n\n" +
                "Le simulateur n'exécute jamais de commande réelle sur Android.";
        }

        if ("clear".equals(n)) {
            history.clear();
            return "";
        }

        if ("history".equals(n)) {
            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < commandHistory.size(); i++) {
                builder.append(i + 1)
                    .append("  ")
                    .append(commandHistory.get(i))
                    .append("\n");
            }
            return builder.toString().trim();
        }

        if ("whoami".equals(n)) return "ubuntu";
        if ("hostname".equals(n)) return "academy";
        if ("id".equals(n)) return "uid=1000(ubuntu) gid=1000(ubuntu) groupes=1000(ubuntu)";
        if ("uname".equals(n)) return "Linux";
        if ("uname -a".equals(n)) {
            return "Linux academy 6.8.0-sim #1 SMP PREEMPT_DYNAMIC aarch64 GNU/Linux";
        }
        if ("date".equals(n)) {
            return new SimpleDateFormat(
                "EEE dd MMM yyyy HH:mm:ss",
                Locale.FRANCE
            ).format(new Date());
        }

        if ("pwd".equals(n)) return cwd;

        if ("ls".equals(n)) return listBasic(false);
        if ("ls -l".equals(n)) return listLong(false);
        if ("ls -la".equals(n) || "ls -al".equals(n)) return listLong(true);

        if ("cd".equals(n) || "cd ~".equals(n)) {
            cwd = "/home/ubuntu";
            return "";
        }

        if ("cd ..".equals(n)) {
            if (!"/".equals(cwd)) {
                int cut = cwd.lastIndexOf('/');
                cwd = cut <= 0 ? "/" : cwd.substring(0, cut);
            }
            return "";
        }

        if (n.startsWith("cd ")) {
            String target = command.substring(3).trim();

            if (target.startsWith("/")) {
                if (dirs.contains(target) || "/home/ubuntu".equals(target)) {
                    cwd = target;
                    return "";
                }
            }

            if (dirs.contains(target) || dirs.contains(cwd + "/" + target)) {
                cwd = target.startsWith("/")
                    ? target
                    : ("/".equals(cwd) ? "" : cwd) + "/" + target;
                return "";
            }

            return "bash: cd: " + target + ": Aucun fichier ou dossier de ce type";
        }

        if (n.startsWith("mkdir -p ")) {
            String path = command.substring(
                command.toLowerCase(Locale.ROOT).indexOf("-p") + 2
            ).trim();

            createDirectoryTree(path);
            return "";
        }

        if (n.startsWith("mkdir ")) {
            String target = command.substring(6).trim();
            dirs.add(target);
            return "";
        }

        if (n.startsWith("touch ")) {
            String target = command.substring(6).trim();
            files.add(target);
            contents.putIfAbsent(target, "");
            return "";
        }

        if (n.startsWith("cat ")) {
            String file = command.substring(4).trim();

            if ("~/.ssh/id_ed25519.pub".equals(file)) {
                return sshKey
                    ? "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAA... email"
                    : "cat: /home/ubuntu/.ssh/id_ed25519.pub: Aucun fichier ou dossier de ce type";
            }

            return files.contains(file)
                ? contents.getOrDefault(file, "")
                : "cat: " + file + ": Aucun fichier ou dossier de ce type";
        }

        if (n.startsWith("echo ")) {
            if (command.contains(">>") || command.contains(">")) {
                boolean append = command.contains(">>");
                String[] parts = command.split(append ? ">>" : ">", 2);

                if (parts.length == 2) {
                    String text = parts[0]
                        .substring(4)
                        .trim()
                        .replaceAll("^\\\"|\\\"$", "");

                    String file = parts[1].trim();

                    files.add(file);

                    if (append) {
                        String old = contents.getOrDefault(file, "");
                        contents.put(
                            file,
                            old + (old.isEmpty() ? "" : "\n") + text
                        );
                    } else {
                        contents.put(file, text);
                    }
                    return "";
                }
            }

            return command.substring(5)
                .trim()
                .replaceAll("^\\\"|\\\"$", "");
        }

        if (n.startsWith("cp -r ")) {
            String[] args = command.substring(6).trim().split("\\s+");

            if (args.length >= 2) {
                dirs.add(args[1]);
                return "";
            }

            return "cp: opérande de fichier manquant";
        }

        if (n.startsWith("cp ")) {
            String[] args = command.substring(3).trim().split("\\s+");

            if (args.length >= 2) {
                String src = args[0];
                String dst = args[1];

                if (!files.contains(src)) {
                    return "cp: impossible d'évaluer '" + src + "': Aucun fichier";
                }

                files.add(dst);
                contents.put(dst, contents.getOrDefault(src, ""));
                return "";
            }

            return "cp: opérande de fichier manquant";
        }

        if (n.startsWith("mv ")) {
            String[] args = command.substring(3).trim().split("\\s+");

            if (args.length >= 2) {
                String src = args[0];
                String dst = args[1];

                if (files.remove(src)) {
                    String content = contents.remove(src);
                    files.add(dst);
                    contents.put(dst, content == null ? "" : content);
                    return "";
                }

                if (dirs.remove(src)) {
                    dirs.add(dst);
                    return "";
                }

                return "mv: impossible d'évaluer '" + src + "': Aucun fichier";
            }

            return "mv: opérande de fichier manquant";
        }

        if (n.startsWith("rm -r ") || n.startsWith("rm -rf ")) {
            String target = command.substring(
                command.toLowerCase(Locale.ROOT).startsWith("rm -rf ") ? 7 : 6
            ).trim();

            dirs.remove(target);
            files.remove(target);
            contents.remove(target);
            return "";
        }

        if (n.startsWith("rm ")) {
            String target = command.substring(3).trim();

            if (files.remove(target)) {
                contents.remove(target);
                return "";
            }

            return "rm: impossible de supprimer '" + target + "': Aucun fichier";
        }

        if (n.startsWith("rmdir ")) {
            String target = command.substring(6).trim();

            if (dirs.remove(target)) return "";

            return "rmdir: échec de suppression de '" + target + "'";
        }

        if (n.startsWith("nano ")) {
            String target = command.substring(5).trim();
            files.add(target);
            contents.putIfAbsent(target, "");

            return
                "GNU nano (simulation) : '" + target + "' est prêt.\n" +
                "L'éditeur plein écran n'est pas reproduit ; utilise echo pour modifier son contenu.";
        }

        if ("git --version".equals(n)) {
            return "git version 2.43.0";
        }

        if ("git init".equals(n)) {
            gitInit = true;
            return "Dépôt Git vide initialisé dans " + cwd + "/.git/";
        }

        if ("git status".equals(n)) {
            if (!gitInit) return "fatal: not a git repository";

            if (staged) {
                return
                    "On branch " + branch + "\n" +
                    "Changes to be committed:\n" +
                    "  modified: README.md";
            }

            return
                "On branch " + branch + "\n" +
                "working tree simulé";
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

            String message = command
                .substring(command.toLowerCase(Locale.ROOT).indexOf("-m") + 2)
                .trim()
                .replace("\"", "");

            return "[" + branch + " a1b2c3d] " + message;
        }

        if ("git log".equals(n) || "git log -p".equals(n)) {
            return committed
                ? "commit a1b2c3d\nAuthor: ubuntu <ubuntu@academy>\n\n    Commit simulé"
                : "fatal: your current branch has no commits yet";
        }

        if ("git diff".equals(n)) {
            return "- ancienne ligne\n+ nouvelle ligne";
        }

        if ("git diff --staged".equals(n)) {
            return staged
                ? "- ancienne version\n+ version staged"
                : "";
        }

        if ("git branch".equals(n)) {
            return "* " + branch;
        }

        if (n.startsWith("git branch -d ")) {
            String target = command.substring("git branch -d ".length()).trim();

            if (target.equals(branch)) {
                return "error: Cannot delete branch '" + target + "' checked out";
            }

            return "Deleted branch " + target + " (was a1b2c3d).";
        }

        if (n.startsWith("git switch -c ")) {
            branch = command.substring("git switch -c ".length()).trim();
            return "Switched to a new branch '" + branch + "'";
        }

        if (n.startsWith("git switch ")) {
            branch = command.substring("git switch ".length()).trim();
            return "Switched to branch '" + branch + "'";
        }

        if ("git remote -v".equals(n)) {
            return remoteOrigin.isEmpty()
                ? ""
                : "origin  " + remoteOrigin + " (fetch)\n" +
                  "origin  " + remoteOrigin + " (push)";
        }

        if (n.startsWith("git remote add origin ")) {
            remoteOrigin = command.substring(
                "git remote add origin ".length()
            ).trim();
            return "";
        }

        if (n.startsWith("git remote set-url origin ")) {
            remoteOrigin = command.substring(
                "git remote set-url origin ".length()
            ).trim();
            return "";
        }

        if ("git fetch".equals(n) || "git fetch origin".equals(n)) {
            if (remoteOrigin.isEmpty()) {
                return "fatal: 'origin' does not appear to be a git repository";
            }

            return "From " + remoteOrigin + "\n * [new branch] main -> origin/main";
        }

        if ("git pull --rebase origin main".equals(n)) {
            if (remoteOrigin.isEmpty()) {
                return "fatal: 'origin' does not appear to be a git repository";
            }

            return
                "From " + remoteOrigin + "\n" +
                "Successfully rebased and updated refs/heads/" + branch + ".";
        }

        if (n.startsWith("git push -u origin ")) {
            if (remoteOrigin.isEmpty()) {
                return "fatal: 'origin' does not appear to be a git repository";
            }

            String pushedBranch = command.substring(
                "git push -u origin ".length()
            ).trim();

            return
                "branch '" + pushedBranch + "' set up to track 'origin/" +
                pushedBranch + "'.\nEverything up-to-date (simulation)";
        }

        if (n.startsWith("git push origin --delete ")) {
            if (remoteOrigin.isEmpty()) {
                return "fatal: 'origin' does not appear to be a git repository";
            }

            String deleted = command.substring(
                "git push origin --delete ".length()
            ).trim();

            return "- [deleted] " + deleted;
        }

        if (n.startsWith("git push origin ")) {
            if (remoteOrigin.isEmpty()) {
                return "fatal: 'origin' does not appear to be a git repository";
            }

            String pushed = command.substring(
                "git push origin ".length()
            ).trim();

            return "[new branch] " + pushed + " -> " + pushed;
        }

        if ("git push".equals(n)) {
            return remoteOrigin.isEmpty()
                ? "fatal: aucun remote configuré"
                : "Everything up-to-date (simulation)";
        }

        if (n.startsWith("ssh-keygen -t ed25519")) {
            sshKey = true;

            return
                "Your identification has been saved in /home/ubuntu/.ssh/id_ed25519\n" +
                "Your public key has been saved in /home/ubuntu/.ssh/id_ed25519.pub";
        }

        if ("ssh -t git@github.com".equals(n)) {
            return sshKey
                ? "Hi USER! You've successfully authenticated, but GitHub does not provide shell access."
                : "Permission denied (publickey).";
        }

        if (n.startsWith("sudo ")) {
            return
                "sudo: non disponible dans ce terminal simulé.\n" +
                "Aucune commande système réelle n'est exécutée sur Android.";
        }

        String firstWord = command.split("\\s+")[0];

        return
            firstWord + ": commande introuvable dans ce simulateur.\n" +
            "Tape help pour voir les commandes disponibles.";
    }

    private void createDirectoryTree(String path) {
        dirs.add(path);

        String[] parts = path.split("/");
        String accumulator = "";

        for (String part : parts) {
            if (part.isEmpty()) continue;

            accumulator = accumulator.isEmpty()
                ? part
                : accumulator + "/" + part;

            dirs.add(accumulator);
        }
    }

    private String listBasic(boolean hidden) {
        List<String> all = new ArrayList<>();

        all.addAll(dirs);
        all.addAll(files);

        Collections.sort(all);

        StringBuilder builder = new StringBuilder();

        if (hidden && gitInit) {
            builder.append(".git  ");
        }

        for (String item : all) {
            if (item.startsWith("/")) continue;

            builder.append(item).append("  ");
        }

        return builder.toString().trim();
    }

    private String listLong(boolean hidden) {
        StringBuilder builder = new StringBuilder();

        if (hidden) {
            builder.append("drwxr-xr-x  .\n");
            builder.append("drwxr-xr-x  ..\n");
        }

        if (hidden && gitInit) {
            builder.append("drwxr-xr-x  .git\n");
        }

        List<String> sortedDirs = new ArrayList<>(dirs);
        Collections.sort(sortedDirs);

        for (String dir : sortedDirs) {
            if (!dir.startsWith("/")) {
                builder.append("drwxr-xr-x  ")
                    .append(dir)
                    .append("\n");
            }
        }

        List<String> sortedFiles = new ArrayList<>(files);
        Collections.sort(sortedFiles);

        for (String file : sortedFiles) {
            builder.append("-rw-r--r--  ")
                .append(file)
                .append("\n");
        }

        return builder.toString().trim();
    }

    private String normalize(String value) {
        return value
            .trim()
            .replaceAll("\\s+", " ")
            .toLowerCase(Locale.ROOT);
    }

    private String shortCwd() {
        if ("/home/ubuntu".equals(cwd)) return "~";

        if (cwd.startsWith("/home/ubuntu")) {
            return "~" + cwd.substring("/home/ubuntu".length());
        }

        return cwd;
    }

    private void appendTerminal(String line) {
        if (!line.isEmpty()) {
            history.add("__OUT__" + line);
        }

        if (terminalView != null) {
            terminalView.setText(historyText());
        }
    }

    private void refreshTerminal() {
        if (terminalView != null) {
            terminalView.setText(historyText());
        }
    }

    private String historyText() {
        StringBuilder builder = new StringBuilder();

        for (String line : history) {
            if (line.startsWith("__OUT__")) {
                builder.append(line.substring(7));
            } else {
                builder.append(line);
            }

            builder.append("\n");
        }

        return builder.toString();
    }

    private void scrollTerminalToBottom() {
        if (contentScroll == null) return;

        contentScroll.post(() ->
            contentScroll.fullScroll(View.FOCUS_DOWN)
        );
    }

    private void protectFromSystemBars(View view) {
        final int baseLeft = view.getPaddingLeft();
        final int baseTop = view.getPaddingTop();
        final int baseRight = view.getPaddingRight();
        final int baseBottom = view.getPaddingBottom();

        view.setFitsSystemWindows(false);

        view.setOnApplyWindowInsetsListener((v, insets) -> {
            int left = insets.getSystemWindowInsetLeft();
            int top = insets.getSystemWindowInsetTop();
            int right = insets.getSystemWindowInsetRight();
            int bottom = insets.getSystemWindowInsetBottom();

            v.setPadding(
                baseLeft + left,
                baseTop + top + dp(8),
                baseRight + right,
                baseBottom + bottom + dp(8)
            );

            return insets;
        });

        view.requestApplyInsets();
    }

    private LinearLayout panel(int color, int radius) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(15), dp(14), dp(15), dp(14));
        layout.setBackground(rounded(color, radius, 0));
        return layout;
    }

    private TextView label(String text, int size, boolean bold, int color) {
        TextView view = new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);

        if (bold) {
            view.setTypeface(view.getTypeface(), Typeface.BOLD);
        }

        return view;
    }

    private Button accentButton(String text) {
        Button button = new Button(this);

        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setBackground(
            rounded(Color.rgb(226,83,45), 14, 0)
        );

        return button;
    }

    private Button smallButton(String text) {
        Button button = new Button(this);

        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(Color.rgb(45,45,49));
        button.setBackground(
            rounded(Color.rgb(232,232,236), 12, 0)
        );
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(12), dp(7), dp(12), dp(7));

        return button;
    }

    private Button smallFullButton(String text) {
        Button button = smallButton(text);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );

        params.topMargin = dp(7);
        button.setLayoutParams(params);

        return button;
    }

    private Button choiceButton(String text) {
        Button button = new Button(this);

        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(14f);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setTypeface(Typeface.MONOSPACE);
        button.setBackground(
            rounded(
                Color.rgb(50,50,56),
                12,
                Color.rgb(90,90,98)
            )
        );

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );

        params.bottomMargin = dp(7);
        button.setLayoutParams(params);

        return button;
    }

    private GradientDrawable rounded(int fill, int radiusDp, int stroke) {
        GradientDrawable drawable = new GradientDrawable();

        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));

        if (stroke != 0) {
            drawable.setStroke(dp(1), stroke);
        }

        return drawable;
    }

    private LinearLayout.LayoutParams marginTop(int top) {
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
