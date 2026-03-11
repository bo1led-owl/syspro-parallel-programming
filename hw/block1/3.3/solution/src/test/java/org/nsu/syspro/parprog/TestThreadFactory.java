package org.nsu.syspro.parprog;

import java.util.concurrent.ThreadFactory;

class TestThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r);
    }
}
