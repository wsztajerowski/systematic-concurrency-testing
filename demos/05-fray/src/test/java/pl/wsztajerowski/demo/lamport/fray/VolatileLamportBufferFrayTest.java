package pl.wsztajerowski.demo.lamport.fray;

import org.junit.jupiter.api.Disabled;
import pl.wsztajerowski.demo.lamport.LamportBuffer;
import pl.wsztajerowski.demo.lamport.spsc.VolatileLamportBuffer;

@Disabled
class VolatileLamportBufferFrayTest
    extends AbstractSPSCLamportBufferFrayTest {

    protected <T> LamportBuffer<T> createBuffer(Class<T> clazz, int capacity) {
        return VolatileLamportBuffer.createBuffer(clazz, capacity);
    }
}
