package com.cry.io_c;

import com.cry.io_c.core.Source;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * DESCRIPTION:
 * Author: Cry
 * DATE: 2018/7/2 下午11:36
 */

public class RealBufferedSource implements BufferedSource {

    public final Buffer buffer = new Buffer();
    public final Source source;
    boolean closed;

    public RealBufferedSource(Source source) {
        this.source = source;
    }

    @Override
    public Buffer buffer() {
        if (source == null) throw new NullPointerException("source == null");
        return buffer;
    }

    @Override
    public boolean exhausted() throws IOException {
        if (closed) throw new IllegalStateException("closed");
        return buffer.exhausted() && source.read(buffer, Segment.SIZE) == -1;
    }

    @Override
    public void skip(long byteCount) throws IOException {
        if (closed) throw new IllegalStateException("closed");
        while (byteCount > 0) {
            if (buffer.size == 0 && source.read(buffer, Segment.SIZE) == -1) {
                throw new EOFException();
            }
            long toSkip = Math.min(byteCount, buffer.size());
            buffer.skip(toSkip);
            byteCount -= toSkip;
        }
    }

    @Override
    public long read(Buffer sink, long byteCount) throws IOException {
        if (sink == null) throw new IllegalArgumentException("sink == null");
        if (byteCount < 0) throw new IllegalArgumentException("byteCount < 0: " + byteCount);
        if (closed) throw new IllegalStateException("closed");

        if (buffer.size == 0) {
            long read = source.read(buffer, Segment.SIZE);
            if (read == -1) return -1;
        }

        long toRead = Math.min(byteCount, buffer.size);
        return buffer.read(sink, toRead);
    }

    @Override
    public Timeout timeout() {
        return source.timeout();
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        source.close();
        buffer.clear();
    }

    @Override
    public int read(ByteBuffer sink) throws IOException {
        if (buffer.size == 0) {
            long read = source.read(buffer, Segment.SIZE);
            if (read == -1) return -1;
        }

        return buffer.read(sink);
    }
}
