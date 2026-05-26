# 07-jmh

JMH benchmarks for two Lamport buffer implementations:

- `VolatileLamportBuffer` from `01-lamport-volatile` (SPSC scenario)
- `LockBasedLamportBuffer` from `03-lamport-lock` (MPMC scenario)

It also includes an apples-to-apples benchmark where both implementations are
run under the same producer/consumer setup (1 producer, 1 consumer):

- `ApplesToApplesLamportBufferBenchmark` with `implementation=VOLATILE|LOCK`

This comparison is workload-equal (same topology and capacity), but the
implementations have different design targets (`VOLATILE` is SPSC-optimized,
`LOCK` is MPMC-capable), so interpret results in that context.

## Build and run

```bash
# Build the shaded JMH runner
mvn -f demos/pom.xml -pl 07-jmh -am package

# Run all benchmarks
java -jar demos/07-jmh/target/benchmarks.jar

# Run one benchmark class
java -jar demos/07-jmh/target/benchmarks.jar ".*VolatileLamportBufferBenchmark.*"
java -jar demos/07-jmh/target/benchmarks.jar ".*LockBasedLamportBufferBenchmark.*"

# Run the apples-to-apples comparison only
java -jar demos/07-jmh/target/benchmarks.jar ".*ApplesToApplesLamportBufferBenchmark.*"

# Short run with GC profiler (quick sanity check)
java -jar demos/07-jmh/target/benchmarks.jar ".*ApplesToApplesLamportBufferBenchmark.*" -wi 3 -i 3 -f 1 -prof gc
```

The module creates a self-contained benchmark JAR (`target/benchmarks.jar`) using `maven-shade-plugin`.



