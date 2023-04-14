package nekomods.deckcontrols;

import com.mojang.logging.LogUtils;
import com.sun.jna.NativeLong;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class TouchscreenInput extends Thread {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int EV_SYN = 0x00;
    private static final int EV_ABS = 0x03;

    private static final int SYN_REPORT = 0;

    private static final int ABS_MT_SLOT = 0x2f;
    private static final int ABS_MT_POSITION_X = 0x35;
    private static final int ABS_MT_POSITION_Y = 0x36;
    private static final int ABS_MT_TRACKING_ID = 0x39;

    private int fd;
    private int latest_slot;

    public static class TouchPoint implements Cloneable {
        public int tracking_id;
        public int x;
        public int y;

        public TouchPoint() {
            this.tracking_id = -1;
            this.x = 0;
            this.y = 0;
        }

        @Override
        public TouchPoint clone() {
            TouchPoint clone = new TouchPoint();
            clone.tracking_id = this.tracking_id;
            clone.x = this.x;
            clone.y = this.y;
            return clone;
        }
    }
    public static final int NUM_TOUCH_POINTS = 10;
    public TouchPoint[] touchPoints = new TouchPoint[NUM_TOUCH_POINTS];
    private TouchPoint[] newTouchPoints = new TouchPoint[NUM_TOUCH_POINTS];

    public TouchscreenInput() {
        super("Steam Deck Touchscreen Thread");
        super.setDaemon(true);

        for (int i = 0; i < NUM_TOUCH_POINTS; i++) {
            touchPoints[i] = new TouchPoint();
            newTouchPoints[i] = new TouchPoint();
        }

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
        if (fd == -1) return;

        LOGGER.debug("found touchscreen, fd " + fd);

        byte[] slot_input_absinfo = new byte[24];
        // EVIOCGABS
        int ret = LibcIo.ioctl(fd, 0x80184540 + ABS_MT_SLOT, slot_input_absinfo);
        if (ret < 0) {
            LOGGER.warn("could not EVIOCGABS(ABS_MT_SLOT)");
            return;
        }

        latest_slot = ByteBuffer.wrap(slot_input_absinfo).order(ByteOrder.nativeOrder()).getInt();
        LOGGER.debug("value of ABS_MT_SLOT is " + latest_slot);
    }

    @Override
    public void run() {
        LOGGER.info("Deck controls touchscreen thread init...");

        if (fd == -1) return;

        while (true) {
            // FIXME verify if 32-bit still works
            byte[] event_bytes;
            if (Minecraft.getInstance().is64Bit()) {
                event_bytes = new byte[24];
            } else {
                event_bytes = new byte[16];
            }
            int ret = LibcIo.read(fd, event_bytes, event_bytes.length);
            if (ret != event_bytes.length) {
                LOGGER.warn("Touchscreen evdev expected " + event_bytes.length + " bytes, got " + ret + " bytes");
                continue;
            }

            ByteBuffer event_buf = ByteBuffer.wrap(event_bytes).order(ByteOrder.nativeOrder());
            long tv_sec;
            long tv_usec;
            if (Minecraft.getInstance().is64Bit()) {
                tv_sec = event_buf.getLong();
                tv_usec = event_buf.getLong();
            } else {
                tv_sec = event_buf.getInt();
                tv_usec = event_buf.getInt();
            }
            int type = event_buf.getShort();
            int code = event_buf.getShort();
            int value = event_buf.getInt();

            if (type == EV_ABS && code == ABS_MT_SLOT) {
                latest_slot = value;
            }
            if (type == EV_ABS && code == ABS_MT_TRACKING_ID) {
                newTouchPoints[latest_slot].tracking_id = value;
            }
            if (type == EV_ABS && code == ABS_MT_POSITION_X) {
                newTouchPoints[latest_slot].x = value;
            }
            if (type == EV_ABS && code == ABS_MT_POSITION_Y) {
                newTouchPoints[latest_slot].y = value;
            }

            if (type == EV_SYN && code == SYN_REPORT) {
                // FIXME this might not be GC efficient
                TouchPoint[] clonedTouchPoints = new TouchPoint[NUM_TOUCH_POINTS];
                for (int i = 0; i < NUM_TOUCH_POINTS; i++)
                    clonedTouchPoints[i] = newTouchPoints[i].clone();
                touchPoints = clonedTouchPoints;
            }
        }
    }

    public static void touchGrab() {
        LOGGER.debug("grab touchscreen");

        if (DeckControls.TOUCH_INPUT != null) {
            // EVIOCGRAB
            LibcIo.ioctl(DeckControls.TOUCH_INPUT.fd, 0x40044590, new NativeLong(1));
        }
    }

    public static void touchUngrab() {
        LOGGER.debug("ungrab touchscreen");

        if (DeckControls.TOUCH_INPUT != null) {
            // EVIOCGRAB
            LibcIo.ioctl(DeckControls.TOUCH_INPUT.fd, 0x40044590, new NativeLong(0));
        }
    }
}
