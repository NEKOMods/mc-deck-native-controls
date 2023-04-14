package nekomods.deckcontrols;

import com.mojang.logging.LogUtils;
import com.sun.jna.NativeLong;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class TouchscreenInput {
    private static final Logger LOGGER = LogUtils.getLogger();

    private int fd;

    public TouchscreenInput() {
        AtomicInteger touch_fd = new AtomicInteger(-1);

        try {
            Files.list(Paths.get("/dev/input")).forEach((p) -> {
                if (p.getFileName().toString().startsWith("event")) {
                    int this_fd = LibcIo.open(p.toString(), 0);
                    if (this_fd == -1) return;

                    LOGGER.debug("Checking input device " + p);

                    byte[] name = new byte[64];
                    // EVIOCGNAME
                    int actual_len = LibcIo.ioctl(this_fd, 0x80004506 | (name.length << 16), name);
                    if (actual_len < 0) {
                        LibcIo.close(this_fd);
                        return;
                    }

                    String name_str = new String(Arrays.copyOfRange(name, 0, actual_len - 1), StandardCharsets.UTF_8);
                    LOGGER.debug("... it is a " + name_str);

                    if (!name_str.equals("FTS3528:00 2808:1015")) {
                        LibcIo.close(this_fd);
                        return;
                    }

                    touch_fd.set(this_fd);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Couldn't open touchscreen", e);
        }

        fd = touch_fd.get();
        if (fd != -1) {
            LOGGER.debug("found touchscreen, fd " + fd);
        }
    }

    public static void touchRemoveFromCursor() {
        LOGGER.debug("remove touchscreen from cursor");

        if (DeckControls.TOUCH_INPUT != null) {
            // EVIOCGRAB
            LibcIo.ioctl(DeckControls.TOUCH_INPUT.fd, 0x40044590, new NativeLong(1));
        }
    }

    public static void touchReturnToCursor() {
        LOGGER.debug("return touchscreen back to cursor");

        if (DeckControls.TOUCH_INPUT != null) {
            // EVIOCGRAB
            LibcIo.ioctl(DeckControls.TOUCH_INPUT.fd, 0x40044590, new NativeLong(0));
        }
    }
}
