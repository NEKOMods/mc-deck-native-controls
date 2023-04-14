package nekomods.deckcontrols;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Arrays;

@Mod(DeckControls.MODID)
public class DeckControls
{
    public static final String MODID = "deckcontrols";
    public static InputHooks HOOKS;
    public static HidInput HID_INPUT;
    public static TouchscreenInput TOUCH_INPUT;

    public DeckControls() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Settings.CONFIG_SPEC);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            HID_INPUT = new HidInput();
            HID_INPUT.start();

            TOUCH_INPUT = new TouchscreenInput();
            TOUCH_INPUT.start();

            HOOKS = new InputHooks();
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents
    {
        @SubscribeEvent
        public static void onDebugText(CustomizeGuiOverlayEvent.DebugText dt) {
            if (Minecraft.getInstance().options.renderDebug) {
                dt.getLeft().add("");
                dt.getLeft().add(
                        "Steam Deck native input: " +
                                (HID_INPUT.isAlive() ? "ok" : "not ok") +
                                (HID_INPUT.debug ? ", using simulator" : "")
                );
                dt.getLeft().add(String.format(
                        "Missed frames: %d, avg frame time %f ms",
                        DeckControls.HID_INPUT.missedFrames,
                        Arrays.stream(DeckControls.HID_INPUT.frameTimes).average().orElse(0) / 1e6
                ));
            }
        }
    }
}
