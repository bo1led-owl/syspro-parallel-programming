package org.nsu.syspro.parprog;

import java.util.concurrent.Callable;

class Task<V> {
    public enum Result {
        Ok, Fatal;

        public boolean isOk() {
            return this == Ok;
        }

        public boolean isFatal() {
            return this == Fatal;
        }
    }

    private final Callable<V> callable;
    private final CondVarFuture<V> future;

    Task(Callable<V> callable) {
        this.callable = callable;
        future = new CondVarFuture<>();
    }

    /**
     * Run the task and fill its future.
     * 
     * @return `Result` with `Ok` if the computation terminated normally or threw an
     *         exception, and `Fatal` if the computation threw an unchecked
     *         `Throwable`.
     */
    public Result run() {
        try {
            future.setResult(callable.call());
            return Result.Ok;
        } catch (Exception e) {
            future.setError(e);
            return Result.Ok;
        } catch (Throwable e) {
            future.setError(e);
            return Result.Fatal;
        }
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
