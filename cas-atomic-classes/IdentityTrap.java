import java.util.concurrent.atomic.AtomicReference;

// "What compareAndSet Actually Compares". compareAndSet compares
// with ==, not equals(). Argument "cache1000": run it with
// -XX:AutoBoxCacheMax=1000, so 1000 is cached like 127.
public class IdentityTrap {

    public static void main(String[] args) {
        boolean bigCache = args.length > 0 && args[0].equals("cache1000");

        AtomicReference<Integer> small = new AtomicReference<>(127);
        boolean s = small.compareAndSet(127, 128);

        AtomicReference<Integer> big = new AtomicReference<>(1000);
        boolean b = big.compareAndSet(1000, 1001);

        System.out.printf("%-14s CAS(127) = %-5s CAS(1000) = %s%n",
                bigCache ? "cache 1000:" : "default JVM:", s, b);
        boolean ok = s && (bigCache ? b : !b);
        if (!ok) {
            System.out.println("<<< UNEXPECTED: expected 127 true, 1000 "
                    + (bigCache ? "true" : "false"));
        }
    }
}
