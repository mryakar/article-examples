public class VtDl {
    static final Object A = new Object();
    static final Object B = new Object();

    public static void main(String[] args) throws Exception {
        Thread.ofVirtual().name("vt1").start(() -> {
            synchronized (A) { nap(); synchronized (B) {} }
        });
        Thread.ofVirtual().name("vt2").start(() -> {
            synchronized (B) { nap(); synchronized (A) {} }
        });
        Thread.sleep(600_000);
    }

    static void nap() { try { Thread.sleep(400); } catch (InterruptedException e) {} }
}
