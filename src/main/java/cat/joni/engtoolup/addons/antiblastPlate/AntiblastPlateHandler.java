package cat.joni.engtoolup.addons.antiblastPlate;

import blusunrize.immersiveengineering.api.tool.upgrade.IUpgradeableTool;
import blusunrize.immersiveengineering.common.items.IEShieldItem;
import cat.joni.engtoolup.Engtoolup;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Simple upgrade that nullifies explosion knockback as well as explosive damage.
 * By the way, I did not know that explosions did not make damage is blocked by shield. But the code was very easy and
 * it's done.
 */
@EventBusSubscriber(modid = Engtoolup.MODID)
public class AntiblastPlateHandler
{
    //private static final Logger LOGGER = LogUtils.getLogger();

    private static final float DAMAGE_MULTIPLIER_WHILE_BLOCKING = 0.2f; // 80% damage reduction

    private static final Set<UUID> pendingKnockbackCancel = new HashSet<>();

    @SubscribeEvent
    public static void onExplosionDamage(LivingDamageEvent.Pre event)
    {

        if(!event.getSource().is(DamageTypeTags.IS_EXPLOSION))
            return;

        LivingEntity entity = event.getEntity();

        if(!isBlockingWithAntiblastPlate(entity))
        {
            return;
        }

        event.setNewDamage(event.getNewDamage() * DAMAGE_MULTIPLIER_WHILE_BLOCKING); // 80% damage reduction on top of any other damage reductions

        entity.setDeltaMovement(Vec3.ZERO); // Knockback 0
        pendingKnockbackCancel.add(entity.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event)
    {
        Player player = event.getEntity();
        if(player.level().isClientSide())
            return;

        if(pendingKnockbackCancel.remove(player.getUUID()))
        {
            player.setDeltaMovement(Vec3.ZERO); // Failsafe as sometimes it did not work without this
        }
    }

    private static boolean isBlockingWithAntiblastPlate(LivingEntity entity)
    {
        if(!entity.isBlocking()) {
            return false;
        }

        ItemStack blockingStack = entity.getUseItem();
        boolean isShield = blockingStack.getItem() instanceof IEShieldItem;
        //LOGGER.info("[DEBUG]   -> isShield={}", isShield);
        if(!isShield)
            return false;

        boolean isUpgradeableTool = blockingStack.getItem() instanceof IUpgradeableTool;
        //LOGGER.info("[DEBUG]   -> isUpgradeableTool={}", isUpgradeableTool);
        if(!isUpgradeableTool)
            return false;

        boolean hasPlate = ((IUpgradeableTool)blockingStack.getItem()).getUpgrades(blockingStack).getBoolean(AntiblastPlateItem.UPGRADE_KEY);
        //LOGGER.info("[DEBUG]   -> hasAntiblastPlateFlag={}", hasPlate);
        return hasPlate;
    }
}
