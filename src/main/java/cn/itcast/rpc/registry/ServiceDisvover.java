package cn.itcast.rpc.registry;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class ServiceDisvover {
    private static final org.slf4j.Logger LOGGER = (org.slf4j.Logger) LoggerFactory.getLogger(ServiceRegistry.class);
    private CountDownLatch latch = new CountDownLatch(1);
    private String registryAddress;
    private volatile List<String> dataList = new ArrayList<String>();

    public ServiceDisvover(String registryAddress) {
        this.registryAddress = registryAddress;
        ZooKeeper zk = connectServer();

        if (zk != null) {
            watchNode(zk);

        }

    }

    private void watchNode(final ZooKeeper zk) {
        try {
            List<String> nodeList = zk.getChildren(Constant.ZK_REGISTRY_PATH, new Watcher() {
                @Override
                public void process(WatchedEvent event) {

                    if (event.getType() == Event.EventType.NodeChildrenChanged) {
                        watchNode(zk);
                    }
                }
            });
            for (String node : nodeList) {

                byte[] bytes = zk.getData(Constant.ZK_REGISTRY_PATH + "/" + node, false, null);
                dataList.add(new String(bytes));
            }
            LOGGER.debug("node data: {}", dataList);
            // 将节点信息记录在成员变量
            this.dataList = dataList;
        } catch (Exception e) {
            LOGGER.error("", e);
            e.printStackTrace();
        }
    }


    private ZooKeeper connectServer() {
        ZooKeeper zk = null;
        try {

           zk= new ZooKeeper(registryAddress, cn.itcast.rpc.registry.Constant.ZK_SESSION_TIMEOUT, new Watcher() {
                public void process(WatchedEvent watchedEvent) {
                    if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                        latch.countDown();
                        ;

                    }
                }
            });
            latch.await();
        } catch (Exception e) {
            LOGGER.error("", e);
            e.printStackTrace();
        }
        return zk;

    }

    public String discover() {
        String data = null;
        int size = dataList.size();
        // 存在新节点，使用即可
        if (size > 0) {
            if (size == 1) {
                data = dataList.get(0);
                LOGGER.debug("using only data: {}", data);
            } else {
                data = dataList.get(ThreadLocalRandom.current().nextInt(size));
                LOGGER.debug("using random data: {}", data);
            }
        }
        return data;
    }

}
