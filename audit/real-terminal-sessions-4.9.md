# 4.9 — Terminal libre réel et sessions

Le terminal libre utilise désormais RealGitClient/RealWorkspace : les créations,
les éditions nano, les commits et les opérations réseau concernent le même dossier
sur le téléphone. Le répertoire existant `files/github-real` devient la session 1,
sans migration destructive. Chaque nouvelle session possède son dossier et son cwd.
Les exercices guidés conservent leur environnement pédagogique isolé.

Le menu contient Terminal libre, Missions guidées, Help, Commandes et Retour à
l’Académie. Les panneaux compte/dépôt et la rubrique Git/SSH séparée sont retirés.
La connexion est initiée par `gh auth login`. Le jeton est masqué, absent de
l’historique et des snapshots, puis chiffré par Android Keystore après validation.
Aucun jeton ne doit être ajouté aux arguments de commande ou à une URL Git.

`session new [nom]`, `session list`, `session open ID`, `session rename nom`,
`session save` et `session close` pilotent les sessions ; un bouton Sessions permet
aussi de les choisir. `session delete ID --confirm` supprime explicitement une
session inactive et ses fichiers. Fermer l’application conserve fichiers, brouillon,
historique, avancement et événements des missions. Les snapshots sont remplacés
atomiquement. Les identités `git config --global user.name/user.email` sont communes
aux sessions. Les fichiers privés de l’app ne deviennent pas des fichiers du
répertoire Téléchargements ; `ls`, `cat` et `nano` les affichent dans l’application.

Validation ajoutée : reprise des métadonnées/fichiers, isolation de deux sessions,
conservation du dossier historique, confinement des identifiants ; tests Android
Robolectric du routage réel, de `git init`/`ls -la`, de la destruction/recréation
Activity, du changement de session et de l’exclusion d’un jeton des sauvegardes.
La suite existante inclut un aller-retour JGit avec dépôt distant local, vérifiant
noms/contenus des fichiers et refus des push divergents. Elle ne constitue pas un
essai avec le compte GitHub personnel ni sur un téléphone physique.

Le terminal n’installe pas une distribution Ubuntu complète. Une commande réelle
non implémentée renvoie une erreur ; elle ne bascule jamais vers une fausse
exécution virtuelle. Les exemples du catalogue restent une référence pédagogique,
pas une promesse d’exécution de toutes les commandes Linux.
