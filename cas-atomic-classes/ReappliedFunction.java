import java.util.concurrent.atomic.AtomicLong;

// "The Loop Can Run Your Code Twice". updateAndGet with a function
// that counts its own calls. The Javadoc says the function "may be
// re-applied when attempted updates fail due to contention among
// threads".
public class ReappliedFunction {

    static final AtomicLong value = new AtomicLong();
    static final AtomicLong calls = new AtomicLong();
    static final int PER_THREAD = 1_000_000;

    public static void main(String[] args) throws Exception {
        int threads = Integer.parseInt(args[0]);
        Thread[] ts = new Thread[threads];
        for (int k = 0; k < threads; k++) {
            ts[k] = new Thread(() -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    value.updateAndGet(v -> {
                        calls.incrementAndGet();   // a side effect
                        return v + 1;
                    });
                }
            });
            ts[k].start();
        }
        for (Thread t : ts) t.join();

        long updates = (long) threads * PER_THREAD;
        System.out.printf("%d thread%s: %,d calls for %,d updates%n",
                threads, threads == 1 ? " " : "s", calls.get(), updates);
        boolean ok = value.get() == updates
                && (threads == 1 ? calls.get() == updates
                                 : calls.get() > updates);
        if (!ok) {
            System.out.println("<<< UNEXPECTED: expected calls "
                    + (threads == 1 ? "==" : ">") + " updates");
        }
    }
}
