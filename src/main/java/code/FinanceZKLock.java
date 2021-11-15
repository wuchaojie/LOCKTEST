package code;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Component
public class FinanceZKLock {
    private  CountDownLatch cdl;
    private static final String IP_PORT = "127.0.0.1:2181";
    private static final String Z_NODE = "/LOCK";
    private volatile String beforeNodePath;
    private volatile String nodePath;


    ZkClient zkClient = new ZkClient(IP_PORT);

    public FinanceZKLock() {
        if (!zkClient.exists(Z_NODE)) {
            zkClient.createPersistent(Z_NODE);
        }
    }

    public String lock() {
        System.out.println(Thread.currentThread().getName() + " 尝试获取锁");
        if (tryLock()) {
            return this.nodePath;
        } else {
            // 尝试加锁
            // 进入等待 监听
            waitForLock();
        }
        return this.nodePath;
    }

    private void waitForLock() {
        IZkDataListener listener = new IZkDataListener() {
            @Override
            public void handleDataChange(String dataPath, Object data) throws Exception {
                System.out.println(Thread.currentThread().getName() + ":监听到节点改变事件！---------------------------");

            }

            @Override
            public void handleDataDeleted(String dataPath) throws Exception {
                System.out.println(dataPath + ":监听到节点删除事件！---------------------------");
                cdl.countDown();
            }
        };

        // 监听
        this.zkClient.subscribeDataChanges(beforeNodePath, listener);

        if (zkClient.exists(beforeNodePath)) {
            try {
                cdl = new CountDownLatch(1);
                System.out.println(Thread.currentThread().getName() + " 获取锁失败 等待");
                cdl.await();
                System.out.println(Thread.currentThread().getName() + " 开始干活");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 释放监听
        zkClient.unsubscribeDataChanges(beforeNodePath, listener);
    }

    private synchronized boolean tryLock() {
        // 第一次就进来创建自己的临时节点
        if (ObjectUtils.isEmpty(nodePath)) {
            nodePath = zkClient.createEphemeralSequential(Z_NODE + "/", "lock");
        }
        // 节点排序
        List<String> children = zkClient.getChildren(Z_NODE);
        Collections.sort(children);

        // 如果是最小节点 返回加锁成功
        System.out.println(Thread.currentThread().getName() + " 我拿到的节点列表：" + children);

        if (nodePath.equals(Z_NODE + "/" + children.get(0))) {
            System.out.println("最小节点 返回加锁成功 " + Thread.currentThread().getName() + " " + beforeNodePath);
            return true;
        } else {
            // 不是最小节点 找到自己的前一个
            int i = Collections.binarySearch(children, this.nodePath.substring(Z_NODE.length() + 1));
            System.out.println("i:" + i);

            beforeNodePath = Z_NODE + "/" + children.get(i - 1);
            System.out.println(Thread.currentThread().getName() + " 我监听的前一个节点：" + beforeNodePath);
        }

        return false;
    }

    public void unlock(String lock) {
        zkClient.delete(lock);
        System.out.println("unlock" + Thread.currentThread().getName() + " success");

    }
}
