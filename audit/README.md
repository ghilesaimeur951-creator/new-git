# Audit Ubuntu Lab 4.6 — 28 septembre 2026

**Le moteur reste un simulateur pédagogique. L’objectif « toutes les commandes Ubuntu et toutes leurs combinaisons fonctionnent » n’est pas atteint.** Un résultat sans erreur ou une entrée de catalogue ne constitue pas une certification de conformité à Ubuntu.

## Résultats vérifiés

| Périmètre | Résultat | Portée |
| --- | --- | --- |
| Catalogue complet | 1 509 exemples exécutés, aucun plantage | Chaque sortie et code sont conservés dans `catalogue-results.json` ; les erreurs attendues dépendent du scénario |
| Texte GNU | 5 904 / 5 904 comparaisons réussies | 984 variantes × six fichiers ; stdout et code exacts |
| Scénarios Bash/Git | 60 / 60 réussis | Pipelines, conditions, fichiers, empreintes et opérations Git ; voir `scenarios.json` |
| Suite JVM locale | 29 tests réussis | Inclut les 5 904 vecteurs GNU et l’ancien audit de 250 commandes |

Les 984 variantes combinent les options de `cat`, `grep`, `sort`, `wc`, `head` et `tail`. Ce ne sont pas 984 programmes différents. Les fichiers couvrent le texte de cours, le fichier vide, l’absence de saut de ligne final, les nombres, les accents/emoji et les caractères de contrôle. Trente-deux combinaisons `sort -n -d`, rejetées par GNU, ont été retirées du catalogue initial de 1 541 exemples.

Les résultats détaillés sont reproductibles avec les scripts décrits dans [tools/README.md](../tools/README.md). `text-corpus.json` contient le bilan ; le fichier TSV des ressources JUnit conserve chaque commande, entrée et résultat GNU attendu. `catalogue-results.json` contient toutes les sorties du catalogue, y compris les échecs, sans les transformer artificiellement en succès.

## Corrections

- Pipelines et opérateurs `&&`, `||`, `;` respectant le code de sortie ; redirections de fichier et prise en compte des guillemets. Les diagnostics sont séparés de stdout pour ne plus alimenter `wc` ou les fichiers de sortie.
- Tri numérique/dictionnaire, Unicode, affichage des contrôles dans `cat`, comptage GNU, filtres texte et empreintes MD5/SHA/CRC calculées sur le contenu.
- Opérations sur fichiers et chemins avec espaces, modes de permissions consultables, liens symboliques, état des paquets/services/processus simulés.
- Archives virtuelles conservant les données et les restaurant effectivement ; erreur si l’archive manque.
- Index Git conservant le contenu ajouté, suppressions indexées, restauration, reset, protection des modifications lors du changement de branche, fast-forward, replay de modifications pour cherry-pick/revert, HEAD détachée sans déplacer la branche.
- `stash` ne remplace plus tout le système de fichiers : les fichiers non suivis et extérieurs au dépôt sont préservés. Les collisions sont refusées.
- Les commandes non implémentées renvoient une erreur explicite au lieu d’un succès documentaire. Correction du routage de `ssh`, auparavant confondu avec le préfixe `ss`.
- Le test `git init` puis `ls -la` conserve la visibilité de `.git`, `.` et `..`.

## Limites restantes — ne pas annoncer ces fonctions comme équivalentes à Ubuntu

- 525 exemples hors corpus de texte ont été exécutés, mais ne sont pas tous certifiés par comparaison native. Plusieurs options et commandes restent absentes, dont rebase, amend, blame, certaines opérations stash/branches, grep récursif, certains tris et les interactions `-i`/`-p`.
- `ls` conserve un affichage pédagogique : plusieurs options avancées ne modifient pas encore le résultat ; les dates et tailles ne constituent pas un véritable modèle de métadonnées Unix. Les horodatages de `touch` et les permissions d’accès ne sont pas entièrement émulés.
- Les archives sont des instantanés en mémoire, **pas des fichiers tar/gzip/zip compatibles avec les outils externes**. Les archives importées et les données binaires ne sont pas prises en charge.
- Le shell ne couvre pas Bash complet : substitutions, globbing général, descripteurs `2>`, scripts, tâches asynchrones et expansions complexes restent incomplets. L’affichage combiné stdout/stderr ne conserve pas tous les entrelacements temporels.
- Git est un modèle d’apprentissage avec un seul dépôt actif : patches, reflog, auteurs, objets, tags, fusions divergentes et échanges distants ne reproduisent pas toutes les règles du vrai Git. Les clonages du simulateur sont des scénarios, pas des téléchargements.
- Le réseau du simulateur (`curl`, `wget`, `ping`, SSH pédagogique), les données système, les paquets et services restent simulés. Des sorties y sont encore prédéfinies. `sleep` n’attend pas réellement.
- **La connexion Internet et les opérations effectives sur GitHub passent par le mode « GitHub réel » existant.** Cet audit local n’a pas testé une synchronisation téléphone/ordinateur ni une connexion avec un jeton personnel.

La suite automatise les cas annoncés dans ce rapport ; elle n’explore pas toutes les combinaisons possibles d’états, de fichiers, de commandes et d’options. Pour une compatibilité Ubuntu générale, l’application doit être reliée à un véritable environnement Ubuntu en plus du simulateur.
