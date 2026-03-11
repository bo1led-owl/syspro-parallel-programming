package org.nsu.syspro.parprog;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class ThreadIdTest {
    private final TestThreadFactory threadFactory = new TestThreadFactory();

    @Test
    public void idsShouldBeDifferent() throws InterruptedException {
        class IdContainer {
            public long id;

            @Override
            public boolean equals(Object that) {
                return that instanceof IdContainer && ((IdContainer) that).id == id;
            }
        }

        final IdContainer aId = new IdContainer();
        final IdContainer bId = new IdContainer();

        Thread a = threadFactory.newThread(() -> {
            aId.id = ThreadId.get();
        });
        Thread b = threadFactory.newThread(() -> {
            bId.id = ThreadId.get();
        });

        a.start();
        b.start();
        a.join();
        b.join();

        assertNotEquals(aId, bId);
    }
}
