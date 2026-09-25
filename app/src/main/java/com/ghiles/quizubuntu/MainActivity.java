package com.ghiles.quizubuntu;

import android.app.Activity;
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

    static class Question {
        final String category;
        final String text;
        final List<String> options;
        final int correctIndex;
        final String explanation;

        Question(String category, String text, int correctIndex, String explanation, String... options) {
            this.category = category;
            this.text = text;
            this.options = Arrays.asList(options);
            this.correctIndex = correctIndex;
            this.explanation = explanation;
        }
    }

    private final List<Question> questions = Arrays.asList(
        q("Bash","Quelle commande affiche le chemin absolu du dossier courant ?",0,
            "pwd signifie Print Working Directory et affiche le chemin courant.",
            "pwd","ls","cd","mkdir"),
        q("Bash","Quelle commande liste le contenu visible du dossier courant ?",1,
            "ls affiche les fichiers et dossiers visibles du répertoire courant.",
            "pwd","ls","cat","history"),
        q("Bash","Quelle commande affiche aussi les fichiers cachés avec des détails ?",0,
            "ls -la combine l'affichage détaillé (-l) et les fichiers cachés (-a).",
            "ls -la","ls -r","pwd -a","cat -la"),
        q("Bash","Quelle commande te ramène dans ton dossier personnel ?",2,
            "~ représente le dossier personnel de l'utilisateur.",
            "cd /","cd .","cd ~","cd .."),
        q("Bash","Quelle commande remonte d'un niveau dans l'arborescence ?",0,
            ".. représente le dossier parent.",
            "cd ..","cd .","cd ~","pwd"),
        q("Bash","Comment créer les dossiers imbriqués a/b/c en une seule commande ?",1,
            "mkdir -p crée les répertoires parents nécessaires.",
            "mkdir a/b/c","mkdir -p a/b/c","touch a/b/c","cp -r a/b/c"),
        q("Bash","Quelle commande crée un fichier vide nommé test.txt ?",0,
            "touch permet de créer un fichier vide.",
            "touch test.txt","mkdir test.txt","cat test.txt","nano -r test.txt"),
        q("Bash","Que fait > dans : echo \"test\" > fichier.txt ?",1,
            "> redirige la sortie et crée ou remplace le contenu du fichier.",
            "Ajoute le texte à la fin","Crée ou remplace le contenu","Supprime le fichier","Affiche seulement le fichier"),
        q("Bash","Que fait >> dans : echo \"suite\" >> fichier.txt ?",0,
            ">> ajoute la sortie à la fin du fichier existant.",
            "Ajoute le texte à la fin","Remplace tout le fichier","Renomme le fichier","Crée un dossier"),
        q("Bash","Quelle commande lit le contenu d'un fichier texte ?",0,
            "cat affiche le contenu d'un fichier dans le terminal.",
            "cat fichier.txt","mv fichier.txt","mkdir fichier.txt","clear fichier.txt"),
        q("Bash","Quelle commande supprime un dossier et tout son contenu ?",2,
            "rm -r réalise une suppression récursive.",
            "rm dossier","rmdir dossier","rm -r dossier","clear dossier"),
        q("Git","Quelle commande initialise un dépôt Git dans le dossier courant ?",0,
            "git init crée les métadonnées Git du dépôt local.",
            "git init","git clone","git add .","git status"),
        q("Git","Quelle commande montre l'état du working directory et de la staging area ?",0,
            "git status indique notamment les fichiers modifiés, staged et non suivis.",
            "git status","git log","git push","git config"),
        q("Git","Que fait git add README.md ?",1,
            "git add prépare l'état du fichier pour le prochain commit.",
            "Pousse le fichier sur GitHub","Place son état actuel dans la staging area","Crée automatiquement un commit","Supprime le fichier"),
        q("Git","Quelle commande crée un commit avec un message directement dans la commande ?",0,
            "L'option -m fournit le message du commit.",
            "git commit -m \"message\"","git add -m \"message\"","git push -m \"message\"","git status -m \"message\""),
        q("Git","Quel ordre correspond au cycle de travail recommandé après modification ?",1,
            "Le cycle présenté dans les supports est : status, add, commit, puis push.",
            "git push → git init → git add","git status → git add → git commit → git push","git commit → git status → git clone","git remote → git init → git pull"),
        q("Git","Quelle commande affiche les remotes et leurs URL de fetch/push ?",0,
            "git remote -v permet de vérifier vers quel dépôt un push sera envoyé.",
            "git remote -v","git log -p","git status -s","git branch -a"),
        q("Branches","Dans Git, qu'est-ce qu'une branche ?",1,
            "Une branche est un pointeur nommé vers un commit.",
            "Un dossier contenant des commits","Un pointeur nommé vers un commit","Une copie complète du dépôt","Un fichier de configuration"),
        q("Branches","Que représente généralement HEAD ?",1,
            "HEAD indique la position courante et pointe généralement vers la branche active.",
            "Le dépôt distant GitHub","La branche ou position actuellement checkoutée","Le premier commit du dépôt","La staging area"),
        q("Branches","Quelle commande crée la branche cheese puis bascule dessus ?",2,
            "git switch -c crée la branche au commit courant puis place HEAD dessus.",
            "git switch cheese","git branch -d cheese","git switch -c cheese","git push cheese"),
        q("Branches","Quelle commande compare le working directory à la staging area ?",0,
            "git diff montre les changements non staged.",
            "git diff","git diff --staged","git log --graph","git status -s"),
        q("Branches","Quelle commande montre ce qui est déjà préparé pour le prochain commit ?",0,
            "git diff --staged compare la staging area à HEAD.",
            "git diff --staged","git branch -a","git clone","git fetch --all"),
        q("Branches","git push origin cheese fait quoi ?",1,
            "Le push publie la branche. La fusion dans main est une opération distincte.",
            "Fusionne cheese dans main","Publie la branche cheese sur origin","Supprime cheese localement","Crée automatiquement une Pull Request"),
        q("Branches","Quelle commande supprime prudemment une branche locale déjà intégrée ?",0,
            "git branch -d supprime le pointeur de branche local.",
            "git branch -d cheese","git push origin --delete cheese","git rm cheese","git prune cheese"),
        q("Branches","Quelle commande supprime réellement une branche cheese sur le remote origin ?",1,
            "git push origin --delete cheese demande au remote de supprimer cette branche.",
            "git branch -d cheese","git push origin --delete cheese","git fetch --prune cheese","git switch -d cheese"),
        q("SSH","Quelle commande crée une paire de clés SSH Ed25519 ?",0,
            "ssh-keygen -t ed25519 crée une paire de clés privée et publique.",
            "ssh-keygen -t ed25519 -C \"email\"","ssh-add -t ed25519","git keygen ed25519","ssh -T ed25519"),
        q("SSH","Quel fichier peut être copié dans GitHub > SSH and GPG keys ?",1,
            "Le fichier .pub contient la clé publique. La clé privée ne doit pas être partagée.",
            "~/.ssh/id_ed25519","~/.ssh/id_ed25519.pub","~/.ssh/known_hosts uniquement","~/.gitconfig"),
        q("SSH","Quelle commande teste l'authentification SSH auprès de GitHub ?",0,
            "ssh -T git@github.com permet de tester l'authentification SSH.",
            "ssh -T git@github.com","git status github.com","ssh-add github.com","git remote -T"),
        q("Synchronisation","Que fait git pull --rebase origin main dans le mémo fourni ?",1,
            "Cette commande récupère les commits distants puis rejoue les commits locaux au-dessus.",
            "Supprime main puis la recrée","Récupère les commits distants puis rejoue les commits locaux par-dessus","Force le dépôt distant à accepter le local","Change l'URL du remote"),
        q("Conflits","Un conflit Git signifie-t-il forcément que Git a crashé ?",1,
            "Git s'arrête volontairement lorsqu'il ne peut pas décider seul.",
            "Oui, Git doit être réinstallé","Non, Git s'arrête car une décision humaine est nécessaire","Oui, le dépôt est détruit","Non, mais Git choisit toujours la version distante"),
        q("Conflits","Dans les marqueurs de conflit, que signifie <<<<<<< HEAD ?",0,
            "HEAD introduit la version 'ours', celle de la branche actuellement checkoutée.",
            "Début de la version de la branche actuellement checkoutée","Début de la version distante uniquement","Fin du conflit","Un commentaire sans effet"),
        q("Conflits","Pendant un conflit, que fait git add README.md après résolution ?",1,
            "git add enregistre la décision de résolution dans l'index ; il ne juge pas sa qualité.",
            "Vérifie que le contenu choisi est sémantiquement correct","Marque le fichier comme résolu dans l'index","Annule automatiquement le merge","Pousse directement la résolution sur GitHub")
    );

    private static Question q(String category, String text, int correctIndex, String explanation, String... options) {
        return new Question(category, text, correctIndex, explanation, options);
    }

    private List<Question> quiz = new ArrayList<>();
    private int currentIndex = 0;
    private int score = 0;
    private boolean answered = false;

    private TextView categoryView;
    private TextView progressView;
    private TextView questionView;
    private TextView feedbackView;
    private Button nextButton;
    private final List<Button> optionButtons = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        restartQuiz();
    }

    private void restartQuiz() {
        quiz = new ArrayList<>(questions);
        Collections.shuffle(quiz);
        currentIndex = 0;
        score = 0;
        showQuiz();
    }

    private void showQuiz() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(32));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Quiz Ubuntu & Git");
        title.setTextSize(28f);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        root.addView(title);

        categoryView = new TextView(this);
        categoryView.setTextSize(16f);
        categoryView.setPadding(0, dp(12), 0, dp(4));
        root.addView(categoryView);

        progressView = new TextView(this);
        progressView.setTextSize(14f);
        progressView.setPadding(0, 0, 0, dp(18));
        root.addView(progressView);

        questionView = new TextView(this);
        questionView.setTextSize(22f);
        questionView.setTypeface(questionView.getTypeface(), Typeface.BOLD);
        questionView.setPadding(0, 0, 0, dp(18));
        root.addView(questionView);

        optionButtons.clear();
        for (int i = 0; i < 4; i++) {
            final int answerIndex = i;
            Button button = new Button(this);
            button.setAllCaps(false);
            button.setTextSize(16f);
            button.setOnClickListener(v -> answer(answerIndex));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = dp(10);
            root.addView(button, params);
            optionButtons.add(button);
        }

        feedbackView = new TextView(this);
        feedbackView.setTextSize(16f);
        feedbackView.setVisibility(View.GONE);
        feedbackView.setPadding(0, dp(10), 0, dp(10));
        root.addView(feedbackView);

        nextButton = new Button(this);
        nextButton.setText("Question suivante");
        nextButton.setAllCaps(false);
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
        progressView.setText("Question " + (currentIndex + 1) + " / " + quiz.size() + "    •    Score : " + score);
        questionView.setText(q.text);
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
        if (isCorrect) score++;

        for (Button button : optionButtons) {
            button.setEnabled(false);
        }

        if (isCorrect) {
            feedbackView.setText("✓ Bonne réponse\n\n" + q.explanation);
        } else {
            feedbackView.setText("✗ Mauvaise réponse\n\nBonne réponse : " +
                    q.options.get(q.correctIndex) + "\n\n" + q.explanation);
        }

        feedbackView.setVisibility(View.VISIBLE);
        nextButton.setText(currentIndex == quiz.size() - 1 ? "Voir le résultat" : "Question suivante");
        nextButton.setVisibility(View.VISIBLE);
        progressView.setText("Question " + (currentIndex + 1) + " / " + quiz.size() + "    •    Score : " + score);
    }

    private void nextQuestion() {
        if (currentIndex < quiz.size() - 1) {
            currentIndex++;
            renderQuestion();
        } else {
            showResult();
        }
    }

    private void showResult() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(60), dp(24), dp(40));

        TextView title = new TextView(this);
        title.setText("Résultat");
        title.setTextSize(32f);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        root.addView(title);

        TextView result = new TextView(this);
        result.setText(score + " / " + quiz.size());
        result.setTextSize(36f);
        result.setTypeface(result.getTypeface(), Typeface.BOLD);
        result.setPadding(0, dp(28), 0, dp(12));
        root.addView(result);

        int percent = (score * 100) / quiz.size();
        String message;
        if (percent >= 90) {
            message = "Excellent. Tu maîtrises très bien ces commandes.";
        } else if (percent >= 70) {
            message = "Très bien. Encore quelques révisions et ce sera solide.";
        } else if (percent >= 50) {
            message = "Bon début. Rejoue pour renforcer les points faibles.";
        } else {
            message = "Continue à pratiquer. Le quiz change d'ordre à chaque partie.";
        }

        TextView messageView = new TextView(this);
        messageView.setText(message);
        messageView.setTextSize(18f);
        messageView.setGravity(Gravity.CENTER);
        messageView.setPadding(0, 0, 0, dp(28));
        root.addView(messageView);

        Button restart = new Button(this);
        restart.setText("Recommencer");
        restart.setAllCaps(false);
        restart.setOnClickListener(v -> restartQuiz());
        root.addView(restart);

        setContentView(root);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
