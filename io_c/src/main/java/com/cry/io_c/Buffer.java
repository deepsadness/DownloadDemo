package com.cry.io_c;

import com.sun.istack.internal.Nullable;

import java.io.Closeable;
import java.io.EOFException;
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
    public BufferedSink emitCompleteSegments() throws IOException {
        return this; // Nowhere to emit to!
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
        sink.write(this, byteCount);
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
        while (byteCount > 0) {
            //都是从head开始读
            //是否需要移动header?.
            //表示还取得出
            if (byteCount < (source.head.limit - source.head.pos)) {
                //取出head的prev就是尾部的segment
                Segment tail = head != null ? head.prev : null;
                if (tail != null && (byteCount + tail.limit - tail.pos <= Segment.SIZE)) {
                    //如果tail还有容量。
                    //就将source的head往当前的tail中写入
                    source.head.writeTo(tail, (int) byteCount);
                    //写入后，当前的大小变大，而source的变小
                    source.size -= byteCount;
                    size += byteCount;
                    return;
                } else {
                    //如果tail的容量不够，或者没有tail,则将head分成两个部分。
                    source.head = source.head.split((int) byteCount);
                }
            }

            //如果进行了切分，就需要重新分配
            Segment segmentToMove = source.head;
            long movedByteCount = segmentToMove.limit - segmentToMove.pos;
            //先将自己从原来的关系中清除
            source.head = segmentToMove.pop();
            //判断当前的情况
            if (head == null) {
                head = segmentToMove;
                head.next = head.prev = head;
            } else {
                //如果已经有了，就添加到最后去。
                Segment tail = head.prev;
                tail = tail.push(segmentToMove);
                tail.compact();
            }
            source.size -= movedByteCount;
            size += movedByteCount;
            byteCount -= movedByteCount;
        }
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
        //读取的话，就是读取head
        Segment s = head;
        if (s == null) return -1;

        int toCopy = Math.min(sink.remaining(), s.limit - s.pos);
        sink.put(s.data, s.pos, toCopy);

        s.pos += toCopy;
        size -= toCopy;

        //如果读取结束。就将head回收
        if (s.pos == s.limit) {
            head = s.pop();
            SegmentPool.recycle(s);
        }

        return toCopy;
    }

    @Override
    public int write(ByteBuffer source) throws IOException {
        if (source == null) throw new IllegalArgumentException("source == null");

        int byteCount = source.remaining();
        int remaining = byteCount;
        //不断通过segment进行复制bytes
        while (remaining > 0) {
            Segment tail = writableSegment(1);
            //获取需要copy的真实大小
            int toCopy = Math.min(remaining, Segment.SIZE - tail.limit);
            source.get(tail.data, tail.limit, toCopy);

            remaining -= toCopy;
            tail.limit += toCopy;
        }

        size += byteCount;
        return byteCount;
    }


    /**
     * Returns a tail segment that we can write at least {@code minimumCapacity}
     * bytes to, creating it if necessary.
     */
    Segment writableSegment(int minimumCapacity) {
        if (minimumCapacity < 1 || minimumCapacity > Segment.SIZE) throw new IllegalArgumentException();

        if (head == null) {
            head = SegmentPool.take(); // Acquire a first segment.
            return head.next = head.prev = head;
        }
        //取出尾部的。如果已经满了的话，就在构造一个
        Segment tail = head.prev;
//        if (tail.limit + minimumCapacity > Segment.SIZE || !tail.owner) {
        if (tail.limit + minimumCapacity > Segment.SIZE ) {
            tail = tail.push(SegmentPool.take()); // Append a new empty segment to fill up.
        }
        return tail;
    }

    /**
     * 返回可以直接写出的segment.这个大小不包括尾部，被写入的部分
     * Returns the number of bytes in segments that are not writable. This is the
     * number of bytes that can be flushed immediately to an underlying sink
     * without harming throughput.
     */
    public long completeSegmentByteCount() {
        long result = size;
        if (result == 0) return 0;

        // Omit the tail if it's still writable.
        Segment tail = head.prev;
        if (tail.limit < Segment.SIZE) {
            result -= tail.limit - tail.pos;
        }

        return result;
    }

    /**
     * Discards all bytes in this buffer. Calling this method when you're done
     * with a buffer will return its segments to the pool.
     */
    public void clear() {
        try {
            skip(size);
        } catch (EOFException e) {
            throw new AssertionError(e);
        }
    }
    /** Discards {@code byteCount} bytes from the head of this buffer. */
    @Override public void skip(long byteCount) throws EOFException {
        while (byteCount > 0) {
            if (head == null) throw new EOFException();

            int toSkip = (int) Math.min(byteCount, head.limit - head.pos);
            size -= toSkip;
            byteCount -= toSkip;
            head.pos += toSkip;

            if (head.pos == head.limit) {
                Segment toRecycle = head;
                head = toRecycle.pop();
                SegmentPool.recycle(toRecycle);
            }
        }
    }

}
