package cat.joni.engtoolup;

import blusunrize.immersiveengineering.api.client.ieobj.IEOBJCallbacks;
import cat.joni.engtoolup.addons.magneticExtender.MagneticDrillExtenderCallbacks;
import cat.joni.engtoolup.registry.ModBlocks;
import cat.joni.engtoolup.registry.ModCreativeTabs;
import cat.joni.engtoolup.registry.ModItems;
import cat.joni.engtoolup.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Engtoolup.MODID)
public class Engtoolup {


    public static final String MODID = "engtoolup";

    private static final Logger LOGGER = LogUtils.getLogger();

    public Engtoolup(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }



    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("Overwriting IE's own \"immersiveengineering:drill\" entry");

            IEOBJCallbacks.register(
                    ResourceLocation.fromNamespaceAndPath("immersiveengineering", "drill"),
                    MagneticDrillExtenderCallbacks.INSTANCE
            );
            LOGGER.info("Overwritten IE's - Maybe this should be removed or better implemented");
        }
    }
}
