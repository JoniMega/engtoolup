package cat.joni.engtoolup.registry;

import cat.joni.engtoolup.Engtoolup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("removal")
public class ModSounds
{
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Engtoolup.MODID);

    public static final RegistryObject<SoundEvent> LAVA_ALARM = SOUNDS.register("lava_alarm",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Engtoolup.MODID, "lava_alarm")));

    private ModSounds()
    {
    }
}
