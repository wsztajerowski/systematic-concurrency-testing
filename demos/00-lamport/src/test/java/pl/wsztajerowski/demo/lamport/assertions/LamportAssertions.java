package pl.wsztajerowski.demo.lamport.assertions;

import pl.wsztajerowski.demo.lamport.LamportBuffer;

public final class LamportAssertions {

    private LamportAssertions() {
    }

    public static <E> LamportBufferAssert<E> assertThat(LamportBuffer<E> actual) {
        return new LamportBufferAssert<>(actual);
    }
}

