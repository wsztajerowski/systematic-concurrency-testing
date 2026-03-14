package pl.wsztajerowski.demo.lamport.fray;

import pl.wsztajerowski.demo.lamport.LamportBuffer;
import pl.wsztajerowski.demo.lamport.singlethread.SingleThreadLamportBuffer;

class SingleThreadLamportBufferFrayTest
    extends AbstractLamportBufferFrayTest
{

    @Override
    protected <T> LamportBuffer<T> createBuffer(Class<T> clazz, int capacity) {
        return SingleThreadLamportBuffer.createBuffer(clazz, capacity);
    }
}
