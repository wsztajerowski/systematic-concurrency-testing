package pl.wsztajerowski.demo.lamport.fray;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;
import pl.wsztajerowski.demo.lamport.LamportBuffer;
import pl.wsztajerowski.demo.lamport.mpmc.LockBasedLamportBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Idea 1 — TOCTOU: thread-safe objects do not compose into thread-safe operations.
 *
 * LockBasedLamportBuffer is internally correct, but the compound client-side
 * pattern "if (!isEmpty()) poll().get()" is not atomic. Between the isEmpty()
 * check and the poll() call, another thread can consume the only element,
 * leaving the second caller with Optional.empty() and a crashing .get().
 */
@ExtendWith(FrayTestExtension.class)
class TocTouCompoundActionFrayTest {

    @Disabled
    @ConcurrencyTest
    void emptyCheckAndPollMustBeAtomic() throws InterruptedException {
        LamportBuffer<Integer> buffer = LockBasedLamportBuffer.createBuffer(Integer.class, 4);
        buffer.offer(42);
        List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());

        Runnable consumer = () -> {
            if (!buffer.isEmpty()) {
                // Caller assumes element is still present — but another thread
                // may have consumed it between isEmpty() and poll().
                consumed.add(buffer.poll().get());
            }
        };

        Thread t1 = Thread.ofPlatform().name("consumer-1").start(consumer);
        Thread t2 = Thread.ofPlatform().name("consumer-2").start(consumer);
        t1.join();
        t2.join();

        assertThat(consumed).hasSize(1).containsExactly(42);
    }
}
