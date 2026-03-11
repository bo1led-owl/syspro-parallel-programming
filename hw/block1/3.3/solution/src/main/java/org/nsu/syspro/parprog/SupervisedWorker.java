package org.nsu.syspro.parprog;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ThreadFactory;

/**
 * Worker thread with a supervisor thread monitoring it. If the worker
 * terminates, supervisor restarts it using provided job and thread factory.
 */
class SupervisedWorker {
    private final Lock lock;
    private final ThreadFactory threadFactory;
    private final Runnable job;
    private final Thread supervisor;
    private boolean started;
    private Thread worker;

    public SupervisedWorker(ThreadFactory threadFactory, Runnable job) {
        this.lock = new ReentrantLock();
        this.threadFactory = threadFactory;
        this.job = job;
        this.supervisor = createSupervisor();
        this.started = false;
        this.worker = null;
    }

    /**
     * Start the "supervisor + worker" system if it is not running already.
     */
    public void startIfNotRunning() {
        lock.lock();
        try {
            if (started) {
                return;
            }

            restartWorker();
            supervisor.start();
            started = true;
        } finally {
            lock.unlock();
        }
    }

    private Thread createSupervisor() {
        var res = threadFactory.newThread(() -> {
            for (;;) {
                Thread snapshot = null;

                lock.lock();
                try {
                    snapshot = worker;
                } finally {
                    lock.unlock();
                }

                if (snapshot != null) {
                    try {
                        snapshot.join();
                    } catch (InterruptedException e) {
                        // this thread is a supervisor, so the best it can do is to die
                        break;
                    }
                }

                restartWorker();
            }
        });
        res.setDaemon(true);
        return res;
    }

    /**
     * Restart (create + start) worker, assuming it terminated or does not exist at
     * the moment.
     */
    private void restartWorker() {
        lock.lock();

        try {
            worker = threadFactory.newThread(job);
            worker.start();
        } finally {
            lock.unlock();
        }
    }
};
