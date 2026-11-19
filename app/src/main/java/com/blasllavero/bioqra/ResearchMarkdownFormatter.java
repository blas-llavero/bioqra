package com.blasllavero.bioqra;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays model-generated Markdown as styled, selectable Android text.
 * No HTML, JavaScript, WebView, remote images or active content is executed.
 * The original model response is retained verbatim in local saved results.
 */
public final class ResearchMarkdownFormatter {
    private ResearchMarkdownFormatter(){}

    private static void inline(SpannableStringBuilder out,String line){
        int i=0;
        while(i<line.length()){
            int tick=line.indexOf('`',i);
            int bold=line.indexOf("**",i);
            int star=line.indexOf('*',i);
            int next=line.length(); int mode=0;
            if(tick>=0 && tick<next){next=tick;mode=1;}
            if(bold>=0 && bold<next){next=bold;mode=2;}
            if(star>=0 && star<next){next=star;mode=3;}
            if(next>i)out.append(line.substring(i,next));
            if(mode==0)break;
            String delimiter=mode==1?"`":mode==2?"**":"*";
            int close=line.indexOf(delimiter,next+delimiter.length());
            if(close<0){out.append(delimiter);i=next+delimiter.length();continue;}
            int start=out.length();
            out.append(line.substring(next+delimiter.length(),close));
            int end=out.length();
            if(end>start){
                if(mode==1){
                    out.setSpan(new TypefaceSpan("monospace"),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    out.setSpan(new BackgroundColorSpan(Color.rgb(231,239,241)),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }else out.setSpan(new StyleSpan(mode==2?Typeface.BOLD:Typeface.ITALIC),
                        start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            i=close+delimiter.length();
        }
    }

    private static boolean tableSeparator(String line){
        return line.trim().matches("\\|?[\\s:|\\-]{3,}\\|?");
    }
    private static String[] cells(String line){
        String clean=line.trim();
        if(clean.startsWith("|"))clean=clean.substring(1);
        if(clean.endsWith("|"))clean=clean.substring(0,clean.length()-1);
        return clean.split("\\|",-1);
    }
    private static void styledLine(SpannableStringBuilder out,String line,int kind){
        int start=out.length();
        inline(out,line);
        int end=out.length();
        if(end>start){
            if(kind==1){
                out.setSpan(new StyleSpan(Typeface.BOLD),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new RelativeSizeSpan(1.2f),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new ForegroundColorSpan(Color.rgb(18,73,91)),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }else if(kind==2){
                out.setSpan(new LeadingMarginSpan.Standard(24,24),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }else if(kind==3){
                out.setSpan(new TypefaceSpan("monospace"),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new BackgroundColorSpan(Color.rgb(239,244,246)),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        out.append('\n');
    }

    public static CharSequence format(String markdown){
        if(markdown==null || markdown.isEmpty())return "";
        String[] lines=markdown.replace("\r\n","\n").replace('\r','\n').split("\n",-1);
        SpannableStringBuilder out=new SpannableStringBuilder();
        boolean code=false;
        for(int i=0;i<lines.length;i++){
            String raw=lines[i], trimmed=raw.trim();
            if(trimmed.startsWith("```")){code=!code;continue;}
            if(code){styledLine(out,raw,3);continue;}
            if(trimmed.matches("#{1,6}\\s+.*")){
                if(out.length()>0 && out.charAt(out.length()-1)=='\n')out.append('\n');
                styledLine(out,trimmed.replaceFirst("^#{1,6}\\s+",""),1);
                out.append('\n');
                continue;
            }
            if(trimmed.startsWith("|") && i+1<lines.length &&
                    tableSeparator(lines[i+1]) && lines[i+1].contains("|")){
                String[] headers=cells(trimmed);
                i+=2;
                for(;i<lines.length && lines[i].trim().startsWith("|");i++){
                    String[] values=cells(lines[i]);
                    for(int j=0;j<values.length;j++){
                        String key=j<headers.length?headers[j].trim():"Column "+(j+1);
                        if(key.isEmpty()||values[j].trim().isEmpty())continue;
                        int start=out.length();
                        out.append(key).append(": ");
                        out.setSpan(new StyleSpan(Typeface.BOLD),start,out.length(),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        inline(out,values[j].trim());
                        out.append('\n');
                    }
                    out.append('\n');
                }
                i--;
                continue;
            }
            if(trimmed.matches("([-*+]\\s+|\\d+[.)]\\s+).*")){
                String item=trimmed.replaceFirst("^([-*+]\\s+|\\d+[.)]\\s+)","");
                styledLine(out,"• "+item,2);
            }else{
                styledLine(out,raw,0);
            }
        }
        return out;
    }
}
