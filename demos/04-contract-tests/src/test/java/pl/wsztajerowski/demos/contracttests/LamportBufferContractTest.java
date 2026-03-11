package pl.wsztajerowski.demos.contracttests;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import pl.wsztajerowski.demos.lamport.LamportBuffer;
import pl.wsztajerowski.demos.lamportlock.LockBasedLamportBuffer;
import pl.wsztajerowski.demos.lamportsinglethread.SingleThreadLamportBuffer;
import pl.wsztajerowski.demos.lamportvolatile.VolatileLamportBuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static pl.wsztajerowski.demos.lamport.assertions.LamportAssertions.assertThat;

class LamportBufferContractTest {

    private static final int CAPACITY = 4;

    private static LamportBuffer<Integer> createIntBuffer(Implementation implementation) {
        return implementation.factory.create(Integer.class, CAPACITY);
    }

    private static Stream<Implementation> implementations() {
        return Stream.of(
            new Implementation("01 volatile-based", VolatileLamportBuffer::createBuffer),
            new Implementation("02 single-thread", SingleThreadLamportBuffer::createBuffer),
            new Implementation("03 lock-based", LockBasedLamportBuffer::createBuffer)
        );
    }

    private record Implementation(String name, BufferFactory factory) {
        @Override
        public String toString() {
            return name;
        }
    }

    @FunctionalInterface
    private interface BufferFactory {
        <T> LamportBuffer<T> create(Class<T> clazz, int bufferSize);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("New buffer starts empty")
    void startsEmpty(Implementation implementation) {
        LamportBuffer<Integer> sut = createIntBuffer(implementation);

        assertThat(sut)
            .isEmptyBuffer()
            .hasSize(0)
            .pollsEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("Offer and poll a single element")
    void offerAndPollSingleElement(Implementation implementation) {
        LamportBuffer<Integer> sut = createIntBuffer(implementation);

        assertThat(sut)
            .offers(42)
            .pollsValue(42)
            .isEmptyBuffer();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("Multiple polls on empty buffer stay empty")
    void multiplePollsOnEmptyBuffer(Implementation implementation) {
        LamportBuffer<Integer> sut = createIntBuffer(implementation);

        assertThat(sut)
            .pollsEmpty()
            .pollsEmpty()
            .pollsEmpty()
            .isEmptyBuffer()
            .hasSize(0);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("Elements are consumed in FIFO order")
    void supportsFifoOrdering(Implementation implementation) {
        LamportBuffer<Integer> sut = createIntBuffer(implementation);

        assertThat(sut)
            .offers(1)
            .offers(2)
            .offers(3)
            .hasSize(3)
            .pollsValue(1)
            .pollsValue(2)
            .pollsValue(3)
            .pollsEmpty()
            .isEmptyBuffer();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("Interleaved produce/consume keeps FIFO order")
    void interleavedProduceConsumeMaintainsOrder(Implementation implementation) {
        LamportBuffer<Integer> sut = createIntBuffer(implementation);

        assertThat(sut)
            .offers(1)
            .offers(2)
            .pollsValue(1)
            .offers(3)
            .pollsValue(2)
            .pollsValue(3)
            .pollsEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("Offer on full buffer is rejected")
    void rejectsOfferWhenFull(Implementation implementation) {
        LamportBuffer<Integer> sut = createIntBuffer(implementation);

        assertThat(sut)
            .offers(10)
            .offers(11)
            .offers(12)
            .offers(13)
            .rejectsOffer(14)
            .hasSize(4)
            .pollsValue(10)
            .offers(14);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("Effective capacity matches configured capacity")
    void effectiveCapacityMatchesConfiguredCapacity(Implementation implementation) {
        LamportBuffer<Integer> sut = createIntBuffer(implementation);

        int count = 0;
        while (sut.offer(count)) {
            count++;
        }

        assertEquals(CAPACITY, count);
        assertThat(sut).hasSize(CAPACITY);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("Buffer handles wrap-around correctly")
    void wrapsAroundCorrectly(Implementation implementation) {
        LamportBuffer<Integer> sut = createIntBuffer(implementation);

        assertThat(sut)
            .offers(1)
            .offers(2)
            .offers(3)
            .pollsValue(1)
            .pollsValue(2)
            .offers(4)
            .offers(5)
            .hasSize(3)
            .pollsValue(3)
            .pollsValue(4)
            .pollsValue(5)
            .pollsEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("Wrap-around works across multiple fill/drain rounds")
    void handlesWrapAroundAcrossMultipleRounds(Implementation implementation) {
        LamportBuffer<Integer> sut = createIntBuffer(implementation);

        for (int round = 0; round < 3; round++) {
            for (int i = 0; i < CAPACITY; i++) {
                sut.offer(round * 100 + i);
            }
            for (int i = 0; i < CAPACITY; i++) {
                assertThat(sut).pollsValue(round * 100 + i);
            }
            assertThat(sut).isEmptyBuffer();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("Partial fill/drain with wrap-around preserves order")
    void partialWrapAroundKeepsOrder(Implementation implementation) {
        LamportBuffer<Integer> sut = createIntBuffer(implementation);

        for (int i = 0; i < 5; i++) {
            assertThat(sut)
                .offers(i)
                .pollsValue(i);
        }

        for (int i = 0; i < CAPACITY; i++) {
            assertThat(sut).offers(i * 10);
        }

        for (int i = 0; i < CAPACITY; i++) {
            assertThat(sut).pollsValue(i * 10);
        }
        assertThat(sut).pollsEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("Minimum capacity buffer works")
    void minimumCapacityBufferWorks(Implementation implementation) {
        LamportBuffer<String> small = implementation.factory.create(String.class, 1);

        assertThat(small)
            .offers("A")
            .rejectsOffer("B")
            .pollsValue("A")
            .pollsEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("Size reflects current buffer state")
    void sizeReflectsCurrentState(Implementation implementation) {
        LamportBuffer<Integer> sut = createIntBuffer(implementation);

        assertThat(sut)
            .hasSize(0)
            .offers(1)
            .hasSize(1)
            .offers(2)
            .offers(3)
            .hasSize(3)
            .pollsValue(1)
            .hasSize(2);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("Null values are rejected")
    void rejectsNullValues(Implementation implementation) {
        LamportBuffer<Integer> sut = createIntBuffer(implementation);

        Exception thrown = catchException(() -> sut.offer(null));

        assertThat(thrown)
            .isNotNull()
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    @DisplayName("Invalid buffer size is rejected")
    void rejectsInvalidBufferSize(Implementation implementation) {
        Exception thrown = catchException(() -> implementation.factory.create(Integer.class, 0));

        assertThat(thrown)
            .isNotNull()
            .isInstanceOf(IllegalArgumentException.class);
    }
}

