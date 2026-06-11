package pl.wsztajerowski.demo.lamport.jcstress;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.III_Result;
import pl.wsztajerowski.demo.lamport.LamportBuffer;
import pl.wsztajerowski.demo.lamport.spsc.VolatileLamportBuffer;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE_INTERESTING;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

// This test will
@JCStressTest
@Outcome(id = {"1, 0, 1", "1, 0, -1"}, expect = ACCEPTABLE,
        desc = "P1 offered, P2 backed off; consumer may or may not observe the value.")
@Outcome(id = {"0, 1, 2", "0, 1, -1"}, expect = ACCEPTABLE,
        desc = "P2 offered, P1 backed off; consumer may or may not observe the value.")
@Outcome(id = {"1, 1, 1", "1, 1, 2", "1, 1, -1"}, expect = ACCEPTABLE_INTERESTING,
        desc = "TOCTOU race: both offers returned true — lost update due to missing synchronisation in SPSC-only buffer.")
@Outcome(id = ".*", expect = FORBIDDEN,
        desc = "Unexpected state — possible corruption from concurrent use of a single-producer buffer.")
@State
public class TwoProducersVolatileBufferStress {

    private final LamportBuffer<Integer> buffer = VolatileLamportBuffer.createBuffer(Integer.class, 2);

    @Actor
    public void producer1(III_Result r) {
        r.r1 = buffer.offer(1) ? 1 : 0;
    }

    @Actor
    public void producer2(III_Result r) {
        r.r2 = buffer.offer(2) ? 1 : 0;
    }

    @Actor
    public void consumer(III_Result r) {
        r.r3 = buffer.poll().orElse(-1);
    }
}
