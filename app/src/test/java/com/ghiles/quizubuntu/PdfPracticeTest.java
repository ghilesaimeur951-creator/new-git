package com.ghiles.quizubuntu;
import org.junit.Test;
import static org.junit.Assert.*;
public class PdfPracticeTest {
    @Test public void writtenMatchingRespectsCaseAndQuotedArguments() {
        assertTrue(QuizAnswerMatcher.matches(" git commit  -m 'Ajout' ","git commit -m \"Ajout\""));
        assertFalse(QuizAnswerMatcher.matches("git add readme.md","git add README.md"));
        assertFalse(QuizAnswerMatcher.matches("git push","git add README.md"));
        assertFalse(QuizAnswerMatcher.matches("","pwd"));
    }
    @Test public void eachLevelHasWrittenAndTrapExercisesAndValidAnswers() {
        for(int level=1;level<=5;level++) {
            boolean typed=false,trap=false;
            for(MainActivity.Question q:PdfPracticeBank.questions())if(q.level==level) {
                assertTrue(q.correctIndex>=0&&q.correctIndex<q.options.size());
                assertFalse(q.explanation.isEmpty());assertFalse(q.example.isEmpty());
                if(q.accepted!=null){typed=true;for(String answer:q.accepted)assertTrue(QuizAnswerMatcher.matches(answer,q.accepted));}
                trap|=q.options.get(q.correctIndex).equals("Aucune de ces réponses");
            }
            assertTrue("written level "+level,typed);assertTrue("trap level "+level,trap);
        }
    }
}
