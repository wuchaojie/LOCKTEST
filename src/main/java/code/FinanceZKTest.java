package code;

public class FinanceZKTest {
    static int inventory = 1;
    private static final int NUM = 5;


    public static void main(String[] args) {
        for (int i = 0; i < NUM; i++) {
            new Thread(() -> {
                String name = Thread.currentThread().getName();

                FinanceZKLock zkLock = new FinanceZKLock();
                String lock = zkLock.lock();
                try {
                    Thread.sleep(2000);
                    if (inventory > 0) {
                        inventory--;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println(name + " 尝试释放锁 " + lock);
                    zkLock.unlock(lock);
                    System.out.println("库存余量" + inventory);
                }
            }).start();
        }
    }
}
