package pl.wsztajerowski.demo.lamport.fray;

import org.junit.jupiter.api.Disabled;
import pl.wsztajerowski.demo.lamport.LamportBuffer;
import pl.wsztajerowski.demo.lamport.spsc.VolatileLamportBuffer;

class VolatileLamportBufferFrayTest
    extends AbstractLamportBufferFrayTest
{

    protected <T> LamportBuffer<T> createBuffer(Class<T> clazz, int capacity) {
        return VolatileLamportBuffer.createBuffer(clazz, capacity);
    }

    @Disabled
    @Override
    void twoConcurrentProducersMustNotLoseElements() throws InterruptedException {
    }

    @Disabled
    @Override
    void twoConsumersMustNotReadSameElement() throws InterruptedException {
    }
}
