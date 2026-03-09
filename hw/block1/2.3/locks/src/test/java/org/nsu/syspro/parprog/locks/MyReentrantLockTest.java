package org.nsu.syspro.parprog.locks;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.concurrent.ThreadFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class MyReentrantLockTest {
    private class SimpleThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r);
        }
    }

    private final NonReentrantLockFactory lockFactory = new SimpleNonReentrantLockFactory();
    private final ThreadFactory threadFactory = new SimpleThreadFactory();

    @Test
    public void deepLocking() throws InterruptedException {
        MyReentrantLock lock = new MyReentrantLock(lockFactory);

        final int LOCKS = 1000;

        Runnable lockLockUnlockUnlock = () -> {
            for (int i = 0; i < LOCKS; ++i) {
                lock.lock();
            }
            for (int i = 0; i < LOCKS; ++i) {
                lock.unlock();
            }
        };

        Thread t = threadFactory.newThread(lockLockUnlockUnlock);

        // two threads to check that after a series of `unlock`s lock ownership can
        // really be transferred
        t.start();
        lockLockUnlockUnlock.run();
        t.join();
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 4, 10 })
    public void contention(int contenderCount) throws InterruptedException {
        class Spoon {
            private Thread owner;

            public synchronized void acquire() {
                if (owner != null) {
                    throw new IllegalStateException("mutual exclusion is violated");
                }

                owner = Thread.currentThread();
            }

            public synchronized void release() {
                owner = null;
            }
        }

        MyReentrantLock lock = new MyReentrantLock(lockFactory);

        Spoon spoon = new Spoon();

        final Runnable eat = () -> {
            lock.lock();
            try {
                spoon.acquire();
                Thread.sleep(500);
                spoon.release();
            } catch (InterruptedException e) {
                throw new RuntimeException("test thread got interrupted " + e);
            } finally {
                lock.unlock();
            }
        };

        var contenders = new ArrayList<Thread>();

        for (int i = 0; i < contenderCount; ++i) {
            contenders.add(threadFactory.newThread(eat));
        }

        for (var c : contenders) {
            c.start();
        }

        for (var c : contenders) {
            c.join();
        }
    }

    @Test
    public void unlockByNonOwner() throws InterruptedException {
        MyReentrantLock lock = new MyReentrantLock(lockFactory);

        lock.lock();

        Thread t = threadFactory.newThread(() -> {
            assertThrows(IllegalMonitorStateException.class, () -> lock.unlock());
        });

        t.start();
        t.join();
    }
}
