package cat.joni.engtoolup.addons.antiblastPlate;

import blusunrize.immersiveengineering.api.tool.upgrade.IUpgrade;
import blusunrize.immersiveengineering.api.tool.upgrade.IUpgradeableTool;
import blusunrize.immersiveengineering.api.tool.upgrade.UpgradeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;


public class AntiblastPlateItem extends Item implements IUpgrade {

    public static final String UPGRADE_KEY = "engtoolup_Antiblast_plate";

    public AntiblastPlateItem(Properties p_41383_) {
        super(p_41383_);
    }

    @Override
    public Set<String> getUpgradeTypes(ItemStack itemStack) {
        return Set.of("SHIELD");
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
