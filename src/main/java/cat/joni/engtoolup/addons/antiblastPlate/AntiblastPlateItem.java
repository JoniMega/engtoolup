package cat.joni.engtoolup.addons.antiblastPlate;

import blusunrize.immersiveengineering.api.tool.upgrade.IUpgrade;
import blusunrize.immersiveengineering.api.tool.upgrade.UpgradeData;
import blusunrize.immersiveengineering.api.tool.upgrade.UpgradeEffect;
import blusunrize.immersiveengineering.common.items.IEShieldItem;
import com.mojang.datafixers.util.Unit;
import malte0811.dualcodecs.DualCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;


public class AntiblastPlateItem extends Item implements IUpgrade {

    // IE 1.21.1 replaced the old CompoundTag-flag upgrade system with a typed UpgradeEffect/UpgradeData system
    // (see blusunrize.immersiveengineering.api.tool.upgrade). Every custom upgrade flag now needs its own
    // UpgradeEffect instance instead of a raw NBT key -- this mirrors how UpgradeEffect.unit(...) is built
    // internally by IE (that helper itself is private, so we replicate it here).
    public static final UpgradeEffect<Unit> UPGRADE_KEY =
            new UpgradeEffect<>("engtoolup_antiblast_plate", DualCodecs.unit(Unit.INSTANCE), Unit.INSTANCE);

    public AntiblastPlateItem(Properties p_41383_) {
        super(p_41383_);
    }

    @Override
    public Set<String> getUpgradeTypes(ItemStack itemStack) {
        return Set.of(IEShieldItem.TYPE);
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
