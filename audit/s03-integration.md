# Version 5.0 — supports S03

Sources : les cinq PDF joints (Windows, droits Linux 1/2/3, VirtualBox) et le
DOCX « S03 — Exercices ». Le DOCX fixe l'ordre pédagogique, sans ajouter de
commande technique substantielle. Les supports antérieurs restent présents.

## Accès dans l’application

Missions guidées → Ateliers S03 → choisir l'un des cinq ateliers. La saisie est
libre, sans validation automatique fondée sur une simple orthographe de commande.
Les sessions conservent les comptes, permissions, ACL et états VirtualBox/Windows
par le journal de commandes déjà utilisé pour les missions. Terminal libre reste
le terminal réel introduit en 4.9 ; aucune commande S03 n'y crée de compte Android.

Révision ciblée → Utilisateurs / Permissions / ACL et droits spéciaux / VirtualBox /
Windows. Ajout de 61 questions (saisie, diagnostic, ordre, prédiction et pièges
annoncés), avec source, exemple et correction. Les niveaux existants accueillent
également ces questions. Les corrections comportent des schémas textuels explicatifs.

## Commandes et comportements

72 exemples indexés, listés dans s03-command-coverage.json ; total : 1581 exemples
pédagogiques. Ils ne correspondent pas à 1581 exécutables Ubuntu distincts.

- Comptes/groupes : useradd/adduser, groupadd/addgroup, usermod -a -G/-aG/-G,
  passwd, id/groups, su/exit, sudo, lecture de passwd/group/shadow/sudoers.
- Modes symboliques et octaux, chown/chgrp récursifs, umask, traversée des dossiers,
  SGID hérité, suppression protégée par sticky bit, SUID sur le modèle whoami,
  ACL nommées utilisateurs/groupes avec masque et retrait, find -perm.
- VirtualBox : registre, noms/UUID, configuration RAM/vidéo/réseau, contrôleurs,
  médias, raccordements, clonage de média, démarrage/arrêt/pause et snapshots de
  configuration. Les erreurs (VM absente, doublon, média absent, option inconnue,
  modification pendant le fonctionnement) ne sont pas présentées comme des succès.
- Contexte CMD séparé : cmd, cd/chdir, lettres de lecteurs, dir, mkdir/md, echo,
  type, cls, exit. Chemins Windows insensibles à la casse ; pas de confusion avec
  les chemins et permissions Linux.
- Corrections découvertes : head -1 et parcours find / ; ls -l affiche aussi le
  propriétaire, le groupe, les bits spéciaux et le marqueur ACL.

## Limites explicites du modèle

Il ne démarre pas d’OS invité, n’alloue pas des disques de 30 Go et n’exécute pas
un interpréteur Bash complet. Les scripts VirtualBox du PDF sont étudiés dans les
questions, avec exécution de leurs opérations VBoxManage individuellement dans
l’atelier. Les snapshots portent sur la configuration, pas sur un OS en mémoire.
Adduser abrège le questionnaire interactif. Passwd explique la double saisie sans
collecter de mot de passe ; su ne simule pas l’authentification. Le contexte Windows
couvre les commandes de navigation du support ; les ACL NTFS sont étudiées dans
le quiz. Le modèle ACL Linux couvre les entrées nommées, pas les ACL par défaut.

## Corrections pédagogiques vérifiées

Umask = masquage bit à bit, pas soustraction : 0666 & ~0033 = 0644.
2048 Mo de RAM = 2 Gio, pas 2048 Go. Sticky bit autorise également root et le
propriétaire du dossier. Pour un accès à un dossier par ACL, r-x est généralement
nécessaire ; r seul liste des noms. L’expérience SUID concerne un binaire whoami,
pas un script shell. Références complémentaires :
- https://www.gnu.org/software/coreutils/manual/html_node/Umask-and-Protection.html
- https://www.gnu.org/software/coreutils/manual/html_node/Changing-Special-Mode-Bits.html
- https://man7.org/linux/man-pages/man5/acl.5.html
- https://docs.oracle.com/en/virtualization/virtualbox/7.0/user/vboxmanage.html

## Validation

9 tests de scénarios S03 couvrant plus de 100 commandes et vérifications d’état,
37 tests JVM de commandes réussis localement, 78/78 scénarios comparés à Bash/Git,
1581 exemples exécutés sans crash (56 limitations explicitement signalées).
Tests Android ajoutés : reprise d’une session S03 avec utilisateur, umask et fichier
protégé ; banque des 61 questions, sources et réponses. La CI exécute aussi les
5904 comparaisons GNU existantes, compile et produit l’APK Android.
