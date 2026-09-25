package com.ghiles.quizubuntu;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {

    private static final int PASS_PERCENT = 70;

    static class Question {
        final int level;
        final String category;
        final String text;
        final List<String> options;
        final int correctIndex;
        final String explanation;
        final String example;

        Question(int level, String category, String text, int correctIndex,
                 String explanation, String example, String... options) {
            this.level = level;
            this.category = category;
            this.text = text;
            this.options = Arrays.asList(options);
            this.correctIndex = correctIndex;
            this.explanation = explanation;
            this.example = example;
        }
    }

    static class LevelInfo {
        final int number;
        final String title;
        final String description;

        LevelInfo(int number, String title, String description) {
            this.number = number;
            this.title = title;
            this.description = description;
        }
    }

    private final List<LevelInfo> levels = Arrays.asList(
        new LevelInfo(1, "Fondamentaux Ubuntu", "Se déplacer, lister, créer des dossiers et fichiers."),
        new LevelInfo(2, "Fichiers et premiers pas Git", "Redirections, lecture, suppression et bases de Git."),
        new LevelInfo(3, "Workflow Git", "Staging, commits, remotes, diff et cycle de travail."),
        new LevelInfo(4, "Branches, GitHub et SSH", "Branches, push distant et authentification SSH."),
        new LevelInfo(5, "Synchronisation et conflits", "Rebase, merge, marqueurs et résolution de conflits.")
    );

    private final List<Question> questions = Arrays.asList(
        q(1,"Bash","Quelle commande affiche le chemin absolu du dossier courant ?",0,
            "pwd signifie Print Working Directory et affiche le chemin courant.",
            "$ pwd\n/home/ghiles/dossier-git",
            "pwd","ls","cd","mkdir"),
        q(1,"Bash","Quelle commande liste le contenu visible du dossier courant ?",1,
            "ls affiche les fichiers et dossiers visibles du répertoire courant.",
            "$ ls\nREADME.md  exercices  notes.txt",
            "pwd","ls","cat","history"),
        q(1,"Bash","Quelle commande affiche aussi les fichiers cachés avec des détails ?",0,
            "ls -la combine l'affichage détaillé (-l) et les fichiers cachés (-a).",
            "$ ls -la\ndrwxr-xr-x .git\n-rw-r--r-- README.md",
            "ls -la","ls -r","pwd -a","cat -la"),
        q(1,"Bash","Quelle commande te ramène dans ton dossier personnel ?",2,
            "~ représente le dossier personnel de l'utilisateur.",
            "$ cd ~\n$ pwd\n/home/ghiles",
            "cd /","cd .","cd ~","cd .."),
        q(1,"Bash","Quelle commande remonte d'un niveau dans l'arborescence ?",0,
            ".. représente le dossier parent.",
            "/home/ghiles/projet/src\n$ cd ..\n/home/ghiles/projet",
            "cd ..","cd .","cd ~","pwd"),
        q(1,"Bash","Comment créer les dossiers imbriqués a/b/c en une seule commande ?",1,
            "mkdir -p crée les répertoires parents nécessaires.",
            "$ mkdir -p a/b/c\n$ ls a/b\nc",
            "mkdir a/b/c","mkdir -p a/b/c","touch a/b/c","cp -r a/b/c"),
        q(1,"Bash","Quelle commande crée un fichier vide nommé test.txt ?",0,
            "touch permet de créer un fichier vide.",
            "$ touch test.txt\n$ ls\ntest.txt",
            "touch test.txt","mkdir test.txt","cat test.txt","nano -r test.txt"),

        q(2,"Bash","Que fait > dans : echo \"test\" > fichier.txt ?",1,
            "> redirige la sortie et crée ou remplace le contenu du fichier.",
            "$ echo \"test\" > fichier.txt\n$ cat fichier.txt\ntest",
            "Ajoute le texte à la fin","Crée ou remplace le contenu","Supprime le fichier","Affiche seulement le fichier"),
        q(2,"Bash","Que fait >> dans : echo \"suite\" >> fichier.txt ?",0,
            ">> ajoute la sortie à la fin du fichier existant.",
            "$ echo \"suite\" >> fichier.txt\n$ cat fichier.txt\ntest\nsuite",
            "Ajoute le texte à la fin","Remplace tout le fichier","Renomme le fichier","Crée un dossier"),
        q(2,"Bash","Quelle commande lit le contenu d'un fichier texte ?",0,
            "cat affiche le contenu d'un fichier dans le terminal.",
            "$ cat README.md\n# Mon projet",
            "cat fichier.txt","mv fichier.txt","mkdir fichier.txt","clear fichier.txt"),
        q(2,"Bash","Quelle commande supprime un dossier et tout son contenu ?",2,
            "rm -r réalise une suppression récursive.",
            "$ rm -r ancien-projet",
            "rm dossier","rmdir dossier","rm -r dossier","clear dossier"),
        q(2,"Git","Quelle commande initialise un dépôt Git dans le dossier courant ?",0,
            "git init crée les métadonnées Git du dépôt local.",
            "$ git init\nInitialized empty Git repository...",
            "git init","git clone","git add .","git status"),
        q(2,"Git","Quelle commande montre l'état du working directory et de la staging area ?",0,
            "git status indique notamment les fichiers modifiés, staged et non suivis.",
            "$ git status\nOn branch main\nChanges not staged for commit:",
            "git status","git log","git push","git config"),
        q(2,"Git","Que fait git add README.md ?",1,
            "git add prépare l'état du fichier pour le prochain commit.",
            "$ git add README.md\n$ git status\nChanges to be committed:",
            "Pousse le fichier sur GitHub","Place son état actuel dans la staging area","Crée automatiquement un commit","Supprime le fichier"),
        q(2,"Git","Quelle commande crée un commit avec un message directement dans la commande ?",0,
            "L'option -m fournit le message du commit.",
            "$ git commit -m \"Ajoute le README\"\n[main abc1234] Ajoute le README",
            "git commit -m \"message\"","git add -m \"message\"","git push -m \"message\"","git status -m \"message\""),

        q(3,"Git","Quel ordre correspond au cycle de travail recommandé après modification ?",1,
            "Le cycle de travail est : inspecter, préparer, enregistrer puis publier.",
            "modifier → git status → git add → git commit → git push",
            "git push → git init → git add","git status → git add → git commit → git push","git commit → git status → git clone","git remote → git init → git pull"),
        q(3,"Git","Quelle commande affiche les remotes et leurs URL de fetch/push ?",0,
            "git remote -v permet de vérifier vers quel dépôt un push sera envoyé.",
            "$ git remote -v\norigin  git@github.com:USER/REPO.git (fetch)\norigin  git@github.com:USER/REPO.git (push)",
            "git remote -v","git log -p","git status -s","git branch -a"),
        q(3,"Branches","Dans Git, qu'est-ce qu'une branche ?",1,
            "Une branche est un pointeur nommé vers un commit.",
            "main → C1 → C2\n          ↑\n        feature",
            "Un dossier contenant des commits","Un pointeur nommé vers un commit","Une copie complète du dépôt","Un fichier de configuration"),
        q(3,"Branches","Que représente généralement HEAD ?",1,
            "HEAD indique la position courante et pointe généralement vers la branche active.",
            "HEAD → main → C3",
            "Le dépôt distant GitHub","La branche ou position actuellement checkoutée","Le premier commit du dépôt","La staging area"),
        q(3,"Branches","Quelle commande crée la branche cheese puis bascule dessus ?",2,
            "git switch -c crée la branche au commit courant puis place HEAD dessus.",
            "$ git switch -c cheese\nSwitched to a new branch 'cheese'",
            "git switch cheese","git branch -d cheese","git switch -c cheese","git push cheese"),
        q(3,"Git","Quelle commande compare le working directory à la staging area ?",0,
            "git diff montre les changements non staged.",
            "$ git diff\n- ancienne ligne\n+ nouvelle ligne",
            "git diff","git diff --staged","git log --graph","git status -s"),
        q(3,"Git","Quelle commande montre ce qui est déjà préparé pour le prochain commit ?",0,
            "git diff --staged compare la staging area à HEAD.",
            "$ git add README.md\n$ git diff --staged",
            "git diff --staged","git branch -a","git clone","git fetch --all"),

        q(4,"Branches","git push origin cheese fait quoi ?",1,
            "Le push publie la branche. La fusion dans main est une opération distincte.",
            "$ git push origin cheese\n...\n[new branch] cheese -> cheese",
            "Fusionne cheese dans main","Publie la branche cheese sur origin","Supprime cheese localement","Crée automatiquement une Pull Request"),
        q(4,"Branches","Quelle commande supprime prudemment une branche locale déjà intégrée ?",0,
            "git branch -d supprime le pointeur de branche local.",
            "$ git switch main\n$ git branch -d cheese",
            "git branch -d cheese","git push origin --delete cheese","git rm cheese","git prune cheese"),
        q(4,"Branches","Quelle commande supprime réellement une branche cheese sur le remote origin ?",1,
            "git push origin --delete cheese demande au remote de supprimer cette branche.",
            "$ git push origin --delete cheese",
            "git branch -d cheese","git push origin --delete cheese","git fetch --prune cheese","git switch -d cheese"),
        q(4,"SSH","Quelle commande crée une paire de clés SSH Ed25519 ?",0,
            "ssh-keygen -t ed25519 crée une paire de clés privée et publique.",
            "$ ssh-keygen -t ed25519 -C \"email@example.com\"",
            "ssh-keygen -t ed25519 -C \"email\"","ssh-add -t ed25519","git keygen ed25519","ssh -T ed25519"),
        q(4,"SSH","Quel fichier peut être copié dans GitHub > SSH and GPG keys ?",1,
            "Le fichier .pub contient la clé publique. La clé privée ne doit pas être partagée.",
            "$ cat ~/.ssh/id_ed25519.pub\nssh-ed25519 AAAA...",
            "~/.ssh/id_ed25519","~/.ssh/id_ed25519.pub","~/.ssh/known_hosts uniquement","~/.gitconfig"),
        q(4,"SSH","Quelle commande teste l'authentification SSH auprès de GitHub ?",0,
            "ssh -T git@github.com permet de tester l'authentification SSH.",
            "$ ssh -T git@github.com\nHi USER! You've successfully authenticated...",
            "ssh -T git@github.com","git status github.com","ssh-add github.com","git remote -T"),

        q(5,"Synchronisation","Que fait git pull --rebase origin main ?",1,
            "Cette commande récupère les commits distants puis rejoue les commits locaux au-dessus.",
            "origin/main : A—B—C\nlocal :       B—D\nrebase : A—B—C—D'",
            "Supprime main puis la recrée","Récupère les commits distants puis rejoue les commits locaux par-dessus","Force le dépôt distant à accepter le local","Change l'URL du remote"),
        q(5,"Conflits","Un conflit Git signifie-t-il forcément que Git a crashé ?",1,
            "Git s'arrête volontairement lorsqu'il ne peut pas décider seul.",
            "BASE\n├─ LOCAL\n└─ REMOTE\nGit demande une décision humaine.",
            "Oui, Git doit être réinstallé","Non, Git s'arrête car une décision humaine est nécessaire","Oui, le dépôt est détruit","Non, mais Git choisit toujours la version distante"),
        q(5,"Conflits","Dans les marqueurs de conflit, que signifie <<<<<<< HEAD ?",0,
            "HEAD introduit la version 'ours', celle de la branche actuellement checkoutée.",
            "<<<<<<< HEAD\nLOCAL\n=======\nREMOTE\n>>>>>>> abc123",
            "Début de la version de la branche actuellement checkoutée","Début de la version distante uniquement","Fin du conflit","Un commentaire sans effet"),
        q(5,"Conflits","Pendant un conflit, que fait git add README.md après résolution ?",1,
            "git add enregistre la décision de résolution dans l'index ; il ne juge pas sa qualité.",
            "éditer → supprimer les marqueurs → git add README.md → git commit",
            "Vérifie que le contenu choisi est sémantiquement correct","Marque le fichier comme résolu dans l'index","Annule automatiquement le merge","Pousse directement la résolution sur GitHub"),
        q(5,"Conflits","Quelle commande permet d'abandonner un merge en cours et de revenir à l'état précédent ?",2,
            "git merge --abort annule le merge en cours quand Git peut restaurer l'état précédent.",
            "$ git status\nYou have unmerged paths.\n$ git merge --abort",
            "git merge --stop","git reset --merge-now","git merge --abort","git pull --cancel"),
        q(5,"Conflits","Dans un fichier en conflit, quel marqueur sépare les deux versions proposées ?",1,
            "La ligne ======= sépare la version HEAD de la version entrante.",
            "<<<<<<< HEAD\nnotre version\n=======\nleur version\n>>>>>>> commit",
            "<<<<<<< HEAD","=======",">>>>>>>","-------"),
        q(5,"Git","Que représente origin/main dans un dépôt local ?",2,
            "origin/main est une référence locale qui mémorise le dernier état connu de main sur le remote origin.",
            "GitHub main → C5\nfetch/pull\norigin/main → C5",
            "La branche main réellement exécutée sur GitHub","Une copie complète du dépôt distant","Une référence locale du dernier état distant connu","Le fichier de configuration du remote"),
        q(5,"Conflits","Pourquoi parle-t-on de merge à trois voies ?",3,
            "Git compare l'ancêtre commun BASE, notre version OURS et la version entrante THEIRS.",
            "BASE\n /  \\\nOURS  THEIRS",
            "Parce que Git crée toujours trois commits","Parce qu'il faut trois branches distantes","Parce que trois utilisateurs doivent valider","Parce que Git examine BASE, OURS et THEIRS")
    );

    private static Question q(int level, String category, String text, int correctIndex,
                              String explanation, String example, String... options) {
        return new Question(level, category, text, correctIndex, explanation, example, options);
    }

    private SharedPreferences prefs;
    private List<Question> quiz = new ArrayList<>();
    private int activeLevel = 1;
    private int currentIndex = 0;
    private int correctCount = 0;
    private int sessionPoints = 0;
    private int streak = 0;
    private boolean answered = false;

    private TextView categoryView;
    private TextView progressView;
    private TextView questionView;
    private TextView feedbackView;
    private TextView pointsView;
    private Button nextButton;
    private final List<Button> optionButtons = new ArrayList<>();
    private IllustrationView illustrationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("quiz_progress", MODE_PRIVATE);
        showHome();
    }

    private void showHome() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = column(20, 24, 20, 36);
        scroll.addView(root);

        addTitle(root, "Ubuntu & Git Academy", 30);
        addBody(root, "Apprends les commandes par niveaux, gagne des points et suis ta progression.", 17);

        int totalPoints = prefs.getInt("totalPoints", 0);
        int answeredTotal = prefs.getInt("answeredTotal", 0);
        int correctTotal = prefs.getInt("correctTotal", 0);
        int accuracy = answeredTotal == 0 ? 0 : (correctTotal * 100 / answeredTotal);
        int bestStreak = prefs.getInt("bestStreak", 0);

        TextView summary = card(
            "TABLEAU DE BORD\n\n" +
            "Points : " + totalPoints +
            "\nPrécision : " + accuracy + "%" +
            "\nMeilleure série : " + bestStreak
        );
        root.addView(summary);

        Button stats = actionButton("Voir mes statistiques");
        stats.setOnClickListener(v -> showStats());
        root.addView(stats);

        Button badges = actionButton("Voir mes badges");
        badges.setOnClickListener(v -> showBadges());
        root.addView(badges);

        Button memo = actionButton("Mémo des commandes");
        memo.setOnClickListener(v -> showMemo());
        root.addView(memo);

        addSection(root, "Niveaux");

        for (LevelInfo info : levels) {
            boolean unlocked = isLevelUnlocked(info.number);
            int best = prefs.getInt("best_level_" + info.number, 0);

            LinearLayout levelCard = column(16, 16, 16, 16);
            levelCard.setBackgroundColor(Color.rgb(245, 246, 248));

            TextView name = text(
                "Niveau " + info.number + " — " + info.title +
                (unlocked ? "" : "  [VERROUILLÉ]"),
                19, true
            );
            levelCard.addView(name);

            addBody(levelCard, info.description, 15);
            addBody(levelCard, "Meilleur score : " + best + "%", 14);

            Button start = actionButton(unlocked ? "Commencer ce niveau" : "Atteins 70% au niveau précédent");
            start.setEnabled(unlocked);
            start.setOnClickListener(v -> startLevel(info.number));
            levelCard.addView(start);

            root.addView(levelCard, spaced(12));
        }

        setContentView(scroll);
    }

    private boolean isLevelUnlocked(int level) {
        if (level <= 1) return true;
        return prefs.getInt("best_level_" + (level - 1), 0) >= PASS_PERCENT;
    }

    private void startLevel(int level) {
        activeLevel = level;
        quiz = new ArrayList<>();
        for (Question q : questions) {
            if (q.level == level) quiz.add(q);
        }
        Collections.shuffle(quiz);

        currentIndex = 0;
        correctCount = 0;
        sessionPoints = 0;
        streak = 0;
        showQuizScreen();
    }

    private void showQuizScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = column(20, 18, 20, 32);
        scroll.addView(root);

        Button back = smallButton("← Accueil");
        back.setOnClickListener(v -> showHome());
        root.addView(back);

        addTitle(root, "Niveau " + activeLevel, 25);

        pointsView = text("", 15, true);
        root.addView(pointsView);

        categoryView = text("", 15, true);
        categoryView.setPadding(0, dp(10), 0, dp(4));
        root.addView(categoryView);

        progressView = text("", 14, false);
        progressView.setPadding(0, 0, 0, dp(10));
        root.addView(progressView);

        illustrationView = new IllustrationView(this);
        root.addView(illustrationView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(140)
        ));

        questionView = text("", 22, true);
        questionView.setPadding(0, dp(16), 0, dp(14));
        root.addView(questionView);

        optionButtons.clear();
        for (int i = 0; i < 4; i++) {
            final int answerIndex = i;
            Button button = actionButton("");
            button.setOnClickListener(v -> answer(answerIndex));
            root.addView(button);
            optionButtons.add(button);
        }

        feedbackView = card("");
        feedbackView.setVisibility(View.GONE);
        root.addView(feedbackView, spaced(10));

        nextButton = actionButton("Question suivante");
        nextButton.setVisibility(View.GONE);
        nextButton.setOnClickListener(v -> nextQuestion());
        root.addView(nextButton);

        setContentView(scroll);
        renderQuestion();
    }

    private void renderQuestion() {
        Question q = quiz.get(currentIndex);
        answered = false;

        categoryView.setText("Catégorie : " + q.category);
        progressView.setText("Question " + (currentIndex + 1) + " / " + quiz.size() +
                "   •   Bonnes réponses : " + correctCount);
        pointsView.setText("Points gagnés : " + sessionPoints + "   •   Série : " + streak);
        questionView.setText(q.text);
        illustrationView.setCategory(q.category);

        feedbackView.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);

        for (int i = 0; i < optionButtons.size(); i++) {
            Button button = optionButtons.get(i);
            button.setText(q.options.get(i));
            button.setEnabled(true);
        }
    }

    private void answer(int index) {
        if (answered) return;
        answered = true;

        Question q = quiz.get(currentIndex);
        boolean isCorrect = index == q.correctIndex;

        int answeredTotal = prefs.getInt("answeredTotal", 0) + 1;
        int correctTotal = prefs.getInt("correctTotal", 0);

        if (isCorrect) {
            correctCount++;
            streak++;
            correctTotal++;

            int bonus = Math.min(100, Math.max(0, streak - 1) * 25);
            int earned = 100 + bonus;
            sessionPoints += earned;

            int totalPoints = prefs.getInt("totalPoints", 0) + earned;
            prefs.edit().putInt("totalPoints", totalPoints).apply();

            int bestStreak = Math.max(prefs.getInt("bestStreak", 0), streak);
            prefs.edit().putInt("bestStreak", bestStreak).apply();

            feedbackView.setText(
                "✓ Bonne réponse\n\n" +
                q.explanation +
                "\n\nEXEMPLE\n" + q.example +
                "\n\n+" + earned + " points" +
                (bonus > 0 ? " (bonus de série +" + bonus + ")" : "")
            );
        } else {
            streak = 0;
            feedbackView.setText(
                "✗ Mauvaise réponse\n\n" +
                "Bonne réponse : " + q.options.get(q.correctIndex) +
                "\n\n" + q.explanation +
                "\n\nEXEMPLE\n" + q.example
            );
        }

        prefs.edit()
            .putInt("answeredTotal", answeredTotal)
            .putInt("correctTotal", correctTotal)
            .apply();

        for (Button button : optionButtons) button.setEnabled(false);

        feedbackView.setVisibility(View.VISIBLE);
        nextButton.setText(currentIndex == quiz.size() - 1 ? "Voir le résultat" : "Question suivante");
        nextButton.setVisibility(View.VISIBLE);
        pointsView.setText("Points gagnés : " + sessionPoints + "   •   Série : " + streak);
    }

    private void nextQuestion() {
        if (currentIndex < quiz.size() - 1) {
            currentIndex++;
            renderQuestion();
        } else {
            finishLevel();
        }
    }

    private void finishLevel() {
        int percent = quiz.isEmpty() ? 0 : correctCount * 100 / quiz.size();
        int previousBest = prefs.getInt("best_level_" + activeLevel, 0);
        boolean newBest = percent > previousBest;

        if (newBest) {
            prefs.edit().putInt("best_level_" + activeLevel, percent).apply();
        }
        if (percent == 100) {
            prefs.edit().putBoolean("perfectLevel", true).apply();
        }

        LinearLayout root = column(24, 48, 24, 40);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        addTitle(root, "Niveau terminé", 31);

        TextView big = text(percent + "%", 42, true);
        big.setGravity(Gravity.CENTER);
        root.addView(big);

        addBody(root,
            correctCount + " / " + quiz.size() + " bonnes réponses\n" +
            sessionPoints + " points gagnés" +
            (newBest ? "\nNouveau meilleur score !" : ""),
            18
        );

        String message;
        if (percent >= 90) {
            message = "Excellent. Les commandes de ce niveau sont bien maîtrisées.";
        } else if (percent >= PASS_PERCENT) {
            message = "Niveau validé. Le niveau suivant est maintenant accessible.";
        } else if (percent >= 50) {
            message = "Presque. Rejoue pour atteindre 70% et débloquer la suite.";
        } else {
            message = "Reprends les exemples et le mémo puis retente ce niveau.";
        }
        TextView resultCard = card(message);
        root.addView(resultCard, spaced(16));

        Button replay = actionButton("Rejouer ce niveau");
        replay.setOnClickListener(v -> startLevel(activeLevel));
        root.addView(replay);

        Button stats = actionButton("Voir mes statistiques");
        stats.setOnClickListener(v -> showStats());
        root.addView(stats);

        Button home = actionButton("Retour à l'accueil");
        home.setOnClickListener(v -> showHome());
        root.addView(home);

        setContentView(root);
    }

    private void showStats() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = column(20, 20, 20, 36);
        scroll.addView(root);

        Button back = smallButton("← Accueil");
        back.setOnClickListener(v -> showHome());
        root.addView(back);

        addTitle(root, "Statistiques", 28);

        int answeredTotal = prefs.getInt("answeredTotal", 0);
        int correctTotal = prefs.getInt("correctTotal", 0);
        int accuracy = answeredTotal == 0 ? 0 : correctTotal * 100 / answeredTotal;

        TextView summary = card(
            "Points totaux : " + prefs.getInt("totalPoints", 0) +
            "\nQuestions répondues : " + answeredTotal +
            "\nBonnes réponses : " + correctTotal +
            "\nPrécision globale : " + accuracy + "%" +
            "\nMeilleure série : " + prefs.getInt("bestStreak", 0)
        );
        root.addView(summary);

        addSection(root, "Précision globale");
        AccuracyDonutView donut = new AccuracyDonutView(this);
        donut.setAccuracy(accuracy);
        root.addView(donut, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(220)
        ));

        addSection(root, "Meilleurs scores par niveau");
        ProgressChartView chart = new ProgressChartView(this);
        int[] best = new int[5];
        for (int i = 0; i < 5; i++) best[i] = prefs.getInt("best_level_" + (i + 1), 0);
        chart.setValues(best);
        root.addView(chart, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(260)
        ));

        addBody(root, "Objectif : atteindre au moins 70% pour débloquer le niveau suivant.", 15);

        setContentView(scroll);
    }

    private void showBadges() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = column(20, 20, 20, 36);
        scroll.addView(root);

        Button back = smallButton("← Accueil");
        back.setOnClickListener(v -> showHome());
        root.addView(back);

        addTitle(root, "Badges", 28);
        addBody(root, "Les badges se débloquent automatiquement avec ta progression.", 16);

        int answered = prefs.getInt("answeredTotal", 0);
        int totalPoints = prefs.getInt("totalPoints", 0);
        int bestStreak = prefs.getInt("bestStreak", 0);

        addBadge(root, "Premier pas", "Répondre à une première question.", answered >= 1);
        addBadge(root, "Série x3", "Réussir au moins 3 bonnes réponses d'affilée.", bestStreak >= 3);
        addBadge(root, "Explorateur Bash", "Atteindre 70% au niveau 1.", prefs.getInt("best_level_1", 0) >= 70);
        addBadge(root, "Git prêt", "Atteindre 70% au niveau 3.", prefs.getInt("best_level_3", 0) >= 70);
        addBadge(root, "Maître des branches", "Atteindre 70% au niveau 4.", prefs.getInt("best_level_4", 0) >= 70);
        addBadge(root, "Résolveur de conflits", "Atteindre 70% au niveau 5.", prefs.getInt("best_level_5", 0) >= 70);
        addBadge(root, "Sans faute", "Obtenir 100% sur un niveau.", prefs.getBoolean("perfectLevel", false));
        addBadge(root, "1000 points", "Accumuler au moins 1000 points.", totalPoints >= 1000);

        setContentView(scroll);
    }

    private void addBadge(LinearLayout root, String title, String description, boolean earned) {
        TextView badge = card(
            (earned ? "DÉBLOQUÉ — " : "À DÉBLOQUER — ") + title +
            "\n" + description
        );
        badge.setAlpha(earned ? 1f : 0.55f);
        root.addView(badge, spaced(8));
    }

    private void showMemo() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = column(20, 20, 20, 36);
        scroll.addView(root);

        Button back = smallButton("← Accueil");
        back.setOnClickListener(v -> showHome());
        root.addView(back);

        addTitle(root, "Mémo des commandes", 28);
        addBody(root, "Une fiche rapide pour réviser avant de relancer un niveau.", 16);

        addMemoGroup(root, "Navigation Bash",
            "pwd — afficher le chemin courant",
            "ls — lister le contenu",
            "ls -la — détails + fichiers cachés",
            "cd dossier — entrer dans un dossier",
            "cd .. — remonter d'un niveau",
            "cd ~ — revenir au dossier personnel");

        addMemoGroup(root, "Fichiers et dossiers",
            "mkdir -p a/b/c — créer une arborescence",
            "touch fichier.txt — créer un fichier vide",
            "cat fichier.txt — lire un fichier",
            "echo \"texte\" > fichier.txt — créer/remplacer",
            "echo \"suite\" >> fichier.txt — ajouter",
            "rm -r dossier — supprimer récursivement");

        addMemoGroup(root, "Git quotidien",
            "git status — voir l'état du dépôt",
            "git add README.md — préparer un fichier",
            "git diff — voir les changements non staged",
            "git diff --staged — voir ce qui sera commité",
            "git commit -m \"message\" — créer un commit",
            "git push — publier les commits");

        addMemoGroup(root, "Branches et distant",
            "git switch -c feature — créer + basculer",
            "git branch -d feature — supprimer localement",
            "git push origin feature — publier une branche",
            "git push origin --delete feature — supprimer à distance",
            "git remote -v — voir les remotes",
            "git pull --rebase origin main — intégrer le distant par rebase");

        addMemoGroup(root, "SSH et conflits",
            "ssh-keygen -t ed25519 -C \"email\" — créer une paire de clés",
            "cat ~/.ssh/id_ed25519.pub — afficher la clé publique",
            "ssh -T git@github.com — tester GitHub en SSH",
            "git merge --abort — abandonner un merge",
            "<<<<<<< HEAD / ======= / >>>>>>> — marqueurs de conflit");

        setContentView(scroll);
    }

    private void addMemoGroup(LinearLayout root, String title, String... lines) {
        addSection(root, title);
        StringBuilder b = new StringBuilder();
        for (String line : lines) b.append("• ").append(line).append("\n");
        TextView box = card(b.toString().trim());
        root.addView(box, spaced(8));
    }

    private LinearLayout column(int left, int top, int right, int bottom) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(left), dp(top), dp(right), dp(bottom));
        return layout;
    }

    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.rgb(28, 31, 36));
        if (bold) t.setTypeface(t.getTypeface(), Typeface.BOLD);
        return t;
    }

    private void addTitle(LinearLayout root, String value, int size) {
        TextView t = text(value, size, true);
        t.setPadding(0, dp(4), 0, dp(12));
        root.addView(t);
    }

    private void addSection(LinearLayout root, String value) {
        TextView t = text(value, 20, true);
        t.setPadding(0, dp(22), 0, dp(10));
        root.addView(t);
    }

    private void addBody(LinearLayout root, String value, int size) {
        TextView t = text(value, size, false);
        t.setLineSpacing(0, 1.15f);
        t.setPadding(0, 0, 0, dp(12));
        root.addView(t);
    }

    private TextView card(String value) {
        TextView t = text(value, 16, false);
        t.setLineSpacing(0, 1.18f);
        t.setPadding(dp(16), dp(16), dp(16), dp(16));
        t.setBackgroundColor(Color.rgb(245, 246, 248));
        return t;
    }

    private Button actionButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(15f);
        b.setAllCaps(false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.bottomMargin = dp(8);
        b.setLayoutParams(p);
        return b;
    }

    private Button smallButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14f);
        return b;
    }

    private LinearLayout.LayoutParams spaced(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.topMargin = dp(top);
        return p;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    static class IllustrationView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private String category = "Bash";

        IllustrationView(Activity context) {
            super(context);
            paint.setStrokeWidth(6f);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        void setCategory(String category) {
            this.category = category;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.rgb(248, 249, 251));

            float w = getWidth();
            float h = getHeight();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5f);
            paint.setColor(Color.rgb(69, 76, 86));

            if ("SSH".equals(category)) {
                float cx = w * 0.37f;
                float cy = h * 0.5f;
                canvas.drawCircle(cx, cy, h * 0.18f, paint);
                canvas.drawLine(cx + h * 0.18f, cy, w * 0.78f, cy, paint);
                canvas.drawLine(w * 0.66f, cy, w * 0.66f, cy + h * 0.13f, paint);
                canvas.drawLine(w * 0.75f, cy, w * 0.75f, cy + h * 0.10f, paint);
                drawLabel(canvas, "clé publique / clé privée", w * 0.5f, h * 0.87f);
            } else if ("Conflits".equals(category) || "Synchronisation".equals(category)) {
                paint.setColor(Color.rgb(70, 95, 140));
                canvas.drawLine(w * 0.18f, h * 0.5f, w * 0.42f, h * 0.27f, paint);
                canvas.drawLine(w * 0.18f, h * 0.5f, w * 0.42f, h * 0.73f, paint);
                canvas.drawLine(w * 0.42f, h * 0.27f, w * 0.78f, h * 0.5f, paint);
                canvas.drawLine(w * 0.42f, h * 0.73f, w * 0.78f, h * 0.5f, paint);
                node(canvas, w * 0.18f, h * 0.5f);
                node(canvas, w * 0.42f, h * 0.27f);
                node(canvas, w * 0.42f, h * 0.73f);
                node(canvas, w * 0.78f, h * 0.5f);
                drawLabel(canvas, "BASE", w * 0.18f, h * 0.18f);
                drawLabel(canvas, "OURS", w * 0.42f, h * 0.13f);
                drawLabel(canvas, "THEIRS", w * 0.42f, h * 0.93f);
                drawLabel(canvas, "MERGE", w * 0.78f, h * 0.18f);
            } else if ("Branches".equals(category) || "Git".equals(category)) {
                paint.setColor(Color.rgb(75, 105, 155));
                canvas.drawLine(w * 0.16f, h * 0.55f, w * 0.82f, h * 0.55f, paint);
                canvas.drawLine(w * 0.42f, h * 0.55f, w * 0.58f, h * 0.25f, paint);
                node(canvas, w * 0.20f, h * 0.55f);
                node(canvas, w * 0.42f, h * 0.55f);
                node(canvas, w * 0.64f, h * 0.55f);
                node(canvas, w * 0.82f, h * 0.55f);
                node(canvas, w * 0.58f, h * 0.25f);
                drawLabel(canvas, "main", w * 0.76f, h * 0.82f);
                drawLabel(canvas, "feature", w * 0.58f, h * 0.12f);
            } else {
                paint.setColor(Color.rgb(64, 70, 78));
                paint.setStyle(Paint.Style.STROKE);
                RectF r = new RectF(w * 0.10f, h * 0.18f, w * 0.90f, h * 0.82f);
                canvas.drawRoundRect(r, 18f, 18f, paint);
                canvas.drawLine(w * 0.10f, h * 0.34f, w * 0.90f, h * 0.34f, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(h * 0.14f);
                paint.setTypeface(Typeface.MONOSPACE);
                canvas.drawText("$", w * 0.17f, h * 0.57f, paint);
                canvas.drawText("commande", w * 0.25f, h * 0.57f, paint);
                canvas.drawText("résultat", w * 0.25f, h * 0.73f, paint);
            }
        }

        private void node(Canvas canvas, float x, float y) {
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, y, 10f, paint);
            paint.setStyle(Paint.Style.STROKE);
        }

        private void drawLabel(Canvas canvas, String s, float x, float y) {
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(28f);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(s, x, y, paint);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setStyle(Paint.Style.STROKE);
        }
    }

    static class ProgressChartView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int[] values = new int[]{0,0,0,0,0};

        ProgressChartView(Activity context) {
            super(context);
        }

        void setValues(int[] values) {
            this.values = values;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.rgb(248, 249, 251));
            float w = getWidth();
            float h = getHeight();
            float left = w * 0.10f;
            float bottom = h * 0.82f;
            float top = h * 0.12f;
            float chartHeight = bottom - top;
            float slot = (w * 0.82f) / 5f;

            paint.setStrokeWidth(3f);
            paint.setColor(Color.rgb(170, 175, 184));
            canvas.drawLine(left, bottom, w * 0.94f, bottom, paint);

            for (int i = 0; i < 5; i++) {
                float barW = slot * 0.52f;
                float x = left + slot * i + slot * 0.24f;
                float bh = chartHeight * values[i] / 100f;

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(values[i] >= PASS_PERCENT
                    ? Color.rgb(65, 145, 95)
                    : Color.rgb(80, 112, 170));
                canvas.drawRoundRect(new RectF(x, bottom - bh, x + barW, bottom), 12f, 12f, paint);

                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(26f);
                paint.setColor(Color.rgb(35, 40, 48));
                canvas.drawText("N" + (i + 1), x + barW / 2, h * 0.94f, paint);
                canvas.drawText(values[i] + "%", x + barW / 2, Math.max(top + 24f, bottom - bh - 10f), paint);
            }

            paint.setTextAlign(Paint.Align.LEFT);
            paint.setStyle(Paint.Style.STROKE);
        }
    }

    static class AccuracyDonutView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int accuracy = 0;

        AccuracyDonutView(Activity context) {
            super(context);
        }

        void setAccuracy(int accuracy) {
            this.accuracy = Math.max(0, Math.min(100, accuracy));
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.rgb(248, 249, 251));
            float size = Math.min(getWidth(), getHeight()) * 0.62f;
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            RectF oval = new RectF(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * 0.16f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(Color.rgb(220, 223, 228));
            canvas.drawArc(oval, -90f, 360f, false, paint);

            paint.setColor(Color.rgb(65, 145, 95));
            canvas.drawArc(oval, -90f, 360f * accuracy / 100f, false, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setTextSize(size * 0.22f);
            paint.setColor(Color.rgb(30, 35, 42));
            canvas.drawText(accuracy + "%", cx, cy + size * 0.07f, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }
}
