package com.ghiles.quizubuntu;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.ContextThemeWrapper;
import android.widget.Button;
import com.google.android.material.button.MaterialButton;

/** Shared Material 3 components for the academy and terminal controls. */
final class AcademyDesign {
    static final int INK=Color.rgb(29,35,52), INDIGO=Color.rgb(81,70,200), TEAL=Color.rgb(8,126,130);
    static int dp(Context c,int size){return Math.round(size*c.getResources().getDisplayMetrics().density);}
    static Button button(Context context){
        boolean dark=context.getSharedPreferences("quiz_progress",Context.MODE_PRIVATE).getBoolean("darkMode",false);
        MaterialButton b=new MaterialButton(new ContextThemeWrapper(context,dark?R.style.Theme_Academy_Dark:R.style.Theme_Academy));
        b.setAllCaps(false);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        b.setMinHeight(dp(context,48));b.setMinimumHeight(dp(context,48));
        b.setInsetTop(0);b.setInsetBottom(0);b.setCornerRadius(dp(context,18));
        b.setSoundEffectsEnabled(context.getSharedPreferences("quiz_progress",Context.MODE_PRIVATE).getBoolean("soundEnabled",true));
        return b;
    }
    static void style(Button button,int fill,int radius,int stroke){
        if(!(button instanceof MaterialButton))return;
        MaterialButton b=(MaterialButton)button;
        b.setBackgroundTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{blend(fill,Color.GRAY,0.22f),fill}));
        b.setCornerRadius(dp(b.getContext(),radius));b.setStrokeWidth(stroke==0?0:dp(b.getContext(),1));b.setStrokeColor(ColorStateList.valueOf(stroke));
        b.setRippleColor(ColorStateList.valueOf(Color.argb(40,128,120,245)));
    }
    private static int blend(int a,int b,float ratio){return Color.rgb((int)(Color.red(a)*(1-ratio)+Color.red(b)*ratio),(int)(Color.green(a)*(1-ratio)+Color.green(b)*ratio),(int)(Color.blue(a)*(1-ratio)+Color.blue(b)*ratio));}
}
