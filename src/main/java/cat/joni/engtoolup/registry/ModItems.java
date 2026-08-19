package cat.joni.engtoolup.registry;

import cat.joni.engtoolup.Engtoolup;
import cat.joni.engtoolup.addons.antiblastPlate.AntiblastPlateItem;
import cat.joni.engtoolup.addons.flashlight.FlashlightItem;
import cat.joni.engtoolup.addons.fluidSensor.FluidSensorItem;
import cat.joni.engtoolup.addons.magneticExtender.MagneticDrillExtenderItem;
import cat.joni.engtoolup.drills.leadDrill.LeadDrillHead;
import cat.joni.engtoolup.drills.prospectorDrill.ProspectorDrillHead;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Engtoolup.MODID);

    public static final DeferredItem<Item> STORAGE_DRILL_ITEM = ITEMS.register("storage_drill",
            () -> new BlockItem(ModBlocks.STORAGE_DRILL.get(), new Item.Properties()));

    public static final DeferredItem<Item> MAGNETIC_DRILL_EXTENDER = ITEMS.register("magnetic_drill_extender",
            () -> new MagneticDrillExtenderItem(new Item.Properties()));

    public static final DeferredItem<Item> STEEL_SPRING = ITEMS.register("steel_spring",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> LEAD_DRILLHEAD = ITEMS.register("lead_drillhead",
            () -> new LeadDrillHead());

    public static final DeferredItem<Item> PROSPECTOR_BOX = ITEMS.register("prospector_box",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> PROSPECTOR_DRILLHEAD = ITEMS.register("prospector_drillhead",
            () -> new ProspectorDrillHead());

    public static final DeferredItem<Item> FLASHLIGHT = ITEMS.register("flashlight",
            () -> new FlashlightItem(new Item.Properties()));

    public static final DeferredItem<Item> FLUID_SENSOR = ITEMS.register("fluid_sensor",
            () -> new FluidSensorItem(new Item.Properties()));

    public static final DeferredItem<Item> ANTIBLAST_PLATE = ITEMS.register("antiblast_plate",
            () -> new AntiblastPlateItem(new Item.Properties()));

    private ModItems() {
    }
}
