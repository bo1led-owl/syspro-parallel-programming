package org.nsu.syspro.parprog;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * An ExecutorService that uses a single worker thread operating off an
 * unbounded queue.
 * Note however that if this single thread terminates due to a failure during
 * execution, a new one will take its place if needed to execute subsequent
 * tasks.
 * 
 * Tasks are guaranteed to execute sequentially, and no more than one task will
 * be active at any given time.
 */
class SingleTheadExecutorService {
    public SingleTheadExecutorService(ThreadFactory f) {
        taskQueue = new LinkedBlockingQueue<>();
        threadFactory = f;
        worker = null;
    }

    /**
     * Submits a value-returning task for execution and returns a `CondVarFuture`
     * representing the pending results of the task.
     * The `CondVarFuture`s `get` method will return the task's result upon
     * successful completion.
     * 
     * @param computation computation to submit.
     * @return future to the result of the computation.
     */
    <T> CondVarFuture<T> submit(Callable<T> computation) {
        if (worker == null || !worker.isAlive()) {
            worker = threadFactory.newThread(this::workerLoop);
            worker.start();
        }

        Task<T> task = new Task<T>(computation);
        try {
            taskQueue.put(task);
        } catch (InterruptedException e) {
            return null;
        }
        return task.getFuture();
    }

    private final ThreadFactory threadFactory;
    private final LinkedBlockingQueue<Task<?>> taskQueue;
    private Thread worker;

    private class Task<V> {
        private final Callable<V> callable;
        private final CondVarFuture<V> future;

        Task(Callable<V> callable) {
            this.callable = callable;
            future = new CondVarFuture<>();
        }

        /**
         * Run the task and fill the future.
         * 
         * @return `true` if the computation terminated normally or threw an exception,
         *         `false` if it threw an unchecked `Throwable`, meaning a severe error
         *         occured.
         */
        public boolean run() {
            boolean severe = false;
            try {
                future.setResult(callable.call());
            } catch (Exception e) {
                future.setError(e);
            } catch (Throwable e) {
                future.setError(e);
                severe = true;
            }

            return !severe;
        }

        /**
         * Get the future associated with this task.
         * 
         * @return the future
         */
        public CondVarFuture<V> getFuture() {
            return future;
        }
    }

    /**
     * Internal method with the task execution loop for the worker thread to use.
     */
    private void workerLoop() {
        for (;;) {
            try {
                var task = taskQueue.take();
                boolean shouldDie = !task.run();
                if (shouldDie) {
                    return;
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
