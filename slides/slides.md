---
theme: default
title: "From False Confidence to Systematic Proof: a story about testing Concurrent Algorithms in Java"
info: |
  A journey through Lamport's Circular Buffer using JUnit, Fray, jcstress, and JMH.
  Each tool asks a fundamentally different question about correctness.
mdc: true
lineNumbers: false
highlighter: shiki
colorSchema: dark
fonts:
  sans: Inter
  mono: Fira Code
---

# From False Confidence<br>to Systematic Proof

A story about testing Concurrent Algorithms in Java

<p class="cover-tools">JUnit · Fray · jcstress · JMH</p>

---
layout: section
---

# The Problem

---

# CI Was Green. Production Was on Fire.

<v-clicks>

- A bounded queue — simple, elegant, well-tested
- Thousands of unit tests, all green ✅
- Deployed to production

</v-clicks>

<v-click>

```
Exception in thread "consumer-3" java.lang.NullPointerException
    at pl.wsztajerowski.demo.lamport.LamportBuffer.poll(LamportBuffer.java:47)
```
</v-click>

<v-click>
<div class="callout yellow">
Single-threaded tests are <strong>blind to concurrency bugs by design</strong> <br>
They cannot fail on a race condition — there is no race.
</div>
</v-click>

---

# Four Questions. Four Tools.

<div class="tools-grid">
<div v-click class="card blue">
  <strong>① JUnit</strong>
  <small>Functional logic · single thread</small>
  <p>Does the algorithm work at all?</p>
</div>
<div v-click class="card purple">
  <strong>② Fray</strong>
  <small>Systematic interleaving exploration</small>
  <p>Does any thread schedule break it?</p>
</div>
<div v-click class="card orange">
  <strong>③ jcstress</strong>
  <small>Java Memory Model stress testing</small>
  <p>Does the CPU / JVM / JIT break it?</p>
</div>
<div v-click class="card green">
  <strong>④ JMH</strong>
  <small>Rigorous microbenchmarking</small>
  <p>What does correctness cost?</p>
</div>
</div>

<v-click>
<div class="callout">
Each layer catches bugs the others cannot see.
</div>
</v-click>

---
layout: section
---

# Section 1
## Lamport's Circular Buffer

---
layout: two-cols
---

# The Algorithm

**Lamport's wait-free SPSC queue (1983)**

- Bounded circular array of capacity `N`
- Two indices: `head` (producer) and `tail` (consumer)
- **No locks. No CAS. No blocking.**
- O(1) offer and poll · low allocation pressure

::right::

```
capacity = 4
[ _, _, _, _ ]
  ↑        ↑
 tail     head
```

---

# The Algorithm — Pseudocode

<v-click>
```
function offer(element):
    h ← head                         // read producer index
    if buffer[h] != null:
        return false                 // buffer full
    buffer[h] ← element
    head ← (h + 1) mod capacity      // publish to consumer
    return true
```
</v-click> 

<v-click>
```
function poll():
    t ← tail                         // read consumer index
    if buffer[t] == null:
        return empty                 // buffer empty
    element ← buffer[t]
    buffer[t] ← null                 // free slot for producer
    tail ← (t + 1) mod capacity      // publish to producer
    return element
```
</v-click>

---
layout: two-cols
---

# Why `volatile` Is Non-Negotiable

- Each CPU core has its own cache
- Without a memory barrier, writes stay **local**
- The JIT compiler and CPU may **reorder** instructions
- `volatile` creates a **happens-before** guarantee

::right::

<v-click>
**Without `volatile` — stale read:**

```
Producer CPU             Consumer CPU
─────────────────────────────────────
write head = 1
  └─ stays in L1 cache
                         read head → 0  ← stale!
```
</v-click>

<v-click>
**With `volatile` — guaranteed visibility:**

```
Producer CPU             Consumer CPU
─────────────────────────────────────
write head = 1
  └─ flushed to main memory
                         read head → 1  ✅
```
</v-click>

---

# Four Implementations — One Interface

```java
public interface LamportBuffer<E> {
    boolean offer(E element);
    Optional<E> poll();
}
```

<div class="impl-grid">
<div v-click class="card blue">
  <strong>Volatile</strong>
  <small>SPSC · volatile fields</small>
  <p>✅ Correct for one producer, one consumer</p>
</div>
<div v-click class="card red">
  <strong>SingleThread</strong>
  <small>No synchronisation</small>
  <p>❌ <code>volatile</code> deliberately removed</p>
</div>
<div v-click class="card yellow">
  <strong>LockBased</strong>
  <small>MPMC · ReentrantLock</small>
  <p>✅ Safe for many producers and consumers</p>
</div>
<div v-click class="card orange">
  <strong>FastTrack</strong>
  <small>Lock + unlocked fast-path</small>
  <p>⚠️ Subtle race in the optimisation path</p>
</div>
</div>

---

# The Catch

<br>

<div class="callout yellow" style="font-size: 1.15em; line-height: 1.7;">
All four pass unit tests.<br>
Not all are actually correct.
</div>

---
layout: section
class: section-junit
---

# Section 2
## Layer 1: JUnit Contract Tests

---
layout: two-cols
---

# The Foundation

**Purpose: verify the algorithm's functional logic**

- Single-threaded — no concurrency
- Same contract suite runs against **all four implementations**
- Fast, deterministic, always-on

**What gets tested:**
- Basic offer / poll · FIFO ordering
- Wrap-around behaviour · capacity enforcement
- Size tracking · null rejection

::right::

<v-click>

```java
@ParameterizedTest
@MethodSource("bufferImplementations")
void shouldPreserveFifoOrder(
        LamportBuffer<Integer> buffer) {
    assertThat(buffer)
        .accepting(1, 2, 3)
        .whenPolled(3)
        .returns(1, 2, 3);
}

static Stream<LamportBuffer<Integer>>
        bufferImplementations() {
    return Stream.of(
        new VolatileLamportBuffer<>(8),
        new SingleThreadLamportBuffer<>(8),
        new LockBasedLamportBuffer<>(8),
        new OptimisticLamportBuffer<>(8)
    );
}
```
</v-click>

---

# The False Positive

Run the full suite against all four implementations — including deliberately broken ones:

```
[INFO] Running LamportBufferContractTest
[INFO] Tests run: 56, Failures: 0, Errors: 0, Skipped: 0

[INFO] BUILD SUCCESS
```

**All green. ✅  But two implementations are broken.**

---

# Why JUnit Cannot See This

<v-clicks>

- JUnit runs on **one thread** — no concurrent access to trigger the bug
- The JVM applies no memory barrier — the test never exercises visibility
- Missing `volatile` is **invisible** to single-threaded tests

</v-clicks>

<v-click>
<div class="callout blue">
Write contract tests first. Keep them always.<br>
But do not mistake a green suite for a concurrency correctness proof.
</div>
</v-click>

---
layout: section
class: section-fray
---

# Section 3
## Layer 2: Fray — Systematic Interleaving Exploration

---

# Fray's Core Idea

> "Does **any execution schedule** exist that breaks the logic of my system?"

<v-clicks>

- Controls the **scheduler** — decides which thread runs next
- Instruments synchronisation points and controls thread switches
- Every run explores a **different interleaving** — exhaustively

</v-clicks>

---
hide: true
---
 
# Every Run — A Different Schedule

```
Run #1:   T1 → T1 → T2 → T1 → T2
Run #2:   T2 → T1 → T2 → T2 → T1
Run #3:   T1 → T2 → T2 → T1 → T1
...
Run #N:   every reachable schedule explored
```

No reliance on the OS scheduler "getting lucky".

---

# Funny lib settings

By default, Fray use share report directory for all test case outputs and remove old results before run new one. 
Since all tests default to the same fray-report/ folder, each new test class nukes the previous test's recording. 

Solution:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <systemPropertyVariables>
            <fray.organize.by.test>true</fray.organize.by.test>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

---

# A Fray Test

```java
@ConcurrencyTest
void twoConsumersMustNotReadSameElement() {
    buffer.offer(1);
    buffer.offer(2);
    Thread t1 = new Thread(() -> results.add(buffer.poll()));
    Thread t2 = new Thread(() -> results.add(buffer.poll()));
    t1.start(); t2.start();
    t1.join();  t2.join();
    assertThat(results).containsExactlyInAnyOrder(
        Optional.of(1), Optional.of(2));
}
```

Standard JUnit test — Fray controls the scheduler underneath.

---
layout: two-cols
---

# What Fray Finds — Unlocked Fast-path

```java
public Optional<E> poll() {
    if (buffer[readPosition] == null) // ← no lock!
        return Optional.empty();
    lock.lock();
    try {
        E elem = (E) buffer[readPosition]; // ← re-read
        // ...
    } finally { lock.unlock(); }
}
```

::right::

**The interleaving Fray constructs:**

```
T1: passes null-check      (slot has element)
T2: passes null-check      (slot still has element)
T1: acquires lock, reads, advances readPosition
T2: acquires lock, reads... null → 💥 NPE
```
<v-click>
<div class="callout purple">
Check-then-act without a lock is a race.
</div>
</v-click>

---
layout: two-cols
---

# What Fray Finds — Spurious Wakeup

```java
public Optional<E> poll() throws InterruptedException {
    lock.lock();
    try {
        if (isEmpty()) condition.await(); // ← if, not while!
        return Optional.of((E) buffer[readPosition++]);
    } finally { lock.unlock(); }
}
```

::right::

**The schedule Fray triggers:**

```
await() returns early
  → still empty
  → Optional.of(null)
  → 💥
```

<v-click>
<div class="callout purple">
Guard <code>Condition.await()</code> with <code>while</code>, not <code>if</code>.
</div>
</v-click>
<v-click>

```java
// ❌ Unsafe — one spurious wakeup is enough
if (isEmpty()) condition.await();

// ✅ Correct — re-check after every wakeup
while (isEmpty()) condition.await();
```
</v-click>

---

# Deterministic Replay

Classic problem: a test fails on iteration 721 — but which schedule caused it?

```
2026-05-26 22:49:04 [INFO]: Error found at iter: 721, step: 1850, Elapsed time: 60ms
2026-05-26 22:49:04 [INFO]: Error: java.lang.AssertionError: [consumer must receive the produced value, 
not null from a spurious wakeup] 
Expecting actual not to be null
Thread: Thread[#3,main,5,main]
java.lang.AssertionError: [consumer must receive the produced value, not null from a spurious wakeup] 
Expecting actual not to be null
	at pl.wsztajerowski.demo.lamport.fray.edgecase.ConditionalLamportBufferFrayTest
	.spuriousWakeupCausesReadFromEmptyBuffer(ConditionalLamportBufferFrayTest.java:54)
    ...

2026-05-26 22:49:04 [INFO]: The recording is saved to /demos/05-fray/target/fray/fray-report/.../recording

```

<v-click>

**Fray records and replays the exact schedule:**

```java
@ConcurrencyTest(
        replay = "PATH_TO_FRAY_REPORT/recording"
)
```

Attach a debugger. Step through the exact thread switches.

</v-click>

---

# Fray's Blind Spot

`NonVolatileLamportBufferFrayTest` — missing `volatile`. Fray result:

```
[INFO] Running pl.wsztajerowski.demo.lamport.fray.NonVolatileLamportBufferFrayTest
[INFO] Tests run: 3000, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.459 s 

BUILD SUCCESS
```

---

# Why Fray Cannot See This

<v-clicks>

- Fray controls **thread scheduling** — not CPU caches or JIT optimisations
- The bug is a **memory visibility** failure, not a scheduling failure
- Appears only with real hardware effects (cache latency + instruction reordering)

</v-clicks>

<v-click>
<div class="callout purple">

**Fray proves:** "No thread ordering breaks my logic."<br>
**Fray cannot prove:** "The JVM / CPU will not reorder my memory accesses."

</div>
</v-click>

---
layout: section
class: section-jcstress
---

# Section 4
## Layer 3: jcstress — JMM Stress Testing

---

# jcstress's Core Idea

> "Is this code correct relative to the **Java Memory Model**?"

<v-clicks>

- OpenJDK's laboratory for the Java Memory Model
- Runs millions of iterations on **real hardware**
- Lets OS / JVM / CPU choose execution order, then **observes outcomes**
- Can stress cache coherence by controlling where actors run.
- It can <u>pin actors to specific CPUs</u> (CPU affinity, where supported)

</v-clicks>

---

# A jcstress Test

```java
@JCStressTest
@Outcome(id = "1, 1, 0", expect = Expect.ACCEPTABLE, 
    desc = "Producer offers the element, then consumer polls it")
@Outcome(id = "1, 0, 1", expect = Expect.ACCEPTABLE, 
    desc = "Consumer polls the element, before producer offers")
@Outcome(id = "1, 0, 0", expect = Expect.FORBIDDEN, 
    desc = "Producer offers the element, but consumer fails to poll it - visibility bug")
@State
public class NonVolatileSpscLamportBufferJcstressTest {
    private final LamportBuffer<Integer> queue = NonVolatileLamportBuffer.createBuffer(Integer.class, 2);

    @Actor
    public void producer(III_Result r) {
        r.r1 = queue.offer(1) ? 1 : 0;
    }
    @Actor
    public void consumer(III_Result r) {
        r.r2 = queue.poll().orElse(0);
    }
    @Arbiter
    public void arbiter(III_Result r) {
        r.r3 = queue.poll().orElse(0);
    }
}
```

---

# jcstress Finds What Fray Cannot

`SingleThreadLamportBuffer` — the broken implementation:

```
   RESULT      SAMPLES     FREQ       EXPECT  DESCRIPTION
  1, 0, 0       35.222   <0,01%    Forbidden  Producer offers the element, but consumer fails to poll i...
  1, 0, 1  308.860.121   47,12%   Acceptable  Consumer polls the element, before producer offers
  1, 1, 0  346.641.431   52,88%   Acceptable  Producer offers the element, then consumer polls it
```

<v-click>

<div class="callout orange">
<strong>0.1%</strong> of executions: the producer's write never reached the consumer's cache.
</div>
</v-click>

---

<img src="/src/resources/jcstress-result.png" alt="jcstress result output" style="max-height: 80vh; margin: 0 auto;" />

---

# Fray vs. jcstress

|  | Fray | jcstress |
|--|------|---------|
| **Core question** | Does a bad schedule exist? | Is this correct vs. the JMM? |
| **Controls** | Thread scheduling | Nothing — lets OS/CPU/JIT decide |
| **Finds** | Logic races · deadlocks | Visibility bugs · reorderings |
| **Deterministic replay** | ✅ Yes | ❌ No |
| **Blind spot** | JMM / hardware reorderings | Rare interleavings |

<v-click>
<div class="callout orange">

`NonVolatileLamportBuffer` passes Fray, fails jcstress.<br>
`FastTrackLamportBuffer` is caught by Fray, may be missed by jcstress.

</div>
</v-click>

---
hide: true
layout: two-cols
---

# TOCTOU with Two Producers

```java
@JCStressTest
@Outcome(id = "true, false",  expect = ACCEPTABLE)
@Outcome(id = "false, true",  expect = ACCEPTABLE)
@Outcome(id = "true, true",   expect = ACCEPTABLE_INTERESTING,
         desc = "TOCTOU: both believe they succeeded")
@Outcome(id = "false, false", expect = FORBIDDEN)
@State
public class TwoProducersVolatileBufferStress {

    private final LamportBuffer<Integer> buffer =
        new VolatileLamportBuffer<>(1); // capacity 1!

    @Actor public void producer1(ZZ_Result r) { r.r1 = buffer.offer(1); }
    @Actor public void producer2(ZZ_Result r) { r.r2 = buffer.offer(2); }
}
```

::right::

```
RESULT           FREQ    EXPECT
true, false      48.3%   Acceptable
false, true      48.1%   Acceptable
true, true        3.6%   Interesting ← TOCTOU!
false, false      0.0%   Forbidden
```

<v-click>

Both producers see the buffer as non-full **simultaneously** — one silently overwrites the other.

<div class="callout orange">
<code>volatile</code> protects <strong>visibility</strong>, not <strong>atomicity</strong>.
</div>

</v-click>

---
layout: section
class: section-jmh
---

# Section 5
## Layer 4: JMH — Measuring the Cost of Correctness

---

# Only Benchmark Correct Code

**Why naive benchmarks lie:**

- JIT eliminates "dead" computations — you measure nothing
- Poor warmup distorts steady-state numbers
- Loop hoisting can move work outside the benchmark body

**JMH solves this:**

- `Blackhole` prevents dead code elimination
- Warmup + fork isolation produce stable, comparable measurements

---

# A JMH Benchmark

```java
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Group)
public class ApplesToApplesBenchmark {

    @Param({"64", "1024"}) int capacity;
    LamportBuffer<Integer> buffer;

    @Benchmark @Group("spsc") @GroupThreads(1)
    public void producer(Blackhole bh) { bh.consume(buffer.offer(42)); }

    @Benchmark @Group("spsc") @GroupThreads(1)
    public void consumer(Blackhole bh) { bh.consume(buffer.poll()); }
}
```

---

# Lock-Free vs. Lock-Based

**1 producer + 1 consumer — same topology, two implementations:**

```
Benchmark                             (capacity)  (implementation)   Mode  Cnt         Score   Error  Units
JmhBenchmark.applesToApples                   64          VOLATILE  thrpt    2  16814467,255          ops/s
JmhBenchmark.applesToApples:consumer          64          VOLATILE  thrpt    2   8370300,747          ops/s
JmhBenchmark.applesToApples:producer          64          VOLATILE  thrpt    2   8444166,508          ops/s
JmhBenchmark.applesToApples                   64              LOCK  thrpt    2   4277676,491          ops/s
JmhBenchmark.applesToApples:consumer          64              LOCK  thrpt    2   2138839,366          ops/s
JmhBenchmark.applesToApples:producer          64              LOCK  thrpt    2   2138837,125          ops/s
JmhBenchmark.applesToApples                 1024          VOLATILE  thrpt    2  18750040,034          ops/s
JmhBenchmark.applesToApples:consumer        1024          VOLATILE  thrpt    2   9097580,713          ops/s
JmhBenchmark.applesToApples:producer        1024          VOLATILE  thrpt    2   9652459,321          ops/s
JmhBenchmark.applesToApples                 1024              LOCK  thrpt    2   5942621,982          ops/s
JmhBenchmark.applesToApples:consumer        1024              LOCK  thrpt    2   2971303,913          ops/s
JmhBenchmark.applesToApples:producer        1024              LOCK  thrpt    2   2971318,069          ops/s
```

<v-click>
<p class="callout green">

Lock-free volatile: **~3× higher throughput** for SPSC workloads.<br>
Lock-free is not always better — only when used correctly (SPSC only).

</p>
</v-click>

---
layout: section
---

# Section 6
## The Testing Pyramid

---

# Each Layer Answers a Different Question

```
                    ┌───────────────┐
                    │      JMH      │               "How fast is it?"
                    └───────────────┘
                 ┌─────────────────────┐
                 │      jcstress       │            "Does CPU/JVM/JIT break it?"
                 └─────────────────────┘
              ┌───────────────────────────┐
              │           Fray            │         "Does any thread schedule break it?"
              └───────────────────────────┘
           ┌─────────────────────────────────┐
           │      JUnit Contract Tests       │      "Does the algorithm work at all?"
           └─────────────────────────────────┘
```

---

# When to Reach for Which Tool

| Symptom / Goal | Reach for |
|---|---|
| Functional correctness and regression safety | **JUnit** |
| Scheduling bugs (races, deadlocks, ordering) | **Fray** |
| JMM bugs (visibility, reordering, atomicity) | **jcstress** |
| Throughput / latency / perf regressions | **JMH** |

---

# Three Golden Rules

<v-clicks>
<div class="rule">
  <h2>① Never trust green unit tests as a concurrency proof</h2>
  <p>A single-threaded test cannot trigger a race. All four implementations passed; two were broken.</p>
</div>
<div class="rule">
  <h2>② Fray and jcstress are complementary — not interchangeable</h2>
  <p>Fray finds scheduling bugs jcstress may miss. jcstress finds JMM bugs Fray cannot see. Run both.</p>
</div>
<div class="rule">
  <h2>③ Only benchmark code you have already proven correct</h2>
  <p>Accurate numbers for broken code are worse than no numbers.</p>
</div>
</v-clicks>

---
layout: two-cols
---

# Resources

**Tools used in this talk**

- [Fray](https://github.com/cmu-pasta/fray) — CMU PASTA Lab / Microsoft Research
- [jcstress](https://openjdk.org/projects/code-tools/jcstress/) — OpenJDK
- [JMH](https://github.com/openjdk/jmh) — OpenJDK

**Demo repository**

```
github.com/wsztajerowski/systematic-concurrency-testing
```

::right::

**Further reading**

- [A Randomized Scheduler with
  Probabilistic Guarantees of Finding Bugs](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/asplos277-pct.pdf)
- [Partial Order Aware Concurrency Sampling](https://www.cs.columbia.edu/~junfeng/papers/pos-cav18.pdf)
- [JSR-133 Java Memory Model](https://jcp.org/en/jsr/detail?id=133)

---
layout: center
class: text-center
---

# Thank You

<br>

Questions?

<br>
<br>

<span style="color: rgba(255,255,255,0.3); font-size: 0.85em;">
From False Confidence to Systematic Proof: a story about testing Concurrent Algorithms in Java
</span>
