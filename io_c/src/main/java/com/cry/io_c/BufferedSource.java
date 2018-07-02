package com.cry.io_c;

import com.cry.io_c.core.Source;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 * 带有缓存的source.无性能损耗的读取
 * Created by a2957 on 2018/7/2.
 */

public interface BufferedSource extends Source,ReadableByteChannel {
    Buffer buffer();


    /**
     * Returns true if there are no more bytes in this source. This will block until there are bytes
     * to read or the source is definitely exhausted.
     * 如果这个source没有更多的bytes时。返回true.直到还有bytes来读取。或者完全没有了。
     */
    boolean exhausted() throws IOException;

    void skip(long byteCount) throws IOException;

}
