package cat.joni.engtoolup.drills.prospectorDrill;

import blusunrize.immersiveengineering.api.tool.upgrade.IUpgradeableTool;
import blusunrize.immersiveengineering.api.tool.upgrade.UpgradeEffect;
import blusunrize.immersiveengineering.common.items.DrillItem;
import cat.joni.engtoolup.Engtoolup;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.function.Predicate;

@EventBusSubscriber(modid = Engtoolup.MODID)
public class ProspectorDrillHandler {
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        syncSilkTouch(event.getPlayer(), event.getPlayer().getMainHandItem());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level(); // Nothing wrong is going to happen here for sure
        if (level.isClientSide())
            return;

        syncSilkTouch(player, player.getMainHandItem());
        syncSilkTouch(player, player.getOffhandItem());
    }

    // Enchantments became a data-driven registry in 1.21 (Enchantments.SILK_TOUCH is now a ResourceKey<Enchantment>,
    // not an Enchantment directly), so we resolve a Holder<Enchantment> through the level's registry access before
    // querying/applying it. This whole method mirrors IE's own DrillItem#finishUpgradeRecalculation, which keeps
    // its Fortune enchantment in sync the exact same way: resolve the Holder via
    // registryOrThrow(Registries.ENCHANTMENT).getHolder(...).orElseThrow(), rebuild an ItemEnchantments.Mutable
    // from the stack's current DataComponents.ENCHANTMENTS, then idempotently set(...) or removeIf(...) it and
    // write back with EnchantmentHelper.setEnchantments(...).
    private static void syncSilkTouch(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof DrillItem))
            return;

        Holder<Enchantment> silkTouch = player.level().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(Enchantments.SILK_TOUCH)
                .orElseThrow();

        ItemStack head = DrillItem.getHeadStatic(stack);
        boolean hasFortuneUpgrade = stack.getItem() instanceof IUpgradeableTool tool
                && tool.getUpgrades(stack).has(UpgradeEffect.FORTUNE);
        boolean shouldHaveSilkTouch = head.getItem() instanceof ProspectorDrillHead && !hasFortuneUpgrade;

        // This part is meant to prevent the drill from keeping the enchantment after removing or losing the drill head
        ItemEnchantments.Mutable newEnchantments = new ItemEnchantments.Mutable(
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
        );
        if (shouldHaveSilkTouch) {
            newEnchantments.set(silkTouch, 1);
        } else {
            newEnchantments.removeIf(Predicate.isEqual(silkTouch));
        }
        // withTooltip(false) hides the auto-applied enchantment from the tooltip, replacing the old
        // stack.hideTooltipPart(TooltipPart.ENCHANTMENTS) API which was removed with the data components rework --
        // the "show in tooltip" flag now lives directly on the ItemEnchantments component itself.
        EnchantmentHelper.setEnchantments(stack, newEnchantments.toImmutable().withTooltip(!shouldHaveSilkTouch));
    }
}
