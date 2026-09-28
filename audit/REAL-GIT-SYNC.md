# Synchronisation GitHub réelle — correctif 4.7

## Défauts identifiés dans le code

- Le terminal réel n’implémentait que `pwd` et quelques variantes de `ls` pour les fichiers. L’aide recommandait pourtant `nano`, sans branchement de cet éditeur sur le dossier réel.
- Les appels JGit `push.call()` ignoraient les résultats des références distantes. Un refus pouvait donc être suivi du texte « Push terminé ».
- Le panneau affichait le dépôt sélectionné dans le sélecteur, et pas nécessairement le remote du dossier actif.
- Les opérations virtuelles pouvaient être confondues avec une synchronisation Internet.

## Corrections

Le dossier réel persistant est partagé par les commandes `mkdir`, `cd`, `touch`, `echo` avec `>`/`>>`, `cat`, `ls`, l’éditeur `nano` et JGit. Les chemins avec espaces sont acceptés. Les écritures restent confinées à cet espace et ne peuvent pas modifier directement `.git` ni suivre un lien symbolique vers l’extérieur.

`git init` initialise le dossier courant sur `main`. L’identité globale peut être définie avant l’initialisation. `git add` utilise des chemins relatifs au dossier courant et prend en charge `.`/`-A`/`-u`.

Chaque référence renvoyée par push est examinée : acceptée, déjà à jour ou refusée. Un refus ou l’absence de confirmation lève une erreur ; un upstream n’est enregistré qu’après acceptation. Un push ne publie jamais les modifications non commitées, et le résultat le rappelle si nécessaire.

Le pull annonce les chemins modifiés par les commits récupérés ; les conflits ne sont pas annoncés comme synchronisés. Le panneau et le bouton GitHub utilisent le remote du dossier actif. Changer de transport convertit l’URL du remote courant sans la remplacer par celle d’un autre dépôt sélectionné.

## Validation automatisée

`RealSyncRoundTripTest` utilise le véritable moteur JGit, un dépôt distant bare local et deux copies de travail indépendantes. Il vérifie les noms et contenus UTF-8 après téléphone → distant → ordinateur, puis ordinateur → distant → téléphone, puis une nouvelle sauvegarde et publication depuis le téléphone. Il vérifie aussi les modifications non commitées, un push refusé pour divergence, un conflit au pull, les chemins avec espaces et le confinement des écritures.

Ces tests sont exécutés par GitHub Actions avec la compilation Android. Ils ne constituent pas un essai sur le téléphone de l’utilisateur ou une connexion HTTPS avec son jeton personnel ; l’authentification et les permissions réelles du dépôt doivent toujours être valides.

## Utilisation

Choisir **GITHUB RÉEL**, connecter le compte, puis travailler dans le dépôt cloné ou créer un dossier dans ce mode. Le mode Simulation conserve ses propres fichiers virtuels et n’envoie rien sur GitHub ; un avertissement est désormais affiché devant ses commandes réseau.

Après modification sur le téléphone :

```bash
cat mon-fichier.txt
git status
git add mon-fichier.txt
git commit -m "Modification depuis le téléphone"
git push origin main
```

Après modification et commit sur GitHub :

```bash
git pull origin main
cat mon-fichier.txt
```

`git remote -v` montre les URL ; il ne prouve pas à lui seul qu’une authentification ou un envoi a réussi. Vérifier le même dépôt et la même branche sur les deux appareils. Ne pas désinstaller l’application pour appliquer la mise à jour : le dossier réel est conservé dans ses données privées.
