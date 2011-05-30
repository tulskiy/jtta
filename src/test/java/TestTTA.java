import com.tulskiy.tta.TTADecoder;
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

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
