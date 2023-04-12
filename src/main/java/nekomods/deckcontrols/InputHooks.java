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
    private int last_lthumb_x;
    private int last_lthumb_y;
    private int last_rthumb_x;
    private int last_rthumb_y;
    private boolean last_btn_view_down_was_e;
    boolean sneak_is_latched;
    private boolean sneak_latched_while_manually_sneaking;
    private boolean manually_sneaking;
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
    private long scroll_up_repeat_time = -1;
    private long scroll_down_repeat_time = -1;
    private final TouchKeyboard touchKeyboard = new TouchKeyboard(this::onTouchKeyboardKey);
    private boolean last_was_gui_mode;

    private boolean touch_keyboard_active;

    // need to maintain our own sense of which buttons are pressed
    private int buttonsPressedCache = 0;

    public static class SimpleButtonMapping {
        public int buttonBitfield;
        public KeyMapping keyMapping;
        // controls whether _this_ mapping is active. context in keyMapping is ignored
        public IKeyConflictContext keyConflictContext;

        boolean was_active;

        public SimpleButtonMapping(int buttonBitfield, KeyMapping keyMapping, IKeyConflictContext keyConflictContext) {
            this.buttonBitfield = buttonBitfield;
            this.keyMapping = keyMapping;
            this.keyConflictContext = keyConflictContext;
        }

        public SimpleButtonMapping(int buttonBitfield, KeyMapping keyMapping) {
            this(buttonBitfield, keyMapping, KeyConflictContext.UNIVERSAL);
        }
    }

    private final SimpleButtonMapping[] simpleMappings = new SimpleButtonMapping[] {
            new SimpleButtonMapping(HidInput.GamepadButtons.BTN_Y, minecraft.options.keySprint, KeyConflictContext.IN_GAME),
    };

    static int CONTROLS_GPB_LCLICK             = HidInput.GamepadButtons.BTN_A;
    static int CONTROLS_GPB_JUMP               = HidInput.GamepadButtons.BTN_B;
    static int CONTROLS_GPB_MCLICK             = HidInput.GamepadButtons.BTN_RT_DIGITAL;
    static int CONTROLS_GPB_SWAPHAND           = HidInput.GamepadButtons.BTN_D_UP;
    static int CONTROLS_GPB_DROPITEM           = HidInput.GamepadButtons.BTN_D_DOWN;
    static int CONTROLS_GPB_SWAPHAND_GUI       = HidInput.GamepadButtons.BTN_D_LEFT;
    static int CONTROLS_GPB_DROPITEM_GUI       = HidInput.GamepadButtons.BTN_D_RIGHT;
    static int CONTROLS_GPB_ESCAPE             = HidInput.GamepadButtons.BTN_OPTIONS;
    static int CONTROLS_GPB_ESCAPEALT          = HidInput.GamepadButtons.BTN_A;
    static int CONTROLS_GPB_INVENTORY          = HidInput.GamepadButtons.BTN_VIEW;
    static int CONTROLS_GPB_LCTRL              = HidInput.GamepadButtons.BTN_L4;
    static int CONTROLS_GPB_LALT               = HidInput.GamepadButtons.BTN_L5;
    static int CONTROLS_GPB_LSHIFTALT          = HidInput.GamepadButtons.BTN_R5;
    static int CONTROLS_GPB_RCLICK             = HidInput.GamepadButtons.BTN_RT_ANALOG_FULL;
    static int CONTROLS_GPB_GYROINHIBIT        = HidInput.GamepadButtons.BTN_X;
    static int CONTROLS_GPB_LCLICKALT          = HidInput.GamepadButtons.BTN_RPAD_CLICK;
    static int CONTROLS_GPB_SCROLL_UP          = HidInput.GamepadButtons.BTN_D_UP;
    static int CONTROLS_GPB_SCROLL_DOWN        = HidInput.GamepadButtons.BTN_D_DOWN;
    static int CONTROLS_GPB_SCROLL_LEFT        = HidInput.GamepadButtons.BTN_D_LEFT;
    static int CONTROLS_GPB_SCROLL_RIGHT       = HidInput.GamepadButtons.BTN_D_RIGHT;
    static int CONTROLS_GPB_CLICK_MODESWITCH   = HidInput.GamepadButtons.BTN_R4;
    static int CONTROLS_GPB_HOLDSNEAK          = HidInput.GamepadButtons.BTN_LT_ANALOG_FULL;
    static int CONTROLS_GPB_TOGGLESNEAK        = HidInput.GamepadButtons.BTN_LTHUMB_CLICK;

    private static double flickStickEase(double input) {
        double flipped = 1 - input;
        return 1 - flipped * flipped;
    }

    private double flickSmoothed(double input) {
        flick_stick_smoothing[flick_stick_smoothing_i] = input;
        flick_stick_smoothing_i = (flick_stick_smoothing_i + 1) % flick_stick_smoothing.length;
        return Arrays.stream(flick_stick_smoothing).average().orElse(0);
    }

    private void press(KeyMapping mapping) {
        LOGGER.debug("new press " + mapping.getName());
        press(mapping.getKey());
    }

    private void release(KeyMapping mapping) {
        LOGGER.debug("new release " + mapping.getName());
        release(mapping.getKey());
    }

    private void press(InputConstants.Key key) {
        if (key.getType() == InputConstants.Type.KEYSYM)
            keyboardPress(key.getValue());
        else if (key.getType() == InputConstants.Type.SCANCODE)
            scancodePress(key.getValue());
        else if (key.getType() == InputConstants.Type.MOUSE)
            mousePress(key.getValue());
    }

    private void release(InputConstants.Key key) {
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
        if (!is_gui_mode) {
            if (last_lthumb_y < THUMB_DIGITAL_ACTIVATE && gamepad.lthumb_y >= THUMB_DIGITAL_ACTIVATE) {
                press(minecraft.options.keyUp.getKey());
            }
            if (last_lthumb_y >= THUMB_DIGITAL_DEACTIVATE && gamepad.lthumb_y < THUMB_DIGITAL_DEACTIVATE) {
                release(minecraft.options.keyUp.getKey());
            }
            if (-last_lthumb_y < THUMB_DIGITAL_ACTIVATE && -gamepad.lthumb_y >= THUMB_DIGITAL_ACTIVATE) {
                press(minecraft.options.keyDown.getKey());
            }
            if (-last_lthumb_y >= THUMB_DIGITAL_DEACTIVATE && -gamepad.lthumb_y < THUMB_DIGITAL_DEACTIVATE) {
                release(minecraft.options.keyDown.getKey());
            }
            if (last_lthumb_x < THUMB_DIGITAL_ACTIVATE && gamepad.lthumb_x >= THUMB_DIGITAL_ACTIVATE) {
                press(minecraft.options.keyRight.getKey());
            }
            if (last_lthumb_x >= THUMB_DIGITAL_DEACTIVATE && gamepad.lthumb_x < THUMB_DIGITAL_DEACTIVATE) {
                release(minecraft.options.keyRight.getKey());
            }
            if (-last_lthumb_x < THUMB_DIGITAL_ACTIVATE && -gamepad.lthumb_x >= THUMB_DIGITAL_ACTIVATE) {
                press(minecraft.options.keyLeft.getKey());
            }
            if (-last_lthumb_x >= THUMB_DIGITAL_DEACTIVATE && -gamepad.lthumb_x < THUMB_DIGITAL_DEACTIVATE) {
                release(minecraft.options.keyLeft.getKey());
            }
        }
        last_lthumb_x = gamepad.lthumb_x;
        last_lthumb_y = gamepad.lthumb_y;

        // mouse cursor
        if (rpad_menu == null) {
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

        // release sneak latch if a GUI opens
        if (is_gui_mode && sneak_is_latched) {
            if (!manually_sneaking) {
                LOGGER.debug("unSNEAK because unlatching because GUI");
                release(minecraft.options.keyShift.getKey());
            } else {
                LOGGER.debug("ignoring unSNEAK because manually sneaking while GUI");
            }
            sneak_is_latched = false;
        }
        // workarounds for if sneak key isn't shift
        if (minecraft.options.keyShift.getKey().getValue() != GLFW_KEY_LEFT_SHIFT) {
            if (manually_sneaking) {
                if (is_gui_mode && !last_was_gui_mode) {
                    LOGGER.debug("entering GUI while sneak/shift held");
                    release(minecraft.options.keyShift.getKey());
                    keyboardPress(GLFW_KEY_LEFT_SHIFT);
                }
                if (!is_gui_mode && last_was_gui_mode) {
                    LOGGER.debug("exiting GUI while sneak/shift held");
                    keyboardRelease(GLFW_KEY_LEFT_SHIFT);
                    press(minecraft.options.keyShift.getKey());
                }
            }
        }

        // key repeat
        if (scroll_down_repeat_time > 0) {
            scroll_down_repeat_time -= (current_nanos - last_nanos);
            if (scroll_down_repeat_time <= 0) {
                minecraft.mouseHandler.onScroll(minecraft.getWindow().getWindow(), 0, -1);
                scroll_down_repeat_time = KEY_REPEAT_REPEAT_TIME_NANOS;
            }
        }
        if (scroll_up_repeat_time > 0) {
            scroll_up_repeat_time -= (current_nanos - last_nanos);
            if (scroll_up_repeat_time <= 0) {
                minecraft.mouseHandler.onScroll(minecraft.getWindow().getWindow(), 0, 1);
                scroll_up_repeat_time = KEY_REPEAT_REPEAT_TIME_NANOS;
            }
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
            for (SimpleButtonMapping mapping : simpleMappings) {
                if (mapping.keyConflictContext.isActive()) {
                    if ((keyevent & mapping.buttonBitfield) != 0) {
                        if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0) {
                            press(mapping.keyMapping);
                        } else {
                            release(mapping.keyMapping);
                        }
                        // mark this as active so we don't try to press it again later
                        mapping.was_active = true;
                    }
                }
            }

            if ((keyevent & CONTROLS_GPB_LCLICK) != 0) {
                if (!is_gui_mode) {
                    if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0) {
                        mousePress(GLFW_MOUSE_BUTTON_1);
                    } else {
                        mouseRelease(GLFW_MOUSE_BUTTON_1);
                    }
                }
            }
            if ((keyevent & CONTROLS_GPB_JUMP) != 0) {
                if (!is_gui_mode) {
                    if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                        press(minecraft.options.keyJump.getKey());
                    else
                        release(minecraft.options.keyJump.getKey());
                }
            }
            if ((keyevent & CONTROLS_GPB_MCLICK) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                    mousePress(GLFW_MOUSE_BUTTON_3);
                else
                    mouseRelease(GLFW_MOUSE_BUTTON_3);
            }
            if ((keyevent & CONTROLS_GPB_SWAPHAND) != 0) {
                if (!is_gui_mode) {
                    if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                        press(minecraft.options.keySwapOffhand.getKey());
                    else
                        release(minecraft.options.keySwapOffhand.getKey());
                }
            }
            if ((keyevent & CONTROLS_GPB_DROPITEM) != 0) {
                if (!is_gui_mode) {
                    if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                        press(minecraft.options.keyDrop.getKey());
                    else
                        release(minecraft.options.keyDrop.getKey());
                }
            }
            if ((keyevent & CONTROLS_GPB_SWAPHAND_GUI)  != 0) {
                if (is_gui_mode) {
                    if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                        press(minecraft.options.keySwapOffhand.getKey());
                    else
                        release(minecraft.options.keySwapOffhand.getKey());
                }
            }
            if ((keyevent & CONTROLS_GPB_DROPITEM_GUI) != 0) {
                if (is_gui_mode) {
                    if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                        press(minecraft.options.keyDrop.getKey());
                    else
                        release(minecraft.options.keyDrop.getKey());
                }
            }
            if ((keyevent & CONTROLS_GPB_ESCAPE) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                    keyboardPress(GLFW_KEY_ESCAPE);
                else
                    keyboardRelease(GLFW_KEY_ESCAPE);
            }
            if ((keyevent & CONTROLS_GPB_ESCAPEALT) != 0) {
                if (is_gui_mode) {
                    if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                        keyboardPress(GLFW_KEY_ESCAPE);
                    else
                        keyboardRelease(GLFW_KEY_ESCAPE);
                }
            }
            if ((keyevent & CONTROLS_GPB_INVENTORY) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0) {
                    if (!is_gui_mode) {
                        // no gui
                        LOGGER.debug("NO GUI e DOWN");
                        last_btn_view_down_was_e = true;
                        press(minecraft.options.keyInventory.getKey());
                    } else {
                        // gui
                        LOGGER.debug("YES GUI ESC DOWN");
                        last_btn_view_down_was_e = false;
                        keyboardPress(GLFW_KEY_ESCAPE);
                    }
                } else {
                    if (last_btn_view_down_was_e) {
                        // no gui
                        LOGGER.debug("NO GUI e UP");
                        release(minecraft.options.keyInventory.getKey());
                    } else {
                        // gui
                        LOGGER.debug("YES GUI ESC UP");
                        keyboardRelease(GLFW_KEY_ESCAPE);
                    }
                }
            }
            if ((keyevent & CONTROLS_GPB_LCTRL) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                    keyboardPress(GLFW_KEY_LEFT_CONTROL);
                else
                    keyboardRelease(GLFW_KEY_LEFT_CONTROL);
            }
            if ((keyevent & CONTROLS_GPB_LALT) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                    keyboardPress(GLFW_KEY_LEFT_ALT);
                else
                    keyboardRelease(GLFW_KEY_LEFT_ALT);
            }
            if ((keyevent & CONTROLS_GPB_LSHIFTALT) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                    keyboardPress(GLFW_KEY_LEFT_SHIFT);
                else
                    keyboardRelease(GLFW_KEY_LEFT_SHIFT);
            }
            if ((keyevent & CONTROLS_GPB_RCLICK) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0) {
                    mousePress(GLFW_MOUSE_BUTTON_2);
                } else {
                    mouseRelease(GLFW_MOUSE_BUTTON_2);
                }
            }
            if ((keyevent & CONTROLS_GPB_GYROINHIBIT) != 0) {
                if (!is_gui_mode) {
                    if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                        gyro_is_enabled = false;
                    else
                        gyro_is_enabled = true;
                } else {
                    if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                        mousePress(GLFW_MOUSE_BUTTON_1);
                    else
                        mouseRelease(GLFW_MOUSE_BUTTON_1);
                }
            }
            if ((keyevent & CONTROLS_GPB_LCLICKALT) != 0) {
                if (!touch_keyboard_active) {
                    if (is_gui_mode) {
                        if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                            mousePress(GLFW_MOUSE_BUTTON_1);
                        else
                            mouseRelease(GLFW_MOUSE_BUTTON_1);
                    }
                }
            }
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
            if ((keyevent & CONTROLS_GPB_CLICK_MODESWITCH) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0) {
                    touch_keyboard_active = !touch_keyboard_active;
                    lpad_menu_selection = -1;
                    rpad_menu_selection = -1;
                    touchKeyboard.resetState();
                }
            }
            if ((keyevent & CONTROLS_GPB_SCROLL_UP) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0) {
                    if (is_gui_mode) {
                        scroll_up_repeat_time = KEY_REPEAT_ACTIVATE_TIME_NANOS;
                        minecraft.mouseHandler.onScroll(minecraft.getWindow().getWindow(), 0, 1);
                    }
                } else {
                    scroll_up_repeat_time = -1;
                }
            }
            if ((keyevent & CONTROLS_GPB_SCROLL_DOWN) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0) {
                    if (is_gui_mode) {
                        scroll_down_repeat_time = KEY_REPEAT_ACTIVATE_TIME_NANOS;
                        minecraft.mouseHandler.onScroll(minecraft.getWindow().getWindow(), 0, -1);
                    }
                } else {
                    scroll_down_repeat_time = -1;
                }
            }
            if ((keyevent & CONTROLS_GPB_SCROLL_LEFT) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0) {
                    if (!is_gui_mode) {
                        scroll_up_repeat_time = KEY_REPEAT_ACTIVATE_TIME_NANOS;
                        minecraft.mouseHandler.onScroll(minecraft.getWindow().getWindow(), 0, 1);
                    }
                } else {
                    scroll_up_repeat_time = -1;
                }
            }
            if ((keyevent & CONTROLS_GPB_SCROLL_RIGHT) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0) {
                    if (!is_gui_mode) {
                        scroll_down_repeat_time = KEY_REPEAT_ACTIVATE_TIME_NANOS;
                        minecraft.mouseHandler.onScroll(minecraft.getWindow().getWindow(), 0, -1);
                    }
                } else {
                    scroll_down_repeat_time = -1;
                }
            }
            if ((keyevent & CONTROLS_GPB_HOLDSNEAK) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0) {
                    manually_sneaking = true;
                    if (!sneak_is_latched) {
                        LOGGER.debug("SNEAK because manual");
                        if (!is_gui_mode)
                            press(minecraft.options.keyShift.getKey());
                        else
                            keyboardPress(GLFW_KEY_LEFT_SHIFT);
                    } else {
                        LOGGER.debug("ignoring SNEAK because already latched");
                    }
                } else {
                    manually_sneaking = false;
                    if (!sneak_is_latched || !sneak_latched_while_manually_sneaking) {
                        if (sneak_is_latched) {
                            if (MODE_SWITCH_BEEP_LEN > 0)
                                DeckControls.INPUT.beep(MODE_SWITCH_BEEP_FREQ, MODE_SWITCH_BEEP_LEN);
                        }
                        sneak_is_latched = false;
                        LOGGER.debug("unSNEAK!!");
                        if (!is_gui_mode)
                            release(minecraft.options.keyShift.getKey());
                        else
                            keyboardRelease(GLFW_KEY_LEFT_SHIFT);
                    }
                    sneak_latched_while_manually_sneaking = false;
                }
            }
            if ((keyevent & CONTROLS_GPB_TOGGLESNEAK) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0) {
                    if (!is_gui_mode) {
                        if (MODE_SWITCH_BEEP_LEN > 0)
                            DeckControls.INPUT.beep(MODE_SWITCH_BEEP_FREQ, MODE_SWITCH_BEEP_LEN);
                        sneak_is_latched = !sneak_is_latched;
                        if (sneak_is_latched) {
                            sneak_latched_while_manually_sneaking = manually_sneaking;
                            if (!manually_sneaking) {
                                LOGGER.debug("SNEAK because latching");
                                press(minecraft.options.keyShift.getKey());
                            } else {
                                LOGGER.debug("ignoring SNEAK because already sneaking");
                            }
                        } else {
                            if (!manually_sneaking) {
                                LOGGER.debug("unSNEAK because unlatching");
                                release(minecraft.options.keyShift.getKey());
                            } else {
                                LOGGER.debug("ignoring unSNEAK because manually sneaking");
                            }
                        }
                    } else {
                        mousePress(GLFW_MOUSE_BUTTON_3);
                    }
                } else {
                    if (is_gui_mode)
                        mouseRelease(GLFW_MOUSE_BUTTON_3);
                }
            }
        }

        // if the mode has changed, we have to unpress/press keys as appropriate
        for (SimpleButtonMapping mapping : simpleMappings) {
            if ((buttonsPressedCache & mapping.buttonBitfield) != 0) {
                if (mapping.keyConflictContext.isActive() && !mapping.was_active) {
                    // mode became active
                    // (but physical button state didn't change)
                    press(mapping.keyMapping);
                }
                if (!mapping.keyConflictContext.isActive() && mapping.was_active) {
                    // mode became inactive
                    release(mapping.keyMapping);
                }
            }
            mapping.was_active = mapping.keyConflictContext.isActive();
        }

        // good gyro controls
        if (!is_gui_mode && minecraft.player != null) {
            double tot_turn_yaw = 0;
            double tot_turn_pitch = 0;

            if (gyro_is_enabled && ((gamepad.buttons & HidInput.GamepadButtons.BTN_RPAD_TOUCH) == 0)) {
                if (minecraft.player.isScoping()) {
                    tot_turn_yaw += -accumState.camYaw * GYRO_CAM_SENSITIVITY_SCOPE_X;
                    tot_turn_pitch += -accumState.camPitch * GYRO_CAM_SENSITIVITY_SCOPE_Y;
                } else {
                    tot_turn_yaw += -accumState.camYaw * GYRO_CAM_SENSITIVITY_X;
                    tot_turn_pitch += -accumState.camPitch * GYRO_CAM_SENSITIVITY_Y;
                }
            }

            if ((gamepad.rthumb_x * gamepad.rthumb_x + gamepad.rthumb_y * gamepad.rthumb_y > FLICK_STICK_ACTIVATE_DIST * FLICK_STICK_ACTIVATE_DIST) &&
                    (last_rthumb_x * last_rthumb_x + last_rthumb_y * last_rthumb_y <= FLICK_STICK_ACTIVATE_DIST * FLICK_STICK_ACTIVATE_DIST)) {
                LOGGER.debug("flick stick start");
                flick_stick_progress = 0;
                flick_stick_amount = Math.toDegrees(Math.atan2(gamepad.rthumb_x, gamepad.rthumb_y));
            } else if (gamepad.rthumb_x * gamepad.rthumb_x + gamepad.rthumb_y * gamepad.rthumb_y > FLICK_STICK_DEACTIVATE_DIST * FLICK_STICK_DEACTIVATE_DIST) {
                double cur_angle = Math.toDegrees(Math.atan2(gamepad.rthumb_x, gamepad.rthumb_y));
                double last_angle = Math.toDegrees(Math.atan2(last_rthumb_x, last_rthumb_y));

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
            } else if (last_rthumb_x * last_rthumb_x + last_rthumb_y * last_rthumb_y >= FLICK_STICK_DEACTIVATE_DIST * FLICK_STICK_DEACTIVATE_DIST) {
                LOGGER.debug("flick stick deactivate");
                for (int i = 0; i < flick_stick_smoothing.length; i++)
                    flick_stick_smoothing[i] = 0;
            }

            if (flick_stick_progress < FLICK_STICK_TIME_NANOS) {
                double last_flick_progress = (double)flick_stick_progress / FLICK_STICK_TIME_NANOS;
                flick_stick_progress = Math.min(flick_stick_progress + current_nanos - last_nanos, FLICK_STICK_TIME_NANOS);
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
        last_rthumb_x = gamepad.rthumb_x;
        last_rthumb_y = gamepad.rthumb_y;

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
