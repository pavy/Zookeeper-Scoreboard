import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.*;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;

public class ZUtils
{
    private ZooKeeper zk;
    final CountDownLatch connectSignal = new CountDownLatch(1);
    String znode="/scoreboard";

    public ZooKeeper connect(String hostname) throws IOException, InterruptedException
    {
        zk = new ZooKeeper(hostname, 5000, new Watcher(){
            public void process(WatchedEvent we)
            {
                if(we.getState()==KeeperState.SyncConnected)
                {
                    connectSignal.countDown();
                }
            }
        });
        connectSignal.await();
        return zk;
    }

    public void create(String path, byte[] data) throws KeeperException, InterruptedException
    {
        zk.create(znode, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    public void close() throws InterruptedException
    {
        zk.close();
    }

}