package pl.wsztajerowski.demo.lamport.spsc;

import java.lang.reflect.Array;
import java.util.Objects;
import java.util.Optional;

import pl.wsztajerowski.demo.lamport.LamportBuffer;

public final class VolatileLamportBuffer<E> implements LamportBuffer<E> {
    private volatile int readPosition;
    private volatile int writePosition;
    private final E[] buffer;

    VolatileLamportBuffer(E[] buffer) {
        this.readPosition = 0;
        this.writePosition = 0;
        this.buffer = buffer;
    }

    public static <T> LamportBuffer<T> createBuffer(Class<T> clazz, int bufferSize){
        if (bufferSize < 1) {
            throw new IllegalArgumentException("bufferSize must be >= 1");
        }
        T[] buffer = (T[]) Array.newInstance(clazz, bufferSize);
        return new VolatileLamportBuffer<>(buffer);
    }

    @Override
    public Optional<E> poll(){
        if (buffer[readPosition] == null){
            return Optional.empty();
        }
        E elem = buffer[readPosition];
        buffer[readPosition] = null;
        int nextReadPosition = (readPosition + 1 >= buffer.length) ? 0 : readPosition + 1;
        readPosition = nextReadPosition;
        return Optional.of(elem);
    }

    @Override
    public boolean offer(E element){
        Objects.requireNonNull(element, "element must not be null");
        if(buffer[writePosition] != null){
            return false;
        }
        buffer[writePosition] = element;
        int nextWritePosition = (writePosition + 1 >= buffer.length) ? 0 : writePosition + 1;
        writePosition = nextWritePosition;
        return true;
    }

    @Override
    public boolean isEmpty() {
        return buffer[readPosition] == null;
    }

    @Override
    public int size() {
        int diff = writePosition - readPosition;
        if (diff < 0) {
            diff += buffer.length;
        }
        // In this implementation write==read means either empty or full.
        if (diff == 0 && buffer[readPosition] != null) {
            return buffer.length;
        }
        return diff;
    }
}
