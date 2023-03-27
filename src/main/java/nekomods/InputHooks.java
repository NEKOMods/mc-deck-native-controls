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

    public InputHooks() {
        minecraft = Minecraft.getInstance();
    }
    public void runTick() {
        minecraft.getWindow().setErrorSection("DeckControlsMod");
        minecraft.getProfiler().push("deck_controls_mod");

        HidInput.OtherHidState gamepad = HidInput.latestInput;
        if (last_lthumb_y < 16000 && gamepad.lthumb_y >= 16000) {
            LOGGER.info("FORWARD DOWN");
            minecraft.keyboardHandler.keyPress(
                    minecraft.getWindow().getWindow(),
                    minecraft.options.keyUp.getKey().getValue(),
                    glfwGetKeyScancode(minecraft.options.keyUp.getKey().getValue()),
                    GLFW_PRESS,
                    0);
        }
        if (last_lthumb_y >= 15000 && gamepad.lthumb_y < 15000) {
            LOGGER.info("FORWARD UP");
            minecraft.keyboardHandler.keyPress(
                    minecraft.getWindow().getWindow(),
                    minecraft.options.keyUp.getKey().getValue(),
                    glfwGetKeyScancode(minecraft.options.keyUp.getKey().getValue()),
                    GLFW_RELEASE,
                    0);
        }
        if (-last_lthumb_y < 16000 && -gamepad.lthumb_y >= 16000) {
            LOGGER.info("BACKWARD DOWN");
            minecraft.keyboardHandler.keyPress(
                    minecraft.getWindow().getWindow(),
                    minecraft.options.keyDown.getKey().getValue(),
                    glfwGetKeyScancode(minecraft.options.keyDown.getKey().getValue()),
                    GLFW_PRESS,
                    0);
        }
        if (-last_lthumb_y >= 15000 && -gamepad.lthumb_y < 15000) {
            LOGGER.info("BACKWARD UP");
            minecraft.keyboardHandler.keyPress(
                    minecraft.getWindow().getWindow(),
                    minecraft.options.keyDown.getKey().getValue(),
                    glfwGetKeyScancode(minecraft.options.keyDown.getKey().getValue()),
                    GLFW_RELEASE,
                    0);
        }
        if (last_lthumb_x < 16000 && gamepad.lthumb_x >= 16000) {
            LOGGER.info("RIGHT DOWN");
            minecraft.keyboardHandler.keyPress(
                    minecraft.getWindow().getWindow(),
                    minecraft.options.keyRight.getKey().getValue(),
                    glfwGetKeyScancode(minecraft.options.keyRight.getKey().getValue()),
                    GLFW_PRESS,
                    0);
        }
        if (last_lthumb_x >= 15000 && gamepad.lthumb_x < 15000) {
            LOGGER.info("RIGHT UP");
            minecraft.keyboardHandler.keyPress(
                    minecraft.getWindow().getWindow(),
                    minecraft.options.keyRight.getKey().getValue(),
                    glfwGetKeyScancode(minecraft.options.keyRight.getKey().getValue()),
                    GLFW_RELEASE,
                    0);
        }
        if (-last_lthumb_x < 16000 && -gamepad.lthumb_x >= 16000) {
            LOGGER.info("LEFT DOWN");
            minecraft.keyboardHandler.keyPress(
                    minecraft.getWindow().getWindow(),
                    minecraft.options.keyLeft.getKey().getValue(),
                    glfwGetKeyScancode(minecraft.options.keyLeft.getKey().getValue()),
                    GLFW_PRESS,
                    0);
        }
        if (-last_lthumb_x >= 15000 && -gamepad.lthumb_x < 15000) {
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
            }
        }

        minecraft.getProfiler().pop();
    }

    public float fbImpulse(float keyboardImpulse) {
        HidInput.OtherHidState gamepad = HidInput.latestInput;
        if (gamepad.lthumb_x * gamepad.lthumb_x + gamepad.lthumb_y * gamepad.lthumb_y > 5000 * 5000) {
            float ret = gamepad.lthumb_y / 32700f;
            if (ret > 1) ret = 1;
            if (ret < -1) ret = -1;
            return ret;
        } else {
            return keyboardImpulse;
        }
    }

    public float lrImpulse(float keyboardImpulse) {
        HidInput.OtherHidState gamepad = HidInput.latestInput;
        if (gamepad.lthumb_x * gamepad.lthumb_x + gamepad.lthumb_y * gamepad.lthumb_y > 5000 * 5000) {
            float ret = gamepad.lthumb_x / -32700f;
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
