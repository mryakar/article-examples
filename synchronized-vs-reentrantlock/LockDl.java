import java.util.concurrent.locks.ReentrantLock;

public class LockDl {
    static final ReentrantLock A = new ReentrantLock();
    static final ReentrantLock B = new ReentrantLock();

    public static void main(String[] args) throws Exception {
        new Thread(() -> { A.lock(); nap(); B.lock(); }, "t1").start();
        new Thread(() -> { B.lock(); nap(); A.lock(); }, "t2").start();
        Thread.sleep(600_000);
    }

    static void nap() { try { Thread.sleep(300); } catch (InterruptedException e) {} }
}
