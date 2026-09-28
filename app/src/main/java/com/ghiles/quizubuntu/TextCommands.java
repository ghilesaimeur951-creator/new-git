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

    static VirtualMachine.Result run(List<String> words, Function<String,String> read) {
        String name = words.get(0), flags = "", pattern = null;
        List<String> paths = new ArrayList<>();
        int count = 10;
        boolean bytes = false, end = false;
        try {
            for (int i = 1; i < words.size(); i++) {
                String arg = words.get(i);
                if (!end && arg.equals("--")) { end = true; continue; }
                if (!end && arg.startsWith("-") && arg.length() > 1) {
                    if ((name.equals("head") || name.equals("tail")) && (arg.equals("-n") || arg.equals("-c"))) {
                        bytes = arg.equals("-c");
                        count = Integer.parseInt(words.get(++i));
                        if (count < 0) throw new IllegalArgumentException("Le nombre doit être positif ou nul.");
                    } else {
                        String allowed = name.equals("grep") ? "ivncxFwhHs" : name.equals("sort") ? "nrufbds" : name.equals("cat") ? "nbsETvA" : name.equals("wc") ? "lwcm" : "";
                        for (char c : arg.substring(1).toCharArray()) {
                            if (allowed.indexOf(c) < 0) throw new IllegalArgumentException("Option non prise en charge : " + arg);
                        }
                        flags += arg.substring(1);
                    }
                } else if (name.equals("grep") && pattern == null) pattern = arg;
                else paths.add(arg);
            }
            if (paths.isEmpty()) return VirtualMachine.Result.error(name + ": indique un fichier (ex. README.md).");
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
            return VirtualMachine.Result.normal(output.toString());
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
            for (char c : line.toCharArray()) {
                if (c == '\t' && flags.indexOf('T') >= 0) out.append("^I");
                else if (flags.indexOf('v') >= 0 && c < 32 && c != '\t') out.append('^').append((char)(c+64));
                else if (flags.indexOf('v') >= 0 && c == 127) out.append("^?");
                else out.append(c);
            }
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
        if (flags.indexOf('w') >= 0) counts.add(String.valueOf(value.trim().isEmpty() ? 0 : value.trim().split("(?U)\\s+").length));
        if (flags.indexOf('m') >= 0) counts.add(String.valueOf(value.codePointCount(0, value.length())));
        if (flags.indexOf('c') >= 0) counts.add(String.valueOf(value.getBytes(StandardCharsets.UTF_8).length));
        return String.join(" ", counts) + " " + path + "\n";
    }

    private static String key(String line, String flags) {
        if (flags.indexOf('b') >= 0) line = line.replaceFirst("^\\s+", "");
        if (flags.indexOf('d') >= 0) line = line.replaceAll("[^\\p{L}\\p{N}\\s]", "");
        if (flags.indexOf('f') >= 0) line = line.toUpperCase(Locale.ROOT);
        return line;
    }
    private static BigDecimal numeric(String value) {
        Matcher m = Pattern.compile("^\\s*([+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+))").matcher(value);
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
