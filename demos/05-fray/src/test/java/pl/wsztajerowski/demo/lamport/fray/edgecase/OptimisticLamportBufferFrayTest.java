package pl.wsztajerowski.demo.lamport.fray.edgecase;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;
import pl.wsztajerowski.demo.lamport.LamportBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Idea 3 — Optimistic lock-free fast path without re-verification.
 *
 * OptimisticLamportBuffer checks buffer[readPosition] == null outside the lock
 * as a fast-path early return. When two consumers both pass that unlocked check,
 * they serialize on the lock. The first consumer reads the element and advances
 * readPosition. The second consumer, now inside the lock, reads the slot that
 * readPosition now points to — which is null — and passes it to Optional.of(),
 * causing a NullPointerException.
 */
@ExtendWith(FrayTestExtension.class)
class OptimisticLamportBufferFrayTest {

    @Disabled
    @ConcurrencyTest
    void twoConsumersOnSingleElementMustNotCrash() throws InterruptedException {
        LamportBuffer<Integer> buffer = pl.wsztajerowski.demo.lamport.mpmc.OptimisticLamportBuffer.createBuffer(Integer.class, 4);
        buffer.offer(42);
        List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());

        Thread consumer1 = Thread.ofPlatform().name("consumer-1").start(() ->
            buffer.poll().ifPresent(consumed::add)
        );
        Thread consumer2 = Thread.ofPlatform().name("consumer-2").start(() ->
            buffer.poll().ifPresent(consumed::add)
        );

        consumer1.join();
        consumer2.join();

        assertThat(consumed).hasSize(1).containsExactly(42);
    }
}
