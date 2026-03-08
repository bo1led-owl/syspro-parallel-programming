package org.nsu.syspro.parprog.locks;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.ThreadFactory;

import org.junit.jupiter.api.Test;

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
                try {
                    lock.lock();
                } catch (InterruptedException e) {
                }
            }
            for (int i = 0; i < LOCKS; ++i) {
                lock.unlock();
            }
        };

        Thread t = threadFactory.newThread(lockLockUnlockUnlock);

        t.start();
        lockLockUnlockUnlock.run();
        t.join();
    }

    @Test
    public void exception() throws InterruptedException {
        MyReentrantLock lock = new MyReentrantLock(lockFactory);

        lock.lock();

        Thread t = threadFactory.newThread(() -> {
            assertThrows(IllegalMonitorStateException.class, () -> lock.unlock());
        });

        t.join();
    }
}
