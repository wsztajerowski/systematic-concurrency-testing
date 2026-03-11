package pl.wsztajerowski.demos.lamport;

import java.util.Optional;

public interface LamportBuffer<E> {

    boolean offer(E element);

    Optional<E> poll();

    boolean isEmpty();

    int size();
}
