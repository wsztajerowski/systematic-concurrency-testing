package pl.wsztajerowski.demo.lamport.fray.edgecase;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Idea 4 — Condition variable spurious wakeup: {@code if} vs {@code while}.
 *
 * ConditionalLamportBuffer uses {@code if (empty) await()} instead of
 * {@code while (empty) await()}. The JVM specification permits await() to
 * return without a corresponding signal (a "spurious wakeup"). Fray explores
 * this as a valid scheduling choice: the consumer wakes before the producer
 * has put anything, proceeds past the {@code if} guard, and reads a null slot.
 * This is a class of bug that never appears in ordinary test runs — it requires
 * systematic exploration to expose.
 */
@ExtendWith(FrayTestExtension.class)
class ConditionalLamportBufferFrayTest {

    @Disabled
    @ConcurrencyTest
    void spuriousWakeupCausesReadFromEmptyBuffer() throws InterruptedException {
        pl.wsztajerowski.demo.lamport.mpmc.ConditionalLamportBuffer<Integer> buffer = new pl.wsztajerowski.demo.lamport.mpmc.ConditionalLamportBuffer<>(Integer.class, 4);
        AtomicReference<Integer> consumed = new AtomicReference<>();

        Thread consumer = Thread.ofPlatform().name("consumer").start(() -> {
            try {
                consumed.set(buffer.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread producer = Thread.ofPlatform().name("producer").start(() -> {
            try {
                buffer.put(42);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        consumer.join();
        producer.join();

        assertThat(consumed.get())
            .as("consumer must receive the produced value, not null from a spurious wakeup")
            .isNotNull()
            .isEqualTo(42);
    }
}
