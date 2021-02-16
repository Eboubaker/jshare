package transfer;

import classes.ItemsList;
import classes.ParsedSize;
import classes.TransferItem;
import def.Utility;
import net.NetworkHandler;
import operation.SendOperation;
import operation.Tracker;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;


public class Sender
{
    private String ip;
    private Tracker tracker;


    public Sender()
    {
        this.tracker = new Tracker();
    }


    private String getIPInput()
    {
        boolean ok = false;
        String sip;
        do
        {
            System.out.print("IP: ");
            sip = Utility.console.nextLine();
            ok = Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$").matcher(sip).find();
            if (!ok)
                System.out.println("Please Enter a Valid IPv4 Address");
        } while (!ok);
        return sip;
    }


    public void startTransferOperation(File... files)
    {
        tracker.setStage("Searching for available JNetworks");
        this.ip = NetworkHandler.connectToJNetwork();
        startTransferOperation(this.ip, files);
    }


    public void startTransferOperation(String ip, File... files)
    {
        tracker.setStage("Sending Signal to the Receivers");
        this.ip = ip;
        int connectTries = 10;
        for (int i = 0; i < connectTries; i--)
        {
            try (Socket signal = new Socket(this.ip, Utility.JShare_PORT))
            {
                tracker.setStage("Signal Received");
                break;
            } catch (IOException e)
            {
//                e.printStackTrace();
                if (i == connectTries - 1)
                {
                    System.out.printf("Connection failed with `%s`\n", ip);
                    System.exit(1242);
                }
                try
                {
                    NetworkHandler.refreshNetworks();
                    Thread.sleep(1000);
                } catch (InterruptedException ex)
                {
                }
            }
        }
        tracker.setStage("Preparing Files");
        ItemsList items = new ItemsList();
        for (File transferItem : files)
        {
            items.addAll(TransferItem.getItemsFromPath(transferItem, ""));
        }
        long totalSize = 0;
        for (TransferItem item : items)
        {
            totalSize += item.getSize();
        }
        tracker.setTotal(totalSize);
        ArrayBlockingQueue<TransferItem> works = new ArrayBlockingQueue<TransferItem>(items.size());
        works.addAll(items);
        items.setSize(totalSize);
        tracker.setStage("Sending MetaInfo");
        items.sendItemsMetaData(this.ip);
        List<Thread> workingThreads = new ArrayList<Thread>();

        tracker.setStage("Sending");
        tracker.start();
        this.tracker.startTracking();
        Date start = new Date();
        for (int i = 0; i < Utility.CPU_CORES; i++)
        {
            Thread t = new SendOperation(this.ip, works, this.tracker);
            workingThreads.add(t);
            t.start();
        }
        workingThreads.forEach(t ->
        {
            try
            {
                t.join();
            } catch (InterruptedException e)
            {
            }
        });
        long delta = (new Date().getTime() - start.getTime()) / 1000;
        delta = delta == 0 ? 1 : delta;
        long size = items.getSize();
        ParsedSize p = ParsedSize.parseWithTime(size, delta);
        ParsedSize tot = ParsedSize.parse(size);
        System.out.println(String.format("T- sent %.2f%s in %s (AVG_SPEED:%.2f%s/s)", tot.getSize(), tot.getUnit(), String.format("%02dm:%02ds", (delta / 3600 * 60 + ((delta % 3600) / 60)), (delta % 60)), p.getSize(), p.getUnit()));
    }
}