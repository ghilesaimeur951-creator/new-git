# Version 4.8 — saisie dans le terminal et pédagogie

Le terminal affiche désormais l’historique, le prompt et la commande dans une seule surface de saisie. Le petit champ séparé est supprimé. Cela concerne le terminal libre, les missions et le mode GitHub réel. Toucher le terminal ouvre le clavier ; Entrée ou ↵ lance la commande. ↑/↓ rappellent les commandes. L’historique et le prompt ne peuvent pas être effacés en éditant la commande ; un collage multiligne n’exécute rien automatiquement. Les sorties asynchrones préservent la commande en cours de rédaction.

Les banques existantes sont conservées. 26 exercices supplémentaires sont issus exclusivement des cinq PDF déjà fournis : Fiche Bash S01, Guide Git débutant Ubuntu, mémo commandes Git/GitHub/SSH, Guide Branches et Flow, Guide Merge et Conflits. Chaque correction nomme son support. Les cinq niveaux comportent des réponses écrites et des QCM avec « Aucune de ces réponses », parfois correcte et parfois incorrecte pour éviter un indice systématique.

Formats : commande à écrire, commande à corriger, résultat à prévoir, ordre à rétablir, diagnostic et choix raisonné. Les sessions alternent réponses écrites et choix lorsque ces formats sont présents. La correction écrite accepte des variantes explicitement prévues ainsi que des espaces/guillemets équivalents, sans accepter un mauvais chemin dont la casse diffère. Ce n’est pas une évaluation générale de toute formulation en français ou de toute commande équivalente possible. Les réponses de quiz ne sont pas exécutées.

Les corrections montrent une explication, un exemple de terminal et, pour Git/branches/conflits, une illustration officielle obligatoire après réponse (hors examen, dont les corrections restent regroupées à la fin). Les deux SVG sont embarqués, donc lisibles hors ligne, avec légende française, attribution, licence et lien vers la source. Les schémas pédagogiques déjà présents restent utilisés avant les questions. Les sources web complètent les illustrations, sans ajouter de sujets aux exercices issus des PDF.

Sources consultées le 28 septembre 2026 :
- https://git-scm.com/book/en/v2/Getting-Started-What-is-Git%3F
- https://git-scm.com/book/en/v2/Git-Branching-Basic-Branching-and-Merging
- https://git-scm.com/book/en/v2 (licence CC BY-NC-SA 3.0)
- https://github.com/progit/progit2/blob/main/images/areas.svg
- https://github.com/progit/progit2/blob/main/images/basic-merging-1.svg

Tests ajoutés : protection de l’historique du terminal, conservation du brouillon lors d’une sortie, collage non exécutant, tolérance contrôlée des réponses écrites et cohérence des exercices aux cinq niveaux. Les tests du terminal utilisent les composants Android avec Robolectric ; cela ne remplace pas un essai tactile sur chaque téléphone et clavier.
