package org.nsu.syspro.parprog.locks;

class SimpleNonReentrantLockFactory implements NonReentrantLockFactory {
    @Override
    public NonReentrantLock create() {
        return new SimpleNonReentrantLock();
    }
}