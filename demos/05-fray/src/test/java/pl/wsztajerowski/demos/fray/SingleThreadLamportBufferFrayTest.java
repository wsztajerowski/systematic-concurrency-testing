package pl.wsztajerowski.demos.fray;

import pl.wsztajerowski.demos.lamport.LamportBuffer;
import pl.wsztajerowski.demos.lamportsinglethread.SingleThreadLamportBuffer;

class SingleThreadLamportBufferFrayTest extends AbstractLamportBufferFrayTest {

    @Override
    protected <T> LamportBuffer<T> createBuffer(Class<T> clazz, int capacity) {
        return SingleThreadLamportBuffer.createBuffer(clazz, capacity);
    }
}

