package cat.joni.engtoolup.drills.prospectorDrill;

import blusunrize.immersiveengineering.api.tool.IUpgradeableTool;
import blusunrize.immersiveengineering.common.items.DrillItem;
import cat.joni.engtoolup.Engtoolup;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Engtoolup.MODID)
public class ProspectorDrillHandler
{
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event)
    {
        syncSilkTouch(event.getPlayer(), event.getPlayer().getMainHandItem());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event)
    {
        Player player = event.getEntity();
        Level level = player.level(); // Nothing wrong is going to happen here for sure
        if(level.isClientSide())
            return;

        syncSilkTouch(player, player.getMainHandItem());
        syncSilkTouch(player, player.getOffhandItem());
    }

    // NOTE: enchantments became a data-driven registry in 1.21 (Enchantments.SILK_TOUCH is now a
    // ResourceKey<Enchantment>, not an Enchantment), so we need a Holder<Enchantment> resolved through the
    // level's registry access before we can query/apply it. This block wasn't compile-tested against the
    // final 1.21.1 mappings -- double check EnchantmentHelper's exact method names if this doesn't compile.
    private static void syncSilkTouch(Player player, ItemStack stack)
    {
        if(!(stack.getItem() instanceof DrillItem))
            return;

        Holder<Enchantment> silkTouch = player.level().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH);

        ItemStack head = DrillItem.getHeadStatic(stack);
        boolean hasFortuneUpgrade = stack.getItem() instanceof IUpgradeableTool tool
                && tool.getUpgrades(stack).getBoolean("fortune");
        boolean shouldHaveSilkTouch = head.getItem() instanceof ProspectorDrillHead&&!hasFortuneUpgrade;
        boolean currentlyHasSilkTouch = EnchantmentHelper.getItemEnchantmentLevel(silkTouch, stack) > 0;

        // This part is meant to prevent the drill from keeping the enchantment after removing or losing the drill head
        if(shouldHaveSilkTouch&&!currentlyHasSilkTouch) {
            stack.enchant(silkTouch, 1);
            stack.hideTooltipPart(ItemStack.TooltipPart.ENCHANTMENTS);

        } else if(!shouldHaveSilkTouch&&currentlyHasSilkTouch) {
            EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.removeIf(h -> h.equals(silkTouch)));
        }
    }
}
