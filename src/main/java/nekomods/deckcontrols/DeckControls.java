package nekomods.deckcontrols;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(DeckControls.MODID)
public class DeckControls
{
    public static final String MODID = "deckcontrols";
    public static InputHooks HOOKS;
    public static HidInput INPUT;

    public DeckControls() {}

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            INPUT = new HidInput();
            INPUT.start();

            HOOKS = new InputHooks();
        }
    }
}
