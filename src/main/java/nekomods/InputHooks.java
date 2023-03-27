package nekomods;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import static org.lwjgl.glfw.GLFW.*;

public class InputHooks {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Minecraft minecraft;

    public InputHooks() {
        minecraft = Minecraft.getInstance();
    }
    public void runTick() {
        minecraft.getWindow().setErrorSection("DeckControlsMod");
        minecraft.getProfiler().push("deck_controls_mod");

        HidInput.keyEvents.addLast(HidInput.GamepadButtons.FLAG_BARRIER);
        int keyevent;
        while ((keyevent = HidInput.keyEvents.removeFirst()) != HidInput.GamepadButtons.FLAG_BARRIER) {
            if ((keyevent & HidInput.GamepadButtons.FLAG_BTN_UP) == 0) {
                LOGGER.info("KEY DOWN " + keyevent);

                if ((keyevent & HidInput.GamepadButtons.BTN_Y) != 0) {
                    // jump hack
                    minecraft.keyboardHandler.keyPress(
                        minecraft.getWindow().getWindow(),
                        GLFW_KEY_SPACE,
                        glfwGetKeyScancode(GLFW_KEY_SPACE),
                        GLFW_PRESS,
                        0);
                }
            } else {
                LOGGER.info("KEY UP " + (keyevent & (~HidInput.GamepadButtons.FLAG_BTN_UP)));

                if ((keyevent & HidInput.GamepadButtons.BTN_Y) != 0) {
                    // jump hack
                    minecraft.keyboardHandler.keyPress(
                            minecraft.getWindow().getWindow(),
                            GLFW_KEY_SPACE,
                            glfwGetKeyScancode(GLFW_KEY_SPACE),
                            GLFW_RELEASE,
                            0);
                }
            }
        }

        minecraft.getProfiler().pop();
    }

    public static void runTickHook() {
        if (DeckControls.HOOKS != null) {
            DeckControls.HOOKS.runTick();
        }
    }
}
