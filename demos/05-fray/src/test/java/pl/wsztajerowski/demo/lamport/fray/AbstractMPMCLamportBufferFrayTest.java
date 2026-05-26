package pl.wsztajerowski.demo.lamport.fray;

import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;
import pl.wsztajerowski.demo.lamport.LamportBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractMPMCLamportBufferFrayTest extends AbstractSPSCLamportBufferFrayTest {

    @ConcurrencyTest
    void twoConcurrentProducersMustNotLoseElements() throws InterruptedException {
        LamportBuffer<Integer> buffer = createBuffer(Integer.class, 4);
        buffer.offer(100); // pre-fills slot 0; writePosition wraps to 1
        List<Integer> offered = Collections.synchronizedList(new ArrayList<>());

        Thread producer1 = Thread.ofPlatform().name("fray-producer-1").start(() -> {
            if (buffer.offer(1)) offered.add(1);
        });
        Thread producer2 = Thread.ofPlatform().name("fray-producer-2").start(() -> {
            if (buffer.offer(2)) offered.add(2);
        });

        producer1.join();
        producer2.join();

        List<Integer> drained = new ArrayList<>();
        Optional<Integer> v;
        while ((v = buffer.poll()).isPresent()) drained.add(v.get());

        assertThat(drained).contains(100);       // pre-filled element must not be overwritten
        assertThat(drained).containsAll(offered); // every successfully offered element must be present
    }

    @ConcurrencyTest
    void twoConsumersMustNotReadSameElement() throws InterruptedException {
        List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());
        LamportBuffer<Integer> buffer = createBuffer(Integer.class, 4);
        buffer.offer(1);
        buffer.offer(2);

        Thread consumer1 = Thread.ofPlatform().name("fray-consumer-1").start(() -> {
            Optional<Integer> value = buffer.poll();
            value.ifPresent(consumed::add);
        });
        Thread consumer2 = Thread.ofPlatform().name("fray-consumer-2").start(() -> {
            Optional<Integer> value = buffer.poll();
            value.ifPresent(consumed::add);
        });

        consumer1.join();
        consumer2.join();

        assertThat(consumed)
            .hasSize(2)
            .containsExactlyInAnyOrder(1, 2);
    }

}
