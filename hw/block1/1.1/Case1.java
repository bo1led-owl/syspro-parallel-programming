import java.lang.Thread;

public class Case1 {
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

        a.start();
        a.join();
        System.out.println("I'm alive!");
    }
};
