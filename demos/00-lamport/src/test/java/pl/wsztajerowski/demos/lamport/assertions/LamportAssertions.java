package pl.wsztajerowski.demos.lamport.assertions;

import pl.wsztajerowski.demos.lamport.LamportBuffer;

public final class LamportAssertions {

    private LamportAssertions() {
    }

    public static <E> LamportBufferAssert<E> assertThat(LamportBuffer<E> actual) {
        return new LamportBufferAssert<>(actual);
    }
}

