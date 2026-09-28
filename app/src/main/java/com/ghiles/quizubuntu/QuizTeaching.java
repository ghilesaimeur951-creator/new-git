package com.ghiles.quizubuntu;

/** Two layers of help for every question, including questions loaded from the PDF banks. */
final class QuizTeaching {
    private QuizTeaching() { }

    static String answer(MainActivity.Question q) { return q.options.get(q.correctIndex); }

    static String simple(MainActivity.Question q, String response, boolean correct) {
        String lead = correct ? "Oui, c’est ça !" : "Pas encore. Tu as répondu « " + response + " ».";
        String lesson=reason(q);
        return lead + "\n\nLa réponse attendue est « " + answer(q) + " ».\n\n" + lesson;
    }

    static String details(MainActivity.Question q, String response, boolean correct) {
        StringBuilder out = new StringBuilder();
        QuizPrimer primer=QuizPrimer.forQuestion(q);
        out.append("1. Ce que la question te demande\n").append(primer.question).append("\n\n");
        out.append("2. Comment trouver la réponse\n");
        out.append(primer.context).append("\n").append(concept(q.category)).append("\n");
        String specific = permissions(q, true);
        if (!specific.isEmpty()) out.append(specific).append("\n");
        String reason = cleanReason(q.explanation);
        if (specific.isEmpty() && pdfLesson(q.text).isEmpty() && !reason.isEmpty())
            out.append("Pour cette question : ").append(reason).append("\n");
        out.append("\n3. Un exemple à lire pas à pas\n");
        if(!primer.command.isEmpty())out.append("Commande tapée :\n").append(primer.command).append("\n");
        if(!primer.output.isEmpty())out.append("Sortie montrée avant ta réponse :\n").append(primer.output).append("\n");
        if(!primer.focus.isEmpty())out.append("La partie visée : ").append(primer.focus).append("\n");
        out.append("Exemple du cours :\n").append(q.example).append("\n");
        out.append(exampleGuide(q)).append("\n");
        if (!correct) {
            out.append("\n4. Pourquoi revoir ta réponse\n");
            out.append("Ta réponse « ").append(response).append(" » ne répond pas à toute la demande. ");
            out.append("Repère dans l’énoncé les mots qui comptent : ");
            out.append(q.category.equals("Permissions") || q.category.equals("ACL et droits spéciaux")
                ? "qui reçoit les droits, quels droits, et sur quel fichier ou dossier."
                : "l’action, la cible et le résultat demandé.");
            out.append(" La réponse ici est « ").append(answer(q)).append(" ».");
        } else {
            out.append("\n4. Pour vérifier que tu as compris\n");
            out.append("Essaie de dire avec tes mots pourquoi « ").append(answer(q)).append(" » convient, puis relis l’exemple.");
        }
        if (q.accepted != null && q.accepted.length > 1) {
            out.append("\n\nAutres écritures acceptées : ");
            for (int i = 1; i < q.accepted.length; i++) {
                if (i > 1) out.append(" ; ");
                out.append(q.accepted[i]);
            }
            out.append(".");
        }
        if (q.accepted == null && q.options.size() > 1) {
            out.append("\n\nPourquoi chaque choix convient ou non\n");
            for (int i=0;i<q.options.size();i++) {
                String option=q.options.get(i);
                out.append("• ").append(option).append(" : ");
                if(i==q.correctIndex)
                    out.append("c’est le bon choix. ").append(reason(q));
                else out.append(QuizOptionFeedback.explain(q,option));
                out.append("\n");
            }
        }
        out.append("\n\nL’exemple du quiz sert à apprendre : il n’exécute aucune commande sur ton téléphone.");
        return out.toString();
    }

    static String reason(MainActivity.Question q) {
        String specific=permissions(q,false);
        return specific.isEmpty()?plainReason(q):specific;
    }

    private static String plainReason(MainActivity.Question q) {
        String authored = pdfLesson(q.text);
        if (!authored.isEmpty()) return authored;
        String reason = cleanReason(q.explanation);
        if (reason.isEmpty() || reason.equalsIgnoreCase(q.category))
            return concept(q.category) + " Dans l’exemple, on voit « " + q.example.replace('\n', ' ') + " ».";
        return concept(q.category) + "\n\n" + reason;
    }

    // The first PDF bank used source headings as its only explanation. Give its
    // questions an actual reason, with the more detailed source in the second layer.
    private static String pdfLesson(String t) {
        if (t.startsWith("Que désigne le chemin « . »")) return "Le point tout seul veut dire « ici », le dossier où tu te trouves déjà. ls . montre donc son contenu.";
        if (t.startsWith("Que désigne le chemin « .. »")) return "Deux points veulent dire « un étage au-dessus ». cd .. te fait remonter au dossier parent.";
        if (t.startsWith("Quelle commande crée plusieurs niveaux")) return "mkdir crée un dossier. Avec -p, il crée aussi les dossiers parents manquants avant de créer le dernier.";
        if (t.startsWith("Que fait Ctrl+C")) return "Ctrl+C demande d’arrêter la commande qui tourne. Dans le terminal, ce raccourci ne veut pas dire copier.";
        if (t.startsWith("Quelle commande affiche les commandes précédemment")) return "history montre les anciennes commandes, comme un cahier des exercices déjà essayés.";
        if (t.startsWith("Quel est l’effet de > fichier.txt")) return "Le signe > dirige la sortie vers fichier.txt. Même sans texte à envoyer, il crée le fichier s’il manque ou le vide s’il existe.";
        if (t.startsWith("Quelle commande ajoute une ligne sans effacer")) return "Deux chevrons >> ajoutent à la fin. Un seul > remplacerait le texte qui était déjà dans le fichier.";
        if (t.startsWith("Quand rmdir dossier")) return "rmdir supprime un dossier seulement quand il est vide. Pour supprimer aussi son contenu, c’est une autre opération.";
        if (t.startsWith("Quelle commande copie un dossier avec")) return "cp signifie copier ; -r veut dire descendre aussi dans les sous-dossiers et copier leur contenu.";
        if (t.startsWith("Quelle commande vérifie la version de Git")) return "git --version demande à Git son numéro de version ; cela permet aussi de vérifier que Git est disponible.";
        if (t.startsWith("Quelle commande configure le nom utilisé")) return "git config --global user.name enregistre le nom qui sera écrit comme auteur de tes prochains commits.";
        if (t.startsWith("Après git add puis une nouvelle modification")) return "git add prend une photo de la version actuelle pour le prochain commit. Si tu modifies encore le fichier après, il faut refaire git add pour inclure cette nouvelle modification.";
        if (t.startsWith("Quelle commande compare l’index au dernier")) return "L’index contient ce que tu as préparé avec git add. git diff --staged montre ce qui a changé entre cette préparation et le dernier commit.";
        if (t.startsWith("Quelle commande compare le dossier de travail")) return "git diff montre les modifications dans tes fichiers qui n’ont pas encore été préparées par git add.";
        if (t.startsWith("Un git commit normal remplit-il")) return "Un commit normal crée une nouvelle version avec un lien vers la précédente. Il ne remplit pas l’ancienne version.";
        if (t.startsWith("Quelle commande indique les URL des dépôts")) return "git remote -v affiche l’adresse du dépôt distant pour récupérer et envoyer. Lire l’adresse ne prouve pas encore que la connexion fonctionne.";
        if (t.startsWith("Comment afficher l’origine des valeurs")) return "Git peut lire des réglages dans plusieurs fichiers. --show-origin indique quel fichier a fourni chaque valeur.";
        if (t.startsWith("Comment supprimer la valeur globale")) return "--global vise la configuration de ton compte ; --unset retire la valeur user.email, sans effacer les commits déjà faits.";
        if (t.startsWith("Créer une branche copie-t-il")) return "Une branche est une petite étiquette sur un commit déjà présent. La créer ne recopie pas tout l’historique.";
        if (t.startsWith("Après un commit sur cheese")) return "Quand tu travailles sur cheese, son étiquette avance au nouveau commit. L’étiquette main attend là où elle était.";
        if (t.startsWith("Que fait git push origin cheese")) return "Cette commande envoie la branche cheese sur le dépôt distant origin. Elle ne la mélange pas automatiquement avec main.";
        if (t.startsWith("Après une fusion sur GitHub")) return "La fusion a changé main sur GitHub. Reviens sur main sur ton appareil, puis récupère ce changement avec git pull origin main.";
        if (t.startsWith("git branch -d cheese")) return "git branch -d enlève l’étiquette cheese sur ton appareil. Pour la supprimer aussi sur GitHub, il faut une commande distante différente.";
        if (t.startsWith("Quelle commande vérifie les branches réellement")) return "git ls-remote --heads origin demande directement à origin quelles branches il possède, au lieu de regarder seulement tes branches locales.";
        if (t.startsWith("Quel fichier peut être ajouté aux clés SSH")) return "La clé qui finit par .pub est publique : tu peux la donner à GitHub. L’autre est privée et reste sur ton appareil.";
        if (t.startsWith("Une empreinte SHA256 remplace-t-elle")) return "L’empreinte est un résumé qui sert à reconnaître la clé. GitHub a besoin de la vraie clé publique, pas uniquement de son résumé.";
        if (t.startsWith("Quel outil mémorise la clé SSH")) return "ssh-agent garde la clé SSH utilisable pendant la session ; ssh-add lui demande de charger une clé.";
        if (t.startsWith("Quelle commande charge la clé privée")) return "ssh-add charge la clé privée dans ssh-agent. Ce n’est pas git add : celui-ci prépare des fichiers pour un commit.";
        if (t.startsWith("Comment remplacer l’URL d’un remote")) return "origin existe déjà. git remote set-url change son adresse ; git remote add tenterait d’ajouter un second origin.";
        if (t.startsWith("À quoi sert -u lors")) return "Au premier push, -u relie ta branche locale à la branche distante. Ensuite, un simple git push sait où envoyer les commits.";
        if (t.startsWith("Quelles versions sont comparées")) return "Git regarde le point de départ commun, ta version et l’autre version. Ces trois vues l’aident à réunir les changements.";
        if (t.startsWith("Après avoir résolu un conflit")) return "Les lignes <<<<<<<, ======= et >>>>>>> sont des panneaux temporaires. Après avoir choisi le texte final, enlève tous ces panneaux.";
        if (t.startsWith("git add vérifie-t-il")) return "git add enregistre ton choix pour le prochain commit. Il ne sait pas si ton texte est juste : relis-le toi-même.";
        if (t.startsWith("Pourquoi git diff peut-il être vide")) return "Après git add, les changements sont dans l’index. git diff regarde ce qui reste hors de l’index ; utilise git diff --staged pour voir ce qui est déjà préparé.";
        if (t.startsWith("git fetch -a signifie-t-il")) return "Les lettres se ressemblent, mais -a ne veut pas dire « tous les remotes » ici. Pour cela, écris l’option complète --all.";
        if (t.startsWith("Pourquoi un push normal peut-il être refusé")) return "GitHub a des commits que ton appareil ne connaît pas encore. Récupère-les et intègre-les avant de renvoyer tes commits.";
        if (t.startsWith("Que fait git pull --rebase")) return "Git récupère les nouvelles versions de GitHub, puis replace tes commits locaux après elles, dans l’ordre.";
        if (t.startsWith("Quand --allow-unrelated-histories")) return "Deux dépôts créés séparément peuvent n’avoir aucun premier commit en commun. Cette option autorise leur fusion quand c’est bien ce que tu veux.";
        if (t.startsWith("Que vérifie --force-with-lease")) return "Avant de réécrire le distant, Git vérifie qu’il n’a pas avancé depuis la dernière fois que tu l’as vu. Cela reste une opération délicate.";
        if (t.startsWith("Combien de parents possède le merge commit")) return "Un merge relie deux chemins de travail. Son commit de réunion indique donc les deux commits précédents comme parents.";
        return "";
    }

    private static String cleanReason(String raw) {
        String s = raw == null ? "" : raw.trim();
        // PDF labels are useful as sources, but not as the first words a learner sees.
        int dash = s.indexOf(" — ");
        if (dash >= 0) s = s.substring(dash + 3).trim();
        if (raw != null && raw.trim().startsWith("S03 — ")) {
            int second = s.indexOf(" — ");
            if (second >= 0) s = s.substring(second + 3).trim();
        }
        if (s.startsWith("Fiche ") || s.startsWith("Guide ") || s.startsWith("Mémo ")) {
            int colon = s.indexOf(" : ");
            if (colon >= 0) s = s.substring(colon + 3).trim();
        }
        return s;
    }

    private static String concept(String category) {
        switch (category) {
            case "Permissions": return "Sur Linux, les droits sont comme trois trousseaux de clés : propriétaire, groupe, puis autres. r permet de lire, w de modifier et x d’exécuter ou de traverser un dossier.";
            case "ACL et droits spéciaux": return "Les droits rwx sont la règle de base. Une ACL donne une règle supplémentaire à une personne précise ; umask retire des droits aux nouveaux fichiers ; les bits spéciaux changent un comportement particulier.";
            case "Utilisateurs": return "Un compte est une personne identifiée par Linux ; un groupe rassemble plusieurs comptes. Avoir un compte et appartenir à un groupe sont deux choses différentes.";
            case "Bash": return "Le terminal suit ton dossier courant, comme si tu te trouvais dans une pièce. Les commandes permettent de te déplacer, de voir ou de modifier ce qui s’y trouve.";
            case "Git": return "Pense à Git comme à un carnet de versions : tes fichiers sont sur la table, git add choisit ce que tu vas enregistrer, git commit crée la version et git push l’envoie sur GitHub.";
            case "Branches": return "Une branche est une étiquette posée sur une version. Travailler sur une branche ne déplace pas automatiquement l’étiquette main.";
            case "Synchronisation": return "Ton téléphone et GitHub gardent chacun leurs versions. Push envoie tes commits ; fetch récupère les informations distantes ; pull récupère et intègre les commits dans ta branche.";
            case "Conflits": return "Si deux personnes changent la même partie d’un fichier, Git montre les deux versions et te demande de choisir le contenu final.";
            case "SSH": return "SSH utilise deux clés : la clé publique peut être copiée sur GitHub ; la clé privée reste sur ton appareil, comme une clé de maison.";
            case "VirtualBox": return "Une machine virtuelle est un ordinateur représenté dans un autre ordinateur. La créer, régler sa mémoire, lui attacher un disque et la démarrer sont des étapes séparées.";
            case "Windows": return "Sous Windows, un chemin ressemble à C:\\Users\\nom ; sous Linux, à /home/nom. Les commandes changent, mais fichiers et dossiers restent les mêmes idées.";
            default: return "Lis l’énoncé en cherchant l’action à faire, l’objet concerné et le résultat attendu.";
        }
    }

    private static String exampleGuide(MainActivity.Question q) {
        switch (q.category) {
            case "Permissions":
                if(q.example.trim().startsWith("-"))return "Cette ligne vient de ls -l : le premier signe - indique un fichier ; les neuf lettres suivantes forment trois groupes de trois. Lis propriétaire, groupe, autres, puis cherche r, w ou x dans le groupe demandé.";
                if(q.example.trim().startsWith("chmod u=r"))return "La lettre u vise le propriétaire, g le groupe et o les autres. Le signe = fixe exactement les droits de chacun ; un droit absent n’est pas donné.";
                return "Dans la commande, repère le fichier ou dossier à droite, puis les droits et l’action à gauche. Trois chiffres se lisent dans l’ordre propriétaire / groupe / autres.";
            case "ACL et droits spéciaux": return "Repère la commande, la personne ou le groupe concerné, puis les droits accordés. Pour umask, pars des droits possibles et enlève les bits du masque.";
            case "Git": case "Branches": case "Synchronisation": case "Conflits": return "Lis les lignes de haut en bas : la commande vient après le signe $, et les lignes suivantes montrent son résultat. Un fichier modifié reste local tant qu’il n’a pas été ajouté, commité et poussé.";
            default: return "Lis la première ligne comme l’action, puis demande-toi ce qui apparaît ou change ensuite. Reprends l’exemple avec un autre nom pour vérifier que tu sais refaire le geste.";
        }
    }

    private static String permissions(MainActivity.Question q, boolean extended) {
        String t = q.text;
        if (q.category.equals("Permissions")) {
            if (t.startsWith("Dans -rw-r--r--")) return extended ? "Découpe -rw-r--r-- en quatre blocs : - (fichier), rw- (propriétaire), r-- (groupe), r-- (autres). Dans le bloc du groupe, il n’y a pas de w : il possède 0 droit d’écriture." : "Découpe les lettres en groupes de trois. Le groupe a r-- : il peut lire, mais le w de l’écriture manque. Cela fait 0 droit d’écriture.";
            if (t.startsWith("Donne rwx au propriétaire, rx")) return extended ? "u signifie propriétaire, g groupe et o autres. Dans chmod u=rwx,g=rx,o= test.txt, le signe = remplace les anciens droits : u reçoit rwx, g reçoit rx et o ne reçoit rien. Ne confonds pas le signe = avec +, qui ajoute aux droits existants." : "u = propriétaire, g = groupe, o = autres. Écris exactement les droits demandés pour chacun avec = : rwx, rx, puis rien.";
            if (t.startsWith("Donne rwx au propriétaire, r au groupe")) return extended ? "Chaque droit a une valeur : r vaut 4, w vaut 2, x vaut 1. Propriétaire rwx : 4+2+1=7 ; groupe r : 4 ; autres sans droits : 0. Écris les trois nombres dans cet ordre : 740." : "Calcule séparément chaque personne : rwx = 7, r = 4 et rien = 0. Dans l’ordre propriétaire / groupe / autres, cela donne 740.";
            if (t.startsWith("Rends confidentiel.txt")) return extended ? "600 signifie 6 pour le propriétaire, 0 pour le groupe et 0 pour les autres. Le 6 vaut 4+2, donc lecture et écriture, sans exécution. Cela produit rw-------." : "Seul le propriétaire doit lire et écrire : r+w = 4+2 = 6. Les deux autres groupes ont 0, donc 600.";
            if (t.startsWith("Rends partage.txt")) return extended ? "444 signifie 4 pour chacun : lecture seule pour le propriétaire, le groupe et les autres. Aucun de ces trois triplets ne contient w. Le compte root dispose de privilèges particuliers : il ne faut pas dire que la machine entière est incapable de modifier ce fichier." : "Lecture vaut 4. Pour que chacun puisse lire sans recevoir le droit w, mets 4 dans les trois positions : 444.";
            if (t.startsWith("Applique rwxr-xr-x")) return extended ? "Sépare rwxr-xr-x en rwx / r-x / r-x. Le premier vaut 4+2+1=7 ; chaque r-x vaut 4+1=5. On obtient 755, puis le nom du fichier." : "rwx vaut 7. r-x vaut 5. Le propriétaire, le groupe et les autres donnent donc 7-5-5, soit 755.";
            if (t.startsWith("Retire uniquement ton droit x")) return extended ? "chmod u-x testdir enlève uniquement x au propriétaire : u indique qui, - indique qu’on retire, x indique le droit. Sur un dossier, x sert à entrer et à atteindre un fichier par son nom. Sans x, cd testdir échoue pour cet utilisateur ordinaire, même s’il peut encore voir certains noms avec r." : "u représente le propriétaire et -x enlève son droit d’entrer dans ce dossier. Les autres droits ne changent pas.";
            if (t.startsWith("Un dossier a r mais pas x")) return extended ? "Sur un dossier, r permet d’obtenir la liste des noms ; x permet d’entrer et d’atteindre les fichiers. On peut donc connaître un nom sans avoir le droit de lire le fichier ni de faire cd. Imagine la liste des portes d’un couloir sans clé pour franchir la porte." : "r permet de lister les noms. Sans x, tu ne peux pas traverser le dossier ; voir un nom ne garantit pas l’accès au fichier.";
            if (t.startsWith("Avec sudo, affecte test2.txt")) return extended ? "Dans sudo chown root:groupetest test2.txt, chown change le propriétaire, root est le nouveau propriétaire, groupetest est le groupe après les deux-points et test2.txt est le fichier. sudo fournit les droits nécessaires si ton compte y est autorisé." : "Avec chown, écris propriétaire:groupe puis le fichier. Ici : root:groupetest sur test2.txt.";
            if (t.startsWith("Tu es autorisé à le faire : change seulement")) return extended ? "chgrp groupetest test2.txt change uniquement le groupe du fichier. chown root:groupetest modifierait aussi le propriétaire : ce n’est pas la consigne. Si nécessaire, un compte autorisé peut ajouter sudo devant chgrp." : "Il faut changer le groupe sans toucher au propriétaire. chgrp fait exactement cette opération.";
            if (t.startsWith("Avec sudo, applique root:groupetest")) return extended ? "sudo chown -R root:groupetest mondossier applique le nouveau propriétaire et groupe au dossier et à ses descendants. -R veut dire récursif : on descend dans les sous-dossiers. Sans -R, seul mondossier serait modifié." : "-R veut dire « tout ce qu’il y a dedans aussi ». Ajoute-le à chown pour changer le dossier et son contenu.";
            if (t.startsWith("Piège : laquelle modifie les permissions")) return extended ? "chmod touche les permissions rwx ; chown touche le propriétaire. -R descend dans les sous-dossiers et fichiers. Les choix proposés sont chmod sans -R et chown avec -R : aucun ne fait les deux choses requises. La commande voulue serait chmod -R 750 mondossier." : "Il faut chmod pour les droits et -R pour tout le contenu. Aucune proposition ne combine les deux : choisis « Aucune de ces réponses ».";
        }
        if (q.category.equals("ACL et droits spéciaux")) {
            if (t.startsWith("Affiche le masque")) return extended ? "Tape umask sans nombre pour voir le masque actuel. Le masque retire certains droits aux nouveaux fichiers et dossiers ; il ne réécrit pas les droits des fichiers qui existent déjà." : "umask seul affiche les droits qui seront retirés lors des prochaines créations.";
            if (t.startsWith("Fixe le masque")) return extended ? "umask 077 enlève tous les droits au groupe et aux autres pour les prochaines créations. Un fichier ordinaire commence avec au plus 666, donc 600 ; un dossier commence avec au plus 777, donc 700." : "Avec 077, le groupe et les autres n’obtiennent aucun droit sur les nouveaux fichiers et dossiers.";
            if (t.startsWith("Avec umask 077")) return extended ? "Un fichier neuf part de 666 : r et w possibles, sans x. Le masque 077 retire les droits du groupe et des autres. Il reste 600 : le propriétaire peut lire et écrire, les autres n’ont rien." : "Pour un fichier, pars de 666 puis enlève les droits indiqués par 077. Il reste 600.";
            if (t.startsWith("Avec umask 033")) return extended ? "Applique le masque bit par bit : le fichier part de 666. Le propriétaire conserve 6 ; pour le groupe 6 (rw-) privé de 3 (wx) garde seulement r, soit 4 ; même chose pour les autres. Résultat : 644. Une simple soustraction 666 - 033 donnerait une réponse trompeuse." : "Le masque enlève des droits, il ne se soustrait pas comme un nombre ordinaire. 666 avec le masque 033 laisse 644.";
            if (t.startsWith("Ajoute SGID")) return extended ? "chmod g+s partage-equipe ajoute le bit SGID au dossier sans remplacer rwx. Un nouveau fichier créé dedans peut hériter du groupe du dossier. SGID ne protège pas, à lui seul, un fichier contre la suppression." : "g+s ajoute SGID au dossier : les nouveaux éléments prennent son groupe, sans remplacer les droits rwx.";
            if (t.startsWith("Ajoute le sticky bit")) return extended ? "chmod +t partage-equipe place le sticky bit sur le dossier partagé. Même si plusieurs utilisateurs ont le droit d’y écrire, ils ne peuvent normalement pas supprimer le fichier d’un autre ; le propriétaire du fichier, celui du dossier et root gardent ce pouvoir." : "Le sticky bit empêche un utilisateur de supprimer le fichier d’un autre dans un dossier partagé.";
            if (t.startsWith("Ajoute SUID")) return extended ? "chmod u+s mon-whoami ajoute SUID à ce binaire. Lorsqu’on l’exécute, il prend temporairement l’identité effective de son propriétaire. Sur Linux, un script shell n’obtient généralement pas ce comportement." : "u+s ajoute SUID au programme : il agit avec l’identité effective de son propriétaire pendant l’exécution.";
            if (t.startsWith("Retire SUID")) return extended ? "chmod u-s mon-whoami retire seulement le bit SUID du propriétaire. Le - enlève ce droit spécial, sans supprimer le fichier ni modifier les autres droits rwx." : "u-s retire le bit spécial SUID du propriétaire sans enlever les autres permissions.";
            if (t.startsWith("Un binaire whoami appartient")) return extended ? "Le fichier mon-whoami appartient à root et porte SUID. Wilder reste la personne connectée, mais quand il lance ce binaire, son identité effective devient celle du propriétaire du programme : root. C’est cette identité que whoami montre ici." : "SUID donne au programme l’identité effective de son propriétaire. Comme le propriétaire est root, il affiche root.";
            if (t.startsWith("Donne à wilder une ACL")) return extended ? "Dans setfacl -m u:wilder:r fichier.txt, -m ajoute ou modifie une règle ; u:wilder vise la personne wilder ; r donne la lecture. Le masque ACL peut encore limiter le droit effectif. Le propriétaire du fichier ne change pas." : "Une ACL permet de donner la lecture à wilder seul, sans changer le propriétaire du fichier.";
            if (t.startsWith("Affiche les ACL")) return extended ? "getfacl fichier.txt montre les règles ordinaires et les règles ajoutées pour des personnes précises. Lis aussi la ligne mask : elle peut limiter les droits effectifs indiqués ailleurs." : "getfacl affiche les règles ACL et leur masque pour ce fichier.";
            if (t.startsWith("Retire l’entrée ACL")) return extended ? "setfacl -x u:wilder fichier.txt retire l’entrée visant wilder. -x enlève une entrée, alors que -m l’ajoute ou la modifie. Le fichier lui-même n’est pas supprimé." : "-x retire la règle ACL de wilder ; -m la modifierait ou l’ajouterait.";
            if (t.startsWith("Autorise invite")) return extended ? "Sur un dossier, r permet de lister les noms et x permet d’entrer. Dans setfacl -m u:invite:rx partage-equipe, u:invite désigne la personne, rx lui donne les deux droits. L’ACL du dossier ne donne pas automatiquement accès au contenu des fichiers." : "Pour lister ET traverser le dossier, invite a besoin de r et de x.";
            if (t.startsWith("En tant que root, ajoute wilder")) return extended ? "usermod -aG sudo wilder ajoute le groupe sudo à la liste des groupes supplémentaires de wilder. -a est essentiel : sans lui, -G remplacerait cette liste. Une nouvelle session peut être nécessaire pour que l’appartenance soit prise en compte." : "-G indique le groupe ; -a l’ajoute sans retirer les groupes que wilder avait déjà.";
            if (t.startsWith("Lis la configuration sudoers")) return extended ? "sudo cat /etc/sudoers affiche ce fichier protégé. Lire n’est pas éditer : si tu dois le modifier sur un vrai Ubuntu, utilise visudo, qui vérifie la syntaxe avant de sauvegarder." : "cat affiche le texte du fichier ; sudo donne l’autorisation nécessaire pour le lire.";
            if (t.startsWith("Recherche les chemins")) return extended ? "Dans find / -perm -4000 2>/dev/null, / veut dire partir du sommet des dossiers ; -perm -4000 cherche les fichiers portant SUID ; 2> redirige les erreurs, notamment les accès refusés, vers /dev/null pour ne pas encombrer l’écran." : "find cherche les chemins ; -4000 sélectionne SUID ; 2>/dev/null cache les messages d’erreur.";
            if (t.startsWith("Piège : quel mécanisme")) return extended ? "La protection demandée est le sticky bit du dossier. SGID fait hériter le groupe ; umask règle les droits à la création : aucun de ces deux choix n’empêche la suppression des fichiers d’autrui. Le vrai mécanisme n’étant pas proposé, la réponse est « Aucune de ces réponses »." : "Le sticky bit protège les fichiers d’autrui contre la suppression. Comme il n’est pas dans la liste, choisis « Aucune de ces réponses ».";
        }
        return "";
    }
}
