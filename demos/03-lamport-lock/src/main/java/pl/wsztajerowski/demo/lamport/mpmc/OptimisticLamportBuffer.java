package pl.wsztajerowski.demo.lamport.mpmc;


import pl.wsztajerowski.demo.lamport.LamportBuffer;

import java.lang.reflect.Array;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Buggy: uses an unlocked fast-path null check in poll() to avoid lock
 * acquisition when the buffer appears empty, but does NOT re-verify the
 * condition after acquiring the lock. Two consumers can both pass the
 * unlocked check, and the second one reads a null slot after the first
 * has already consumed the element and advanced readPosition.
 */
public final class OptimisticLamportBuffer<E> implements LamportBuffer<E> {
    private int readPosition;
    private int writePosition;
    private final E[] buffer;
    private final Lock lock = new ReentrantLock();

    private OptimisticLamportBuffer(E[] buffer) {
        this.buffer = buffer;
    }

    @SuppressWarnings("unchecked")
    public static <T> LamportBuffer<T> createBuffer(Class<T> clazz, int bufferSize) {
        if (bufferSize < 1) throw new IllegalArgumentException("bufferSize must be >= 1");
        return new OptimisticLamportBuffer<>((T[]) Array.newInstance(clazz, bufferSize));
    }

    @Override
    public Optional<E> poll() {
        if (buffer[readPosition] == null) { // fast path checked WITHOUT the lock
            return Optional.empty();
        }
        lock.lock();
        try {
            // BUG: no re-check inside the lock — assumes slot is still non-null
            E elem = buffer[readPosition];     // null if first consumer already advanced readPosition
            buffer[readPosition] = null;
            readPosition = (readPosition + 1 >= buffer.length) ? 0 : readPosition + 1;
            return Optional.of(elem);          // throws NullPointerException when elem is null
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean offer(E element) {
        Objects.requireNonNull(element);
        lock.lock();
        try {
            if (buffer[writePosition] != null) return false;
            buffer[writePosition] = element;
            writePosition = (writePosition + 1 >= buffer.length) ? 0 : writePosition + 1;
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        return buffer[readPosition] == null;
    }

    @Override
    public int size() {
        lock.lock();
        try {
            int diff = writePosition - readPosition;
            if (diff < 0) diff += buffer.length;
            if (diff == 0 && buffer[readPosition] != null) return buffer.length;
            return diff;
        } finally {
            lock.unlock();
        }
    }
}
