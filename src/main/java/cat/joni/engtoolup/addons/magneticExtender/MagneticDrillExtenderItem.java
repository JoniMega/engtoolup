package cat.joni.engtoolup.addons.magneticExtender;

import blusunrize.immersiveengineering.api.tool.IUpgrade;
import blusunrize.immersiveengineering.api.tool.IUpgradeableTool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public class MagneticDrillExtenderItem extends Item implements IUpgrade
{
    public static final String UPGRADE_KEY = "engtoolup_reach";

    public MagneticDrillExtenderItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public Set<String> getUpgradeTypes(ItemStack upgrade)
    {
        return Set.of("DRILL");
    }

    @Override
    public boolean canApplyUpgrades(ItemStack target, ItemStack upgrade)
    {
        return target.getItem() instanceof IUpgradeableTool tool
                && !tool.getUpgrades(target).getBoolean(UPGRADE_KEY);
    }

    @Override
    public void applyUpgrades(ItemStack target, ItemStack upgrade, CompoundTag modifications)
    {
        modifications.putBoolean(UPGRADE_KEY, true);
    }
}
