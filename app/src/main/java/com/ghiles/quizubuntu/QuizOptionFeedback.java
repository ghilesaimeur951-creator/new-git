package com.ghiles.quizubuntu;

/** Explains each distractor after grading, never on the question screen. */
final class QuizOptionFeedback {
    private QuizOptionFeedback() { }

    static String explain(MainActivity.Question q,String option) {
        String t=q.text;
        if (option.equals("Aucune de ces réponses"))
            return "La bonne réponse « "+QuizTeaching.answer(q)+" » figure bien parmi les propositions : « aucune » ne convient pas ici.";
        if (t.startsWith("Dans /etc/passwd, x")) {
            if(option.equals("Oui"))return "Le x n’est pas un mot de passe lisible : /etc/passwd peut être consulté par d’autres utilisateurs. Les données protégées sont dans /etc/shadow.";
            if(option.contains("compte supprimé"))return "Ce x est un repère dans le deuxième champ ; il ne dit pas que le compte alice a été supprimé.";
        }
        if (t.startsWith("Un dossier a r mais pas x")) {
            if(option.contains("cd"))return "Entrer dans un dossier avec cd exige x sur ce dossier ; r seul permet d’obtenir la liste des noms.";
            if(option.contains("contenu de chaque fichier"))return "Les droits du dossier ne donnent pas automatiquement le droit de lire les fichiers qu’il contient.";
        }
        if (t.startsWith("Piège : laquelle modifie les permissions")) {
            if(option.startsWith("chmod"))return "chmod vise bien les droits, mais sans -R il modifie seulement le dossier donné, pas tous ses descendants.";
            if(option.startsWith("chown"))return "chown change le propriétaire ; -R étend cette opération aux descendants, mais ne change pas leurs droits rwx.";
        }
        if (t.startsWith("Piège : quel mécanisme empêche")) {
            if(option.contains("SGID"))return "SGID peut transmettre le groupe d’un dossier aux nouveaux fichiers ; il ne protège pas à lui seul contre la suppression.";
            if(option.contains("umask"))return "umask règle les droits au moment de créer un fichier ; il ne protège pas, à lui seul, les fichiers des autres dans un dossier partagé.";
        }
        if (t.contains("groupes secondaires") && option.startsWith("usermod -G"))
            return "-G seul remplace la liste des groupes secondaires. Il faut aussi -a pour conserver les groupes déjà présents.";
        if (t.contains("clé publique") && option.contains("id_ed25519") && !option.contains(".pub"))
            return "Le fichier sans .pub contient la clé privée : elle doit rester sur ton appareil. La clé à copier se termine par .pub.";
        if (t.contains("empreinte") && option.contains("SHA256"))
            return "Une empreinte SHA256 est seulement un résumé pour reconnaître la clé ; GitHub attend le contenu de la clé publique.";

        String meaning=commandMeaning(option);
        if(!meaning.isEmpty())return meaning+" Cela ne réalise pas exactement l’action demandée ici : « "+t+" ».";
        return "Cette proposition affirme « "+option+" ». Ici, cela ne marche pas : "+QuizTeaching.reason(q)+
            " La réponse qui respecte cette règle est « "+QuizTeaching.answer(q)+" ».";
    }

    private static String commandMeaning(String raw) {
        String s=raw.toLowerCase(java.util.Locale.ROOT).trim();
        if(s.startsWith("git push"))return "git push envoie des commits ou une branche vers le dépôt distant ; il ne crée pas de commit local ni de fusion à lui seul.";
        if(s.startsWith("git pull"))return "git pull récupère des versions distantes puis les intègre dans la branche locale.";
        if(s.startsWith("git fetch"))return "git fetch récupère des informations distantes sans les intégrer directement à tes fichiers de travail.";
        if(s.startsWith("git add"))return "git add prépare une version de fichier pour le prochain commit ; il ne la publie pas.";
        if(s.startsWith("git commit"))return "git commit enregistre localement les changements déjà préparés ; il ne les envoie pas sur GitHub.";
        if(s.startsWith("git status"))return "git status décrit l’état local, sans modifier ni envoyer les fichiers.";
        if(s.startsWith("git diff --staged"))return "git diff --staged compare les changements préparés avec le dernier commit.";
        if(s.startsWith("git diff"))return "git diff montre les changements du dossier de travail qui ne sont pas encore préparés.";
        if(s.startsWith("git remote"))return "git remote concerne l’adresse du dépôt distant ; il ne modifie pas le contenu des fichiers.";
        if(s.startsWith("git log"))return "git log affiche les commits déjà enregistrés ; il ne crée pas de nouvelle version.";
        if(s.startsWith("git switch -c"))return "git switch -c crée une nouvelle branche et la sélectionne.";
        if(s.startsWith("git switch"))return "git switch change la branche active sans envoyer son contenu.";
        if(s.startsWith("git branch -d"))return "git branch -d retire une branche locale ; il ne supprime pas la branche sur GitHub.";
        if(s.startsWith("git init"))return "git init initialise le suivi Git local ; il ne récupère ni n’envoie un dépôt distant.";
        if(s.startsWith("chmod"))return "chmod change les droits sur le fichier ou dossier indiqué ; sans -R, il n’agit pas sur tous ses descendants.";
        if(s.startsWith("chown"))return "chown change le propriétaire et éventuellement le groupe, pas les lettres de droits rwx.";
        if(s.startsWith("chgrp"))return "chgrp change uniquement le groupe propriétaire du fichier.";
        if(s.startsWith("mkdir"))return "mkdir crée un dossier ; cela n’affiche pas le chemin courant et ne crée pas un fichier texte.";
        if(s.startsWith("touch"))return "touch crée un fichier vide s’il manque ; il ne crée pas un dossier.";
        if(s.startsWith("rmdir"))return "rmdir supprime un dossier vide uniquement.";
        if(s.startsWith("rm"))return "rm supprime un fichier ; l’option -r est nécessaire pour descendre dans un dossier.";
        if(s.startsWith("cat"))return "cat affiche le contenu du fichier cité ; il ne change pas le dossier courant.";
        if(s.startsWith("ls"))return "ls affiche des noms de fichiers et dossiers ; il ne montre pas le chemin absolu du dossier courant.";
        if(s.startsWith("cd"))return "cd change le dossier courant ; il n’en affiche pas directement le chemin.";
        if(s.startsWith("pwd"))return "pwd affiche le chemin du dossier courant ; il ne liste pas son contenu.";
        if(s.startsWith("clear"))return "clear nettoie l’écran du terminal ; il ne modifie ni ne lit un fichier.";
        if(s.startsWith("ssh-add"))return "ssh-add charge une clé privée dans un agent SSH ; git add prépare des fichiers pour Git.";
        if(s.startsWith("vboxmanage"))return "VBoxManage agit sur une machine virtuelle ; observe la sous-commande pour distinguer créer, configurer, attacher et démarrer.";
        return "";
    }
}
