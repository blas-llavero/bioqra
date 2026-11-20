package com.blasllavero.bioqra;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/** Writes an app-owned research ZIP without a folder picker or broad file permissions. */
public final class DefaultEvidenceStorage {
    private DefaultEvidenceStorage(){}

    public static String relativeDirectory(AiPreferences.SavedResult result){
        return DefaultPdfStorage.relativeDirectory(result);
    }
    public static String filename(AiPreferences.SavedResult result){
        return ArchiveFiles.cleanName(result.title)+"-research-evidence.zip";
    }
    public static Uri export(Context context,String key,AiPreferences.SavedResult result)
            throws Exception {
        if(Build.VERSION.SDK_INT<29){
            File root=context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if(root==null)throw new Exception("App storage unavailable");
            File folder=new File(new File(root,"Bioqra"),DefaultPdfStorage.projectFolder(result));
            if(!folder.isDirectory()&&!folder.mkdirs())
                throw new Exception("Cannot create research evidence folder");
            File file=new File(folder,filename(result));
            // Use FileProvider-independent app-owned storage for legacy Android.
            // ResearchEvidenceArchive.export accepts content URIs only; copy
            // through a bounded app-private temporary ZIP instead.
            File temp=new File(context.getCacheDir(),"research-evidence-"+key+".zip");
            if(!key.matches("[a-f0-9]{32}"))throw new Exception("Invalid challenge key");
            try {
                Uri uri=Uri.fromFile(temp);
                // On older Android the exporter needs a content:// provider.
                // Do not attempt file scheme through ContentResolver.
                throw new Exception("Automatic ZIP export requires Android 10 or later");
            }finally{temp.delete();}
        }
        ContentResolver resolver=context.getContentResolver();
        Uri collection=MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        ContentValues fields=new ContentValues();
        fields.put(MediaStore.Downloads.DISPLAY_NAME,filename(result));
        fields.put(MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS+"/Bioqra/"
                    +DefaultPdfStorage.projectFolder(result)+"/");
        fields.put(MediaStore.Downloads.MIME_TYPE,"application/zip");
        fields.put(MediaStore.Downloads.IS_PENDING,1);
        Uri target=resolver.insert(collection,fields);
        if(target==null)throw new Exception("Unable to create research ZIP in Downloads");
        boolean published=false;
        try{
            ResearchEvidenceArchive.export(context,key,target,result);
            ContentValues ready=new ContentValues();
            ready.put(MediaStore.Downloads.IS_PENDING,0);
            if(resolver.update(target,ready,null,null)!=1)
                throw new Exception("Unable to publish research ZIP");
            published=true;
            return target;
        }finally{
            if(!published)try{resolver.delete(target,null,null);}catch(Exception ignored){}
        }
    }
}
