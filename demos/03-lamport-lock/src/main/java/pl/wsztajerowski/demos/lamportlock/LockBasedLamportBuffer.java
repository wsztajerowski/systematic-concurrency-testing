package pl.wsztajerowski.demos.lamportlock;

import java.lang.reflect.Array;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import pl.wsztajerowski.demos.lamport.LamportBuffer;

public final class LockBasedLamportBuffer<E> implements LamportBuffer<E> {
    private int readPosition;
    private int writePosition;
    private final E[] buffer;
    private final Lock lock = new ReentrantLock();

    LockBasedLamportBuffer(E[] buffer) {
        this.readPosition = 0;
        this.writePosition = 0;
        this.buffer = buffer;
    }

    public static <T> LamportBuffer<T> createBuffer(Class<T> clazz, int bufferSize) {
        if (bufferSize < 1) {
            throw new IllegalArgumentException("bufferSize must be >= 1");
        }
        T[] buffer = (T[]) Array.newInstance(clazz, bufferSize);
        return new LockBasedLamportBuffer<>(buffer);
    }

    @Override
    public Optional<E> poll() {
        lock.lock();
        try {
            if (buffer[readPosition] == null) {
                return Optional.empty();
            }
            E elem = buffer[readPosition];
            buffer[readPosition] = null;
            readPosition = nextIndex(readPosition);
            return Optional.of(elem);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean offer(E element) {
        Objects.requireNonNull(element, "element must not be null");
        lock.lock();
        try {
            if (buffer[writePosition] != null) {
                return false;
            }
            buffer[writePosition] = element;
            writePosition = nextIndex(writePosition);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.lock();
        try {
            return buffer[readPosition] == null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        lock.lock();
        try {
            int diff = writePosition - readPosition;
            if (diff < 0) {
                diff += buffer.length;
            }
            if (diff == 0 && buffer[readPosition] != null) {
                return buffer.length;
            }
            return diff;
        } finally {
            lock.unlock();
        }
    }

    private int nextIndex(int index) {
        int next = index + 1;
        return next >= buffer.length ? 0 : next;
    }
}
