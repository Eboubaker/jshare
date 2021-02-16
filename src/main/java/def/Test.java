package def;

import net.NetworkHandler;
import org.apache.commons.cli.*;
import transfer.Receiver;
import transfer.Sender;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.MalformedInputException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Test{
    static File TARGET_DIR;

    static void redirectExceptions() throws FileNotFoundException {
        System.setErr(new PrintStream(new File("crash.log")));
    }
    static void startWithArgs(String[] args) {
        Options sendopt = new Options();
        sendopt.addOption(Option.builder("f").hasArgs()
                        .argName("input_files")
                        .desc("set the files to be sent").required(true).build());
        sendopt.addOption(Option.builder("i").hasArg()
                .argName("ip")
                .desc("set the ip address of the receiver").optionalArg(true).build());

        HelpFormatter sendopthelper = new HelpFormatter();


        Options recopt = new Options();
        recopt.addOption(Option.builder("o").hasArgs().argName("output_dir")
                .desc("set the output folder").optionalArg(true).build());
        recopt.addOption(Option.builder("i").hasArg().argName("ip")
                .desc("set the ip address of the sender").optionalArg(true).build());

        HelpFormatter recopthelper = new HelpFormatter();
        CommandLine cmd = null;

        if(args.length<1||(!args[0].equalsIgnoreCase("send")&&!args[0].equalsIgnoreCase("get"))){
            sendopthelper.printHelp("send -f <\"directory1\" [\"directory2\"] [\"directory..n\"]> [-i ip_adress]", sendopt);
            recopthelper.printHelp("get [-o <\"output directory\">] [-i \"ip_address\"]", sendopt);
            System.exit(0);
        }
        if(args[0].equalsIgnoreCase("get")){
            try {
                cmd = new DefaultParser().parse(recopt, args);
            }catch(ParseException e){
                System.out.println(">> Invalid Parameters for function get");
                recopthelper.printHelp("get [-o <\"output directory\">] [-i \"ip_address\"]", sendopt);
                System.exit(1);
            }
            String ip = null;
            if(cmd.hasOption("i")){
                if(!Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$").matcher(cmd.getOptionValue('i')).find()){
                    System.out.println(">> Error, the given ip address is not a valid IPv4 address");
                    System.exit(3);
                }
                ip = cmd.getOptionValue('i');
            }
            File out = null;
            if(cmd.hasOption("o")){
                out = new File(cmd.getOptionValue('o'));
                if((out.exists()&&out.isFile())
                        ||(!out.exists()&&!out.mkdirs())){
                    System.out.println(">> Error, could not create the given output folder");
                    System.exit(3);
                }
            }
            Receiver receiver = new Receiver();
            if(ip!=null&&out!=null)
                receiver.startReceiveOperation(ip,out);
            else if(ip!=null)
                receiver.startReceiveOperation(ip);
            else if(out!=null)
                receiver.startReceiveOperation(out);
            else
                receiver.startReceiveOperation();
        }else if(args[0].equalsIgnoreCase("send")){
            try {
                cmd = new DefaultParser().parse(sendopt, args);
            }catch(ParseException e){
                System.out.println(">> Invalid Parameters for function send");
                sendopthelper.printHelp("send -f <\"directory1\" [\"directory2\"] [\"directory..n\"]> [-i ip_adress]", sendopt);
                System.exit(1);
            }
            String ip = null;
            if(cmd.hasOption("i")){
                if(!Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$").matcher(cmd.getOptionValue('i')).find()){
                    System.out.println("Error, the given ip address is not a valid IPv4 address");
                    System.exit(3);
                }
                ip = cmd.getOptionValue('i');
            }

            String[] paths = new String[0];
            try {
                paths = Utility.getPathsFromArgValues(cmd.getOptionValues('f'));
            } catch (MalformedInputException e) {
                System.out.println(">> Invalid Value for Parameter -f");
                System.out.println(">> Invalid Parameters for function send");
                sendopthelper.printHelp("send -f <\"directory1\" [\"directory2\"] [\"directory..n\"]> [-i ip_adress]", sendopt);
            }
//            System.out.println(Arrays.deepToString(paths));
//            System.exit(0);
            if(paths.length==0){
                System.out.println("Error, no files were given");
                System.exit(4);
            }
            File[] files = new File[paths.length];
            for(int i = 0; i < paths.length; i++){
                files[i] = new File(paths[i]);
                if(!files[i].exists()){
                    System.out.printf("Error, The File does not exist: \"%s\"\n",paths[i]);
                    System.exit(2);
                }
                if(!files[i].canRead()){
                    System.out.printf("Error, The File is being used by another Process: \"%s\"\n",paths[i]);
                    System.exit(2);
                }
                Sender sender = new Sender();
                if(ip!=null)
                    sender.startTransferOperation(ip, files);
                else
                    sender.startTransferOperation(files);
            }
        }

    }
    public static void main(String[] args) throws Exception {
//        NetworkHandler.refreshNetworks();
//        redirectExceptions();
        if(!Utility.isRunningAsAdministrator()){
            System.out.println("This Program must be run with administrator privileges");
            //System.exit(9);
        }
        if(args.length==0)
            args = getArgs();
        startWithArgs(args);
    }//8.30 m.s
    private static String[] getArgs() {
        System.out.print("Args please: ");
        return Utility.console.nextLine().split(" ");
    }
}
