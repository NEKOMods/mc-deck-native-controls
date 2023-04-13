package nekomods.deckcontrols;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = DeckControls.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Settings {
    private static final Logger LOGGER = LogUtils.getLogger();

    public enum ButtonMappingType {
        SIMPLE,
        DISABLE_GYRO,
        TOGGLE_KEYBOARD,
        SCROLL_UP,
        SCROLL_DOWN,
        TOGGLE_SNEAK,
        HOLD_SNEAK,
    }

    public enum ButtonMappingContext {
        UNIVERSAL,
        IN_GAME,
        GUI;

        public IKeyConflictContext toIKCC() {
            return switch(this) {
                case UNIVERSAL -> KeyConflictContext.UNIVERSAL;
                case IN_GAME -> KeyConflictContext.IN_GAME;
                case GUI -> KeyConflictContext.GUI;
            };
        }
    }

    public enum GamepadButtons {
        DPAD_UP,
        DPAD_DOWN,
        DPAD_LEFT,
        DPAD_RIGHT,
        A,
        B,
        X,
        Y,
        L4,
        L5,
        R4,
        R5,
        VIEW,
        OPTIONS,
        // don't allow binding steam/dots, nor pads
        LEFT_THUMB_TOUCH,
        RIGHT_THUMB_TOUCH,
        LEFT_THUMB_CLICK,
        RIGHT_THUMB_CLICK,
        LB,
        RB,
        LT,
        RT;

        public int toBitfield() {
            return switch(this) {
                case DPAD_UP -> HidInput.GamepadButtons.BTN_D_UP;
                case DPAD_DOWN -> HidInput.GamepadButtons.BTN_D_DOWN;
                case DPAD_LEFT -> HidInput.GamepadButtons.BTN_D_LEFT;
                case DPAD_RIGHT -> HidInput.GamepadButtons.BTN_D_RIGHT;
                case A -> HidInput.GamepadButtons.BTN_A;
                case B -> HidInput.GamepadButtons.BTN_B;
                case X -> HidInput.GamepadButtons.BTN_X;
                case Y -> HidInput.GamepadButtons.BTN_Y;
                case L4 -> HidInput.GamepadButtons.BTN_L4;
                case L5 -> HidInput.GamepadButtons.BTN_L5;
                case R4 -> HidInput.GamepadButtons.BTN_R4;
                case R5 -> HidInput.GamepadButtons.BTN_R5;
                case VIEW -> HidInput.GamepadButtons.BTN_VIEW;
                case OPTIONS -> HidInput.GamepadButtons.BTN_OPTIONS;
                case LEFT_THUMB_TOUCH -> HidInput.GamepadButtons.BTN_LTHUMB_TOUCH;
                case RIGHT_THUMB_TOUCH -> HidInput.GamepadButtons.BTN_RTHUMB_TOUCH;
                case LEFT_THUMB_CLICK -> HidInput.GamepadButtons.BTN_LTHUMB_CLICK;
                case RIGHT_THUMB_CLICK -> HidInput.GamepadButtons.BTN_RTHUMB_CLICK;
                case LB -> HidInput.GamepadButtons.BTN_LT_DIGITAL;
                case RB -> HidInput.GamepadButtons.BTN_RT_DIGITAL;
                case LT -> HidInput.GamepadButtons.BTN_LT_ANALOG_FULL;
                case RT -> HidInput.GamepadButtons.BTN_RT_ANALOG_FULL;
            };
        }
    }

    public static class ButtonMappingConfig {
        public ButtonMappingType type;
        public ButtonMappingContext context;
        public GamepadButtons gamepadButton;
        public String binding;
        public boolean pressAgainWhenActivated;

        public ButtonMappingConfig() {
            this.type = ButtonMappingType.SIMPLE;
            this.context = ButtonMappingContext.UNIVERSAL;
            this.gamepadButton = GamepadButtons.A;
            this.binding = "";
            this.pressAgainWhenActivated = false;
        }

        public ButtonMappingConfig setType(ButtonMappingType type) {
            this.type = type;
            return this;
        }

        public ButtonMappingConfig setContext(ButtonMappingContext context) {
            this.context = context;
            return this;
        }

        public ButtonMappingConfig setGamepadButton(GamepadButtons gamepadButton) {
            this.gamepadButton = gamepadButton;
            return this;
        }

        public ButtonMappingConfig setBinding(String binding) {
            this.binding = binding;
            return this;
        }

        public ButtonMappingConfig setPressAgainWhenActivated(boolean pressAgainWhenActivated) {
            this.pressAgainWhenActivated = pressAgainWhenActivated;
            return this;
        }

        public static boolean validate(Object config_) {
            Config config = (Config)config_;
            if (config.getEnumOrElse("type", ButtonMappingType.SIMPLE) == ButtonMappingType.SIMPLE) {
                String binding = config.getOrElse("binding", "");

                if (binding.startsWith("keymapping.")) {
                    String keymapping = binding.substring(11);

                    boolean found = false;
                    for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
                        if (mapping.getName().equals(keymapping)) {
                            found = true;
                            break;
                        }
                    }
                    return found;
                } else if (binding.startsWith("inputconstant.")) {
                    String inputconstant = binding.substring(14);
                    try {
                        InputConstants.Key _dummy = InputConstants.getKey(inputconstant);
                        return true;
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            // guess it's fine :shrug:
            return true;
        }
    }

    static final ObjectConverter objectConverter = new ObjectConverter();
    static final Config[] defaultMappings = new Config[] {
            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SIMPLE)
                    .setGamepadButton(GamepadButtons.A)
                    .setBinding("keymapping.key.attack")
                    .setContext(ButtonMappingContext.IN_GAME), Config::inMemory),
            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SIMPLE)
                    .setGamepadButton(GamepadButtons.B)
                    .setBinding("keymapping.key.jump")
                    .setContext(ButtonMappingContext.IN_GAME), Config::inMemory),
            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SIMPLE)
                    .setGamepadButton(GamepadButtons.Y)
                    .setBinding("keymapping.key.sprint")
                    .setContext(ButtonMappingContext.IN_GAME), Config::inMemory),

            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SIMPLE)
                    .setGamepadButton(GamepadButtons.RT)
                    .setBinding("keymapping.key.use")
                    .setContext(ButtonMappingContext.IN_GAME), Config::inMemory),
            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SIMPLE)
                    .setGamepadButton(GamepadButtons.RB)
                    .setBinding("keymapping.key.pickItem")
                    .setContext(ButtonMappingContext.IN_GAME), Config::inMemory),
            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SIMPLE)
                    .setGamepadButton(GamepadButtons.RT)
                    .setBinding("inputconstant.key.mouse.right")
                    .setContext(ButtonMappingContext.GUI), Config::inMemory),
            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SIMPLE)
                    .setGamepadButton(GamepadButtons.RB)
                    .setBinding("inputconstant.key.mouse.middle")
                    .setContext(ButtonMappingContext.GUI), Config::inMemory),

            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SIMPLE)
                    .setGamepadButton(GamepadButtons.DPAD_UP)
                    .setBinding("keymapping.key.swapOffhand")
                    .setContext(ButtonMappingContext.IN_GAME), Config::inMemory),
            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SIMPLE)
                    .setGamepadButton(GamepadButtons.DPAD_DOWN)
                    .setBinding("keymapping.key.drop")
                    .setContext(ButtonMappingContext.IN_GAME), Config::inMemory),
            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SIMPLE)
                    .setGamepadButton(GamepadButtons.DPAD_LEFT)
                    .setBinding("keymapping.key.swapOffhand")
                    .setContext(ButtonMappingContext.GUI), Config::inMemory),
            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SIMPLE)
                    .setGamepadButton(GamepadButtons.DPAD_RIGHT)
                    .setBinding("keymapping.key.drop")
                    .setContext(ButtonMappingContext.GUI), Config::inMemory),

            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SIMPLE)
                    .setGamepadButton(GamepadButtons.OPTIONS)
                    .setBinding("inputconstant.key.keyboard.escape"), Config::inMemory),
            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SIMPLE)
                    .setGamepadButton(GamepadButtons.A)
                    .setBinding("inputconstant.key.keyboard.escape")
                    .setContext(ButtonMappingContext.GUI), Config::inMemory),

            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SIMPLE)
                    .setGamepadButton(GamepadButtons.VIEW)
                    .setBinding("keymapping.key.inventory")
                    .setContext(ButtonMappingContext.IN_GAME), Config::inMemory),
            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SIMPLE)
                    .setGamepadButton(GamepadButtons.VIEW)
                    .setBinding("inputconstant.key.keyboard.escape")
                    .setContext(ButtonMappingContext.GUI), Config::inMemory),

            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SIMPLE)
                    .setGamepadButton(GamepadButtons.L4)
                    .setBinding("inputconstant.key.keyboard.left.control"), Config::inMemory),
            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SIMPLE)
                    .setGamepadButton(GamepadButtons.L5)
                    .setBinding("inputconstant.key.keyboard.left.alt"), Config::inMemory),
            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SIMPLE)
                    .setGamepadButton(GamepadButtons.R5)
                    .setBinding("inputconstant.key.keyboard.left.shift"), Config::inMemory),

            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.TOGGLE_KEYBOARD)
                    .setGamepadButton(GamepadButtons.R4)
                    .setBinding("TOGGLE_KEYBOARD"), Config::inMemory),

            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.DISABLE_GYRO)
                    .setGamepadButton(GamepadButtons.X)
                    .setBinding("DISABLE_GYRO")
                    .setPressAgainWhenActivated(true), Config::inMemory),
            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SIMPLE)
                    .setGamepadButton(GamepadButtons.X)
                    .setBinding("inputconstant.key.mouse.left")
                    .setContext(ButtonMappingContext.GUI), Config::inMemory),

            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SCROLL_UP)
                    .setGamepadButton(GamepadButtons.DPAD_UP)
                    .setBinding("SCROLL_UP")
                    .setContext(ButtonMappingContext.GUI)
                    .setPressAgainWhenActivated(true), Config::inMemory),
            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SCROLL_UP)
                    .setGamepadButton(GamepadButtons.DPAD_LEFT)
                    .setBinding("SCROLL_UP")
                    .setContext(ButtonMappingContext.IN_GAME)
                    .setPressAgainWhenActivated(true), Config::inMemory),
            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SCROLL_DOWN)
                    .setGamepadButton(GamepadButtons.DPAD_DOWN)
                    .setBinding("SCROLL_DOWN")
                    .setContext(ButtonMappingContext.GUI)
                    .setPressAgainWhenActivated(true), Config::inMemory),
            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SCROLL_DOWN)
                    .setGamepadButton(GamepadButtons.DPAD_RIGHT)
                    .setBinding("SCROLL_DOWN")
                    .setContext(ButtonMappingContext.IN_GAME)
                    .setPressAgainWhenActivated(true), Config::inMemory),

            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.SIMPLE)
                    .setGamepadButton(GamepadButtons.LT)
                    .setBinding("inputconstant.key.keyboard.left.shift")
                    .setContext(ButtonMappingContext.GUI)
                    .setPressAgainWhenActivated(true), Config::inMemory),

            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.HOLD_SNEAK)
                    .setGamepadButton(GamepadButtons.LT)
                    .setBinding("HOLD_SNEAK")
                    .setContext(ButtonMappingContext.IN_GAME)
                    .setPressAgainWhenActivated(true), Config::inMemory),
            objectConverter.toConfig(new ButtonMappingConfig()
                    .setType(ButtonMappingType.TOGGLE_SNEAK)
                    .setGamepadButton(GamepadButtons.LEFT_THUMB_CLICK)
                    .setBinding("TOGGLE_SNEAK")
                    .setContext(ButtonMappingContext.IN_GAME)
                    .setPressAgainWhenActivated(true), Config::inMemory),
    };

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
    final ForgeConfigSpec.ConfigValue<List<? extends Config>> mappingsVal;
    public static ButtonMappingConfig[] MAPPING_CONFIG;

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
            ArrayList mappings = new ArrayList();
            for (Config config : CONFIG.mappingsVal.get()) {
                mappings.add(objectConverter.toObject(config, ButtonMappingConfig::new));
            }
            ButtonMappingConfig[] mappingsArray = new ButtonMappingConfig[mappings.size()];
            mappings.toArray(mappingsArray);
            MAPPING_CONFIG = mappingsArray;
        }
    }

    public Settings(ForgeConfigSpec.Builder builder) {
        builder.push("gyro");
        gyroTightenMagVal = builder
                .comment("Threshold below which gyro motion will have tightened sensitivity (°/s)")
                .defineInRange("gyroTightenMag", 5, 0, Double.POSITIVE_INFINITY);
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
        builder.pop();

        builder.push("mouse");
        mouseSmoothingThreshVal = builder
                .comment("Threshold below which mouse motion will be smoothed (LSBs, [0-65535])")
                .defineInRange("mouseSmoothingThresh", 500, 0.0, 65536);
        mouseTightenThreshVal = builder
                .comment("Threshold below which mouse motion will have tightened sensitivity (LSBs, [0-65535])")
                .defineInRange("mouseTightenThresh", 100, 0.0, 65536);
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
        builder.pop();

        builder.push("thumbstick_movement");
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
        builder.pop();

        builder.push("misc");
        modeSwitchBeepFreqVal = builder
                .comment("Sneak toggle beep frequency (Hz)")
                .defineInRange("sneakToggleBeepFreq", 1000, 7.6, 495483);
        modeSwitchBeepLenVal = builder
                .comment("Sneak toggle beep length (s)")
                .defineInRange("sneakToggleBeepLen", 0.1, 0, 8622);
        builder.pop();

        builder.push("flick_stick");
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
        builder.pop();

        builder.push("keyboard");
        keyRepeatActivateTimeVal = builder
                .comment("Key repeat activation time (ns)")
                .defineInRange("keyRepeatActivateTime", 500_000_000, 1, Long.MAX_VALUE);
        keyRepeatRepeatTimeVal = builder
                .comment("Key repeat repetition time (ns)")
                .defineInRange("keyRepeatRepeatTime", 300_000_000, 1, Long.MAX_VALUE);
        builder.pop();

        mappingsVal = builder
                .comment("Key mappings")
                .defineListAllowEmpty(List.of("mapping"), () -> Arrays.asList(defaultMappings), ButtonMappingConfig::validate);
    }
}
