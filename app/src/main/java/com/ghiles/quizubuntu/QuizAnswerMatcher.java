package com.ghiles.quizubuntu;
import java.util.Arrays;
/** Whitespace/quote tolerance without lowercasing case-sensitive paths or accepting fuzzy commands. */
final class QuizAnswerMatcher {
    static boolean matches(String answer,String... accepted) {
        if(answer==null||answer.trim().isEmpty())return false;
        for(String expected:accepted) {
            try {if(ShellSyntax.words(answer.trim()).equals(ShellSyntax.words(expected.trim())))return true;}
            catch(IllegalArgumentException ignored) { }
        }
        return false;
    }
}
