package pl.wsztajerowski.demos.fray;

import pl.wsztajerowski.demos.lamport.LamportBuffer;
import pl.wsztajerowski.demos.lamportvolatile.VolatileLamportBuffer;

class VolatileLamportBufferFrayTest extends AbstractLamportBufferFrayTest {

    @Override
    protected <T> LamportBuffer<T> createBuffer(Class<T> clazz, int capacity) {
        return VolatileLamportBuffer.createBuffer(clazz, capacity);
    }
}

