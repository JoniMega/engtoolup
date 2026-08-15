package cat.joni.engtoolup.client;

import cat.joni.engtoolup.Engtoolup;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Engtoolup.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class EngtoolupKeybinds
{
    public static final KeyMapping OPEN_MANUAL_ENTRY = new KeyMapping(
            "key.engtoolup.openManualEntry",
            -1,
            "key.categories.engtoolup"
    );

    @SubscribeEvent
    public static void registerKeybinds(RegisterKeyMappingsEvent event)
    {
        event.register(OPEN_MANUAL_ENTRY);
    }

    private EngtoolupKeybinds()
    {
    }
}
