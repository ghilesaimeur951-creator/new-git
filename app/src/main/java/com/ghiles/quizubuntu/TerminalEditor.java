package com.ghiles.quizubuntu;

import android.content.Context;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.widget.EditText;

/** One terminal surface: immutable transcript/prompt, editable command at its end. */
public final class TerminalEditor extends EditText {
    private int boundary;
    private boolean rendering;
    private Runnable submit;
    public void setOnSubmit(Runnable submit) { this.submit=submit; }
    @Override public android.view.inputmethod.InputConnection onCreateInputConnection(android.view.inputmethod.EditorInfo info) {
        android.view.inputmethod.InputConnection connection=super.onCreateInputConnection(info);
        if(connection==null)return null;
        info.imeOptions &= ~android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        return new android.view.inputmethod.InputConnectionWrapper(connection,false) {
            @Override public boolean commitText(CharSequence text,int position) {
                if(submit!=null && (text.toString().equals("\n")||text.toString().equals("\r\n"))) {submit.run();return true;}
                return super.commitText(text,position);
            }
        };
    }
    public TerminalEditor(Context context) {
        super(context);
        setFilters(new InputFilter[]{(source,start,end,dest,dstart,dend) -> {
            if(rendering)return null;
            if(dstart<boundary)return dest.subSequence(dstart,dend);
            String input=source.subSequence(start,end).toString();
            // Pasting never executes commands or injects extra prompts.
            return input.contains("\n")||input.contains("\r") ? input.replace('\n',' ').replace('\r',' ') : null;
        }});
    }
    public String command() { return getText().toString().substring(Math.min(boundary,length())); }
    public void setCommand(String value) {
        getText().replace(Math.min(boundary,length()),length(),value);
        setSelection(length());
    }
    public void render(CharSequence transcript,CharSequence prompt) {
        String draft=command();
        int cursor=Math.max(0,getSelectionStart()-boundary);
        rendering=true;
        SpannableStringBuilder text=new SpannableStringBuilder(transcript);
        if(text.length()>0&&text.charAt(text.length()-1)!='\n')text.append('\n');
        text.append(prompt); boundary=text.length();text.append(draft);
        setText(text);
        setSelection(Math.min(length(),boundary+cursor));
        rendering=false;
    }
    @Override protected void onSelectionChanged(int start,int end) {
        super.onSelectionChanged(start,end);
        if(!rendering&&(start<boundary||end<boundary)&&boundary<=length())setSelection(boundary);
    }
}
