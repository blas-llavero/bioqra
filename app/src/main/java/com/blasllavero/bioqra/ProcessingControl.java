package com.blasllavero.bioqra;
final class ProcessingControl {
    private static boolean paused,stopped;
    private static long generation;
    static synchronized void pause(){if(!stopped)paused=true;}
    static synchronized void stop(){stopped=true;paused=false;generation++;ProcessingControl.class.notifyAll();}
    static synchronized void resume(){paused=false;stopped=false;ProcessingControl.class.notifyAll();}
    static synchronized boolean paused(){return paused;}
    static synchronized boolean stopped(){return stopped;}
    static synchronized void styleButton(android.widget.Button button,String action){
        boolean selected=paused?"Pause processing".equals(action):
            stopped?"Stop processing".equals(action):"Resume processing".equals(action);
        button.setSelected(selected);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
            selected?0xff888888:0xff18485b));
        button.setTextColor(0xffffffff);
    }
    static synchronized long token(){return generation;}
    static synchronized String label(){return stopped?"Processing stopped":paused?"Processing paused":"Processing resumed";}
    static synchronized void checkpoint(long token) throws InterruptedException {
        while(paused && token==generation)ProcessingControl.class.wait();
        if(stopped||token!=generation)throw new InterruptedException("Processing stopped");
    }
}
