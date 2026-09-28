package com.ghiles.quizubuntu;

/** What a first-time learner needs to know before attempting a question. */
final class QuizPrimer {
    final String context;
    final String command;
    final String output;
    final String focus;
    final String question;

    private QuizPrimer(String context, String command, String output, String focus, String question) {
        this.context=context; this.command=command; this.output=output; this.focus=focus; this.question=question;
    }

    static QuizPrimer forQuestion(MainActivity.Question q) {
        String t=q.text;
        String context=chapter(q.category)+"\n"+vocabulary(t,q.category);
        String command="",output="",focus="",question=t;

        if (t.startsWith("Dans /etc/passwd, x")) {
            context="Linux conserve une liste de comptes dans le fichier /etc/passwd. Le chemin /etc/passwd désigne un FICHIER, pas une commande. cat est une commande qui affiche le contenu d’un fichier. Les deux-points séparent les champs d’une ligne : le premier donne le nom du compte, le deuxième contient ici x. Les ... raccourcissent cet exemple ; ce n’est pas un mot à taper. /home/alice est le dossier personnel d’alice et /bin/bash le programme de son terminal. /etc/shadow est un autre fichier protégé lié aux mots de passe.";
            command="cat /etc/passwd";
            output="alice:x:1000:1000:...:/home/alice:/bin/bash";
            focus="Le x placé juste après « alice: » et avant le deuxième « : ». Il est dans la SORTIE de cat, pas dans la commande.";
            question="Dans cette ligne de compte, que signifie précisément ce x du deuxième champ ?";
        } else if (t.startsWith("Dans -rw-r--r--")) {
            context="ls -l affiche une ligne par fichier. Dans la sortie, le premier caractère indique le type (- signifie fichier ordinaire). Les neuf caractères suivants sont répartis en trois groupes de trois : propriétaire, groupe, autres. Dans chaque groupe, r veut dire lire, w écrire, x exécuter et - indique qu’un droit manque. notes.txt est le FICHIER examiné.";
            command="ls -l notes.txt";
            output="-rw-r--r-- 1 ubuntu ubuntu 0 notes.txt";
            focus="Les trois caractères du milieu, r--, juste après rw-. Compte uniquement les w dans CE groupe.";
            question="Dans la SORTIE ci-dessus, combien de droits d’écriture possède le groupe : 0 ou 1 ?";
        } else if (t.startsWith("Le fichier notes.txt contient bonjour")) {
            context="notes.txt est un FICHIER texte. echo envoie un mot vers la sortie ; >> ajoute cette sortie à la fin du fichier. cat affiche ensuite le fichier. Dans la sortie montrée ici, la dernière ligne est volontairement cachée pour te laisser chercher.";
            command="echo suite >> notes.txt\ncat notes.txt";
            output="bonjour\n[dernière ligne à trouver]";
            focus="La dernière ligne que cat affichera après la ligne bonjour.";
        } else if (t.startsWith("Avec umask 077") || t.startsWith("Avec umask 033")) {
            String mask=t.contains("077")?"077":"033";
            String file=t.contains("077")?"secret.txt":"note.txt";
            context="umask fixe les droits retirés aux NOUVEAUX fichiers. touch crée un fichier vide. ls -l montre ses droits : r = lire, w = écrire, x = exécuter ; un fichier ordinaire peut commencer au maximum à 666 (rw-rw-rw-). Le nombre demandé est la traduction des trois groupes de droits en chiffres (r = 4, w = 2, x = 1).";
            command="umask "+mask+"\ntouch "+file+"\nls -l "+file;
            output="-????????? 1 ubuntu ubuntu 0 "+file+"  ← droits à trouver";
            focus="Les neuf signes ? de la SORTIE de ls -l : calcule les trois chiffres correspondants. Les ? cachent seulement la réponse, pas une vraie sortie du terminal.";
        } else if (t.startsWith("Un binaire whoami appartient")) {
            context="Un binaire est un programme exécutable. mon-whoami est ici le FICHIER programme ; root est le compte administrateur et Wilder est le compte qui lance le programme. SUID est un droit spécial qui peut changer l’identité utilisée PAR le programme pendant son exécution. whoami affiche l’identité effective du programme.";
            command="sudo chown root mon-whoami\nsudo chmod u+s mon-whoami\n./mon-whoami";
            output="[nom de compte à trouver]";
            focus="Le nom que la dernière commande affichera. Les deux premières commandes préparent le fichier ; leur résultat n’est pas la réponse.";
        } else if (t.startsWith("--memory 2048")) {
            context="VirtualBox configure un ordinateur virtuel. Ubuntu10 est le NOM de cette machine, --memory règle sa mémoire vive et 2048 désigne ici des mébioctets (Mio), pas des gigaoctets. Pour convertir, 1024 Mio correspondent à 1 Gio.";
            command="VBoxManage modifyvm Ubuntu10 --memory 2048\nVBoxManage showvminfo Ubuntu10";
            output="Memory size: 2048MB";
            focus="Le nombre 2048 indiqué dans la SORTIE ; convertis-le en Gio, sans confondre MB affiché par VirtualBox et Go.";
        } else if (t.startsWith("Dans le script du support, que signifie ${3:-1}")) {
            context="Un script est un fichier de commandes. ${3:-1} est une expression Bash : $3 désigne la troisième valeur donnée au script ; les caractères :- servent à prévoir une valeur si cette troisième valeur manque ou est vide. num_vms est le nom d’une variable, pas un chemin de fichier.";
            command="printf '%s\\n' \"${2:-5}\"";
            output="5  ← si aucune deuxième valeur n’a été fournie";
            focus="Le rôle du 5 dans cet EXEMPLE avec $2. Applique le même mécanisme à ${3:-1}, sans confondre les numéros et la valeur de remplacement.";
        } else if (t.startsWith("Tu es sur cheese et crées un commit")) {
            context="main et cheese sont deux branches : des noms qui pointent vers des versions enregistrées (commits). git switch choisit la branche de travail ; git commit enregistre une nouvelle version sur celle qui est active.";
            command="git switch cheese\ngit commit -m 'Pizza'\ngit branch -v";
            output="  main    [version à comparer]\n* cheese  [nouvelle version]";
            focus="La position de main après le commit sur cheese. L’astérisque indique seulement la branche active.";
        } else if (q.format.equals("Prédire le résultat") || q.format.equals("Lire une sortie")) {
            // A new output question must supply its actual command; fail loudly in
            // tests if a bank adds one without an authored, answer-free preview.
            throw new IllegalArgumentException("Exemple de sortie manquant pour : "+t);
        }

        if (t.startsWith("Dans CMD Windows, liste"))
            context="CMD est le terminal de Windows. Un DOSSIER contient des fichiers ; « dossier courant » veut dire celui où le terminal se trouve maintenant. Dans une autre situation, la commande cd change de dossier. Écris ici la commande Windows qui montre les éléments du dossier courant.";
        if (t.startsWith("Affiche les comptes Linux avec cat"))
            context="Un compte est une identité sur Linux. La liste des comptes est le FICHIER /etc/passwd. cat est la COMMANDE qui montre le contenu d’un fichier ; par exemple, « cat notes.txt » affiche les lignes de notes.txt. À toi de choisir le bon fichier après cat.";
        if (t.startsWith("Quelle commande liste le contenu visible"))
            context="Un DOSSIER contient des fichiers et parfois d’autres dossiers. « Dossier courant » signifie celui où tu te trouves dans le terminal. La question demande une COMMANDE à taper, pas un nom de fichier.";
        if (t.startsWith("Quelle commande affiche aussi les fichiers cachés"))
            context="Sous Linux, un nom qui commence par un point, par exemple .git, désigne un élément caché dans la liste ordinaire. -a demande de les inclure et -l demande les détails. La question demande de combiner ces deux options dans une COMMANDE complète.";
        if (t.startsWith("Un dossier a r mais pas x")) {
            context="Un DOSSIER est comme un couloir. La lettre r permet de demander la liste des noms à l’intérieur ; la lettre x concerne l’accès à un nom ou la traversée. « Avoir r » est donc différent d’avoir aussi x.";
            command="ls -ld testdir";
            output="dr--r--r-- 2 ubuntu ubuntu 4096 testdir";
            focus="Les lettres r-- dans le premier groupe de la SORTIE ; ce sont les droits de l’utilisateur propriétaire sur testdir.";
        }
        if (t.startsWith("Que désigne le chemin « . »"))
            context="Dans un CHEMIN de dossier, un point est un signe spécial. Ici, tu vois ce signe dans une commande ; il n’est pas le nom d’un fichier à créer. Observe quel dossier la commande vise, puis choisis sa signification.";
        if (t.startsWith("Que désigne le chemin « .. »"))
            context="Dans un CHEMIN de dossier, les deux points .. sont un signe spécial. cd est la commande pour changer de dossier. On cherche où l’on arrive quand on utilise ces signes.";

        return new QuizPrimer(context,command,output,focus,question);
    }

    private static String chapter(String category) {
        switch(category) {
            case "Bash": return "Bash est le langage du terminal Linux. Une commande est ce qu’on tape ; une sortie est le texte que le terminal affiche ensuite.";
            case "Git": return "Git enregistre des versions de fichiers. Un fichier est un document du projet ; une commande git est une instruction tapée dans le terminal.";
            case "Branches": return "Une branche Git est un nom attaché à une version enregistrée ; main est souvent la branche principale. Une branche n’est pas un dossier.";
            case "SSH": return "SSH est une manière sécurisée de s’identifier à un service comme GitHub. Une clé publique peut être communiquée ; la clé privée reste sur ton appareil.";
            case "Synchronisation": return "Ton appareil et GitHub ont chacun leur copie des versions. origin est le nom habituel de l’adresse GitHub, main est le nom d’une branche.";
            case "Conflits": return "Un conflit survient quand Git ne peut pas choisir seul entre deux modifications. Les signes dans le fichier montrent où ta décision est nécessaire.";
            case "Utilisateurs": return "Sur Linux, un compte identifie une personne ou un service ; un groupe rassemble des comptes. /etc contient des fichiers de configuration du système.";
            case "Permissions": return "Les droits Linux indiquent qui peut lire (r), écrire (w) ou exécuter (x). Le premier groupe concerne le propriétaire, le deuxième son groupe, le troisième les autres.";
            case "ACL et droits spéciaux": return "Une ACL est une règle de droit ajoutée pour une personne précise. D’autres règles, comme umask ou SUID, changent les droits d’un nouveau fichier ou d’un programme.";
            case "VirtualBox": return "VirtualBox est un programme qui gère des machines virtuelles, c’est-à-dire des ordinateurs représentés dans un autre ordinateur. VBoxManage est sa commande de configuration.";
            case "Windows": return "CMD est le terminal de Windows. Une commande y agit sur des fichiers ou dossiers, comme dans le terminal Linux, mais son nom peut changer.";
            default: return "Lis séparément la commande, le fichier visé et la sortie avant de choisir.";
        }
    }

    private static String vocabulary(String text,String category) {
        StringBuilder out=new StringBuilder();
        if (text.contains("/etc/passwd")) out.append(" /etc/passwd est un FICHIER de comptes ; ce chemin n’est pas à taper seul.");
        if (text.contains("/etc/group")) out.append(" /etc/group est un FICHIER décrivant les groupes.");
        if (text.contains("/etc/shadow")) out.append(" /etc/shadow est un FICHIER protégé lié aux mots de passe.");
        if (text.contains(".pub")) out.append(" Un chemin finissant par .pub désigne le FICHIER de clé publique.");
        if (text.contains("README.md")) out.append(" README.md est un FICHIER du projet ; son nom ne désigne pas une commande.");
        if (text.contains(".txt")) out.append(" Les noms finissant par .txt désignent des FICHIERS texte.");
        if (text.contains("dossier personnel") || text.contains("home")) out.append(" Le dossier personnel est l’espace de fichiers propre à un compte, souvent sous /home/nom.");
        if (text.contains("shell")) out.append(" Un shell est le programme qui lit les commandes tapées dans le terminal ; Bash en est un exemple.");
        if (text.contains("UID") || text.contains("GID")) out.append(" UID est le numéro d’un compte ; GID est le numéro d’un groupe.");
        if (text.contains("root")) out.append(" root est le compte administrateur de Linux.");
        if (text.contains(".git")) out.append(" .git est le DOSSIER caché où Git conserve ses données locales.");
        if (text.contains("~")) out.append(" Le signe ~ désigne ton dossier personnel.");
        if (text.contains("--rebase")) out.append(" --rebase est une option, c’est-à-dire un réglage supplémentaire de la commande.");
        if (text.contains(">>") || text.contains(" > ")) out.append(" Les signes > et >> envoient le texte produit par une commande vers un FICHIER ; la question porte sur la différence entre eux.");
        if (text.contains("-p")) out.append(" Une option précédée de - modifie le fonctionnement de la commande ; lis chaque lettre séparément.");
        if (text.contains("-G") || text.contains("-aG")) out.append(" -G indique une liste de groupes supplémentaires ; -a est un réglage à examiner pour savoir si la liste existante est conservée.");
        if (text.contains("rwx") || text.contains("octal")) out.append(" Les lettres r, w et x désignent lecture, écriture et exécution. En notation octale, r vaut 4, w vaut 2 et x vaut 1 pour chaque groupe de personnes.");
        if (text.contains("-R") || text.contains("récursiv")) out.append(" Récursivement veut dire : le dossier et tout ce qu’il contient, y compris les sous-dossiers.");
        if (text.contains("sudo")) out.append(" sudo demande de lancer la commande avec une autorisation supplémentaire si ton compte y a droit.");
        if (text.contains("chmod")) out.append(" chmod est une commande qui modifie les droits ; le nom qui suit désigne le FICHIER ou DOSSIER ciblé.");
        if (text.contains("chown")) out.append(" chown modifie le propriétaire d’un fichier ou dossier.");
        if (text.contains("umask")) out.append(" umask enlève certains droits aux fichiers et dossiers créés ensuite ; il ne change pas les anciens.");
        if (text.contains("origin")) out.append(" origin est un nom court pour l’adresse du dépôt distant, souvent GitHub.");
        if (text.contains("HEAD")) out.append(" HEAD est un nom spécial de Git pour parler de la position actuelle ; cherche dans les choix ce qu’il désigne exactement ici.");
        if (text.contains("remote") || text.contains("distant")) out.append(" Un dépôt distant est une copie accessible par le réseau, par exemple sur GitHub ; un dépôt local est sur ton appareil.");
        if (text.contains("staging") || text.contains("index")) out.append(" L’index (staging area) est la liste des modifications préparées pour le prochain commit.");
        if (text.contains("commit")) out.append(" Un commit est une version enregistrée localement avec un message.");
        if (text.contains("NAT")) out.append(" NAT est un mode réseau pour la machine virtuelle.");
        if (text.contains("VMDK") || text.contains(".vmdk")) out.append(" Un fichier .vmdk représente un disque virtuel, pas une commande.");
        if (text.contains("ISO") || text.contains(".iso")) out.append(" Un fichier .iso contient une image de disque pouvant servir à installer un système.");
        if (text.contains("RAM")) out.append(" La RAM est la mémoire de travail ; elle se mesure ici en Mio ou en Gio.");
        if (text.contains("NTFS")) out.append(" NTFS est un format de disque Windows ; ses droits peuvent être détaillés par des ACL.");
        if (text.contains("SUID")) out.append(" SUID est un droit spécial d’un programme ; regarde quelle identité il utilise pendant son exécution.");
        if (text.contains("SGID")) out.append(" SGID est un droit spécial pouvant faire hériter le groupe d’un dossier.");
        if (text.contains("sticky")) out.append(" Le sticky bit est une protection possible sur un dossier partagé.");
        if (text.contains("ACL")) out.append(" Une ACL précise les droits accordés à une personne ou à un groupe particulier.");
        if (out.length()==0)out.append(" Lis l’action demandée, puis repère si la question parle d’une commande, d’un fichier, d’un dossier ou d’une sortie.");
        return out.toString().trim();
    }
}
