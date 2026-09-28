package com.ghiles.quizubuntu;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
/** Short optional cues; no downloaded recordings or background playback. */
final class AcademyFeedback {
    private ToneGenerator tones;
    private final Context context;
    AcademyFeedback(Context context){this.context=context.getApplicationContext();}
    void success(){
        if(!context.getSharedPreferences("quiz_progress",Context.MODE_PRIVATE).getBoolean("soundEnabled",true))return;
        AudioManager audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if(audio==null||audio.getRingerMode()!=AudioManager.RINGER_MODE_NORMAL||audio.getStreamVolume(AudioManager.STREAM_MUSIC)==0||audio.isMusicActive())return;
        try {if(tones==null)tones=new ToneGenerator(AudioManager.STREAM_MUSIC,22);tones.startTone(ToneGenerator.TONE_PROP_ACK,140);}catch(RuntimeException ignored){release();}
    }
    void release(){if(tones!=null){tones.release();tones=null;}}
}
