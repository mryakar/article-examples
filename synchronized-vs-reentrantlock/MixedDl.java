import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MixedDl {
    static final Object MON = new Object();
    static final ReentrantReadWriteLock RW = new ReentrantReadWriteLock();

    public static void main(String[] args) throws Exception {
        // t1: monitor, then write lock
        new Thread(() -> {
            synchronized (MON) { nap(); RW.writeLock().lock(); }
        }, "t1").start();

        // t2: read lock, then monitor
        new Thread(() -> {
            RW.readLock().lock(); nap(); synchronized (MON) {}
        }, "t2").start();

        Thread.sleep(600_000);
    }

    static void nap() { try { Thread.sleep(400); } catch (InterruptedException e) {} }
}
