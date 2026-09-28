# 5.1 — Material 3 et parcours par chapitre

Base préconstruite retenue : Material Components for Android (Material 3),
com.google.android.material:material:1.12.0, licence Apache 2.0. Les boutons et
la barre de navigation utilisent les composants de la bibliothèque, avec une
palette et des cartes propres à l’Academy. Le moteur Java existant est conservé.
Licence embarquée : assets/licenses/material-components-Apache-2.0.txt.

Références consultées :
- https://github.com/material-components/material-components-android
- https://github.com/material-components/material-components-android/blob/master/docs/components/Button.md
- https://developer.android.com/develop/ui/compose/designsystems/material3
- https://developer.android.com/develop/ui/compose/samples
- https://developer.android.com/reference/android/media/ToneGenerator

Choix : Material pour les composants natifs, les états et les cibles tactiles ;
les exemples Compose comme référence d’organisation, sans migrer le moteur de
l’application en Kotlin/Compose. Les sources des cours et quiz ne changent pas.

Accueil : titre clair, carte de reprise en dégradé indigo/turquoise, raccourci
terminal, accès aux chapitres, révisions. Les anciens modes sont accessibles dans
« Tous les entraînements et outils » : rapide, examen, adaptatif, niveaux,
fiches, commandes/favoris, badges, export. Navigation fixe sur l’accueil et le
parcours. Couleurs et boutons partagés avec le terminal. Thèmes clair/sombre,
textes redimensionnables, boutons de 48 dp minimum, animations ripple natives.

11 chapitres : Bash, Git, Branches, SSH, Synchronisation, Conflits, Utilisateurs,
Permissions, ACL et droits spéciaux, VirtualBox, Windows. Chaque quiz filtre
strictement sa catégorie, enregistre son meilleur score et permet de rejouer
ce même chapitre. L’accueil reprend le dernier chapitre choisi (nouvelle série,
pas restauration d’une question interrompue).

Sons : clics système selon les réglages Android ; validation courte sur bonne
réponse hors examen et réussite de mission. Option « Sons discrets » persistée
avec les réglages du thème. Pas de révélation de bonne réponse par son en examen.
La validation respecte le mode silencieux/vibreur, le volume média nul et la
musique déjà en cours. Ressource audio libérée lorsque l’application perd le focus.

Validation prévue : tests des quiz par chapitre, score et reprise, option muette,
composants Material, rendu Android natif sur 360 × 800 pour accueil clair/sombre,
chapitres, quiz et terminal ; puis suite existante et compilation APK. Les captures
sont produites en CI dans l’artefact academy-design-previews.
