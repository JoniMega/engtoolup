package cat.joni.engtoolup.drills.prospectorDrill;

import blusunrize.immersiveengineering.api.tool.IUpgradeableTool;
import blusunrize.immersiveengineering.common.items.DrillItem;
import cat.joni.engtoolup.Engtoolup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Engtoolup.MODID)
public class ProspectorDrillHandler
{
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event)
    {
        syncSilkTouch(event.getPlayer().getMainHandItem());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if(event.phase!=TickEvent.Phase.END)
            return;

        Player player = event.player;
        Level level = player.level(); // Nothing wrong is going to happen here for sure
        if(level.isClientSide())
            return;

        syncSilkTouch(player.getMainHandItem());
        syncSilkTouch(player.getOffhandItem());
    }

    private static void syncSilkTouch(ItemStack stack)
    {
        if(!(stack.getItem() instanceof DrillItem))
            return;

        ItemStack head = DrillItem.getHeadStatic(stack);
        boolean hasFortuneUpgrade = stack.getItem() instanceof IUpgradeableTool tool
                && tool.getUpgrades(stack).getBoolean("fortune");
        boolean shouldHaveSilkTouch = head.getItem() instanceof ProspectorDrillHead&&!hasFortuneUpgrade;
        boolean currentlyHasSilkTouch = EnchantmentHelper.getTagEnchantmentLevel(Enchantments.SILK_TOUCH, stack) > 0;

        // This part is meant to prevent the drill from keeping the enchantment after removing or losing the drill head
        if(shouldHaveSilkTouch&&!currentlyHasSilkTouch) {
            stack.enchant(Enchantments.SILK_TOUCH, 1);
            stack.hideTooltipPart(ItemStack.TooltipPart.ENCHANTMENTS);

        } else if(!shouldHaveSilkTouch&&currentlyHasSilkTouch) {
            Map<Enchantment, Integer> enchants = new HashMap<>(EnchantmentHelper.getEnchantments(stack));
            enchants.remove(Enchantments.SILK_TOUCH);
            EnchantmentHelper.setEnchantments(enchants, stack);
        }
    }
}
