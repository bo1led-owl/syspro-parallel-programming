package org.nsu.syspro.parprog.counters.impls;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SplitCounter implements Counter {
    private Lock[] locks;
    private long[] value;

    private int granularity() {
        return locks.length;
    }

    private int myPortion() {
        return (int) (Thread.currentThread().getId() % granularity());
    }

    /**
     * Create new `SplitCounter`.
     *
     * @param GRANULARITY the amount of `parts` to split the counter into. Must be
     *                    positive.
     * @throws IllegalArgumentException if granularity is less than one.
     */
    public SplitCounter(int GRANULARITY) {
        if (GRANULARITY <= 0) {
            throw new IllegalArgumentException("granularity must be positive");
        }

        value = new long[GRANULARITY];
        locks = new Lock[GRANULARITY];
        for (int i = 0; i < GRANULARITY; ++i) {
            locks[i] = new ReentrantLock();
        }
    }

    @Override
    public void increment() {
        int i = myPortion();
        Lock l = locks[i];
        l.lock();
        try {
            ++value[i];
        } finally {
            l.unlock();
        }
    }

    @Override
    public long get() {
        long res = 0;

        int offset = myPortion();
        for (int i = 0; i < granularity(); ++i) {
            int idx = (offset + i) % granularity();

            locks[idx].lock();
            try {
                res += value[idx];
            } finally {
                locks[idx].unlock();
            }
        }
        return res;
    }
}
