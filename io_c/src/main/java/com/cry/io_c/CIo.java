package com.cry.io_c;

import com.cry.io_c.core.Sink;
import com.cry.io_c.core.Source;

import java.io.File;

/**
 * Created by a2957 on 2018/7/2.
 */

public interface CIo {
    BufferedSink buffer(Sink sink);
    BufferedSource buffer(Source sink);
    Sink sink(File file);
    Source source(File file);
}
