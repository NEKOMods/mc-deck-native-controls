package nekomods.deckcontrols;

import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static nekomods.deckcontrols.Settings.*;

public class HidInput extends Thread {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static class GamepadButtons {
        public static int BTN_D_UP      = 1 << 0;
        public static int BTN_D_DOWN    = 1 << 1;
        public static int BTN_D_LEFT    = 1 << 2;
        public static int BTN_D_RIGHT   = 1 << 3;
        public static int BTN_A      = 1 << 4;
        public static int BTN_B    = 1 << 5;
        public static int BTN_X    = 1 << 6;
        public static int BTN_Y   = 1 << 7;
        public static int BTN_L4   = 1 << 8;
        public static int BTN_L5   = 1 << 9;
        public static int BTN_R4   = 1 << 10;
        public static int BTN_R5   = 1 << 11;
        public static int BTN_VIEW   = 1 << 12;
        public static int BTN_OPTIONS   = 1 << 13;
        public static int BTN_STEAM   = 1 << 14;
        public static int BTN_DOTS   = 1 << 15;
        public static int BTN_LTHUMB_TOUCH = 1 << 16;
        public static int BTN_RTHUMB_TOUCH = 1 << 17;
        public static int BTN_LPAD_TOUCH = 1 << 18;
        public static int BTN_RPAD_TOUCH = 1 << 19;
        public static int BTN_LTHUMB_CLICK = 1 << 20;
        public static int BTN_RTHUMB_CLICK = 1 << 21;
        public static int BTN_LPAD_CLICK = 1 << 22;
        public static int BTN_RPAD_CLICK = 1 << 23;
        public static int BTN_LT_DIGITAL = 1 << 24;
        public static int BTN_RT_DIGITAL = 1 << 25;
        public static int BTN_LT_ANALOG_FULL = 1 << 26;
        public static int BTN_RT_ANALOG_FULL = 1 << 27;

        public static int FLAG_BTN_UP = 1 << 30;
        public static int FLAG_BARRIER = 1 << 31;
    }

    public static class AccumHidState {
        // degrees, CW
        public double camYaw;
        // degrees, + is up
        public double camPitch;
        public double mouseDX;
        public double mouseDY;
    }

    public static class OtherHidState {
        public int buttons;
        public short lpad_x;
        public short lpad_y;
        public short rpad_x;
        public short rpad_y;
        public short lthumb_x;
        public short lthumb_y;
        public short rthumb_x;
        public short rthumb_y;
        public int ltrig;
        public int rtrig;

        public int frame;
        public int lpad_force;
        public int rpad_force;
        public short lthumb_capa;
        public short rthumb_capa;

        public short accel_x;
        public short accel_y;
        public short accel_z;
        public short gyro_pitch;
        public short gyro_roll;
        public short gyro_yaw;
        public short pose_quat_w;
        public short pose_quat_x;
        public short pose_quat_y;
        public short pose_quat_z;
    }

    public HidInput() {
        super("Steam Deck Input Thread");
        super.setDaemon(true);
    }

    public final ConcurrentLinkedDeque<Integer> keyEvents = new ConcurrentLinkedDeque();
    public OtherHidState latestInput = new OtherHidState();
    public AtomicReference<AccumHidState> accumInput = new AtomicReference(new AccumHidState());
    public int missedFrames = 0;
    public long[] frameTimes = new long[250];

    // SteamDeckGyroDSU claims 16, but a random Invensense datasheet for ICM-42605 (probably not the right part)
    // implies 16.4 which might be more correct.
    private static final double GYRO_LSBS_PER_DEGREE = 16.4;

    private int fd = -1;
    boolean debug;

    private static Map<String, String> parse_uevent(String s) {
        return Arrays.stream(s.split("\n"))
            .filter((x) -> x.trim().length() > 0)
            .map((line) -> line.split("=", 2))
            .collect(Collectors.toMap((entry) -> entry[0], (entry) -> entry[1]));
    }

    public void run() {
        LOGGER.info("Deck controls thread init...");

        fd = OsIo.open("/tmp/mc-deck-debug-sim", 0);
        if (fd != -1) {
            LOGGER.info("Deck controls using simulator");
            debug = true;
        } else {
            try {
                Path[] fns = Files.list(Paths.get("/sys/class/hidraw")).toArray(Path[]::new);
                for (Path f : fns) {
                    Path hidraw_uevent_fn = f.resolve("uevent");
                    Path hiddev_uevent_fn = f.resolve("device/uevent");

                    String hidraw_uevent = Files.readString(hidraw_uevent_fn);
                    String hiddev_uevent = Files.readString(hiddev_uevent_fn);

                    Map<String, String> hidraw_uevent_data = parse_uevent(hidraw_uevent);
                    Map<String, String> hiddev_uevent_data = parse_uevent(hiddev_uevent);

                    LOGGER.info(hidraw_uevent_data.get("MODALIAS"));

                    String modalias = hiddev_uevent_data.get("MODALIAS");
                    if (modalias.equals("hid:b0003g0103v000028DEp00001205")) {
                        String devname = hidraw_uevent_data.get("DEVNAME");
                        String devname_full_path = "/dev/" + devname;

                        LOGGER.info("Deck controls are at " + devname_full_path);

                        fd = OsIo.open(devname_full_path, 0);
                        break;
                    }
                }
            }
            catch (Exception e) {
                LOGGER.error("Deck couldn't open hidraw", e);
                return;
            }
        }

        if (fd == -1) {
            LOGGER.error("Deck controls could not open device!");
            return;
        }

        byte[] buf = new byte[64];
        int lastPressedButtons = 0;
        long last_frame_id = -1;
        long last_frame_nanos = -1;
        int frame_times_i = 0;
        // mouse smoothing
        int last_mouse_pad_x = 0;
        int last_mouse_pad_y = 0;
        boolean last_mouse_pad_valid = false;
        double[] pad_mouse_smoothing_x = new double[32];
        double[] pad_mouse_smoothing_y = new double[32];
        int pad_mouse_smoothing_i = 0;

        while (true) {
            int ret = OsIo.read(fd, buf, 64);
            if (ret != 64) {
                LOGGER.error("Bad read length " + ret);
                continue;
            }

            if (buf[0] != 0x01 || buf[1] != 0x00 || buf[2] != 0x09 || buf[3] != 0x40) {
                LOGGER.error("Bad frame " + Arrays.toString(buf));
                continue;
            }

            long current_nanos = Util.getNanos();

            long frame_id = (buf[4] & 0xFF) | ((buf[5] & 0xFF) << 8) | ((buf[6] & 0xFF) << 16) | ((buf[7] & 0xFF) << 24);
            if (last_frame_id != -1) {
                if (last_frame_id - frame_id > 1) {
                    long missed_frames_now = last_frame_id - frame_id - 1;
                    LOGGER.warn("Dropped " + missed_frames_now + " deck HID frames");
                    missedFrames += missed_frames_now;
                }

                frameTimes[frame_times_i] = current_nanos - last_frame_nanos;
                frame_times_i = (frame_times_i + 1) % frameTimes.length;
            }

            OtherHidState newState = new OtherHidState();
            int currentPressedButtons = 0;
            if ((buf[8] & 0x01) != 0)
                currentPressedButtons |= GamepadButtons.BTN_RT_ANALOG_FULL;
            if ((buf[8] & 0x02) != 0)
                currentPressedButtons |= GamepadButtons.BTN_LT_ANALOG_FULL;
            if ((buf[8] & 0x04) != 0)
                currentPressedButtons |= GamepadButtons.BTN_RT_DIGITAL;
            if ((buf[8] & 0x08) != 0)
                currentPressedButtons |= GamepadButtons.BTN_LT_DIGITAL;
            if ((buf[8] & 0x10) != 0)
                currentPressedButtons |= GamepadButtons.BTN_Y;
            if ((buf[8] & 0x20) != 0)
                currentPressedButtons |= GamepadButtons.BTN_B;
            if ((buf[8] & 0x40) != 0)
                currentPressedButtons |= GamepadButtons.BTN_X;
            if ((buf[8] & 0x80) != 0)
                currentPressedButtons |= GamepadButtons.BTN_A;
            if ((buf[9] & 0x01) != 0)
                currentPressedButtons |= GamepadButtons.BTN_D_UP;
            if ((buf[9] & 0x02) != 0)
                currentPressedButtons |= GamepadButtons.BTN_D_RIGHT;
            if ((buf[9] & 0x04) != 0)
                currentPressedButtons |= GamepadButtons.BTN_D_LEFT;
            if ((buf[9] & 0x08) != 0)
                currentPressedButtons |= GamepadButtons.BTN_D_DOWN;
            if ((buf[9] & 0x10) != 0)
                currentPressedButtons |= GamepadButtons.BTN_VIEW;
            if ((buf[9] & 0x20) != 0)
                currentPressedButtons |= GamepadButtons.BTN_STEAM;
            if ((buf[9] & 0x40) != 0)
                currentPressedButtons |= GamepadButtons.BTN_OPTIONS;
            if ((buf[9] & 0x80) != 0)
                currentPressedButtons |= GamepadButtons.BTN_L5;
            if ((buf[10] & 0x01) != 0)
                currentPressedButtons |= GamepadButtons.BTN_R5;
            if ((buf[10] & 0x02) != 0)
                currentPressedButtons |= GamepadButtons.BTN_LPAD_CLICK;
            if ((buf[10] & 0x04) != 0)
                currentPressedButtons |= GamepadButtons.BTN_RPAD_CLICK;
            if ((buf[10] & 0x08) != 0)
                currentPressedButtons |= GamepadButtons.BTN_LPAD_TOUCH;
            if ((buf[10] & 0x10) != 0)
                currentPressedButtons |= GamepadButtons.BTN_RPAD_TOUCH;
            if ((buf[10] & 0x40) != 0)
                currentPressedButtons |= GamepadButtons.BTN_LTHUMB_CLICK;
            if ((buf[11] & 0x04) != 0)
                currentPressedButtons |= GamepadButtons.BTN_RTHUMB_CLICK;
            if ((buf[13] & 0x02) != 0)
                currentPressedButtons |= GamepadButtons.BTN_L4;
            if ((buf[13] & 0x04) != 0)
                currentPressedButtons |= GamepadButtons.BTN_R4;
            if ((buf[13] & 0x40) != 0)
                currentPressedButtons |= GamepadButtons.BTN_LTHUMB_TOUCH;
            if ((buf[13] & 0x80) != 0)
                currentPressedButtons |= GamepadButtons.BTN_RTHUMB_TOUCH;
            if ((buf[14] & 0x04) != 0)
                currentPressedButtons |= GamepadButtons.BTN_DOTS;
            newState.buttons = currentPressedButtons;
            newState.frame = (buf[4] & 0xFF) | ((buf[5] & 0xFF) << 8) | ((buf[6] & 0xFF) << 16) | ((buf[7] & 0xFF) << 24);
            newState.lpad_x = (short)((buf[16] & 0xFF) | ((buf[17] & 0xFF) << 8));
            newState.lpad_y = (short)((buf[18] & 0xFF) | ((buf[19] & 0xFF) << 8));
            newState.rpad_x = (short)((buf[20] & 0xFF) | ((buf[21] & 0xFF) << 8));
            newState.rpad_y = (short)((buf[22] & 0xFF) | ((buf[23] & 0xFF) << 8));
            newState.accel_x = (short)((buf[24] & 0xFF) | ((buf[25] & 0xFF) << 8));
            newState.accel_y = (short)((buf[26] & 0xFF) | ((buf[27] & 0xFF) << 8));
            newState.accel_z = (short)((buf[28] & 0xFF) | ((buf[29] & 0xFF) << 8));
            newState.gyro_pitch = (short)((buf[30] & 0xFF) | ((buf[31] & 0xFF) << 8));
            newState.gyro_roll = (short)((buf[32] & 0xFF) | ((buf[33] & 0xFF) << 8));
            newState.gyro_yaw = (short)((buf[34] & 0xFF) | ((buf[35] & 0xFF) << 8));
            newState.pose_quat_w = (short)((buf[36] & 0xFF) | ((buf[37] & 0xFF) << 8));
            newState.pose_quat_x = (short)((buf[38] & 0xFF) | ((buf[39] & 0xFF) << 8));
            newState.pose_quat_y = (short)((buf[40] & 0xFF) | ((buf[41] & 0xFF) << 8));
            newState.pose_quat_z = (short)((buf[42] & 0xFF) | ((buf[43] & 0xFF) << 8));
            newState.ltrig = (buf[44] & 0xFF) | ((buf[45] & 0xFF) << 8);
            newState.rtrig = (buf[46] & 0xFF) | ((buf[47] & 0xFF) << 8);
            newState.lthumb_x = (short)((buf[48] & 0xFF) | ((buf[49] & 0xFF) << 8));
            newState.lthumb_y = (short)((buf[50] & 0xFF) | ((buf[51] & 0xFF) << 8));
            newState.rthumb_x = (short)((buf[52] & 0xFF) | ((buf[53] & 0xFF) << 8));
            newState.rthumb_y = (short)((buf[54] & 0xFF) | ((buf[55] & 0xFF) << 8));
            newState.lpad_force = (buf[56] & 0xFF) | ((buf[57] & 0xFF) << 8);
            newState.rpad_force = (buf[58] & 0xFF) | ((buf[59] & 0xFF) << 8);
            newState.lthumb_capa = (short)((buf[60] & 0xFF) | ((buf[61] & 0xFF) << 8));
            newState.rthumb_capa = (short)((buf[62] & 0xFF) | ((buf[63] & 0xFF) << 8));
            latestInput = newState;

            // gyro accum
            double delta_seconds = (current_nanos - last_frame_nanos) / 1e9;
            double pitch_deg_per_s = newState.gyro_pitch / GYRO_LSBS_PER_DEGREE;
            double yaw_deg_per_s = newState.gyro_roll / GYRO_LSBS_PER_DEGREE;
            double gyro_mag = Math.sqrt(pitch_deg_per_s * pitch_deg_per_s + yaw_deg_per_s * yaw_deg_per_s);
            if (gyro_mag < GYRO_TIGHTEN_MAG) {
                pitch_deg_per_s *= gyro_mag / GYRO_TIGHTEN_MAG;
                yaw_deg_per_s *= gyro_mag / GYRO_TIGHTEN_MAG;
            }
            double pitch_delta_deg = pitch_deg_per_s * delta_seconds;
            double yaw_delta_deg = yaw_deg_per_s * delta_seconds;

            // mouse accum
            int mouse_pad_touch;
            short mouse_pad_x;
            short mouse_pad_y;
            if (!SWAP_PADS) {
                mouse_pad_touch = HidInput.GamepadButtons.BTN_RPAD_TOUCH;
                mouse_pad_x = newState.rpad_x;
                mouse_pad_y = newState.rpad_y;
            } else {
                mouse_pad_touch = HidInput.GamepadButtons.BTN_LPAD_TOUCH;
                mouse_pad_x = newState.lpad_x;
                mouse_pad_y = newState.lpad_y;
            }

            double mouse_final_dx = 0;
            double mouse_final_dy = 0;
            if ((currentPressedButtons & mouse_pad_touch) != 0) {
                if (last_mouse_pad_valid) {
                    int pad_dx = mouse_pad_x - last_mouse_pad_x;
                    int pad_dy = mouse_pad_y - last_mouse_pad_y;

                    double tier_smooth_thresh_1 = Settings.MOUSE_SMOOTH_THRESH / 2;
                    double tier_smooth_thresh_2 = Settings.MOUSE_SMOOTH_THRESH;

                    double dx_mag = Math.abs(pad_dx);
                    double dy_mag = Math.abs(pad_dy);

                    double smooth_direct_weight_x = (dx_mag - tier_smooth_thresh_1) / (tier_smooth_thresh_2 - tier_smooth_thresh_1);
                    if (smooth_direct_weight_x < 0) smooth_direct_weight_x = 0;
                    if (smooth_direct_weight_x > 1) smooth_direct_weight_x = 1;
                    double smooth_direct_weight_y = (dy_mag - tier_smooth_thresh_1) / (tier_smooth_thresh_2 - tier_smooth_thresh_1);
                    if (smooth_direct_weight_y < 0) smooth_direct_weight_y = 0;
                    if (smooth_direct_weight_y > 1) smooth_direct_weight_y = 1;

                    double mouse_dx_direct = pad_dx * smooth_direct_weight_x;
                    double mouse_dy_direct = pad_dy * smooth_direct_weight_y;
                    double mouse_dx_to_smooth = pad_dx * (1 - smooth_direct_weight_x);
                    double mouse_dy_to_smooth = pad_dy * (1 - smooth_direct_weight_y);
                    pad_mouse_smoothing_x[pad_mouse_smoothing_i] = mouse_dx_to_smooth;
                    pad_mouse_smoothing_y[pad_mouse_smoothing_i] = mouse_dy_to_smooth;
                    pad_mouse_smoothing_i = (pad_mouse_smoothing_i + 1) % pad_mouse_smoothing_x.length;

                    double mouse_smoothed_dx = mouse_dx_direct + Arrays.stream(pad_mouse_smoothing_x).average().orElse(0);
                    double mouse_smoothed_dy = mouse_dy_direct + Arrays.stream(pad_mouse_smoothing_y).average().orElse(0);

                    mouse_final_dx = mouse_smoothed_dx;
                    mouse_final_dy = mouse_smoothed_dy;
                    if (Math.abs(mouse_smoothed_dx) < MOUSE_TIGHTEN_THRESH)
                        mouse_final_dx *= Math.abs(mouse_smoothed_dx) / MOUSE_TIGHTEN_THRESH;
                    if (Math.abs(mouse_smoothed_dy) < MOUSE_TIGHTEN_THRESH)
                        mouse_final_dy *= Math.abs(mouse_smoothed_dy) / MOUSE_TIGHTEN_THRESH;
                }
                last_mouse_pad_x = mouse_pad_x;
                last_mouse_pad_y = mouse_pad_y;
                last_mouse_pad_valid = true;
            } else {
                last_mouse_pad_valid = false;
            }

            // big CAS
            while (true) {
                AccumHidState old_accum = this.accumInput.get();

                AccumHidState new_accum = new AccumHidState();
                new_accum.camPitch = old_accum.camPitch + pitch_delta_deg;
                new_accum.camYaw = old_accum.camYaw + yaw_delta_deg;
                new_accum.mouseDX = old_accum.mouseDX + mouse_final_dx;
                new_accum.mouseDY = old_accum.mouseDY + mouse_final_dy;

                if (this.accumInput.compareAndSet(old_accum, new_accum))
                    break;
            }

            last_frame_id = frame_id;
            last_frame_nanos = current_nanos;

            int newPressedButtons = currentPressedButtons & (~lastPressedButtons);
            int releasedButtons = lastPressedButtons & (~currentPressedButtons);
            if (newPressedButtons != 0)
                keyEvents.addLast(newPressedButtons);
            if (releasedButtons != 0)
                keyEvents.addLast(releasedButtons | GamepadButtons.FLAG_BTN_UP);
            lastPressedButtons = currentPressedButtons;
        }
    }

    public boolean beep(double freq, double seconds) {
        long period = Math.round(495483.0 / freq);
        long repeats = Math.round(freq * seconds * 0.5);

        if (period > 0xFFFF) {
            LOGGER.error("Period out of range: " + period);
            return false;
        }
        if (repeats > 0x7FFF) {
            LOGGER.error("Repeats out of range: " + repeats);
            return false;
        }

        if (fd < 0) return false;

        byte[] buf = new byte[65];
        buf[0] = 0x00;
        buf[1] = (byte)0x8f;
        buf[2] = 0x07;
        buf[3] = 0x02;
        buf[4] = (byte)(period & 0xFF);
        buf[5] = (byte)((period >> 8) & 0xFF);
        buf[6] = (byte)(period & 0xFF);
        buf[7] = (byte)((period >> 8) & 0xFF);
        buf[8] = (byte)(repeats & 0xFF);
        buf[9] = (byte)((repeats >> 8) & 0xFF);

        return OsIo.ioctl(fd, 0xC0414806, buf) == 0;
    }

    public boolean tick(boolean leftRight) {
        // FIXME: hardcoded to be "high" intensity, and 7db other/global intensity setting
        if (fd < 0) return false;

        byte[] buf = new byte[65];
        buf[0] = 0x00;
        buf[1] = (byte)0xea;
        buf[2] = 0x06;
        if (leftRight == false)
            buf[3] = 0x00;  // left
        else
            buf[3] = 0x01;  // right
        buf[4] = 0x01;  // "tick" command
        buf[5] = 0x03;  // low/mid/high in controller haptics intensity
        buf[6] = 0x03;  // -24 to 6, other/global intensity
        // steam sends more bytes, but afaict the firmware doesn't read them

        return OsIo.ioctl(fd, 0xC0414806, buf) == 0;
    }
}
