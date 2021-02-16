package transfer;

import classes.ItemsList;
import classes.ParsedSize;
import def.Utility;
import net.HostedNetwork;
import net.NetworkHandler;
import operation.ReceiveOperation;
import operation.Tracker;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Receiver
{
    private Tracker tracker;
    private String outputDirectory;
    private ServerSocket JShareNetwork;
    private ItemsList items;
    public Receiver()
    {
        this.tracker = new Tracker();
    }
    public void startReceiveOperation(File output)
    {
        tracker.setStage("Preparing JShare Network");
        try {
            String ip = HostedNetwork.startAndWait(5);
            startReceiveOperation(ip, output);
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Failed to create JShare Network");
            System.exit(1);
        }
    }
    public void startReceiveOperation()
    {
        startReceiveOperation(new File(Utility.appdir+"\\Received"));
    }
    public void startReceiveOperation(String ip)
    {
        startReceiveOperation(ip, new File(Utility.appdir+"\\Received"));
    }
    public void startReceiveOperation(String ip, File output)
    {
        try
        {
            this.outputDirectory = output.getCanonicalPath();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.out.println("Failed to set the output folder");
        }

//        this.ip = "192.168.43.53";
        tracker.setStage("Opening JShare Network");
        try{
            JShareNetwork = new ServerSocket();
            JShareNetwork.bind(new InetSocketAddress(ip, Utility.JShare_PORT ));
        }catch(Exception e){
            System.out.printf("Failed to Set a JShare Network on `%s`\n",ip);
            System.exit(1);
        }
        tracker.setStage("Waiting for Sender signal on `" + ip+"` ");
        try {
            Socket signal = JShareNetwork.accept();
            tracker.setStage("Got signal from `" + signal.getInetAddress().getHostAddress()+"`");
        } catch (IOException e) {
            System.out.println("failed to get signal");
            System.exit(1);
        }
        tracker.setStage("Getting MetaInfo");
        loadMetaData();
        tracker.setTotal(items.getSize());
        tracker.setStage("Receiving");
        tracker.startTracking();
        tracker.start();
        AtomicInteger totalItems = new AtomicInteger(this.items.size());
        List<Thread> workingThreads = new ArrayList<Thread>();
        Date start = new Date();
        for(int i = 0; i < Utility.CPU_CORES; i++){
            Thread t = new ReceiveOperation(JShareNetwork, this.outputDirectory, this.tracker, this.items);
            workingThreads.add(t);
            t.start();
        }
        workingThreads.forEach(t-> {try {t.join();} catch (InterruptedException e) {}});
        long delta = (new Date().getTime() - start.getTime())/1000;
        delta = delta == 0 ? 1 : delta;
        long size = this.items.getSize();
        ParsedSize p = ParsedSize.parseWithTime(size,delta);
        ParsedSize tot = ParsedSize.parse(size);
        System.out.println(String.format("T- Received %.2f%s in %s (AVG_SPEED:%.2f%s/s)", tot.getSize(),tot.getUnit(), String.format("%02dm:%02ds", (delta / 3600 * 60 + ((delta % 3600) / 60)), (delta % 60)), p.getSize(),p.getUnit()));
    }


    public boolean loadMetaData()
    {
        try(Socket socket = JShareNetwork.accept())
        {
            this.items = (ItemsList) Utility.readObject(socket.getInputStream());
            return true;

        }
        catch (Exception e)
        {
            e.printStackTrace();
            String ip = JShareNetwork.getInetAddress().getHostAddress();
            System.out.printf("Failed to Read MetaDataInfo from `%s`",ip==null?"0.0.0.0":ip);
        }
        return false;
    }
}