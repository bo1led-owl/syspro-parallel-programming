package org.nsu.syspro.parprog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ThreadPerTaskExecutionServiceTest {
    class TestThreadFactory implements ThreadFactory {
        private long threadsCreated = 0;

        @Override
        public Thread newThread(Runnable r) {
            threadsCreated++;
            return new Thread(r);
        }

        public long threadsCreated() {
            return threadsCreated;
        }
    }

    private final TestThreadFactory threadFactory = new TestThreadFactory();

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

    @ParameterizedTest
    @ValueSource(ints = { 1, 10, 100, 1000 })
    public void countThreads(int tasksToStart) {
        final Callable<Integer> task = () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException("test thread got interrupted: " + e);
            }

            return 42;
        };

        var service = newService();

        long threadsBefore = threadFactory.threadsCreated();

        for (int i = 0; i < tasksToStart; ++i) {
            service.submit(task);
        }

        long threadsAfter = threadFactory.threadsCreated();

        assertEquals(tasksToStart, threadsAfter - threadsBefore);
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 5, 10, 25, 50, 100 })
    public void recursiveSubmission(final int depth) throws ExecutionException {
        final var service = newService();

        class Task implements Callable<Integer> {
            final int depth;

            public Task(int depth) {
                this.depth = depth;
            }

            @Override
            public Integer call() throws ExecutionException {
                int res = depth;
                if (depth > 0) {
                    res += service.submit(new Task(depth - 1)).get();
                }

                return res;
            }
        }

        int expected = (1 + depth) * depth / 2; // arithmetic progression

        var future = service.submit(new Task(depth));
        assertEquals(expected, future.get());
    }
}
