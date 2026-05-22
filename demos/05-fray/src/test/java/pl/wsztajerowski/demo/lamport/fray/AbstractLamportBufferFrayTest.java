package pl.wsztajerowski.demo.lamport.fray;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;
import pl.wsztajerowski.demo.lamport.LamportBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(FrayTestExtension.class)
abstract class AbstractLamportBufferFrayTest {

    protected abstract <T> LamportBuffer<T> createBuffer(Class<T> clazz, int capacity);

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

    @ConcurrencyTest()
    void fifoOrderUnderConcurrency() throws InterruptedException {
        List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());
        LamportBuffer<Integer> buffer = createBuffer(Integer.class, 8);
        buffer.offer(1);
        var consumer = Thread.ofPlatform().name("fray-consumer").start(() -> {
            int count = 0;
            while (count < 5) {
                Optional<Integer> value = buffer.poll();
                if (value.isPresent()) {
                    consumed.add(value.get());
                    count++;
                } else {
                    Thread.yield();
                }
            }
        });
        var producer = Thread.ofPlatform()
            .name("fray-producer")
            .start(() -> {
                for (int i = 2; i <= 5; i++) {
                    boolean offer = buffer.offer(i);
                    assertThat(offer)
                        .isTrue();
                }
            });
        producer.join();
        consumer.join();

        assertThat(consumed).containsExactly(1, 2, 3, 4, 5);
    }

    //    @ConcurrencyTest
    void producerConsumerCompletesWithoutLoss() throws InterruptedException {
        LamportBuffer<Integer> buffer = createBuffer(Integer.class, 16);
        List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());

        Thread producer = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                while (!buffer.offer(i)) {
                    Thread.yield();
                }
            }
        });

        Thread consumer = new Thread(() -> {
            while (consumed.size() < 20) {
                Optional<Integer> value = buffer.poll();
                value.ifPresent(consumed::add);
                if (value.isEmpty()) {
                    Thread.yield();
                }
            }
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        assertThat(consumed).hasSize(20);
    }
}
