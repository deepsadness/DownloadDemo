package com.cry.io_c;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by a2957 on 2018/7/2.
 */

public class Timeout {
    //返回一个空白的TimeOut
    public static final Timeout NONE = new Timeout() {
        @Override
        public Timeout timeout(long timeout, TimeUnit unit) {
            return this;
        }

        @Override
        public Timeout deadlineNanoTime(long deadlineNanoTime) {
            return this;
        }

        @Override
        public void throwIfReached() throws IOException {
        }
    };

    public Timeout timeout(long timeout, TimeUnit unit) {
        return this;
    }

    public Timeout deadlineNanoTime(long deadlineNanoTime) {
        return this;
    }

    public void throwIfReached() throws IOException {
    }
}
