import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

// "When One Variable Is Not Enough". A limiter: at most LIMIT requests
// per window. The window start and the count must change together. Mode
// "two-atomics" keeps them in two AtomicLongs; mode "one-record" keeps
// them in one immutable record behind an AtomicReference.
public class TwoFields {

    // Start gate. Workers spin until the round number changes, so all
    // of them start within nanoseconds of each other. A CyclicBarrier
    // wakes them one by one, microseconds apart, and hides the race.
    static volatile int round = 0;

    static final int LIMIT = 10;
    static final int ROUNDS = 10_000;
    static final int THREADS = 8;
    static final int CALLS = 4;   // 8 x 4 = 32 attempts per window

    interface Limiter { boolean tryAcquire(long now); }

    static class TwoAtomics implements Limiter {
        final AtomicLong windowStart = new AtomicLong(-1);
        final AtomicLong count = new AtomicLong();
        public boolean tryAcquire(long now) {
            if (windowStart.get() != now) {
                windowStart.set(now);
                count.set(0);
            }
            return count.incrementAndGet() <= LIMIT;
        }
    }

    record Window(long start, int count) {}

    static class OneRecord implements Limiter {
        final AtomicReference<Window> window =
                new AtomicReference<>(new Window(-1, 0));
        public boolean tryAcquire(long now) {
            while (true) {
                Window w = window.get();
                Window next;
                if (w.start() != now) next = new Window(now, 1);
                else if (w.count() >= LIMIT) return false;
                else next = new Window(now, w.count() + 1);
                if (window.compareAndSet(w, next)) return true;
            }
        }
    }

    static volatile long now = 0;

    public static void main(String[] args) throws Exception {
        boolean record = args.length > 0 && args[0].equals("one-record");
        Limiter limiter = record ? new OneRecord() : new TwoAtomics();
        AtomicInteger done = new AtomicInteger();
        AtomicInteger admitted = new AtomicInteger();

        for (int t = 0; t < THREADS; t++) {
            Thread w = new Thread(() -> {
                for (int r = 1; r <= ROUNDS; r++) {
                    while (round != r) Thread.yield();
                    long n = now;
                    for (int c = 0; c < CALLS; c++) {
                        if (limiter.tryAcquire(n)) {
                            admitted.incrementAndGet();
                        }
                    }
                    done.incrementAndGet();
                }
            });
            w.setDaemon(true);
            w.start();
        }

        int over = 0, under = 0, most = 0;
        for (int r = 0; r < ROUNDS; r++) {
            now = r;
            admitted.set(0);
            round = r + 1;
            while (done.get() != (r + 1) * THREADS) Thread.yield();
            int a = admitted.get();
            if (a > LIMIT) over++;
            if (a < LIMIT) under++;
            most = Math.max(most, a);
        }

        System.out.printf("%-11s over limit in %d/%d windows, most %d%n",
                record ? "one-record" : "two-atomics", over, ROUNDS, most);
        boolean ok = record ? over == 0 && under == 0 : over > 0;
        if (!ok) {
            System.out.println("<<< UNEXPECTED: expected "
                    + (record ? "0 windows over limit" : "windows over > 0"));
        }
    }
}
