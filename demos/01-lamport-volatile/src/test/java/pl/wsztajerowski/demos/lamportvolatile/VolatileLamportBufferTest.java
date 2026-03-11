package pl.wsztajerowski.demos.lamportvolatile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wsztajerowski.demos.lamport.LamportBuffer;

import static pl.wsztajerowski.demos.lamport.assertions.LamportAssertions.assertThat;

class VolatileLamportBufferTest {

    private LamportBuffer<Integer> sut;

    @BeforeEach
    void setUp() {
        sut = VolatileLamportBuffer.createBuffer(Integer.class, 4);
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
