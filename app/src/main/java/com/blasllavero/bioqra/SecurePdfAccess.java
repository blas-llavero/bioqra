package com.blasllavero.bioqra;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/** Authorizes only the exact saved document within BIOQRA's dated project. */
public final class SecurePdfAccess {
    private SecurePdfAccess(){}
    public static final long MAX_BYTES=10_000_000;

    static boolean safeName(String actual,String expected){
        return expected!=null && expected.endsWith(".pdf") && expected.equals(actual)
            && !expected.contains("/") && !expected.contains("\\");
    }
    public static ParcelFileDescriptor open(Context context,Uri uri,Uri tree,
                                             AiPreferences.SavedResult saved) throws Exception {
        if(uri==null||saved==null)throw new SecurityException("No saved PDF");
        String file=DefaultPdfStorage.filename(saved);
        String project=DefaultPdfStorage.projectFolder(saved);
        if("file".equals(uri.getScheme())){
            if(Build.VERSION.SDK_INT>=29 || tree!=null)
                throw new SecurityException("File URI not permitted");
            File base=context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if(base==null)throw new SecurityException("App directory unavailable");
            File canonical=new File(uri.getPath()).getCanonicalFile();
            File expected=new File(new File(new File(base,"Bioqra"),project),file).getCanonicalFile();
            if(!canonical.equals(expected)||!canonical.isFile()||
                    canonical.length()<5||canonical.length()>MAX_BYTES)
                throw new SecurityException("File is outside authorized project");
            try(InputStream in=new FileInputStream(canonical)){checkMagic(in);}
            return ParcelFileDescriptor.open(canonical,ParcelFileDescriptor.MODE_READ_ONLY);
        }
        if(!"content".equals(uri.getScheme())||uri.getQuery()!=null||uri.getFragment()!=null)
            throw new SecurityException("Invalid PDF URI scheme");
        if(tree==null)verifyMediaStore(context,uri,project,file);
        else verifySaf(context,uri,tree,project,file);
        // Read the signature using a separate stream, then hand PdfRenderer its
        // own descriptor at offset zero; never share a consumed descriptor.
        try(InputStream in=context.getContentResolver().openInputStream(uri)){
            if(in==null)throw new SecurityException("Cannot read PDF");
            checkMagic(in);
        }
        ParcelFileDescriptor result=context.getContentResolver().openFileDescriptor(uri,"r");
        if(result==null)throw new SecurityException("Read permission unavailable");
        long size=result.getStatSize();
        if(size<5||size>MAX_BYTES){result.close();throw new SecurityException("PDF size invalid");}
        return result;
    }

    private static void checkMagic(InputStream in) throws Exception{
        byte[] b=new byte[5];
        int count=0;
        while(count<5){
            int n=in.read(b,count,5-count);
            if(n<=0)throw new SecurityException("Truncated PDF");
            count+=n;
        }
        if(b[0]!='%'||b[1]!='P'||b[2]!='D'||b[3]!='F'||b[4]!='-')
            throw new SecurityException("Document is not a PDF");
    }
    private static void verifyMediaStore(Context c,Uri uri,String project,String file)throws Exception{
        if(Build.VERSION.SDK_INT<29)throw new SecurityException("Unsupported Downloads provider");
        Uri downloads=MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        String prefix=downloads.toString()+"/";
        if(!prefix.equals(uri.toString().substring(0,Math.min(prefix.length(),uri.toString().length())))
                ||uri.getPathSegments().size()!=downloads.getPathSegments().size()+1
                ||!uri.getLastPathSegment().matches("[0-9]{1,18}"))
            throw new SecurityException("Not a Downloads document URI");
        try(Cursor cur=c.getContentResolver().query(uri,new String[]{
                MediaStore.Downloads.DISPLAY_NAME,MediaStore.Downloads.RELATIVE_PATH,
                MediaStore.Downloads.MIME_TYPE,MediaStore.Downloads.OWNER_PACKAGE_NAME,
                MediaStore.Downloads.SIZE},null,null,null)){
            if(cur==null||!cur.moveToFirst()||
                    !safeName(cur.getString(0),file)||
                    !(Environment.DIRECTORY_DOWNLOADS+"/Bioqra/"+project+"/").equals(cur.getString(1))||
                    !"application/pdf".equalsIgnoreCase(cur.getString(2))||
                    !c.getPackageName().equals(cur.getString(3))||
                    cur.isNull(4)||cur.getLong(4)<5||cur.getLong(4)>MAX_BYTES||
                    cur.moveToNext())throw new SecurityException("PDF metadata or ownership mismatch");
        }
    }
    private static Uri findChild(Context c,Uri tree,Uri parent,String name,String mime)throws Exception{
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,
                DocumentsContract.getDocumentId(parent));
        try(Cursor cur=c.getContentResolver().query(children,new String[]{
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){
            if(cur==null)throw new SecurityException("Cannot inspect selected directory");
            Uri match=null;
            while(cur.moveToNext()){
                if(name.equals(cur.getString(1))&&mime.equals(cur.getString(2))){
                    if(match!=null)throw new SecurityException("Ambiguous duplicate document");
                    match=DocumentsContract.buildDocumentUriUsingTree(tree,cur.getString(0));
                }
            }
            if(match==null)throw new SecurityException("Document not found inside project");
            return match;
        }
    }
    private static void verifySaf(Context c,Uri uri,Uri tree,String project,String name)throws Exception{
        if(!DocumentsContract.isTreeUri(tree)||!DocumentsContract.isDocumentUri(c,uri)||
                !tree.getAuthority().equals(uri.getAuthority()))
            throw new SecurityException("Unrecognized document tree");
        boolean authorized=false;
        for(android.content.UriPermission permission:c.getContentResolver().getPersistedUriPermissions())
            if(permission.getUri().equals(tree)&&permission.isReadPermission())authorized=true;
        if(!authorized)throw new SecurityException("Document tree reading permission expired");
        Uri root=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
        Uri app=findChild(c,tree,root,"Bioqra",DocumentsContract.Document.MIME_TYPE_DIR);
        Uri projectFolder=findChild(c,tree,app,project,DocumentsContract.Document.MIME_TYPE_DIR);
        Uri expected=findChild(c,tree,projectFolder,name,"application/pdf");
        if(!DocumentsContract.getDocumentId(uri).equals(DocumentsContract.getDocumentId(expected)))
            throw new SecurityException("URI points to a different document");
        try(Cursor cur=c.getContentResolver().query(expected,new String[]{
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE},null,null,null)){
            if(cur==null||!cur.moveToFirst()||!safeName(cur.getString(0),name)||
                    !"application/pdf".equalsIgnoreCase(cur.getString(1))||
                    cur.isNull(2)||cur.getLong(2)<5||cur.getLong(2)>MAX_BYTES)
                throw new SecurityException("PDF file metadata invalid");
        }
    }
}
