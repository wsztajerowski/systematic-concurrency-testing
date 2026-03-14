package pl.wsztajerowski.demo.lamport.mpmc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wsztajerowski.demo.lamport.LamportBuffer;

import static pl.wsztajerowski.demo.lamport.assertions.LamportAssertions.assertThat;

class LockBasedLamportBufferTest {

    private LamportBuffer<Integer> sut;

    @BeforeEach
    void setUp() {
        sut = LockBasedLamportBuffer.createBuffer(Integer.class, 4);
    }

    @Test
    @DisplayName("Smoke: new buffer starts empty")
    void startsEmpty() {
        assertThat(sut)
            .isEmptyBuffer()
            .hasSize(0)
            .pollsEmpty();
    }

    @Test
    @DisplayName("Smoke: offer then poll returns the same element")
    void offerThenPoll() {
        assertThat(sut)
            .offers(42)
            .pollsValue(42)
            .isEmptyBuffer();
    }
}
