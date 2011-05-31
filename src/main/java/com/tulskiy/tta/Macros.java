package com.tulskiy.tta;

/**
 * Author: Denis Tulskiy
 * Date: 5/31/11
 */
public class Macros {
    static long MUL_FRAME_TIME(long x) {
        return (256 * (x) / 245);
    } // = x * FRAME_TIME

    static int PREDICTOR1(int x, int k) {
        return (x * ((1 << k) - 1)) >> k;
    }

    static int DEC(int x) {
        return (((x & 1) != 0) ? ((x + 1) >> 1) : (-x >> 1));
    }

    static int WRITE_BUFFER(int x, byte[] buf, int pos, int depth) {
	    if (depth == 2) {
            buf[pos++] = (byte) (x & 0xFF);
            buf[pos++] = (byte) ((x >> 8) & 0xFF);
        } else if (depth == 1) {
            buf[pos++] = (byte) (x & 0xFF);
        } else {
            buf[pos++] = (byte) (x & 0xFF);
            buf[pos++] = (byte) ((x >> 8) & 0xFF);
            buf[pos++] = (byte) ((x >> 16) & 0xFF);
        }
        return pos;
    }
}
