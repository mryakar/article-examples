public class TransferOrdered {

    static class Account {
        final long id;
        double balance;
        Account(long id, double balance) { this.id = id; this.balance = balance; }
    }

    static void transfer(Account from, Account to, double amount) {
        Account first  = from.id < to.id ? from : to;
        Account second = from.id < to.id ? to   : from;
        synchronized (first) {
            synchronized (second) {
                from.balance -= amount;
                to.balance += amount;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Account a = new Account(1, 1000);
        Account b = new Account(2, 1000);

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 100_000; i++) transfer(a, b, 1);
        }, "t1");
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 100_000; i++) transfer(b, a, 1);
        }, "t2");

        long start = System.currentTimeMillis();
        t1.start(); t2.start();
        t1.join();  t2.join();
        System.out.println("completed in " + (System.currentTimeMillis() - start) + " ms");
        System.out.println("A=" + a.balance + "  B=" + b.balance);
    }
}
