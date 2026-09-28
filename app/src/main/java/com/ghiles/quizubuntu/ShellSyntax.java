package com.ghiles.quizubuntu;

import java.util.ArrayList;
import java.util.List;

/** Small quote-aware parser. No system shell or command substitution is executed. */
final class ShellSyntax {
    static boolean hasShortOption(String command, char option) {
        List<String> args = words(command);
        for (int i = 1; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.equals("--")) break;
            if (arg.startsWith("-") && !arg.startsWith("--") && arg.indexOf(option, 1) >= 0) return true;
        }
        return false;
    }

    static List<String> words(String input) {
        List<String> result = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        char quote = 0;
        boolean escaped = false, started = false;
        for (char c : input.toCharArray()) {
            if (escaped) { word.append(c); escaped = false; started = true; continue; }
            if (c == '\\' && quote != '\'') { escaped = true; started = true; continue; }
            if (quote != 0) {
                if (c == quote) quote = 0; else word.append(c);
                started = true;
            } else if (c == '\'' || c == '"') { quote = c; started = true; }
            else if (Character.isWhitespace(c)) {
                if (started) { result.add(word.toString()); word.setLength(0); started = false; }
            } else { word.append(c); started = true; }
        }
        if (quote != 0 || escaped) throw new IllegalArgumentException("Guillemet ou échappement non terminé.");
        if (started) result.add(word.toString());
        return result;
    }

    static List<String> split(String input, String operator) {
        List<String> result = new ArrayList<>();
        char quote = 0;
        boolean escaped = false;
        int start = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (c == '\\' && quote != '\'') { escaped = true; continue; }
            if (quote != 0) { if (c == quote) quote = 0; continue; }
            if (c == '\'' || c == '"') { quote = c; continue; }
            if (input.startsWith(operator, i)) {
                result.add(input.substring(start, i));
                i += operator.length() - 1;
                start = i + 1;
            }
        }
        result.add(input.substring(start));
        return result;
    }
}
