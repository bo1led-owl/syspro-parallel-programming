package org.nsu.syspro.parprog.locks;

import java.util.concurrent.locks.ReentrantLock;

class SimpleNonReentrantLock implements NonReentrantLock {
    private final ReentrantLock internalLock;

    SimpleNonReentrantLock() {
        internalLock = new ReentrantLock();
    }

    @Override
    public void lock() {
        if (internalLock.isHeldByCurrentThread()) {
            throw new IllegalMonitorStateException("non-reentrant lock is acquired more than once");
        }

        internalLock.lock();
    }

    @Override
    public void unlock() {
        internalLock.unlock();
    }
}