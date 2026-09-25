# Ubuntu & Git Academy

Application Android de révision des commandes Ubuntu/Bash, Git, GitHub, SSH, branches et conflits.

## Version 2.1

La V2.1 améliore principalement l'interface et la progression :

- nouveau tableau de bord visuel avec niveau joueur et barre XP
- bouton **Continuer** vers le prochain niveau utile
- thème **clair / sombre** mémorisé
- cartes de niveaux avec progression visuelle
- réponses colorées : bonne réponse en vert, erreur sélectionnée en rouge
- mode **Réviser mes erreurs** avec liste persistante des questions à revoir
- une question disparaît de la liste d'erreurs lorsqu'elle est réussie en révision
- navigation rapide **Accueil / Stats / Badges / Mémo**
- graphiques adaptés au thème sombre
- nouvelle identité graphique inspirée du terminal Ubuntu
- version Android 2.1

## Version 2.0

L'application ne se limite plus à un QCM. Elle comprend maintenant :

- 5 niveaux progressifs avec déblocage à 70 %
- points par bonne réponse
- bonus de série pour les bonnes réponses consécutives
- sauvegarde de la progression sur le téléphone
- exemples de terminal après chaque réponse
- illustrations pédagogiques intégrées
- page de statistiques
- graphique des meilleurs scores par niveau
- graphique circulaire de précision globale
- badges de progression
- mémo des commandes
- meilleur score enregistré par niveau

## Niveaux

1. **Fondamentaux Ubuntu** — navigation, dossiers et fichiers
2. **Fichiers et premiers pas Git** — redirections et bases Git
3. **Workflow Git** — staging, commits, diff et remotes
4. **Branches, GitHub et SSH** — branches distantes et clés SSH
5. **Synchronisation et conflits** — rebase, merge et résolution de conflits

## Système de points

Une bonne réponse rapporte 100 points.

Une série de bonnes réponses ajoute un bonus progressif :

- 2e bonne réponse consécutive : +25
- 3e : +50
- 4e : +75
- puis bonus plafonné à +100

Les statistiques et la progression restent enregistrées localement dans l'application.

## Générer l'APK avec GitHub Actions

Le workflow `.github/workflows/build-apk.yml` compile automatiquement l'application à chaque push sur `main`.

1. Ouvre l'onglet **Actions** du dépôt GitHub.
2. Ouvre **Build Android APK**.
3. Ouvre la dernière exécution verte.
4. Dans **Artifacts**, télécharge **quiz-ubuntu-git-apk**.
5. Décompresse le ZIP.
6. Installe `app-debug.apk` sur ton téléphone Android.

## Développement

Le projet utilise :

- Java 17
- Android SDK 35
- Gradle 8.9
- Android Gradle Plugin 8.7.3

Commande de compilation :

```bash
gradle :app:assembleDebug
```

APK généré :

```text
app/build/outputs/apk/debug/app-debug.apk
```
