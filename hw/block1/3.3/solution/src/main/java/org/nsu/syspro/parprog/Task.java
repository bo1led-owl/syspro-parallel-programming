package org.nsu.syspro.parprog;

import java.util.concurrent.Callable;

class Task<V> {
    private final Callable<V> callable;
    private final CondVarFuture<V> future;

    Task(Callable<V> callable) {
        this.callable = callable;
        future = new CondVarFuture<>();
    }

    /**
     * Run the task and fill its future.
     * 
     * @return `true` if the computation terminated normally or threw an
     *         exception, and `false` if the computation threw an unchecked
     *         `Throwable`.
     */
    public boolean run() {
        try {
            future.setResult(callable.call());
            return true;
        } catch (Exception e) {
            future.setError(e);
            return true;
        } catch (Throwable e) {
            future.setError(e);
            return false;
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
