package org.nsu.syspro.parprog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.concurrent.ThreadFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class MyMonitorTest {
    private static class TestThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r);
        }
    }

    private final TestThreadFactory threadFactory = new TestThreadFactory();

    @Test
    @Timeout(5)
    public void spuriousWakeup() throws InterruptedException {
        final var monitor1 = new MyMonitorOld();
        final var monitor2 = new MyMonitorOld();

        // final var monitor1 = new MyMonitor();
        // final var monitor2 = new MyMonitor();

        var t1 = threadFactory.newThread(() -> {
            monitor1.monitorEnter();
            {
                try {
                    monitor1.monitorWait();
                } catch (RuntimeException e) {
                    monitor1.monitorEnter();
                }
            }
            monitor1.monitorExit();

            monitor2.monitorEnter();
            monitor2.monitorWait();
            monitor2.monitorExit();
        });

        t1.start();

        Thread.sleep(1000);
        t1.interrupt();
        Thread.sleep(1000);

        monitor1.monitorEnter();
        monitor1.monitorNotify();
        monitor1.monitorExit();

        t1.join();

        // this sequence endes only if a spurious wakeup occured
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 4 })
    public void mutualExclusion(int threadCount) throws InterruptedException {
        final ExclusiveResource resource = new ExclusiveResource();

        final MyMonitor monitor = new MyMonitor();

        final ArrayList<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; ++i) {
            var thread = threadFactory.newThread(() -> {
                try {
                    monitor.monitorEnter();
                    resource.acquire();
                    Thread.sleep(500);
                    resource.release();
                    monitor.monitorExit();

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            threads.add(thread);
        }

        for (var t : threads) {
            t.start();
        }
        for (var t : threads) {
            t.join();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 4, 16, 40 })
    @Timeout(5)
    public void deadlockFreedom(int threadCount) throws InterruptedException {
        final MyMonitor monitor = new MyMonitor();

        final ArrayList<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; ++i) {
            var thread = threadFactory.newThread(() -> {
                try {
                    monitor.monitorEnter();
                    Thread.sleep(100);
                    monitor.monitorExit();

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            threads.add(thread);
        }

        for (var t : threads) {
            t.start();
        }
        for (var t : threads) {
            t.join();
        }
    }

    @Test
    @Timeout(2)
    public void simpleWaiting() throws InterruptedException {
        class Flag {
            private int value = -1;

            public void set(int v) {
                value = v;
            }

            public int get() {
                return value;
            }
        }

        final MyMonitor monitor = new MyMonitor();

        // no flag lock needed because monitor gives us mutual exclusion
        final Flag flag = new Flag();

        var waitingThread = threadFactory.newThread(() -> {
            monitor.monitorEnter();
            monitor.monitorWait();
            flag.set(1);
            monitor.monitorExit();
        });

        var notifyingThread = threadFactory.newThread(() -> {
            try {
                Thread.sleep(500); // to ensure that the waiting thread got to `wait`
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            monitor.monitorEnter();
            flag.set(0);
            monitor.monitorNotify();
            monitor.monitorExit();
        });

        waitingThread.start();
        notifyingThread.start();

        waitingThread.join();
        notifyingThread.join();

        assertEquals(1, flag.get());
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 4, 16, 40 })
    @Timeout(2)
    public void broadcast(int waitersCount) throws InterruptedException {
        final MyMonitor monitor = new MyMonitor();

        final ArrayList<Thread> waiters = new ArrayList<>();

        for (int i = 0; i < waitersCount; ++i) {
            var t = threadFactory.newThread(() -> {
                monitor.monitorEnter();
                monitor.monitorWait();
                monitor.monitorExit();
            });
            waiters.add(t);
        }

        for (var w : waiters) {
            w.start();
        }

        Thread.sleep(500); // ensure all threads got to `wait`
        monitor.monitorEnter();
        monitor.monitorNotifyAll();
        monitor.monitorExit();

        for (var w : waiters) {
            w.join();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 4, 16, 40 })
    public void noSpuriousWakeups(int waitersCount) throws InterruptedException {
        class Counter {
            private int cnt = 0;

            public void increment() {
                cnt++;
            }

            public int get() {
                return cnt;
            }

            public void reset() {
                cnt = 0;
            }
        }

        final MyMonitor monitor = new MyMonitor();

        final Counter counter = new Counter();
        final ArrayList<Thread> waiters = new ArrayList<>();

        for (int i = 0; i < waitersCount; ++i) {
            var t = threadFactory.newThread(() -> {
                monitor.monitorEnter();
                monitor.monitorWait();
                counter.increment();
                monitor.monitorExit();
            });
            waiters.add(t);
        }

        for (var w : waiters) {
            w.start();
        }

        Thread.sleep(500); // ensure all threads got to `wait`
        for (int i = 0; i < waitersCount; ++i) {
            monitor.monitorEnter();
            monitor.monitorNotify();
            monitor.monitorExit();

            Thread.sleep(500); // wait for all threads to wake up and increment the counter

            monitor.monitorEnter();
            assertEquals(1, counter.get());
            counter.reset();
            monitor.monitorExit();
        }

        for (var w : waiters) {
            w.join();
        }
    }
}
