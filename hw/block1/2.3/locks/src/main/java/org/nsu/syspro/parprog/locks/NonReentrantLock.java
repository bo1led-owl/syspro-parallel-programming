package org.nsu.syspro.parprog.locks;

interface NonReentrantLock {
    void lock();

    void unlock();
}