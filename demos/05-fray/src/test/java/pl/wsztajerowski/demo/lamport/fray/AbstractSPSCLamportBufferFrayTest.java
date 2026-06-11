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
abstract class AbstractSPSCLamportBufferFrayTest {

    protected abstract <T> LamportBuffer<T> createBuffer(Class<T> clazz, int capacity);

    @ConcurrencyTest()
    void fifoOrderUnderConcurrency() throws InterruptedException {
        List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());
        LamportBuffer<Integer> buffer = createBuffer(Integer.class, 8);
//        buffer.offer(1);
        var producer = Thread.ofPlatform()
            .name("fray-producer")
            .start(() -> {
                int i = 1;
                do {
                    boolean offer = buffer.offer(i);
                    if (offer) {
                        i++;
                    }
                } while (i <= 5);
                if (i != 6) {
                    throw new RuntimeException("Producer failed to offer all elements");
                }
            });
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
        producer.join();
        consumer.join();

        assertThat(consumed).containsExactly(1, 2, 3, 4, 5);
    }

    @ConcurrencyTest
    void producerConsumerCompletesWithoutLoss() throws InterruptedException {
        LamportBuffer<Integer> buffer = createBuffer(Integer.class, 32);
        List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());

        var producer = Thread.ofPlatform()
            .name("fray-producer")
            .start(() -> {
                for (int i = 0; i < 20; i++) {
                    buffer.offer(i);
                }
            });

        var consumer = Thread.ofPlatform()
            .name("fray-consumer")
            .start(() -> {
                while (consumed.size() < 20) {
                    Optional<Integer> value = buffer.poll();
                    value.ifPresent(consumed::add);
                    if (value.isEmpty()) {
                        Thread.yield();
                    }
                }
            });

        producer.join();
        consumer.join();

        assertThat(consumed).hasSize(20);
    }
}
