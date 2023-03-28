package nekomods;

import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import static org.lwjgl.glfw.GLFW.*;

public class InputHooks {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Minecraft minecraft;

    private long last_nanos;
    private int last_lthumb_x;
    private int last_lthumb_y;
    private int last_rthumb_x;
    private int last_rthumb_y;
    private boolean last_btn_view_down_was_e;
    boolean btn_b_is_right_click;
    boolean sneak_is_latched;
    boolean sneak_latched_while_manually_sneaking;
    boolean manually_sneaking;
    private boolean gyro_is_enabled = true;
    private long flick_stick_progress = FLICK_STICK_TIME_NANOS;
    private double flick_stick_amount;

    private static final float THUMB_DEADZONE = 5000;
    private static final float THUMB_ANALOG_FULLSCALE = 32700;
    private static final float THUMB_DIGITAL_ACTIVATE = 16000;
    private static final float THUMB_DIGITAL_DEACTIVATE = 15000;
    private static final double THUMB_SCALE_CAM_X = 1000;
    private static final double THUMB_SCALE_CAM_Y = 800;
    private static final double MODE_SWITCH_BEEP_FREQ = 1000;
    private static final double MODE_SWITCH_BEEP_LEN = 0.1;
    private static final double GYRO_CAM_SENSITIVITY_X = 2;
    private static final double GYRO_CAM_SENSITIVITY_Y = 2;
    private static final double GYRO_CAM_SENSITIVITY_SCOPE_X = 0.5;
    private static final double GYRO_CAM_SENSITIVITY_SCOPE_Y = 0.5;
    private static final double FLICK_STICK_ACTIVATE_DIST = 29000;
    private static final double FLICK_STICK_DEACTIVATE_DIST = 28000;
    private static final long FLICK_STICK_TIME_NANOS = 100000000;

    public InputHooks() {
        minecraft = Minecraft.getInstance();
    }
    public void runTick() {
        minecraft.getWindow().setErrorSection("DeckControlsMod");
        minecraft.getProfiler().push("deck_controls_mod");

        long current_nanos = Util.getNanos();

        HidInput.OtherHidState gamepad = DeckControls.INPUT.latestInput;
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

        DeckControls.INPUT.keyEvents.addLast(HidInput.GamepadButtons.FLAG_BARRIER);
        int keyevent;
        while ((keyevent = DeckControls.INPUT.keyEvents.removeFirst()) != HidInput.GamepadButtons.FLAG_BARRIER) {
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
                    DeckControls.INPUT.beep(MODE_SWITCH_BEEP_FREQ, MODE_SWITCH_BEEP_LEN);
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
                    DeckControls.INPUT.beep(MODE_SWITCH_BEEP_FREQ, MODE_SWITCH_BEEP_LEN);
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
                if ((keyevent & HidInput.GamepadButtons.BTN_D_UP) != 0) {
                    minecraft.keyboardHandler.keyPress(
                            minecraft.getWindow().getWindow(),
                            minecraft.options.keySwapOffhand.getKey().getValue(),
                            glfwGetKeyScancode(minecraft.options.keySwapOffhand.getKey().getValue()),
                            GLFW_PRESS,
                            0);
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_D_DOWN) != 0) {
                    minecraft.keyboardHandler.keyPress(
                            minecraft.getWindow().getWindow(),
                            minecraft.options.keyDrop.getKey().getValue(),
                            glfwGetKeyScancode(minecraft.options.keyDrop.getKey().getValue()),
                            GLFW_PRESS,
                            0);
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_RT_DIGITAL) != 0) {
                    minecraft.mouseHandler.onPress(
                            minecraft.getWindow().getWindow(),
                            GLFW_MOUSE_BUTTON_3,
                            GLFW_PRESS,
                            0);
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_RT_ANALOG_FULL) != 0) {
                    gyro_is_enabled = false;
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
                    manually_sneaking = false;
                    if (!sneak_is_latched || !sneak_latched_while_manually_sneaking) {
                        if (sneak_is_latched) {
                            DeckControls.INPUT.beep(MODE_SWITCH_BEEP_FREQ, MODE_SWITCH_BEEP_LEN);
                        }
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
                if ((keyevent & HidInput.GamepadButtons.BTN_D_UP) != 0) {
                    minecraft.keyboardHandler.keyPress(
                            minecraft.getWindow().getWindow(),
                            minecraft.options.keySwapOffhand.getKey().getValue(),
                            glfwGetKeyScancode(minecraft.options.keySwapOffhand.getKey().getValue()),
                            GLFW_RELEASE,
                            0);
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_D_DOWN) != 0) {
                    minecraft.keyboardHandler.keyPress(
                            minecraft.getWindow().getWindow(),
                            minecraft.options.keyDrop.getKey().getValue(),
                            glfwGetKeyScancode(minecraft.options.keyDrop.getKey().getValue()),
                            GLFW_RELEASE,
                            0);
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_RT_DIGITAL) != 0) {
                    minecraft.mouseHandler.onPress(
                            minecraft.getWindow().getWindow(),
                            GLFW_MOUSE_BUTTON_3,
                            GLFW_RELEASE,
                            0);
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_RT_ANALOG_FULL) != 0) {
                    gyro_is_enabled = true;
                }
            }
        }

        HidInput.AccumHidState accumState = DeckControls.INPUT.accumInput.getAndSet(new HidInput.AccumHidState());
        if (minecraft.screen == null && minecraft.player != null) {
            double tot_turn_yaw = 0;
            double tot_turn_pitch = 0;

            if (gyro_is_enabled) {
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
                LOGGER.info("flick stick start");
                flick_stick_progress = 0;
                flick_stick_amount = Math.toDegrees(Math.atan2(gamepad.rthumb_x, gamepad.rthumb_y));
            } else if (gamepad.rthumb_x * gamepad.rthumb_x + gamepad.rthumb_y * gamepad.rthumb_y > FLICK_STICK_DEACTIVATE_DIST * FLICK_STICK_DEACTIVATE_DIST) {
//                LOGGER.info("flick stick turn");
                double cur_angle = Math.toDegrees(Math.atan2(gamepad.rthumb_x, gamepad.rthumb_y));
                double last_angle = Math.toDegrees(Math.atan2(last_rthumb_x, last_rthumb_y));

                double diff_angle = cur_angle - last_angle;
                if (diff_angle < -180) diff_angle += 360;
                if (diff_angle > 180) diff_angle -= 360;

                // TODO: smoothing?
                tot_turn_yaw += diff_angle;
            } else {
//                LOGGER.info("flick stick deactivate");
            }

            if (flick_stick_progress < FLICK_STICK_TIME_NANOS) {
                double last_flick_progress = (double)flick_stick_progress / FLICK_STICK_TIME_NANOS;
                flick_stick_progress = Math.min(flick_stick_progress + current_nanos - last_nanos, FLICK_STICK_TIME_NANOS);
                LOGGER.info("flicking progress raw " + flick_stick_progress);
                double current_flick_progress = (double)flick_stick_progress / FLICK_STICK_TIME_NANOS;

                // TODO: ease?
                LOGGER.info("flicking progress " + current_flick_progress);
                tot_turn_yaw += (current_flick_progress - last_flick_progress) * flick_stick_amount;
            }

            if (tot_turn_yaw != 0 || tot_turn_pitch != 0) {
                minecraft.player.turn(
                        tot_turn_yaw / 0.15,
                        tot_turn_pitch / 0.15);
            }
        }
//        if (gamepad.rthumb_x * gamepad.rthumb_x + gamepad.rthumb_y * gamepad.rthumb_y > THUMB_DEADZONE * THUMB_DEADZONE) {
//            minecraft.mouseHandler.onMove(
//                    minecraft.getWindow().getWindow(),
//                    minecraft.mouseHandler.xpos() + gamepad.rthumb_x / THUMB_SCALE_CAM_X,
//                    minecraft.mouseHandler.ypos() - gamepad.rthumb_y / THUMB_SCALE_CAM_Y
//            );
//        }
        last_rthumb_x = gamepad.rthumb_x;
        last_rthumb_y = gamepad.rthumb_y;

        last_nanos = current_nanos;

        minecraft.getProfiler().pop();
    }

    public float fbImpulse(float keyboardImpulse) {
        HidInput.OtherHidState gamepad = DeckControls.INPUT.latestInput;
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
        HidInput.OtherHidState gamepad = DeckControls.INPUT.latestInput;
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
