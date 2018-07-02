package com.cry.io_c;

import com.sun.istack.internal.Nullable;

/**
 * Buffer中的缓存机制
 * 1. 8192大小
 * 2. 链表的结构
 * 3. pop and push
 * 4. compact 和split
 * <p>
 * Created by a2957 on 2018/7/2.
 */
final class Segment {
    /**
     * The size of all segments in bytes.
     */
    static final int SIZE = 8192;
    /**
     * Segments will be shared when doing so avoids {@code arraycopy()} of this many bytes.
     */
    static final int SHARE_MINIMUM = 1024;
    /**
     * 缓存的数据
     */
    final byte[] data;
    /**
     * The next byte of application data byte to read in this segment.
     * 应用能从这个Segment中读取有效数据的位置
     */
    int pos;
    /**
     * The first byte of available data ready to be written to.
     * 准备写入的可用数据的第一个字节
     */
    int limit;
//    boolean shared;
//    boolean owner;
    /**
     * 循环链表中的下一个segment
     */
    Segment next;
    /**
     * 循环链表中的上一个segment
     */
    Segment prev;

    public Segment() {
        //初始化最大的大小
        this.data = new byte[SIZE];
    }

    /**
     * Removes this segment of a circularly-linked list and returns its successor.
     * Returns null if the list is now empty.
     * 将当前的segment从循环链表中移除，并返回他的后继。如果为空，则返回空
     */
    public final @Nullable
    Segment pop() {
        //如果next为自己，就说明没哟了
        Segment result = next != this ? next : null;
        //修改链表结构
        prev.next = next;
        next.prev = prev;
        //将自己的引用都清空
        next = null;
        prev = null;
        return result;
    }

    /**
     * Appends {@code segment} after this segment in the circularly-linked list.
     * Returns the pushed segment.
     * 添加一个Segment在链表的后面。返回推入的
     */
    public final Segment push(Segment segment) {
        //插入一个。
        segment.prev = this;
        segment.next = next;
        next.prev = segment;
        next = segment;
        return segment;
    }

    /**
     * 在当前segment和它的前驱都小于50%时，调用，会进行合并。并回收当前的segment
     */
    public final void compact() {
        if (prev == this) throw new IllegalStateException();
        //当前的大小
        int byteCount = limit - pos;
        //计算还可以合并的大小
        int availableByteCount = SIZE - prev.limit + prev.pos;
        if (byteCount > availableByteCount) return;//此时说明不够合并
        //将当前的byteCount写入到prev中
        writeTo(prev, byteCount);
        //将当前的Segment推出
        pop();
        //回收这个Segment
        SegmentPool.recycle(this);
    }

    /**
     * 将{@code byteCount}大小的bytes从当前segment移动到{@code sink}.中*
     */
    public final void writeTo(Segment sink, int byteCount) {
//        if (!sink.owner) throw new IllegalArgumentException();

        //如果超过了大小。那就只能重置这个pos,将pos移动到0
        if (sink.limit + byteCount > SIZE) { //这样的话，就超过了？
//            // We can't fit byteCount bytes at the sink's current position. Shift sink first.
//            if (sink.shared) throw new IllegalArgumentException();
            if (sink.limit + byteCount - sink.pos > SIZE) throw new IllegalArgumentException();
            System.arraycopy(sink.data, sink.pos, sink.data, 0, sink.limit - sink.pos);
            sink.limit -= sink.pos;
            sink.pos = 0;
        }

        //将当前的复制到sink中
        System.arraycopy(data, pos, sink.data, sink.limit, byteCount);
        //limit增加对应的byteCount
        sink.limit += byteCount;
        //当前的pos进行移动，表示这段的byteCount大小的数据已经被used掉了
        pos += byteCount;
    }
}
