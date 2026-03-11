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
    private final ThreadFactory threadFactory;
    private final LinkedBlockingQueue<Task<?>> taskQueue;
    private final SupervisedWorker supervisedWorker;

    public SingleTheadExecutorService(ThreadFactory f) {
        taskQueue = new LinkedBlockingQueue<>();
        threadFactory = f;
        Runnable workerJob = () -> {
            for (;;) {
                var task = takeTask();
                if (task == null) {
                    // `null` marks the end of execution
                    break;
                }

                boolean shouldDie = !task.run();
                if (shouldDie) {
                    break;
                }
            }
        };
        supervisedWorker = new SupervisedWorker(threadFactory, workerJob);
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
        supervisedWorker.startIfNotRunning();

        Task<T> task = new Task<T>(computation);
        try {
            taskQueue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        return task.getFuture();
    }

    private Task<?> takeTask() {
        try {
            return taskQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
