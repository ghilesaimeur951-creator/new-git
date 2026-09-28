package com.ghiles.quizubuntu;
import android.graphics.*;
import android.view.View;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,qualifiers="w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AcademyDesignTest {
    private Object field(Object obj,String name)throws Exception{Field f=obj.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(obj);}
    private void call(Object obj,String name)throws Exception{Method m=obj.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(obj);}
    private void capture(android.app.Activity activity,String name)throws Exception{
        View view=activity.getWindow().getDecorView();view.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));view.layout(0,0,360,800);
        Bitmap bitmap=Bitmap.createBitmap(360,800,Bitmap.Config.ARGB_8888);view.draw(new Canvas(bitmap));File dir=new File("build/reports/design");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
    }
    @Test public void renderAcademyAndChapterQuizOnPhone()throws Exception{
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity activity=controller.get();capture(activity,"home-light");
        call(activity,"showCategoryPicker");capture(activity,"chapters-light");
        Method start=MainActivity.class.getDeclaredMethod("startCategoryQuiz",String.class);start.setAccessible(true);start.invoke(activity,"Permissions");
        List<MainActivity.Question> quiz=(List<MainActivity.Question>)field(activity,"quiz");assertFalse(quiz.isEmpty());for(MainActivity.Question q:quiz)assertEquals("Permissions",q.category);capture(activity,"quiz-permissions");
        assertEquals("Permissions",field(activity,"activeChapter"));
        Field correct=MainActivity.class.getDeclaredField("correctCount");correct.setAccessible(true);correct.setInt(activity,quiz.size());call(activity,"finishLevel");assertEquals(100,activity.getSharedPreferences("quiz_progress",0).getInt("chapter_best_Permissions",0));
        activity.getSharedPreferences("quiz_progress",0).edit().putBoolean("darkMode",true).putBoolean("soundEnabled",false).apply();call(activity,"showHome");capture(activity,"home-dark");
        android.widget.Button button=AcademyDesign.button(activity);assertTrue(button instanceof com.google.android.material.button.MaterialButton);assertFalse(button.isSoundEffectsEnabled());assertTrue(button.getMinimumHeight()>=48);
        new AcademyFeedback(activity).success(); // Muted is a no-op.
        controller.pause().stop().destroy();
        ActivityController<SimulatorActivity> terminal=Robolectric.buildActivity(SimulatorActivity.class).setup();capture(terminal.get(),"terminal");terminal.pause().stop().destroy();
    }
    @Test public void allQuestionsBelongToOneChapter()throws Exception{
        ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup();List<MainActivity.Question> all=(List<MainActivity.Question>)field(c.get(),"questions");int covered=0;
        for(AcademyChapters.Chapter chapter:AcademyChapters.ALL){List<MainActivity.Question> questions=AcademyChapters.questions(all,chapter.category);assertFalse(chapter.category,questions.isEmpty());covered+=questions.size();for(MainActivity.Question q:questions)assertEquals(chapter.category,q.category);}
        assertEquals(all.size(),covered);c.pause().stop().destroy();
    }
    @Test public void renderPermissionsCorrectionCollapsedAndExpandedOnPhone()throws Exception{
        ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity=c.get();
        List<MainActivity.Question> all=(List<MainActivity.Question>)field(activity,"questions");
        MainActivity.Question permission=null;
        for(MainActivity.Question q:all)if(q.text.startsWith("Dans -rw-r--r--")){permission=q;break;}
        assertNotNull(permission);
        Field quiz=MainActivity.class.getDeclaredField("quiz");quiz.setAccessible(true);
        quiz.set(activity,new ArrayList<>(Collections.singletonList(permission)));
        call(activity,"showQuizScreen");
        ((android.widget.EditText)field(activity,"writtenAnswer")).setText("1");
        call(activity,"answerWritten");
        capture(activity,"permissions-correction");
        ((android.widget.Button)field(activity,"moreDetailsButton")).performClick();
        capture(activity,"permissions-details");
        c.pause().stop().destroy();
    }
}
