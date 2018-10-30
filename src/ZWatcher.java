import java.util.Arrays;
import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;


public class ZWatcher implements Watcher, StatCallback
{
    ZooKeeper zk=null;
    String znode="/scoreboard";
    ZUtils zutils = new ZUtils();
    int maxlen=-1;
    byte[] currentData=null;

    ZWatcher(String hostname, int maxlen)
    {
        this.maxlen = maxlen;
        try
        {
            zk = zutils.connect(hostname);
        }
        catch(Exception e)
        {
            //e.printStackTrace();
            System.out.println("Unable to connect to the server. "+e.getMessage());
            System.exit(0);
        }

        try{
            zk.exists(znode, this, this, null);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
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
        int maxLen = 0;
        try{
            maxLen = Integer.parseInt(args[1]);
            if(maxLen<0 || maxLen>25)
                throw new IllegalArgumentException("Incorrect argument");
        }
        catch(Exception e){
            System.out.println("Please provide input according to below formats - \n " +
                        "watcher 12.34.45.87:6666 N -- where N is an integer between 0 and 25\n");
            System.exit(0);
        }

        ZWatcher zw=null;
        try
        {
            zw = new ZWatcher(ipPort, maxLen);

        }
        catch(Exception e)
        {
                e.printStackTrace();
        }
        while(true)
        {

        }
    }

    public void process(WatchedEvent event) {
//        System.out.println("Inside the process method");
        String path = event.getPath();
        if (event.getType() == Event.EventType.None) {
            switch (event.getState()) {
                case SyncConnected:
                    break;
                case Expired:
//                    listener.closing(Code.SESSIONEXPIRED);
                    break;
            }
        } else {
            if (path != null && path.equals(znode)) {
                zk.exists(znode, this, this, null);
            }
        }

    }

    public void processResult(int rc, String path, Object ctx, Stat stat)
    {
        //System.out.println("Inside process result");
        boolean printData = false;
        int ok = Code.OK.intValue();
        int nonode = Code.NONODE.intValue();
        int sessionexpired = Code.SESSIONEXPIRED.intValue();
        int noauth = Code.NOAUTH.intValue();
        if(rc==ok)
            printData=true;
        else if(rc==nonode){
            //System.out.println("nonode");
            try{
                byte[] filecontent = Files.readAllBytes(Paths.get("./content.txt"));
                zutils.create(znode, filecontent);
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }//create node &  gtbytes & set data
        else if(rc==sessionexpired||rc==noauth)
            return;
        else
        {
            zk.exists(znode, true, this, null);
            return;
        }


        byte[] data = null;
        if (printData) {
            try {
                data = zk.getData(znode, false, null);

                try
                {
                    if(currentData==null || !Arrays.equals(data, currentData))
                    {
                        String dataStr = new String(data, "UTF-8");
                        System.out.println("\nMost recent scores");
                        System.out.println("------------------");
                        int count=0;
                        for (String line : dataStr.split("\n")){
                            if((count>=0 && count<maxlen) || (count>=25 && count<25+maxlen) ){
                                String cell[] = line.split("\\|");
                                if(!cell[0].equals("#####"))
                                    System.out.format("%-20s%10s%3s%n",cell[0],cell[1],(cell.length>2?cell[2]:""));
                            }
                            count++;
                            if(count==25){
                                System.out.println("");
                                System.out.println("Highest scores");
                                System.out.println("--------------");
                            }

                        }
                        currentData=data;
//                        zk.exists(znode, this, this, null);
                    }

                }
                catch(Exception e)
                {
                    System.out.println("Could not print the data!!");
                }

            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                return;
            }
        }

    }
}
