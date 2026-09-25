package com.ghiles.quizubuntu

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

data class Question(
    val category: String,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

class MainActivity : Activity() {

    private val questions = listOf(
        Question(
            "Bash",
            "Quelle commande affiche le chemin absolu du dossier courant ?",
            listOf("pwd", "ls", "cd", "mkdir"),
            0,
            "pwd signifie Print Working Directory et affiche le chemin courant."
        ),
        Question(
            "Bash",
            "Quelle commande liste le contenu visible du dossier courant ?",
            listOf("pwd", "ls", "cat", "history"),
            1,
            "ls affiche les fichiers et dossiers visibles du répertoire courant."
        ),
        Question(
            "Bash",
            "Quelle commande affiche aussi les fichiers cachés avec des détails ?",
            listOf("ls -la", "ls -r", "pwd -a", "cat -la"),
            0,
            "ls -la combine l'affichage détaillé (-l) et les fichiers cachés (-a)."
        ),
        Question(
            "Bash",
            "Quelle commande te ramène dans ton dossier personnel ?",
            listOf("cd /", "cd .", "cd ~", "cd .."),
            2,
            "~ représente le dossier personnel de l'utilisateur."
        ),
        Question(
            "Bash",
            "Quelle commande remonte d'un niveau dans l'arborescence ?",
            listOf("cd ..", "cd .", "cd ~", "pwd"),
            0,
            ".. représente le dossier parent."
        ),
        Question(
            "Bash",
            "Comment créer les dossiers imbriqués a/b/c en une seule commande ?",
            listOf("mkdir a/b/c", "mkdir -p a/b/c", "touch a/b/c", "cp -r a/b/c"),
            1,
            "mkdir -p crée les répertoires parents nécessaires."
        ),
        Question(
            "Bash",
            "Quelle commande crée un fichier vide nommé test.txt ?",
            listOf("touch test.txt", "mkdir test.txt", "cat test.txt", "nano -r test.txt"),
            0,
            "touch permet de créer un fichier vide."
        ),
        Question(
            "Bash",
            "Que fait > dans : echo \"test\" > fichier.txt ?",
            listOf(
                "Ajoute le texte à la fin",
                "Crée ou remplace le contenu",
                "Supprime le fichier",
                "Affiche seulement le fichier"
            ),
            1,
            "> redirige la sortie et crée ou remplace le contenu du fichier."
        ),
        Question(
            "Bash",
            "Que fait >> dans : echo \"suite\" >> fichier.txt ?",
            listOf(
                "Ajoute le texte à la fin",
                "Remplace tout le fichier",
                "Renomme le fichier",
                "Crée un dossier"
            ),
            0,
            ">> ajoute la sortie à la fin du fichier existant."
        ),
        Question(
            "Bash",
            "Quelle commande lit le contenu d'un fichier texte ?",
            listOf("cat fichier.txt", "mv fichier.txt", "mkdir fichier.txt", "clear fichier.txt"),
            0,
            "cat affiche le contenu d'un fichier dans le terminal."
        ),
        Question(
            "Bash",
            "Quelle commande supprime un dossier et tout son contenu ?",
            listOf("rm dossier", "rmdir dossier", "rm -r dossier", "clear dossier"),
            2,
            "rm -r réalise une suppression récursive."
        ),
        Question(
            "Git",
            "Quelle commande initialise un dépôt Git dans le dossier courant ?",
            listOf("git init", "git clone", "git add .", "git status"),
            0,
            "git init crée les métadonnées Git du dépôt local."
        ),
        Question(
            "Git",
            "Quelle commande montre l'état du working directory et de la staging area ?",
            listOf("git status", "git log", "git push", "git config"),
            0,
            "git status indique notamment les fichiers modifiés, staged et non suivis."
        ),
        Question(
            "Git",
            "Que fait git add README.md ?",
            listOf(
                "Pousse le fichier sur GitHub",
                "Place son état actuel dans la staging area",
                "Crée automatiquement un commit",
                "Supprime le fichier"
            ),
            1,
            "git add prépare l'état du fichier pour le prochain commit."
        ),
        Question(
            "Git",
            "Quelle commande crée un commit avec un message directement dans la commande ?",
            listOf(
                "git commit -m \"message\"",
                "git add -m \"message\"",
                "git push -m \"message\"",
                "git status -m \"message\""
            ),
            0,
            "L'option -m fournit le message du commit."
        ),
        Question(
            "Git",
            "Quel ordre correspond au cycle de travail recommandé après modification ?",
            listOf(
                "git push → git init → git add",
                "git status → git add → git commit → git push",
                "git commit → git status → git clone",
                "git remote → git init → git pull"
            ),
            1,
            "Le cycle présenté dans les supports est : status, add, commit, puis push."
        ),
        Question(
            "Git",
            "Quelle commande affiche les remotes et leurs URL de fetch/push ?",
            listOf("git remote -v", "git log -p", "git status -s", "git branch -a"),
            0,
            "git remote -v permet de vérifier vers quel dépôt un push sera envoyé."
        ),
        Question(
            "Branches",
            "Dans Git, qu'est-ce qu'une branche ?",
            listOf(
                "Un dossier contenant des commits",
                "Un pointeur nommé vers un commit",
                "Une copie complète du dépôt",
                "Un fichier de configuration"
            ),
            1,
            "Une branche est un pointeur nommé vers un commit."
        ),
        Question(
            "Branches",
            "Que représente généralement HEAD ?",
            listOf(
                "Le dépôt distant GitHub",
                "La branche ou position actuellement checkoutée",
                "Le premier commit du dépôt",
                "La staging area"
            ),
            1,
            "HEAD indique la position courante et pointe généralement vers la branche active."
        ),
        Question(
            "Branches",
            "Quelle commande crée la branche cheese puis bascule dessus ?",
            listOf("git switch cheese", "git branch -d cheese", "git switch -c cheese", "git push cheese"),
            2,
            "git switch -c crée la branche au commit courant puis place HEAD dessus."
        ),
        Question(
            "Branches",
            "Quelle commande compare le working directory à la staging area ?",
            listOf("git diff", "git diff --staged", "git log --graph", "git status -s"),
            0,
            "git diff montre les changements non staged."
        ),
        Question(
            "Branches",
            "Quelle commande montre ce qui est déjà préparé pour le prochain commit ?",
            listOf("git diff --staged", "git branch -a", "git clone", "git fetch --all"),
            0,
            "git diff --staged compare la staging area à HEAD."
        ),
        Question(
            "Branches",
            "git push origin cheese fait quoi ?",
            listOf(
                "Fusionne cheese dans main",
                "Publie la branche cheese sur origin",
                "Supprime cheese localement",
                "Crée automatiquement une Pull Request"
            ),
            1,
            "Le push publie la branche. La fusion dans main est une opération distincte."
        ),
        Question(
            "Branches",
            "Quelle commande supprime prudemment une branche locale déjà intégrée ?",
            listOf("git branch -d cheese", "git push origin --delete cheese", "git rm cheese", "git prune cheese"),
            0,
            "git branch -d supprime le pointeur de branche local."
        ),
        Question(
            "Branches",
            "Quelle commande supprime réellement une branche cheese sur le remote origin ?",
            listOf(
                "git branch -d cheese",
                "git push origin --delete cheese",
                "git fetch --prune cheese",
                "git switch -d cheese"
            ),
            1,
            "git push origin --delete cheese demande au remote de supprimer cette branche."
        ),
        Question(
            "SSH",
            "Quelle commande crée une paire de clés SSH Ed25519 ?",
            listOf(
                "ssh-keygen -t ed25519 -C \"email\"",
                "ssh-add -t ed25519",
                "git keygen ed25519",
                "ssh -T ed25519"
            ),
            0,
            "ssh-keygen -t ed25519 crée une paire de clés privée et publique."
        ),
        Question(
            "SSH",
            "Quel fichier peut être copié dans GitHub > SSH and GPG keys ?",
            listOf(
                "~/.ssh/id_ed25519",
                "~/.ssh/id_ed25519.pub",
                "~/.ssh/known_hosts uniquement",
                "~/.gitconfig"
            ),
            1,
            "Le fichier .pub contient la clé publique. La clé privée ne doit pas être partagée."
        ),
        Question(
            "SSH",
            "Quelle commande teste l'authentification SSH auprès de GitHub ?",
            listOf("ssh -T git@github.com", "git status github.com", "ssh-add github.com", "git remote -T"),
            0,
            "ssh -T git@github.com permet de tester l'authentification SSH."
        ),
        Question(
            "Synchronisation",
            "Que fait git pull --rebase origin main dans le mémo fourni ?",
            listOf(
                "Supprime main puis la recrée",
                "Récupère les commits distants puis rejoue les commits locaux par-dessus",
                "Force le dépôt distant à accepter le local",
                "Change l'URL du remote"
            ),
            1,
            "Cette commande récupère les commits distants puis rejoue les commits locaux au-dessus."
        ),
        Question(
            "Conflits",
            "Un conflit Git signifie-t-il forcément que Git a crashé ?",
            listOf(
                "Oui, Git doit être réinstallé",
                "Non, Git s'arrête car une décision humaine est nécessaire",
                "Oui, le dépôt est détruit",
                "Non, mais Git choisit toujours la version distante"
            ),
            1,
            "Les supports expliquent que Git s'arrête volontairement lorsqu'il ne peut pas décider seul."
        ),
        Question(
            "Conflits",
            "Dans les marqueurs de conflit, que signifie <<<<<<< HEAD ?",
            listOf(
                "Début de la version de la branche actuellement checkoutée",
                "Début de la version distante uniquement",
                "Fin du conflit",
                "Un commentaire sans effet"
            ),
            0,
            "HEAD introduit la version 'ours', celle de la branche actuellement checkoutée."
        ),
        Question(
            "Conflits",
            "Pendant un conflit, que fait git add README.md après résolution ?",
            listOf(
                "Vérifie que le contenu choisi est sémantiquement correct",
                "Marque le fichier comme résolu dans l'index",
                "Annule automatiquement le merge",
                "Pousse directement la résolution sur GitHub"
            ),
            1,
            "git add enregistre la décision de résolution dans l'index ; il ne juge pas sa qualité."
        )
    )

    private var quiz = questions.shuffled()
    private var currentIndex = 0
    private var score = 0
    private var answered = false

    private lateinit var categoryView: TextView
    private lateinit var progressView: TextView
    private lateinit var questionView: TextView
    private lateinit var feedbackView: TextView
    private lateinit var nextButton: Button
    private val optionButtons = mutableListOf<Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showQuiz()
    }

    private fun showQuiz() {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(32))
        }
        scroll.addView(root)

        val title = TextView(this).apply {
            text = "Quiz Ubuntu & Git"
            textSize = 28f
            setTypeface(typeface, Typeface.BOLD)
        }
        root.addView(title)

        categoryView = TextView(this).apply {
            textSize = 16f
            setPadding(0, dp(12), 0, dp(4))
        }
        root.addView(categoryView)

        progressView = TextView(this).apply {
            textSize = 14f
            setPadding(0, 0, 0, dp(18))
        }
        root.addView(progressView)

        questionView = TextView(this).apply {
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(18))
        }
        root.addView(questionView)

        optionButtons.clear()
        repeat(4) { index ->
            val button = Button(this).apply {
                isAllCaps = false
                textSize = 16f
                setOnClickListener { answer(index) }
            }
            root.addView(
                button,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(10) }
            )
            optionButtons.add(button)
        }

        feedbackView = TextView(this).apply {
            textSize = 16f
            visibility = View.GONE
            setPadding(0, dp(10), 0, dp(10))
        }
        root.addView(feedbackView)

        nextButton = Button(this).apply {
            text = "Question suivante"
            isAllCaps = false
            visibility = View.GONE
            setOnClickListener { nextQuestion() }
        }
        root.addView(nextButton)

        setContentView(scroll)
        renderQuestion()
    }

    private fun renderQuestion() {
        val q = quiz[currentIndex]
        answered = false

        categoryView.text = "Catégorie : ${q.category}"
        progressView.text = "Question ${currentIndex + 1} / ${quiz.size}    •    Score : ${score}"
        questionView.text = q.text
        feedbackView.visibility = View.GONE
        nextButton.visibility = View.GONE

        optionButtons.forEachIndexed { index, button ->
            button.text = q.options[index]
            button.isEnabled = true
        }
    }

    private fun answer(index: Int) {
        if (answered) return
        answered = true

        val q = quiz[currentIndex]
        val isCorrect = index == q.correctIndex
        if (isCorrect) score++

        optionButtons.forEach { it.isEnabled = false }

        feedbackView.text = if (isCorrect) {
            "✓ Bonne réponse\n\n${q.explanation}"
        } else {
            "✗ Mauvaise réponse\n\nBonne réponse : ${q.options[q.correctIndex]}\n\n${q.explanation}"
        }
        feedbackView.visibility = View.VISIBLE
        nextButton.text = if (currentIndex == quiz.lastIndex) "Voir le résultat" else "Question suivante"
        nextButton.visibility = View.VISIBLE
        progressView.text = "Question ${currentIndex + 1} / ${quiz.size}    •    Score : ${score}"
    }

    private fun nextQuestion() {
        if (currentIndex < quiz.lastIndex) {
            currentIndex++
            renderQuestion()
        } else {
            showResult()
        }
    }

    private fun showResult() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(60), dp(24), dp(40))
        }

        val title = TextView(this).apply {
            text = "Résultat"
            textSize = 32f
            setTypeface(typeface, Typeface.BOLD)
        }
        root.addView(title)

        val result = TextView(this).apply {
            text = "$score / ${quiz.size}"
            textSize = 36f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(28), 0, dp(12))
        }
        root.addView(result)

        val percent = (score * 100) / quiz.size
        val message = TextView(this).apply {
            text = when {
                percent >= 90 -> "Excellent. Tu maîtrises très bien ces commandes."
                percent >= 70 -> "Très bien. Encore quelques révisions et ce sera solide."
                percent >= 50 -> "Bon début. Rejoue pour renforcer les points faibles."
                else -> "Continue à pratiquer. Le quiz change d'ordre à chaque partie."
            }
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(28))
        }
        root.addView(message)

        val restart = Button(this).apply {
            text = "Recommencer"
            isAllCaps = false
            setOnClickListener {
                quiz = questions.shuffled()
                currentIndex = 0
                score = 0
                showQuiz()
            }
        }
        root.addView(restart)

        setContentView(root)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
