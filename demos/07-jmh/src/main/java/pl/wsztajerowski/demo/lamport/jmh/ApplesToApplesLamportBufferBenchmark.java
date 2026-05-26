package pl.wsztajerowski.demo.lamport.jmh;

import java.util.Optional;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.infra.Control;

import pl.wsztajerowski.demo.lamport.LamportBuffer;
import pl.wsztajerowski.demo.lamport.mpmc.LockBasedLamportBuffer;
import pl.wsztajerowski.demo.lamport.spsc.VolatileLamportBuffer;

@BenchmarkMode(Mode.Throughput)
@Fork(1)
@State(Scope.Group)
public class ApplesToApplesLamportBufferBenchmark {

    @Param({"VOLATILE", "LOCK"})
    public String implementation;

    @Param({"64", "1024"})
    public int capacity;

    private LamportBuffer<Long> buffer;
    private long sequence;

    @Setup
    public void setup() {
        this.buffer = createBuffer(implementation, capacity);
        this.sequence = 0L;
    }

    @Benchmark
    @Group("applesToApples")
    @GroupThreads(1)
    public void producer(Control control) {
        long next = ++sequence;
        while (!buffer.offer(next)) {
            if (control.stopMeasurement) return;
            Thread.onSpinWait();
        }
    }

    @Benchmark
    @Group("applesToApples")
    @GroupThreads(1)
    public void consumer(Blackhole blackhole, Control control) {
        Optional<Long> value;
        while ((value = buffer.poll()).isEmpty()) {
            if (control.stopMeasurement) return;
            Thread.onSpinWait();
        }
        blackhole.consume(value.orElseThrow());
    }

    private LamportBuffer<Long> createBuffer(String name, int bufferCapacity) {
        return switch (name) {
            case "VOLATILE" -> VolatileLamportBuffer.createBuffer(Long.class, bufferCapacity);
            case "LOCK" -> LockBasedLamportBuffer.createBuffer(Long.class, bufferCapacity);
            default -> throw new IllegalArgumentException("Unsupported implementation: " + name);
        };
    }
}

