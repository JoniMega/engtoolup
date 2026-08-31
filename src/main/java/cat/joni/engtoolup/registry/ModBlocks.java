package cat.joni.engtoolup.registry;

import cat.joni.engtoolup.Engtoolup;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Engtoolup.MODID);

    public static final RegistryObject<Block> STORAGE_DRILL = BLOCKS.register("storage_drill",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    private ModBlocks() {
    }
}
