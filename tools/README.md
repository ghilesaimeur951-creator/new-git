# Reproduire l’audit (Java 17, Python 3, Bash, GNU coreutils/grep, Git)

Depuis la racine du dépôt :

```bash
mkdir -p build/audit-classes
javac -d build/audit-classes app/src/main/java/com/ghiles/quizubuntu/{VirtualMachine,CommandCatalog,ShellSyntax,TextCommands,TextFilters}.java tools/{CatalogAuditProbe,DifferentialProbe,TextCorpusProbe}.java
python3 tools/audit_catalogue.py
python3 tools/differential_scenarios.py
python3 tools/audit_text_corpus.py
gradle :app:testDebugUnitTest :app:assembleDebug
```

`AUDIT_JAVA` et `AUDIT_CLASSES` permettent de choisir le binaire Java et le dossier des classes. Les scripts sortent en erreur si une comparaison échoue ou si le simulateur plante. Le catalogue utilise un état neuf par exemple, défini dans `CatalogAuditProbe.fixture()`. Il ne prouve pas la conformité sémantique de chaque commande.

Le corpus GNU teste 984 variantes sur six fichiers (5 904 comparaisons exactes de stdout et du code de sortie, locale C.UTF-8). Il régénère les attentes indépendantes dans `app/src/test/resources/gnu-text-corpus.tsv`. Ces attentes sont rejouées par JUnit dans la CI Android sans dépendance à GNU sur Android.

Les 60 scénarios comparent les résultats finaux de chaînes de commandes à Bash/Git. Les erreurs doivent produire un code non nul ; leur formulation et leur code précis ne sont pas imposés. Pour `stat`, seul le mode 0755 est vérifié. Les commandes de préparation ne constituent pas chacune une assertion indépendante. Les dates, chemins temporaires, durées et empreintes Git ne sont pas comparés.

Aucune commande native de cet audit ne contacte GitHub : les dépôts de référence sont temporaires et locaux. Le mode GitHub réel de l’application nécessite une validation réseau séparée.
