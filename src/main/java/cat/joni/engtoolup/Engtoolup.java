package cat.joni.engtoolup;

import blusunrize.immersiveengineering.api.client.ieobj.IEOBJCallbacks;
import cat.joni.engtoolup.addons.magneticExtender.MagneticDrillExtenderCallbacks;
import cat.joni.engtoolup.registry.ModBlocks;
import cat.joni.engtoolup.registry.ModCreativeTabs;
import cat.joni.engtoolup.registry.ModItems;
import cat.joni.engtoolup.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;


@SuppressWarnings("removal")
@Mod(Engtoolup.MODID)
public class Engtoolup {


    public static final String MODID = "engtoolup";

    private static final Logger LOGGER = LogUtils.getLogger();

    public Engtoolup() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }


    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("Overwriting IE's own \"immersiveengineering:drill\" entry");

            IEOBJCallbacks.register(
                    new ResourceLocation("immersiveengineering", "drill"),
                    MagneticDrillExtenderCallbacks.INSTANCE
            );
            LOGGER.info("Overwritten IE's - Maybe this should be removed or better implemented");
        }
    }
}