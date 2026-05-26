package pl.wsztajerowski.demo.lamport.fray;

import pl.wsztajerowski.demo.lamport.LamportBuffer;
import pl.wsztajerowski.demo.lamport.singlethread.NonVolatileLamportBuffer;

class NonVolatileLamportBufferFrayTest
    extends AbstractMPMCLamportBufferFrayTest
{

    @Override
    protected <T> LamportBuffer<T> createBuffer(Class<T> clazz, int capacity) {
        return NonVolatileLamportBuffer.createBuffer(clazz, capacity);
    }
}
