package com.ghiles.quizubuntu;
import java.util.*;
/** Exact category membership: a chapter quiz never pulls questions from another topic. */
final class AcademyChapters {
    static final class Chapter {
        final String category,title,description,symbol;
        Chapter(String category,String title,String description,String symbol){this.category=category;this.title=title;this.description=description;this.symbol=symbol;}
    }
    static final List<Chapter> ALL=Collections.unmodifiableList(Arrays.asList(
        new Chapter("Bash","Prendre la main sur Ubuntu","Navigation, fichiers et redirections",">_"),
        new Chapter("Git","Enregistrer son travail","Index, commits et historique","●"),
        new Chapter("Branches","Travailler avec des branches","Branches locales, publication et flow","⑂"),
        new Chapter("SSH","Se connecter avec SSH","Clés, agent et authentification","↗"),
        new Chapter("Synchronisation","Relier téléphone et GitHub","Remotes, fetch, push et pull","⇄"),
        new Chapter("Conflits","Résoudre les conflits","Fusion, rebase et résolution","⋈"),
        new Chapter("Utilisateurs","Comptes et groupes Linux","Identités, groupes et sudo","◎"),
        new Chapter("Permissions","Comprendre les permissions","Lecture, écriture et exécution","rwx"),
        new Chapter("ACL et droits spéciaux","Aller plus loin avec les droits","ACL, umask, SUID, SGID et sticky bit","+"),
        new Chapter("VirtualBox","Construire une VM en CLI","Configuration, disques et démarrage","▣"),
        new Chapter("Windows","Explorer le terminal Windows","CMD, lecteurs et chemins","C:")
    ));
    static Chapter find(String category){for(Chapter c:ALL)if(c.category.equals(category))return c;return ALL.get(0);}
    static List<MainActivity.Question> questions(List<MainActivity.Question> all,String category){List<MainActivity.Question> selected=new ArrayList<>();for(MainActivity.Question q:all)if(q.category.equals(category))selected.add(q);return selected;}
}
