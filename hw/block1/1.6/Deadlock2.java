import java.lang.Thread;
import java.lang.Runnable;

public class Deadlock2 {
  static volatile Runnable lambda = null;

  public static void main() throws Exception {
    Thread A = new Thread(() -> { lambda.run(); });
    Thread B = new Thread(() -> {
      try {
        A.join();
      } catch (InterruptedException e) {}
    });

    lambda = () -> {
      try {
        B.join();
      } catch (InterruptedException e) {}
    };

    A.start(); B.start();
    A.join(); B.join();
  }
}
