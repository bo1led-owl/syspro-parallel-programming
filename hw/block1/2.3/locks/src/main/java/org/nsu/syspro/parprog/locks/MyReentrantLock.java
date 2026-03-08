package org.nsu.syspro.parprog.locks;

class MyReentrantLock {
    private static final long INITIAL_DELAY = 5;
    private static final long MAX_DELAY = 250;

    private final NonReentrantLock ownershipLock;
    private volatile long ownerId;
    private volatile int acquisitions;

    private static long curThreadId() {
        return Thread.currentThread().getId();
    }

    public MyReentrantLock(NonReentrantLockFactory factory) {
        this.ownerId = 0;
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
        long curId = curThreadId();

        boolean acquired = false;
        ownershipLock.lock();
        try {
            if (ownerId == 0) {
                ownerId = curId;
                assert acquisitions == 0;
            }

            if (ownerId == curId) {
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
    public void lock() throws InterruptedException {
        long curDelay = INITIAL_DELAY;

        while (true) {
            if (tryLock()) {
                break;
            }

            Thread.sleep(curDelay);
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
        long curId = curThreadId();

        ownershipLock.lock();
        try {
            if (ownerId != curId) {
                throw new IllegalMonitorStateException("`unlock()` called not by the thread not owning the lock");
            }

            acquisitions -= 1;
            if (acquisitions == 0) {
                ownerId = 0;
            }
        } finally {
            ownershipLock.unlock();
        }
    }
}
