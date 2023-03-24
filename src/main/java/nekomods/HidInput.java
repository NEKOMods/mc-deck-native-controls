package nekomods;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class HidInput {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void threadFunc() {
        while (true) {
            LOGGER.info("hi from thread");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }
    }
}
