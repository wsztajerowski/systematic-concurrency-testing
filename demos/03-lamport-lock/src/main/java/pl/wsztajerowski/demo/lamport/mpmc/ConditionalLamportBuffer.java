package pl.wsztajerowski.demo.lamport.mpmc;

import java.lang.reflect.Array;
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Blocking queue backed by a circular buffer.
 * Buggy: uses {@code if} instead of {@code while} around Condition.await() calls.
 * A spurious wakeup (which the JVM spec permits, and Fray explores as a valid
 * scheduling choice) causes take() to read from an empty slot and put() to
 * overwrite a full one.
 */
public final class ConditionalLamportBuffer<E> {
    private int readPosition;
    private int writePosition;
    private final E[] buffer;
    private final Lock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();

    @SuppressWarnings("unchecked")
    public ConditionalLamportBuffer(Class<E> clazz, int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be >= 1");
        this.buffer = (E[]) Array.newInstance(clazz, capacity);
    }

    public E take() throws InterruptedException {
        lock.lock();
        try {
            if (buffer[readPosition] == null) { // BUG: should be while — spurious wakeup skips re-check
                notEmpty.await();
            }
            E elem = buffer[readPosition];      // null when woken spuriously on an empty buffer
            buffer[readPosition] = null;
            readPosition = (readPosition + 1 >= buffer.length) ? 0 : readPosition + 1;
            notFull.signal();
            return elem;
        } finally {
            lock.unlock();
        }
    }

    public void put(E element) throws InterruptedException {
        Objects.requireNonNull(element);
        lock.lock();
        try {
            if (buffer[writePosition] != null) { // BUG: should be while
                notFull.await();
            }
            buffer[writePosition] = element;
            writePosition = (writePosition + 1 >= buffer.length) ? 0 : writePosition + 1;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }
}
