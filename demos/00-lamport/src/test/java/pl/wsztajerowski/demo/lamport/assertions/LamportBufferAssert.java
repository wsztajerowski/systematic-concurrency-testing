package pl.wsztajerowski.demo.lamport.assertions;

import java.util.Objects;
import java.util.Optional;
import org.assertj.core.api.AbstractAssert;
import pl.wsztajerowski.demo.lamport.LamportBuffer;

public final class LamportBufferAssert<E> extends AbstractAssert<LamportBufferAssert<E>, LamportBuffer<E>> {

    LamportBufferAssert(LamportBuffer<E> actual) {
        super(actual, LamportBufferAssert.class);
    }

    public LamportBufferAssert<E> isEmptyBuffer() {
        isNotNull();
        if (!actual.isEmpty()) {
            failWithMessage("Expected buffer to be empty but it was not");
        }
        return this;
    }

    public LamportBufferAssert<E> hasSize(int expectedSize) {
        isNotNull();
        int currentSize = actual.size();
        if (currentSize != expectedSize) {
            failWithMessage("Expected buffer size to be <%s> but was <%s>", expectedSize, currentSize);
        }
        return this;
    }

    public LamportBufferAssert<E> offers(E element) {
        isNotNull();
        if (!actual.offer(element)) {
            failWithMessage("Expected offer(%s) to succeed", element);
        }
        return this;
    }

    public LamportBufferAssert<E> rejectsOffer(E element) {
        isNotNull();
        if (actual.offer(element)) {
            failWithMessage("Expected offer(%s) to fail because buffer is full", element);
        }
        return this;
    }

    public LamportBufferAssert<E> pollsValue(E expected) {
        isNotNull();
        Optional<E> polled = actual.poll();
        if (polled.isEmpty()) {
            failWithMessage("Expected poll() to return <%s> but returned Optional.empty", expected);
        }
        if (!Objects.equals(polled.get(), expected)) {
            failWithMessage("Expected poll() to return <%s> but returned <%s>", expected, polled.get());
        }
        return this;
    }

    public LamportBufferAssert<E> pollsEmpty() {
        isNotNull();
        Optional<E> polled = actual.poll();
        polled.ifPresent(e -> failWithMessage("Expected poll() to return Optional.empty but returned <%s>", e));
        return this;
    }
}

