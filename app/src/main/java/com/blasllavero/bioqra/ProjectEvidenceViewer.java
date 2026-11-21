package com.blasllavero.bioqra;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

final class ProjectEvidenceViewer {
    private ProjectEvidenceViewer(){}
    private static boolean archiveName(String actual,String expected){
        if(actual==null||expected==null||!expected.endsWith(".zip"))return false;
        return actual.equals(expected)||actual.matches(Pattern.quote(expected.substring(0,expected.length()-4))
                    +" ?\\([1-9][0-9]{0,8}\\)\\.zip");
    }
    static void show(Activity activity,AiPreferences.SavedResult record){
        new Thread(()->{
            ArrayList<String> names=new ArrayList<>();
            ArrayList<Uri> files=new ArrayList<>();
            String location=DefaultEvidenceStorage.relativeDirectory(record);
            String failure=null;
            try{
                String expected=DefaultEvidenceStorage.filename(record);
                if(Build.VERSION.SDK_INT>=29){
                    Uri collection=MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                    try(Cursor cursor=activity.getContentResolver().query(collection,new String[]{
                            MediaStore.Downloads._ID,MediaStore.Downloads.DISPLAY_NAME,
                            MediaStore.Downloads.OWNER_PACKAGE_NAME,MediaStore.Downloads.SIZE},
                            MediaStore.Downloads.RELATIVE_PATH+"=? AND "+MediaStore.Downloads.MIME_TYPE+"=? AND "+
                            MediaStore.Downloads.IS_PENDING+"=0",new String[]{location,"application/zip"},
                            MediaStore.Downloads.DATE_MODIFIED+" DESC, "+MediaStore.Downloads._ID+" DESC")){
                        if(cursor!=null)while(cursor.moveToNext()&&names.size()<50){
                            String name=cursor.getString(1);
                            if(!archiveName(name,expected)||cursor.getLong(3)<4||
                                    !SecurePdfAccess.ownerMatchesIfReported(cursor.getString(2),activity.getPackageName()))
                                continue;
                            Uri item=ContentUris.withAppendedId(collection,cursor.getLong(0));
                            try(java.io.InputStream stream=activity.getContentResolver().openInputStream(item)){
                                if(stream==null||stream.read()!=80||stream.read()!=75)continue;
                            }catch(Exception denied){continue;}
                            names.add(name);files.add(item);
                        }
                    }
                }else{
                    File base=activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                    if(base==null)throw new Exception("Storage unavailable");
                    File folder=new File(new File(base,"Bioqra"),DefaultPdfStorage.projectFolder(record));
                    location=folder.getAbsolutePath();
                    File archive=new File(folder,expected);
                    if(archive.isFile()&&archive.length()>4&&archive.getCanonicalFile().getParentFile().equals(folder.getCanonicalFile())){
                        names.add(archive.getName());files.add(Uri.fromFile(archive));
                    }
                }
            }catch(Exception e){failure=e.getMessage();}
            final String directory=location;
            final String error=failure;
            activity.runOnUiThread(()->{
                if(activity.isFinishing()||activity.isDestroyed())return;
                String heading=UiLanguage.tr(activity,"Project files");
                if(names.isEmpty()){
                    new AlertDialog.Builder(activity).setTitle(heading)
                        .setMessage(directory+"\n\n"+UiLanguage.tr(activity,"No readable research ZIP found")
                                +(error==null?"":"\n"+error))
                        .setPositiveButton(UiLanguage.tr(activity,"Close"),null).show();
                    return;
                }
                ArrayList<String> labels=new ArrayList<>();
                for(String name:names)labels.add(name);
                new AlertDialog.Builder(activity).setTitle(heading+"\n"+directory)
                    .setItems(labels.toArray(new String[0]),(dialog,which)->{
                        Uri item=files.get(which);
                        if("content".equals(item.getScheme())){
                            try{
                                Intent open=new Intent(Intent.ACTION_VIEW);
                                open.setDataAndType(item,"application/zip");
                                open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                open.setClipData(android.content.ClipData.newRawUri("Research ZIP",item));
                                activity.startActivity(Intent.createChooser(open,UiLanguage.tr(activity,"Open research ZIP")));
                                return;
                            }catch(Exception unavailable){}
                        }
                        new AlertDialog.Builder(activity).setTitle(labels.get(which))
                            .setMessage(directory+"\n"+UiLanguage.tr(activity,"Open this ZIP with a file manager"))
                            .setPositiveButton(UiLanguage.tr(activity,"Close"),null).show();
                    })
                    .setPositiveButton(UiLanguage.tr(activity,"Close"),null).show();
            });
        },"bioqra-project-files").start();
    }
}
