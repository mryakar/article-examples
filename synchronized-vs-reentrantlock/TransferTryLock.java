import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class TransferTryLock {

    static final AtomicLong retries = new AtomicLong();

    static class Account {
        final ReentrantLock lock = new ReentrantLock();
        double balance;
        Account(double balance) { this.balance = balance; }
    }

    static boolean transfer(Account from, Account to, double amount) throws InterruptedException {
        for (int attempt = 0; attempt < 100; attempt++) {
            if (from.lock.tryLock(50, TimeUnit.MILLISECONDS)) {
                try {
                    if (to.lock.tryLock(50, TimeUnit.MILLISECONDS)) {
                        try {
                            from.balance -= amount;
                            to.balance += amount;
                            return true;
                        } finally { to.lock.unlock(); }
                    }
                } finally { from.lock.unlock(); }   // release what we hold
            }
            retries.incrementAndGet();
            Thread.sleep(ThreadLocalRandom.current().nextInt(1, 5));  // backoff with jitter
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        Account a = new Account(1000);
        Account b = new Account(1000);

        Runnable r1 = () -> { try {
            for (int i = 0; i < 20_000; i++) transfer(a, b, 1);
        } catch (InterruptedException e) {} };

        Runnable r2 = () -> { try {
            for (int i = 0; i < 20_000; i++) transfer(b, a, 1);
        } catch (InterruptedException e) {} };

        Thread t1 = new Thread(r1, "t1");
        Thread t2 = new Thread(r2, "t2");

        long start = System.currentTimeMillis();
        t1.start(); t2.start();
        t1.join();  t2.join();
        System.out.println("completed in " + (System.currentTimeMillis() - start) + " ms");
        System.out.println("retries=" + retries.get());
        System.out.println("A=" + a.balance + "  B=" + b.balance);
    }
}
