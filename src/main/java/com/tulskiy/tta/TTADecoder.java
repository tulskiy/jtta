package com.tulskiy.tta;

import java.io.FileInputStream;

import static com.tulskiy.tta.TTACodecStatus.*;

/**
 * Author: Denis Tulskiy
 * Date: 5/30/11
 */
public class TTADecoder {
    public static final int MAX_DEPTH = 3;
    public static final int MAX_BPS = (MAX_DEPTH * 8);
    public static final int MIN_BPS = 16;
    public static final int MAX_NCH = 6;

    // TTA audio format
    public static final int TTA_FORMAT_SIMPLE = 1;
    public static final int TTA_FORMAT_ENCRYPTED = 2;

    public static final int[] flt_set = {10, 9, 10};

    boolean seek_allowed;    // seek table flag

    TTA_fifo fifo;
    TTA_codec[] decoder; // decoder (1 per channel)
    byte[] data;    // decoder initialization data
    boolean password_set;    // password protection flag
    long[] seek_table; // the playing position table
    int format;    // tta data format
    int rate;    // bitrate (kbps)
    long offset;    // data start position (header size, bytes)
    long frames;    // total count of frames
    int depth;    // bytes per sample
    long flen_std;    // default frame length in samples
    long flen_last;    // last frame length in samples
    long flen;    // current frame length in samples
    int fnum;    // currently playing frame index
    long fpos;    // the current position in frame

    public TTADecoder(FileInputStream inputStream) {
        fifo = new TTA_fifo();
        fifo.io = inputStream;
        data = new byte[8];
    }

    public TTA_info init_get_info(long pos) {
        TTA_info info = new TTA_info();
        // set start position if required
        if (pos > 0) {
            fifo.seek(pos);
        }


        fifo.reader_start();
        pos += read_tta_header(info);

        // check for supported formats
        if (info.format > 2 ||
                info.bps < MIN_BPS ||
                info.bps > MAX_BPS ||
                info.nch > MAX_NCH)
            throw new tta_exception(TTA_FORMAT_ERROR);

        // check for required data is present
        if (info.format == TTA_FORMAT_ENCRYPTED) {
            if (!password_set)
                throw new tta_exception(TTA_PASSWORD_ERROR);
        }

        offset = pos; // size of headers
        format = info.format;
        depth = (info.bps + 7) / 8;
        flen_std = MUL_FRAME_TIME(info.sps);
        flen_last = info.samples % flen_std;
        frames = info.samples / flen_std + (flen_last > 0 ? 1 : 0);
        if (flen_last == 0) flen_last = flen_std;
        rate = 0;

        // allocate memory for seek table data
        seek_table = new long[(int) frames];

        seek_allowed = read_seek_table();
        decoder = new TTA_codec[info.nch];
        for (int i = 0; i < decoder.length; i++) {
            decoder[i] = new TTA_codec();
        }

        frame_init(0, false);
        return info;
    }

    int read_tta_header(TTA_info info) {
        int size = skip_id3v2();

        fifo.reader_reset();
        byte[] header = new byte[4];
        fifo.read(header, header.length);

        if (!"TTA1".equals(new String(header))) throw new tta_exception(TTA_FORMAT_ERROR);

        info.format = fifo.read_uint16();
        info.nch = fifo.read_uint16();
        info.bps = fifo.read_uint16();
        info.sps = fifo.read_uint32();
        info.samples = fifo.read_uint32();

        if (!fifo.read_crc32())
            throw new tta_exception(TTA_FILE_ERROR);

        size += 22; // sizeof TTA header

        return size;
    } // read_tta_header

    int skip_id3v2() {
        int size = 0;

        fifo.reader_reset();

        // id3v2 header must be at start
        byte[] header = new byte[3];
        fifo.read(header, 3);
        if (!"ID3".equals(new String(header))) {
            fifo.pos = 0;
            return 0;
        }

        fifo.pos += 2; // skip version bytes
        if ((fifo.read_byte() & 0x10) != 0) size += 10;

        size += (fifo.read_byte() & 0x7f);
        size = (size << 7) | (fifo.read_byte() & 0x7f);
        size = (size << 7) | (fifo.read_byte() & 0x7f);
        size = (size << 7) | (fifo.read_byte() & 0x7f);

        fifo.reader_skip_bytes(size);

        return (size + 10);
    } // skip_id3v2

    boolean read_seek_table() {
        long tmp;
        int i;

        if (seek_table == null) return false;

        fifo.reader_reset();

        tmp = offset + (frames + 1) * 4;
        for (i = 0; i < frames; i++) {
            seek_table[i] = tmp;
            tmp += fifo.read_uint32();
        }

        return fifo.read_crc32();
    } // read_seek_table

    void frame_init(int frame, boolean seek_needed) {
        int shift = flt_set[depth - 1];

        if (frame >= frames) return;

        fnum = frame;

        if (seek_needed && seek_allowed) {
            long pos = seek_table[fnum];
            if (pos > 0) {
                fifo.seek(pos);
            }
            fifo.reader_start();
        }

        if (fnum == frames - 1)
            flen = flen_last;
        else flen = flen_std;

        // init entropy decoder
        for (TTA_codec dec : decoder) {
            filter_init(dec.fst, data, shift);
            rice_init(dec.rice, 10, 10);
            dec.prev = 0;
        }

        fpos = 0;

        fifo.reader_reset();
    } // frame_init

    void filter_init(TTA_fltst fs, byte[] data, int shift) {
        fs.shift = shift;
        fs.round = 1 << (shift - 1);
        fs.qm[0] = data[0];
        fs.qm[1] = data[1];
        fs.qm[2] = data[2];
        fs.qm[3] = data[3];
        fs.qm[4] = data[4];
        fs.qm[5] = data[5];
        fs.qm[6] = data[6];
        fs.qm[7] = data[7];
    } // filter_init

    void rice_init(TTA_adapt rice, int k0, int k1) {
        rice.k0 = k0;
        rice.k1 = k1;
        rice.sum0 = Constants.shift_16[k0];
        rice.sum1 = Constants.shift_16[k1];
    } // rice_init


    long MUL_FRAME_TIME(long x) {
        return (256 * (x) / 245);
    } // = x * FRAME_TIME
}
