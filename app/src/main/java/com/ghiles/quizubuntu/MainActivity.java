package com.ghiles.quizubuntu;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
        String format = "Choix raisonné";
        String[] accepted = null;
        Question written(String format, String... accepted) { this.format=format; this.accepted=accepted; return this; }

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

    private final List<Question> questions = buildQuestions();

    private static List<Question> buildQuestions() {
        List<Question> bank = new ArrayList<>(Arrays.asList(
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
        ));
        bank.addAll(PdfQuizBank.questions());
        bank.addAll(PdfPracticeBank.questions());
        bank.addAll(S03QuizBank.questions());
        return bank;
    }

    private static Question q(int level, String category, String text, int correctIndex,
                              String explanation, String example, String... options) {
        return new Question(level, category, text, correctIndex, explanation, example, options);
    }

    private SharedPreferences prefs;
    private List<Question> quiz = new ArrayList<>();
    private final List<Integer> answerOrder = new ArrayList<>();
    private static class ExamCorrection {
        final Question question;
        final String response;
        final boolean correct;
        ExamCorrection(Question question, String response, boolean correct) {
            this.question=question; this.response=response; this.correct=correct;
        }
    }
    private final List<ExamCorrection> examCorrections = new ArrayList<>();
    private ProgressBarView questionProgress;
    private int activeLevel = 1;
    private int currentIndex = 0;
    private int correctCount = 0;
    private int sessionPoints = 0;
    private int streak = 0;
    private boolean answered = false;
    private boolean reviewMode = false;
    private boolean customMode = false;
    private boolean examMode = false;
    private String customModeTitle = "";

    private TextView categoryView;
    private TextView progressView;
    private TextView questionView;
    private TextView primerContextView;
    private TextView primerCommandView;
    private TextView primerOutputView;
    private TextView primerFocusView;
    private TextView feedbackView;
    private TextView detailView;
    private Button moreDetailsButton;
    private ScrollView quizScroll;
    private EditText writtenAnswer;
    private Button submitWritten;
    private LinearLayout sourceIllustration;
    private TextView pointsView;
    private Button nextButton;
    private final List<Button> optionButtons = new ArrayList<>();
    private IllustrationView illustrationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("quiz_progress", MODE_PRIVATE);
        feedback = new AcademyFeedback(this);
        showHome();
    }

    private AcademyFeedback feedback;
    private String activeChapter;

    private void showHome() {
        activeChapter=null;
        applySystemBars();
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(bgColor());
        LinearLayout root=column(20,20,20,28);scroll.addView(root);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=text("Ubuntu & Git\nACADEMY",20,true);brand.setLineSpacing(dp(2),1f);
        top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        Button settings=secondaryButton("Réglages");settings.setOnClickListener(v->showDesignSettings());top.addView(settings);root.addView(top);
        TextView eyebrow=text("APPRENDRE EN PRATIQUANT",11,true);eyebrow.setLetterSpacing(.13f);eyebrow.setTextColor(secondaryTextColor());eyebrow.setPadding(0,dp(24),0,dp(10));root.addView(eyebrow);
        LinearLayout hero=column(22,24,22,22);
        GradientDrawable gradient=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(48,41,112),Color.rgb(80,71,167),Color.rgb(28,104,118)});gradient.setCornerRadius(dp(28));hero.setBackground(gradient);
        TextView title=text("Une commande.\nUn nouveau réflexe.",29,true);title.setTextColor(Color.WHITE);title.setLineSpacing(dp(2),1f);hero.addView(title);
        TextView subtitle=text("Des chapitres courts. Des exercices concrets.\nÀ ton rythme, depuis ton téléphone.",14,false);subtitle.setTextColor(Color.rgb(229,229,250));subtitle.setLineSpacing(dp(3),1f);subtitle.setPadding(0,dp(12),0,dp(18));hero.addView(subtitle);
        int total=prefs.getInt("totalPoints",0),done=dailyAnswered();
        TextView progress=text(total+" XP     ·     "+Math.min(done,10)+" / 10 aujourd’hui",13,true);progress.setTextColor(Color.rgb(174,242,225));hero.addView(progress);
        AcademyChapters.Chapter resume=AcademyChapters.find(prefs.getString("lastChapter","Bash"));
        Button begin=actionButton("Continuer : "+resume.category+"  →");begin.setTextColor(ACADEMY_INK);AcademyDesign.style(begin,Color.WHITE,18,0);begin.setOnClickListener(v->startCategoryQuiz(resume.category));hero.addView(begin,spaced(18));root.addView(hero);
        addSection(root,"Ton espace de pratique");
        LinearLayout terminalCard=column(18,18,18,16);terminalCard.setBackground(roundedDrawable(isDark()?Color.rgb(28,42,49):Color.rgb(226,243,239),24,0));
        TextView terminalTitle=text(">_  Le terminal, pour de vrai",19,true);terminalCard.addView(terminalTitle);
        addBody(terminalCard,"Fichiers, GitHub et sessions sauvegardées. Ou entraîne-toi dans les missions guidées.",14);
        Button terminal=actionButton("Ouvrir le terminal");AcademyDesign.style(terminal,Color.rgb(8,105,109),18,0);terminal.setTextColor(Color.WHITE);terminal.setOnClickListener(v->startActivity(new Intent(this,SimulatorActivity.class)));terminalCard.addView(terminal);root.addView(terminalCard);
        addSection(root,"Un chapitre. Son quiz.");
        addBody(root,"11 sujets, des bases du terminal aux ACL. Chaque quiz reste dans le chapitre choisi.",14);
        Button chapters=actionButton("Explorer les 11 chapitres  →");chapters.setOnClickListener(v->showCategoryPicker());root.addView(chapters);
        int wrong=prefs.getStringSet("wrongQuestions",Collections.emptySet()).size();
        Button review=secondaryFullButton(wrong>0?"Revoir mes erreurs  ·  "+wrong:"Mes révisions sont à jour  ✓");review.setEnabled(wrong>0);review.setOnClickListener(v->startReviewErrors());root.addView(review);
        Button more=secondaryFullButton("Tous les entraînements et outils");more.setOnClickListener(v->showTrainingTools());root.addView(more);
        TextView note=text("Régularité > vitesse. Une petite session suffit.",12,false);note.setTextColor(secondaryTextColor());note.setPadding(0,dp(12),0,dp(8));root.addView(note);
        installAcademyPage(scroll,1);
    }

    private static final int ACADEMY_INK=Color.rgb(29,35,52);
    private void showDesignSettings(){
        new android.app.AlertDialog.Builder(this).setTitle("À ton goût").setMultiChoiceItems(new String[]{"Thème sombre","Sons discrets"},new boolean[]{isDark(),prefs.getBoolean("soundEnabled",true)},(dialog,which,checked)->{
            prefs.edit().putBoolean(which==0?"darkMode":"soundEnabled",checked).apply();
            if(which==1&&!checked&&feedback!=null)feedback.release();
        }).setPositiveButton("Appliquer",(d,w)->showHome()).show();
    }
    private void showTrainingTools(){
        new android.app.AlertDialog.Builder(this).setTitle("Entraînements & outils").setItems(new String[]{"Quiz rapide · 10 questions","Examen · 20 questions","Entraînement adaptatif","Parcours par niveaux","Fiches de cours","Commandes et favoris","Mes badges","Exporter ma progression","Mémo rapide"},(d,i)->{
            if(i==0)startQuickQuiz();else if(i==1)startExam();else if(i==2)startAdaptiveQuiz();else if(i==3)showLevelPicker();else if(i==4)showCourseMenu();else if(i==5)showCommandLibrary("");else if(i==6)showBadges();else if(i==7)shareProgress();else showMemo();
        }).show();
    }
    private void showLevelPicker(){
        LinearLayout root=column(20,20,20,24);ScrollView scroll=new ScrollView(this);scroll.addView(root);addTitle(root,"Parcours par niveaux",26);
        addBody(root,"Le parcours historique reste disponible. 70 % débloquent le niveau suivant.",14);
        for(LevelInfo level:levels){Button button=secondaryFullButton(level.number+" · "+level.title+"\nMeilleur score : "+prefs.getInt("best_level_"+level.number,0)+" %");button.setEnabled(isLevelUnlocked(level.number));button.setOnClickListener(v->startLevel(level.number));root.addView(button);}
        installAcademyPage(scroll,2);
    }
    private void installAcademyPage(ScrollView scroll,int selected){
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setBackgroundColor(bgColor());
        page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        com.google.android.material.bottomnavigation.BottomNavigationView nav=new com.google.android.material.bottomnavigation.BottomNavigationView(new android.view.ContextThemeWrapper(this,isDark()?R.style.Theme_Academy_Dark:R.style.Theme_Academy));
        nav.setBackgroundColor(surfaceColor());nav.setLabelVisibilityMode(com.google.android.material.navigation.NavigationBarView.LABEL_VISIBILITY_LABELED);
        nav.getMenu().add(0,1,0,"Accueil").setIcon(android.R.drawable.ic_menu_view);
        nav.getMenu().add(0,2,1,"Chapitres").setIcon(android.R.drawable.ic_menu_agenda);
        nav.getMenu().add(0,3,2,"Terminal").setIcon(android.R.drawable.ic_menu_edit);
        nav.getMenu().add(0,4,3,"Progrès").setIcon(android.R.drawable.ic_menu_info_details);
        nav.setSelectedItemId(selected);nav.setOnItemSelectedListener(item->{if(item.getItemId()==1)showHome();else if(item.getItemId()==2)showCategoryPicker();else if(item.getItemId()==3){startActivity(new Intent(this,SimulatorActivity.class));return false;}else showStats();return true;});
        page.addView(nav,new LinearLayout.LayoutParams(-1,-2));protectFromSystemBars(page);setContentView(page);
    }

    private boolean isLevelUnlocked(int level) {
        if (level <= 1) return true;
        return prefs.getInt("best_level_" + (level - 1), 0) >= PASS_PERCENT;
    }

    private void startLevel(int level) {
        activeChapter=null;
        reviewMode = false;
        customMode = false;
        examMode = false;
        customModeTitle = "";
        activeLevel = level;
        quiz = new ArrayList<>();
        for (Question q : questions) {
            if (q.level == level) quiz.add(q);
        }
        mixExercises(quiz);

        currentIndex = 0;
        correctCount = 0;
        sessionPoints = 0;
        streak = 0;
        showQuizScreen();
    }

    private void startReviewErrors() {
        activeChapter=null;
        Set<String> wrong = new HashSet<>(prefs.getStringSet("wrongQuestions", Collections.emptySet()));
        quiz = new ArrayList<>();
        for (Question q : questions) {
            if (wrong.contains(q.text)) quiz.add(q);
        }
        if (quiz.isEmpty()) {
            showHome();
            return;
        }
        mixExercises(quiz);
        reviewMode = true;
        customMode = false;
        examMode = false;
        customModeTitle = "RÉVISION DES ERREURS";
        activeLevel = 0;
        currentIndex = 0;
        correctCount = 0;
        sessionPoints = 0;
        streak = 0;
        showQuizScreen();
    }

    @Override protected void onPause(){if(feedback!=null)feedback.release();super.onPause();}

    private void showQuizScreen() {
        applySystemBars();

        ScrollView scroll = new ScrollView(this);
        quizScroll=scroll;
        scroll.setBackgroundColor(bgColor());
        LinearLayout root = column(18, 16, 18, 28);
        scroll.addView(root);

        Button back = secondaryButton("← Accueil");
        back.setOnClickListener(v -> showHome());
        root.addView(back);

        String modeLabel = reviewMode ? "RÉVISION DES ERREURS" : (customMode ? customModeTitle : "NIVEAU " + activeLevel);
        TextView mode = text(modeLabel, 13, true);
        mode.setTextColor(accentColor());
        mode.setPadding(0, dp(18), 0, dp(4));
        root.addView(mode);

        String screenTitle = reviewMode ? "Renforce tes points faibles" :
                (activeChapter!=null ? AcademyChapters.find(activeChapter).title : customMode ? "Session personnalisée" : levels.get(activeLevel - 1).title);
        addTitle(root, screenTitle, 25);

        pointsView = text("", 14, true);
        root.addView(pointsView);

        categoryView = text("", 13, true);
        categoryView.setTextColor(accentColor());
        categoryView.setPadding(0, dp(12), 0, dp(3));
        root.addView(categoryView);

        progressView = text("", 13, false);
        progressView.setPadding(0, 0, 0, dp(8));
        root.addView(progressView);

        questionProgress = new ProgressBarView(this, isDark());
        questionProgress.setProgress(quiz.isEmpty() ? 0 : ((currentIndex + 1) * 100 / quiz.size()));
        root.addView(questionProgress, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(9)
        ));

        illustrationView = new IllustrationView(this, isDark());
        LinearLayout.LayoutParams illustrationParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(88)
        );
        illustrationParams.topMargin = dp(14);
        root.addView(illustrationView, illustrationParams);
        illustrationView.setVisibility(View.GONE); // Keep the worked example beside the answer, not before the question.

        primerContextView=card("");
        primerContextView.setContentDescription("Contexte à lire avant la question");
        root.addView(primerContextView,spaced(12));
        primerCommandView=card("");
        primerCommandView.setTypeface(Typeface.MONOSPACE);
        primerCommandView.setTextSize(14);
        root.addView(primerCommandView,spaced(8));
        primerOutputView=card("");
        primerOutputView.setTypeface(Typeface.MONOSPACE);
        primerOutputView.setTextSize(14);
        root.addView(primerOutputView,spaced(8));
        primerFocusView=text("",14,true);
        primerFocusView.setTextColor(accentColor());
        root.addView(primerFocusView,spaced(8));

        questionView = text("", 21, true);
        questionView.setPadding(0, dp(18), 0, dp(14));
        root.addView(questionView);

        optionButtons.clear();
        for (int i = 0; i < 5; i++) {
            final int answerIndex = i;
            Button button = answerButton("");
            button.setOnClickListener(v -> answer(answerIndex));
            root.addView(button);
            optionButtons.add(button);
        }

        writtenAnswer = new EditText(this);
        writtenAnswer.setTextColor(textColor());
        writtenAnswer.setHint("Tape ta réponse ici");
        writtenAnswer.setSingleLine(true);
        writtenAnswer.setMinHeight(dp(56));
        writtenAnswer.setHintTextColor(secondaryTextColor());
        writtenAnswer.setPadding(dp(16),dp(12),dp(16),dp(12));
        writtenAnswer.setBackground(roundedDrawable(surfaceColor(),16,borderColor()));
        writtenAnswer.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        writtenAnswer.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        root.addView(writtenAnswer);
        submitWritten = actionButton("Vérifier ma réponse");
        submitWritten.setOnClickListener(v -> answerWritten());
        writtenAnswer.setOnEditorActionListener((v,a,e) -> { if(a==android.view.inputmethod.EditorInfo.IME_ACTION_DONE){answerWritten();return true;}return false; });
        root.addView(submitWritten);
        sourceIllustration = new LinearLayout(this);
        sourceIllustration.setOrientation(LinearLayout.VERTICAL);

        feedbackView = card("");
        feedbackView.setVisibility(View.GONE);
        root.addView(feedbackView, spaced(10));
        moreDetailsButton = secondaryFullButton("Plus de détails et un exemple  ↓");
        moreDetailsButton.setVisibility(View.GONE);
        moreDetailsButton.setOnClickListener(v -> {
            boolean opening=detailView.getVisibility()!=View.VISIBLE;
            detailView.setVisibility(opening?View.VISIBLE:View.GONE);
            sourceIllustration.setVisibility(opening?View.VISIBLE:View.GONE);
            moreDetailsButton.setText(opening?"Masquer les détails  ↑":"Plus de détails et un exemple  ↓");
            if(opening)quizScroll.post(() -> quizScroll.smoothScrollTo(0,moreDetailsButton.getTop()));
        });
        root.addView(moreDetailsButton);
        detailView=card("");
        detailView.setVisibility(View.GONE);
        root.addView(detailView,spaced(8));
        sourceIllustration.setVisibility(View.GONE);
        root.addView(sourceIllustration);

        nextButton = actionButton("Question suivante");
        nextButton.setVisibility(View.GONE);
        nextButton.setOnClickListener(v -> nextQuestion());
        root.addView(nextButton);

        protectFromSystemBars(scroll);

        setContentView(scroll);
        renderQuestion();
    }

    private void renderQuestion() {
        Question q = quiz.get(currentIndex);
        answered = false;
        answerOrder.clear();
        for (int i = 0; i < q.options.size(); i++) answerOrder.add(i);
        Collections.shuffle(answerOrder);
        questionProgress.setProgress((currentIndex + 1) * 100 / quiz.size());

        categoryView.setText(q.category + "  •  difficulté " + q.level + "  •  " + q.format);
        progressView.setText("Question " + (currentIndex + 1) + " / " + quiz.size() +
                (examMode ? "   •   correction à la fin" : "   •   Bonnes réponses : " + correctCount));
        pointsView.setText("+" + sessionPoints + " pts cette session   •   Série " + streak);
        QuizPrimer primer=QuizPrimer.forQuestion(q);
        primerContextView.setText("AVANT DE RÉPONDRE\n\n"+primer.context);
        primerCommandView.setVisibility(primer.command.isEmpty()?View.GONE:View.VISIBLE);
        if(!primer.command.isEmpty())primerCommandView.setText("À TAPER DANS LE TERMINAL\n"+primer.command);
        primerOutputView.setVisibility(primer.output.isEmpty()?View.GONE:View.VISIBLE);
        if(!primer.output.isEmpty())primerOutputView.setText("CE QUI S’AFFICHE (EXEMPLE)\n"+primer.output);
        primerFocusView.setVisibility(primer.focus.isEmpty()?View.GONE:View.VISIBLE);
        if(!primer.focus.isEmpty())primerFocusView.setText("À OBSERVER : "+primer.focus);
        questionView.setText((q.accepted==null?"À TOI DE CHOISIR\n":"À TOI DE TAPER\n")+primer.question);
        illustrationView.setCategory(q.category);

        feedbackView.setVisibility(View.GONE);
        moreDetailsButton.setVisibility(View.GONE);
        moreDetailsButton.setText("Plus de détails et un exemple  ↓");
        detailView.setVisibility(View.GONE);
        sourceIllustration.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);
        sourceIllustration.removeAllViews();
        writtenAnswer.setText("");
        writtenAnswer.setEnabled(true);
        writtenAnswer.setVisibility(q.accepted == null ? View.GONE : View.VISIBLE);
        submitWritten.setVisibility(q.accepted == null ? View.GONE : View.VISIBLE);
        submitWritten.setEnabled(true);

        for (int i = 0; i < optionButtons.size(); i++) {
            Button button = optionButtons.get(i);
            boolean visible=q.accepted==null && i<answerOrder.size();
            button.setVisibility(visible?View.VISIBLE:View.GONE);
            if(!visible)continue;
            button.setText(q.options.get(answerOrder.get(i)));
            button.setEnabled(true);
            styleAnswerNeutral(button);
        }
    }

    private void answer(int button) {
        if(answered)return;
        Question q=quiz.get(currentIndex);
        int index=answerOrder.get(button);
        gradeAnswer(index==q.correctIndex,q.options.get(index),button);
    }
    private void answerWritten() {
        if(answered)return;
        String value=writtenAnswer.getText().toString().trim();
        if(value.isEmpty()){writtenAnswer.setError("Écris une réponse avant de valider.");return;}
        Question q=quiz.get(currentIndex);
        gradeAnswer(QuizAnswerMatcher.matches(value,q.accepted),value,-1);
    }
    private void gradeAnswer(boolean isCorrect,String response,int selectedButton) {
        if(answered)return;
        answered=true;
        Question q=quiz.get(currentIndex);
        writtenAnswer.setEnabled(false);submitWritten.setEnabled(false);
        if(isCorrect&&!examMode&&feedback!=null)feedback.success();
        recordDailyAnswer();
        recordCategoryResult(q.category, isCorrect);
        if (selectedButton >= 0 && selectedButton < optionButtons.size()) {
            optionButtons.get(selectedButton).performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
        }

        if (examMode) {
            examCorrections.add(new ExamCorrection(q,response,isCorrect));
            Set<String> examWrong = new HashSet<>(prefs.getStringSet("wrongQuestions", Collections.emptySet()));
            int answeredTotalExam = prefs.getInt("answeredTotal", 0) + 1;
            int correctTotalExam = prefs.getInt("correctTotal", 0);

            if (isCorrect) {
                correctCount++;
                correctTotalExam++;
                examWrong.remove(q.text);
            } else {
                examWrong.add(q.text);
                String mistakeKey = "mistake_" + Math.abs(q.text.hashCode());
                prefs.edit().putInt(mistakeKey, prefs.getInt(mistakeKey, 0) + 1).apply();
            }

            prefs.edit()
                .putInt("answeredTotal", answeredTotalExam)
                .putInt("correctTotal", correctTotalExam)
                .putStringSet("wrongQuestions", examWrong)
                .apply();

            if (currentIndex < quiz.size() - 1) {
                currentIndex++;
                renderQuestion();
            } else {
                finishLevel();
            }
            return;
        }

        Set<String> wrong = new HashSet<>(prefs.getStringSet("wrongQuestions", Collections.emptySet()));
        int answeredTotal = prefs.getInt("answeredTotal", 0) + 1;
        int correctTotal = prefs.getInt("correctTotal", 0);

        if (isCorrect) {
            correctCount++;
            streak++;
            correctTotal++;
            wrong.remove(q.text);

            int bonus = Math.min(100, Math.max(0, streak - 1) * 25);
            int earned = 100 + bonus;
            sessionPoints += earned;

            int totalPoints = prefs.getInt("totalPoints", 0) + earned;
            int bestStreak = Math.max(prefs.getInt("bestStreak", 0), streak);
            prefs.edit()
                .putInt("totalPoints", totalPoints)
                .putInt("bestStreak", bestStreak)
                .apply();

            feedbackView.setText("✓ Bonne réponse\n\n" + QuizTeaching.simple(q,response,true) +
                "\n\n+" + earned + " points" + (bonus > 0 ? "  •  bonus série +" + bonus : ""));
        } else {
            streak = 0;
            wrong.add(q.text);
            String mistakeKey = "mistake_" + Math.abs(q.text.hashCode());
            prefs.edit().putInt(mistakeKey, prefs.getInt(mistakeKey, 0) + 1).apply();
            feedbackView.setText("✗ À revoir\n\n" + QuizTeaching.simple(q,response,false));
        }

        prefs.edit()
            .putInt("answeredTotal", answeredTotal)
            .putInt("correctTotal", correctTotal)
            .putStringSet("wrongQuestions", wrong)
            .apply();

        for (int i = 0; i < optionButtons.size(); i++) {
            Button button = optionButtons.get(i);
            button.setEnabled(false);
            if(i>=answerOrder.size())continue;
            if (answerOrder.get(i) == q.correctIndex) {
                styleAnswerCorrect(button);
            } else if (i == selectedButton && !isCorrect) {
                styleAnswerWrong(button);
            } else {
                styleAnswerMuted(button);
            }
        }

        feedbackView.setVisibility(View.VISIBLE);
        detailView.setText(QuizTeaching.details(q,response,isCorrect));
        moreDetailsButton.setVisibility(View.VISIBLE);
        showSourceIllustration(q);
        nextButton.setText(currentIndex == quiz.size() - 1 ? "Voir le résultat" : "Question suivante");
        nextButton.setVisibility(View.VISIBLE);
        pointsView.setText("+" + sessionPoints + " pts cette session   •   Série " + streak);
        quizScroll.post(() -> quizScroll.smoothScrollTo(0, feedbackView.getTop()));
    }

    private void showSourceIllustration(Question question) {
        sourceIllustration.removeAllViews();
        if (question.category.equals("Permissions") || question.category.equals("ACL et droits spéciaux")) {
            sourceIllustration.addView(card("PROPRIÉTAIRE  |  GROUPE  |  AUTRES\nrwx = 7         r-x = 5       --- = 0\nFichier : r lire · w modifier · x exécuter\nDossier : r lister · w modifier les entrées · x traverser\nCréation : mode maximal & ~umask\nSGID : groupe hérité · sticky : suppression protégée\nACL nommée : droits limités par le masque ACL"));
            return;
        }
        if (question.category.equals("VirtualBox")) {
            sourceIllustration.addView(card("createvm : configuration de la VM\nstoragectl : contrôleur SATA/IDE\ncreatemedium : disque virtuel\nstorageattach : disque/ISO raccordé au contrôleur\nmodifyvm : RAM, vidéo, réseau\nstartvm : démarrage\n2048 Mo de RAM = 2 Gio ; les modèles Android ne lancent pas d’OS invité."));
            return;
        }
        if (question.category.equals("Windows")) {
            sourceIllustration.addView(card("WINDOWS CMD : C:\\Users\\ubuntu\\Secured · dir\nLINUX BASH : /home/ubuntu/Secured · ls\ncd change de dossier dans les deux contextes.\nNTFS est un système de fichiers ; un chemin indique un emplacement."));
            return;
        }
        if (question.category.equals("Utilisateurs")) {
            sourceIllustration.addView(card("/etc/passwd : compte · UID · GID primaire · home · shell\n/etc/group : groupes et membres supplémentaires\n/etc/shadow : empreintes protégées\nusermod -aG : ajoute sans enlever les autres groupes\nid : vérifie le résultat · whoami : identité effective"));
            return;
        }
        if(!question.category.equals("Git")&&!question.category.equals("Branches")&&!question.category.equals("Conflits")) {
            sourceIllustration.addView(card(question.category.equals("SSH") ? "Clé privée : reste sur ton appareil. Clé publique (.pub) : se copie sur GitHub. Empreinte : identifie la clé sans la remplacer. Source : mémo Git/GitHub/SSH fourni." : "Observe l’exemple : quelle était la situation avant la commande ? Qu’est-ce qui a changé après ? Reprends la fiche du cours indiquée dans la correction."));
            return;
        }
        boolean merge=question.category.equals("Conflits")||question.category.equals("Branches");
        TextView caption=card(merge
            ? "Lire le schéma : BASE est l’ancêtre commun ; les deux branches ont ensuite leurs propres commits. Une fusion combine ces évolutions. Les noms master/iss53 du dessin jouent le rôle de main/cheese dans tes PDF."
            : "Lire le schéma : Working Directory = tes fichiers ; Staging Area = l’index préparé par git add ; .git = les versions enregistrées par commit. Un commit reste local jusqu’au push.");
        sourceIllustration.addView(caption);
        android.webkit.WebView diagram=new android.webkit.WebView(this);
        diagram.setBackgroundColor(Color.WHITE);
        diagram.getSettings().setJavaScriptEnabled(false);
        diagram.getSettings().setAllowContentAccess(false);
        diagram.getSettings().setAllowFileAccess(false);
        diagram.setContentDescription(merge ? "Schéma de fusion à trois sources avec ancêtre commun" : "Schéma des fichiers, de l’index et du dépôt Git");
        try {
            String name=merge?"progit_merge.svg":"progit_areas.svg";
            java.io.InputStream input=getAssets().open(name);
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
            byte[] buffer=new byte[4096];int count;while((count=input.read(buffer))!=-1)bytes.write(buffer,0,count);input.close();
            String svg=new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8).replaceFirst("<\\?xml[^>]*>", "");
            diagram.loadDataWithBaseURL(null,"<html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>body{margin:8px}svg{width:100%;height:auto}</style></head><body>"+svg+"</body></html>","text/html","UTF-8",null);
            sourceIllustration.addView(diagram,new LinearLayout.LayoutParams(-1,dp(230)));
        } catch(java.io.IOException e) { sourceIllustration.addView(card("Le schéma n’a pas pu être chargé. Utilise la légende ci-dessus.")); }
        TextView credit=text("Illustration : Pro Git, Scott Chacon et Ben Straub — CC BY-NC-SA 3.0. Schéma original conservé ; légende française ajoutée.",12,false);
        sourceIllustration.addView(credit);
        Button source=secondaryButton("Lire la source officielle Git");
        source.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(merge
            ? "https://git-scm.com/book/en/v2/Git-Branching-Basic-Branching-and-Merging"
            : "https://git-scm.com/book/en/v2/Getting-Started-What-is-Git%3F"))));
        sourceIllustration.addView(source);
    }

    private static void mixExercises(List<Question> list) {
        List<Question> written=new ArrayList<>(), traps=new ArrayList<>(), choice=new ArrayList<>();
        for(Question q:list) {
            if(q.accepted!=null)written.add(q);
            else if(q.options.contains("Aucune de ces réponses"))traps.add(q);
            else choice.add(q);
        }
        Collections.shuffle(written);Collections.shuffle(traps);Collections.shuffle(choice);
        list.clear();
        for(int i=0;i<Math.max(written.size(),Math.max(traps.size(),choice.size()));i++) {
            if(i<written.size())list.add(written.get(i));
            if(i<choice.size())list.add(choice.get(i));
            if(i<traps.size())list.add(traps.get(i));
        }
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
        boolean newBest = false;
        if(activeChapter!=null){int previous=prefs.getInt("chapter_best_"+activeChapter,0);prefs.edit().putInt("chapter_best_"+activeChapter,Math.max(previous,percent)).apply();}

        if (!reviewMode && !customMode) {
            int previousBest = prefs.getInt("best_level_" + activeLevel, 0);
            newBest = percent > previousBest;
            if (newBest) prefs.edit().putInt("best_level_" + activeLevel, percent).apply();
            if (percent == 100) prefs.edit().putBoolean("perfectLevel", true).apply();
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bgColor());
        LinearLayout root = column(22, 42, 22, 34);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(root);

        addTitle(root, reviewMode ? "Révision terminée" : (customMode ? customModeTitle : "Niveau terminé"), 30);

        TextView big = text(percent + "%", 46, true);
        big.setTextColor(percent >= PASS_PERCENT ? successColor() : accentColor());
        big.setGravity(Gravity.CENTER);
        root.addView(big);

        addBody(root,
            correctCount + " / " + quiz.size() + " bonnes réponses\n" +
            sessionPoints + " points gagnés" +
            (newBest ? "\nNouveau meilleur score !" : ""),
            17
        );

        if (examMode) for (ExamCorrection attempt : examCorrections) {
            root.addView(card((attempt.correct?"✓ ":"✗ ")+attempt.question.text+"\n\n"+
                QuizTeaching.simple(attempt.question,attempt.response,attempt.correct)),spaced(10));
            TextView full=card(QuizTeaching.details(attempt.question,attempt.response,attempt.correct));
            full.setVisibility(View.GONE);
            Button expand=secondaryFullButton("Plus de détails et un exemple  ↓");
            expand.setOnClickListener(v -> {
                boolean opening=full.getVisibility()!=View.VISIBLE;
                full.setVisibility(opening?View.VISIBLE:View.GONE);
                expand.setText(opening?"Masquer les détails  ↑":"Plus de détails et un exemple  ↓");
            });
            root.addView(expand);
            root.addView(full,spaced(8));
        }

        String message;
        if (reviewMode) {
            int remaining = prefs.getStringSet("wrongQuestions", Collections.emptySet()).size();
            message = remaining == 0
                ? "Excellent : toutes les erreurs enregistrées ont été corrigées."
                : remaining + " question(s) restent dans ta liste de révision.";
        } else if (customMode) {
            message = "Session terminée. Les erreurs ont été ajoutées automatiquement à ta liste de révision.";
        } else if (percent >= 90) {
            message = "Excellent. Les commandes de ce niveau sont bien maîtrisées.";
        } else if (percent >= PASS_PERCENT) {
            message = "Niveau validé. Le niveau suivant est maintenant accessible.";
        } else if (percent >= 50) {
            message = "Presque. Rejoue pour atteindre 70% et débloquer la suite.";
        } else {
            message = "Reprends les exemples et le mémo puis retente ce niveau.";
        }
        root.addView(card(message), spaced(14));

        Button replay = actionButton(reviewMode ? "Réviser les erreurs restantes" :
                (customMode ? "Nouvelle session" : "Rejouer ce niveau"));
        replay.setOnClickListener(v -> {
            if (reviewMode) startReviewErrors();
            else if (customMode && examMode) startExam();
            else if (activeChapter != null) startCategoryQuiz(activeChapter);
            else if (customMode && "ENTRAÎNEMENT ADAPTATIF".equals(customModeTitle)) startAdaptiveQuiz();
            else if (customMode) startQuickQuiz();
            else startLevel(activeLevel);
        });
        root.addView(replay);

        Button stats = secondaryFullButton("Voir mes statistiques");
        stats.setOnClickListener(v -> showStats());
        root.addView(stats);

        Button home = secondaryFullButton("Retour à l'accueil");
        home.setOnClickListener(v -> showHome());
        root.addView(home);

        protectFromSystemBars(scroll);

        setContentView(scroll);
    }

    private void startQuickQuiz() {
        activeChapter=null;
        List<Question> pool = new ArrayList<>(questions);
        mixExercises(pool);
        quiz = new ArrayList<>(pool.subList(0, Math.min(10, pool.size())));
        reviewMode = false;
        customMode = true;
        examMode = false;
        customModeTitle = "QUIZ RAPIDE";
        activeLevel = 0;
        currentIndex = 0;
        correctCount = 0;
        sessionPoints = 0;
        streak = 0;
        showQuizScreen();
    }

    private void startExam() {
        activeChapter=null;
        examCorrections.clear();
        List<Question> pool = new ArrayList<>();
        for (int level = 1; level <= 5; level++) {
            List<Question> candidates = new ArrayList<>();
            for (Question question : questions) if (question.level == level) candidates.add(question);
            mixExercises(candidates);
            pool.addAll(candidates.subList(0, Math.min(4, candidates.size())));
        }
        mixExercises(pool);
        quiz = pool;
        reviewMode = false;
        customMode = true;
        examMode = true;
        customModeTitle = "MODE EXAMEN";
        activeLevel = 0;
        currentIndex = 0;
        correctCount = 0;
        sessionPoints = 0;
        streak = 0;
        showQuizScreen();
    }

    private void startAdaptiveQuiz() {
        activeChapter=null;
        Set<String> wrong = prefs.getStringSet("wrongQuestions", Collections.emptySet());
        List<Question> pool = new ArrayList<>();
        for (Question q : questions) if (wrong.contains(q.text)) pool.add(q);
        mixExercises(pool);
        pool.sort((a, b) -> Integer.compare(
            prefs.getInt("mistake_" + Math.abs(b.text.hashCode()), 0),
            prefs.getInt("mistake_" + Math.abs(a.text.hashCode()), 0)));
        if (pool.size() > 8) pool = new ArrayList<>(pool.subList(0, 8));
        List<Question> rest = new ArrayList<>(questions);
        mixExercises(rest);
        for (Question q : rest) {
            if (pool.size() >= 12) break;
            if (!pool.contains(q)) pool.add(q);
        }
        mixExercises(pool);
        quiz = pool;
        reviewMode = false;
        customMode = true;
        examMode = false;
        customModeTitle = "ENTRAÎNEMENT ADAPTATIF";
        activeLevel = 0;
        currentIndex = 0;
        correctCount = 0;
        sessionPoints = 0;
        streak = 0;
        showQuizScreen();
    }

    private void showCategoryPicker() {
        applySystemBars();
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(bgColor());LinearLayout root=column(20,20,20,24);scroll.addView(root);
        TextView overline=text("LE PARCOURS",11,true);overline.setLetterSpacing(.15f);overline.setTextColor(accentColor());root.addView(overline);
        addTitle(root,"Choisis ton chapitre",28);
        addBody(root,"Un sujet à la fois. Un quiz entièrement consacré à ce que tu viens d’apprendre.",15);
        int number=0;
        for(AcademyChapters.Chapter chapter:AcademyChapters.ALL){
            final int index=++number;int count=AcademyChapters.questions(questions,chapter.category).size();
            LinearLayout box=column(18,18,18,16);box.setBackground(roundedDrawable(surfaceColor(),24,borderColor()));
            LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);
            TextView icon=text(chapter.symbol,18,true);icon.setGravity(Gravity.CENTER);icon.setTextColor(accentColor());icon.setBackground(roundedDrawable(accentSurfaceColor(),16,0));heading.addView(icon,new LinearLayout.LayoutParams(dp(48),dp(48)));
            LinearLayout labels=column(12,0,0,0);labels.setBackgroundColor(Color.TRANSPARENT);TextView label=text("CHAPITRE "+String.format(Locale.ROOT,"%02d",index),11,true);label.setTextColor(secondaryTextColor());labels.addView(label);labels.addView(text(chapter.title,18,true));heading.addView(labels,new LinearLayout.LayoutParams(0,-2,1));box.addView(heading);
            TextView description=text(chapter.description,14,false);description.setTextColor(secondaryTextColor());description.setPadding(0,dp(14),0,dp(8));box.addView(description);
            int best=prefs.getInt("chapter_best_"+chapter.category,0);TextView stats=text(count+" questions   ·   "+(best>0?"Meilleur score "+best+" %":"À découvrir"),12,false);stats.setTextColor(secondaryTextColor());box.addView(stats);
            if(best>0){ProgressBarView bar=new ProgressBarView(this,isDark());bar.setProgress(best);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(6));bp.topMargin=dp(10);box.addView(bar,bp);}
            Button start=actionButton("Quiz du chapitre  →");start.setEnabled(count>0);start.setOnClickListener(v->startCategoryQuiz(chapter.category));box.addView(start,spaced(14));root.addView(box,spaced(12));
        }
        installAcademyPage(scroll,2);
    }

    private void startCategoryQuiz(String category) {
        activeChapter=category;
        prefs.edit().putString("lastChapter",category).apply();
        quiz = AcademyChapters.questions(questions,category);
        if(quiz.isEmpty()){showCategoryPicker();return;}
        mixExercises(quiz);
        reviewMode = false;
        customMode = true;
        examMode = false;
        customModeTitle = "CHAPITRE · " + category;
        activeLevel = 0;
        currentIndex = 0;
        correctCount = 0;
        sessionPoints = 0;
        streak = 0;
        showQuizScreen();
    }

    private void showCourseMenu() {
        applySystemBars();
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bgColor());
        LinearLayout root = column(18, 18, 18, 28);
        scroll.addView(root);
        addTitle(root, "Fiches de cours", 28);
        addBody(root, "Relis une fiche courte avant de passer aux exercices.", 15);

        for (LevelInfo info : levels) {
            Button b = secondaryFullButton("Niveau " + info.number + " — " + info.title);
            b.setOnClickListener(v -> showLesson(info.number));
            root.addView(b);
        }
        Button back = secondaryFullButton("← Accueil");
        back.setOnClickListener(v -> showHome());
        root.addView(back, spaced(10));
        protectFromSystemBars(scroll);
        setContentView(scroll);
    }

    private void showLesson(int level) {
        applySystemBars();
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bgColor());
        LinearLayout root = column(18, 18, 18, 28);
        scroll.addView(root);
        LevelInfo info = levels.get(level - 1);
        addTitle(root, info.title, 27);
        addBody(root, info.description, 15);

        if (level == 1) {
            addMemoGroup(root, "Navigation",
                "pwd — afficher le chemin courant",
                "ls — lister le contenu",
                "ls -la — détails + fichiers cachés",
                "cd dossier — entrer dans un dossier",
                "cd .. — remonter d'un niveau",
                "cd ~ — revenir au dossier personnel",
                "mkdir -p a/b/c — créer une arborescence",
                "touch fichier.txt — créer un fichier vide");
        } else if (level == 2) {
            addMemoGroup(root, "Fichiers et Git",
                "cat fichier.txt — lire un fichier",
                "echo \"texte\" > fichier.txt — créer/remplacer",
                "echo \"suite\" >> fichier.txt — ajouter",
                "rm -r dossier — supprimer récursivement",
                "git init — initialiser un dépôt",
                "git status — voir l'état du dépôt",
                "git add README.md — préparer un fichier",
                "git commit -m \"message\" — créer un commit");
        } else if (level == 3) {
            addMemoGroup(root, "Workflow",
                "git status → git add → git commit → git push",
                "git remote -v — voir les remotes",
                "git diff — changements non staged",
                "git diff --staged — changements staged",
                "HEAD — position courante",
                "une branche — pointeur nommé vers un commit");
        } else if (level == 4) {
            addMemoGroup(root, "Branches et SSH",
                "git switch -c feature — créer + basculer",
                "git branch -d feature — supprimer localement",
                "git push origin feature — publier une branche",
                "git push origin --delete feature — supprimer à distance",
                "ssh-keygen -t ed25519 -C \"email\" — créer une paire de clés",
                "cat ~/.ssh/id_ed25519.pub — afficher la clé publique",
                "ssh -T git@github.com — tester l'authentification");
        } else {
            addMemoGroup(root, "Synchronisation et conflits",
                "git pull --rebase origin main — récupérer puis rejouer les commits locaux",
                "<<<<<<< HEAD — version de la branche courante",
                "======= — séparation des versions",
                ">>>>>>> — version entrante",
                "git add fichier — marquer la résolution dans l'index");
        }

        Button practice = actionButton("S'entraîner sur ce niveau");
        practice.setOnClickListener(v -> startLevel(level));
        root.addView(practice, spaced(12));
        Button back = secondaryFullButton("← Fiches de cours");
        back.setOnClickListener(v -> showCourseMenu());
        root.addView(back);
        protectFromSystemBars(scroll);
        setContentView(scroll);
    }

    private void recordDailyAnswer() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        String today = fmt.format(new Date());
        String savedDay = prefs.getString("daily_day", "");

        if (!today.equals(savedDay)) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -1);
            String yesterday = fmt.format(cal.getTime());
            int oldStreak = prefs.getInt("dailyStreak", 0);
            int newStreak = yesterday.equals(savedDay) ? oldStreak + 1 : 1;

            prefs.edit()
                .putString("daily_day", today)
                .putInt("daily_answered", 1)
                .putInt("dailyStreak", newStreak)
                .apply();
        } else {
            prefs.edit().putInt("daily_answered", prefs.getInt("daily_answered", 0) + 1).apply();
        }
    }

    private int dailyAnswered() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
        return today.equals(prefs.getString("daily_day", "")) ? prefs.getInt("daily_answered", 0) : 0;
    }

    private void recordCategoryResult(String category, boolean correct) {
        String a = "cat_answered_" + category;
        String c = "cat_correct_" + category;
        SharedPreferences.Editor e = prefs.edit().putInt(a, prefs.getInt(a, 0) + 1);
        if (correct) e.putInt(c, prefs.getInt(c, 0) + 1);
        e.apply();
    }

    private void showCommandLibrary(String initialQuery) {
        applySystemBars();
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bgColor());
        LinearLayout root = column(18, 18, 18, 28);
        scroll.addView(root);

        addTitle(root, "Bibliothèque des commandes", 27);
        addBody(root, "Recherche une commande, copie-la ou ajoute-la aux favoris.", 15);

        EditText search = new EditText(this);
        search.setHint("Ex. ssh, branch, status, mkdir...");
        search.setText(initialQuery);
        search.setSingleLine(true);
        search.setTextColor(textColor());
        search.setHintTextColor(secondaryTextColor());
        search.setBackground(roundedDrawable(surfaceColor(), 16, borderColor()));
        search.setPadding(dp(14), dp(10), dp(14), dp(10));
        root.addView(search);

        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        root.addView(results, spaced(10));

        Button doSearch = actionButton("Rechercher");
        doSearch.setOnClickListener(v -> renderCommandLibrary(results, search.getText().toString()));
        root.addView(doSearch, spaced(8));

        Button onlyFavorites = secondaryFullButton("Afficher uniquement mes favoris");
        onlyFavorites.setOnClickListener(v -> renderCommandLibrary(results, "__FAVORITES__"));
        root.addView(onlyFavorites);

        renderCommandLibrary(results, initialQuery);

        Button back = secondaryFullButton("← Accueil");
        back.setOnClickListener(v -> showHome());
        root.addView(back, spaced(14));
        protectFromSystemBars(scroll);
        setContentView(scroll);
    }

    private void renderCommandLibrary(LinearLayout results, String query) {
        results.removeAllViews();

        String[][] commands = new String[][]{
            {"pwd", "Afficher le chemin absolu du dossier courant"},
            {"ls", "Lister le contenu visible"},
            {"ls -la", "Afficher les détails et les fichiers cachés"},
            {"cd dossier", "Entrer dans un dossier"},
            {"cd ..", "Remonter d'un niveau"},
            {"cd ~", "Revenir dans le dossier personnel"},
            {"mkdir -p a/b/c", "Créer une arborescence avec ses parents"},
            {"touch fichier.txt", "Créer un fichier vide"},
            {"cat fichier.txt", "Lire le contenu d'un fichier"},
            {"echo \"texte\" > fichier.txt", "Créer ou remplacer le contenu"},
            {"echo \"suite\" >> fichier.txt", "Ajouter du texte à la fin"},
            {"rm -r dossier", "Supprimer récursivement un dossier"},
            {"history", "Afficher l'historique des commandes"},
            {"git init", "Initialiser un dépôt Git"},
            {"git status", "Voir l'état du working directory et du staging"},
            {"git add README.md", "Préparer un fichier pour le prochain commit"},
            {"git commit -m \"message\"", "Créer un commit avec un message"},
            {"git log", "Afficher l'historique des commits"},
            {"git log -p", "Afficher les commits avec leurs différences"},
            {"git remote -v", "Afficher les remotes et leurs URL"},
            {"git remote add origin URL", "Ajouter le remote origin"},
            {"git remote set-url origin URL", "Modifier l'URL du remote origin"},
            {"git push -u origin main", "Premier push de main avec upstream"},
            {"git push", "Publier les commits vers le remote suivi"},
            {"git pull --rebase origin main", "Récupérer le distant puis rejouer les commits locaux"},
            {"git switch -c feature", "Créer une branche et basculer dessus"},
            {"git branch -d feature", "Supprimer prudemment une branche locale"},
            {"git push origin feature", "Publier une branche distante"},
            {"git push origin --delete feature", "Supprimer une branche sur origin"},
            {"git diff", "Afficher les changements non staged"},
            {"git diff --staged", "Afficher ce qui est préparé pour le commit"},
            {"ssh-keygen -t ed25519 -C \"email\"", "Créer une paire de clés SSH Ed25519"},
            {"cat ~/.ssh/id_ed25519.pub", "Afficher la clé publique SSH"},
            {"ssh -T git@github.com", "Tester l'authentification SSH GitHub"}
        };

        Set<String> favorites = new HashSet<>(prefs.getStringSet("favoriteCommands", Collections.emptySet()));
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        boolean favOnly = "__favorites__".equals(q);

        int shown = 0;
        for (String[] item : commands) {
            String cmd = item[0];
            String desc = item[1];
            if (favOnly && !favorites.contains(cmd)) continue;
            if (!favOnly && !q.isEmpty() &&
                !cmd.toLowerCase(Locale.ROOT).contains(q) &&
                !desc.toLowerCase(Locale.ROOT).contains(q)) continue;

            LinearLayout card = column(14, 14, 14, 14);
            card.setBackground(roundedDrawable(surfaceColor(), 18, borderColor()));

            TextView command = text(cmd, 15, true);
            command.setTypeface(Typeface.MONOSPACE);
            command.setTextColor(accentColor());
            card.addView(command);
            addBody(card, desc, 13);

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);

            Button copy = secondaryButton("Copier");
            copy.setOnClickListener(v -> {
                ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                cb.setPrimaryClip(ClipData.newPlainText("commande", cmd));
                copy.setText("Copié ✓");
            });
            actions.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Button favorite = secondaryButton(favorites.contains(cmd) ? "★ Favori" : "☆ Favori");
            favorite.setOnClickListener(v -> {
                Set<String> set = new HashSet<>(prefs.getStringSet("favoriteCommands", Collections.emptySet()));
                if (set.contains(cmd)) {
                    set.remove(cmd);
                    favorite.setText("☆ Favori");
                } else {
                    set.add(cmd);
                    favorite.setText("★ Favori");
                }
                prefs.edit().putStringSet("favoriteCommands", set).apply();
            });
            LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            fp.leftMargin = dp(6);
            actions.addView(favorite, fp);

            card.addView(actions);
            results.addView(card, spaced(7));
            shown++;
        }

        if (shown == 0) {
            TextView empty = card("Aucune commande trouvée.");
            results.addView(empty);
        }
    }

    private void shareProgress() {
        int answered = prefs.getInt("answeredTotal", 0);
        int correct = prefs.getInt("correctTotal", 0);
        int accuracy = answered == 0 ? 0 : correct * 100 / answered;
        StringBuilder report = new StringBuilder();
        report.append("Ubuntu & Git Academy — progression\n\n");
        report.append("XP : ").append(prefs.getInt("totalPoints", 0)).append("\n");
        report.append("Questions : ").append(answered).append("\n");
        report.append("Précision : ").append(accuracy).append("%\n");
        report.append("Meilleure série : ").append(prefs.getInt("bestStreak", 0)).append("\n");
        report.append("Lab XP : ").append(prefs.getInt("labPoints", 0)).append("\n\n");
        for (int i = 1; i <= 5; i++) {
            report.append("Niveau ").append(i).append(" : ")
                  .append(prefs.getInt("best_level_" + i, 0)).append("%\n");
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Ma progression Ubuntu & Git Academy");
        intent.putExtra(Intent.EXTRA_TEXT, report.toString());
        startActivity(Intent.createChooser(intent, "Exporter la progression"));
    }

    private void showStats() {
        applySystemBars();

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bgColor());
        LinearLayout root = column(18, 18, 18, 28);
        scroll.addView(root);

        addTitle(root, "Progression", 28);
        addBody(root, "Visualise tes scores et les thèmes à renforcer.", 15);

        int answeredTotal = prefs.getInt("answeredTotal", 0);
        int correctTotal = prefs.getInt("correctTotal", 0);
        int accuracy = answeredTotal == 0 ? 0 : correctTotal * 100 / answeredTotal;
        int wrongCount = prefs.getStringSet("wrongQuestions", Collections.emptySet()).size();

        LinearLayout summary = column(16, 16, 16, 16);
        summary.setBackground(roundedDrawable(surfaceColor(), 22, borderColor()));
        addBody(summary,
            "Points totaux : " + prefs.getInt("totalPoints", 0) +
            "\nQuestions répondues : " + answeredTotal +
            "\nBonnes réponses : " + correctTotal +
            "\nErreurs à revoir : " + wrongCount +
            "\nMeilleure série : " + prefs.getInt("bestStreak", 0),
            15
        );
        root.addView(summary);

        addSection(root, "Précision globale");
        AccuracyDonutView donut = new AccuracyDonutView(this, isDark());
        donut.setAccuracy(accuracy);
        root.addView(donut, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(210)
        ));

        addSection(root, "Scores par niveau");
        ProgressChartView chart = new ProgressChartView(this, isDark());
        int[] best = new int[5];
        for (int i = 0; i < 5; i++) best[i] = prefs.getInt("best_level_" + (i + 1), 0);
        chart.setValues(best);
        root.addView(chart, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(250)
        ));

        if (wrongCount > 0) {
            Button review = actionButton("Réviser " + wrongCount + " erreur(s)");
            review.setOnClickListener(v -> startReviewErrors());
            root.addView(review, spaced(12));
        }

        addSection(root, "Maîtrise par thème");
        String[] cats = {"Bash","Git","Branches","SSH","Synchronisation","Conflits","Utilisateurs","Permissions","ACL et droits spéciaux","VirtualBox","Windows"};
        for (String cat : cats) {
            int a = prefs.getInt("cat_answered_" + cat, 0);
            int c = prefs.getInt("cat_correct_" + cat, 0);
            int p = a == 0 ? 0 : c * 100 / a;
            TextView row = card(cat + " : " + p + "%  •  " + c + "/" + a);
            root.addView(row, spaced(6));
        }

        installAcademyPage(scroll,4);
    }

    private void showBadges() {
        applySystemBars();
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bgColor());
        LinearLayout root = column(18, 18, 18, 28);
        scroll.addView(root);

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

        installAcademyPage(scroll,4);
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
        applySystemBars();
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bgColor());
        LinearLayout root = column(18, 18, 18, 28);
        scroll.addView(root);

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

        installAcademyPage(scroll,4);
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
        layout.setBackgroundColor(bgColor());
        return layout;
    }

    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(textColor());
        t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));
        t.setLineSpacing(dp(2),1.05f);
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
        t.setTextColor(secondaryTextColor());
        t.setLineSpacing(0, 1.15f);
        t.setPadding(0, 0, 0, dp(12));
        root.addView(t);
    }

    private TextView card(String value) {
        TextView t = text(value, 15, false);
        t.setLineSpacing(0, 1.18f);
        t.setPadding(dp(16), dp(16), dp(16), dp(16));
        t.setBackground(roundedDrawable(surfaceColor(), 20, borderColor()));
        return t;
    }

    private Button actionButton(String label) {
        Button b = AcademyDesign.button(this);
        b.setText(label);
        b.setTextSize(15f);
        b.setAllCaps(false);
        b.setTextColor(isDark() ? Color.rgb(31,24,70) : Color.WHITE);
        AcademyDesign.style(b, accentColor(), 18, 0);
        b.setPadding(dp(14), dp(11), dp(14), dp(11));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.bottomMargin = dp(8);
        b.setLayoutParams(p);
        return b;
    }

    private Button secondaryFullButton(String label) {
        Button b = AcademyDesign.button(this);
        b.setText(label);
        b.setTextSize(14f);
        b.setAllCaps(false);
        b.setTextColor(textColor());
        AcademyDesign.style(b, surfaceColor(), 18, borderColor());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.bottomMargin = dp(8);
        b.setLayoutParams(p);
        return b;
    }

    private Button secondaryButton(String label) {
        Button b = AcademyDesign.button(this);
        b.setText(label);
        b.setTextSize(12f);
        b.setAllCaps(false);
        b.setTextColor(textColor());
        AcademyDesign.style(b, surfaceColor(), 16, borderColor());
        b.setMinHeight(dp(48));
        b.setMinimumHeight(dp(48));
        b.setPadding(dp(12), dp(8), dp(12), dp(8));
        return b;
    }

    private Button smallButton(String label) {
        return secondaryButton(label);
    }

    private Button answerButton(String label) {
        Button b = AcademyDesign.button(this);
        b.setText(label);
        b.setTextSize(15f);
        b.setAllCaps(false);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        b.setPadding(dp(16), dp(14), dp(16), dp(14));
        styleAnswerNeutral(b);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.bottomMargin = dp(9);
        b.setLayoutParams(p);
        return b;
    }

    private void styleAnswerNeutral(Button b) {
        b.setTextColor(textColor());
        AcademyDesign.style(b, surfaceColor(), 18, borderColor());
    }

    private void styleAnswerCorrect(Button b) {
        b.setTextColor(Color.WHITE);
        AcademyDesign.style(b, successColor(), 18, 0);
    }

    private void styleAnswerWrong(Button b) {
        b.setTextColor(Color.WHITE);
        AcademyDesign.style(b, errorColor(), 18, 0);
    }

    private void styleAnswerMuted(Button b) {
        b.setTextColor(secondaryTextColor());
        AcademyDesign.style(b, isDark() ? Color.rgb(35,38,44) : Color.rgb(238,240,243), 18, 0);
    }

    private void addBottomNav(LinearLayout root, String active) {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, dp(20), 0, 0);

        String[] labels = {"Accueil", "Stats", "Badges", "Mémo"};
        for (String label : labels) {
            Button b = secondaryButton(label);
            if (label.equals(active)) {
                b.setTextColor(Color.WHITE);
                AcademyDesign.style(b, accentColor(), 16, 0);
            }
            b.setOnClickListener(v -> {
                String t = ((Button) v).getText().toString();
                if ("Accueil".equals(t)) showHome();
                else if ("Stats".equals(t)) showStats();
                else if ("Badges".equals(t)) showBadges();
                else showMemo();
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            p.leftMargin = dp(3);
            p.rightMargin = dp(3);
            nav.addView(b, p);
        }
        root.addView(nav);
    }

    private int findContinueLevel() {
        int candidate = 1;
        for (int i = 1; i <= levels.size(); i++) {
            if (!isLevelUnlocked(i)) break;
            candidate = i;
            if (prefs.getInt("best_level_" + i, 0) < PASS_PERCENT) return i;
        }
        return candidate;
    }

    private boolean isDark() {
        return prefs != null && prefs.getBoolean("darkMode", false);
    }

    private int bgColor() {
        return isDark() ? Color.rgb(16, 22, 36) : Color.rgb(246, 246, 242);
    }

    private int surfaceColor() {
        return isDark() ? Color.rgb(28, 35, 51) : Color.WHITE;
    }

    private int accentSurfaceColor() {
        return isDark() ? Color.rgb(45, 42, 81) : Color.rgb(235, 232, 255);
    }

    private int textColor() {
        return isDark() ? Color.rgb(242, 244, 252) : Color.rgb(29, 35, 52);
    }

    private int secondaryTextColor() {
        return isDark() ? Color.rgb(184, 193, 212) : Color.rgb(86, 98, 118);
    }

    private int borderColor() {
        return isDark() ? Color.rgb(55, 64, 83) : Color.rgb(222, 226, 232);
    }

    private int accentColor() {
        return isDark() ? Color.rgb(174, 163, 255) : AcademyDesign.INDIGO;
    }

    private int successColor() {
        return Color.rgb(19, 113, 81);
    }

    private int errorColor() {
        return Color.rgb(194, 58, 66);
    }

    private GradientDrawable roundedDrawable(int fill, int radiusDp, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
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
                baseBottom + bottom + dp(12)
            );
            return insets;
        });
        view.requestApplyInsets();
    }

    private void applySystemBars() {
        setTheme(isDark() ? R.style.Theme_Academy_Dark : R.style.Theme_Academy);
        getWindow().setStatusBarColor(bgColor());
        getWindow().setNavigationBarColor(bgColor());
        getWindow().getDecorView().setSystemUiVisibility(
            isDark() ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );
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

    static class ProgressBarView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int progress = 0;
        private final boolean dark;

        ProgressBarView(Activity context, boolean dark) {
            super(context);
            this.dark = dark;
        }

        void setProgress(int progress) {
            this.progress = Math.max(0, Math.min(100, progress));
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(dark ? Color.rgb(56, 60, 68) : Color.rgb(226, 229, 234));
            canvas.drawRoundRect(new RectF(0, 0, w, h), h / 2f, h / 2f, paint);
            paint.setColor(dark ? Color.rgb(169,158,255) : AcademyDesign.INDIGO);
            canvas.drawRoundRect(new RectF(0, 0, w * progress / 100f, h), h / 2f, h / 2f, paint);
        }
    }

    static class IllustrationView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private String category = "Bash";
        private final boolean dark;

        IllustrationView(Activity context, boolean dark) {
            super(context);
            this.dark = dark;
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
            canvas.drawColor(dark ? Color.rgb(28, 31, 37) : Color.rgb(248, 249, 251));

            float w = getWidth();
            float h = getHeight();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5f);
            paint.setColor(dark ? Color.rgb(210, 214, 222) : Color.rgb(69, 76, 86));

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
                paint.setColor(dark ? Color.rgb(215, 218, 225) : Color.rgb(64, 70, 78));
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
            paint.setTextSize(12f * getResources().getDisplayMetrics().scaledDensity);
            y = Math.max(paint.getTextSize(), y);
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
        private final boolean dark;

        ProgressChartView(Activity context, boolean dark) {
            super(context);
            this.dark = dark;
        }

        void setValues(int[] values) {
            this.values = values;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(dark ? Color.rgb(28, 31, 37) : Color.rgb(248, 249, 251));
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
                paint.setColor(dark ? Color.rgb(235, 237, 241) : Color.rgb(35, 40, 48));
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
        private final boolean dark;

        AccuracyDonutView(Activity context, boolean dark) {
            super(context);
            this.dark = dark;
        }

        void setAccuracy(int accuracy) {
            this.accuracy = Math.max(0, Math.min(100, accuracy));
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(dark ? Color.rgb(28, 31, 37) : Color.rgb(248, 249, 251));
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
            paint.setColor(dark ? Color.rgb(242, 244, 247) : Color.rgb(30, 35, 42));
            canvas.drawText(accuracy + "%", cx, cy + size * 0.07f, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
    }
}
