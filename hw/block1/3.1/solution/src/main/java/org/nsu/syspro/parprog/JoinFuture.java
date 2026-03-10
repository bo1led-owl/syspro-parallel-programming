package org.nsu.syspro.parprog;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

public class JoinFuture<V> {
    private final Thread worker;
    private V result = null;
    private Exception thrownException = null;

    private JoinFuture(ThreadFactory workerFactory, Callable<V> task) {
        this.worker = workerFactory.newThread(() -> {
            try {
                result = task.call();
            } catch (Exception e) {
                thrownException = e;
            }
        });
    }

    public static <V> JoinFuture<V> createAndStart(ThreadFactory threadFactory, Callable<V> task) {
        var res = new JoinFuture<V>(threadFactory, task);
        res.worker.start();
        return res;
    }

    /**
     * Waits if necessary for the computation to complete, and then retrieves its
     * result.
     * 
     * @return the computed result.
     * @throws ExecutionException if the computation threw an exception.
     */
    public V get() throws ExecutionException {
        // falls through after `join` if not interrupted, retried otherwise
        do {
            try {
                worker.join();
            } catch (InterruptedException e) {
                continue; // continue waiting
            }
        } while (false);

        if (thrownException != null) {
            throw new ExecutionException(thrownException);
        }
        return result;
    }

    /**
     * Check whether the computation is complete. Completion may be reached due to
     * normal termination or
     * an exception - in all of these cases, this method will return true.
     * 
     * @returns `true` if this task completed.
     */
    public boolean isDone() {
        return !worker.isAlive();
    }
}
