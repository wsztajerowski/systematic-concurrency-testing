package pl.wsztajerowski.demo.lamport.jcstress;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;
import pl.wsztajerowski.demo.lamport.LamportBuffer;
import pl.wsztajerowski.demo.lamport.singlethread.NonVolatileLamportBuffer;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE_INTERESTING;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

/**
 * Detects visibility bugs caused by non-volatile fields in SingleThreadLamportBuffer.
 *
 * The @Arbiter runs after both actors complete (with a full memory barrier),
 * so it always sees the producer's write.  If the consumer's poll() also
 * consumed the value, the arbiter finds nothing — and vice-versa.
 *
 * Expected correct outcomes (for a thread-safe buffer):
 *   (42, -1) — consumer got the value, arbiter finds nothing
 *   (-1, 42) — consumer missed the value, arbiter picks it up
 *
 * Interesting / forbidden outcomes that reveal non-volatile bugs:
 *   (42, 42)  — consumer read value but did NOT clear it (stale readPosition
 *               not flushed → other  re-reads the same slot)
 *   (-1, -1)  — value lost entirely (arbiter can't find it either)
 */
@JCStressTest
@Outcome(id = "42, -1", expect = ACCEPTABLE,
        desc = "Consumer polled the value; arbiter finds buffer empty.")
@Outcome(id = "-1, 42", expect = ACCEPTABLE,
        desc = "Consumer missed the value; arbiter picks it up.")
@Outcome(id = "42, 42", expect = ACCEPTABLE_INTERESTING,
        desc = "Duplicate read! Consumer got the value but non-volatile readPosition " +
               "update was lost — arbiter re-reads the same slot.")
@Outcome(id = "-1, -1", expect = FORBIDDEN,
        desc = "Value lost — both consumer and arbiter missed it.")
@Outcome(id = ".*", expect = FORBIDDEN,
        desc = "Unexpected state — data race or logic error.")
@State
public class SingleThreadLamportBufferOfferPollStress {

    private final LamportBuffer<Integer> buffer = prefilledBuffer();

    private static LamportBuffer<Integer> prefilledBuffer() {
        LamportBuffer<Integer> buffer = NonVolatileLamportBuffer.createBuffer(Integer.class, 2);

        return buffer;
    }

    @Actor
    public void producer() {
        buffer.offer(42);
    }

    @Actor
    public void consumer(II_Result r) {
        r.r1 = buffer.poll().orElse(-1);
    }

    @Actor
    public void arbiter(II_Result r) {
        r.r2 = buffer.poll().orElse(-1);
    }
}

