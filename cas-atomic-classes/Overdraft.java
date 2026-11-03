import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// "Every Field Is Atomic" and "Compare-and-Set". Every call on the
// balance is atomic. Mode "check-then-act": get() then addAndGet().
// Mode "cas": a compareAndSet loop. Everything else is identical.
public class Overdraft {

    // Start gate. Workers spin until the round number changes, so all
    // of them start within nanoseconds of each other. A CyclicBarrier
    // wakes them one by one, microseconds apart, and hides the race.
    static volatile int round = 0;

    static class Account {
        final String id;
        final AtomicLong balance;
        Account(String id, long balance) {
            this.id = id;
            this.balance = new AtomicLong(balance);
        }
    }

    static boolean withdraw(Account from, long amount) {
        if (from.balance.get() >= amount) {
            from.balance.addAndGet(-amount);
            return true;
        }
        return false;
    }

    static boolean withdrawCas(Account from, long amount) {
        while (true) {
            long current = from.balance.get();
            if (current < amount) return false;
            if (from.balance.compareAndSet(current, current - amount)) {
                return true;
            }
        }
    }

    static final int ROUNDS = 10_000;
    static final int THREADS = 8;
    static final long START = 100, AMOUNT = 100;

    static volatile Account a;

    public static void main(String[] args) throws Exception {
        boolean cas = args.length > 0 && args[0].equals("cas");
        AtomicInteger done = new AtomicInteger();
        AtomicInteger succeeded = new AtomicInteger();

        for (int t = 0; t < THREADS; t++) {
            Thread w = new Thread(() -> {
                for (int r = 1; r <= ROUNDS; r++) {
                    while (round != r) Thread.yield();
                    boolean ok = cas ? withdrawCas(a, AMOUNT)
                                     : withdraw(a, AMOUNT);
                    if (ok) succeeded.incrementAndGet();
                    done.incrementAndGet();
                }
            });
            w.setDaemon(true);
            w.start();
        }

        int overdrawn = 0, wrongCount = 0;
        long lowest = START;
        for (int r = 0; r < ROUNDS; r++) {
            a = new Account("A", START);
            succeeded.set(0);
            round = r + 1;
            while (done.get() != (r + 1) * THREADS) Thread.yield();
            long b = a.balance.get();
            if (b < 0) overdrawn++;
            if (succeeded.get() != 1) wrongCount++;
            lowest = Math.min(lowest, b);
        }

        String mode = cas ? "cas" : "check-then-act";
        System.out.printf("%-15s overdrawn %d/%d rounds, lowest %d%n",
                mode, overdrawn, ROUNDS, lowest);
        boolean expected = cas ? overdrawn == 0 && wrongCount == 0
                               : overdrawn > 0;
        if (!expected) {
            System.out.println("<<< UNEXPECTED: expected "
                    + (cas ? "0 overdrawn rounds" : "overdrawn rounds > 0"));
        }
    }
}
