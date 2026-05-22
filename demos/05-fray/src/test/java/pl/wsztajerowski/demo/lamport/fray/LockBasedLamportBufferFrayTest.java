package pl.wsztajerowski.demo.lamport.fray;

import pl.wsztajerowski.demo.lamport.LamportBuffer;
import pl.wsztajerowski.demo.lamport.mpmc.LockBasedLamportBuffer;

class LockBasedLamportBufferFrayTest
    extends AbstractLamportBufferFrayTest {

    @Override
    protected <T> LamportBuffer<T> createBuffer(Class<T> clazz, int capacity) {
        return LockBasedLamportBuffer.createBuffer(clazz, capacity);
    }
}
