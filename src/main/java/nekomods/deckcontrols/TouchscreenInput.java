package nekomods.deckcontrols;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class TouchscreenInput {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void touchRemoveFromCursor() {
        LOGGER.debug("remove touchscreen from cursor");
    }

    public static void touchReturnToCursor() {
        LOGGER.debug("return touchscreen back to cursor");
    }
}
