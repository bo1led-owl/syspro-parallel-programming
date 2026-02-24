import java.lang.Thread;

public class Deadlock1 {
  public static void main() throws Exception {
    Thread.currentThread().join();
  }
}
