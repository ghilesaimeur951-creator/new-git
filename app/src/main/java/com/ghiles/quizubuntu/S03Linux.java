package com.ghiles.quizubuntu;

import java.util.*;
import com.ghiles.quizubuntu.VirtualMachine.Result;

/** Course S03 account and permission model. Never changes Android accounts. */
final class S03Linux {
    private final VirtualMachine vm;
    static final class User {
        final String name; final int uid; String primary, shell="/bin/bash";
        final Set<String> supplementary=new LinkedHashSet<>();
        User(String name,int uid,String group) {this.name=name;this.uid=uid;primary=group;}
    }
    final Map<String,User> users=new LinkedHashMap<>();
    final Map<String,Integer> groupIds=new LinkedHashMap<>();
    final Map<String,Map<String,Integer>> acls=new LinkedHashMap<>();
    private final Deque<String> logins=new ArrayDeque<>();
    String current="ubuntu";
    private int nextUid=1001,nextGid=1001;
    S03Linux(VirtualMachine vm) {this.vm=vm;}
    void reset() {
        users.clear();groupIds.clear();acls.clear();logins.clear();current="ubuntu";nextUid=nextGid=1001;
        groupIds.put("root",0);groupIds.put("sudo",27);groupIds.put("adm",4);groupIds.put("ubuntu",1000);
        users.put("root",new User("root",0,"root"));users.put("ubuntu",new User("ubuntu",1000,"ubuntu"));users.get("ubuntu").supplementary.addAll(Arrays.asList("sudo","adm"));
        for(String d:Arrays.asList("/etc","/root","/tmp","/usr/bin"))vm.ensureDir(d);
        for(String d:vm.directories) {vm.owners.put(d,d.startsWith("/home/ubuntu")?"ubuntu":"root");vm.groups.put(d,d.startsWith("/home/ubuntu")?"ubuntu":"root");}
        vm.permissionModes.put("/root","700");vm.permissionModes.put("/tmp","1777");
        vm.files.put("/usr/bin/whoami","S03_WHOAMI_BINARY");vm.permissionModes.put("/usr/bin/whoami","755");vm.owners.put("/usr/bin/whoami","root");vm.groups.put("/usr/bin/whoami","root");
        refreshAccounts();
    }
    void refreshAccounts() {
        StringBuilder passwd=new StringBuilder(), group=new StringBuilder(), shadow=new StringBuilder();
        for(User u:users.values()) {passwd.append(u.name+":x:"+u.uid+":"+groupIds.get(u.primary)+"::"+home(u.name)+":"+u.shell+"\n");shadow.append(u.name+":!:20000:0:99999:7:::\n");}
        for(Map.Entry<String,Integer> g:groupIds.entrySet()) {List<String> members=new ArrayList<>();for(User u:users.values())if(u.supplementary.contains(g.getKey()))members.add(u.name);group.append(g.getKey()+":x:"+g.getValue()+":"+String.join(",",members)+"\n");}
        vm.files.put("/etc/passwd",passwd.toString());vm.files.put("/etc/group",group.toString());vm.files.put("/etc/shadow",shadow.toString());vm.files.put("/etc/sudoers","root ALL=(ALL:ALL) ALL\n%sudo ALL=(ALL:ALL) ALL\n");
        for(String f:Arrays.asList("passwd","group","shadow","sudoers")) {String p="/etc/"+f;vm.owners.put(p,"root");vm.groups.put(p,"root");vm.permissionModes.put(p,f.equals("shadow")||f.equals("sudoers")?"600":"644");}
    }
    String home(String user) {return user.equals("root")?"/root":"/home/"+user;}
    int mode(String p) {return Integer.parseInt(vm.permissionModes.getOrDefault(p,vm.directories.contains(p)?"755":"644"),8);}
    String owner(String p) {return vm.owners.getOrDefault(p,"ubuntu");}
    String group(String p) {return vm.groups.getOrDefault(p,"ubuntu");}
    boolean member(String name) {User u=users.get(current);return u.primary.equals(name)||u.supplementary.contains(name);}
    boolean allowed(String path,int bits) {
        if(current.equals("root"))return true;
        int m=mode(path);if(owner(path).equals(current))return ((m>>6)&bits)==bits;
        Map<String,Integer> acl=acls.getOrDefault(path,Collections.emptyMap());int mask=acl.getOrDefault("m:",(m>>3)&7);
        if(acl.containsKey("u:"+current))return (acl.get("u:"+current)&mask&bits)==bits;
        int granted=0;boolean matched=false;
        if(member(group(path))) {matched=true;granted=acl.getOrDefault("g:",(m>>3)&7);}
        for(Map.Entry<String,Integer> e:acl.entrySet())if(e.getKey().startsWith("g:")&&e.getKey().length()>2&&member(e.getKey().substring(2))) {matched=true;granted|=e.getValue();}
        return matched ? (granted&mask&bits)==bits : (m&bits)==bits;
    }
    void require(String path,int bits) {
        for(String p=vm.parent(path);!p.equals("/");p=vm.parent(p))if(vm.directories.contains(p)&&!allowed(p,1))throw new IllegalArgumentException("Permission denied : "+p+" (traversée x requise)");
        if(!allowed(path,bits))throw new IllegalArgumentException("Permission denied : "+path);
    }
    void writable(String p) {if(vm.files.containsKey(p))require(p,2);else require(vm.parent(p),3);}
    void created(Set<String> before) {
        Set<String> now=new LinkedHashSet<>(vm.directories);now.addAll(vm.files.keySet());
        for(String p:now)if(!before.contains(p)) {
            String parent=vm.parent(p);boolean sgid=(mode(parent)&02000)!=0;
            vm.owners.putIfAbsent(p,current);vm.groups.putIfAbsent(p,sgid?group(parent):users.get(current).primary);
            int m=(vm.directories.contains(p)?0777:0666)&~Integer.parseInt(vm.umask,8);
            if(vm.files.containsKey(p)&&"S03_WHOAMI_BINARY".equals(vm.files.get(p)))m=0755;
            if(sgid&&vm.directories.contains(p))m|=02000;
            vm.permissionModes.putIfAbsent(p,Integer.toOctalString(m));
            if(sgid&&vm.directories.contains(p))vm.permissionModes.put(p,Integer.toOctalString(mode(p)|02000));
        }
        vm.owners.keySet().retainAll(now);vm.groups.keySet().retainAll(now);vm.permissionModes.keySet().retainAll(now);acls.keySet().retainAll(now);
    }
    Result handle(String raw,List<String> a) {
        if(a.isEmpty())return null;String cmd=a.get(0);
        if(cmd.equals("sudo")) {
            if(!current.equals("root")&&!member("sudo"))return Result.error("sudo: "+current+" n'est pas autorisé dans sudoers");
            if(a.size()==2&&a.get(1).equals("-i")){login("root",true);return Result.normal("Session root virtuelle. exit pour revenir.");}
            if(a.size()<2)return Result.error("sudo: commande requise");
            String previous=current;current="root";try{return vm.execute(raw.substring(raw.indexOf(' ')+1));}finally{current=previous;}
        }
        if(cmd.equals("whoami"))return Result.normal(current+"\n");
        if(cmd.equals("su")) {String user=a.size()==1?"root":a.get(a.size()-1);if(!users.containsKey(user))return Result.error("su: utilisateur absent");login(user,a.contains("-"));return Result.normal("Identité virtuelle : "+current+" (authentification non simulée).\n");}
        if(cmd.equals("exit")) {if(logins.isEmpty())return Result.normal("Aucune session su active.");String[] previous=logins.pop().split("\\|",2);current=previous[0];vm.cwd=previous[1];vm.environment.put("PWD",vm.cwd);updateEnvironment();return Result.normal("Retour à "+current+"\n");}
        if(cmd.equals("id")||cmd.equals("groups")) {
            String name=current;for(int i=1;i<a.size();i++)if(!a.get(i).startsWith("-"))name=a.get(i);
            User u=users.get(name);if(u==null)return Result.error("id: utilisateur inconnu : "+name);
            if(a.contains("-u"))return Result.normal(u.uid+"\n");if(a.contains("-g"))return Result.normal(groupIds.get(u.primary)+"\n");
            List<String> names=new ArrayList<>();names.add(u.primary);for(String g:u.supplementary)if(!names.contains(g))names.add(g);
            if(cmd.equals("groups"))return Result.normal(String.join(" ",names)+"\n");
            List<String> ids=new ArrayList<>();for(String g:names)ids.add(groupIds.get(g)+"("+g+")");
            return Result.normal("uid="+u.uid+"("+name+") gid="+groupIds.get(u.primary)+"("+u.primary+") groups="+String.join(",",ids)+"\n");
        }
        if(Arrays.asList("useradd","adduser","groupadd","addgroup","usermod","passwd").contains(cmd)) {
            if(!current.equals("root"))return Result.error(cmd+": Permission denied ; utilise sudo ou sudo -i dans cet atelier.");
            if(a.size()<2)return Result.error(cmd+": nom requis");String name=a.get(a.size()-1);
            if(!name.matches("[a-z_][a-z0-9_-]*"))return Result.error("Nom invalide");
            if(cmd.equals("groupadd")||cmd.equals("addgroup")) {if(groupIds.containsKey(name))return Result.error("Groupe déjà présent");groupIds.put(name,nextGid++);}
            else if(cmd.equals("useradd")||cmd.equals("adduser")) {
                if(users.containsKey(name))return Result.error("Utilisateur déjà présent");
                for(int i=1;i<a.size()-1;i++){String flag=a.get(i);if(flag.equals("-m"))continue;if(flag.equals("-s")){i++;continue;}return Result.error("useradd: option non prise en charge : "+flag);}
                if(!groupIds.containsKey(name))groupIds.put(name,nextGid++);
                User u=new User(name,nextUid++,name);if(a.contains("-s"))u.shell=a.get(a.indexOf("-s")+1);users.put(name,u);
                if(a.contains("-m")||cmd.equals("adduser")){String h=home(name);vm.ensureDir(h);vm.owners.put(h,name);vm.groups.put(h,name);vm.permissionModes.put(h,"755");}
            } else if(cmd.equals("usermod")) {
                User u=users.get(name);if(u==null)return Result.error("Utilisateur absent");
                boolean append=a.contains("-a")||a.contains("-aG");int index=a.indexOf("-G");if(index<0)index=a.indexOf("-aG");
                if(index<0||index+1>=a.size()-1)return Result.error("Usage : usermod [-a] -G groupe1,groupe2 utilisateur");
                String[] targets=a.get(index+1).split(",");for(String g:targets)if(!groupIds.containsKey(g))return Result.error("Groupe absent : "+g);
                if(!append)u.supplementary.clear();u.supplementary.addAll(Arrays.asList(targets));
            } else {if(!users.containsKey(name))return Result.error("Utilisateur absent");return Result.normal("passwd : étape de changement de mot de passe reconnue. Cet atelier ne demande ni ne stocke de mot de passe ; sur Linux, deux saisies masquées sont nécessaires.\n");}
            refreshAccounts();return Result.normal(cmd.equals("adduser")?"Compte virtuel créé. Le questionnaire interactif adduser est abrégé dans cet atelier.\n":"");
        }
        if(Arrays.asList("chmod","chown","chgrp").contains(cmd))return changePermissions(a);
        if(cmd.equals("getfacl")||cmd.equals("setfacl"))return acl(a);
        if(cmd.equals("visudo"))return Result.normal("Atelier : consulte sudo cat /etc/sudoers. L'édition de sudoers n'est pas proposée dans ce cours.\n");
        if(cmd.equals("cd")) {String target=a.size()==1?home(current):a.get(1).equals("-")?vm.previousCwd:vm.resolve(a.get(1));if(!vm.directories.contains(target))return Result.error("cd: dossier absent");require(target,1);vm.previousCwd=vm.cwd;vm.cwd=target;vm.environment.put("PWD",target);return Result.normal("");}
        if(cmd.startsWith("./")||cmd.startsWith("/")) {String p=vm.resolve(cmd);if("S03_WHOAMI_BINARY".equals(vm.files.get(p))){require(p,1);return Result.normal(((mode(p)&04000)!=0?owner(p):current)+"\n");}}
        if(cmd.equals("ls"))for(int i=1;i<a.size();i++)if(!a.get(i).startsWith("-")){String p=vm.resolve(a.get(i));if(vm.directories.contains(p))require(p,4);} 
        if(cmd.equals("ls")&&a.stream().skip(1).noneMatch(x->!x.startsWith("-")))require(vm.cwd,4);
        if(cmd.equals("touch")||cmd.equals("mkdir")||cmd.equals("rm")||cmd.equals("rmdir"))for(int i=1;i<a.size();i++)if(!a.get(i).startsWith("-")) {
            String p=vm.resolve(a.get(i));if(cmd.equals("touch"))writable(p);
            else if(cmd.equals("mkdir")){String parent=vm.parent(p);while(!vm.directories.contains(parent))parent=vm.parent(parent);require(parent,3);}
            else {String parent=vm.parent(p);require(parent,3);if((mode(parent)&01000)!=0&&!current.equals("root")&&!owner(p).equals(current)&&!owner(parent).equals(current))throw new IllegalArgumentException("Operation not permitted : sticky bit protège "+p);}
        }
        if((cmd.equals("cp")||cmd.equals("mv"))&&a.size()>=3) {
            String source=vm.resolve(a.get(a.size()-2)),target=vm.resolve(a.get(a.size()-1));
            if(vm.directories.contains(target))target+="/"+source.substring(source.lastIndexOf('/')+1);
            writable(target);
            if(cmd.equals("mv")) {require(vm.parent(source),3);if((mode(vm.parent(source))&01000)!=0&&!current.equals("root")&&!owner(source).equals(current)&&!owner(vm.parent(source)).equals(current))return Result.error("Operation not permitted : sticky bit");}
        }
        if(cmd.equals("nano")&&a.size()>1)writable(vm.resolve(a.get(1)));
        return null;
    }
    private void login(String name,boolean changeHome){logins.push(current+"|"+vm.cwd);current=name;updateEnvironment();if(changeHome){vm.previousCwd=vm.cwd;vm.cwd=home(name);vm.environment.put("PWD",vm.cwd);}}
    private void updateEnvironment(){vm.environment.put("USER",current);vm.environment.put("HOME",home(current));vm.environment.put("LOGNAME",current);}
    private Result changePermissions(List<String> a) {
        String cmd=a.get(0);int i=1;boolean recursive=i<a.size()&&a.get(i).equals("-R");if(recursive)i++;
        if(a.size()<i+2)return Result.error(cmd+": mode/propriétaire et chemin requis");String value=a.get(i++);
        for(;i<a.size();i++) {
            String p=vm.resolve(a.get(i));if(!vm.files.containsKey(p)&&!vm.directories.contains(p))return Result.error(cmd+": fichier absent");
            Set<String> paths=new LinkedHashSet<>();paths.add(p);if(recursive){for(String f:vm.files.keySet())if(f.startsWith(p+"/"))paths.add(f);for(String d:vm.directories)if(d.startsWith(p+"/"))paths.add(d);}
            for(String path:paths){require(vm.parent(path),1);if(!current.equals("root")&&!owner(path).equals(current))return Result.error(cmd+": Operation not permitted");
                if(cmd.equals("chmod")){int m=symbolic(mode(path),value,vm.directories.contains(path),Integer.parseInt(vm.umask,8));vm.permissionModes.put(path,Integer.toOctalString(m));if(acls.containsKey(path))acls.get(path).put("m:",(m>>3)&7);}
                else if(cmd.equals("chgrp")){if(!groupIds.containsKey(value))return Result.error("chgrp: groupe absent");if(!current.equals("root")&&!member(value))return Result.error("chgrp: Operation not permitted");vm.groups.put(path,value);}
                else {String[] pair=value.split(":",-1);if(!users.containsKey(pair[0]))return Result.error("chown: utilisateur absent");if(!current.equals("root")&&(!pair[0].equals(current)||(pair.length>1&&!member(pair[1]))))return Result.error("chown: utilise sudo");if(pair.length>1&&!groupIds.containsKey(pair[1]))return Result.error("chown: groupe absent");vm.owners.put(path,pair[0]);if(pair.length>1)vm.groups.put(path,pair[1]);}
            }
        }
        return Result.normal("");
    }
    static int symbolic(int mode,String value,boolean directory,int mask) {
        if(value.matches("[0-7]{3,4}"))return Integer.parseInt(value,8);
        for(String part:value.split(",",-1)) {
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("([ugoa]*)([+=-])([rwxXst]*)").matcher(part);
            if(!m.matches())throw new IllegalArgumentException("chmod: mode symbolique invalide : "+part);
            String who=m.group(1),perms=m.group(3);boolean implicit=who.isEmpty();if(implicit||who.contains("a"))who="ugo";
            int affected=0,bits=0;for(char c:who.toCharArray()){int shift=c=='u'?6:c=='g'?3:0;affected|=7<<shift;int b=0;if(perms.contains("r"))b|=4;if(perms.contains("w"))b|=2;if(perms.contains("x")||(perms.contains("X")&&(directory||(mode&0111)!=0)))b|=1;bits|=b<<shift;
                if(c=='u'){affected|=04000;if(perms.contains("s"))bits|=04000;}if(c=='g'){affected|=02000;if(perms.contains("s"))bits|=02000;}if(c=='o'){affected|=01000;if(perms.contains("t"))bits|=01000;}}
            if(implicit){affected&=~mask;bits&=~mask;}
            char op=m.group(2).charAt(0);mode=op=='+'?mode|bits:op=='-'?mode&~bits:(mode&~affected)|bits;
        }return mode;
    }
    static String rwx(int bits){return ""+((bits&4)!=0?'r':'-')+((bits&2)!=0?'w':'-')+((bits&1)!=0?'x':'-');}
    private int bits(String s){if(s.matches("[0-7]"))return Integer.parseInt(s,8);if(!s.matches("[rwx-]+"))throw new IllegalArgumentException("ACL: permissions invalides");return(s.contains("r")?4:0)|(s.contains("w")?2:0)|(s.contains("x")?1:0);}
    private Result acl(List<String> a){
        if(a.size()<2)return Result.error("ACL: chemin requis");String p=vm.resolve(a.get(a.size()-1));if(!vm.files.containsKey(p)&&!vm.directories.contains(p))return Result.error("ACL: fichier absent");
        if(a.get(0).equals("getfacl")){require(vm.parent(p),1);Map<String,Integer> acl=acls.getOrDefault(p,Collections.emptyMap());StringBuilder out=new StringBuilder("# file: "+a.get(a.size()-1)+"\n# owner: "+owner(p)+"\n# group: "+group(p)+"\nuser::"+rwx(mode(p)>>6)+"\n");
            for(Map.Entry<String,Integer> e:acl.entrySet())if(e.getKey().startsWith("u:"))out.append("user:"+e.getKey().substring(2)+":"+rwx(e.getValue())+"\n");
            out.append("group::"+rwx(acl.getOrDefault("g:",(mode(p)>>3)&7))+"\n");for(Map.Entry<String,Integer> e:acl.entrySet())if(e.getKey().startsWith("g:")&&e.getKey().length()>2)out.append("group:"+e.getKey().substring(2)+":"+rwx(e.getValue())+"\n");if(!acl.isEmpty())out.append("mask::"+rwx(acl.getOrDefault("m:",(mode(p)>>3)&7))+"\n");return Result.normal(out+"other::"+rwx(mode(p))+"\n");}
        if(!current.equals("root")&&!owner(p).equals(current))return Result.error("setfacl: Operation not permitted");
        if(a.size()!=4||(!a.get(1).equals("-m")&&!a.get(1).equals("-x")))return Result.error("Usage : setfacl -m u:nom:r fichier | setfacl -x u:nom fichier");
        String[] spec=a.get(2).split(":",-1);boolean modify=a.get(1).equals("-m");if(spec.length!=(modify?3:2)||(!spec[0].equals("u")&&!spec[0].equals("g"))||spec[1].isEmpty())return Result.error("ACL nommée utilisateur/groupe attendue");
        if(spec[0].equals("u")?!users.containsKey(spec[1]):!groupIds.containsKey(spec[1]))return Result.error("ACL: utilisateur/groupe absent");
        int requestedBits=modify?bits(spec[2]):0;
        Map<String,Integer> acl=acls.computeIfAbsent(p,x->new LinkedHashMap<>());acl.putIfAbsent("g:",(mode(p)>>3)&7);String key=spec[0]+":"+spec[1];if(modify)acl.put(key,requestedBits);else acl.remove(key);
        int mask=acl.get("g:");for(Map.Entry<String,Integer> e:acl.entrySet())if(!e.getKey().equals("m:"))mask|=e.getValue();acl.put("m:",mask);vm.permissionModes.put(p,Integer.toOctalString((mode(p)&~0070)|(mask<<3)));return Result.normal("");
    }
}
