import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

// "No Lock Does Not Mean No Waiting". A compare-and-set loop that
// counts its own failures. No thread ever waits for a lock. Argument:
// number of threads, or "dump" to take a thread dump of 8 threads while
// they run.
public class CasContention {

    static final AtomicLong counter = new AtomicLong();
    static final int PER_THREAD = 1_000_000;
    static volatile boolean stop = false;

    static long failures = 0;   // summed after join
    static long worst = 0;

    public static void main(String[] args) throws Exception {
        if (args[0].equals("dump")) {
            dump();
            return;
        }
        int threads = Integer.parseInt(args[0]);
        long[] fails = new long[threads];
        long[] worstTries = new long[threads];
        Thread[] ts = new Thread[threads];
        for (int k = 0; k < threads; k++) {
            final int id = k;
            ts[k] = new Thread(() -> {
                long f = 0, w = 0;
                for (int i = 0; i < PER_THREAD; i++) {
                    long tries = 0;
                    while (true) {
                        tries++;
                        long v = counter.get();
                        if (counter.compareAndSet(v, v + 1)) break;
                        f++;
                    }
                    if (tries > w) w = tries;
                }
                fails[id] = f;
                worstTries[id] = w;
            });
            ts[k].start();
        }
        for (Thread t : ts) t.join();
        for (int k = 0; k < threads; k++) {
            failures += fails[k];
            worst = Math.max(worst, worstTries[k]);
        }

        long ops = (long) threads * PER_THREAD;
        System.out.printf("%d thread%s: %.2f failed per success, worst %d%n",
                threads, threads == 1 ? " " : "s",
                (double) failures / ops, worst);
        boolean ok = counter.get() == ops
                && (threads == 1 ? failures == 0 && worst == 1
                                 : failures > 0 && worst >= 2);
        if (!ok) {
            System.out.println("<<< UNEXPECTED: expected "
                    + (threads == 1 ? "0 failures" : "failures > 0, worst >= 2"));
        }
    }

    static void dump() throws Exception {
        int threads = 8;
        List<Thread> ts = new ArrayList<>();
        for (int k = 0; k < threads; k++) {
            Thread t = new Thread(() -> {
                while (!stop) {
                    long v = counter.get();
                    counter.compareAndSet(v, v + 1);
                }
            }, "worker-" + k);
            t.start();
            ts.add(t);
        }
        Thread.sleep(1000);

        String jstack = System.getProperty("java.home") + "/bin/jstack";
        long pid = ProcessHandle.current().pid();
        Process p = new ProcessBuilder(jstack, "-l", Long.toString(pid))
                .redirectErrorStream(true).start();
        List<String> lines = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) lines.add(line);
        }
        p.waitFor();
        stop = true;
        for (Thread t : ts) t.join();

        int runnable = 0, waitingToLock = 0, parked = 0, seen = 0;
        boolean inWorker = false;
        for (String line : lines) {
            if (line.startsWith("\"")) {
                inWorker = line.startsWith("\"worker-");
                if (inWorker) seen++;
                continue;
            }
            if (!inWorker) continue;
            if (line.contains("java.lang.Thread.State: RUNNABLE")) runnable++;
            if (line.contains("waiting to lock")) waitingToLock++;
            if (line.contains("parking to wait for")) parked++;
        }
        boolean deadlock = lines.stream()
                .anyMatch(l -> l.contains("Found one Java-level deadlock"));

        System.out.printf("dump: RUNNABLE %d/%d, waiting to lock %d,"
                + " parked %d%n", runnable, seen, waitingToLock, parked);
        System.out.printf("dump: deadlock reported: %s%n",
                deadlock ? "yes" : "no");
        boolean ok = seen == threads && runnable == threads
                && waitingToLock == 0 && parked == 0 && !deadlock;
        if (!ok) {
            System.out.println("<<< UNEXPECTED: expected 8/8 RUNNABLE,"
                    + " nothing waiting, no deadlock");
        }
    }
}
