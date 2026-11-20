package com.blasllavero.bioqra;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Local PDF rendering with Android PdfRenderer, with bounded memory and pages. */
public final class ChallengePdfViewer {
    private ChallengePdfViewer(){}
    private static int dp(Activity a,int value){
        return Math.round(value*a.getResources().getDisplayMetrics().density);
    }
    public static void show(Activity activity,Uri uri,Uri tree,
                            AiPreferences.SavedResult result){
        LinearLayout layout=new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(activity,8),0,dp(activity,8),dp(activity,8));
        TextView label=new TextView(activity);
        label.setGravity(Gravity.CENTER);label.setText("Loading PDF...");
        layout.addView(label,new LinearLayout.LayoutParams(-1,dp(activity,40)));
        ImageView image=new ImageView(activity);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setAdjustViewBounds(true);
        image.setBackgroundColor(Color.WHITE);
        ScrollView scroller=new ScrollView(activity);
        scroller.addView(image,new ScrollView.LayoutParams(-1,-2));
        layout.addView(scroller,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout actions=new LinearLayout(activity);
        Button previous=new Button(activity);previous.setAllCaps(false);
        previous.setText(UiLanguage.tr(activity,"Previous"));
        Button next=new Button(activity);next.setAllCaps(false);
        next.setText(UiLanguage.tr(activity,"Next"));
        previous.setEnabled(false);next.setEnabled(false);
        actions.addView(previous,new LinearLayout.LayoutParams(0,dp(activity,54),1));
        actions.addView(next,new LinearLayout.LayoutParams(0,dp(activity,54),1));
        layout.addView(actions);

        AlertDialog dialog=new AlertDialog.Builder(activity)
            .setTitle(UiLanguage.tr(activity,"PDF preview"))
            .setView(layout).setPositiveButton(UiLanguage.tr(activity,"Close"),(d,w)->{})
            .create();
        ExecutorService executor=Executors.newSingleThreadExecutor();
        AtomicInteger version=new AtomicInteger(0);
        int[] selected={0}, total={0};
        Bitmap[] visible={null};
        boolean[] closed={false};
        class Navigator {
            void openPage(int index){
                if(closed[0]||index<0||(total[0]>0&&index>=total[0]))return;
                int request=version.incrementAndGet();
                previous.setEnabled(false);next.setEnabled(false);
                label.setText(UiLanguage.tr(activity,"Loading PDF..."));
                executor.execute(()->{
                    Bitmap rendered=null;
                    int pages=0;
                    String error=null;
                    try(ParcelFileDescriptor fd=SecurePdfAccess.open(activity,uri,tree,result);
                        PdfRenderer renderer=new PdfRenderer(fd)){
                        pages=renderer.getPageCount();
                        if(pages<1||pages>150||index>=pages)
                            throw new SecurityException("PDF page count exceeds the supported limit");
                        try(PdfRenderer.Page page=renderer.openPage(index)){
                            if(page.getWidth()<1||page.getHeight()<1)
                                throw new SecurityException("Invalid page dimensions");
                            double scale=Math.min(1.8,
                                Math.min(1100.0/page.getWidth(),1500.0/page.getHeight()));
                            int w=Math.max(1,(int)Math.round(page.getWidth()*scale));
                            int h=Math.max(1,(int)Math.round(page.getHeight()*scale));
                            if((long)w*h>2_000_000L)
                                throw new SecurityException("Page bitmap exceeds memory limit");
                            rendered=Bitmap.createBitmap(w,h,Bitmap.Config.RGB_565);
                            rendered.eraseColor(Color.WHITE);
                            page.render(rendered,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        }
                    }catch(Exception ex){error=ex.getMessage();}
                    final Bitmap output=rendered;
                    final String failure=error;
                    final int count=pages;
                    activity.runOnUiThread(()->{
                        if(closed[0]||!dialog.isShowing()||request!=version.get()){
                            if(output!=null)output.recycle();
                            return;
                        }
                        if(failure!=null){
                            label.setText(UiLanguage.tr(activity,"Could not display PDF")+": "+failure);
                            if(output!=null)output.recycle();
                            return;
                        }
                        image.setImageBitmap(output);
                        Bitmap old=visible[0];visible[0]=output;
                        if(old!=null)old.recycle();
                        total[0]=count;selected[0]=index;
                        label.setText(UiLanguage.tr(activity,"Page")+" "+(index+1)+" / "+count);
                        previous.setEnabled(index>0);
                        next.setEnabled(index+1<count);
                        scroller.scrollTo(0,0);
                    });
                });
            }
        }
        Navigator navigator=new Navigator();
        previous.setOnClickListener(v->navigator.openPage(selected[0]-1));
        next.setOnClickListener(v->navigator.openPage(selected[0]+1));
        dialog.setOnDismissListener(d->{
            closed[0]=true;version.incrementAndGet();executor.shutdownNow();
            image.setImageDrawable(null);
            if(visible[0]!=null){visible[0].recycle();visible[0]=null;}
        });
        dialog.setOnShowListener(d->{
            if(dialog.getWindow()!=null)dialog.getWindow().setLayout(-1,
                (int)(activity.getResources().getDisplayMetrics().heightPixels*0.82));
            navigator.openPage(0);
        });
        dialog.show();
    }
}
