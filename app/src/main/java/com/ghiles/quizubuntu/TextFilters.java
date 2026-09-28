package com.ghiles.quizubuntu;
import java.util.*;
import java.util.regex.*;

/** Small, explicitly bounded text filters used by shell pipelines. */
final class TextFilters {
    static String printf(List<String> args) {
        if (args.size()<2) throw new IllegalArgumentException("printf: format manquant");
        String format=args.get(1).replace("\\n","\n").replace("\\t","\t").replace("\\r","\r").replace("\\\\","\\");
        StringBuilder out=new StringBuilder(); int argument=2;
        do {
            boolean consumed=false;
            for(int i=0;i<format.length();i++) {
                char c=format.charAt(i);
                if(c!='%') { out.append(c); continue; }
                if(++i>=format.length()) throw new IllegalArgumentException("printf: format incomplet");
                char type=format.charAt(i);
                if(type=='%') { out.append('%'); continue; }
                String value=argument<args.size()?args.get(argument++):""; consumed=true;
                if(type=='s') out.append(value);
                else if(type=='d') out.append(value.isEmpty()?0:Long.parseLong(value));
                else if(type=='b') out.append(value.replace("\\n","\n").replace("\\t","\t"));
                else throw new IllegalArgumentException("printf: format non pris en charge : %"+type);
            }
            if(!consumed) break;
        } while(argument<args.size());
        return out.toString();
    }
    static String transform(String name, List<String> args, String value) {
        List<String> lines=TextCommands.lines(value);
        if(name.equals("tac")) { Collections.reverse(lines); return String.join("\n",lines)+(lines.isEmpty()?"":"\n"); }
        if(name.equals("less")||name.equals("more")) return value;
        if(name.equals("uniq")) {
            boolean count=args.contains("-c"), repeated=args.contains("-d"), unique=args.contains("-u");
            StringBuilder out=new StringBuilder();
            for(int i=0;i<lines.size();) {
                String line=lines.get(i); int end=i+1;
                while(end<lines.size()&&line.equals(lines.get(end))) end++;
                int n=end-i;
                if((!repeated||n>1)&&(!unique||n==1)) out.append(count?String.format(Locale.ROOT,"%7d ",n):"").append(line).append('\n');
                i=end;
            }
            return out.toString();
        }
        if(name.equals("cut")) {
            String delimiter="\t",field=null; boolean suppress=false;
            for(int i=1;i<args.size();i++) {
                String a=args.get(i);
                if(a.equals("-s")) suppress=true;
                else if(a.startsWith("-d")) delimiter=a.length()>2?a.substring(2):args.get(++i);
                else if(a.startsWith("-f")) field=a.length()>2?a.substring(2):args.get(++i);
            }
            if(field==null||delimiter.length()!=1) throw new IllegalArgumentException("cut: utiliser -d CARACTERE -f NUMERO");
            int number=Integer.parseInt(field); if(number<1) throw new IllegalArgumentException("cut: champ positif requis");
            StringBuilder out=new StringBuilder();
            for(String line:lines) {
                if(!line.contains(delimiter)) { if(!suppress) out.append(line).append('\n'); continue; }
                String[] fields=line.split(Pattern.quote(delimiter),-1);
                out.append(number<=fields.length?fields[number-1]:"").append('\n');
            }
            return out.toString();
        }
        if(name.equals("tr")) {
            if(args.size()!=3) throw new IllegalArgumentException("tr: deux ensembles requis");
            String from=expand(args.get(1)),to=expand(args.get(2));
            if(to.isEmpty()) throw new IllegalArgumentException("tr: ensemble destination vide");
            StringBuilder out=new StringBuilder();
            for(char c:value.toCharArray()) { int i=from.indexOf(c); out.append(i<0?c:to.charAt(Math.min(i,to.length()-1))); }
            return out.toString();
        }
        if(name.equals("sed")) {
            boolean quiet=args.contains("-n"); String expression=args.get(quiet?2:1);
            Matcher range=Pattern.compile("(\\d+)(?:,(\\d+))?p").matcher(expression);
            if(range.matches()) {
                int start=Integer.parseInt(range.group(1)),end=range.group(2)==null?start:Integer.parseInt(range.group(2)); StringBuilder out=new StringBuilder();
                for(int i=0;i<lines.size();i++) { if(!quiet) out.append(lines.get(i)).append('\n'); if(i+1>=start&&i+1<=end) out.append(lines.get(i)).append('\n'); }
                return out.toString();
            }
            if(expression.startsWith("s/")) {
                String[] parts=expression.split("/",-1);
                if(parts.length!=4) throw new IllegalArgumentException("sed: substitution simple s/motif/texte/[g] requise");
                StringBuilder out=new StringBuilder();
                for(String line:lines) { String replacement=parts[3].equals("g")?line.replaceAll(parts[1],parts[2]):line.replaceFirst(parts[1],parts[2]); if(!quiet) out.append(replacement).append('\n'); }
                return out.toString();
            }
            throw new IllegalArgumentException("sed: expression non prise en charge");
        }
        if(name.equals("awk")) {
            Matcher field=Pattern.compile("\\{\\s*print\\s+\\$(\\d+)\\s*;?\\s*\\}").matcher(args.get(1));
            if(!field.matches()) throw new IllegalArgumentException("awk: seuls {print $N} et {print $0} sont pris en charge");
            int number=Integer.parseInt(field.group(1)); StringBuilder out=new StringBuilder();
            for(String line:lines) { String[] fields=line.trim().split("\\s+"); out.append(number==0?line:number<=fields.length?fields[number-1]:"").append('\n'); }
            return out.toString();
        }
        throw new IllegalArgumentException("Filtre inconnu");
    }
    static String expand(String value) {
        StringBuilder result=new StringBuilder();
        for(int i=0;i<value.length();i++) {
            if(i+2<value.length()&&value.charAt(i+1)=='-') { for(char c=value.charAt(i);c<=value.charAt(i+2);c++) result.append(c); i+=2; }
            else result.append(value.charAt(i));
        }
        return result.toString();
    }
}
