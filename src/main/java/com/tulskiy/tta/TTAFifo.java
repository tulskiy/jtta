package com.tulskiy.tta;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Author: Denis Tulskiy
 * Date: 5/30/11
 */
class TTAFifo {
    public static final int TTA_FIFO_BUFFER_SIZE = 5120;

    byte[] buffer = new byte[TTA_FIFO_BUFFER_SIZE];
    byte end;
    int pos;
    int bcount; // count of bits in cache
    int bcache; // bit cache
    long crc;
    int count;
    DataInputStream io;
    FileInputStream inputStream;

    void reader_reset() {
        // init crc32, reset counter
        crc = 0xffffffffL;
        bcache = 0;
        bcount = 0;
        count = 0;
    }


}
