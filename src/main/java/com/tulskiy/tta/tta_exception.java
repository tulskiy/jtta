package com.tulskiy.tta;

/**
 * Author: Denis Tulskiy
 * Date: 5/30/11
 */
public class tta_exception extends Throwable {
    public tta_exception(TTACodecStatus ttaFormatError) {
        super("TTA Exception: " + ttaFormatError);
    }

    public tta_exception(TTACodecStatus ttaFormatError, Throwable cause) {
        super("TTA Exception: " + ttaFormatError, cause);
    }
}
