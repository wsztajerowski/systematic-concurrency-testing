package pl.wsztajerowski.demo.lamport.jmh;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.infra.Control;

import pl.wsztajerowski.demo.lamport.LamportBuffer;
import pl.wsztajerowski.demo.lamport.spsc.VolatileLamportBuffer;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Group)
public class VolatileLamportBufferBenchmark {

    @Param({"64", "1024"})
    public int capacity;

    private LamportBuffer<Long> buffer;

    @Setup
    public void setup() {
        this.buffer = VolatileLamportBuffer.createBuffer(Long.class, capacity);
    }

    @State(Scope.Thread)
    public static class ProducerState {
        private long sequence;

        @Setup
        public void setup() {
            this.sequence = 0L;
        }
    }

    @Benchmark
    @Group("spsc")
    @GroupThreads(1)
    public void producer(ProducerState producerState, Control control) {
        long next = ++producerState.sequence;
        while (!buffer.offer(next)) {
            if (control.stopMeasurement) return;
            Thread.onSpinWait();
        }
    }

    @Benchmark
    @Group("spsc")
    @GroupThreads(1)
    public void consumer(Blackhole blackhole, Control control) {
        Optional<Long> value;
        while ((value = buffer.poll()).isEmpty()) {
            if (control.stopMeasurement) return;
            Thread.onSpinWait();
        }
        blackhole.consume(value.orElseThrow());
    }
}

