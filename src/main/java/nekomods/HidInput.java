package nekomods;

import com.mojang.logging.LogUtils;
import com.sun.jna.platform.unix.LibC;
import org.slf4j.Logger;

import java.util.Arrays;

public class HidInput {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void threadFunc() {
        LOGGER.info("Deck controls thread init...");

        int fd = -1;
        boolean debug = false;

        fd = OsIo.open("/tmp/mc-deck-debug-sim", 0);
        if (fd != -1) {
            LOGGER.info("Deck controls using simulator");
            debug = true;
        } else {
            // TODO
        }

        if (fd == -1) {
            LOGGER.error("Deck controls could not open device!");
            return;
        }

        byte[] buf = new byte[64];

        while (true) {
            int ret = OsIo.read(fd, buf, 64);
            if (ret != 64) {
                LOGGER.error("Bad read length " + ret);
                continue;
            }

            LOGGER.info(Arrays.toString(buf));
        }
    }
}
