package org.nsu.syspro.parprog;

class ThreadId {
    private static long nextId = 0;
    private static final Object guard = new Object();

    private static final ThreadLocal<Long> threadId = new ThreadLocal<Long>() {
        @Override
        protected Long initialValue() {
            synchronized (guard) {
                return nextId++;
            }
        }
    };

    public static long get() {
        return threadId.get();
    }
}
