package pl.wsztajerowski.demo.lamport.fray;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;
import pl.wsztajerowski.demo.lamport.LamportBuffer;
import pl.wsztajerowski.demo.lamport.singlethread.SingleThreadLamportBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(FrayTestExtension.class)
public class LamportSingleThreadBufferFrayTest {

    // two threads, one producer and one consumer
    // producer offers a 3 elements to the buffer, consumer polls it in a loop until it gets the element,
    // then it checks if the elements are in correct order
    // FIFO odrering under concurrency


    @ConcurrencyTest
    void fifoOrderUnderConcurrency() throws InterruptedException {
        List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());
        LamportBuffer<Integer> buffer = SingleThreadLamportBuffer.createBuffer(Integer.class, 8);
        buffer.offer(1);
        var consumer = Thread.ofPlatform().name("fray-consumer").start(() -> {
            int count = 0;
            while (count < 5) {
                Optional<Integer> value = buffer.poll();
                if (value.isPresent()) {
                    consumed.add(value.get());
                    count++;
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

}
