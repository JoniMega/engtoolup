package cat.joni.engtoolup.addons.fluidSensor;

import blusunrize.immersiveengineering.api.tool.IDrillHead;
import blusunrize.immersiveengineering.api.tool.upgrade.IUpgradeableTool;
import blusunrize.immersiveengineering.common.items.DrillItem;
import cat.joni.engtoolup.Engtoolup;
import cat.joni.engtoolup.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ClientTickEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Every 30 ticks check getExtraBlocksDug if any block surrounding it is LAVA. If so, play a sound
 * There is a bug not addressed: There is a second function in the IE drill that shrinks its mining area to 1x1,
 * yet getExtraBlocksDug still returns its max drilling area.
 */
@EventBusSubscriber(modid = Engtoolup.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class FluidSensorHandler
{
    private static final int WARNING_INTERVAL_TICKS = 30; // 1.5s between repeat warnings while still near lava

    private static int cooldown = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if(player==null||level==null) {
            cooldown = 0;
            return;
        }

        ItemStack drill = getDrillWithSensor(player);
        if(drill.isEmpty()||!(mc.hitResult instanceof BlockHitResult blockHit)||blockHit.getType()!=HitResult.Type.BLOCK) {
            cooldown = 0;
            return;
        }

        if(cooldown > 0) {
            cooldown--;
            return;
        }

        if(isMiningAreaAdjacentToLava(level, player, blockHit, drill)) {
            BlockPos pos = blockHit.getBlockPos();
            level.playLocalSound(
                    pos.getX(), pos.getY(), pos.getZ(),
                    ModSounds.LAVA_ALARM.get(), SoundSource.PLAYERS, 0.6F, 1.0F, false
            );
            spawnNoteParticle(level, player);
            cooldown = WARNING_INTERVAL_TICKS;
        }
    }

    private static void spawnNoteParticle(ClientLevel level, LocalPlayer player)
    {
        Vec3 origin = player.getEyePosition()
                .add(player.getLookAngle().scale(0.4))
                .add(0.5, -1, 1);
        double noteColor = level.random.nextInt(24)/24.0;
        level.addParticle(ParticleTypes.NOTE, origin.x, origin.y, origin.z, noteColor, 0.0, 0.0);
    }

    private static boolean isMiningAreaAdjacentToLava(
            ClientLevel level, LocalPlayer player, BlockHitResult blockHit, ItemStack drill
    )
    {
        Set<BlockPos> mined = new HashSet<>();
        mined.add(blockHit.getBlockPos());

        ItemStack head = DrillItem.getHeadStatic(drill);
        if(head.getItem() instanceof IDrillHead drillHead)
            mined.addAll(drillHead.getExtraBlocksDug(head, level, player, blockHit));

        for(BlockPos pos: mined)
            for(Direction dir: Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if(!mined.contains(neighbor)&&level.getFluidState(neighbor).is(FluidTags.LAVA))
                    return true;
            }

        return false;
    }

    private static ItemStack getDrillWithSensor(LocalPlayer player)
    {
        ItemStack main = player.getMainHandItem();
        if(isDrillWithSensor(main))
            return main;
        ItemStack off = player.getOffhandItem();
        if(isDrillWithSensor(off))
            return off;
        return ItemStack.EMPTY;
    }

    private static boolean isDrillWithSensor(ItemStack stack)
    {
        return stack.getItem() instanceof IUpgradeableTool tool
                && tool.getUpgrades(stack).getBoolean(FluidSensorItem.UPGRADE_KEY);
    }
}
