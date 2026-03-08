package org.nsu.syspro.parprog.locks;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class SimpleNonReentrantLockTest {
    private final NonReentrantLockFactory factory = new SimpleNonReentrantLockFactory();

    @Test
    public void lockUnlockLockUnlock() {
        NonReentrantLock lock1 = factory.create();
        NonReentrantLock lock2 = factory.create();

        final int FLIPS = 1000;

        for (int i = 0; i < FLIPS; ++i) {
            lock1.lock();
            lock2.lock();

            if (i % 2 == 0) {
                lock1.unlock();
                lock2.unlock();
            } else {
                lock2.unlock();
                lock1.unlock();
            }
        }
    }

    @Test
    public void throwables() {
        NonReentrantLock lock = factory.create();

        lock.lock();
        assertThrows(IllegalMonitorStateException.class, () -> lock.lock());

        lock.unlock();
        assertThrows(IllegalMonitorStateException.class, () -> lock.unlock());
    }
}
