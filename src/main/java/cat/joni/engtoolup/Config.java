package cat.joni.engtoolup;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Engtoolup.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.DoubleValue MAGNETIC_DRILL_EXTENDER_REACH_BONUS;
    private static final ModConfigSpec.BooleanValue MANUAL_INSPECT_REQUIRES_MANUAL;

    static {

        BUILDER.push("magneticDrillExtender");

        MAGNETIC_DRILL_EXTENDER_REACH_BONUS = BUILDER
                .comment("How many extra blocks of reach the Magnetic Drill Extender upgrade grants a drill (or IE shield).")
                .defineInRange("reachBonus", 5.0, 0.0, 32.0);

        BUILDER.pop();

        BUILDER.push("manualInspect");

        MANUAL_INSPECT_REQUIRES_MANUAL = BUILDER
                .comment(
                        "If true, the \"Inspect any item\" QoL feature (hold the keybind while hovering an item to",
                        "jump to its page in the Engineer's Manual) only works while an Engineer's Manual is",
                        "somewhere in the player's inventory."
                )
                .define("requiresManual", false);

        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    public static double magneticDrillExtenderReachBonus;
    public static boolean manualInspectRequiresManual;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        magneticDrillExtenderReachBonus = MAGNETIC_DRILL_EXTENDER_REACH_BONUS.get();
        manualInspectRequiresManual = MANUAL_INSPECT_REQUIRES_MANUAL.get();
    }

    private Config() {
    }
}