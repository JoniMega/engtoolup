package cat.joni.engtoolup.addons.leadWeight;

import blusunrize.immersiveengineering.api.tool.upgrade.IUpgrade;
import blusunrize.immersiveengineering.api.tool.upgrade.IUpgradeableTool;
import blusunrize.immersiveengineering.api.tool.upgrade.UpgradeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/** WORK IN PROGRESS
 *
 *  This is meant to be an upgrade for a boots armor. Forcing the player to sink fast, instead of sinking slowly
 **/

public class LeadWeightItem extends Item implements IUpgrade {

    public static final String UPGRADE_KEY = "engtoolup_Lead_Weight";

    public LeadWeightItem(Properties p_41383_) {
        super(p_41383_);
    }

    @Override
    public Set<String> getUpgradeTypes(ItemStack itemStack) {
        return Set.of("BOOTS");
    }

    @Override
    public boolean canApplyUpgrades(UpgradeData upgradeData, ItemStack itemStack) {
        return false;
    }

    @Override
    public UpgradeData applyUpgrades(UpgradeData upgradeData, ItemStack itemStack) {
        return null;
    }

    @Override
    public boolean canApplyUpgrades(ItemStack target, ItemStack upgrade) {
        return target.getItem() instanceof IUpgradeableTool tool
                && !tool.getUpgrades(target).getBoolean(UPGRADE_KEY);
    }

    @Override
    public void applyUpgrades(ItemStack target, ItemStack upgrade, CompoundTag modifications) {
        modifications.putBoolean(UPGRADE_KEY, true);
    }
}
