package operation;

import classes.ItemsList;
import classes.TransferItem;
import def.Utility;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ReceiveOperation extends Thread{
    private Tracker tracker;
    private ServerSocket JShareNetwork;
    private File outputFolder;
    private AtomicInteger remainingItems;
    public static AtomicReference<String> machineName = new AtomicReference<String>("Unnamed");
    public ReceiveOperation(ServerSocket JShareNetwork, String outputFolder, Tracker tracker, ItemsList list){
        this.tracker = tracker;
        this.JShareNetwork = JShareNetwork;
        this.remainingItems = new AtomicInteger(list.size());
        this.machineName.set(list.machineName);
        this.outputFolder = new File(outputFolder+File.separator+this.machineName);
    }
    @Override
    public void run() {
        while(this.remainingItems.get()>0){
            try {
                Socket senderSocket = JShareNetwork.accept();
                DataInputStream dis = new DataInputStream(senderSocket.getInputStream());
                TransferItem item = TransferItem.fromStreamMetaData(dis);
                long filesize = item.getSize();
                File out = item.getTargetFile(this.outputFolder);
                tracker.setFileName(item.getLocalPath());
//                System.out.println("Reciving item: " + item.getLocalPath());
                tracker.setFileSize(filesize);
                if (!item.isDirectory()) {
                    FileOutputStream fos = new FileOutputStream(out);
                    int read = 0;
                    long remaining = filesize;
                    byte[] buffer = new byte[Utility.KB_SIZE * 16];
                    while ((read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) > 0) {
                        remaining -= read;
                        fos.write(buffer, 0, read);
                        if (read > 0) {
                            tracker.addProgress(read);
                        }
                    }
                    fos.close();
                    String name = out.getAbsolutePath();
                    out.renameTo(new File(name.substring(0, name.lastIndexOf(TransferItem.TRANSFER_ITEM_EXTENSION_NAME))));
                }
//                System.out.println("item Recived: " + item.getLocalPath());
                this.remainingItems.decrementAndGet();
                tracker.fileCompleted();
                dis.close();
                senderSocket.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        tracker.end();
        try {
            tracker.join(2000);
        } catch (InterruptedException e) {}
    }
}
