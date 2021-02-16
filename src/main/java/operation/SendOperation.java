package operation;

import classes.TransferItem;
import def.Utility;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class SendOperation extends Thread {
    private String ip;
    private Tracker tracker;
    private ArrayBlockingQueue<TransferItem> items;
    public static ReentrantLock operations = new ReentrantLock();
    public SendOperation(String ip, ArrayBlockingQueue<TransferItem> items, Tracker tracker){
        this.ip = ip;
        this.tracker = tracker;
        this.items = items;
        if(!operations.isLocked())
            operations.lock();
    }
    @Override
    public void run() {
        while(items.size()>0) {
            try {
                Socket socket = new Socket(ip, Utility.JShare_PORT);
                DataOutputStream stream = new DataOutputStream(socket.getOutputStream());

                TransferItem item = items.poll();
                this.tracker.setFileName(item.getLocalPath());
                this.tracker.setFileSize(item.getSize());
                item.writeItemMeta(stream);
                if (!item.isDirectory()) {
                    byte[] buffer = new byte[Utility.KB_SIZE * 16];
                    FileInputStream fis = new FileInputStream(item.getItem());
                    int read = 0;
                    while ((read = fis.read(buffer)) > 0) {
                        stream.write(buffer, 0, read);
                        this.tracker.addProgress(read);
                    }
                    fis.close();
                }
                this.tracker.fileCompleted();
                stream.close();
                socket.close();
//                System.out.println("item Sent: " + item.getLocalPath());
            } catch (IOException e) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {}

                //e.printStackTrace();
            }
        }
        tracker.end();
        try {
            tracker.join(2000);
        } catch (InterruptedException e) {}
        if(operations.isHeldByCurrentThread())
            operations.unlock();
    }
}
