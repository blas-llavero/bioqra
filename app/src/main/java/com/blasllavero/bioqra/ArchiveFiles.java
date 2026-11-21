package com.blasllavero.bioqra;

import android.content.Context;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.provider.DocumentsContract;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Portable JSON backups and locally generated PDF research drafts. */
public final class ArchiveFiles {
    private ArchiveFiles(){}
    private static final int MAX_BACKUP_BYTES=15_000_000;

    public static JSONObject readJson(Context context,Uri uri) throws Exception{
        try(InputStream in=context.getContentResolver().openInputStream(uri);
            ByteArrayOutputStream out=new ByteArrayOutputStream()){
            if(in==null)throw new Exception("No file access.");
            byte[] buf=new byte[8192];int n;
            while((n=in.read(buf))!=-1){
                out.write(buf,0,n);
                if(out.size()>MAX_BACKUP_BYTES)throw new Exception("Backup exceeds 15 MB limit.");
            }
            return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
        }
    }
    public static void writeJson(Context context,Uri uri,JSONObject json) throws Exception{
        try(OutputStream out=context.getContentResolver().openOutputStream(uri,"wt")){
            if(out==null)throw new Exception("Unable to write backup.");
            out.write(json.toString(2).getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    public static String cleanName(String name){
        if(name==null || name.trim().isEmpty())return "Challenge";
        String safe=name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]+","-")
            .replaceAll("\\s+"," ").replaceAll("^\\.+","").trim();
        if(safe.length()>75)safe=safe.substring(0,75).trim();
        return safe.isEmpty()?"Challenge":safe;
    }

    private static Uri folder(Context context,Uri tree,Uri parent,String name) throws Exception{
        ContentResolver resolver=context.getContentResolver();
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,
                DocumentsContract.getDocumentId(parent));
        try(Cursor cursor=resolver.query(children,
                new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){
            if(cursor!=null){
                while(cursor.moveToNext()){
                    if(name.equals(cursor.getString(1))
                        && DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(2)))
                        return DocumentsContract.buildDocumentUriUsingTree(tree,cursor.getString(0));
                }
            }
        }
        Uri created=DocumentsContract.createDocument(resolver,parent,
                DocumentsContract.Document.MIME_TYPE_DIR,name);
        if(created==null)throw new Exception("Could not create "+name);
        return created;
    }

    /** Reuse a saved document rather than generating suffixes on repeated same-day exports. */
    private static Uri existingPdf(Context context,Uri tree,Uri parent,String filename) {
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(
                tree,DocumentsContract.getDocumentId(parent));
        try(Cursor cursor=context.getContentResolver().query(children,
                new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){
            if(cursor!=null){
                while(cursor.moveToNext()){
                    if(filename.equals(cursor.getString(1))
                        && "application/pdf".equals(cursor.getString(2)))
                        return DocumentsContract.buildDocumentUriUsingTree(
                                tree,cursor.getString(0));
                }
            }
        }catch(Exception ignored){}
        return null;
    }

    /** Default: Downloads/Bioqra/YYMMDD-Project/Project.pdf.
     * A user-selected SAF tree remains a supported override. */
    public static Uri makePdf(Context context,Uri tree,AiPreferences.SavedResult result) throws Exception{
        if(tree==null)return DefaultPdfStorage.save(context,result);
        Uri root=DocumentsContract.buildDocumentUriUsingTree(
                tree,DocumentsContract.getTreeDocumentId(tree));
        Uri appFolder=folder(context,tree,root,"Bioqra");
        Uri project=folder(context,tree,appFolder,DefaultPdfStorage.projectFolder(result));
        String filename=DefaultPdfStorage.filename(result);
        Uri target=existingPdf(context,tree,project,filename);
        boolean created=target==null;
        if(created)target=DocumentsContract.createDocument(context.getContentResolver(),
                project,"application/pdf",filename);
        if(target==null)throw new Exception("Could not create PDF");
        boolean ok=false;
        try(OutputStream out=context.getContentResolver().openOutputStream(target,"rwt")){
            if(out==null)throw new Exception("Cannot open PDF");
            writePdf(out,result);ok=true;
        }finally{
            if(!ok&&created)try{DocumentsContract.deleteDocument(context.getContentResolver(),target);}
                catch(Exception ignored){}
        }
        return target;
    }

    public static byte[] generatedPdf(AiPreferences.SavedResult result) throws Exception{
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        writePdf(out,result);
        return out.toByteArray();
    }

    public static String archivePdfPath(AiPreferences.SavedResult result){
        return "pdf/Bioqra/"+DefaultPdfStorage.projectFolder(result)+"/"
                +DefaultPdfStorage.filename(result);
    }

    public static Uri restorePdf(Context context,Uri tree,String project,String filename,
                                 byte[] bytes) throws Exception{
        if(tree==null)return DefaultPdfStorage.restore(context,project,filename,bytes);
        if(project==null||!project.matches("[0-9]{6}-[^/\\\\]{1,75}")||
                filename==null||!filename.equals(project.substring(7)+".pdf"))
            throw new Exception("Invalid PDF archive path.");
        if(bytes==null||bytes.length<5||bytes.length>8_000_000
                || bytes[0]!='%'||bytes[1]!='P'||bytes[2]!='D'
                || bytes[3]!='F'||bytes[4]!='-')
            throw new Exception("Invalid PDF archive content.");
        Uri root=DocumentsContract.buildDocumentUriUsingTree(
                tree,DocumentsContract.getTreeDocumentId(tree));
        Uri bioqra=folder(context,tree,root,"Bioqra");
        Uri folderUri=folder(context,tree,bioqra,project);
        Uri target=existingPdf(context,tree,folderUri,filename);
        if(target!=null)return target; // Do not overwrite the user's existing PDFs.
        target=DocumentsContract.createDocument(context.getContentResolver(),
                folderUri,"application/pdf",filename);
        if(target==null)throw new Exception("Could not restore PDF.");
        boolean ok=false;
        try(OutputStream out=context.getContentResolver().openOutputStream(target,"w")){
            if(out==null)throw new Exception("Cannot write restored PDF.");
            out.write(bytes);out.flush();ok=true;
        }finally{
            if(!ok)try{
                DocumentsContract.deleteDocument(context.getContentResolver(),target);
            }catch(Exception ignored){}
        }
        return target;
    }

    private static void writePdf(OutputStream output,AiPreferences.SavedResult result) throws Exception{
        ResearchPdfLayout.write(output,result);
    }
}
