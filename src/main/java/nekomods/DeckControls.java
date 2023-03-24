package nekomods;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(DeckControls.MODID)
public class DeckControls
{
    public static final String MODID = "deckcontrols";

    public DeckControls() {}

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            Thread inputThread = new Thread(HidInput::threadFunc);
            inputThread.setDaemon(true);
            inputThread.start();
        }
    }
}
