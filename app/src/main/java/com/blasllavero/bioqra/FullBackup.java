package com.blasllavero.bioqra;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Portable, single ZIP archive: settings, latest AI draft per challenge and PDFs.
 * Never exports encrypted OAuth credentials, refresh tokens or automatic consent.
 * Uses only Storage Access Framework permissions granted by the user.
 */
public final class FullBackup {
    private FullBackup(){}
    private static final long MAX_TOTAL=50_000_000;
    private static final int MAX_ENTRIES=550;
    private static final int MAX_PDF_BYTES=8_000_000;

    private static byte[] jsonBytes(JSONObject obj) throws Exception{
        return obj.toString(2).getBytes(StandardCharsets.UTF_8);
    }
    private static void add(ZipOutputStream zip,String name,byte[] data) throws Exception{
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);zip.closeEntry();
    }
    private static Uri children(Uri tree,Uri parent){
        return DocumentsContract.buildChildDocumentsUriUsingTree(
                tree,DocumentsContract.getDocumentId(parent));
    }
    private static class Doc {
        final Uri uri; final String name,type;
        Doc(Uri uri,String name,String type){this.uri=uri;this.name=name;this.type=type;}
    }
    private static List<Doc> list(Context context,Uri tree,Uri parent) throws Exception{
        List<Doc> docs=new ArrayList<>();
        try(Cursor c=context.getContentResolver().query(children(tree,parent),
                new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){
            if(c==null)return docs;
            while(c.moveToNext()){
                if(docs.size()>=MAX_ENTRIES)
                    throw new Exception("Too many PDF files/folders in selected directory.");
                docs.add(new Doc(DocumentsContract.buildDocumentUriUsingTree(tree,c.getString(0)),
                        c.getString(1),c.getString(2)));
            }
        }
        return docs;
    }
    private static byte[] read(InputStream in,int limit) throws Exception{
        try(ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int n;
            while((n=in.read(buffer))!=-1){
                out.write(buffer,0,n);
                if(out.size()>limit)throw new Exception("Backup component exceeds safe size.");
            }
            return out.toByteArray();
        }
    }
    private static boolean safeProject(String text){
        return text!=null && text.length()>0 && text.length()<=75
                && ArchiveFiles.cleanName(text).equals(text)
                && !text.equals(".") && !text.equals("..");
    }

    public static final class Report{
        public final int results,pdfs;
        Report(int results,int pdfs){this.results=results;this.pdfs=pdfs;}
    }

    public static Report exportAll(Context context,Uri destination,
                                   AiPreferences prefs) throws Exception{
        List<AiPreferences.SavedResult> results=prefs.savedResults();
        Uri sourceTree=null;
        try{
            if(prefs.pdfTreeUri()!=null)sourceTree=Uri.parse(prefs.pdfTreeUri());
        }catch(Exception ignored){}
        long total=0;int pdfs=0;
        Set<String> included=new HashSet<>();
        try(OutputStream out=context.getContentResolver().openOutputStream(destination,"wt")){
            if(out==null)throw new Exception("Cannot open ZIP destination.");
            try(ZipOutputStream zip=new ZipOutputStream(out,StandardCharsets.UTF_8)){
                byte[] settings=jsonBytes(prefs.exportSettings());
                byte[] entries=jsonBytes(prefs.exportResults());
                total+=settings.length+entries.length;
                if(total>MAX_TOTAL)throw new Exception("Backup too large.");
                add(zip,"settings.json",settings);
                add(zip,"results.json",entries);
                // Original dated PDF files take precedence over regenerated
                // latest-result PDFs, preserving past dates in each project.
                if(sourceTree!=null){
                    try{
                        Uri root=DocumentsContract.buildDocumentUriUsingTree(sourceTree,
                                DocumentsContract.getTreeDocumentId(sourceTree));
                        for(Doc project:list(context,sourceTree,root)){
                            if(!DocumentsContract.Document.MIME_TYPE_DIR.equals(project.type)
                                    || !safeProject(project.name))continue;
                            for(Doc pdf:list(context,sourceTree,project.uri)){
                                if(!"application/pdf".equals(pdf.type)
                                        || !pdf.name.matches("[0-9]{6}-[^/\\\\]{1,90}\\.pdf"))continue;
                                String name="pdf/"+project.name+"/"+pdf.name;
                                if(!included.add(name))continue;
                                try(InputStream in=context.getContentResolver().openInputStream(pdf.uri)){
                                    if(in==null)throw new Exception("Cannot read "+name);
                                    byte[] bytes=read(in,MAX_PDF_BYTES);
                                    if(bytes.length<5||bytes[0]!='%'||bytes[1]!='P'
                                            ||bytes[2]!='D'||bytes[3]!='F'||bytes[4]!='-')
                                        throw new Exception("Invalid PDF "+name);
                                    total+=bytes.length;
                                    if(total>MAX_TOTAL||pdfs>=MAX_ENTRIES)
                                        throw new Exception("PDF backup size/count exceeded.");
                                    add(zip,name,bytes);
                                    pdfs++;
                                }
                            }
                        }
                    }catch(SecurityException e){
                        throw new Exception("PDF folder permission was revoked. Select the folder again.",e);
                    }
                }
                for(AiPreferences.SavedResult result:results){
                    String name=ArchiveFiles.archivePdfPath(result);
                    if(!included.add(name))continue;
                    byte[] bytes=ArchiveFiles.generatedPdf(context,result);
                    total+=bytes.length;
                    if(bytes.length>MAX_PDF_BYTES||total>MAX_TOTAL||pdfs>=MAX_ENTRIES)
                        throw new Exception("PDF backup size/count exceeded.");
                    add(zip,name,bytes);pdfs++;
                }
            }
        }
        return new Report(results.size(),pdfs);
    }

    private static final class PdfEntry{
        final String project,filename;
        final byte[] data;
        PdfEntry(String project,String filename,byte[] data){
            this.project=project;this.filename=filename;this.data=data;
        }
    }
    public static Report importAll(Context context,Uri input,AiPreferences prefs) throws Exception{
        byte[] settings=null,results=null;
        List<PdfEntry> pdfs=new ArrayList<>();
        long total=0;int count=0;
        Set<String> seen=new HashSet<>();
        try(InputStream raw=context.getContentResolver().openInputStream(input)){
            if(raw==null)throw new Exception("Cannot open backup ZIP.");
            try(ZipInputStream zip=new ZipInputStream(raw,StandardCharsets.UTF_8)){
                ZipEntry entry;
                while((entry=zip.getNextEntry())!=null){
                    if(++count>MAX_ENTRIES+2)throw new Exception("Too many ZIP items.");
                    if(entry.isDirectory())continue;
                    String name=entry.getName();
                    if(name.contains("\\") || name.startsWith("/") || name.contains("..")
                        || !seen.add(name))
                        throw new Exception("Invalid or duplicate archive path.");
                    int limit=name.endsWith(".json")?15_000_000:MAX_PDF_BYTES;
                    byte[] bytes=read(zip,limit);
                    total+=bytes.length;
                    if(total>MAX_TOTAL)throw new Exception("Backup exceeds 50 MB.");
                    if("settings.json".equals(name))settings=bytes;
                    else if("results.json".equals(name))results=bytes;
                    else if(name.startsWith("pdf/")){
                        String[] parts=name.split("/",-1);
                        boolean dated=parts.length==4 && "Bioqra".equals(parts[1])
                                && parts[2].matches("[0-9]{6}-[^/\\\\]{1,75}")
                                && parts[3].equals(parts[2].substring(7)+".pdf");
                        boolean legacy=parts.length==3 && safeProject(parts[1])
                                && parts[2].matches("[0-9]{6}-[^/\\\\]{1,90}\\.pdf");
                        if(!dated && !legacy)
                            throw new Exception("Invalid PDF path in archive.");
                        if(bytes.length<5||bytes[0]!='%'||bytes[1]!='P'
                                ||bytes[2]!='D'||bytes[3]!='F'||bytes[4]!='-')
                            throw new Exception("Not a PDF: "+name);
                        pdfs.add(new PdfEntry(dated?parts[2]:parts[1],dated?parts[3]:parts[2],bytes));
                    }else throw new Exception("Unexpected file in BIOQRA backup: "+name);
                    zip.closeEntry();
                }
            }
        }
        if(settings==null||results==null)
            throw new Exception("Not a complete BIOQRA backup (settings/results missing).");
        JSONObject settingsObj=new JSONObject(new String(settings,StandardCharsets.UTF_8));
        JSONObject resultsObj=new JSONObject(new String(results,StandardCharsets.UTF_8));
        if(!"bioqra-settings".equals(settingsObj.optString("type"))
            || settingsObj.optInt("version")!=1
            || !"bioqra-results".equals(resultsObj.optString("type"))
            || resultsObj.optInt("version")!=1)
            throw new Exception("Unsupported backup version.");
        Uri tree=null;
        if(!pdfs.isEmpty() && prefs.pdfTreeUri()!=null){
            tree=Uri.parse(prefs.pdfTreeUri());
            boolean found=false;
            for(android.content.UriPermission perm:context.getContentResolver()
                    .getPersistedUriPermissions()){
                if(perm.getUri().equals(tree) && perm.isWritePermission()){found=true;break;}
            }
            if(!found)throw new Exception("Custom PDF folder permission was lost. Select it again.");
        }
        // Validate before restoring PDFs. The existing JSON importers validate
        // input independently and preserve newer local results.
        settingsObj.getString("global_model");
        settingsObj.getJSONObject("overrides");
        resultsObj.getJSONArray("results");
        for(PdfEntry p:pdfs)
            ArchiveFiles.restorePdf(context,tree,p.project,p.filename,p.data);
        prefs.importSettings(settingsObj); // disables unattended consent
        int restored=prefs.importResults(resultsObj);
        return new Report(restored,pdfs.size());
    }
}
