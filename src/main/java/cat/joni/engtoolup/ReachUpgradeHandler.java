package cat.joni.engtoolup;

import blusunrize.immersiveengineering.api.tool.IUpgradeableTool;
import cat.joni.engtoolup.addons.magneticExtender.MagneticDrillExtenderItem;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Engtoolup.MODID)
public class ReachUpgradeHandler {
    //This is nuts, there has to be a more intended way to do it
    private static final UUID REACH_MODIFIER_ID = UUID.fromString("6f1c9e3a-6b2b-4d2a-9c7d-1e2f3a4b5c6d");
    private static final double REACH_BONUS = 5.0D;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        Player player = event.player;
        AttributeInstance reach = player.getAttribute(ForgeMod.BLOCK_REACH.get());
        if (reach == null)
            return;

        boolean shouldHaveBonus = hasReachUpgrade(player.getMainHandItem()) || hasReachUpgrade(player.getOffhandItem());
        AttributeModifier existing = reach.getModifier(REACH_MODIFIER_ID);

        if (shouldHaveBonus && existing == null) {
            reach.addTransientModifier(new AttributeModifier(
                    REACH_MODIFIER_ID, "engtoolup_auger_guide_wire", REACH_BONUS, AttributeModifier.Operation.ADDITION
            ));
        } else if (!shouldHaveBonus && existing != null) {
            reach.removeModifier(REACH_MODIFIER_ID);
        }
    }

    private static boolean hasReachUpgrade(ItemStack stack) {
        return stack.getItem() instanceof IUpgradeableTool tool
                && tool.getUpgrades(stack).getBoolean(MagneticDrillExtenderItem.UPGRADE_KEY);
    }
}
