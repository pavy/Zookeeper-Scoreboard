import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.lang.NumberFormatException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.Scanner;
import java.util.Random;

interface IShutdownThreadParent {
    public void shutdown();
}

class ShutdownThread extends Thread {

    private IShutdownThreadParent mShutdownThreadParent;

    public ShutdownThread(IShutdownThreadParent mShutdownThreadParent) {
        this.mShutdownThreadParent = mShutdownThreadParent;
    }

    @Override
    public void run() {
        this.mShutdownThreadParent.shutdown();
    }
}

class ZPlayer implements IShutdownThreadParent
{
    ZooKeeper zk=null;
    String znode="/scoreboard";
    ZUtils zutils = new ZUtils();
    String playerName = null;


    private  ShutdownThread fShutdownThread;

    ZPlayer(String hostname, String playerName)
    {
        this.playerName = playerName;
        fShutdownThread = new ShutdownThread(this);
        Runtime.getRuntime().addShutdownHook(fShutdownThread);

        try
        {
            zk = zutils.connect(hostname);
            if(zk.exists(znode, true)==null){
                try{
                    byte[] filecontent = Files.readAllBytes(Paths.get("./content.txt"));
                    zutils.create(znode, filecontent);
                }
                catch(Exception e){
                    //e.printStackTrace();
                    System.out.println("Could not create Znode.");
                    System.exit(0);
                }
            }
            join();
        }
        catch(Exception e)
        {
            //e.printStackTrace();
            System.out.println("Unable to connect to the server.");
            System.exit(0);
        }
    }

    public void shutdown() {
        // code to cleanly shutdown your Parent instance.
        leave();
    }

    public static void main(String[] args)
    {
        String ipPort = args[0];
        String ipPortsplit[] = ipPort.split(":");
        if(ipPortsplit.length==1){
            ipPort=ipPort+":6000";
        }
        else if(ipPortsplit.length>2){
            System.out.println("Please provide IP and optional port according to below formats - \n " +
                        "xx.xx.xx.xx[:yyyy]\n");
            System.exit(0);
        }
        String playerName = args[1];
        int count=0, delay=0, score=0;
        if(args.length==5)
        {
            try
            {
                count=Integer.parseInt(args[2]);
                delay=Integer.parseInt(args[3]);
                score=Integer.parseInt(args[4]);
                if(count<0)
                    throw new IllegalArgumentException("Count cannot be negative");
                if(delay<0)
                    throw new IllegalArgumentException("Delay cannot be negative");
                if(score<0)
                    throw new IllegalArgumentException("Score cannot be negative");
            }
            catch(NumberFormatException e)
            {
                System.out.println("Interactive Player is given wrong. Please provide according to below formats - \n" +
                        "watcher 12.34.45.87:6666 N -- where N is an integer\n" +
                        "player 12.34.45.87:6666 name\n" +
                        "player 12.34.45.87:6666 \"first last\"\n" +
                        "player 12.34.45.87:6666 name count delay score -- count, delay and score values should be less than 2147483648.");
                System.exit(0);
            }
            catch(IllegalArgumentException iaex){
                System.out.println(iaex.getMessage());
                System.exit(0);
            }

        }
        else if(args.length != 2){
            System.out.println("Please provide arguments according to below formats - \n" +
                        "watcher 12.34.45.87:6666 N -- where N is an integer\n" +
                        "player 12.34.45.87:6666 name\n" +
                        "player 12.34.45.87:6666 \"first last\"\n" +
                        "player 12.34.45.87:6666 name count delay score -- count, delay and score values should be less than 2147483648.");
            System.exit(0);
        }
        ZPlayer zp =null;
        try
        {
            zp = new ZPlayer(ipPort, playerName);

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }


        Scanner scan = new Scanner(System.in);
        System.out.println("Enter your score (Ctrl+c to abort):");

        if(args.length==5)
        {
            Random randScore = new Random();
            Random randDelay = new Random();
            int nextScore=0, nextDelay=0;
            double scoreVar = 3;
            double delayVar = 3;
            while(count-->0)
            {
                nextScore = Math.max(0, score+(int) Math.round(randScore.nextGaussian() * scoreVar));
                nextDelay = Math.max(0, delay+(int) Math.round(randScore.nextGaussian() * delayVar));
                zp.post(nextScore);
                try
                {
                    Thread.sleep(nextDelay * 1000);
                }
                catch(Exception e)
                {
                    System.out.println("Player too excited.. Unable to take rest between posting.");
                }

            }

        }
        else
         {
            while (true)
            {
                String input = scan.nextLine();
                try{
                    score = Integer.parseInt(input);
                    if(score<0)
                        throw new IllegalArgumentException("Negative score.");
                    zp.post(score);
                }
                catch(Exception e){
                    System.out.println("Score should be positive integer less than 2147483648.");
                }
                System.out.println("\nEnter your score (Ctrl+c to abort):");
            }
        }

    }

    public  void join()
    {
        // get the data from znode and write it again.
        try
        {
            final CountDownLatch connectedSignal = new CountDownLatch(1);
            Stat stat = zk.exists(znode, true);
            if(stat!=null)
            {
                byte[] data = zk.getData(znode, new Watcher(){
                    public void process(WatchedEvent we){
                        if(we.getType()==Event.EventType.None)
                        {
                            switch(we.getState())
                            {
                                case Expired:
                                    connectedSignal.countDown();
                                    break;
                            }
                        }
                        else {
                            String path = "/scoreboard";

                            try {
                                byte[] bn = zk.getData(path,false, null);
                                String data = new String(bn,"UTF-8");
                                connectedSignal.countDown();

                            } catch(Exception ex) {
                                System.out.println(ex.getMessage());
                            }
                        }
                    }
                }, null);
                String dataStr = new String(data, "UTF-8");
                String[] lines = dataStr.split("\n");
                StringBuffer result = new StringBuffer();

                for(String line: lines){
                    if(line.indexOf(this.playerName)!=-1){
                        if(line.indexOf("|**")!=-1){
                            try
                            {
                                //zutils.close();
                            }
                            catch(Exception e){}
                            System.out.println("Player name already used. Please use a different name to play.");
                            System.exit(0);
                        }
                        line=line + "|**";
                    }
                    result.append(line);
                    result.append("\n");
                }

                zk.setData(znode, result.deleteCharAt(result.length()-1).toString().getBytes(), zk.exists(znode,true).getVersion());
                connectedSignal.await();
            }
            else {
                System.out.println("Node does not exists");
            }
        }
        catch(Exception e )
        {
            System.out.println("Could not check the existence of the znode.");
            //e.printStackTrace();
        }

    }

    public  void post(int score)
    {
        // get the data from the znode and write it again.
        try
        {
            final CountDownLatch connectedSignal = new CountDownLatch(1);
            Stat stat = zk.exists(znode, true);
            if(stat!=null)
            {
                byte[] data = zk.getData(znode, new Watcher(){
                    public void process(WatchedEvent we){
                        if(we.getType()==Event.EventType.None)
                        {
                            switch(we.getState())
                            {
                                case Expired:
                                    connectedSignal.countDown();
                                    break;
                            }
                        }
                        else {
                            String path = "/scoreboard";

                            try {
                                byte[] bn = zk.getData(path,false, null);
                                String data = new String(bn,"UTF-8");
                                connectedSignal.countDown();

                            } catch(Exception ex) {
                                System.out.println(ex.getMessage());
                            }
                        }
                    }
                }, null);
                String dataStr = new String(data, "UTF-8");
                String[] lines = dataStr.split("\n");
                StringBuffer result = new StringBuffer();
                result.append(this.playerName+"|"+score+"|**\n");
                for(int i=0; i<24; i++){
                    result.append(lines[i]);
                    result.append("\n");
                }
                boolean check=true;
//                System.out.println("boolean check is OK");
                int subtract=0;
                for(int i=25;i<50-subtract;i++)
                {
//                    System.out.println("Hello");
                    String line=lines[i];
                    if(check)
                    {
                        if(Integer.parseInt(line.split("\\|")[1])<=score)
                        {
                            result.append(this.playerName+"|"+score+"|**\n");
                            i--;
                            subtract++;
                            check=false;
                        }
                        else
                            result.append(line+"\n");
                    }
                    else
                    {
                        result.append(line+"\n");
                    }
                }

//                for(int i=25; i<49;i++){
//                    String line=lines[i];
//                    if(check)
//                    {
//                        if(Integer.parseInt(line.split(" ")[1]) >score)
//                            result.append(this.playerName+" "+score+" **\n");
//                        else if
//                    }
//                    if(!ishigh && Integer.parseInt(line.split(" ")[1]) <= score){
//                        result.append(this.playerName+" "+score+" **\n");
//                        ishigh=true;
//                    }
//                    result.append(line);
//                    result.append("\n");
//                }
//                if(!ishigh){
//                    result.append(lines[49]);
//                    result.append("\n");
//                }
                zk.setData(znode, result.deleteCharAt(result.length()-1).toString().getBytes(), zk.exists(znode,true).getVersion());
                connectedSignal.await();
            }
            else {
                System.out.println("Node does not exists");
            }
        }
        catch(Exception e )
        {
            System.out.println("Could not check the existence of the znode.");
            //e.printStackTrace();
        }
    }

    public void leave()
    {
        //get the data and write it again.
        try
        {
            final CountDownLatch connectedSignal = new CountDownLatch(1);
            Stat stat = zk.exists(znode, true);
            if(stat!=null)
            {
                byte[] data = zk.getData(znode, new Watcher(){
                    public void process(WatchedEvent we){
                        if(we.getType()==Event.EventType.None)
                        {
                            switch(we.getState())
                            {
                                case Expired:
                                    connectedSignal.countDown();
                                    break;
                            }
                        }
                        else {
                            String path = "/scoreboard";

                            try {
                                byte[] bn = zk.getData(path,false, null);
                                String data = new String(bn,"UTF-8");
                                connectedSignal.countDown();

                            } catch(Exception ex) {
                                System.out.println(ex.getMessage());
                            }
                        }
                    }
                }, null);
                String dataStr = new String(data, "UTF-8");
                String[] lines = dataStr.split("\n");
                StringBuffer result = new StringBuffer();
                for(String line: lines){
                    if(line.indexOf(this.playerName)!=-1 && line.indexOf("|**")!=-1){
                        line=line.substring(0,line.indexOf("|**"));
                    }
                    result.append(line);
                    result.append("\n");
                }

                zk.setData(znode, result.deleteCharAt(result.length()-1).toString().getBytes(), zk.exists(znode,true).getVersion());
                connectedSignal.await();
            }
            else {
                System.out.println("Node does not exists");
            }
        }
        catch(Exception e )
        {
            System.out.println("Could not check the existence of the znode.");
        }

        //remove the connection.
        try
        {
            zutils.close();
        }
        catch(Exception e)
        {
            System.out.println("Unable to close the connection.");
        }

    }
}