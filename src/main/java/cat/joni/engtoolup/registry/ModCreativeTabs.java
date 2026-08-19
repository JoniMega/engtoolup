package cat.joni.engtoolup.registry;

import cat.joni.engtoolup.Engtoolup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Engtoolup.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENGTOOLUP_TAB = CREATIVE_MODE_TABS.register("engtoolup_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.engtoolup"))
                    .icon(() -> new ItemStack(ModItems.FLASHLIGHT.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.STORAGE_DRILL_ITEM.get());
                        output.accept(ModItems.MAGNETIC_DRILL_EXTENDER.get());
                        output.accept(ModItems.STEEL_SPRING.get());
                        output.accept(ModItems.LEAD_DRILLHEAD.get());
                        output.accept(ModItems.PROSPECTOR_BOX.get());
                        output.accept(ModItems.PROSPECTOR_DRILLHEAD.get());
                        output.accept(ModItems.FLASHLIGHT.get());
                        output.accept(ModItems.FLUID_SENSOR.get());
                        output.accept(ModItems.ANTIBLAST_PLATE.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
