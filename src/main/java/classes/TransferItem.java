package classes;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TransferItem implements Serializable {
    private File item;
    private String localPath;
    private boolean isDirectory;
    private long itemSize;
    public static final String TRANSFER_ITEM_EXTENSION_NAME = ".JItem";
    public TransferItem(File file, String localPath, boolean isDirectory){
        this.item = file;
        if(file != null) {
            if(file.isFile()&&!file.canRead()){
                System.out.println("Error can't read File: " + file.getAbsolutePath());
                System.exit(1);
            }
            this.itemSize = file.length();
            this.localPath = localPath + File.separator + file.getName();
        }
        else {
            this.localPath = localPath;
        }
        this.isDirectory = isDirectory;
    }
    public void writeItemMeta(DataOutputStream stream) throws IOException {
        stream.writeLong(this.itemSize);
        stream.writeUTF(this.localPath);
        stream.writeBoolean(this.isDirectory);
    }
    public static TransferItem fromStreamMetaData(DataInputStream stream) throws IOException {
        long size = stream.readLong();
        String name = stream.readUTF();
        boolean isdir = stream.readBoolean();
        return new TransferItem(null, name, isdir).setItemSize(size);
    }
    /**
     * it will also create the file and it's parent folders
     * @param outputDirectory
     * @return
     */
    public File getTargetFile(File outputDirectory) throws IOException {
        File target = null;
        if(this.localPath.startsWith(File.separator)){
            target = new File(outputDirectory + this.localPath);
        }else{
            target = new File(outputDirectory + File.separator + this.localPath);
        }
        if(this.isDirectory) {
            target.mkdirs();
        }else {
            new File(target.getParent()).mkdirs();
            target = new File(target.getAbsolutePath()+TRANSFER_ITEM_EXTENSION_NAME);
            target.createNewFile();
        }
        return target;
    }
    public static List<TransferItem> getItemsFromPath(File path, String localPath){
        List<TransferItem> items = new ArrayList<TransferItem>();
        if(path.isDirectory()){
            File[] files = path.listFiles();
            if(files.length == 0)
                items.add(new TransferItem(path, localPath, true));
            for(File f: files)
                items.addAll(getItemsFromPath(f, localPath + File.separator + path.getName()));
        }else{
            items.add(new TransferItem(path, localPath, false));
        }
        return items;
    }

    public TransferItem setItemSize(long itemSize) {
        this.itemSize = itemSize;
        return this;
    }

    public boolean isDirectory() {
        return isDirectory;
    }
    public File getItem() {
        return item;
    }
    public String getLocalPath() {
        return localPath;
    }

    public long getSize() {
        return this.itemSize;
    }
}
