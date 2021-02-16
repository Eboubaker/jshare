package operation;

import classes.ParsedSize;

import java.io.File;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Tracker extends Thread{
    private AtomicLong transferred, progress, partialTotal, total, partialProgress;
    private AtomicBoolean stop;
    private AtomicBoolean track = new AtomicBoolean(false);
    private AtomicReference<String> fileName;
    private AtomicReference<String> fullFileName;
    private Date lastT = new Date();
    private String lastS = "";
    private String stage;
    private static final int sleepMillis = 500;
    private static final float perSecond = sleepMillis / 1000f;
    public Tracker(){
        this.transferred = new AtomicLong(0);
        this.progress = new AtomicLong(0);
        this.partialTotal = new AtomicLong(1);
        this.total = new AtomicLong(1);
        this.fileName = new AtomicReference<String>("");
        this.stop = new AtomicBoolean(false);
        this.fullFileName = new AtomicReference<String>("");
        this.partialProgress = new AtomicLong(0);
    }
    public void run(){
        lastT = new Date();
        while(!stop.get()&&!this.isInterrupted()){
            if(track.get())
                updateNow();
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {}
        }
d    }
    public void setFileSize(long v){
        this.transferred.set(0);
        this.partialTotal.set(v);
    }
    public void end(){
        stop.set(true);
        fileCompleted();
        System.out.println("Operations Tracker Out.");
    }
    public void setTotal(long v){
        this.total.set(v);
    }
    public void addProgress(long v){
        this.progress.addAndGet(v);
        this.partialProgress.addAndGet(v);
        this.transferred.addAndGet(v);
    }
    public void setFileName(String filelocalpath) {
        this.partialProgress.set(0);
        this.transferred.set(0);
        this.fullFileName.set(filelocalpath);
        this.fileName.set(filelocalpath.substring(filelocalpath.lastIndexOf(File.separator)+1));
    }
    public void setStage(String stage){
        this.stage = stage;
        System.out.println("Stage: " + this.stage+"...");
    }
    public void fileCompleted() {
        _updateNow(true);
    }
    private synchronized void _updateNow(boolean end){
        {
            char[] spaces = new char[lastS.length()];
            for (int i = 0; i < spaces.length; i++)
                spaces[i] = ' ';
            System.out.printf("\r%s",new String(spaces));
        }
        String filename = fileName.get();
        if(filename.length()>25)
            filename = filename.substring(0, 25) + "...";
        float perc = ((float)partialProgress.get()/partialTotal.get()*100);
        perc = perc > 100 ? 100f : perc;
        if(!end) {
            char[] bar = new char[50];
            int perci = (int) perc;
            for (int i = 0; i < perci/2; i++)
                bar[i] = '#';
            for (int i = perci/2; i < 50; i++)
                bar[i] = '.';
            float totalperc = ((float) progress.get() / total.get() * 100);
            long delta = (new Date().getTime()-lastT.getTime());
            delta = delta == 0 ? 1 : delta;
            ParsedSize transferred = ParsedSize.parseWithTime(this.transferred.get(), delta/1000f);
            lastT = new Date();
            String sizespeed = String.format("%.2f%s/sec", transferred.getSize(), transferred.getUnit());
            lastS = String.format("\r%s %s [%s] - %.2f%% At %s", this.stage, filename, new String(bar), totalperc, sizespeed);
            System.out.print(lastS);
            this.transferred.set(0);
        }else{
            System.out.printf("\r%s (Completed). %s\n",this.stage, fullFileName.get());
        }
    }
    public void updateNow(){
        this._updateNow(false);
    }

    public void startTracking() {
        this.track.set(true);
    }
}
