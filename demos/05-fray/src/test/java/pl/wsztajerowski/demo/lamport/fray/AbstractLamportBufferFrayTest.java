package pl.wsztajerowski.demo.lamport.fray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import pl.wsztajerowski.demo.lamport.LamportBuffer;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(FrayTestExtension.class)
abstract class AbstractLamportBufferFrayTest {

    protected abstract <T> LamportBuffer<T> createBuffer(Class<T> clazz, int capacity);

//    @ConcurrencyTest()
    void fifoOrderUnderConcurrency() throws InterruptedException {
        LamportBuffer<Integer> buffer = createBuffer(Integer.class, 8);
//        buffer.offer(1);
        List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());

        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                boolean offer = buffer.offer(i);
                assertThat(offer)
                    .isTrue();
            }
        });

        Thread consumer = new Thread(() -> {
            int count = 0;
            while (count < 5) {
                Optional<Integer> value = buffer.poll();
                if (value.isPresent()) {
                    consumed.add(value.get());
                    count++;
                }
            }
        });
        producer.start();
        consumer.start();

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
