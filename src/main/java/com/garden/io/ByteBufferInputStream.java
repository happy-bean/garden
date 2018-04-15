package com.garden.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @author wgt
 * @date 2018-03-11
 * @description
 **/
public class ByteBufferInputStream extends InputStream {
    ByteBuffer bb;

    public ByteBufferInputStream(ByteBuffer bb) {
        this.bb = bb;
    }

    @Override
    public int read() throws IOException {
        if (bb.remaining() == 0) {
            return -1;
        }
        return bb.get() & 0xff;
    }

    @Override
    public int available() throws IOException {
        return bb.remaining();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (bb.remaining() == 0) {
            return -1;
        }
        if (len > bb.remaining()) {
            len = bb.remaining();
        }
        bb.get(b, off, len);
        return len;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public long skip(long n) throws IOException {
        if (n < 0L) {
            return 0;
        }
        n = Math.min(n, bb.remaining());
        bb.position(bb.position() + (int) n);
        return n;
    }

}
