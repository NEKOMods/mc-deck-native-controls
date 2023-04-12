package nekomods.deckcontrols;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.slf4j.Logger;

import java.util.Arrays;

import static nekomods.deckcontrols.Settings.*;
import static org.lwjgl.glfw.GLFW.*;

public class InputHooks {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Minecraft minecraft = Minecraft.getInstance();

    private long last_nanos;
    private int backup_wasd_last_thumb_x;
    private int backup_wasd_last_thumb_y;
    private int flick_stick_last_thumb_x;
    private int flick_stick_last_thumb_y;
    private boolean gyro_is_enabled = true;
    private long flick_stick_progress = FLICK_STICK_TIME_NANOS;
    private double flick_stick_amount;
    private double[] flick_stick_smoothing = new double[16];
    private int flick_stick_smoothing_i;
    private double mouse_gui_leftover_frac_x;
    private double mouse_gui_leftover_frac_y;
    boolean shift_pressed;
    private boolean ctrl_pressed;
    private boolean alt_pressed;
    int lpad_menu_selection = -1;
    private int last_lpad_menu_selection;
    private boolean lpad_is_pressed;
    ITouchMenu lpad_menu;
    int rpad_menu_selection = -1;
    private int last_rpad_menu_selection;
    private boolean rpad_is_pressed;
    ITouchMenu rpad_menu;
    private final ITouchMenu hotbarMenu = new HotbarGridTouchMenu(
            (option) -> keyboardPress(GLFW_KEY_1 + option),
            (option) -> keyboardRelease(GLFW_KEY_1 + option),
            (old_option, new_option) -> {
                keyboardRelease(GLFW_KEY_1 + old_option);
                keyboardPress(GLFW_KEY_1 + new_option);
            });
    private final TouchKeyboard touchKeyboard = new TouchKeyboard(this::onTouchKeyboardKey);
    private boolean last_was_gui_mode;

    private boolean touch_keyboard_active;

    // need to maintain our own sense of which buttons are pressed
    private int buttonsPressedCache = 0;

    public abstract class AbstractButtonMapping {
        public int buttonBitfield;
        // controls whether _this_ mapping is active. context in keyMapping is ignored when using e.g. SimpleButtonMapping
        public IKeyConflictContext keyConflictContext;
        // press this key if key conflict context becomes active?
        public boolean activateOnSwitchIn;

        boolean was_active;
        protected boolean is_pressed;

        protected abstract void _press();
        protected abstract void _release();

        public void press() {
            _press();
            is_pressed = true;
        }
        public void release() {
            if (is_pressed) {
                _release();
                is_pressed = false;
            }
        }

        public void activate() { }
        public void deactivate() { }

        public AbstractButtonMapping setActivateOnSwitchIn(boolean val) {
            this.activateOnSwitchIn = val;
            return this;
        }
    }

    public class KeyConstantOrMapping {
        private InputConstants.Key key;
        private KeyMapping keyMapping;

        public KeyConstantOrMapping(InputConstants.Key key) {
            this.key = key;
            this.keyMapping = null;
        }

        public KeyConstantOrMapping(KeyMapping keyMapping) {
            this.key = null;
            this.keyMapping = keyMapping;
        }

        public InputConstants.Key getKey() {
            if (keyMapping != null)
                return keyMapping.getKey();

            return key;
        }
    }

    public class SimpleButtonMapping extends AbstractButtonMapping {
        public KeyConstantOrMapping key;

        public SimpleButtonMapping(int buttonBitfield, KeyMapping keyMapping, IKeyConflictContext keyConflictContext) {
            this.buttonBitfield = buttonBitfield;
            // FIXME probably need to save something about the KeyMapping when we save/load config files
            this.key = new KeyConstantOrMapping(keyMapping);
            this.keyConflictContext = keyConflictContext;
        }

        public SimpleButtonMapping(int buttonBitfield, KeyMapping keyMapping) {
            this(buttonBitfield, keyMapping, KeyConflictContext.UNIVERSAL);
        }

        public SimpleButtonMapping(int buttonBitfield, InputConstants.Key key, IKeyConflictContext keyConflictContext) {
            this.buttonBitfield = buttonBitfield;
            this.key = new KeyConstantOrMapping(key);
            this.keyConflictContext = keyConflictContext;
        }

        public SimpleButtonMapping(int buttonBitfield, InputConstants.Key key) {
            this(buttonBitfield, key, KeyConflictContext.UNIVERSAL);
        }

        @Override
        protected void _press() {
            InputHooks.this.press(key);
        }

        @Override
        protected void _release() {
            InputHooks.this.release(key);
        }
    }

    private class DisableGyroButton extends AbstractButtonMapping {
        public DisableGyroButton(int buttonBitfield) {
            this.buttonBitfield = buttonBitfield;
            this.keyConflictContext = KeyConflictContext.IN_GAME;
        }

        @Override
        protected void _press() {
            gyro_is_enabled = false;
        }

        @Override
        protected void _release() {
            gyro_is_enabled = true;
        }
    }

    private class ToggleTouchKeyboardButton extends AbstractButtonMapping {
        public ToggleTouchKeyboardButton(int buttonBitfield) {
            this.buttonBitfield = buttonBitfield;
            this.keyConflictContext = KeyConflictContext.UNIVERSAL;
        }

        @Override
        protected void _press() {
            touch_keyboard_active = !touch_keyboard_active;
            lpad_menu_selection = -1;
            rpad_menu_selection = -1;
            touchKeyboard.resetState();
        }

        @Override
        protected void _release() { }
    }

    private class TouchKeyboardInactiveContext implements IKeyConflictContext {
        @Override
        public boolean isActive() {
            return minecraft.screen != null && !touch_keyboard_active;
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return false;
        }
    }

    private class KeyRepeatStateHolder {
        public long repeat_time = -1;
        public final Runnable repeat;

        public KeyRepeatStateHolder(Runnable repeat) {
            this.repeat = repeat;
        }

        public void update(long delta_nanos) {
            if (repeat_time > 0) {
                repeat_time -= delta_nanos;
                if (repeat_time <= 0) {
                    repeat.run();
                    repeat_time = KEY_REPEAT_REPEAT_TIME_NANOS;
                }
            }
        }
    }

    private class KeyRepeatButtonMapping extends AbstractButtonMapping {
        private KeyRepeatStateHolder keyRepeatStateHolder;

        public KeyRepeatButtonMapping(int buttonBitfield, IKeyConflictContext keyConflictContext, KeyRepeatStateHolder keyRepeatStateHolder) {
            this.buttonBitfield = buttonBitfield;
            this.keyConflictContext = keyConflictContext;
            this.keyRepeatStateHolder = keyRepeatStateHolder;
        }

        @Override
        protected void _press() {
            this.keyRepeatStateHolder.repeat_time = KEY_REPEAT_ACTIVATE_TIME_NANOS;
            this.keyRepeatStateHolder.repeat.run();
        }

        @Override
        protected void _release() {
            this.keyRepeatStateHolder.repeat_time = -1;
        }
    }

    public class ToggleButtonMapping extends SimpleButtonMapping {
        public boolean toggleIsActive;
        boolean dontReleaseYet;
        ToggleCompanionButtonMapping toggleCompanionButtonMapping;

        public ToggleButtonMapping(int buttonBitfield, KeyMapping keyMapping, IKeyConflictContext keyConflictContext) {
            super(buttonBitfield, keyMapping, keyConflictContext);
        }

        public ToggleButtonMapping(int buttonBitfield, KeyMapping keyMapping) {
            super(buttonBitfield, keyMapping);
        }

        public ToggleButtonMapping(int buttonBitfield, InputConstants.Key key, IKeyConflictContext keyConflictContext) {
            super(buttonBitfield, key, keyConflictContext);
        }

        public ToggleButtonMapping(int buttonBitfield, InputConstants.Key key) {
            super(buttonBitfield, key);
        }

        @Override
        public void activate() {
            LOGGER.debug("new toggle logic -- activate");
            if (toggleIsActive) {
                LOGGER.debug("new toggle logic -- activate -- pressing");
                InputHooks.this.press(this.key);
            }
        }

        @Override
        public void deactivate() {
            LOGGER.debug("new toggle logic -- deactivate");
            if (toggleIsActive) {
                LOGGER.debug("new toggle logic -- deactivate -- releasing");
                InputHooks.this.release(this.key);
            }
            toggleIsActive = false;
        }

        @Override
        protected void _press() {
            toggleIsActive = !toggleIsActive;
            if (MODE_SWITCH_BEEP_LEN > 0)
                DeckControls.INPUT.beep(MODE_SWITCH_BEEP_FREQ, MODE_SWITCH_BEEP_LEN);
            LOGGER.debug("new toggle logic -- toggle");
            if (toggleIsActive) {
                boolean already_pressed = false;
                if (toggleCompanionButtonMapping != null) {
                    already_pressed = toggleCompanionButtonMapping.is_pressed;
                }

                if (!already_pressed) {
                    LOGGER.debug("new toggle logic -- toggle -- pressing");
                    InputHooks.this.press(this.key);
                }

                dontReleaseYet = already_pressed;
            } else {
                LOGGER.debug("new toggle logic -- toggle -- releasing");
                InputHooks.this.release(this.key);
            }
        }

        @Override
        protected void _release() { }
    }

    public class ToggleCompanionButtonMapping extends AbstractButtonMapping {
        private ToggleButtonMapping toggleButtonMapping;

        public ToggleCompanionButtonMapping(int buttonBitfield, IKeyConflictContext keyConflictContext, ToggleButtonMapping toggleButtonMapping) {
            this.buttonBitfield = buttonBitfield;
            this.keyConflictContext = keyConflictContext;
            this.toggleButtonMapping = toggleButtonMapping;
            this.toggleButtonMapping.toggleCompanionButtonMapping = this;
        }

        @Override
        protected void _press() {
            LOGGER.debug("new toggle logic -- hold button press");
            if (!toggleButtonMapping.toggleIsActive) {
                LOGGER.debug("new toggle logic -- hold button press -- actually pressing");
                InputHooks.this.press(toggleButtonMapping.key);
            } else {
                LOGGER.debug("new toggle logic -- hold button press -- already latched");
            }
        }

        @Override
        protected void _release() {
            LOGGER.debug("new toggle logic -- hold button release");
            if (!toggleButtonMapping.toggleIsActive) {
                LOGGER.debug("new toggle logic -- hold button release -- releasing (toggle not active)");
                InputHooks.this.release(toggleButtonMapping.key);
            } else if (toggleButtonMapping.toggleIsActive && !toggleButtonMapping.dontReleaseYet) {
                LOGGER.debug("new toggle logic -- hold button release -- releasing toggle");
                if (MODE_SWITCH_BEEP_LEN > 0)
                    DeckControls.INPUT.beep(MODE_SWITCH_BEEP_FREQ, MODE_SWITCH_BEEP_LEN);
                InputHooks.this.release(toggleButtonMapping.key);
                toggleButtonMapping.toggleIsActive = false;
            }
            toggleButtonMapping.dontReleaseYet = false;
        }
    }

    private final KeyRepeatStateHolder[] keyRepeats = new KeyRepeatStateHolder[] {
            new KeyRepeatStateHolder(() -> {
                minecraft.mouseHandler.onScroll(minecraft.getWindow().getWindow(), 0, 1);
            }),
            new KeyRepeatStateHolder(() -> {
                minecraft.mouseHandler.onScroll(minecraft.getWindow().getWindow(), 0, -1);
            }),
    };

    final ToggleButtonMapping toggleSneak = new ToggleButtonMapping(HidInput.GamepadButtons.BTN_LTHUMB_CLICK, minecraft.options.keyShift, KeyConflictContext.IN_GAME);

    private final AbstractButtonMapping[] simpleMappings = new AbstractButtonMapping[] {
            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_A, minecraft.options.keyAttack, KeyConflictContext.IN_GAME),
            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_B, minecraft.options.keyJump, KeyConflictContext.IN_GAME),
            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_Y, minecraft.options.keySprint, KeyConflictContext.IN_GAME),

            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_RT_ANALOG_FULL, minecraft.options.keyUse, KeyConflictContext.IN_GAME),
            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_RT_DIGITAL, minecraft.options.keyPickItem, KeyConflictContext.IN_GAME),
            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_RT_ANALOG_FULL, InputConstants.Type.MOUSE.getOrCreate(GLFW_MOUSE_BUTTON_RIGHT), KeyConflictContext.GUI),
            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_RT_DIGITAL, InputConstants.Type.MOUSE.getOrCreate(GLFW_MOUSE_BUTTON_MIDDLE), KeyConflictContext.GUI),

            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_D_UP, minecraft.options.keySwapOffhand, KeyConflictContext.IN_GAME),
            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_D_DOWN, minecraft.options.keyDrop, KeyConflictContext.IN_GAME),
            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_D_LEFT, minecraft.options.keySwapOffhand, KeyConflictContext.GUI),
            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_D_RIGHT, minecraft.options.keyDrop, KeyConflictContext.GUI),

            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_OPTIONS, InputConstants.Type.KEYSYM.getOrCreate(GLFW_KEY_ESCAPE), KeyConflictContext.UNIVERSAL),
            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_A, InputConstants.Type.KEYSYM.getOrCreate(GLFW_KEY_ESCAPE), KeyConflictContext.GUI),

            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_VIEW, minecraft.options.keyInventory, KeyConflictContext.IN_GAME),
            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_VIEW, InputConstants.Type.KEYSYM.getOrCreate(GLFW_KEY_ESCAPE), KeyConflictContext.GUI),

            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_L4, InputConstants.Type.KEYSYM.getOrCreate(GLFW_KEY_LEFT_CONTROL), KeyConflictContext.UNIVERSAL),
            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_L5, InputConstants.Type.KEYSYM.getOrCreate(GLFW_KEY_LEFT_ALT), KeyConflictContext.UNIVERSAL),
            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_R5, InputConstants.Type.KEYSYM.getOrCreate(GLFW_KEY_LEFT_SHIFT), KeyConflictContext.UNIVERSAL),

            new ToggleTouchKeyboardButton(HidInput.GamepadButtons.BTN_R4),

            new DisableGyroButton(HidInput.GamepadButtons.BTN_X),
            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_X, InputConstants.Type.MOUSE.getOrCreate(GLFW_MOUSE_BUTTON_LEFT), KeyConflictContext.GUI),
            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_RPAD_CLICK, InputConstants.Type.MOUSE.getOrCreate(GLFW_MOUSE_BUTTON_LEFT), new TouchKeyboardInactiveContext()),

            new KeyRepeatButtonMapping(HidInput.GamepadButtons.BTN_D_UP, KeyConflictContext.GUI, keyRepeats[0]).setActivateOnSwitchIn(true),
            new KeyRepeatButtonMapping(HidInput.GamepadButtons.BTN_D_LEFT, KeyConflictContext.IN_GAME, keyRepeats[0]).setActivateOnSwitchIn(true),
            new KeyRepeatButtonMapping(HidInput.GamepadButtons.BTN_D_DOWN, KeyConflictContext.GUI, keyRepeats[1]).setActivateOnSwitchIn(true),
            new KeyRepeatButtonMapping(HidInput.GamepadButtons.BTN_D_RIGHT, KeyConflictContext.IN_GAME, keyRepeats[1]).setActivateOnSwitchIn(true),

            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_LT_ANALOG_FULL, InputConstants.Type.KEYSYM.getOrCreate(GLFW_KEY_LEFT_SHIFT), KeyConflictContext.GUI).setActivateOnSwitchIn(true),

            toggleSneak,
            new ToggleCompanionButtonMapping(HidInput.GamepadButtons.BTN_LT_ANALOG_FULL, KeyConflictContext.IN_GAME, toggleSneak).setActivateOnSwitchIn(true),
    };

    private static double flickStickEase(double input) {
        double flipped = 1 - input;
        return 1 - flipped * flipped;
    }

    private double flickSmoothed(double input) {
        flick_stick_smoothing[flick_stick_smoothing_i] = input;
        flick_stick_smoothing_i = (flick_stick_smoothing_i + 1) % flick_stick_smoothing.length;
        return Arrays.stream(flick_stick_smoothing).average().orElse(0);
    }

    private void press(KeyConstantOrMapping key) {
        press(key.getKey());
    }

    private void release(KeyConstantOrMapping key) {
        release(key.getKey());
    }

    private void press(InputConstants.Key key) {
        LOGGER.debug("new press " + key.getName());
        if (key.getType() == InputConstants.Type.KEYSYM)
            keyboardPress(key.getValue());
        else if (key.getType() == InputConstants.Type.SCANCODE)
            scancodePress(key.getValue());
        else if (key.getType() == InputConstants.Type.MOUSE)
            mousePress(key.getValue());
    }

    private void release(InputConstants.Key key) {
        LOGGER.debug("new release " + key.getName());
        if (key.getType() == InputConstants.Type.KEYSYM)
            keyboardRelease(key.getValue());
        else if (key.getType() == InputConstants.Type.SCANCODE)
            scancodeRelease(key.getValue());
        else if (key.getType() == InputConstants.Type.MOUSE)
            mouseRelease(key.getValue());
    }

    private int calcKeyModifiers() {
        int modifiers = 0;
        if (shift_pressed)
            modifiers |= GLFW_MOD_SHIFT;
        if (alt_pressed)
            modifiers |= GLFW_MOD_ALT;
        if (ctrl_pressed)
            modifiers |= GLFW_MOD_CONTROL;
        return modifiers;
    }

    private void keyboardPress(int key) {
        if (key == GLFW_KEY_LEFT_SHIFT || key == GLFW_KEY_RIGHT_SHIFT)
            shift_pressed = true;
        if (key == GLFW_KEY_LEFT_CONTROL || key == GLFW_KEY_RIGHT_CONTROL)
            ctrl_pressed = true;
        if (key == GLFW_KEY_LEFT_ALT || key == GLFW_KEY_RIGHT_ALT)
            alt_pressed = true;

        minecraft.keyboardHandler.keyPress(
                minecraft.getWindow().getWindow(),
                key,
                glfwGetKeyScancode(key),
                GLFW_PRESS,
                calcKeyModifiers());
    }

    private void keyboardRelease(int key) {
        if (key == GLFW_KEY_LEFT_SHIFT || key == GLFW_KEY_RIGHT_SHIFT)
            shift_pressed = false;
        if (key == GLFW_KEY_LEFT_CONTROL || key == GLFW_KEY_RIGHT_CONTROL)
            ctrl_pressed = false;
        if (key == GLFW_KEY_LEFT_ALT || key == GLFW_KEY_RIGHT_ALT)
            alt_pressed = false;

        minecraft.keyboardHandler.keyPress(
                minecraft.getWindow().getWindow(),
                key,
                glfwGetKeyScancode(key),
                GLFW_RELEASE,
                calcKeyModifiers());
    }

    private void scancodePress(int scancode) {
        // FIXME this is not adequate?
        // what if mapping is for a scancode, but that key _does_ have a named key?
        minecraft.keyboardHandler.keyPress(
                minecraft.getWindow().getWindow(),
                -1,
                scancode,
                GLFW_PRESS,
                calcKeyModifiers());
    }

    private void scancodeRelease(int scancode) {
        // FIXME this is not adequate?
        minecraft.keyboardHandler.keyPress(
                minecraft.getWindow().getWindow(),
                -1,
                scancode,
                GLFW_RELEASE,
                calcKeyModifiers());
    }

    private void mousePress(int button) {
        minecraft.mouseHandler.onPress(
                minecraft.getWindow().getWindow(),
                button,
                GLFW_PRESS,
                calcKeyModifiers());
    }

    private void mouseRelease(int button) {
        minecraft.mouseHandler.onPress(
                minecraft.getWindow().getWindow(),
                button,
                GLFW_RELEASE,
                calcKeyModifiers());
    }

    private void onTouchKeyboardKey(int key) {
        if (key >= '0' && key <= '9') {
            keyboardPress(GLFW_KEY_0 + key - '0');
            keyboardRelease(GLFW_KEY_0 + key - '0');
            if (!shift_pressed) {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        key, calcKeyModifiers());
            } else {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        new int[] {')', '!', '@', '#', '$', '%', '^', '&', '*', '('} [key - '0'],
                        calcKeyModifiers());
            }
        } else if (key >= 'a' && key <= 'z') {
            keyboardPress(GLFW_KEY_A + key - 'a');
            keyboardRelease(GLFW_KEY_A + key - 'a');
            if (!shift_pressed) {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        key, calcKeyModifiers());
            } else {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        key + 'A' - 'a',
                        calcKeyModifiers());
            }
        } else if (key >= GLFW_KEY_F1 && key <= GLFW_KEY_F12) {
            keyboardPress(key);
            keyboardRelease(key);
        } else if (key == ' ') {
            keyboardPress(GLFW_KEY_SPACE);
            keyboardRelease(GLFW_KEY_SPACE);
            minecraft.keyboardHandler.charTyped(
                    minecraft.getWindow().getWindow(),
                    key, calcKeyModifiers());
        } else if (key == '\t') {
            keyboardPress(GLFW_KEY_TAB);
            keyboardRelease(GLFW_KEY_TAB);
        } else if (key == '\b') {
            keyboardPress(GLFW_KEY_BACKSPACE);
            keyboardRelease(GLFW_KEY_BACKSPACE);
        } else if (key == '\n') {
            keyboardPress(GLFW_KEY_ENTER);
            keyboardRelease(GLFW_KEY_ENTER);
        } else if (key == ',') {
            keyboardPress(GLFW_KEY_COMMA);
            keyboardRelease(GLFW_KEY_COMMA);
            if (!shift_pressed) {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        key, calcKeyModifiers());
            } else {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        '<', calcKeyModifiers());
            }
        } else if (key == '.') {
            keyboardPress(GLFW_KEY_PERIOD);
            keyboardRelease(GLFW_KEY_PERIOD);
            if (!shift_pressed) {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        key, calcKeyModifiers());
            } else {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        '>', calcKeyModifiers());
            }
        } else if (key == '[') {
            keyboardPress(GLFW_KEY_LEFT_BRACKET);
            keyboardRelease(GLFW_KEY_LEFT_BRACKET);
            if (!shift_pressed) {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        key, calcKeyModifiers());
            } else {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        '{', calcKeyModifiers());
            }
        } else if (key == ']') {
            keyboardPress(GLFW_KEY_RIGHT_BRACKET);
            keyboardRelease(GLFW_KEY_RIGHT_BRACKET);
            if (!shift_pressed) {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        key, calcKeyModifiers());
            } else {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        '}', calcKeyModifiers());
            }
        } else if (key == '-') {
            keyboardPress(GLFW_KEY_MINUS);
            keyboardRelease(GLFW_KEY_MINUS);
            if (!shift_pressed) {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        key, calcKeyModifiers());
            } else {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        '_', calcKeyModifiers());
            }
        } else if (key == '=') {
            keyboardPress(GLFW_KEY_EQUAL);
            keyboardRelease(GLFW_KEY_EQUAL);
            if (!shift_pressed) {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        key, calcKeyModifiers());
            } else {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        '+', calcKeyModifiers());
            }
        } else if (key == '/') {
            keyboardPress(GLFW_KEY_SLASH);
            keyboardRelease(GLFW_KEY_SLASH);
            if (!shift_pressed) {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        key, calcKeyModifiers());
            } else {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        '?', calcKeyModifiers());
            }
        } else if (key == '\\') {
            keyboardPress(GLFW_KEY_BACKSLASH);
            keyboardRelease(GLFW_KEY_BACKSLASH);
            if (!shift_pressed) {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        key, calcKeyModifiers());
            } else {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        '|', calcKeyModifiers());
            }
        } else if (key == '`') {
            keyboardPress(GLFW_KEY_GRAVE_ACCENT);
            keyboardRelease(GLFW_KEY_GRAVE_ACCENT);
            if (!shift_pressed) {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        key, calcKeyModifiers());
            } else {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        '~', calcKeyModifiers());
            }
        } else if (key == ';') {
            keyboardPress(GLFW_KEY_SEMICOLON);
            keyboardRelease(GLFW_KEY_SEMICOLON);
            if (!shift_pressed) {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        key, calcKeyModifiers());
            } else {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        ':', calcKeyModifiers());
            }
        } else if (key == '\'') {
            keyboardPress(GLFW_KEY_APOSTROPHE);
            keyboardRelease(GLFW_KEY_APOSTROPHE);
            if (!shift_pressed) {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        key, calcKeyModifiers());
            } else {
                minecraft.keyboardHandler.charTyped(
                        minecraft.getWindow().getWindow(),
                        '"', calcKeyModifiers());
            }
        } else {
            LOGGER.error("Don't know how to press key " + key);
        }
    }

    private void doBackupWASD(int thumb_x, int thumb_y) {
        if (minecraft.screen == null) {
            if (backup_wasd_last_thumb_y < THUMB_DIGITAL_ACTIVATE && thumb_y >= THUMB_DIGITAL_ACTIVATE) {
                press(minecraft.options.keyUp.getKey());
            }
            if (backup_wasd_last_thumb_y >= THUMB_DIGITAL_DEACTIVATE && thumb_y < THUMB_DIGITAL_DEACTIVATE) {
                release(minecraft.options.keyUp.getKey());
            }
            if (-backup_wasd_last_thumb_y < THUMB_DIGITAL_ACTIVATE && -thumb_y >= THUMB_DIGITAL_ACTIVATE) {
                press(minecraft.options.keyDown.getKey());
            }
            if (-backup_wasd_last_thumb_y >= THUMB_DIGITAL_DEACTIVATE && -thumb_y < THUMB_DIGITAL_DEACTIVATE) {
                release(minecraft.options.keyDown.getKey());
            }
            if (backup_wasd_last_thumb_x < THUMB_DIGITAL_ACTIVATE && thumb_x >= THUMB_DIGITAL_ACTIVATE) {
                press(minecraft.options.keyRight.getKey());
            }
            if (backup_wasd_last_thumb_x >= THUMB_DIGITAL_DEACTIVATE && thumb_x < THUMB_DIGITAL_DEACTIVATE) {
                release(minecraft.options.keyRight.getKey());
            }
            if (-backup_wasd_last_thumb_x < THUMB_DIGITAL_ACTIVATE && -thumb_x >= THUMB_DIGITAL_ACTIVATE) {
                press(minecraft.options.keyLeft.getKey());
            }
            if (-backup_wasd_last_thumb_x >= THUMB_DIGITAL_DEACTIVATE && -thumb_x < THUMB_DIGITAL_DEACTIVATE) {
                release(minecraft.options.keyLeft.getKey());
            }
        }
        backup_wasd_last_thumb_x = thumb_x;
        backup_wasd_last_thumb_y = thumb_y;
    }

    private void doMouseCursor(HidInput.AccumHidState accumState) {
        boolean is_gui_mode = minecraft.screen != null;

        if (!is_gui_mode) {
            mouse_gui_leftover_frac_x = 0;
            mouse_gui_leftover_frac_y = 0;
        }
        if (accumState.mouseDX != 0 || accumState.mouseDY != 0) {
            if (!is_gui_mode) {
                minecraft.mouseHandler.onMove(
                        minecraft.getWindow().getWindow(),
                        minecraft.mouseHandler.xpos() + accumState.mouseDX / RPAD_MOUSE_SCALE_X_CAM,
                        minecraft.mouseHandler.ypos() - accumState.mouseDY / RPAD_MOUSE_SCALE_Y_CAM
                );
            } else {
                // gross, integers and units are hard
                double mouse_final_dx = accumState.mouseDX + mouse_gui_leftover_frac_x;
                double mouse_final_dy = accumState.mouseDY + mouse_gui_leftover_frac_y;

                long mouse_int_dx = (long) (mouse_final_dx / RPAD_MOUSE_SCALE_X_GUI);
                long mouse_int_dy = (long) (mouse_final_dy / RPAD_MOUSE_SCALE_Y_GUI);

                mouse_gui_leftover_frac_x = mouse_final_dx - mouse_int_dx * RPAD_MOUSE_SCALE_X_GUI;
                mouse_gui_leftover_frac_y = mouse_final_dy - mouse_int_dy * RPAD_MOUSE_SCALE_Y_GUI;

                if (mouse_int_dx != 0 || mouse_int_dy != 0) {
                    // YUCK
                    double[] curX = new double[1];
                    double[] curY = new double[1];
                    glfwGetCursorPos(
                            minecraft.getWindow().getWindow(),
                            curX,
                            curY
                    );
                    glfwSetCursorPos(
                            minecraft.getWindow().getWindow(),
                            curX[0] + mouse_int_dx,
                            curY[0] - mouse_int_dy
                    );
                    minecraft.mouseHandler.onMove(
                            minecraft.getWindow().getWindow(),
                            curX[0] + mouse_int_dx,
                            curY[0] - mouse_int_dy
                    );
                }
            }
        }
    }

    private void doGyroAndFlickStick(boolean use_gyro, int thumb_x, int thumb_y, HidInput.AccumHidState accumState, long delta_nanos) {
        if (minecraft.screen == null && minecraft.player != null) {
            double tot_turn_yaw = 0;
            double tot_turn_pitch = 0;

            if (use_gyro) {
                if (minecraft.player.isScoping()) {
                    tot_turn_yaw += -accumState.camYaw * GYRO_CAM_SENSITIVITY_SCOPE_X;
                    tot_turn_pitch += -accumState.camPitch * GYRO_CAM_SENSITIVITY_SCOPE_Y;
                } else {
                    tot_turn_yaw += -accumState.camYaw * GYRO_CAM_SENSITIVITY_X;
                    tot_turn_pitch += -accumState.camPitch * GYRO_CAM_SENSITIVITY_Y;
                }
            }

            if ((thumb_x * thumb_x + thumb_y * thumb_y > FLICK_STICK_ACTIVATE_DIST * FLICK_STICK_ACTIVATE_DIST) &&
                    (flick_stick_last_thumb_x * flick_stick_last_thumb_x + flick_stick_last_thumb_y * flick_stick_last_thumb_y <= FLICK_STICK_ACTIVATE_DIST * FLICK_STICK_ACTIVATE_DIST)) {
                LOGGER.debug("flick stick start");
                flick_stick_progress = 0;
                flick_stick_amount = Math.toDegrees(Math.atan2(thumb_x, thumb_y));
            } else if (thumb_x * thumb_x + thumb_y * thumb_y > FLICK_STICK_DEACTIVATE_DIST * FLICK_STICK_DEACTIVATE_DIST) {
                double cur_angle = Math.toDegrees(Math.atan2(thumb_x, thumb_y));
                double last_angle = Math.toDegrees(Math.atan2(flick_stick_last_thumb_x, flick_stick_last_thumb_y));

                double diff_angle = cur_angle - last_angle;
                if (diff_angle < -180) diff_angle += 360;
                if (diff_angle > 180) diff_angle -= 360;

                double tier_smooth_thresh_1 = FLICK_STICK_SMOOTH_THRESH / 2;
                double tier_smooth_thresh_2 = FLICK_STICK_SMOOTH_THRESH;
                double diff_mag = Math.abs(diff_angle);
                double smooth_direct_weight = (diff_mag - tier_smooth_thresh_1) / (tier_smooth_thresh_2 - tier_smooth_thresh_1);
                if (smooth_direct_weight < 0) smooth_direct_weight = 0;
                if (smooth_direct_weight > 1) smooth_direct_weight = 1;

                tot_turn_yaw += diff_angle * smooth_direct_weight + flickSmoothed(diff_angle * (1 - smooth_direct_weight));
            } else if (flick_stick_last_thumb_x * flick_stick_last_thumb_x + flick_stick_last_thumb_y * flick_stick_last_thumb_y >= FLICK_STICK_DEACTIVATE_DIST * FLICK_STICK_DEACTIVATE_DIST) {
                LOGGER.debug("flick stick deactivate");
                for (int i = 0; i < flick_stick_smoothing.length; i++)
                    flick_stick_smoothing[i] = 0;
            }

            if (flick_stick_progress < FLICK_STICK_TIME_NANOS) {
                double last_flick_progress = (double)flick_stick_progress / FLICK_STICK_TIME_NANOS;
                flick_stick_progress = Math.min(flick_stick_progress + delta_nanos, FLICK_STICK_TIME_NANOS);
                LOGGER.debug("flicking progress raw " + flick_stick_progress);
                double current_flick_progress = (double)flick_stick_progress / FLICK_STICK_TIME_NANOS;

                last_flick_progress = flickStickEase(last_flick_progress);
                current_flick_progress = flickStickEase(current_flick_progress);

                LOGGER.debug("flicking progress " + current_flick_progress);
                tot_turn_yaw += (current_flick_progress - last_flick_progress) * flick_stick_amount;
            }

            if (tot_turn_yaw != 0 || tot_turn_pitch != 0) {
                minecraft.player.turn(
                        tot_turn_yaw / 0.15,
                        tot_turn_pitch / 0.15);
            }
        }
        flick_stick_last_thumb_x = thumb_x;
        flick_stick_last_thumb_y = thumb_y;
    }

    public void runTick() {
        minecraft.getWindow().setErrorSection("DeckControlsMod");
        minecraft.getProfiler().push("deck_controls_mod");

        long current_nanos = Util.getNanos();
        boolean is_gui_mode = minecraft.screen != null;

        if (!is_gui_mode && last_was_gui_mode) {
            // switch out of GUI mode --> activate hotbar
            touch_keyboard_active = false;
            lpad_menu_selection = -1;
            rpad_menu_selection = -1;
            touchKeyboard.resetState();
        }

        if (touch_keyboard_active) {
            lpad_menu = touchKeyboard.getLeft();
            rpad_menu = touchKeyboard.getRight();
        } else {
            lpad_menu = hotbarMenu;
            rpad_menu = null;
        }

        HidInput.AccumHidState accumState = DeckControls.INPUT.accumInput.getAndSet(new HidInput.AccumHidState());
        HidInput.OtherHidState gamepad = DeckControls.INPUT.latestInput;

        // movement keys (backup only, for e.g. boats)
        doBackupWASD(gamepad.lthumb_x, gamepad.lthumb_y);

        // mouse cursor
        if (rpad_menu == null) {
            doMouseCursor(accumState);
        }

        // menu
        if (lpad_menu != null) {
            // input coordinate system:
            //         32767
            //           ^
            //           |
            // -32768 <--+--> 32767
            //           |
            //           v
            //        -32768
            int menu_x = gamepad.lpad_x + 32768;
            int menu_y = 32767 - gamepad.lpad_y;
            // being remapped to
            // 0 --> 65535
            // |
            // v
            // 65535
            if ((gamepad.buttons & HidInput.GamepadButtons.BTN_LPAD_TOUCH) != 0) {
                if (lpad_menu_selection == -1) {
                    lpad_menu_selection = lpad_menu.padToOption(menu_x, menu_y);
                } else {
                    boolean outside_hysteresis = lpad_menu.hysteresisExceeded(lpad_menu_selection, menu_x, menu_y);
                    if (outside_hysteresis) {
                        int old_lpad_menu_selection = lpad_menu_selection;
                        lpad_menu_selection = lpad_menu.padToOption(menu_x, menu_y);
                        if (lpad_is_pressed)
                            lpad_menu.onChangeWhileClicked(old_lpad_menu_selection, lpad_menu_selection);
                        DeckControls.INPUT.tick(false);
                    }
                }
            } else {
                lpad_menu_selection = -1;
            }
        }
        if (rpad_menu != null) {
            int menu_x = gamepad.rpad_x + 32768;
            int menu_y = 32767 - gamepad.rpad_y;
            if ((gamepad.buttons & HidInput.GamepadButtons.BTN_RPAD_TOUCH) != 0) {
                if (rpad_menu_selection == -1) {
                    rpad_menu_selection = rpad_menu.padToOption(menu_x, menu_y);
                } else {
                    boolean outside_hysteresis = rpad_menu.hysteresisExceeded(rpad_menu_selection, menu_x, menu_y);
                    if (outside_hysteresis) {
                        int old_rpad_menu_selection = rpad_menu_selection;
                        rpad_menu_selection = rpad_menu.padToOption(menu_x, menu_y);
                        if (rpad_is_pressed)
                            rpad_menu.onChangeWhileClicked(old_rpad_menu_selection, rpad_menu_selection);
                        DeckControls.INPUT.tick(true);
                    }
                }
            } else {
                rpad_menu_selection = -1;
            }
        }
        // reset keyboard to not-sym mode if neither pad is touched
        if ((gamepad.buttons & HidInput.GamepadButtons.BTN_LPAD_TOUCH) == 0 && (gamepad.buttons & HidInput.GamepadButtons.BTN_RPAD_TOUCH) == 0) {
            touchKeyboard.resetState();
        }

        // key repeat
        for (KeyRepeatStateHolder repeat : keyRepeats) {
            repeat.update(current_nanos - last_nanos);
        }

        DeckControls.INPUT.keyEvents.addLast(HidInput.GamepadButtons.FLAG_BARRIER);
        int keyevent;
        while ((keyevent = DeckControls.INPUT.keyEvents.removeFirst()) != HidInput.GamepadButtons.FLAG_BARRIER) {
            // update cache
            if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0) {
                buttonsPressedCache |= keyevent;
            } else {
                buttonsPressedCache &= ~keyevent;
            }

            // boring keys
            for (AbstractButtonMapping mapping : simpleMappings) {
                if (mapping.keyConflictContext.isActive()) {
                    if ((keyevent & mapping.buttonBitfield) != 0) {
                        // if this mapping just activated, let it know
                        if (!mapping.was_active)
                            mapping.activate();

                        if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0) {
                            mapping.press();
                        } else {
                            mapping.release();
                        }

                        // mark this as active so we don't try to press it again later
                        mapping.was_active = true;
                    }
                }
            }

            // menu clicking
            if ((keyevent & HidInput.GamepadButtons.BTN_LPAD_CLICK) != 0) {
                if (lpad_menu != null) {
                    if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0) {
                        last_lpad_menu_selection = lpad_menu_selection;
                        if (lpad_menu_selection != -1) {
                            lpad_menu.onPress(lpad_menu_selection);
                            lpad_is_pressed = true;
                        }
                    } else {
                        if (lpad_is_pressed) {
                            int release_option = lpad_menu.useInitialKeydownOption() ? last_lpad_menu_selection : lpad_menu_selection;
                            if (release_option != -1)
                                lpad_menu.onRelease(release_option);
                            lpad_is_pressed = false;
                        }
                    }
                }
            }
            if ((keyevent & HidInput.GamepadButtons.BTN_RPAD_CLICK) != 0) {
                if (rpad_menu != null) {
                    if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0) {
                        last_rpad_menu_selection = rpad_menu_selection;
                        if (rpad_menu_selection != -1) {
                            rpad_menu.onPress(rpad_menu_selection);
                            rpad_is_pressed = true;
                        }
                    } else {
                        if (rpad_is_pressed) {
                            int release_option = rpad_menu.useInitialKeydownOption() ? last_rpad_menu_selection : rpad_menu_selection;
                            if (release_option != -1)
                                rpad_menu.onRelease(release_option);
                            rpad_is_pressed = false;
                        }
                    }
                }
            }
        }

        // if the mode has changed, we have to unpress/press keys as appropriate
        for (AbstractButtonMapping mapping : simpleMappings) {
            if (mapping.keyConflictContext.isActive() && !mapping.was_active) {
                // mode became active
                // (but physical button state didn't change)
                mapping.activate();
                // if it is currently pressed, press the mapping (if wanted)
                if ((buttonsPressedCache & mapping.buttonBitfield) != 0) {
                    if (mapping.activateOnSwitchIn) {
                        mapping.press();
                    }
                }
            }
            if (!mapping.keyConflictContext.isActive() && mapping.was_active) {
                // mode became inactive
                mapping.release();
                mapping.deactivate();
            }
            mapping.was_active = mapping.keyConflictContext.isActive();
        }

        // good gyro controls
        doGyroAndFlickStick(
                gyro_is_enabled && ((gamepad.buttons & HidInput.GamepadButtons.BTN_RPAD_TOUCH) == 0),
                gamepad.rthumb_x, gamepad.rthumb_y,
                accumState, current_nanos - last_nanos);

        last_was_gui_mode = is_gui_mode;
        last_nanos = current_nanos;

        minecraft.getProfiler().pop();
    }

    public float fbImpulse(float keyboardImpulse) {
        if (minecraft.screen != null) return keyboardImpulse;   // don't move when inventory is up
        HidInput.OtherHidState gamepad = DeckControls.INPUT.latestInput;
        if (gamepad.lthumb_x * gamepad.lthumb_x + gamepad.lthumb_y * gamepad.lthumb_y > THUMB_DEADZONE * THUMB_DEADZONE) {
            float ret = (float)(gamepad.lthumb_y / THUMB_ANALOG_FULLSCALE);
            if (ret > 1) ret = 1;
            if (ret < -1) ret = -1;
            return ret;
        } else {
            return keyboardImpulse;
        }
    }

    public float lrImpulse(float keyboardImpulse) {
        if (minecraft.screen != null) return keyboardImpulse;   // don't move when inventory is up
        HidInput.OtherHidState gamepad = DeckControls.INPUT.latestInput;
        if (gamepad.lthumb_x * gamepad.lthumb_x + gamepad.lthumb_y * gamepad.lthumb_y > THUMB_DEADZONE * THUMB_DEADZONE) {
            float ret = (float)(gamepad.lthumb_x / -THUMB_ANALOG_FULLSCALE);
            if (ret > 1) ret = 1;
            if (ret < -1) ret = -1;
            return ret;
        } else {
            return keyboardImpulse;
        }
    }

    public boolean modifierPressed(int key) {
        if (key == GLFW_KEY_LEFT_SHIFT || key == GLFW_KEY_RIGHT_SHIFT)
            return shift_pressed;
        if (key == GLFW_KEY_LEFT_CONTROL || key == GLFW_KEY_RIGHT_CONTROL)
            return ctrl_pressed;
        if (key == GLFW_KEY_LEFT_ALT || key == GLFW_KEY_RIGHT_ALT)
            return alt_pressed;

        return false;
    }

    public static void runTickHook() {
        if (DeckControls.HOOKS != null) {
            DeckControls.HOOKS.runTick();
        }
    }

    public static float playerFBImpulse(float keyboardImpulse) {
        if (DeckControls.HOOKS == null) return keyboardImpulse;
        return DeckControls.HOOKS.fbImpulse(keyboardImpulse);
    }

    public static float playerLRImpulse(float keyboardImpulse) {
        if (DeckControls.HOOKS == null) return keyboardImpulse;
        return DeckControls.HOOKS.lrImpulse(keyboardImpulse);
    }

    public static boolean hookKeyDown(boolean existing, int key) {
        if (DeckControls.HOOKS == null) return existing;
        return existing || DeckControls.HOOKS.modifierPressed(key);
    }
}
