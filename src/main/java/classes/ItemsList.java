package classes;

import def.Utility;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;

public class ItemsList extends ArrayList<TransferItem> implements Serializable {
    private long size;
    public String machineName = "Default_Machine";
    public ItemsList(){
        super();
        String n = null;
        try {
            n = Utility.execv("hostname")[0].trim().replaceAll(" ", "_");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(n!=null&&!n.equalsIgnoreCase(""))
            machineName = n;
    }
    public boolean sendItemsMetaData(String ip){
        try {
            return Utility.writeObject(new Socket(ip, Utility.JShare_PORT).getOutputStream(), this);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.printf("Failed to Send MetaDataInfo to `%s`\n", ip);
            System.exit(1);
        }
        return false;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getSize() {
        return size;
    }
}