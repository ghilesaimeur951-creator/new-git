package com.ghiles.quizubuntu;
import java.util.*;
import com.ghiles.quizubuntu.VirtualMachine.Result;
/** Explicit CMD exercise context, separate from Linux path and permission semantics. */
final class S03Windows {
    boolean active;
    private String cwd;
    private final Set<String> dirs=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String,String> files=new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    void reset(){active=false;cwd="C:\\Users\\ubuntu";dirs.clear();files.clear();dirs.addAll(Arrays.asList("C:\\","C:\\Users",cwd,"D:\\"));}
    String cwd(){return cwd;}
    private String path(String raw){String value=raw.replace("\"", "").replace('/', '\\');if(value.matches("[A-Za-z]:"))value+="\\";if(value.startsWith("\\"))value=cwd.substring(0,2)+value;else if(!value.matches("[A-Za-z]:\\\\.*"))value=cwd+"\\"+value;String drive=value.substring(0,2).toUpperCase(Locale.ROOT);List<String> parts=new ArrayList<>();for(String p:value.substring(2).split("\\\\")){if(p.isEmpty()||p.equals("."))continue;if(p.equals("..")){if(!parts.isEmpty())parts.remove(parts.size()-1);}else parts.add(p);}return drive+"\\"+String.join("\\",parts);}
    private String parent(String path){int i=path.lastIndexOf('\\');return i<=2?path.substring(0,3):path.substring(0,i);}
    Result handle(String raw){String command=raw.trim();if(command.equalsIgnoreCase("cmd")){active=true;return Result.normal("Atelier Windows CMD simulé. cd, dir, mkdir, echo, type ; exit revient à Ubuntu.\n"+cwd+">\n");}
        if(command.toLowerCase(Locale.ROOT).startsWith("cmd /c ")){boolean previous=active;active=true;try{return handle(command.substring(7));}finally{active=previous;}}
        if(!active)return null;if(command.equalsIgnoreCase("exit")){active=false;return Result.normal("Retour à Ubuntu virtuel.\n");}
        int split=command.indexOf(' ');String verb=(split<0?command:command.substring(0,split)).toLowerCase(Locale.ROOT),arg=split<0?"":command.substring(split+1).trim();
        if(verb.equals("cd")||verb.equals("chdir")){if(arg.isEmpty())return Result.normal(cwd+"\n");if(arg.toLowerCase(Locale.ROOT).startsWith("/d "))arg=arg.substring(3);String p=path(arg);if(!dirs.contains(p))return Result.error("Le chemin d'accès spécifié est introuvable.");cwd=p;return Result.normal("");}
        if(verb.matches("[a-z]:")){String p=verb.toUpperCase(Locale.ROOT)+"\\";if(!dirs.contains(p))return Result.error("Lecteur absent");cwd=p;return Result.normal("");}
        if(verb.equals("dir")){String p=arg.isEmpty()?cwd:path(arg);if(!dirs.contains(p))return Result.error("Dossier introuvable");StringBuilder out=new StringBuilder("Répertoire de "+p+"\n");for(String d:dirs)if(!d.equals(p)&&parent(d).equalsIgnoreCase(p))out.append("<DIR> "+d.substring(d.lastIndexOf('\\')+1)+"\n");for(String f:files.keySet())if(parent(f).equalsIgnoreCase(p))out.append(f.substring(f.lastIndexOf('\\')+1)+"\n");return Result.normal(out.toString());}
        if(verb.equals("mkdir")||verb.equals("md")){if(arg.isEmpty())return Result.error("Nom de dossier requis");String p=path(arg);if(dirs.contains(p)||files.containsKey(p))return Result.error("Le chemin existe déjà");if(!dirs.contains(parent(p)))return Result.error("Parent absent");dirs.add(p);return Result.normal("");}
        if(verb.equals("echo")){int redirect=arg.indexOf('>');if(redirect<0)return Result.normal(arg+"\n");boolean append=arg.substring(redirect).startsWith(">>");String p=path(arg.substring(redirect+(append?2:1)).trim());if(!dirs.contains(parent(p))||dirs.contains(p))return Result.error("Destination invalide");files.put(p,(append?files.getOrDefault(p,""):"")+arg.substring(0,redirect).trim()+"\n");return Result.normal("");}
        if(verb.equals("type")){String p=path(arg);return files.containsKey(p)?Result.normal(files.get(p)):Result.error("Fichier introuvable");}
        if(verb.equals("cls"))return Result.clear();
        return Result.error("Atelier CMD : commande non prise en charge. Utilise cd, dir, mkdir, echo, type ou exit. Les ACL NTFS sont étudiées dans le quiz.");
    }
}
