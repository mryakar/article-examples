import java.util.concurrent.atomic.AtomicInteger;

// "One Instruction Underneath". Two ways to add one. Run it with the
// JVM flags in the README to print the machine code of inc().
// Argument "contended": four threads call inc(), so the JIT sees
// compare-and-set failures and compiles a real retry loop.
public class Inc {

    static final AtomicInteger c = new AtomicInteger();

    static int inc() {
        return c.getAndIncrement();
    }

    static int casLoop() {
        int v;
        do {
            v = c.get();
        } while (!c.compareAndSet(v, v + 1));
        return v;
    }

    public static void main(String[] args) throws Exception {
        boolean contended = args.length > 0 && args[0].equals("contended");
        int threads = contended ? 4 : 1;
        Thread[] ts = new Thread[threads];
        for (int k = 0; k < threads; k++) {
            ts[k] = new Thread(() -> {
                long s = 0;
                for (int i = 0; i < 5_000_000; i++) {
                    s += inc();
                    s += casLoop();
                }
                if (s == 42) System.out.print("");
            });
            ts[k].start();
        }
        for (Thread t : ts) t.join();
    }
}
