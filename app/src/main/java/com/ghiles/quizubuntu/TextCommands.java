package com.ghiles.quizubuntu;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.*;
import java.util.function.Function;

/** Executable text operations on virtual files; never documentary success messages. */
final class TextCommands {
    static boolean handles(String name) {
        return Arrays.asList("grep", "sort", "cat", "wc", "head", "tail").contains(name);
    }

    static VirtualMachine.Result run(List<String> words, Function<String,String> read) { return run(words, read, null); }
    static VirtualMachine.Result run(List<String> words, Function<String,String> fileReader, String stdin) {
        Function<String,String> read = path -> path.equals("-") ? (stdin == null ? "" : stdin) : fileReader.apply(path);
        String name = words.get(0), flags = "", pattern = null;
        List<String> paths = new ArrayList<>();
        int count = 10;
        boolean bytes = false, end = false;
        try {
            for (int i = 1; i < words.size(); i++) {
                String arg = words.get(i);
                if (!end && arg.equals("--")) { end = true; continue; }
                if (!end && arg.startsWith("-") && arg.length() > 1) {
                    if ((name.equals("head") || name.equals("tail")) && arg.matches("-[0-9]+")) {
                        count = Integer.parseInt(arg.substring(1));
                    } else if ((name.equals("head") || name.equals("tail")) && (arg.equals("-n") || arg.equals("-c"))) {
                        bytes = arg.equals("-c");
                        count = Integer.parseInt(words.get(++i));
                        if (count < 0) throw new IllegalArgumentException("Le nombre doit être positif ou nul.");
                    } else {
                        String allowed = name.equals("grep") ? "ivncxFwhHsElLq" : name.equals("sort") ? "nrufbds" : name.equals("cat") ? "nbsETvA" : name.equals("wc") ? "lwcm" : "";
                        for (char c : arg.substring(1).toCharArray()) {
                            if (allowed.indexOf(c) < 0) throw new IllegalArgumentException("Option non prise en charge : " + arg);
                        }
                        flags += arg.substring(1);
                    }
                } else if (name.equals("grep") && pattern == null) pattern = arg;
                else paths.add(arg);
            }
            if (paths.isEmpty()) paths.add("-");
            if (name.equals("sort") && flags.indexOf('n') >= 0 && flags.indexOf('d') >= 0) return VirtualMachine.Result.error("sort: options -n et -d incompatibles").withStatus(2);
            if (name.equals("grep") && pattern == null) return VirtualMachine.Result.error("grep : motif manquant");
            StringBuilder output = new StringBuilder();
            for (String path : paths) {
                String value = read.apply(path);
                if (value == null) return VirtualMachine.Result.error(name + ": " + path + ": Aucun fichier" + (flags.indexOf('s') >= 0 ? "" : " dans le dossier courant"));
                switch (name) {
                    case "grep": output.append(grep(value, pattern, flags, path, paths.size() > 1)); break;
                    case "cat": output.append(cat(value, flags)); break;
                    case "wc": output.append(wc(value, flags, path)); break;
                    case "head": case "tail":
                        if (bytes) {
                            byte[] data = value.getBytes(StandardCharsets.UTF_8);
                            int length = Math.min(count, data.length);
                            output.append(new String(data, name.equals("head") ? 0 : data.length - length, length, StandardCharsets.UTF_8));
                        } else {
                            List<String> lines = lines(value);
                            int from = name.equals("head") ? 0 : Math.max(0, lines.size() - count);
                            int to = name.equals("head") ? Math.min(count, lines.size()) : lines.size();
                            for (int j = from; j < to; j++) output.append(lines.get(j)).append(j < lines.size()-1 || value.endsWith("\n") ? "\n" : "");
                        }
                        break;
                    case "sort": break;
                }
            }
            if (name.equals("sort")) {
                List<String> all = new ArrayList<>();
                for (String path : paths) all.addAll(lines(read.apply(path)));
                output.append(sort(all, flags));
            }
            int status = 0;
            if (name.equals("grep")) {
                // Count selected lines independently of -c/-H output formatting.
                String countFlags = flags.replace("H", "").replace("h", "").replace("l", "").replace("L", "").replace("q", "") + "c";
                int selected = 0;
                for (String path : paths) selected += Integer.parseInt(grep(read.apply(path), pattern, countFlags, path, false).trim());
                if (selected == 0) status = 1;
            }
            return VirtualMachine.Result.status(output.toString(), status);
        } catch (RuntimeException ex) {
            return VirtualMachine.Result.error(name + ": " + (ex.getMessage() == null ? "arguments invalides" : ex.getMessage()));
        }
    }

    static List<String> lines(String value) {
        if (value.isEmpty()) return new ArrayList<>();
        List<String> result = new ArrayList<>(Arrays.asList(value.split("\n", -1)));
        if (value.endsWith("\n")) result.remove(result.size()-1);
        return result;
    }

    private static String grep(String value, String needle, String flags, String path, boolean multiple) {
        String expression = flags.indexOf('F') >= 0 ? Pattern.quote(needle) : needle;
        if (flags.indexOf('F') < 0 && flags.indexOf('E') < 0) {
            // GNU basic regex: + ? | ( ) { } are literals unless backslash-escaped.
            StringBuilder basic = new StringBuilder();
            for (int i=0; i<expression.length(); i++) {
                char ch=expression.charAt(i);
                if(ch=='\\' && i+1<expression.length() && "+?|(){}".indexOf(expression.charAt(i+1))>=0) basic.append(expression.charAt(++i));
                else { if("+?|(){}".indexOf(ch)>=0) basic.append('\\'); basic.append(ch); }
            }
            expression=basic.toString();
        }
        if (flags.indexOf('w') >= 0) expression = "(?<![\\p{L}\\p{N}_])(?:" + expression + ")(?![\\p{L}\\p{N}_])";
        Pattern p = Pattern.compile(expression, flags.indexOf('i') >= 0 ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0);
        StringBuilder out = new StringBuilder();
        int n = 0, matches = 0;
        boolean filename = flags.indexOf('H') >= 0 || (multiple && flags.indexOf('h') < 0);
        for (String line : lines(value)) {
            n++;
            Matcher m = p.matcher(line);
            boolean match = flags.indexOf('x') >= 0 ? m.matches() : m.find();
            if (flags.indexOf('v') >= 0) match = !match;
            if (!match) continue;
            matches++;
            if (flags.indexOf('c') >= 0) continue;
            if (filename) out.append(path).append(':');
            if (flags.indexOf('n') >= 0) out.append(n).append(':');
            out.append(line).append('\n');
        }
        if (flags.indexOf('q') >= 0) return "";
        if (flags.indexOf('l') >= 0) return matches > 0 ? path + "\n" : "";
        if (flags.indexOf('L') >= 0) return matches == 0 ? path + "\n" : "";
        if (flags.indexOf('c') >= 0) return (filename ? path + ":" : "") + matches + "\n";
        return out.toString();
    }

    private static String cat(String value, String flags) {
        if (flags.indexOf('A') >= 0) flags += "vET";
        StringBuilder out = new StringBuilder();
        int number = 0;
        boolean previousBlank = false;
        List<String> lines = lines(value);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            boolean blank = line.isEmpty();
            if (blank && previousBlank && flags.indexOf('s') >= 0) continue;
            previousBlank = blank;
            if ((flags.indexOf('b') >= 0 && !blank) || (flags.indexOf('b') < 0 && flags.indexOf('n') >= 0)) out.append(String.format(Locale.ROOT, "%6d\t", ++number));
            if (flags.indexOf('v') >= 0) {
                for (byte octet : line.getBytes(StandardCharsets.UTF_8)) {
                    int c=octet & 255;
                    boolean meta=c>=128;
                    if(meta) { out.append("M-"); c-=128; }
                    if(c==9 && (flags.indexOf('T')>=0 || meta)) out.append("^I");
                    else if(c<32 && c!=9) out.append('^').append((char)(c+64));
                    else if(c==127) out.append("^?");
                    else out.append((char)c);
                }
            } else out.append(flags.indexOf('T')>=0 ? line.replace("\t","^I") : line);
            boolean newline = i < lines.size()-1 || value.endsWith("\n");
            if (newline && flags.indexOf('E') >= 0) out.append('$');
            if (newline) out.append('\n');
        }
        return out.toString();
    }

    private static String wc(String value, String flags, String path) {
        if (flags.isEmpty()) flags = "lwc";
        List<String> counts = new ArrayList<>();
        if (flags.indexOf('l') >= 0) counts.add(String.valueOf(value.chars().filter(c -> c == '\n').count()));
        if (flags.indexOf('w') >= 0) {
            int words=0;
            for(String token:value.split("(?U)\\s+")) if(token.codePoints().anyMatch(c -> !Character.isISOControl(c) && !Character.isWhitespace(c))) words++;
            counts.add(String.valueOf(words));
        }
        if (flags.indexOf('m') >= 0) counts.add(String.valueOf(value.codePointCount(0, value.length())));
        if (flags.indexOf('c') >= 0) counts.add(String.valueOf(value.getBytes(StandardCharsets.UTF_8).length));
        if(counts.size()>1 && !path.equals("-")) {
            int width=String.valueOf(value.getBytes(StandardCharsets.UTF_8).length).length();
            for(int i=0;i<counts.size();i++) counts.set(i,String.format(Locale.ROOT,"%"+width+"s",counts.get(i)));
        }
        return String.join(" ", counts) + (path.equals("-") ? "" : " " + path) + "\n";
    }

    private static String key(String line, String flags) {
        if (flags.indexOf('b') >= 0) line = line.replaceFirst("^\\s+", "");
        if (flags.indexOf('d') >= 0) line = line.replaceAll("[^A-Za-z0-9 \t]", "");
        if (flags.indexOf('f') >= 0) {
            StringBuilder folded=new StringBuilder();
            for(char c:line.toCharArray()) folded.append(c>='a'&&c<='z'?(char)(c-32):c);
            line=folded.toString();
        }
        return line;
    }
    private static BigDecimal numeric(String value) {
        Matcher m = Pattern.compile("^\\s*(-?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+))").matcher(value);
        return m.find() ? new BigDecimal(m.group(1)) : BigDecimal.ZERO;
    }
    private static String sort(List<String> lines, String flags) {
        Comparator<String> compareKey = (a,b) -> flags.indexOf('n') >= 0 ? numeric(key(a,flags)).compareTo(numeric(key(b,flags))) : key(a,flags).compareTo(key(b,flags));
        Comparator<String> order = (a,b) -> {
            int result = compareKey.compare(a,b);
            if (result == 0 && flags.indexOf('s') < 0 && flags.indexOf('u') < 0) result = a.compareTo(b);
            return flags.indexOf('r') >= 0 ? -result : result;
        };
        lines.sort(order);
        StringBuilder out = new StringBuilder();
        String previous = null;
        for (String line : lines) {
            if (flags.indexOf('u') < 0 || previous == null || compareKey.compare(previous, line) != 0) out.append(line).append('\n');
            previous = line;
        }
        return out.toString();
    }
}
