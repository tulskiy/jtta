import com.tulskiy.tta.TTADecoder;
import com.tulskiy.tta.TTA_info;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

/**
 * Author: Denis Tulskiy
 * Date: 5/30/11
 */
public class TestTTA {
    @Test
    public void testAll() {
        try {
            TTADecoder decoder = new TTADecoder(
                    new FileInputStream(
                            new File(getClass().getResource("sample.tta").toURI())));

            TTA_info info = decoder.init_get_info(0);
            assertEquals(16, info.bps);
            assertEquals(2, info.nch);
            assertEquals(29400, info.samples);
            assertEquals(44100, info.sps);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
