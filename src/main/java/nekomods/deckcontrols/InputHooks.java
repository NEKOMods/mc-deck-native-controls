package nekomods.deckcontrols;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.util.Arrays;

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
    private boolean manually_sneaking;
    private boolean gyro_is_enabled = true;
    private long flick_stick_progress = FLICK_STICK_TIME_NANOS;
    private double flick_stick_amount;
    private double[] flick_stick_smoothing = new double[16];
    private int flick_stick_smoothing_i;

    private static final float THUMB_DEADZONE = 5000;
    private static final float THUMB_ANALOG_FULLSCALE = 32700;
    private static final float THUMB_DIGITAL_ACTIVATE = 16000;
    private static final float THUMB_DIGITAL_DEACTIVATE = 15000;
    private static final double RPAD_MOUSE_SCALE_X = 50;
    private static final double RPAD_MOUSE_SCALE_Y = 80;
    private static final double MODE_SWITCH_BEEP_FREQ = 1000;
    private static final double MODE_SWITCH_BEEP_LEN = 0.1;
    private static final double GYRO_CAM_SENSITIVITY_X = 2;
    private static final double GYRO_CAM_SENSITIVITY_Y = 2;
    private static final double GYRO_CAM_SENSITIVITY_SCOPE_X = 0.5;
    private static final double GYRO_CAM_SENSITIVITY_SCOPE_Y = 0.5;
    private static final double FLICK_STICK_ACTIVATE_DIST = 29000;
    private static final double FLICK_STICK_DEACTIVATE_DIST = 28000;
    private static final long FLICK_STICK_TIME_NANOS = 100000000;
    private static final double FLICK_STICK_SMOOTH_THRESH = 0.1;

    private static double flickStickEase(double input) {
        double flipped = 1 - input;
        return 1 - flipped * flipped;
    }

    private double flickSmoothed(double input) {
        flick_stick_smoothing[flick_stick_smoothing_i] = input;
        flick_stick_smoothing_i = (flick_stick_smoothing_i + 1) % flick_stick_smoothing.length;
        return Arrays.stream(flick_stick_smoothing).average().orElse(0);
    }

    public InputHooks() {
        minecraft = Minecraft.getInstance();
    }

    private void press(InputConstants.Key key) {
        minecraft.keyboardHandler.keyPress(
                minecraft.getWindow().getWindow(),
                key.getValue(),
                glfwGetKeyScancode(key.getValue()),
                GLFW_PRESS,
                0);
    }

    private void press(int key) {
        minecraft.keyboardHandler.keyPress(
                minecraft.getWindow().getWindow(),
                key,
                glfwGetKeyScancode(key),
                GLFW_PRESS,
                0);
    }

    private void release(InputConstants.Key key) {
        minecraft.keyboardHandler.keyPress(
                minecraft.getWindow().getWindow(),
                key.getValue(),
                glfwGetKeyScancode(key.getValue()),
                GLFW_RELEASE,
                0);
    }

    private void release(int key) {
        minecraft.keyboardHandler.keyPress(
                minecraft.getWindow().getWindow(),
                key,
                glfwGetKeyScancode(key),
                GLFW_RELEASE,
                0);
    }

    private void mousePress(int button) {
        minecraft.mouseHandler.onPress(
                minecraft.getWindow().getWindow(),
                button,
                GLFW_PRESS,
                0);
    }

    private void mouseRelease(int button) {
        minecraft.mouseHandler.onPress(
                minecraft.getWindow().getWindow(),
                button,
                GLFW_RELEASE,
                0);
    }

    public void runTick() {
        minecraft.getWindow().setErrorSection("DeckControlsMod");
        minecraft.getProfiler().push("deck_controls_mod");

        long current_nanos = Util.getNanos();
        boolean is_gui_mode = minecraft.screen != null;

        HidInput.AccumHidState accumState = DeckControls.INPUT.accumInput.getAndSet(new HidInput.AccumHidState());
        HidInput.OtherHidState gamepad = DeckControls.INPUT.latestInput;

        // movement keys (backup only, for e.g. boats)
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
        last_lthumb_x = gamepad.lthumb_x;
        last_lthumb_y = gamepad.lthumb_y;

        // mouse cursor
        if (accumState.mouseDX != 0 || accumState.mouseDY != 0) {
            if (!is_gui_mode) {
                minecraft.mouseHandler.onMove(
                        minecraft.getWindow().getWindow(),
                        minecraft.mouseHandler.xpos() + accumState.mouseDX / RPAD_MOUSE_SCALE_X,
                        minecraft.mouseHandler.ypos() - accumState.mouseDY / RPAD_MOUSE_SCALE_Y
                );
            } else {
                // WTF?
                double mouse_final_dx = accumState.mouseDX;
                double mouse_final_dy = accumState.mouseDY;
                if (Math.abs(mouse_final_dx) < 1)
                    mouse_final_dx = 0;
                if (Math.abs(mouse_final_dy) < 1)
                    mouse_final_dy = 0;
                if (mouse_final_dx != 0 || mouse_final_dy != 0) {
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
                            curX[0] + mouse_final_dx / RPAD_MOUSE_SCALE_X,
                            curY[0] - mouse_final_dy / RPAD_MOUSE_SCALE_Y
                    );
                    minecraft.mouseHandler.onMove(
                            minecraft.getWindow().getWindow(),
                            curX[0] + mouse_final_dx / RPAD_MOUSE_SCALE_X,
                            curY[0] - mouse_final_dy / RPAD_MOUSE_SCALE_Y
                    );
                }
            }
        }

        DeckControls.INPUT.keyEvents.addLast(HidInput.GamepadButtons.FLAG_BARRIER);
        int keyevent;
        while ((keyevent = DeckControls.INPUT.keyEvents.removeFirst()) != HidInput.GamepadButtons.FLAG_BARRIER) {
            // boring keys
            if ((keyevent & HidInput.GamepadButtons.BTN_A) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                    press(minecraft.options.keySprint.getKey());
                else
                    release(minecraft.options.keySprint.getKey());
            }
            if ((keyevent & HidInput.GamepadButtons.BTN_B) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0) {
                    if (!btn_b_is_right_click)
                        mousePress(GLFW_MOUSE_BUTTON_1);
                    else
                        mousePress(GLFW_MOUSE_BUTTON_2);
                } else {
                    if (!btn_b_is_right_click)
                        mouseRelease(GLFW_MOUSE_BUTTON_1);
                    else
                        mouseRelease(GLFW_MOUSE_BUTTON_2);
                }
            }
            if ((keyevent & HidInput.GamepadButtons.BTN_X) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0) {
                    if (!btn_b_is_right_click)
                        mousePress(GLFW_MOUSE_BUTTON_2);
                    else
                        mousePress(GLFW_MOUSE_BUTTON_1);
                } else {
                    if (!btn_b_is_right_click)
                        mouseRelease(GLFW_MOUSE_BUTTON_2);
                    else
                        mouseRelease(GLFW_MOUSE_BUTTON_1);
                }
            }
            if ((keyevent & HidInput.GamepadButtons.BTN_Y) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                    press(minecraft.options.keyJump.getKey());
                else
                    release(minecraft.options.keyJump.getKey());
            }
            if ((keyevent & HidInput.GamepadButtons.BTN_D_UP) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                    press(minecraft.options.keySwapOffhand.getKey());
                else
                    release(minecraft.options.keySwapOffhand.getKey());
            }
            if ((keyevent & HidInput.GamepadButtons.BTN_D_DOWN) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                    press(minecraft.options.keyDrop.getKey());
                else
                    release(minecraft.options.keyDrop.getKey());
            }
            if ((keyevent & HidInput.GamepadButtons.BTN_OPTIONS) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                    press(GLFW_KEY_ESCAPE);
                else
                    release(GLFW_KEY_ESCAPE);
            }
            if ((keyevent & HidInput.GamepadButtons.BTN_VIEW) != 0) {
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
                        press(GLFW_KEY_ESCAPE);
                    }
                } else {
                    if (last_btn_view_down_was_e) {
                        // no gui
                        LOGGER.debug("NO GUI e UP");
                        release(minecraft.options.keyInventory.getKey());
                    } else {
                        // gui
                        LOGGER.debug("YES GUI ESC UP");
                        release(GLFW_KEY_ESCAPE);
                    }
                }
            }
            if ((keyevent & HidInput.GamepadButtons.BTN_RT_ANALOG_FULL) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                    gyro_is_enabled = false;
                else
                    gyro_is_enabled = true;
            }
            if ((keyevent & HidInput.GamepadButtons.BTN_RT_DIGITAL) != 0) {
                if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0)
                    mousePress(GLFW_MOUSE_BUTTON_3);
                else
                    mouseRelease(GLFW_MOUSE_BUTTON_3);
            }
            if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0) {
                // special keydown
                // TODO: repeat?
                if ((keyevent & HidInput.GamepadButtons.BTN_D_LEFT) != 0) {
                    minecraft.mouseHandler.onScroll(minecraft.getWindow().getWindow(), 0, 1);
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_D_RIGHT) != 0) {
                    minecraft.mouseHandler.onScroll(minecraft.getWindow().getWindow(), 0, -1);
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_R4) != 0) {
                    DeckControls.INPUT.beep(MODE_SWITCH_BEEP_FREQ, MODE_SWITCH_BEEP_LEN);
                    btn_b_is_right_click = !btn_b_is_right_click;
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_LT_ANALOG_FULL) != 0) {
                    manually_sneaking = true;
                    if (!sneak_is_latched) {
                        LOGGER.debug("SNEAK because manual");
                        press(minecraft.options.keyShift.getKey());
                    } else {
                        LOGGER.debug("ignoring SNEAK because already latched");
                    }
                }
                if ((keyevent & HidInput.GamepadButtons.BTN_LT_DIGITAL) != 0) {
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
                }
            } else {
                // special keyup
                if ((keyevent & HidInput.GamepadButtons.BTN_LT_ANALOG_FULL) != 0) {
                    manually_sneaking = false;
                    if (!sneak_is_latched || !sneak_latched_while_manually_sneaking) {
                        if (sneak_is_latched) {
                            DeckControls.INPUT.beep(MODE_SWITCH_BEEP_FREQ, MODE_SWITCH_BEEP_LEN);
                        }
                        sneak_is_latched = false;
                        LOGGER.debug("unSNEAK!!");
                        release(minecraft.options.keyShift.getKey());
                    }
                    sneak_latched_while_manually_sneaking = false;
                }
            }
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
