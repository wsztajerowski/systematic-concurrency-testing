package pl.wsztajerowski.demos.fray;

import org.junit.jupiter.api.extension.ExtendWith;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;
import pl.wsztajerowski.demos.lamport.LamportBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(FrayTestExtension.class)
abstract class LamportBufferFrayTest {

    protected abstract <T> LamportBuffer<T> createBuffer(Class<T> clazz, int capacity);

    @ConcurrencyTest
    void offerPollRace() throws InterruptedException {
    }

    //

    // two threads, one producer and one consumer
    // producer offers a 3 elements to the buffer, consumer polls it in a loop until it gets the element,
    // then it checks if the elements are in correct order
    // FIFO odrering under concurrency
     @ConcurrencyTest
     void fifoOrderUnderConcurrency() throws InterruptedException {
         LamportBuffer<Integer> buffer = createBuffer(Integer.class, 8);
//         List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());
     }


}

