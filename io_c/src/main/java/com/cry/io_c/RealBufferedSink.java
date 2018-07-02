package com.cry.io_c;

import com.cry.io_c.core.Sink;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * DESCRIPTION:
 * BufferedSink的包装类。将sink的操作转给里面的Buffer.
 * Author: Cry
 * DATE: 2018/7/2 下午11:28
 */
public class RealBufferedSink implements BufferedSink {

    public final Buffer buffer = new Buffer();
    public final Sink sink;
    boolean closed;

    RealBufferedSink(Sink sink) {
        if (sink == null) throw new NullPointerException("sink == null");
        this.sink = sink;
    }

    @Override
    public Buffer buffer() {
        return buffer;
    }

    @Override
    public void write(Buffer source, long byteCount) throws IOException {
        if (closed) throw new IllegalStateException("closed");
        buffer.write(source, byteCount);
        emitCompleteSegments();
    }

    @Override
    public Timeout timeout() {
        return Timeout.NONE;
    }

    @Override
    public void flush() throws IOException {
        if (closed) throw new IllegalStateException("closed");
        if (buffer.size > 0) {
            sink.write(buffer, buffer.size);
        }
        sink.flush();
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public void close() throws IOException {
        if (closed) return;

        // Emit buffered data to the underlying sink. If this fails, we still need
        // to close the sink; otherwise we risk leaking resources.
        Throwable thrown = null;
        try {
            if (buffer.size > 0) {
                sink.write(buffer, buffer.size);
            }
        } catch (Throwable e) {
            thrown = e;
        }

        try {
            sink.close();
        } catch (Throwable e) {
            if (thrown == null) thrown = e;
        }
        closed = true;

//        if (thrown != null) Util.sneakyRethrow(thrown);
    }

    @Override
    public int write(ByteBuffer source) throws IOException {
        if (closed) throw new IllegalStateException("closed");
        int result = buffer.write(source);
        emitCompleteSegments();
        return result;
    }

    @Override
    public BufferedSink emitCompleteSegments() throws IOException {
        if (closed) throw new IllegalStateException("closed");
        long byteCount = buffer.completeSegmentByteCount();
        if (byteCount > 0) sink.write(buffer, byteCount);
        return this;
    }
}
