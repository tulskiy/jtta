package com.tulskiy.tta;

import java.io.FileInputStream;
import java.io.IOException;

import static com.tulskiy.tta.Constants.*;
import static com.tulskiy.tta.Macros.DEC;

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
                if (io.read(buffer, 0, TTA_FIFO_BUFFER_SIZE) == -1) {
                    System.out.println(io.getChannel().position());
                    throw new tta_exception(TTACodecStatus.TTA_READ_ERROR);
                }
                pos = 0;
            }

            short val = (short) (buffer[pos++] & 0xFF);
            // update crc32 and statistics
            crc = crc32_table[((int) ((crc ^ val) & 0xFF))] ^ (crc >> 8);
            count++;

            return val;
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

    int get_value(TTA_adapt rice) {
        int k, level, tmp;
        int value = 0;

        // decode Rice unsigned
        if ((bcache ^ bit_mask[bcount]) == 0) {
            value += bcount;
            bcache = read_byte();
            bcount = 8;
            while (bcache == 0xff) {
                value += 8;
                bcache = read_byte();
            }
        }

        while ((bcache & 1) != 0) {
            value++;
            bcache >>= 1;
            bcount--;
        }
        bcache >>= 1;
        bcount--;

        if (value != 0) {
            level = 1;
            k = rice.k1;
            value--;
        } else {
            level = 0;
            k = rice.k0;
        }

        if (k != 0) {
            while (bcount < k) {
                tmp = read_byte();
                bcache |= tmp << bcount;
                bcount += 8;
            }
            value = (int) ((value << k) + (bcache & bit_mask[k]));
            bcache >>= k;
            bcount -= k;
            bcache &= bit_mask[bcount];
        }

        if (level != 0) {
            rice.sum1 += value - (rice.sum1 >> 4);
            if (rice.k1 > 0 && rice.sum1 < shift_16[rice.k1])
                rice.k1--;
            else if (rice.sum1 > shift_16[rice.k1 + 1])
                rice.k1++;
            value += bit_shift[rice.k0];
        }

        rice.sum0 += value - (rice.sum0 >> 4);
        if (rice.k0 > 0 && rice.sum0 < shift_16[rice.k0])
            rice.k0--;
        else if (rice.sum0 > shift_16[rice.k0 + 1])
            rice.k0++;

        value = DEC(value);

        return value;
    } // get_value
}
