package net;

import def.Utility;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NetworkHandler {
    private static File refnet;
    static String twocharformat = "%02X";
    public static String getPhysicalAddress(NetworkInterface ni) throws IOException{
        byte mac [] = ni.getHardwareAddress();
        if( mac != null ) {
            final StringBuilder macAddress = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                macAddress.append(String.format("%s"+twocharformat, (i == 0) ? "" : ":", mac[i]) );
            }
            return macAddress.toString().toUpperCase().trim();
        }else
            throw new IOException("No Mac For::"+ni.getName());
    }

    public static String connectToJNetwork(){
        int tries = 30;
        while(tries-- > 0) {
            for (String s : getAvailableJNetworks()) {
                String ip = Utility.base64decode(s.substring(s.indexOf("_") + 1).replaceAll("-","="));
                try {
                    registerProfile(s, s.substring(s.indexOf("_")+1));
                    Utility.sleep(2000);
                    Process p = Utility.exec(String.format("netsh wlan connect \"%s\"", s), 3000);
                    if (p.exitValue() == 0)
                        return "192.168." + ip;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
                return ip;
            }
            refreshNetworks();
            Utility.sleep(1000);
        }
        System.out.println("No JNetwork was found");
        return null;
    }
    public static void refreshNetworks(){
        try {
            if(refnet==null) {
                //InputStream stream = NetworkHandler.class.getResourceAsStream("refnet.exe");
//                System.out.println(stream);
                refnet = new File("JShare.exe");
//                refnet.createNewFile();
//                FileOutputStream out = new FileOutputStream(refnet);
//                stream.transferTo(out);
//                stream.close();
//                out.close();
            }
            String s = Utility.powershell("-WindowStyle Hidden start-process \"" + refnet.getCanonicalPath()+"\" -WindowStyle Hidden");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static String getSSIDHash(String ssid){
        try {
            return Utility.powershell("(\\\""+ssid+"\\\".ToCharArray() |foreach-object {'{0:X}' -f ([int]$_)}) -join ''");
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
    public static boolean registerProfile(String ssid, String key) throws IOException {
        String hash = getSSIDHash(ssid);
        File temp = File.createTempFile("netprof", ".xml");
        PrintStream stream = new PrintStream(temp);
        stream.print("<?xml version=\"1.0\"?>\n" +
                "<WLANProfile xmlns=\"http://www.microsoft.com/networking/WLAN/profile/v1\">\n" +
                "\t<name>"+ssid+"</name>\n" +
                "\t<SSIDConfig>\n" +
                "\t\t<SSID>\n" +
                "\t\t\t<hex>"+hash+"</hex>\n" +
                "\t\t\t<name>"+ssid+"</name>\n" +
                "\t\t</SSID>\n" +
                "\t</SSIDConfig>\n" +
                "\t<connectionType>ESS</connectionType>\n" +
                "\t<connectionMode>auto</connectionMode>\n" +
                "\t<MSM>\n" +
                "\t\t<security>\n" +
                "\t\t\t<authEncryption>\n" +
                "\t\t\t\t<authentication>WPA2PSK</authentication>\n" +
                "\t\t\t\t<encryption>AES</encryption>\n" +
                "\t\t\t\t<useOneX>false</useOneX>\n" +
                "\t\t\t</authEncryption>\n" +
                "\t\t\t<sharedKey>\n" +
                "\t\t\t\t<keyType>passPhrase</keyType>\n" +
                "\t\t\t\t<protected>false</protected>\n" +
                "\t\t\t\t<keyMaterial>"+key+"</keyMaterial>\n" +
                "\t\t\t</sharedKey>\n" +
                "\t\t</security>\n" +
                "\t</MSM>\n" +
                "\t<MacRandomization xmlns=\"http://www.microsoft.com/networking/WLAN/profile/v3\">\n" +
                "\t\t<enableRandomization>false</enableRandomization>\n" +
                "\t</MacRandomization>\n" +
                "</WLANProfile>");
        stream.close();
        return Utility.exec("netsh wlan add profile filename=\""+temp.getAbsolutePath()+"\"", 3000).exitValue()==0;
    }
    public static String[] getAvailableJNetworks(){
        ArrayList<String> ssids = new ArrayList<>();
        try {
            String[] lines = Utility.execv("netsh wlan show all|find \"SSID\"", 10);
            int index = 0;
            for (; index < lines.length; index++) if (lines[index].contains("SHOW NETWORKS MODE=BSSID")) break;
            for (; index < lines.length; index++) {
                if (lines[index].contains("SSID") && !lines[index].contains("BSSID")&&lines[index].contains("JS_")) {
                    ssids.add(lines[index].substring(lines[index].indexOf(":") + 1).trim());
                }
            }
        }catch (Exception e){e.printStackTrace();}
        return ssids.toArray(new String[]{});
    }
    public boolean testJShareNetwork(String ip){
        try(Socket socket = new Socket(ip, Utility.JShare_PORT)) {
            return true;
        } catch (IOException e) {
            System.out.println("No JShare Network is setup on " + ip + " or the ip is not in this network");
            return false;
        }
    }
}
