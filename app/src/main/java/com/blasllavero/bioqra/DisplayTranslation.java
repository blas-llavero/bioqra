package com.blasllavero.bioqra;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Optional translation for verified public organizer text or pre-existing
 * generated proposals. User-entered and patient data are never sent.
 * Original text stays in the caller. A failed request leaves it unchanged.
 */
final class DisplayTranslation {
    interface Callback {void onTranslation(String translated);}
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private static final Handler UI=new Handler(Looper.getMainLooper());
    private DisplayTranslation(){}

    static void translate(Activity activity,String challengeKey,String original,Callback callback){
        if(original==null||original.trim().isEmpty()||callback==null)return;
        Context app=activity.getApplicationContext();
        AiPreferences prefs=new AiPreferences(app);
        ChatGptConnection chat=new ChatGptConnection(app);
        if(!prefs.translateDisplayText())return;
        final String lang=UiLanguage.language(app);
        // Do not show a partly translated document mixed with untranslated text.
        if(original.length()>100000)return;
        final String source=original;
        WORKER.execute(()->{
            try{
                File folder=new File(app.getFilesDir(),"translations");
                List<String> parts=split(source,4900);
                String model=null;
                StringBuilder translated=new StringBuilder();
                for(String part:parts){
                    String key=digest(lang+"\n"+part);
                    File cached=new File(folder,key+".txt");
                    String text=readCache(cached);
                    if(text==null){
                        if(!prefs.translateDisplayText()||!prefs.autoConsent()
                                ||!chat.canUsePlan())return;
                        if(model==null)model=AiPreferences.choose(chat.models(),
                                prefs.selected(challengeKey)).slug;
                        if(!prefs.reserve(true))return;
                        text=chat.translateDisplayText(model,part,lang);
                        if(text.isEmpty())return;
                        writeCache(folder,cached,text);
                    }
                    translated.append(text);
                }
                if(translated.length()==0)return;
                String result=translated.toString();
                UI.post(()->{
                    if(!activity.isFinishing()&&!activity.isDestroyed()
                            &&new AiPreferences(app).translateDisplayText())
                        callback.onTranslation(result);
                });
            }catch(Exception ignored){
                // Preserve original content and never assert a translation was made.
            }
        });
    }
    private static List<String> split(String text,int limit){
        List<String> parts=new ArrayList<>();
        int pos=0;
        while(pos<text.length()){
            int end=Math.min(text.length(),pos+limit);
            if(end<text.length()){
                int newline=text.lastIndexOf('\n',end);
                if(newline>pos+limit/2)end=newline+1;
            }
            parts.add(text.substring(pos,end));
            pos=end;
        }
        return parts;
    }
    private static String digest(String text)throws Exception{
        byte[] bytes=MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder out=new StringBuilder();
        for(byte b:bytes)out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        return out.toString();
    }
    private static String readCache(File file){
        if(!file.isFile()||file.length()<1||file.length()>160000)return null;
        try(FileInputStream in=new FileInputStream(file)){
            byte[] buffer=new byte[(int)file.length()];
            int off=0,n;
            while(off<buffer.length&&(n=in.read(buffer,off,buffer.length-off))>0)off+=n;
            return off==buffer.length?new String(buffer,StandardCharsets.UTF_8):null;
        }catch(Exception ignored){return null;}
    }
    private static void writeCache(File folder,File file,String value){
        if(value.length()>80000)return;
        try{
            if(!folder.isDirectory()&&!folder.mkdirs())return;
            File temp=new File(folder,file.getName()+".part");
            try(FileOutputStream out=new FileOutputStream(temp,false)){
                out.write(value.getBytes(StandardCharsets.UTF_8));
            }
            if(!temp.renameTo(file))temp.delete();
            File[] existing=folder.listFiles((dir,name)->name.endsWith(".txt"));
            if(existing!=null&&existing.length>120){
                java.util.Arrays.sort(existing,
                    (a,b)->Long.compare(a.lastModified(),b.lastModified()));
                for(int i=0;i<existing.length-120;i++)existing[i].delete();
            }
        }catch(Exception ignored){}
    }
}
