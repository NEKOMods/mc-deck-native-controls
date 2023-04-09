package nekomods.deckcontrols;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

@Mod.EventBusSubscriber(modid = DeckControls.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Settings {
    public static final Settings CONFIG;
    public static final ForgeConfigSpec CONFIG_SPEC;

    final ForgeConfigSpec.DoubleValue gyroTightenMagVal;
    public static double GYRO_TIGHTEN_MAG = 5;
    final ForgeConfigSpec.DoubleValue mouseSmoothingThreshVal;
    public static double MOUSE_SMOOTH_THRESH = 500;
    final ForgeConfigSpec.DoubleValue mouseTightenThreshVal;
    public static double MOUSE_TIGHTEN_THRESH = 100;

    static {
        final Pair<Settings, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Settings::new);
        CONFIG = specPair.getLeft();
        CONFIG_SPEC = specPair.getRight();
    }

    @SubscribeEvent
    public static void onModConfigEvent(final ModConfigEvent configEvent) {
        if (configEvent.getConfig().getSpec() == CONFIG_SPEC) {
            GYRO_TIGHTEN_MAG = CONFIG.gyroTightenMagVal.get();
            MOUSE_SMOOTH_THRESH = CONFIG.mouseSmoothingThreshVal.get();
            MOUSE_TIGHTEN_THRESH = CONFIG.mouseTightenThreshVal.get();
        }
    }

    public Settings(ForgeConfigSpec.Builder builder) {
        gyroTightenMagVal = builder
                .comment("Threshold below which gyro motion will have tightened sensitivity (°/s)")
                .defineInRange("gyroTightenMag", 5, 0, Double.POSITIVE_INFINITY);
        mouseSmoothingThreshVal = builder
                .comment("Threshold below which mouse motion will be smoothed (LSBs, [0-65535]")
                .defineInRange("mouseSmoothingThresh", 500, 0.0, 65536);
        mouseTightenThreshVal = builder
                .comment("Threshold below which mouse motion will have tightened sensitivity (LSBs, [0-65535]")
                .defineInRange("mouseTightenThresh", 100, 0.0, 65536);
    }
}
