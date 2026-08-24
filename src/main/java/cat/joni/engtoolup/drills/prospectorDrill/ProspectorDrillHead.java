package cat.joni.engtoolup.drills.prospectorDrill;

import blusunrize.immersiveengineering.api.EnumMetals;
import blusunrize.immersiveengineering.api.IETags;
import blusunrize.immersiveengineering.common.items.DrillheadItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tiers;

public class ProspectorDrillHead extends DrillheadItem {
    public ProspectorDrillHead() {
        super(new DrillHeadPerm(
                "prospector",
                IETags.getTagsFor(EnumMetals.GOLD).ingot,
                3, // drillSize
                1, // drillDepth
                Tiers.DIAMOND, // mining level
                10.0f, // drillSpeed
                2, // attack damage
                10000, // max durability
                ResourceLocation.fromNamespaceAndPath("engtoolup", "item/drill_prospector") // TO-DO new texture
        ));
    }
}
