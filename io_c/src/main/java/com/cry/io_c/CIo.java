package com.cry.io_c;

import com.cry.io_c.core.Sink;
import com.cry.io_c.core.Source;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 通过这个类来构建Source和Sink.同时进行inputStream的转换.
 *
 * 到此为止是本地文件的io.如果是socket的io呢？nio呢？
 * Created by a2957 on 2018/7/2.
 */
public class CIo {
    public static BufferedSink buffer(Sink sink) {
        return new RealBufferedSink(sink);
    }

    public static BufferedSource buffer(Source source) {
        return new RealBufferedSource(source);
    }

    public static Sink sink(File file) throws FileNotFoundException {
        if (file == null) throw new IllegalArgumentException("file == null");
        return sink(new FileOutputStream(file));
    }

    /**
     * Returns a sink that writes to {@code out}.
     */
    public static Sink sink(OutputStream out) {
        return sink(out, new Timeout());
    }

    private static Sink sink(final OutputStream out, final Timeout timeout) {
        if (out == null) throw new IllegalArgumentException("out == null");
        if (timeout == null) throw new IllegalArgumentException("timeout == null");

        return new Sink() {
            @Override
            public void write(Buffer source, long byteCount) throws IOException {
//                checkOffsetAndCount(source.size, 0, byteCount);
                while (byteCount > 0) {
                    timeout.throwIfReached();
                    Segment head = source.head;
                    int toCopy = (int) Math.min(byteCount, head.limit - head.pos);

                    out.write(head.data, head.pos, toCopy);

                    head.pos += toCopy;
                    byteCount -= toCopy;
                    source.size += toCopy;

                    if (head.pos == head.limit) {
                        source.head = head.pop();
                        SegmentPool.recycle(head);
                    }
                }
            }

            @Override
            public Timeout timeout() {
                return timeout;
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void close() throws IOException {
                out.close();
            }
        };
    }

    //
    public static Source source(File file) throws FileNotFoundException {
        if (file == null) throw new IllegalArgumentException("file == null");
        return source(new FileInputStream(file));
    }

    public static Source source(InputStream in) throws FileNotFoundException {
        if (in == null) throw new IllegalArgumentException("in == null");
        return source(in, new Timeout());
    }

    private static Source source(final InputStream in, final Timeout timeout) {
        if (in == null) throw new IllegalArgumentException("in == null");
        if (timeout == null) throw new IllegalArgumentException("timeout == null");

        return new Source() {
            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                if (byteCount < 0)
                    throw new IllegalArgumentException("byteCount < 0: " + byteCount);
                if (byteCount == 0) return 0;
                try {
                    timeout.throwIfReached();
//                    写入是往最后写入
                    Segment tail = sink.writableSegment(1);
                    int maxToCp = (int) Math.min(byteCount, Segment.SIZE - tail.limit);
                    int bytesRead = in.read(tail.data, tail.limit, maxToCp);
                    if (bytesRead == -1) {
                        return -1;
                    }
                    tail.limit += bytesRead;
                    sink.size += bytesRead;
                    return bytesRead;
                }catch (AssertionError e){
                    if (isAndroidGetsocknameError(e)) throw new IOException(e);
                    throw e;
                }
            }

            @Override
            public Timeout timeout() {
                return timeout;
            }

            @Override
            public void close() throws IOException {
                in.close();
            }
        };
    }
    /**
     * Returns true if {@code e} is due to a firmware bug fixed after Android 4.2.2.
     * https://code.google.com/p/android/issues/detail?id=54072
     */
    static boolean isAndroidGetsocknameError(AssertionError e) {
        return e.getCause() != null && e.getMessage() != null
                && e.getMessage().contains("getsockname failed");
    }

}
