package cat.joni.engtoolup.client;

import cat.joni.engtoolup.Engtoolup;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = Engtoolup.MODID, value = Dist.CLIENT)
public class EngtoolupKeybinds {
    public static final KeyMapping OPEN_MANUAL_ENTRY = new KeyMapping(
            "key.engtoolup.openManualEntry",
            -1,
            "key.categories.engtoolup"
    );

    @SubscribeEvent
    public static void registerKeybinds(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MANUAL_ENTRY);
    }

    private EngtoolupKeybinds() {
    }
}
