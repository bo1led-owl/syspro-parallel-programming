package org.nsu.syspro.parprog.counters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.ThreadFactory;

import org.nsu.syspro.parprog.counters.impls.SplitCounter;

public class SplitCounterTest {
    class SimpleThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            return new Thread(r);
        }
    }

    private ThreadFactory factory = new SimpleThreadFactory();

    @ParameterizedTest
    @CsvSource({
            "1, 2", "1, 4", "1, 10",
            "2, 2", "2, 4", "2, 10",
            "4, 4", "4, 10", "4, 16",
            "10, 10", "10, 14", "20, 10",
            "20, 14", "20, 6",
    })
    public void noIncrementsAreLost(int granularity, int incrementersCount) throws InterruptedException {
        var cnt = new SplitCounter(granularity);

        var incrementers = new Thread[incrementersCount];

        final int INCS = 10000;
        for (int i = 0; i < incrementersCount; ++i) {
            incrementers[i] = factory.newThread(() -> {
                for (int j = 0; j < INCS; ++j) {
                    cnt.increment();
                }
            });
        }

        for (var t : incrementers) {
            t.start();
        }
        for (var t : incrementers) {
            t.join();
        }

        assertEquals(cnt.get(), incrementersCount * INCS);
    }
}
