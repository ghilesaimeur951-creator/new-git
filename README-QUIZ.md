# Ubuntu & Git Academy

Application Android d'apprentissage de Bash/Ubuntu, Git, GitHub, SSH, branches, synchronisation, merge et conflits, construite à partir des supports de cours du projet.

## Version 4.0 — Ubuntu Lab

La V4 transforme le Laboratoire Ubuntu en deux environnements clairement séparés :

- **SIMULATION** : machine Ubuntu/Git virtuelle, locale et sans danger ;
- **GITHUB RÉEL** : dépôt Git réellement cloné dans l'espace privé de l'application, avec opérations réseau HTTPS explicites.

### Interface Ubuntu

Le terminal utilise une interface compacte inspirée de GNOME Terminal :

```text
ubuntu@academy:~$ ls
Documents  Projets  Téléchargements  notes.txt

ubuntu@academy:~$ cd Projets
ubuntu@academy:~/Projets$
```

Le prompt reflète le dossier courant. Les dossiers sont affichés en bleu, les fichiers ordinaires en blanc, les erreurs en rouge et les succès en vert. La police est monospace et volontairement plus petite pour conserver une vraie sensation de terminal sur téléphone.

La barre de saisie est fixe sous la zone de terminal. Android `adjustResize` et les safe areas gardent le champ de commande au-dessus du clavier et des boutons/gestes système. La touche Entrée exécute la commande, le terminal défile automatiquement vers les dernières lignes, et des boutons ↑/↓ permettent de rappeler l'historique.

## SIMULATION

### Terminal libre

Le terminal libre conserve un état cohérent pendant toute la session :

- arborescence virtuelle `/`, `/home`, `/home/ubuntu` et sous-dossiers ;
- dossier courant ;
- fichiers, dossiers et contenu des fichiers ;
- historique des commandes ;
- dépôt Git, HEAD, branche courante et branches ;
- staging area ;
- commits avec messages et snapshots ;
- remote `origin`, branches distantes simulées et `origin/main` ;
- état de conflit ;
- clés et agent SSH simulés.

La simulation ne lance **aucun shell Android** et ne modifie pas le système réel du téléphone.

### Commandes Bash prises en charge

```text
pwd
ls
ls -l
ls -la
cd
cd ..
cd ~
mkdir
mkdir -p
touch
echo
>
>>
cat
nano
mv
cp
cp -r
rm
rm -r
rmdir
clear
history
whoami
hostname
uname
uname -a
id
date
```

`nano fichier` ouvre un éditeur pédagogique dans l'application et enregistre le contenu dans le système de fichiers virtuel.

### Git simulé

```text
git --version
git config --global user.name "..."
git config --global user.email "..."
git config --global --list
git config --list --show-origin
git config --global --unset ...
git clone URL
git init
git status
git add fichier
git add .
git commit -m "message"
git log
git log -p
git log --oneline
git log --graph --oneline --decorate --all
git diff
git diff --staged
git branch
git branch -a
git branch -M main
git branch -d branche
git switch branche
git switch -c branche
git remote -v
git remote get-url origin
git remote add origin URL
git remote set-url origin URL
git remote remove origin
git fetch
git fetch --all
git fetch --prune
git pull origin main
git pull --rebase origin main
git pull origin main --allow-unrelated-histories
git push
git push origin main
git push -u origin main
git push --set-upstream origin main
git push origin branche
git push origin --delete branche
git push --force-with-lease origin main
git ls-remote origin
git ls-remote --heads origin
git merge --abort
git show --oneline --stat HASH
```

Le modèle virtuel représente `working directory`, `staging area`, commits, branches, `HEAD`, `main`, `origin/main`, divergence et conflits. Un scénario de conflit peut produire les marqueurs :

```text
<<<<<<< HEAD
LOCAL
=======
REMOTE
>>>>>>> origin/main
```

### SSH simulé

```text
ls -al ~/.ssh
ssh-keygen -t ed25519 -C "email"
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519
cat ~/.ssh/id_ed25519.pub
ssh-keygen -lf ~/.ssh/id_ed25519.pub
ssh -T git@github.com
```

La clé privée du simulateur n'est jamais exposée comme une vraie clé.

## Missions guidées

Les missions utilisent le même terminal et le même état virtuel. Pour chaque situation, l'utilisateur peut choisir entre :

- **4 propositions** ;
- **écrire lui-même la commande**.

Les scénarios couvrent navigation, fichiers, redirections, initialisation Git, staging, commits, branches, remotes, premier push, rebase, SSH, création volontaire d'un conflit, lecture des marqueurs, résolution, vérification staged et lecture du graphe Git.

## GITHUB RÉEL

Le mode **GITHUB RÉEL** est séparé visuellement et fonctionnellement de la simulation. Les commandes réseau ne sont jamais déclenchées depuis le mode SIMULATION.

### Connexion

La V4 utilise HTTPS avec un **fine-grained personal access token GitHub** fourni volontairement par l'utilisateur. Aucun token n'est présent dans le code source ou dans le dépôt.

Le token est :

- saisi dans l'application ;
- validé auprès de l'API GitHub ;
- chiffré avec AES/GCM ;
- protégé par une clé stockée dans **Android Keystore** ;
- supprimé avec le bouton **Déconnexion**.

Pour lire/cloner/pull un dépôt, le token doit disposer des droits nécessaires sur ce dépôt. Pour pousser, il doit également autoriser l'écriture du contenu.

La V4 utilise ce mécanisme plutôt qu'un faux écran OAuth : le GitHub Device Flow nécessite l'identifiant public d'une OAuth App ou GitHub App enregistrée. Aucun client secret ou token n'est embarqué dans l'APK.

### Dépôts

Après connexion :

1. **Choisir dépôt** récupère les dépôts réellement accessibles au compte.
2. Le dépôt choisi devient la cible affichée par l'application.
3. **Cloner / ouvrir** clone le dépôt via HTTPS dans l'espace privé Android de l'application.
4. Les commandes Git prises en charge travaillent sur cette copie locale réelle.

Le moteur Git réel est **JGit 6.10.1**. Il n'exécute pas de binaire shell arbitraire.

### Commandes réelles principales

Le mode réel prend notamment en charge :

```text
git clone https://github.com/OWNER/REPO.git
git status
git remote -v
git remote get-url origin
git remote add origin URL
git remote set-url origin URL
git remote remove origin
git branch
git branch -a
git branch -M main
git branch -d branche
git switch branche
git switch -c branche
git add fichier
git add .
git commit -m "message"
git log
git log --oneline
git log --graph --oneline --decorate --all
git diff
git diff --staged
git fetch
git fetch origin
git fetch --all
git fetch --prune
git pull origin main
git pull --rebase origin main
git push
git push origin main
git push -u origin main
git push origin branche
git push origin --delete branche
git ls-remote origin
git ls-remote --heads origin
git config ...
git show ...
```

Les opérations réseau affichent explicitement qu'elles sont **réelles**.

### Sécurité du mode réel

- aucun shell root ou shell Android arbitraire ;
- dépôt réel isolé dans le stockage privé de l'application ;
- authentification HTTPS uniquement dans cette version ;
- les exercices SSH restent dans la simulation ;
- suppression de branche distante : confirmation obligatoire ;
- `git push --force-with-lease` : confirmation obligatoire, puis refus de l'exécuter automatiquement dans cette version tant que la protection de lease n'est pas garantie de bout en bout ;
- le dépôt ciblé `origin = OWNER/REPO` est affiché avant les actions dangereuses ;
- aucune clé privée SSH réelle n'est demandée ou stockée.

## Autres modes de l'Academy

L'application conserve également :

- 5 niveaux de quiz ;
- XP, bonus de séries et progression ;
- quiz rapide de 10 questions ;
- examen de 20 questions sans correction immédiate ;
- entraînement adaptatif ;
- révision ciblée ;
- révision des erreurs ;
- fiches de cours ;
- badges ;
- statistiques et graphiques ;
- objectif et série quotidienne ;
- bibliothèque de commandes avec recherche, copie et favoris ;
- thème clair/sombre ;
- export de progression.

## Limites assumées

Ubuntu Lab est une **machine pédagogique**, pas une VM Linux complète. Les commandes de la formation sont reproduites dans le mode SIMULATION, mais une commande Linux arbitraire inconnue n'est jamais exécutée directement sur Android.

Le mode GITHUB RÉEL est différent : ses opérations Git prises en charge utilisent réellement Internet et le dépôt sélectionné. L'interface signale toujours l'environnement actif.

## Générer l'APK

Le workflow `.github/workflows/build-apk.yml` compile automatiquement chaque push sur `main`.

1. Ouvre **Actions**.
2. Ouvre **Build Android APK**.
3. Choisis la dernière exécution verte.
4. Télécharge l'artifact **quiz-ubuntu-git-apk**.
5. Décompresse le ZIP.
6. Installe `app-debug.apk` sur Android.

## Développement

- Java 17
- Android SDK 35
- Gradle 8.9
- Android Gradle Plugin 8.7.3
- JGit 6.10.1
- Android Keystore / AES-GCM pour le token GitHub

Compilation :

```bash
gradle :app:assembleDebug
```

APK :

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Historique récent

- **4.0** — Ubuntu Lab réaliste, VM Git stateful, GitHub réel HTTPS sécurisé.
- **3.2** — terminal libre et correctif clavier.
- **3.1** — safe areas Android.
- **3.0** — premier Ubuntu Lab.
- **2.1** — UI, progression et révision des erreurs.
