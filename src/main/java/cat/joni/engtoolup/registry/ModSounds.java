package cat.joni.engtoolup.registry;

import cat.joni.engtoolup.Engtoolup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Engtoolup.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> LAVA_ALARM = SOUNDS.register("lava_alarm",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Engtoolup.MODID, "lava_alarm")));

    private ModSounds() {
    }
}
