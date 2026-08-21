public class Transfer {

    static class Account {
        final String id;
        double balance;
        Account(String id, double balance) { this.id = id; this.balance = balance; }
    }

    static void transfer(Account from, Account to, double amount) {
        synchronized (from) {
            synchronized (to) {
                from.balance -= amount;
                to.balance += amount;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Account a = new Account("A", 1000);
        Account b = new Account("B", 1000);

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 100_000; i++) transfer(a, b, 1);
            System.out.println("t1 done");
        }, "t1");

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 100_000; i++) transfer(b, a, 1);
            System.out.println("t2 done");
        }, "t2");

        t1.start(); t2.start();
        t1.join();  t2.join();
        System.out.println("A=" + a.balance + "  B=" + b.balance);
    }
}
