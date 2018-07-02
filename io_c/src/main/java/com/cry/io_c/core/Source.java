package com.cry.io_c.core;

import com.cry.io_c.Buffer;
import com.cry.io_c.Timeout;

import java.io.Closeable;
import java.io.IOException;

/**
 * 相当于InputStream
 *
 * 1. 加入Buffer进行缓存机制的优化
 * 2. 加入的TimeOut机制
 *
 * Created by a2957 on 2018/7/2.
 */
public interface Source extends Closeable{
    long read(Buffer sink, long byteCount) throws IOException;
    Timeout timeout();
    @Override void close() throws IOException;
}
