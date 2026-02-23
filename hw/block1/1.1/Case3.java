import java.lang.Thread;

public class Case3 {
    public static void main() throws Exception {
        Thread b = new Thread(() -> {
            throw new RuntimeException("exception from B");
        });

        Thread a = new Thread(() -> {
            b.start();
            try {
                b.join();
            } catch (InterruptedException e) {
            }
        });

        Thread c = new Thread(() -> {
            try {
                b.join();
            } catch (InterruptedException e) {
            }
        });

        Thread d = new Thread(() -> {
            try {
                a.join();
            } catch (InterruptedException e) {
            }
        });

        a.start();
        d.start();
        a.join();
        c.start();
        c.join();
        d.join();
        System.out.println("I'm alive!");
    }
};
