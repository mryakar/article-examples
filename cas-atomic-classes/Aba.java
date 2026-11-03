import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

// "What compareAndSet Actually Compares". The balance goes 100 -> 50 ->
// 100 while one thread is between its read and its compareAndSet. The
// order is forced with latches, so the result is the same on every run.
public class Aba {

    public static void main(String[] args) throws Exception {
        plain();
        stamped();
    }

    static void plain() throws Exception {
        AtomicInteger balance = new AtomicInteger(100);
        CountDownLatch read = new CountDownLatch(1);
        CountDownLatch changed = new CountDownLatch(1);
        boolean[] result = new boolean[1];

        Thread t1 = new Thread(() -> {
            int seen = balance.get();
            read.countDown();
            await(changed);
            result[0] = balance.compareAndSet(seen, 70);
        });
        Thread t2 = new Thread(() -> {
            await(read);
            balance.set(50);
            balance.set(100);
            changed.countDown();
        });
        t1.start(); t2.start();
        t1.join();  t2.join();

        System.out.printf("plain   : CAS(100 -> 70) = %s%n", result[0]);
        if (!result[0]) {
            System.out.println("<<< UNEXPECTED: expected true");
        }
    }

    static void stamped() throws Exception {
        AtomicStampedReference<Integer> balance =
                new AtomicStampedReference<>(100, 0);
        CountDownLatch read = new CountDownLatch(1);
        CountDownLatch changed = new CountDownLatch(1);
        boolean[] result = new boolean[1];
        int[] stamps = new int[2];

        Thread t1 = new Thread(() -> {
            int[] holder = new int[1];
            Integer seen = balance.get(holder);
            stamps[0] = holder[0];
            read.countDown();
            await(changed);
            result[0] = balance.compareAndSet(
                    seen, 70, stamps[0], stamps[0] + 1);
        });
        Thread t2 = new Thread(() -> {
            await(read);
            balance.compareAndSet(100, 50, 0, 1);
            balance.compareAndSet(50, 100, 1, 2);
            stamps[1] = balance.getStamp();
            changed.countDown();
        });
        t1.start(); t2.start();
        t1.join();  t2.join();

        System.out.printf("stamped : CAS(100 -> 70) = %s (stamp %d, now %d)%n",
                result[0], stamps[0], stamps[1]);
        if (result[0]) {
            System.out.println("<<< UNEXPECTED: expected false");
        }
    }

    static void await(CountDownLatch l) {
        try {
            l.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
