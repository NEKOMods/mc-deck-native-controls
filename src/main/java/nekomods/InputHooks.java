package nekomods;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import static org.lwjgl.glfw.GLFW.*;

public class InputHooks {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Minecraft minecraft;

    private int last_lthumb_x;
    private int last_lthumb_y;
    private boolean last_btn_view_down_was_e;
    boolean btn_b_is_right_click;
    boolean sneak_is_latched;
    boolean sneak_latched_while_manually_sneaking;
    boolean manually_sneaking;

    private static final float THUMB_DEADZONE = 5000;
    private static final float THUMB_ANALOG_FULLSCALE = 32700;
    private static final float THUMB_DIGITAL_ACTIVATE = 16000;
    private static final float THUMB_DIGITAL_DEACTIVATE = 15000;
    private static final double THUMB_SCALE_CAM_X = 1000;
    private static final double THUMB_SCALE_CAM_Y = 800;

    public InputHooks() {
        minecraft = Minecraft.getInstance();
    }
    public void runTick() {
        minecraft.getWindow().setErrorSection("DeckControlsMod");
        minecraft.getProfiler().push("deck_controls_mod");

        HidInput.OtherHidState gamepad = HidInput.latestInput;
        if (last_lthumb_y < THUMB_DIGITAL_ACTIVATE && gamepad.lthumb_y >= THUMB_DIGITAL_ACTIVATE) {
            LOGGER.info("FORWARD DOWN");
            minecraft.keyboardHandler.keyPress(
                    minecraft.getWindow().getWindow(),
                    minecraft.options.keyUp.getKey().getValue(),
                    glfwGetKeyScancode(minecraft.options.keyUp.getKey().getValue()),
                    GLFW_PRESS,
                    0);
        }
        if (last_lthumb_y >= THUMB_DIGITAL_DEACTIVATE && gamepad.lthumb_y < THUMB_DIGITAL_DEACTIVATE) {
            LOGGER.info("FORWARD UP");
            minecraft.keyboardHandler.keyPress(
                    minecraft.getWindow().getWindow(),
                    minecraft.options.keyUp.getKey().getValue(),
                    glfwGetKeyScancode(minecraft.options.keyUp.getKey().getValue()),
                    GLFW_RELEASE,
                    0);
        }
        if (-last_lthumb_y < THUMB_DIGITAL_ACTIVATE && -gamepad.lthumb_y >= THUMB_DIGITAL_ACTIVATE) {
            LOGGER.info("BACKWARD DOWN");
            minecraft.keyboardHandler.keyPress(
                    minecraft.getWindow().getWindow(),
                    minecraft.options.keyDown.getKey().getValue(),
                    glfwGetKeyScancode(minecraft.options.keyDown.getKey().getValue()),
                    GLFW_PRESS,
                    0);
        }
        if (-last_lthumb_y >= THUMB_DIGITAL_DEACTIVATE && -gamepad.lthumb_y < THUMB_DIGITAL_DEACTIVATE) {
            LOGGER.info("BACKWARD UP");
            minecraft.keyboardHandler.keyPress(
                    minecraft.getWindow().getWindow(),
                    minecraft.options.keyDown.getKey().getValue(),
                    glfwGetKeyScancode(minecraft.options.keyDown.getKey().getValue()),
                    GLFW_RELEASE,
                    0);
        }
        if (last_lthumb_x < THUMB_DIGITAL_ACTIVATE && gamepad.lthumb_x >= THUMB_DIGITAL_ACTIVATE) {
            LOGGER.info("RIGHT DOWN");
            minecraft.keyboardHandler.keyPress(
                    minecraft.getWindow().getWindow(),
                    minecraft.options.keyRight.getKey().getValue(),
                    glfwGetKeyScancode(minecraft.options.keyRight.getKey().getValue()),
                    GLFW_PRESS,
                    0);
        }
        if (last_lthumb_x >= THUMB_DIGITAL_DEACTIVATE && gamepad.lthumb_x < THUMB_DIGITAL_DEACTIVATE) {
            LOGGER.info("RIGHT UP");
            minecraft.keyboardHandler.keyPress(
                    minecraft.getWindow().getWindow(),
                    minecraft.options.keyRight.getKey().getValue(),
                    glfwGetKeyScancode(minecraft.options.keyRight.getKey().getValue()),
                    GLFW_RELEASE,
                    0);
        }
        if (-last_lthumb_x < THUMB_DIGITAL_ACTIVATE && -gamepad.lthumb_x >= THUMB_DIGITAL_ACTIVATE) {
            LOGGER.info("LEFT DOWN");
            minecraft.keyboardHandler.keyPress(
                    minecraft.getWindow().getWindow(),
                    minecraft.options.keyLeft.getKey().getValue(),
                    glfwGetKeyScancode(minecraft.options.keyLeft.getKey().getValue()),
                    GLFW_PRESS,
                    0);
        }
        if (-last_lthumb_x >= THUMB_DIGITAL_DEACTIVATE && -gamepad.lthumb_x < THUMB_DIGITAL_DEACTIVATE) {
            LOGGER.info("LEFT UP");
            minecraft.keyboardHandler.keyPress(
                    minecraft.getWindow().getWindow(),
                    minecraft.options.keyLeft.getKey().getValue(),
                    glfwGetKeyScancode(minecraft.options.keyLeft.getKey().getValue()),
                    GLFW_RELEASE,
                    0);
        }
        last_lthumb_x = gamepad.lthumb_x;
        last_lthumb_y = gamepad.lthumb_y;
        if (gamepad.rthumb_x * gamepad.rthumb_x + gamepad.rthumb_y * gamepad.rthumb_y > THUMB_DEADZONE * THUMB_DEADZONE) {
            minecraft.mouseHandler.onMove(
                minecraft.getWindow().getWindow(),
                minecraft.mouseHandler.xpos() + gamepad.rthumb_x / THUMB_SCALE_CAM_X,
                minecraft.mouseHandler.ypos() - gamepad.rthumb_y / THUMB_SCALE_CAM_Y
            );
        }

        HidInput.keyEvents.addLast(HidInput.GamepadButtons.FLAG_BARRIER);
        int keyevent;
        while ((keyevent = HidInput.keyEvents.removeFirst()) != HidInput.GamepadButtons.FLAG_BARRIER) {
            if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0) {
                LOGGER.info("KEY DOWN " + keyevent);

                if ((keyevent & HidInput.GamepadButtons.BTN_Y) != 0) {
                    minecraft.keyboardHandler.keyPress(
                            minecraft.getWindow().getWindow(),
                            minecraft.options.keyJump.getKey().getValue(),
                            glfwGetKeyScancode(minecraft.options.keyJump.getKey().getValue()),
                            GLFW_PRESS,
                            0);
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_OPTIONS) != 0) {
                    minecraft.keyboardHandler.keyPress(
                            minecraft.getWindow().getWindow(),
                            GLFW_KEY_ESCAPE,
                            glfwGetKeyScancode(GLFW_KEY_ESCAPE),
                            GLFW_PRESS,
                            0);
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_VIEW) != 0) {
                    if (minecraft.screen == null) {
                        // no gui
                        LOGGER.info("NO GUI e DOWN");
                        last_btn_view_down_was_e = true;
                        minecraft.keyboardHandler.keyPress(
                                minecraft.getWindow().getWindow(),
                                minecraft.options.keyInventory.getKey().getValue(),
                                glfwGetKeyScancode(minecraft.options.keyInventory.getKey().getValue()),
                                GLFW_PRESS,
                                0);
                    } else {
                        // gui
                        LOGGER.info("YES GUI ESC DOWN");
                        last_btn_view_down_was_e = false;
                        minecraft.keyboardHandler.keyPress(
                                minecraft.getWindow().getWindow(),
                                GLFW_KEY_ESCAPE,
                                glfwGetKeyScancode(GLFW_KEY_ESCAPE),
                                GLFW_PRESS,
                                0);
                    }
                }
                // TODO: repeat?
                if ((keyevent & HidInput.GamepadButtons.BTN_D_LEFT) != 0) {
                    minecraft.mouseHandler.onScroll(minecraft.getWindow().getWindow(), 0, 1);
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_D_RIGHT) != 0) {
                    minecraft.mouseHandler.onScroll(minecraft.getWindow().getWindow(), 0, -1);
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_B) != 0) {
                    if (!btn_b_is_right_click) {
                        minecraft.mouseHandler.onPress(
                                minecraft.getWindow().getWindow(),
                                GLFW_MOUSE_BUTTON_1,
                                GLFW_PRESS,
                                0);
                    } else {
                        minecraft.mouseHandler.onPress(
                                minecraft.getWindow().getWindow(),
                                GLFW_MOUSE_BUTTON_2,
                                GLFW_PRESS,
                                0);
                    }
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_X) != 0) {
                    if (!btn_b_is_right_click) {
                        minecraft.mouseHandler.onPress(
                                minecraft.getWindow().getWindow(),
                                GLFW_MOUSE_BUTTON_2,
                                GLFW_PRESS,
                                0);
                    } else {
                        minecraft.mouseHandler.onPress(
                                minecraft.getWindow().getWindow(),
                                GLFW_MOUSE_BUTTON_1,
                                GLFW_PRESS,
                                0);
                    }
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_A) != 0) {
                    minecraft.keyboardHandler.keyPress(
                            minecraft.getWindow().getWindow(),
                            minecraft.options.keySprint.getKey().getValue(),
                            glfwGetKeyScancode(minecraft.options.keySprint.getKey().getValue()),
                            GLFW_PRESS,
                            0);
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_R4) != 0) {
                    // TODO: sound/haptics?
                    btn_b_is_right_click = !btn_b_is_right_click;
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_LT_ANALOG_FULL) != 0) {
                    manually_sneaking = true;
                    if (!sneak_is_latched) {
                        LOGGER.info("SNEAK because manual");
                        minecraft.keyboardHandler.keyPress(
                                minecraft.getWindow().getWindow(),
                                minecraft.options.keyShift.getKey().getValue(),
                                glfwGetKeyScancode(minecraft.options.keyShift.getKey().getValue()),
                                GLFW_PRESS,
                                0);
                    } else {
                        LOGGER.info("ignoring SNEAK because already latched");
                    }
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_LT_DIGITAL) != 0) {
                    // TODO: sound/haptics?
                    sneak_is_latched = !sneak_is_latched;
                    if (sneak_is_latched) {
                        sneak_latched_while_manually_sneaking = manually_sneaking;
                        if (!manually_sneaking) {
                            LOGGER.info("SNEAK because latching");
                            minecraft.keyboardHandler.keyPress(
                                    minecraft.getWindow().getWindow(),
                                    minecraft.options.keyShift.getKey().getValue(),
                                    glfwGetKeyScancode(minecraft.options.keyShift.getKey().getValue()),
                                    GLFW_PRESS,
                                    0);
                        } else {
                            LOGGER.info("ignoring SNEAK because already sneaking");
                        }
                    } else {
                        if (!manually_sneaking) {
                            LOGGER.info("unSNEAK because unlatching");
                            minecraft.keyboardHandler.keyPress(
                                    minecraft.getWindow().getWindow(),
                                    minecraft.options.keyShift.getKey().getValue(),
                                    glfwGetKeyScancode(minecraft.options.keyShift.getKey().getValue()),
                                    GLFW_RELEASE,
                                    0);
                        } else {
                            LOGGER.info("ignoring unSNEAK because manually sneaking");
                        }
                    }
                }
            } else {
                LOGGER.info("KEY UP " + (keyevent & (~HidInput.GamepadButtons.FLAG_BTN_UP)));

                if ((keyevent & HidInput.GamepadButtons.BTN_Y) != 0) {
                    minecraft.keyboardHandler.keyPress(
                            minecraft.getWindow().getWindow(),
                            minecraft.options.keyJump.getKey().getValue(),
                            glfwGetKeyScancode(minecraft.options.keyJump.getKey().getValue()),
                            GLFW_RELEASE,
                            0);
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_OPTIONS) != 0) {
                    minecraft.keyboardHandler.keyPress(
                            minecraft.getWindow().getWindow(),
                            GLFW_KEY_ESCAPE,
                            glfwGetKeyScancode(GLFW_KEY_ESCAPE),
                            GLFW_RELEASE,
                            0);
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_VIEW) != 0) {
                    if (last_btn_view_down_was_e) {
                        // no gui
                        LOGGER.info("NO GUI e UP");
                        minecraft.keyboardHandler.keyPress(
                                minecraft.getWindow().getWindow(),
                                minecraft.options.keyInventory.getKey().getValue(),
                                glfwGetKeyScancode(minecraft.options.keyInventory.getKey().getValue()),
                                GLFW_RELEASE,
                                0);
                    } else {
                        // gui
                        LOGGER.info("YES GUI ESC UP");
                        minecraft.keyboardHandler.keyPress(
                                minecraft.getWindow().getWindow(),
                                GLFW_KEY_ESCAPE,
                                glfwGetKeyScancode(GLFW_KEY_ESCAPE),
                                GLFW_RELEASE,
                                0);
                    }
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_B) != 0) {
                    if (!btn_b_is_right_click) {
                        minecraft.mouseHandler.onPress(
                                minecraft.getWindow().getWindow(),
                                GLFW_MOUSE_BUTTON_1,
                                GLFW_RELEASE,
                                0);
                    } else {
                        minecraft.mouseHandler.onPress(
                                minecraft.getWindow().getWindow(),
                                GLFW_MOUSE_BUTTON_2,
                                GLFW_RELEASE,
                                0);
                    }
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_X) != 0) {
                    if (!btn_b_is_right_click) {
                        minecraft.mouseHandler.onPress(
                                minecraft.getWindow().getWindow(),
                                GLFW_MOUSE_BUTTON_2,
                                GLFW_RELEASE,
                                0);
                    } else {
                        minecraft.mouseHandler.onPress(
                                minecraft.getWindow().getWindow(),
                                GLFW_MOUSE_BUTTON_1,
                                GLFW_RELEASE,
                                0);
                    }
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_A) != 0) {
                    minecraft.keyboardHandler.keyPress(
                            minecraft.getWindow().getWindow(),
                            minecraft.options.keySprint.getKey().getValue(),
                            glfwGetKeyScancode(minecraft.options.keySprint.getKey().getValue()),
                            GLFW_RELEASE,
                            0);
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_LT_ANALOG_FULL) != 0) {
                    // TODO: sound/haptics?
                    manually_sneaking = false;
                    if (!sneak_is_latched || !sneak_latched_while_manually_sneaking) {
                        sneak_is_latched = false;
                        LOGGER.info("unSNEAK!!");
                        minecraft.keyboardHandler.keyPress(
                                minecraft.getWindow().getWindow(),
                                minecraft.options.keyShift.getKey().getValue(),
                                glfwGetKeyScancode(minecraft.options.keyShift.getKey().getValue()),
                                GLFW_RELEASE,
                                0);
                    }
                    sneak_latched_while_manually_sneaking = false;
                }
            }
        }

        minecraft.getProfiler().pop();
    }

    public float fbImpulse(float keyboardImpulse) {
        HidInput.OtherHidState gamepad = HidInput.latestInput;
        if (gamepad.lthumb_x * gamepad.lthumb_x + gamepad.lthumb_y * gamepad.lthumb_y > THUMB_DEADZONE * THUMB_DEADZONE) {
            float ret = gamepad.lthumb_y / THUMB_ANALOG_FULLSCALE;
            if (ret > 1) ret = 1;
            if (ret < -1) ret = -1;
            return ret;
        } else {
            return keyboardImpulse;
        }
    }

    public float lrImpulse(float keyboardImpulse) {
        HidInput.OtherHidState gamepad = HidInput.latestInput;
        if (gamepad.lthumb_x * gamepad.lthumb_x + gamepad.lthumb_y * gamepad.lthumb_y > THUMB_DEADZONE * THUMB_DEADZONE) {
            float ret = gamepad.lthumb_x / -THUMB_ANALOG_FULLSCALE;
            if (ret > 1) ret = 1;
            if (ret < -1) ret = -1;
            return ret;
        } else {
            return keyboardImpulse;
        }
    }

    public static void runTickHook() {
        if (DeckControls.HOOKS != null) {
            DeckControls.HOOKS.runTick();
        }
    }

    public static float playerFBImpulse(float keyboardImpulse) {
        return DeckControls.HOOKS.fbImpulse(keyboardImpulse);
    }

    public static float playerLRImpulse(float keyboardImpulse) {
        return DeckControls.HOOKS.lrImpulse(keyboardImpulse);
    }
}
