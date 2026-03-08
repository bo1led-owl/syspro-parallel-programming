package org.nsu.syspro.parprog;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;

class ThreadPerTaskExecutorService {
    private final ThreadFactory factory;

    public ThreadPerTaskExecutorService(ThreadFactory f) {
        factory = f;
    }

    /**
     * Submits a value-returning task for execution.
     * 
     * @param task the computation to submit.
     * @return future to check computation status and wait for completion.
     */
    <T> JoinFuture<T> submit(Callable<T> task) {
        return new JoinFuture<T>(factory, task);
    }
}
