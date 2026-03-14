package org.nsu.syspro.parprog;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class ExclusiveResource {
    private final Lock lock;
    private Thread owner;

    public ExclusiveResource() {
        lock = new ReentrantLock();
        owner = null;
    }

    public void acquire() {
        lock.lock();
        try {
            if (owner != null) {
                throw new IllegalStateException(
                        "attempt to acquire a resource that is already owned by another thread");
            }

            owner = Thread.currentThread();
        } finally {
            lock.unlock();
        }
    }

    public void release() {
        lock.lock();
        try {
            if (owner != Thread.currentThread()) {
                throw new IllegalStateException("attempt to release a resource by a thread that does not own it");
            }

            owner = null;
        } finally {
            lock.unlock();
        }
    }
}
