package cat.joni.engtoolup;

import blusunrize.immersiveengineering.api.tool.upgrade.IUpgradeableTool;
import cat.joni.engtoolup.addons.magneticExtender.MagneticDrillExtenderItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Engtoolup.MODID)
public class ReachUpgradeHandler {
    //This is nuts, there has to be a more intended way to do it
    private static final ResourceLocation REACH_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(Engtoolup.MODID, "auger_guide_wire_reach");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        AttributeInstance reach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (reach == null)
            return;

        boolean shouldHaveBonus = hasReachUpgrade(player.getMainHandItem()) || hasReachUpgrade(player.getOffhandItem());
        AttributeModifier existing = reach.getModifier(REACH_MODIFIER_ID);
        double bonus = Config.magneticDrillExtenderReachBonus;

        if (shouldHaveBonus && (existing == null || existing.amount() != bonus)) {
            if (existing != null)
                reach.removeModifier(REACH_MODIFIER_ID);
            reach.addTransientModifier(new AttributeModifier(
                    REACH_MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_VALUE
            ));
        } else if (!shouldHaveBonus && existing != null) {
            reach.removeModifier(REACH_MODIFIER_ID);
        }
    }

    private static boolean hasReachUpgrade(ItemStack stack) {
        return stack.getItem() instanceof IUpgradeableTool tool
                && tool.getUpgrades(stack).has(MagneticDrillExtenderItem.UPGRADE_KEY);
    }
}
