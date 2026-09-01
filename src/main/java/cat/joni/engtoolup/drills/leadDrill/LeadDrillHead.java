package cat.joni.engtoolup.drills.leadDrill;

import blusunrize.immersiveengineering.api.EnumMetals;
import blusunrize.immersiveengineering.api.IETags;
import blusunrize.immersiveengineering.common.items.DrillheadItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tiers;


@SuppressWarnings("removal")
public class LeadDrillHead extends DrillheadItem {
    public LeadDrillHead() {
        super(new DrillheadItem.DrillHeadPerm(
                "lead",
                IETags.getTagsFor(EnumMetals.LEAD).ingot,
                1, // drillSize
                1, // drillDepth
                Tiers.DIAMOND, // mining level
                24.0f, // drillSpeed
                12, // attack damage
                5000, // max durability
                new ResourceLocation("engtoolup", "item/drill_lead")
        ));
    }
}
