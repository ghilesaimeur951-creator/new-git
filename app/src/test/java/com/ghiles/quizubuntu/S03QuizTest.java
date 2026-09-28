package com.ghiles.quizubuntu;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class S03QuizTest {
    @Test public void sixSourcesAndNewTopicsHaveWrittenAndExplainedExercises(){
        List<MainActivity.Question> questions=S03QuizBank.questions();assertEquals(61,questions.size());
        Set<String> categories=new HashSet<>();int written=0,traps=0;boolean docx=false;
        for(MainActivity.Question q:questions){categories.add(q.category);assertTrue(q.level>=1&&q.level<=5);assertTrue(q.correctIndex>=0&&q.correctIndex<q.options.size());assertTrue(q.explanation.startsWith("S03"));assertFalse(q.example.isEmpty());docx|=q.explanation.contains("DOCX");
            if(q.accepted!=null){written++;for(String answer:q.accepted)assertTrue(QuizAnswerMatcher.matches(answer,q.accepted));}
            if(q.options.get(q.correctIndex).equals("Aucune de ces réponses"))traps++;
        }
        assertEquals(new HashSet<>(Arrays.asList("Utilisateurs","Permissions","ACL et droits spéciaux","VirtualBox","Windows")),categories);
        assertTrue(written>=40);assertTrue(traps>=3);assertTrue(docx);
    }
}
