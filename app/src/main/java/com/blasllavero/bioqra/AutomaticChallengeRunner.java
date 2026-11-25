package com.blasllavero.bioqra;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipInputStream;

/**
 * Executes the selected public research queue without a ChallengeActivity.
 * The queue is local to the current app process; Android may terminate it if
 * the app process is killed. It does not upload private patient data or submit
 * anything to a challenge organizer.
 */
final class AutomaticChallengeRunner {
    static final class Item {
        final String title,url,kind;
        Item(String title,String url,String kind){
            this.title=title;this.url=url;this.kind=kind;
        }
    }

    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private static boolean running=false;
    private static String activeKey="";
    private static String status="";
    private static long queueToken=0;

    static synchronized boolean running(){return running;}
    static synchronized String activeKey(){return activeKey;}
    static synchronized String status(){return status;}

    static synchronized void start(Context context,List<Item> items){
        if(running||items==null||items.isEmpty())return;
        if(ProcessingControl.stopped()||ProcessingControl.paused())return;
        Context app=context.getApplicationContext();
        List<Item> tasks=new ArrayList<>(items);
        running=true;
        long token=++queueToken;
        status="Automatic research started";
        WORKER.execute(()->runQueue(app,tasks,token));
    }

    private static void runQueue(Context app,List<Item> tasks,long token){
        AiPreferences prefs=new AiPreferences(app);
        ChatGptConnection chat=new ChatGptConnection(app);
        try{
            for(Item item:tasks){
                ProcessingControl.checkpoint(ProcessingControl.token());
                if(token!=queueToken)break;
                if(!chat.canUsePlan()||!prefs.autoConsent()){
                    update("Automatic research paused: ChatGPT authorization unavailable");
                    break;
                }
                String key=AiPreferences.stableKey(item.title,item.title);
                if(prefs.automaticExportCompleted(key))continue;
                if(!TrustedChallengeReader.approved(item.url)){
                    prefs.setProcessingOutcome(key,"source_error");
                    continue;
                }
                if(prefs.lastResult(key)==null &&
                        (prefs.alreadyAttempted(key)||prefs.remaining()<=0)){
                    if(prefs.remaining()<=0){
                        update("Automatic research paused: daily request limit reached");
                        break;
                    }
                    prefs.setProcessingOutcome(key,"attempt_pending");
                    continue;
                }
                synchronized(AutomaticChallengeRunner.class){activeKey=key;}
                update("Processing: "+item.title);
                try{
                    process(app,chat,prefs,item,key,token);
                }catch(InterruptedException cancelled){
                    update("Automatic research "+(ProcessingControl.stopped()?"stopped":"paused"));
                    break;
                }catch(Exception failure){
                    prefs.setProcessingOutcome(key,"processing_error");
                    prefs.setProcessingError(key,safeMessage(failure));
                    update("Research step failed for "+item.title+": "+safeMessage(failure));
                    if(isAuthorizationOrQuotaError(failure)){
                        break;
                    }
                    // One missing/unavailable challenge must not block other
                    // public research tasks. Failure is visible per challenge.
                }
            }
        }catch(InterruptedException interrupted){
            update(ProcessingControl.stopped()?"Automatic research stopped":"Automatic research interrupted");
        }finally{
            synchronized(AutomaticChallengeRunner.class){
                running=false;activeKey="";
                if(status.startsWith("Processing:"))status="Automatic queue finished";
            }
        }
    }

    private static void process(Context app,ChatGptConnection chat,AiPreferences prefs,
                                Item item,String key,long queueToken) throws Exception{
        long stopToken=ProcessingControl.token();
        AiPreferences.SavedResult saved=findSaved(prefs,key);
        if(saved==null){
            ProcessingControl.checkpoint(stopToken);
            prefs.setProcessingStage(key,1);
            update("Retrieving official statement: "+item.title);
            ChallengePageResolver.Outcome statement=
                    ChallengePageResolver.resolve(app,item.url,item.title);
            if(!statement.specific || statement.text==null ||
                    statement.text.trim().length()<100){
                prefs.setProcessingOutcome(key,"source_error");
                prefs.setProcessingError(key,"Verified detailed challenge statement unavailable");
                return;
            }
            ProcessingControl.checkpoint(stopToken);
            prefs.setProcessingStage(key,2);
            update("Discovering public datasets: "+item.title);
            PublicDatasetDiscovery.Report datasets=null;
            String evidenceNote="";
            try{
                datasets=PublicDatasetDiscovery.discover(item.title);
                ResearchEvidenceArchive.acquire(app,key,item.title,datasets,statement.resolvedUrl);
            }catch(Exception dataError){
                evidenceNote="Dataset discovery or download unavailable: "
                        +safeMessage(dataError);
            }
            ProcessingControl.checkpoint(stopToken);
            prefs.setProcessingStage(key,3);
            update("Generating research proposal: "+item.title);
            if(!prefs.reserve(true))throw new Exception("Daily request limit reached");
            prefs.markAttempted(key);
            AiPreferences.Selection model=AiPreferences.choose(chat.models(),prefs.selected(key));
            String context="Official source: "+statement.resolvedUrl+"\n\n"
                    +ChallengeTextFormatter.truncate(statement.text,15000)
                    +"\n\nREAL RESEARCH DATA STATUS:\n"
                    +ResearchEvidenceArchive.summary(app,key)
                    +(datasets==null?"":"\n"+ChallengeTextFormatter.truncate(datasets.forModel(),3500))
                    +"\n"+evidenceNote;
            String answer=chat.respond(model.slug,item.title,
                    ChallengeTextFormatter.truncate(context,22000),"",item.kind);
            ProcessingControl.checkpoint(stopToken);
            prefs.storeResult(key,item.title,statement.resolvedUrl,model.label,answer);
            saved=findSaved(prefs,key);
            if(saved==null)throw new Exception("Generated research proposal was not saved");
        }
        ProcessingControl.checkpoint(stopToken);
        prefs.setProcessingStage(key,4);
        update("Preparing PDF and ZIP: "+item.title);
        String storedTree=prefs.pdfTreeUri();
        Uri tree=storedTree==null?null:Uri.parse(storedTree);
        Uri pdf=ArchiveFiles.findSavedPdf(app,tree,saved);
        if(pdf==null)pdf=ArchiveFiles.makePdf(app,tree,saved);
        verifyPdf(app,pdf,tree,saved);
        ProcessingControl.checkpoint(stopToken);
        Uri zip=DefaultEvidenceStorage.export(app,key,saved);
        verifyZip(app,zip);
        ProcessingControl.checkpoint(stopToken);
        String outcome=ResearchEvidenceArchive.downloadedCount(app,key)>0
                ?"data_unvalidated":"no_research_data";
        prefs.setProcessingError(key,"");
        prefs.setProcessingOutcome(key,outcome);
        prefs.markAutomaticExportCompleted(key);
        update("Processing completed: "+item.title);
    }

    private static AiPreferences.SavedResult findSaved(AiPreferences prefs,String key){
        for(AiPreferences.SavedResult r:prefs.savedResults())
            if(key.equals(r.key))return r;
        return null;
    }

    private static void verifyPdf(Context app,Uri uri,Uri tree,
                                  AiPreferences.SavedResult record)throws Exception{
        ParcelFileDescriptor descriptor=SecurePdfAccess.open(app,uri,tree,record);
        PdfRenderer renderer=null;
        try{
            renderer=new PdfRenderer(descriptor);
            if(renderer.getPageCount()<1||renderer.getPageCount()>150)
                throw new Exception("Saved PDF has an invalid page count");
            try(PdfRenderer.Page page=renderer.openPage(0)){
                Bitmap bitmap=Bitmap.createBitmap(120,170,Bitmap.Config.ARGB_8888);
                try{page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);}
                finally{bitmap.recycle();}
            }
        }finally{
            if(renderer!=null)renderer.close();
            else descriptor.close();
        }
    }

    private static void verifyZip(Context app,Uri uri)throws Exception{
        if(uri==null)throw new Exception("Research ZIP was not saved");
        InputStream raw="file".equals(uri.getScheme())
                ?new FileInputStream(uri.getPath())
                :app.getContentResolver().openInputStream(uri);
        if(raw==null)throw new Exception("Research ZIP cannot be opened");
        try(ZipInputStream zip=new ZipInputStream(raw)){
            if(zip.getNextEntry()==null)
                throw new Exception("Research ZIP has no readable entries");
        }
    }
    private static synchronized void update(String next){status=next;}
    private static String safeMessage(Exception error){
        String msg=error.getMessage();
        if(msg==null)return error.getClass().getSimpleName();
        return msg.substring(0,Math.min(180,msg.length()));
    }
    private static boolean isAuthorizationOrQuotaError(Exception error){
        String message=safeMessage(error).toLowerCase(java.util.Locale.ROOT);
        return message.contains("limit")||message.contains("unauthorized")
                ||message.contains("authentication")||message.contains("401")
                ||message.contains("429")||message.contains("token expired");
    }
}
