package def;

import java.io.*;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.charset.MalformedInputException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public class Utility {
    public static final int CPU_CORES = 1;//getNumberOfCPUCores();
    public static final int KB_SIZE = 1024;
    public static final int MB_SIZE = KB_SIZE * 1024;
    public static final int GB_SIZE = MB_SIZE * 1024;
    public static final int PACKET_SIZE = KB_SIZE * 16;
    public static final int JShare_PORT = 9991;
    public static final Scanner console = new Scanner(System.in);
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String appdir = getAppDirectory();

    private static String getAppDirectory() {
        try {
            return new File(Utility.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI()).getParent();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getFileHash(File f, String algorithm) throws IOException, NoSuchAlgorithmException {
        FileInputStream stream = new FileInputStream(f);
        MessageDigest hash = MessageDigest.getInstance(algorithm);
        int read = 0;
        byte[] buffer = new byte[16*KB_SIZE];
        while ((stream.read(buffer))>0)
            hash.update(buffer, 0, read);
        return String.format("%0"+hash.getDigestLength()+"x",new BigInteger(1, hash.digest()));
    }
    public static long getSize(File file){
        long size = 0;
        if(file.isDirectory()) {
            for(File f: file.listFiles())
                size += getSize(f);
        }
        else
            size += file.length();
        return size;
    }
    public static void sleep(int timeout){
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {}
    }

    /**
     * Cross-Platform
     * @return int
     */
    public static int getNumberOfCPUCores() {
        String command = "";
        if(OSValidator.isMac()){
            command = "sysctl -n machdep.cpu.core_count";
        }else if(OSValidator.isUnix()){
            command = "lscpu";
        }else if(OSValidator.isWindows()){
            command = "cmd /C WMIC CPU Get /Format:List";
        }
        Process process = null;
        int numberOfCores = 0;
        int sockets = 0;
        try {
            if(OSValidator.isMac()){
                String[] cmd = { "/bin/sh", "-c", command};
                process = Runtime.getRuntime().exec(cmd);
            }else{
                process = Runtime.getRuntime().exec(command);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        String line;

        try {
            while ((line = reader.readLine()) != null) {
                if(OSValidator.isMac()){
                    numberOfCores = line.length() > 0 ? Integer.parseInt(line) : 0;
                }else if (OSValidator.isUnix()) {
                    if (line.contains("Core(s) per socket:")) {
                        numberOfCores = Integer.parseInt(line.split("\\s+")[line.split("\\s+").length - 1]);
                    }
                    if(line.contains("Socket(s):")){
                        sockets = Integer.parseInt(line.split("\\s+")[line.split("\\s+").length - 1]);
                    }
                } else if (OSValidator.isWindows()) {
                    if (line.contains("NumberOfCores")) {
                        numberOfCores = Integer.parseInt(line.split("=")[1]);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(OSValidator.isUnix()){
            return numberOfCores * sockets;
        }
        return numberOfCores;
    }
    /*public static String runAsAdmin(String file, String args, int timeout){
        try {
            String arglist = "\""+args.trim().replaceAll(" ", "\",\"")+"\"";
//            System.out.println("run: " + "powershell -WindowStyle Hidden Start-Process \""+file+"\" -Verb runAs -WindowStyle Hidden -argumentlist " + arglist);
            Process exec = Runtime.getRuntime().exec("powershell -WindowStyle Hidden Start-Process \""+file+"\" -Verb runAs -WindowStyle Hidden -argumentlist " + arglist);
            exec.waitFor(timeout, TimeUnit.MILLISECONDS);
            return new String(exec.getInputStream().readAllBytes());
        } catch (IOException | InterruptedException e) {
            System.err.println("error on :  def.Utility.runAsAdmin()");
            System.out.println("file = " + file + ", args = " + args);
        }
        return null;
    }
    public static String runAsAdmin(String file, String args){
        return runAsAdmin(file, args, 3000);
    }*/

    /**
     * From stack overflow, thought of creating a file in windows directory but this is a better approach
     * https://stackoverflow.com/a/23538961/10387008
     * @return
     */
    public static boolean isRunningAsAdministrator(){
        synchronized (System.err){
            System.setErr(new PrintStream(new OutputStream(){public void write(int b){}}));
            try{
                Preferences preferences = Preferences.systemRoot();
                preferences.put("foo", "bar"); // SecurityException on Windows
                preferences.remove("foo");
                preferences.flush(); // BackingStoreException on Linux <-- but it did to me too!
                return true;
            } catch (Exception exception){
                return false;
            } finally{
                System.setErr(System.err);
            }
        }
    }
    public static Process exec(String command) throws IOException {
        return exec(command, 3000);
    }
    public static String[] execv(String command) throws IOException {
        return getOutput(exec(command));
    }
    public static String[] execv(String command, int timeout) throws IOException {
        return getOutput(exec(command,timeout));
    }
    public static String[] getOutput(Process p){
        try {
            Scanner reader = new Scanner(p.getInputStream());
            ArrayList<String> lines = new ArrayList<>();
            while(reader.hasNext()){
                String l = reader.nextLine().trim();
                if(l.length()>0)
                    lines.add(l);
            }
            return lines.toArray(new String[]{});
        } catch (NullPointerException  e) {}
        return null;
    }
    public static Process exec(String command, int timeout) throws IOException {
//        System.out.println("run: " + command);
        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe", "/c", command);
        builder.redirectErrorStream(false);
        Process p = builder.start();
        try {
            p.waitFor(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return p;
    }
    public static String powershell(String args) throws IOException {
        Process powerShellProcess = Runtime.getRuntime().exec("powershell.exe " + args);
        // Getting the results
        String out = "";
        try {
            powerShellProcess.waitFor(3, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {}
        powerShellProcess.getOutputStream().close();
        String line;
        BufferedReader stdout = new BufferedReader(new InputStreamReader(
                powerShellProcess.getInputStream()));
        while ((line = stdout.readLine()) != null) {
            out+=line+"\n";
        }
        if(out.length()>0)
            out = out.substring(0, out.length()-1);
        stdout.close();
        return out;
    }
    public static String base64encode(String s){
        return new String(Base64.getEncoder().encode(s.getBytes()));
    }
    public static String base64decode(String s){
        return new String(Base64.getDecoder().decode(s.getBytes()));
    }
    public static String[] getPathsFromArgValues(String ...argValues) throws MalformedInputException {
        List<String> tpaths = new ArrayList<String>();
        int off = 0;
        for(String s: argValues){
            s = s.trim();
            if(s.length()<=1&&s.startsWith("\"")){
                throw new MalformedInputException(0);
            }
            if(tpaths.size()==off){
                if(s.startsWith("\"")){
                    tpaths.add(s.substring(1)+" ");
                }else {
                    tpaths.add(s+" ");
                }
            }else{
                if(s.endsWith("\"")){
                    tpaths.set(off, tpaths.get(off)+s.substring(0, s.length()-1));
                    off++;
                }else{
                    tpaths.set(off, tpaths.get(off)+s+" ");
                }
            }
        }
        int last = tpaths.size()-1;
        String lastv = tpaths.get(last);
        if(lastv.endsWith(" "))
            tpaths.set(last,lastv.substring(0, lastv.length()-1));
        return tpaths.toArray(new String[1]);
    }
    public static Object readObject (InputStream stream) throws IOException {
        DataInputStream withMetaReader = new DataInputStream(stream);
        int remaining = withMetaReader.readInt();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int read = 0;
        byte[] buffer = new byte[Utility.PACKET_SIZE];
        while((read=withMetaReader.read(buffer))>0){
            bytes.write(buffer, 0, read);
            remaining -= read;
        }
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static boolean writeObject (OutputStream stream, Object obj) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream objWriter = new ObjectOutputStream(out);
            objWriter.writeObject(obj);
            objWriter.close();
            DataOutputStream withMeta = new DataOutputStream(stream);
            byte[] tosend = out.toByteArray();
            withMeta.writeInt(tosend.length);
            withMeta.write(tosend);
            stream.close();//we must flush
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}


class OSValidator {
    private static String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

    public static boolean isMac() {
        return (OS.indexOf("mac") >= 0);
    }

    public static boolean isUnix() {
        return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);
    }

    public static boolean isSolaris() {
        return (OS.indexOf("sunos") >= 0);
    }

    public static String getOS() {
        if (isWindows()) {
            return "win";
        } else if (isMac()) {
            return "osx";
        } else if (isUnix()) {
            return "uni";
        } else if (isSolaris()) {
            return "sol";
        } else {
            return "err";
        }
    }

}