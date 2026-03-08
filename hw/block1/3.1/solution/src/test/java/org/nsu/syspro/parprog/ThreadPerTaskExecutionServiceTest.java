package org.nsu.syspro.parprog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

import org.junit.jupiter.api.Test;

public class ThreadPerTaskExecutionServiceTest {
    class SimpleThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r);
        }
    }

    private final ThreadFactory threadFactory = new SimpleThreadFactory();

    private ThreadPerTaskExecutorService newService() {
        return new ThreadPerTaskExecutorService(threadFactory);
    }

    @Test
    public void simpleComputations() throws ExecutionException {
        var service = newService();

        var arg1 = service.submit(() -> 42);
        var arg2 = service.submit(() -> 37);
        var res = service.submit(() -> arg1.get() + arg2.get());

        assertEquals(42 + 37, res.get());
    }

    @Test
    void aLotOfSimpleComputations() throws ExecutionException {
        var service = newService();

        int expected = 0;
        ArrayList<JoinFuture<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 100; ++i) {
            final int cur = i;

            expected += cur;
            futures.add(service.submit(() -> cur));
        }

        int actual = 0;
        for (var f : futures) {
            actual += f.get();
        }
        assertEquals(expected, actual);

        for (var f : futures) {
            assertTrue(f.isDone());
        }

        int actualYetAgain = 0;
        for (var f : futures) {
            actualYetAgain += f.get();
        }
        assertEquals(expected, actualYetAgain);
    }

    @Test
    void chainedComputations() throws ExecutionException {
        var service = newService();

        int expected = 1;
        JoinFuture<Integer> lastFuture = service.submit(() -> 1);
        for (int i = 2; i < 100; ++i) {
            final int cur = i;

            expected += cur;
            final var prevFuture = lastFuture;
            lastFuture = service.submit(() -> prevFuture.get() + cur);
        }

        assertEquals(expected, lastFuture.get());
    }

    @Test
    void exceptionThrown() throws ExecutionException {
        var service = newService();

        var future = service.submit(() -> {
            throw new IllegalStateException("it was a bomb!");
        });

        assertThrows(ExecutionException.class, () -> future.get());
        assertThrows(ExecutionException.class, () -> future.get()); // check that exception gets rethrown
        assertTrue(future.isDone());
    }

    @Test
    void exceptionsThrown() throws ExecutionException {
        var service = newService();

        int expected = 0;
        ArrayList<JoinFuture<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < 100; ++i) {
            final int cur = i;

            Callable<Integer> task;
            if (cur % 13 == 0) {
                task = () -> {
                    throw new RuntimeException("I dont like 13");
                };
            } else {
                task = () -> cur;
                expected += cur;
            }

            futures.add(service.submit(task));
        }

        int actual = 0;
        for (int i = 0; i < 100; ++i) {
            final var future = futures.get(i);

            if (i % 13 == 0) {
                assertThrows(ExecutionException.class,
                        () -> future.get());
            } else {
                actual += future.get();
            }
        }

        assertEquals(expected, actual);
    }
}
