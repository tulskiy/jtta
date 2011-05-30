package com.tulskiy.tta;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import static com.tulskiy.tta.TTACodecStatus.*;

/**
 * Author: Denis Tulskiy
 * Date: 5/30/11
 */
public class TTADecoder {
    boolean seek_allowed;    // seek table flag

    TTAFifo fifo;
    //            TTA_codec decoder[MAX_NCH]; // decoder (1 per channel)
//            TTA_codec *decoder_last;
    byte[] data;    // decoder initialization data
    boolean password_set;    // password protection flag
    long[] seek_table; // the playing position table
//            TTAuint32 format;	// tta data format
//            TTAuint32 rate;	// bitrate (kbps)
//            TTAuint64 offset;	// data start position (header size, bytes)
//            TTAuint32 frames;	// total count of frames
//            TTAuint32 depth;	// bytes per sample
//            TTAuint32 flen_std;	// default frame length in samples
//            TTAuint32 flen_last;	// last frame length in samples
//            TTAuint32 flen;	// current frame length in samples
//            TTAuint32 fnum;	// currently playing frame index
//            TTAuint32 fpos;	// the current position in frame

    public TTADecoder(FileInputStream inputStream) {
        fifo = new TTAFifo();
        fifo.inputStream = inputStream;
        fifo.io = new DataInputStream(new BufferedInputStream(inputStream, TTAFifo.TTA_FIFO_BUFFER_SIZE));
        data = new byte[8];
    }

    public TTAInfo init_get_info() {
    }

    int read_tta_header(TTAInfo info) throws tta_exception {
        try {
            int size = skip_id3v2();

            fifo.reader_reset();
            byte[] header = new byte[4];
            fifo.io.readFully(header);

            if (!"TTA1".equals(new String(header))) throw new tta_exception(TTA_FORMAT_ERROR);

            info.format = fifo.read_uint16();
            info.nch = fifo.read_uint16();
            info.bps = fifo.read_uint16();
            info.sps = fifo.read_uint32();
            info.samples = fifo.read_uint32();

            if (read_crc32())
                throw new tta_exception(TTA_FILE_ERROR);

            size += 22; // sizeof TTA header

            return size;
        } catch (IOException e) {
            throw new tta_exception(TTA_FILE_ERROR, e);
        }
    } // read_tta_header

    int skip_id3v2() {
	int size = 0;

	fifo.reader_reset();

	// id3v2 header must be at start
        byte[] header = new byte[3];
        fifo.io.readFully(header);
	if (!"ID3".equals(new String(header))) {
//			s->pos = s->buffer;
			return 0;
	}

	s->pos += 2; // skip version bytes
	if (read_byte(s) & 0x10) size += 10;

	size += (read_byte(s) & 0x7f);
	size = (size << 7) | (read_byte(s) & 0x7f);
	size = (size << 7) | (read_byte(s) & 0x7f);
	size = (size << 7) | (read_byte(s) & 0x7f);

	reader_skip_bytes(s, size);

	return (size + 10);
} // skip_id3v2

}
