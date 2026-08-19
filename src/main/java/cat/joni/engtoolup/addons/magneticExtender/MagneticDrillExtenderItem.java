package cat.joni.engtoolup.addons.magneticExtender;

import blusunrize.immersiveengineering.api.tool.upgrade.IUpgrade;
import blusunrize.immersiveengineering.api.tool.upgrade.UpgradeData;
import blusunrize.immersiveengineering.api.tool.upgrade.UpgradeEffect;
import blusunrize.immersiveengineering.common.items.DrillItem;
import com.mojang.datafixers.util.Unit;
import malte0811.dualcodecs.DualCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public class MagneticDrillExtenderItem extends Item implements IUpgrade {
    public static final UpgradeEffect<Unit> UPGRADE_KEY =
            new UpgradeEffect<>("engtoolup_reach", DualCodecs.unit(Unit.INSTANCE), Unit.INSTANCE);

    public MagneticDrillExtenderItem(Properties properties) {
        super(properties);
    }

    @Override
    public Set<String> getUpgradeTypes(ItemStack upgrade) {
        return Set.of(DrillItem.TYPE);
    }

    @Override
    public boolean canApplyUpgrades(UpgradeData existing, ItemStack upgrade) {
        return !existing.has(UPGRADE_KEY);
    }

    @Override
    public UpgradeData applyUpgrades(UpgradeData base, ItemStack upgrade) {
        return base.with(UPGRADE_KEY);
    }
}
