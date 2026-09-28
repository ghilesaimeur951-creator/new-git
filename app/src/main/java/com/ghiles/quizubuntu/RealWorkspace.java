package com.ghiles.quizubuntu;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

/** Files used by both the real terminal and JGit. Never executes a system shell. */
final class RealWorkspace {
    private final File root;
    private File cwd;
    RealWorkspace(File root) throws IOException {
        this.root = root.getCanonicalFile();
        Files.createDirectories(this.root.toPath());
        cwd = this.root;
    }
    File currentDirectory() { return cwd; }
    File root() { return root; }
    File repositoryDirectory() {
        for (File p = cwd; p != null && inside(p); p = p.getParentFile()) {
            if (new File(p, ".git").isDirectory()) return p;
        }
        return null;
    }
    private boolean inside(File file) {
        return file.equals(root) || file.getPath().startsWith(root.getPath() + File.separator);
    }
    File resolve(String path, boolean write) throws IOException {
        if (path.equals("~") || path.equals("~/github-real")) path = root.getPath();
        else if (path.startsWith("~/github-real/")) path = root.getPath() + path.substring(13);
        File input = new File(path);
        File target = (input.isAbsolute() ? input : new File(cwd, path)).getCanonicalFile();
        if (!inside(target)) throw new IOException("Chemin hors de l'espace Git réel.");
        if (write) {
            String relative = root.toPath().relativize(target.toPath()).toString().replace(File.separatorChar, '/');
            if (Arrays.asList(relative.split("/")).contains(".git")) throw new IOException("Modification directe de .git interdite.");
        }
        return target;
    }
    void changeDirectory(String path) throws IOException {
        File target = resolve(path, false);
        if (!target.isDirectory()) throw new IOException("cd: dossier absent : " + path);
        cwd = target;
    }
    String displayPath() { return "~/github-real" + root.toPath().relativize(cwd.toPath()).toString().replace(File.separatorChar, '/').replaceAll("^(.+)$", "/$1"); }
    String read(String path) throws IOException { return new String(Files.readAllBytes(resolve(path, false).toPath()), StandardCharsets.UTF_8); }
    void write(String path, String text, boolean append) throws IOException {
        File file = resolve(path, true);
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
            append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING);
    }
    String execute(String command) throws IOException {
        List<String> redirects = ShellSyntax.operators(command, ">>", ">");
        if (redirects.size() > 1) {
            if (redirects.size() != 3) throw new IOException("Une seule redirection de sortie est prise en charge.");
            List<String> names = ShellSyntax.words(redirects.get(2));
            if (names.size() != 1) throw new IOException("Destination de redirection manquante ou ambiguë.");
            String result = execute(redirects.get(0).trim());
            write(names.get(0), result, redirects.get(1).equals(">>"));
            return "";
        }
        List<String> args = ShellSyntax.words(command);
        if (args.isEmpty()) return "";
        String name = args.get(0);
        if (name.equals("pwd")) return displayPath() + "\n";
        if (name.equals("cd")) { if(args.size()>2)throw new IOException("cd: un seul chemin attendu"); changeDirectory(args.size()==1?"~":args.get(1)); return ""; }
        if (name.equals("mkdir") || name.equals("touch")) {
            boolean parents = args.contains("-p"); int count=0;
            for (int i=1;i<args.size();i++) {
                String arg=args.get(i); if(arg.equals("-p")&&name.equals("mkdir"))continue;
                if(arg.startsWith("-"))throw new IOException("Option non prise en charge : " + arg);
                File file=resolve(arg,true); count++;
                if(name.equals("mkdir")) { if(parents)Files.createDirectories(file.toPath());else Files.createDirectory(file.toPath()); }
                else if(!file.exists())Files.createFile(file.toPath());
            }
            if(count==0)throw new IOException(name+": chemin requis"); return "";
        }
        if (name.equals("echo")) {
            int start=args.size()>1&&args.get(1).equals("-n")?2:1;
            return String.join(" ",args.subList(start,args.size()))+(start==2?"":"\n");
        }
        if (name.equals("cat")) {
            if(args.size()<2)throw new IOException("cat: fichier requis");
            StringBuilder out=new StringBuilder();for(int i=1;i<args.size();i++)out.append(read(args.get(i)));return out.toString();
        }
        if (name.equals("ls")) {
            boolean all=false,longFormat=false;String path=".";
            for(int i=1;i<args.size();i++) {
                String arg=args.get(i);
                if(arg.startsWith("-")) { for(char flag:arg.substring(1).toCharArray()) {if("al1".indexOf(flag)<0)throw new IOException("ls: option non prise en charge");all|=flag=='a';longFormat|=flag=='l';} }
                else path=arg;
            }
            File target=resolve(path,false);if(!target.exists())throw new IOException("ls: chemin absent");
            File[] entries=target.isDirectory()?target.listFiles():new File[]{target};
            if(entries==null)throw new IOException("ls: dossier illisible");Arrays.sort(entries,(a,b)->a.getName().compareTo(b.getName()));
            StringBuilder out=new StringBuilder();if(all&&target.isDirectory())out.append(".\n..\n");
            for(File file:entries)if(all||!file.getName().startsWith(".")) {
                if(longFormat)out.append(file.isDirectory()?"drwxr-xr-x  ":"-rw-r--r--  ");
                out.append(file.getName()).append(file.isDirectory()?"/":"").append('\n');
            }
            return out.toString();
        }
        throw new IOException("Commande de fichiers réelle non prise en charge : " + name + ". Utilise mkdir, cd, touch, echo, cat, ls ou nano.");
    }
}
