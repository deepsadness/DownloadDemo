package com.cry.io_c.core;

import com.cry.io_c.Buffer;
import com.cry.io_c.Timeout;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

/**
 * 相当于outputStream
 * <p>
 * 1,2和Source相同。
 * outputStream不同的地方是需要flush.将缓存写出
 * <p>
 * Created by a2957 on 2018/7/2.
 */
public interface Sink extends Closeable, Flushable {
    void write(Buffer source, long byteCount) throws IOException;

    Timeout timeout();

    @Override
    void flush() throws IOException;

    @Override
    void close() throws IOException;
}
