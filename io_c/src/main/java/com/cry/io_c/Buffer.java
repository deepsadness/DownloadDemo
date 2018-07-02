package com.cry.io_c;

import com.sun.istack.internal.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 * buffer同时实现了BufferedSink和BufferedSource
 * Created by a2957 on 2018/7/2.
 */

public final class Buffer implements BufferedSource, BufferedSink, Closeable, ByteChannel {
    //内部缓存的头部
    @Nullable
    Segment head;
    long size;

    /**
     * Returns the number of bytes currently in this buffer.
     * 返回当前buffer中的size的大小
     */
    public final long size() {
        return size;
    }

    @Override
    public Buffer buffer() {
        return this;
    }

    @Override
    public boolean exhausted() throws IOException {
        return size == 0;
    }

    @Override
    public long read(Buffer sink, long byteCount) throws IOException {
        if (sink == null) throw new IllegalArgumentException("sink == null");
        if (byteCount < 0) throw new IllegalArgumentException("byteCount < 0: " + byteCount);
        //如果size为0.则返回-1
        if (size == 0) return -1L;
        //如果读取的部分大于当前的size,则将其等于size
        if (byteCount > size) byteCount = size;
        //读取时，直接调用sink，写进去。这里的读取，其实主要是做限制大小的作用的？
        sink.write(this,byteCount);
        return byteCount;
    }

    /*
    这部分是是精华所在
    在不消耗CPU和不浪费内存的情况下，将source buffer的head 中的bytes移动到这个buffer中的tail。

    1. Don't waste CPU (ie. don't copy data around).不浪费CPU.(这意味着不去复制数据)

    复制大量的数据是非常耗费资源的。取而代之的是，我们将整个Segement从这个Buffer，重新分配到另外一个。

    2. Don't waste memory(不浪费内存)

    作为一个不变量，除了head和tail segment中，在buffer中相邻的segments应该至少50%的填充。
    head segment不能有不变量。因为应用从中正在消费bytes,降低他的水平。
    tail segment不能有不变量。因为应用不断向其生产bytes,而要求近乎全空的tail segments来追加bytes.

    3. 在buffers中移动segments

    我们只是将segment重新分配到buffer的segment后。不会去错复制的操作。下面两个例子，都不会。
    When writing one buffer to another, we prefer to reassign entire segments
    over copying bytes into their most compact form. Suppose we have a buffer
    with these segment levels [91%, 61%]. If we append a buffer with a
    single [72%] segment, that yields [91%, 61%, 72%]. No bytes are copied.

    Or suppose we have a buffer with these segment levels: [100%, 2%], and we
    want to append it to a buffer with these segment levels [99%, 3%]. This
    operation will yield the following segments: [100%, 2%, 99%, 3%]. That
    is, we do not spend time copying bytes around to achieve more efficient
    memory use like [100%, 100%, 4%].

    只有两个segment相加，不会超过100%时，我们才会进行合并相邻的segments.如下面的例子
    When combining buffers, we will compact adjacent buffers when their
    combined level doesn't exceed 100%. For example, when we start with
    [100%, 40%] and append [30%, 80%], the result is [100%, 70%, 80%].


    4.切分Segments
    有时候，我们需要指写入一部分的数据。这个时候，就会先去切分。然后再写入。如下例子
    Occasionally we write only part of a source buffer to a sink buffer. For
    example, given a sink [51%, 91%], we may want to write the first 30% of
    a source [92%, 82%] to it. To simplify, we first transform the source to
    an equivalent buffer [30%, 62%, 82%] and then move the head segment,
    yielding sink [51%, 91%, 30%] and source [62%, 82%].

     */
    @Override
    public void write(Buffer source, long byteCount) throws IOException {
        if (source == null) throw new IllegalArgumentException("source == null");
        if (source == this) throw new IllegalArgumentException("source == this");
//        checkOffsetAndCount(source.size,0,byteCount);
//
//        while (byteCount>0){
//
//            if (byteCount<(source.head.limit-source.head.pos))
//        }
    }

    @Override
    public Timeout timeout() {
        return Timeout.NONE;
    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() throws IOException {

    }


    @Override
    public int read(ByteBuffer sink) throws IOException {
        return 0;
    }

    @Override
    public int write(ByteBuffer source) throws IOException {
        return 0;
    }
}
