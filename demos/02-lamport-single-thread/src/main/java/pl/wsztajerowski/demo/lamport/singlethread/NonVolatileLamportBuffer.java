package pl.wsztajerowski.demo.lamport.singlethread;

import pl.wsztajerowski.demo.lamport.LamportBuffer;

import java.lang.reflect.Array;
import java.util.Objects;
import java.util.Optional;

public final class NonVolatileLamportBuffer<E> implements LamportBuffer<E> {
    private int readPosition;
    private int writePosition;
    private final E[] buffer;

    NonVolatileLamportBuffer(E[] buffer) {
        this.readPosition = 0;
        this.writePosition = 0;
        this.buffer = buffer;
    }

    public static <T> LamportBuffer<T> createBuffer(Class<T> clazz, int bufferSize) {
        if (bufferSize < 1) {
            throw new IllegalArgumentException("bufferSize must be >= 1");
        }
        T[] buffer = (T[]) Array.newInstance(clazz, bufferSize);
        return new NonVolatileLamportBuffer<>(buffer);
    }

    @Override
    public boolean offer(E value) {
        Objects.requireNonNull(value, "value must not be null");
        if (isFull()) {
            return false;
        }
        buffer[writePosition] = value;
        writePosition = writePosition + 1; // missing volatile publication
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
        readPosition = readPosition + 1; // missing volatile publication
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
        if (writePosition == readPosition) {
            return buffer[readPosition] == null ? 0 : buffer.length;
        }

        return writePosition > readPosition
                ? writePosition - readPosition
                : buffer.length - readPosition + writePosition;
    }

    private boolean isFull() {
        return size() == buffer.length;
    }
}
