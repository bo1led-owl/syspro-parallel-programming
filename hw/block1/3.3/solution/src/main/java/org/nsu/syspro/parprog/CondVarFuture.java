package org.nsu.syspro.parprog;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CondVarFuture<V> {
    private final Lock lock;
    private final Condition cond;

    private V result;
    private Throwable error;
    private boolean done;

    CondVarFuture() {
        this.lock = new ReentrantLock();
        cond = lock.newCondition();
        done = false;
    }

    /**
     * Put computed value into the future.
     * 
     * @throws IllegalStateException if called on a future that already has a value
     *                               or error in it.
     * @param res computed value.
     */
    public void setResult(V res) {
        lock.lock();
        try {
            if (done) {
                throw new IllegalStateException("future is already marked as done");
            }

            result = res;
            done = true;
            cond.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Put an exception that was thrown by the computation into the future.
     * 
     * @throws IllegalStateException if called on a future that already has a value
     *                               or error in it.
     * @param e the error.
     */
    public void setError(Throwable e) {
        lock.lock();
        try {
            if (done) {
                throw new IllegalStateException("future is already marked as done");
            }

            error = e;
            done = true;
            cond.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Waits if necessary for the computation to complete, and then retrieves its
     * result.
     * 
     * @return the computed result.
     * @throws ExecutionException if the computation threw an exception.
     */
    public V get() throws ExecutionException {
        lock.lock();
        try {
            while (!done) {
                try {
                    cond.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            lock.unlock();
        }

        // this is safe because after the future is marked done there are no
        // modifications of `result` or `error`

        if (error != null) {
            throw new ExecutionException(error);
        }
        return result;
    }

    /**
     * Check task completion. Completion may be due to normal termination or an
     * exception.
     * 
     * @return whether the task is complete.
     */
    public boolean isDone() {
        lock.lock();
        try {
            return done;
        } finally {
            lock.unlock();
        }
    }
}
