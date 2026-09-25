# Ubuntu & Git Academy

Application Android de révision et de pratique basée sur les supports de cours fournis : Bash/Ubuntu, Git, branches, GitHub/SSH, synchronisation et conflits.

## Version 3.2 — Ubuntu Lab libre

Cette mise à jour améliore fortement le laboratoire Ubuntu :

- barre de saisie fixée en bas de l'écran, au-dessus du clavier Android
- le clavier redimensionne l'écran au lieu de masquer le champ de commande
- recentrage automatique du terminal quand le champ reçoit le focus
- deux modes séparés : **Missions guidées** et **Terminal libre**
- en missions guidées : choix entre 4 propositions ou saisie manuelle de la commande
- en terminal libre : possibilité de taper librement des commandes simulées
- commandes Bash simulées supplémentaires : `whoami`, `hostname`, `uname`, `date`, `id`, `cp`, `mv`, `rm`, `rmdir`, `nano`, `history`, `clear`
- commandes Git supplémentaires : `git --version`, `git branch`, `git switch`, `git fetch`, plusieurs formes de `git push`
- enchaînement simple avec `&&`
- commande `help` pour afficher les commandes disponibles

Le terminal reste un **simulateur pédagogique** : il n'exécute jamais de commande réelle sur Android.

## Version 3.1 — Safe Area mobile

Cette mise à jour améliore l'utilisation sur téléphone :

- le contenu ne passe plus sous la barre d'état en haut
- le contenu ne passe plus sous la barre de navigation / gestes Android en bas
- marges de sécurité supplémentaires en haut et en bas
- meilleure prise en compte des écrans avec encoche ou bordures système
- le terminal simulé reste visible lorsque le clavier Android s'ouvre grâce au redimensionnement de la fenêtre

## Version 3.0 — Ubuntu Lab

La V3 ajoute un véritable espace de pratique en plus du quiz.

### PC Ubuntu simulé

Le nouveau **Laboratoire Ubuntu** reproduit un terminal pédagogique sans exécuter de commandes sur le vrai téléphone.

Il propose des objectifs guidés comme :

- afficher le dossier courant avec `pwd`
- inspecter les fichiers cachés avec `ls -la`
- créer une arborescence avec `mkdir -p`
- créer des fichiers avec `touch`
- initialiser un dépôt avec `git init`
- vérifier l'état avec `git status`
- préparer et créer un commit
- vérifier ou ajouter un remote GitHub
- créer une paire de clés SSH Ed25519
- tester GitHub avec `ssh -T git@github.com`
- publier `main` avec `git push -u origin main`
- synchroniser avec `git pull --rebase origin main`
- créer une branche avec `git switch -c`
- inspecter les changements avec `git diff`
- inspecter le staging avec `git diff --staged`
- terminer une résolution de conflit avec `git add README.md`

Chaque mission peut être réalisée de deux façons :

- **4 propositions**
- **commande libre tapée au clavier**

Le simulateur maintient un petit état virtuel : dossier courant, fichiers, dépôt Git, staging, remote, branche et clé SSH. Il ne touche pas au système réel.

### Nouveaux modes d'apprentissage

- **Quiz rapide** : 10 questions aléatoires
- **Mode examen** : 20 questions sans correction immédiate
- **Entraînement adaptatif** : privilégie les questions déjà ratées
- **Révision ciblée** : Bash, Git, Branches, SSH, Synchronisation ou Conflits
- **Révision des erreurs** : retire une question de la liste lorsqu'elle est réussie
- **Fiches de cours** : rappels structurés par niveau

### Progression

- objectif quotidien de 10 questions
- série quotidienne de révision
- XP global
- Lab XP dans le simulateur
- précision globale
- maîtrise par thème
- meilleure série
- scores par niveau
- badges
- export texte de la progression
- bibliothèque de commandes avec recherche, copie et favoris

### Graphisme

- thème clair / sombre
- interface inspirée d'un terminal Ubuntu
- cartes de niveaux
- barres de progression
- graphiques de précision
- réponses correctes en vert et erreurs en rouge
- nouvelle icône terminal Ubuntu & Git Academy

## Version 2.1

- tableau de bord avec niveau joueur et barre XP
- thème clair / sombre
- cartes de niveaux
- mode Réviser mes erreurs
- navigation Accueil / Stats / Badges / Mémo
- graphiques adaptés au thème sombre

## Niveaux

1. **Fondamentaux Ubuntu** — navigation, dossiers et fichiers
2. **Fichiers et premiers pas Git** — redirections et bases Git
3. **Workflow Git** — staging, commits, diff et remotes
4. **Branches, GitHub et SSH** — branches distantes et clés SSH
5. **Synchronisation et conflits** — rebase et résolution de conflits

## Générer l'APK

Le workflow `.github/workflows/build-apk.yml` compile automatiquement l'application à chaque push sur `main`.

1. Ouvre **Actions**.
2. Ouvre **Build Android APK**.
3. Ouvre la dernière exécution verte.
4. Dans **Artifacts**, télécharge **quiz-ubuntu-git-apk**.
5. Décompresse le ZIP.
6. Installe `app-debug.apk` sur Android.

## Développement

- Java 17
- Android SDK 35
- Gradle 8.9
- Android Gradle Plugin 8.7.3

```bash
gradle :app:assembleDebug
```

APK :

```text
app/build/outputs/apk/debug/app-debug.apk
```
