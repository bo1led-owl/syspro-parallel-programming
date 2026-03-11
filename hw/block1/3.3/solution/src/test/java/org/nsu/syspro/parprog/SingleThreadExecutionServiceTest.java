package org.nsu.syspro.parprog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

public class SingleThreadExecutionServiceTest {
    private final TestThreadFactory threadFactory = new TestThreadFactory();

    private SingleTheadExecutorService newService() {
        return new SingleTheadExecutorService(threadFactory);
    }

    @Test
    public void simpleChainedTasks() throws ExecutionException {
        var service = newService();

        int oneBeforeLast = 0;
        int last = 1;

        var oneBeforeLastFuture = service.submit(() -> 0);
        var lastFuture = service.submit(() -> 1);
        for (int i = 0; i <= 42; ++i) {
            int newNumber = last + oneBeforeLast;
            oneBeforeLast = last;
            last = newNumber;

            final var firstFuture = oneBeforeLastFuture;
            final var secondFuture = lastFuture;

            oneBeforeLastFuture = lastFuture;
            lastFuture = service.submit(() -> firstFuture.get() + secondFuture.get());
        }

        assertEquals(last, lastFuture.get());
    }

    @Test
    public void longTaskGetsAwaited() throws ExecutionException {
        var service = newService();

        final var longTask = service.submit(() -> {
            Thread.sleep(1000);
            return 42;
        });

        var futures = new ArrayList<CondVarFuture<Integer>>();
        for (int i = 0; i < 50; ++i) {
            futures.add(service.submit(() -> longTask.get() + 1));
        }

        int result = 0;
        for (var f : futures) {
            result += f.get();
        }

        assertEquals(43 * 50, result);
    }

    @Test
    public void sequentialityOfExecution() throws ExecutionException {
        class Counter {
            private int value;

            public synchronized int tick() {
                return value++;
            }
        }

        final int TICKS = 1000;

        var service = newService();

        final var counter = new Counter();

        var futures = new ArrayList<CondVarFuture<Integer>>();
        for (int i = 0; i < TICKS; ++i) {
            futures.add(service.submit(() -> counter.tick()));
        }

        for (int i = 0; i < futures.size(); ++i) {
            assertEquals(i, futures.get(i).get());
        }
    }

    @Test
    public void singleThreadness() throws ExecutionException {
        var service = newService();

        long lastId = 0;
        for (int i = 0; i < 1000; ++i) {
            long curId = service.submit(() -> ThreadId.get()).get();
            if (lastId == 0) {
                lastId = curId;
            } else {
                assertEquals(lastId, curId);
            }
        }
    }

    @Test
    public void checkedExceptionThrown() throws ExecutionException {
        var service = newService();

        var future = service.submit(() -> {
            throw new IllegalStateException("I just like to explode");
        });

        assertThrows(ExecutionException.class, () -> future.get());
        assertThrows(ExecutionException.class, () -> future.get()); // check that exception is rethrown
    }

    @Test
    public void threadRestartsOnFatalErrors() throws ExecutionException {
        var service = newService();

        var firstId = service.submit(ThreadId::get).get();

        var future = service.submit(() -> {
            assert false; // throws `AssertionError`
            return 42;
        });
        assertThrows(ExecutionException.class, () -> future.get()); // ensure that the task was run

        var newId = service.submit(ThreadId::get).get();

        assertNotEquals(firstId, newId);
    }
}
