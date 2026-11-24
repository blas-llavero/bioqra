package com.blasllavero.bioqra;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Automatically stores PDFs in a publicly accessible Downloads/Bioqra folder.
 * Android 10+ uses MediaStore with no user-selected folder or storage permission.
 * Android 8-9 falls back to app-specific external Documents, requiring no permission.
 */
public final class DefaultPdfStorage {
    private DefaultPdfStorage(){}

    public static String projectFolder(AiPreferences.SavedResult result){
        LocalDate date;
        try{date=LocalDate.parse(result.date);}catch(Exception e){date=LocalDate.now();}
        return date.format(DateTimeFormatter.ofPattern("yyMMdd",Locale.ROOT))
            +"-"+ArchiveFiles.cleanName(result.title);
    }

    public static String filename(AiPreferences.SavedResult result){
        return ArchiveFiles.cleanName(result.title)+".pdf";
    }

    public static String relativeDirectory(AiPreferences.SavedResult result){
        return "Download/Bioqra/"+projectFolder(result)+"/";
    }

    public static Uri save(Context context,AiPreferences.SavedResult result) throws Exception{
        return saveVerifiedPdf(context,projectFolder(result),filename(result),
                ArchiveFiles.generatedPdf(context,result),false);
    }

    /** Restore an archived PDF without overwriting a user-owned existing document. */
    public static Uri restore(Context context,String project,String filename,byte[] bytes)
            throws Exception{
        if(project==null||!project.matches("[0-9]{6}-[^/\\\\]{1,75}")||
           !project.equals(project.trim()) || filename==null||
           !filename.equals(project.substring(7)+".pdf"))
            throw new Exception("Unsafe archived project path");
        return saveVerifiedPdf(context,project,filename,bytes,true);
    }

    private static Uri saveVerifiedPdf(Context context,String project,String name,
                                       byte[] bytes,boolean keepExisting)throws Exception{
        if(bytes==null||bytes.length<5||bytes.length>8_000_000||
           bytes[0]!='%'||bytes[1]!='P'||bytes[2]!='D'||bytes[3]!='F'||bytes[4]!='-')
            throw new Exception("Invalid PDF bytes");
        if(Build.VERSION.SDK_INT<29){
            File base=context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if(base==null)throw new Exception("No external documents storage");
            File folder=new File(new File(base,"Bioqra"),project);
            if(!folder.isDirectory()&&!folder.mkdirs())
                throw new Exception("Could not create the Bioqra folder");
            File target=new File(folder,name);
            if(keepExisting && target.exists())return Uri.fromFile(target);
            try(OutputStream out=new FileOutputStream(target,false)){
                out.write(bytes);
            }
            return Uri.fromFile(target);
        }
        ContentResolver resolver=context.getContentResolver();
        Uri collection=MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        String path=Environment.DIRECTORY_DOWNLOADS+"/Bioqra/"+project+"/";
        Uri existing=null;
        try(Cursor cursor=resolver.query(collection,
                new String[]{MediaStore.Downloads._ID},
                MediaStore.Downloads.DISPLAY_NAME+"=? AND "+MediaStore.Downloads.RELATIVE_PATH+"=?",
                new String[]{name,path},null)){
            if(cursor!=null && cursor.moveToFirst())
                existing=ContentUris.withAppendedId(collection,cursor.getLong(0));
        }
        if(keepExisting && existing!=null)return existing;
        boolean created=existing==null;
        Uri target=existing;
        if(created){
            ContentValues values=new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME,name);
            values.put(MediaStore.Downloads.MIME_TYPE,"application/pdf");
            values.put(MediaStore.Downloads.RELATIVE_PATH,path);
            values.put(MediaStore.Downloads.IS_PENDING,1);
            target=resolver.insert(collection,values);
            if(target==null)throw new Exception("Could not create PDF in Downloads");
        }
        boolean success=false;
        try{
            try(OutputStream out=resolver.openOutputStream(target,"rwt")){
                if(out==null)throw new Exception("Cannot write PDF");
                out.write(bytes);
            }
            if(created){
                ContentValues published=new ContentValues();
                published.put(MediaStore.Downloads.IS_PENDING,0);
                if(resolver.update(target,published,null,null)!=1)
                    throw new Exception("Could not publish the PDF");
            }
            success=true;
            return target;
        }finally{
            if(!success&&created&&target!=null)
                try{resolver.delete(target,null,null);}catch(Exception ignored){}
        }
    }
}
