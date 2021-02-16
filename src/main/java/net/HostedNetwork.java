package net;

import def.Utility;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;

public class HostedNetwork {
    static boolean addedHook = false;
    private static String HNMac;
    public static boolean EndAndWait(int tries){
        stop();
        try {
            while (getAdress()!= null) {
                Utility.sleep(1000);
                if (--tries < 0) {
                    return false;
                }
            }
        }catch (Exception e){}
        return true;
    }
    public static String getPhysicalAddress() throws IOException {
        if (HNMac != null)
            return HNMac;
        try {
            String line = Utility.execv("netsh wlan show hostednetwork|find \"BSSID\"")[0];
            HNMac =  line.substring(line.indexOf(":")+1).trim().toUpperCase();
            if(HNMac.equals(""))
                HNMac =null;
            return HNMac;
        } catch (NullPointerException e) {}
        return null;
    }
    public static boolean isWorking(){
        try {
            for(String line: Utility.execv("netsh wlan show hostednetwork|find \"BSSID\"")){
                if(line.contains("BSSID"))
                    return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    public static String getAdress() throws IOException {
        ArrayList<String> targets = new ArrayList<>();
        String HN_ipv6 = getPhysicalAddress();
        for(Enumeration adapters = NetworkInterface.getNetworkInterfaces(); adapters.hasMoreElements();){
            NetworkInterface adapter = (NetworkInterface) adapters.nextElement();
            Enumeration<InetAddress> addresses =  adapter.getInetAddresses();
            if(!addresses.hasMoreElements()||!adapter.isUp())
                continue;
            while(addresses.hasMoreElements()){
                InetAddress address = addresses.nextElement();
                try {
                    if(address instanceof Inet4Address && !address.getHostAddress().startsWith("127")
                            && NetworkHandler.getPhysicalAddress(adapter).equalsIgnoreCase(HN_ipv6))
                        targets.add(address.getHostAddress());
                }catch (IOException ignored){}
            }
        }
        targets.removeIf(value -> value.startsWith("127."));
        for(String s: targets)
            if(s.contains(".137."))
                return s;
        return targets.size()>0?targets.get(0):null;
    }
    private static String start(int tries) throws IOException{
        if(!addedHook){
            Runtime.getRuntime().addShutdownHook(new Thread(()-> {
                HostedNetwork.stop();
            }));
            addedHook = true;
        }
        Process p = Utility.exec("netsh wlan start hostednetwork");
        if(p.exitValue()!=0)
            throw new IOException("Can't create JShare Network");
        String ip;
        while ((ip=getAdress()) == null) {
            Utility.sleep(1000);
            if (--tries < 0) {
                throw new IOException("Can't find Hosted network address");
            }
        }
        return ip;
    }
    public static String startAndWait(int tries) throws IOException {
        int ntries = tries;
        Process p = Utility.exec("netsh wlan start hostednetwork");
        if(p.exitValue()!=0)
            throw new IOException("Failed to start JShare Network");
        String ip;
        try {
            while ((ip=getAdress()) == null) {
                Utility.sleep(1000);
                if (--tries < 0) {
                    Utility.exec("netsh wlan stop hostednetwork");
                    throw new IOException("Can't find Hosted network address");
                }
            }
        } catch (IOException e) {
            Utility.exec("netsh wlan stop hostednetwork");
            throw e;
        }
        ip = Utility.base64encode(ip.substring("192.168.".length()));
        setup("JS_"+ip, ip);
        EndAndWait(5);
        return start(ntries);
    }
    public static boolean stop(){
        try {
            Process p = Utility.exec("netsh wlan stop hostednetwork");
            return p.exitValue() == 0;
        }catch(IOException e){

        }
        return false;
    }
    public static boolean setup(String ssid, String key){
        try {
            Process p = Utility.exec(String.format("netsh wlan set hostednetwork mode=allow ssid=%s key=%s", ssid.replaceAll("=","-"),key.replaceAll("=","-")));
            return p.exitValue()==0;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
