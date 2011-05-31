package com.tulskiy.tta;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Author: Denis Tulskiy
 * Date: 5/30/11
 */
class TTA_fifo {
    public static final int TTA_FIFO_BUFFER_SIZE = 5120;

    byte[] buffer = new byte[TTA_FIFO_BUFFER_SIZE];
    int pos;
    int bcount; // count of bits in cache
    int bcache; // bit cache
    long crc;
    int count;
    FileInputStream io;

    void reader_reset() {
        // init crc32, reset counter
        crc = 0xffffffffL;
        bcache = 0;
        bcount = 0;
        count = 0;
    }

    short read_byte() {
        try {
            if (pos == buffer.length) {
                if (io.read(buffer, 0, TTA_FIFO_BUFFER_SIZE) == -1)
                    throw new tta_exception(TTACodecStatus.TTA_READ_ERROR);
                pos = 0;
            }

            // update crc32 and statistics
            crc = Constants.crc32_table[((int) ((crc ^ buffer[pos]) & 0xff))] ^ (crc >> 8);
            count++;

            return (short) (buffer[pos++] & 0xFF);
        } catch (IOException e) {
            throw new tta_exception(TTACodecStatus.TTA_READ_ERROR, e);
        }
    }

    int read_uint16() {
        int value = 0;

        value |= read_byte();
        value |= read_byte() << 8;

        return value;
    } // read_uint16

    long read_uint32() {
        long value = 0;

        value |= read_byte();
        value |= read_byte() << 8;
        value |= read_byte() << 16;
        value |= read_byte() << 24;

        return value;
    } // read_uint32

    boolean read_crc32() {
        long new_crc = crc ^ 0xffffffffL;
        return (new_crc != read_uint32());
    } // read_crc32

    void reader_skip_bytes(long size) {
        while (size-- > 0) read_byte();
    } // reader_skip_bytes

    void read(byte[] buf, int size) {
        for (int i = 0; i < size; i++)
            buf[i] = (byte) read_byte();
    }

    void seek(long pos) {
        try {
            io.getChannel().position(pos);
        } catch (IOException e) {
            throw new tta_exception(TTACodecStatus.TTA_SEEK_ERROR, e);
        }
    }

    void reader_start() {
        pos = buffer.length;
    }
}
