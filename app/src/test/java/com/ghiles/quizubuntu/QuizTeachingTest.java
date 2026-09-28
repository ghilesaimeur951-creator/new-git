package com.ghiles.quizubuntu;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class QuizTeachingTest {
    private static Object field(Object obj,String name)throws Exception {
        Field f=obj.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(obj);
    }
    private static void call(Object obj,String name)throws Exception {
        Method m=obj.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(obj);
    }
    private static MainActivity.Question find(List<MainActivity.Question> bank,String text) {
        for(MainActivity.Question q:bank)if(q.text.startsWith(text))return q;
        throw new AssertionError("Question absente : "+text);
    }
    private static boolean hasButton(View root,String label) {
        if(root instanceof Button && ((Button)root).getText().toString().contains(label))return true;
        if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)
            if(hasButton(((ViewGroup)root).getChildAt(i),label))return true;
        return false;
    }
    @SuppressWarnings("unchecked")
    @Test public void everyQuestionHasAReasonAndAnExpandableWorkedExample()throws Exception {
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity app=controller.get();
        List<MainActivity.Question> bank=(List<MainActivity.Question>)field(app,"questions");
        assertTrue(bank.size()>100);
        for(MainActivity.Question q:bank){
            String simple=QuizTeaching.simple(q,"essai",false);
            String details=QuizTeaching.details(q,"essai",false);
            assertTrue(q.text,simple.contains(QuizTeaching.answer(q)));
            assertTrue(q.text,details.contains(QuizPrimer.forQuestion(q).question));
            assertTrue(q.text,details.contains(q.example));
            assertTrue(q.text,details.contains("Pourquoi revoir ta réponse"));
            if (q.accepted==null && q.options.size()>1)
                for(String option:q.options)assertTrue(q.text+" / "+option,details.contains("• "+option+" : "));
        }
        MainActivity.Question permission=find(bank,"Dans -rw-r--r--");
        assertTrue(QuizTeaching.simple(permission,"1",false).contains("groupe a r--"));
        assertTrue(QuizTeaching.details(permission,"1",false).contains("0 droit d’écriture"));
        assertTrue(QuizTeaching.details(permission,"1",false).contains("Cette ligne vient de ls -l"));
        assertFalse(QuizTeaching.details(permission,"1",false).contains("Dans le cours : Droits des utilisateurs"));
        assertTrue(QuizTeaching.simple(find(bank,"Donne rwx au propriétaire, r au groupe"),"",true).contains("740"));
        controller.pause().stop().destroy();
    }
    @SuppressWarnings("unchecked")
    @Test public void beginnerPrimerLabelsRealCommandFileOutputAndMasksResult()throws Exception {
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        List<MainActivity.Question> bank=(List<MainActivity.Question>)field(controller.get(),"questions");
        for(MainActivity.Question q:bank) {
            QuizPrimer p=QuizPrimer.forQuestion(q);
            assertFalse(q.text,p.context.trim().isEmpty());
            assertFalse(q.text,p.question.trim().isEmpty());
            if (q.format.equals("Prédire le résultat")||q.format.equals("Lire une sortie")) {
                assertFalse(q.text,p.command.trim().isEmpty());
                assertFalse(q.text,p.output.trim().isEmpty());
                assertFalse(q.text,p.focus.trim().isEmpty());
            }
        }
        MainActivity.Question passwd=find(bank,"Dans /etc/passwd, x");
        QuizPrimer p=QuizPrimer.forQuestion(passwd);
        assertEquals("cat /etc/passwd",p.command);
        assertTrue(p.output.contains("alice:x:1000:1000"));
        assertTrue(p.focus.contains("deuxième"));
        assertTrue(p.context.contains("FICHIER"));
        assertTrue(QuizTeaching.details(passwd,"Oui",false).contains("Le x n’est pas un mot de passe lisible"));
        MainActivity.Question mask=find(bank,"Avec umask 077");
        QuizPrimer masked=QuizPrimer.forQuestion(mask);
        assertTrue(masked.command.contains("ls -l secret.txt"));
        assertFalse(masked.output.contains("600"));
        assertTrue(masked.output.contains("?"));
        MainActivity.Question permissions=find(bank,"Dans -rw-r--r--");
        assertTrue(QuizPrimer.forQuestion(permissions).command.contains("ls -l notes.txt"));
        assertTrue(QuizPrimer.forQuestion(permissions).focus.contains("r--"));
        assertFalse(QuizPrimer.forQuestion(permissions).context.contains("0 droit d’écriture"));
        MainActivity.Question written=find(bank,"Affiche les comptes Linux avec cat");
        assertFalse(QuizPrimer.forQuestion(written).context.contains("cat /etc/passwd"));
        controller.pause().stop().destroy();
    }
    @SuppressWarnings("unchecked")
    @Test public void explanationAppearsAfterAnswerAndCanBeOpenedWithoutChangingGrade()throws Exception {
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity app=controller.get();
        MainActivity.Question q=find((List<MainActivity.Question>)field(app,"questions"),"Dans -rw-r--r--");
        Field quiz=MainActivity.class.getDeclaredField("quiz");quiz.setAccessible(true);
        List<MainActivity.Question> one=new ArrayList<>();one.add(q);quiz.set(app,one);
        call(app,"showQuizScreen");
        EditText input=(EditText)field(app,"writtenAnswer");
        input.setText("1");call(app,"answerWritten");
        TextView simple=(TextView)field(app,"feedbackView");
        TextView details=(TextView)field(app,"detailView");
        Button more=(Button)field(app,"moreDetailsButton");
        assertTrue(simple.getText().toString().contains("groupe a r--"));
        assertEquals(View.VISIBLE,simple.getVisibility());
        assertEquals(View.GONE,details.getVisibility());
        more.performClick();
        assertEquals(View.VISIBLE,details.getVisibility());
        assertTrue(details.getText().toString().contains("0 droit d’écriture"));
        more.performClick();assertEquals(View.GONE,details.getVisibility());
        assertEquals(0,(int)field(app,"correctCount"));
        controller.pause().stop().destroy();
    }

    @SuppressWarnings("unchecked")
    @Test public void examHidesAnswersUntilResultsAndOffersTheSameDetails()throws Exception {
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity app=controller.get();
        MainActivity.Question q=find((List<MainActivity.Question>)field(app,"questions"),"Dans -rw-r--r--");
        Field quiz=MainActivity.class.getDeclaredField("quiz");quiz.setAccessible(true);
        List<MainActivity.Question> one=new ArrayList<>();one.add(q);quiz.set(app,one);
        Field exam=MainActivity.class.getDeclaredField("examMode");exam.setAccessible(true);exam.setBoolean(app,true);
        Field custom=MainActivity.class.getDeclaredField("customMode");custom.setAccessible(true);custom.setBoolean(app,true);
        call(app,"showQuizScreen");
        assertEquals(View.GONE,((TextView)field(app,"feedbackView")).getVisibility());
        ((EditText)field(app,"writtenAnswer")).setText("0");call(app,"answerWritten");
        assertTrue(hasButton(app.getWindow().getDecorView(),"Plus de détails"));
        controller.pause().stop().destroy();
    }
}
