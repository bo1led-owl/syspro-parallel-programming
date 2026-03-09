package org.nsu.syspro.parprog.locks;

class MyReentrantLock {
    // around the size of a scheduling quantum
    private static final long INITIAL_DELAY = 10;

    // waiting on a lock for multiple seconds is too much, usually tasks
    // that share a lock are not minutes or hours long
    private static final long MAX_DELAY = 1000;

    private final NonReentrantLock ownershipLock;
    private Thread owner;
    private int acquisitions;

    public MyReentrantLock(NonReentrantLockFactory factory) {
        this.owner = null;
        this.acquisitions = 0;

        this.ownershipLock = factory.create();
    }

    /**
     * Try to acquire the lock. Acquisition happens only if the lock is currently
     * free.
     * 
     * @return whether the lock was acquired or not.
     */
    public boolean tryLock() {
        Thread curThread = Thread.currentThread();

        boolean acquired = false;
        ownershipLock.lock();
        try {
            if (owner == null) {
                owner = curThread;
                assert acquisitions == 0;
            }

            if (owner == curThread) {
                acquisitions += 1;
                acquired = true;
            }
        } finally {
            ownershipLock.unlock();
        }

        return acquired;
    }

    /**
     * Acquire the lock.
     * 
     * Blocks if the lock is already acquired by a different thread. If the lock is
     * already acquired by the calling thread, nothing happens.
     */
    public void lock() {
        long curDelay = INITIAL_DELAY;

        while (true) {
            if (tryLock()) {
                break;
            }

            try {
                Thread.sleep(curDelay);
            } catch (InterruptedException e) {
                // continue trying to acquire the lock
            }

            curDelay = Math.min(curDelay * 2, MAX_DELAY);
        }
    }

    /**
     * Releases the lock.
     * 
     * @throws IllegalMonitorStateException if the method is called by a thread that
     *                                      did not acquire the lock.
     */
    public void unlock() {
        Thread curThread = Thread.currentThread();

        ownershipLock.lock();
        try {
            if (owner != curThread) {
                throw new IllegalMonitorStateException("`unlock()` called not by the thread not owning the lock");
            }

            acquisitions -= 1;
            if (acquisitions == 0) {
                owner = null;
            }
        } finally {
            ownershipLock.unlock();
        }
    }
}
