import java.util.concurrent.atomic.AtomicInteger;

// "What One Atomic Call Buys You". The counter from the first article,
// twice: two threads, a million increments each; only the counter
// type differs.
public class AtomicCounter {

    static volatile int plain = 0;
    static final AtomicInteger atomic = new AtomicInteger();
    static final int PER_THREAD = 1_000_000;

    static void twoThreads(Runnable job) throws InterruptedException {
        Thread a = new Thread(job), b = new Thread(job);
        a.start(); b.start();
        a.join();  b.join();
    }

    public static void main(String[] args) throws Exception {
        twoThreads(() -> {
            for (int i = 0; i < PER_THREAD; i++) plain++;
        });
        twoThreads(() -> {
            for (int i = 0; i < PER_THREAD; i++) atomic.incrementAndGet();
        });

        int expected = 2 * PER_THREAD;
        System.out.printf("volatile int : %,d of %,d%n", plain, expected);
        System.out.printf("AtomicInteger: %,d of %,d%n",
                atomic.get(), expected);
        if (!(plain < expected && atomic.get() == expected)) {
            System.out.println("<<< UNEXPECTED: expected volatile < "
                    + expected + ", atomic == " + expected);
        }
    }
}
