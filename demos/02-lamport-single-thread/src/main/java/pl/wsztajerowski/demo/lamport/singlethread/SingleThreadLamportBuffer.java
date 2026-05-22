package pl.wsztajerowski.demo.lamport.singlethread;

import java.lang.reflect.Array;
import java.util.Objects;
import java.util.Optional;

import pl.wsztajerowski.demo.lamport.LamportBuffer;

public final class SingleThreadLamportBuffer<E> implements LamportBuffer<E> {
    private int readPosition;
    private int writePosition;
    private final E[] buffer;

    SingleThreadLamportBuffer(E[] buffer) {
        this.readPosition = 0;
        this.writePosition = 0;
        this.buffer = buffer;
    }

    public static <T> LamportBuffer<T> createBuffer(Class<T> clazz, int bufferSize) {
        if (bufferSize < 1) {
            throw new IllegalArgumentException("bufferSize must be >= 1");
        }
        T[] buffer = (T[]) Array.newInstance(clazz, bufferSize);
        return new SingleThreadLamportBuffer<>(buffer);
    }

    @Override
    public boolean offer(E value) {
        if (isFull()) {
            return false;
        }
        buffer[writePosition] = value;
        writePosition = writePosition + 1; // brak volatile publication
        if (writePosition == buffer.length) {
            writePosition = 0;
        }
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<E> poll() {
        if (isEmpty()) {
            return Optional.empty();
        }
        Object value = buffer[readPosition];
        buffer[readPosition] = null;
        readPosition = readPosition + 1; // brak volatile publication
        if (readPosition == buffer.length) {
            readPosition = 0;
        }
        return Optional.ofNullable((E) value);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public int size() {
        return writePosition >= readPosition ? writePosition - readPosition : writePosition - readPosition + 2 * buffer.length;
    }

    private boolean isFull() {
        return size() == buffer.length;
    }
}
