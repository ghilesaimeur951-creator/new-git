# Quiz Ubuntu & Git

Application Android de révision basée sur les supports de cours fournis : Bash/Ubuntu, Git, branches, GitHub/SSH et conflits.

## Contenu

Le quiz couvre notamment :

- navigation Bash : `pwd`, `ls`, `cd`
- fichiers et dossiers : `mkdir`, `touch`, `cat`, `rm`
- redirections `>` et `>>`
- cycle Git : `status → add → commit → push`
- branches, HEAD, staging area et diffs
- SSH GitHub
- synchronisation et conflits

Les questions sont mélangées à chaque nouvelle partie. Une correction est affichée après chaque réponse.

## Générer l'APK avec GitHub Actions

Le workflow `.github/workflows/build-apk.yml` compile automatiquement l'application.

1. Ouvre l'onglet **Actions** du dépôt GitHub.
2. Ouvre **Build Android APK**.
3. Une fois le build terminé, ouvre l'exécution réussie.
4. Dans **Artifacts**, télécharge **quiz-ubuntu-git-apk**.
5. Décompresse le fichier ZIP pour obtenir `app-debug.apk`.
6. Transfère l'APK sur ton téléphone Android et ouvre-le pour l'installer.

Android peut demander d'autoriser temporairement l'installation d'applications provenant de cette source.

## Développement local

Le projet utilise :

- Kotlin
- Android SDK 35
- Java 17
- Gradle 8.9
- Android Gradle Plugin 8.7.3

Pour compiler sans Android Studio :

```bash
gradle :app:assembleDebug
```

L'APK se trouve ensuite dans :

```text
app/build/outputs/apk/debug/app-debug.apk
```
