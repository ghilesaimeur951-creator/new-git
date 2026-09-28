# Ubuntu & Git Academy 4.5

## Corrections et ajouts

- 1 541 entrées de catalogue, dont 1 016 variantes de texte exécutables ajoutées dans la catégorie « Texte exécutable ». Ce sont des signatures et combinaisons d’options, pas 1 016 programmes Ubuntu distincts. Les anciennes combinaisons génériques invalides ont été retirées.
- `grep`, `sort`, `cat`, `wc`, `head`, `tail` travaillent sur le contenu réel des fichiers virtuels. Les options inconnues donnent une erreur explicite.
- `git add` conserve désormais le contenu préparé : éditer ensuite le fichier ne change plus silencieusement le prochain commit. `git diff` et `git status` montrent aussi les changements effectués après le staging.
- Les opérateurs `&&` entre guillemets ne sont plus interprétés comme une séparation de commandes. Les alias récursifs sont interrompus.
- `echo` écrit la fin de ligne attendue ; `>` vide un fichier. `wc -c` compte les octets UTF-8 et `wc -m` les caractères Unicode.
- La simulation reste une simulation, même pour `git push` ou `ssh-keygen`. Le mode GitHub réel est sélectionné explicitement.

## Quiz : uniquement les cinq PDF

40 questions supplémentaires : Fiche Bash S01, Guide Git débutant Ubuntu, mémo Git/GitHub/SSH, Branches et flow, Merge & Conflits. Chaque nouvelle correction indique son support source.

Les réponses changent d’ordre à chaque question. La barre de progression se met à jour. L’examen tire quatre questions par niveau et présente les réponses choisies et les corrections à la fin. La session adaptative est limitée à douze questions : jusqu’à huit erreurs fréquentes, puis un complément varié. Rejouer une session adaptative conserve ce mode.

## Téléphone et ordinateur

1. Dans Ubuntu Lab, sélectionner **GitHub réel**, puis **Connexion GitHub**.
2. Créer un jeton GitHub à permissions fines, limité aux dépôts nécessaires, avec Contents en lecture/écriture pour publier. Le jeton reste chiffré par Android Keystore. Ne jamais le mettre dans un fichier du dépôt.
3. Sélectionner le dépôt et le cloner. Les dépôts au-delà de la première page de 100 sont désormais proposés.
4. Sur l’ordinateur, ouvrir le même dépôt sur GitHub ; le bouton **Voir le dépôt sur GitHub** permet aussi de le retrouver depuis le téléphone.
5. Configurer son nom et son e-mail Git, puis modifier un fichier, `git add`, `git commit -m "message"`, `git push`. Actualiser GitHub sur l’ordinateur.
6. Après une modification sur GitHub, utiliser `git pull` sur le téléphone. Le pull peut aussi cibler une autre branche avec `git pull origin nom-branche`, `--rebase` ou `--ff-only`.

La connexion GitHub de ChatGPT n’est pas transférée au téléphone. Il faut connecter l’application Android une fois. SSH réel et HTTPS restent disponibles. Aucun compte OAuth fictif ni serveur intermédiaire n’a été ajouté.

## Options de texte

| Commande | Options calculées |
| --- | --- |
| grep | i : ignorer la casse ; v : inverser ; n : numéroter ; c : compter ; x : ligne entière ; F : texte littéral ; w : mot entier ; h/H : masquer/afficher le nom ; s : diagnostic réduit |
| sort | n : numérique ; r : ordre inverse ; u : uniques ; f : ignorer la casse ; b : ignorer espaces initiaux ; d : ordre dictionnaire ; s : tri stable |
| cat | n : numéroter ; b : numéroter les lignes non vides ; s : réduire lignes vides ; E : fins de ligne ; T : tabulations ; v : contrôles ASCII ; A : vET |
| wc | l : sauts de ligne ; w : mots ; c : octets ; m : caractères |
| head / tail | n : lignes ; c : octets ; nombre positif ou nul |

## Limites connues

C’est un simulateur pédagogique et un client Git Android, pas une distribution Ubuntu complète. Les pipelines, les locales GNU et toutes les extensions de regex GNU ne sont pas émulés. Les opérations texte utilisent un sous-ensemble de regex Java ; `grep -F` effectue une recherche littérale. Certains anciens outils système/réseau restent documentaires ou simulés ; seul le mode GitHub réel contacte GitHub. Le graphe Git simulé et les scénarios de conflits restent simplifiés. Le transport réel utilise JGit et ne prend pas toutes les options de Git en charge.

## Vérification

14 tests JVM réussis localement, incluant l’audit de 250 commandes existantes, le parcours du catalogue et les régressions staging, guillemets, redirections, Unicode, tri et recherche. La construction Android et les tests cryptographiques sont également lancés par GitHub Actions sur la pull request. Un essai sur téléphone réel (clavier, réseau, authentification et synchronisation bidirectionnelle) reste nécessaire.
