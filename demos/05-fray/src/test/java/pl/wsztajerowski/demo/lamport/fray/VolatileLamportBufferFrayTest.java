package pl.wsztajerowski.demo.lamport.fray;

import org.junit.jupiter.api.Disabled;
import pl.wsztajerowski.demo.lamport.LamportBuffer;
import pl.wsztajerowski.demo.lamport.spsc.VolatileLamportBuffer;

//@Disabled("VolatileLamportBuffer is SPSC-only; the MPMC tests inherited from " +
//        "AbstractMPMCLamportBufferFrayTest (two producers / two consumers) are not applicable " +
//        "and are expected to fail by design.")
class VolatileLamportBufferFrayTest
    extends AbstractSPSCLamportBufferFrayTest {

    @Override
    protected <T> LamportBuffer<T> createBuffer(Class<T> clazz, int capacity) {
        return VolatileLamportBuffer.createBuffer(clazz, capacity);
    }
}
