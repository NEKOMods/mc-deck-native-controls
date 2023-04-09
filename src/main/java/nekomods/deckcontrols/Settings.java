package nekomods.deckcontrols;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

@Mod.EventBusSubscriber(modid = DeckControls.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Settings {
    public static final Settings CONFIG;
    public static final ForgeConfigSpec CONFIG_SPEC;

    final ForgeConfigSpec.DoubleValue gyroTightenMagVal;
    public static double GYRO_TIGHTEN_MAG = 5;
    final ForgeConfigSpec.DoubleValue mouseSmoothingThreshVal;
    public static double MOUSE_SMOOTH_THRESH = 500;
    final ForgeConfigSpec.DoubleValue mouseTightenThreshVal;
    public static double MOUSE_TIGHTEN_THRESH = 100;
    final ForgeConfigSpec.DoubleValue thumbDeadzoneVal;
    public static double THUMB_DEADZONE = 5000;
    final ForgeConfigSpec.DoubleValue thumbAnalogFullScaleVal;
    public static double THUMB_ANALOG_FULLSCALE = 32700;
    final ForgeConfigSpec.DoubleValue thumbDigitalActivateVal;
    public static double THUMB_DIGITAL_ACTIVATE = 16000;
    final ForgeConfigSpec.DoubleValue thumbDigitalDeactivateVal;
    public static double THUMB_DIGITAL_DEACTIVATE = 15000;
    final ForgeConfigSpec.DoubleValue rpadMouseScaleXCamVal;
    public static double RPAD_MOUSE_SCALE_X_CAM = 50;
    final ForgeConfigSpec.DoubleValue rpadMouseScaleYCamVal;
    public static double RPAD_MOUSE_SCALE_Y_CAM = 80;
    final ForgeConfigSpec.DoubleValue rpadMouseScaleXGuiVal;
    public static double RPAD_MOUSE_SCALE_X_GUI = 120;
    final ForgeConfigSpec.DoubleValue rpadMouseScaleYGuiVal;
    public static double RPAD_MOUSE_SCALE_Y_GUI = 120;
    final ForgeConfigSpec.DoubleValue modeSwitchBeepFreqVal;
    public static double MODE_SWITCH_BEEP_FREQ = 1000;
    final ForgeConfigSpec.DoubleValue modeSwitchBeepLenVal;
    public static double MODE_SWITCH_BEEP_LEN = 0.1;
    final ForgeConfigSpec.DoubleValue gyroCamSensXVal;
    public static double GYRO_CAM_SENSITIVITY_X = 2;
    final ForgeConfigSpec.DoubleValue gyroCamSensYVal;
    public static double GYRO_CAM_SENSITIVITY_Y = 2;
    final ForgeConfigSpec.DoubleValue gyroCamSensXScopeVal;
    public static double GYRO_CAM_SENSITIVITY_SCOPE_X = 0.5;
    final ForgeConfigSpec.DoubleValue gyroCamSensYScopeVal;
    public static double GYRO_CAM_SENSITIVITY_SCOPE_Y = 0.5;
    final ForgeConfigSpec.DoubleValue flickStickActivateVal;
    public static double FLICK_STICK_ACTIVATE_DIST = 29000;
    final ForgeConfigSpec.DoubleValue flickStickDeactivateVal;
    public static double FLICK_STICK_DEACTIVATE_DIST = 28000;
    final ForgeConfigSpec.LongValue flickStickTimeVal;
    public static long FLICK_STICK_TIME_NANOS = 100_000_000;
    final ForgeConfigSpec.DoubleValue flickStickSmoothVal;
    public static double FLICK_STICK_SMOOTH_THRESH = 0.1;
    final ForgeConfigSpec.LongValue keyRepeatActivateTimeVal;
    public static long KEY_REPEAT_ACTIVATE_TIME_NANOS = 500_000_000;
    final ForgeConfigSpec.LongValue keyRepeatRepeatTimeVal;
    public static long KEY_REPEAT_REPEAT_TIME_NANOS = 300_000_000;

    static {
        final Pair<Settings, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Settings::new);
        CONFIG = specPair.getLeft();
        CONFIG_SPEC = specPair.getRight();
    }

    @SubscribeEvent
    public static void onModConfigEvent(final ModConfigEvent configEvent) {
        if (configEvent.getConfig().getSpec() == CONFIG_SPEC) {
            GYRO_TIGHTEN_MAG = CONFIG.gyroTightenMagVal.get();
            MOUSE_SMOOTH_THRESH = CONFIG.mouseSmoothingThreshVal.get();
            MOUSE_TIGHTEN_THRESH = CONFIG.mouseTightenThreshVal.get();
            THUMB_DEADZONE = CONFIG.thumbDeadzoneVal.get();
            THUMB_ANALOG_FULLSCALE = CONFIG.thumbAnalogFullScaleVal.get();
            THUMB_DIGITAL_ACTIVATE = CONFIG.thumbDigitalActivateVal.get();
            THUMB_DIGITAL_DEACTIVATE = CONFIG.thumbDigitalDeactivateVal.get();
            RPAD_MOUSE_SCALE_X_CAM = CONFIG.rpadMouseScaleXCamVal.get();
            RPAD_MOUSE_SCALE_Y_CAM = CONFIG.rpadMouseScaleYCamVal.get();
            RPAD_MOUSE_SCALE_X_GUI = CONFIG.rpadMouseScaleXGuiVal.get();
            RPAD_MOUSE_SCALE_Y_GUI = CONFIG.rpadMouseScaleYGuiVal.get();
            MODE_SWITCH_BEEP_FREQ = CONFIG.modeSwitchBeepFreqVal.get();
            MODE_SWITCH_BEEP_LEN = CONFIG.modeSwitchBeepLenVal.get();
            GYRO_CAM_SENSITIVITY_X = CONFIG.gyroCamSensXVal.get();
            GYRO_CAM_SENSITIVITY_Y = CONFIG.gyroCamSensYVal.get();
            GYRO_CAM_SENSITIVITY_SCOPE_X = CONFIG.gyroCamSensXScopeVal.get();
            GYRO_CAM_SENSITIVITY_SCOPE_Y = CONFIG.gyroCamSensYScopeVal.get();
            FLICK_STICK_ACTIVATE_DIST = CONFIG.flickStickActivateVal.get();
            FLICK_STICK_DEACTIVATE_DIST = CONFIG.flickStickDeactivateVal.get();
            FLICK_STICK_TIME_NANOS = CONFIG.flickStickTimeVal.get();
            FLICK_STICK_SMOOTH_THRESH = CONFIG.flickStickSmoothVal.get();
            KEY_REPEAT_ACTIVATE_TIME_NANOS = CONFIG.keyRepeatActivateTimeVal.get();
            KEY_REPEAT_REPEAT_TIME_NANOS = CONFIG.keyRepeatRepeatTimeVal.get();
        }
    }

    public Settings(ForgeConfigSpec.Builder builder) {
        gyroTightenMagVal = builder
                .comment("Threshold below which gyro motion will have tightened sensitivity (°/s)")
                .defineInRange("gyroTightenMag", 5, 0, Double.POSITIVE_INFINITY);
        mouseSmoothingThreshVal = builder
                .comment("Threshold below which mouse motion will be smoothed (LSBs, [0-65535]")
                .defineInRange("mouseSmoothingThresh", 500, 0.0, 65536);
        mouseTightenThreshVal = builder
                .comment("Threshold below which mouse motion will have tightened sensitivity (LSBs, [0-65535]")
                .defineInRange("mouseTightenThresh", 100, 0.0, 65536);
        thumbDeadzoneVal = builder
                .comment("Thumbstick dead zone distance (LSBs, [0-65535])")
                .defineInRange("thumbDeadzone", 5000, 0.0, 65536);
        thumbAnalogFullScaleVal = builder
                .comment("Thumbstick full-scale value in \"analog\" situations (i.e. walking) (LSBs, [0-65535])")
                .defineInRange("thumbAnalogFullScale", 32700, 0.0, 65536);
        thumbDigitalActivateVal = builder
                .comment("Thumbstick activation threshold in \"digital\" situations (e.g. boats) (LSBs, [0-65535])")
                .defineInRange("thumbDigitalActivate", 16000, 0.0, 65536);
        thumbDigitalDeactivateVal = builder
                .comment("Thumbstick deactivation threshold in \"digital\" situations (e.g. boats) (LSBs, [0-65535])")
                .defineInRange("thumbDigitalDeactivate", 15000, 0.0, 65536);
        rpadMouseScaleXCamVal = builder
                .comment("Mouse sensitivity, camera mode, X axis (left-right), larger -> less sensitive")
                .defineInRange("rpadMouseScaleXCam", 50, Double.MIN_VALUE, Double.POSITIVE_INFINITY);
        rpadMouseScaleYCamVal = builder
                .comment("Mouse sensitivity, camera mode, Y axis (up-down), larger -> less sensitive")
                .defineInRange("rpadMouseScaleYCam", 80, Double.MIN_VALUE, Double.POSITIVE_INFINITY);
        rpadMouseScaleXGuiVal = builder
                .comment("Mouse sensitivity, GUI mode, X axis (left-right), larger -> less sensitive")
                .defineInRange("rpadMouseScaleXGui", 120, Double.MIN_VALUE, Double.POSITIVE_INFINITY);
        rpadMouseScaleYGuiVal = builder
                .comment("Mouse sensitivity, GUI mode, Y axis (up-down), larger -> less sensitive")
                .defineInRange("rpadMouseScaleYGui", 120, Double.MIN_VALUE, Double.POSITIVE_INFINITY);
        modeSwitchBeepFreqVal = builder
                .comment("Sneak toggle beep frequency (Hz)")
                .defineInRange("sneakToggleBeepFreq", 1000, 7.6, 495483);
        modeSwitchBeepLenVal = builder
                .comment("Sneak toggle beep length (s)")
                .defineInRange("sneakToggleBeepLen", 0.1, 0, 8622);
        gyroCamSensXVal = builder
                .comment("Gyro camera sensitivity, X axis (left-right), not using spyglass (°/°)")
                .defineInRange("gyroCamSensitivityX", 2, 0, Double.MAX_VALUE);
        gyroCamSensYVal = builder
                .comment("Gyro camera sensitivity, Y axis (up-down), not using spyglass (°/°)")
                .defineInRange("gyroCamSensitivityY", 2, 0, Double.MAX_VALUE);
        gyroCamSensXScopeVal = builder
                .comment("Gyro camera sensitivity, X axis (left-right), using spyglass (°/°)")
                .defineInRange("gyroCamSensitivityScopeX", 0.5, 0, Double.MAX_VALUE);
        gyroCamSensYScopeVal = builder
                .comment("Gyro camera sensitivity, Y axis (up-down), using spyglass (°/°)")
                .defineInRange("gyroCamSensitivityScopeY", 0.5, 0, Double.MAX_VALUE);
        flickStickActivateVal = builder
                .comment("Flick stick activation threshold (LSBs, [0-65535])")
                .defineInRange("flickStickActivate", 29000, 0.0, 65536);
        flickStickDeactivateVal = builder
                .comment("Flick stick deactivation threshold (LSBs, [0-65535])")
                .defineInRange("flickStickDeactivate", 28000, 0.0, 65536);
        flickStickTimeVal = builder
                .comment("Flick stick flicking time (ns)")
                .defineInRange("flickStickTime", 100_000_000, 1, Long.MAX_VALUE);
        flickStickSmoothVal = builder
                .comment("Flick stick smoothing threshold (°)")
                .defineInRange("flickStickSmoothing", 0.1, 0, 65536);
        keyRepeatActivateTimeVal = builder
                .comment("Key repeat activation time (ns)")
                .defineInRange("keyRepeatActivateTime", 500_000_000, 1, Long.MAX_VALUE);
        keyRepeatRepeatTimeVal = builder
                .comment("Key repeat repetition time (ns)")
                .defineInRange("keyRepeatRepeatTime", 300_000_000, 1, Long.MAX_VALUE);
    }
}
