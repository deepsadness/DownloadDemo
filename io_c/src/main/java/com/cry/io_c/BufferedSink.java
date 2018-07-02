package com.cry.io_c;

import com.cry.io_c.core.Sink;

import java.nio.channels.WritableByteChannel;

/**
 * 带有缓存功能的Sink
 * 可以进行无性能损耗的写入
 *
 * WritableByteChannel 是nio的写入。write ByteBuffer
 * Created by a2957 on 2018/7/2.
 */
public interface BufferedSink extends Sink,WritableByteChannel{
    Buffer buffer();

}
