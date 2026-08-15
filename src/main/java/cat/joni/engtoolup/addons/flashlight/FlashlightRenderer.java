package cat.joni.engtoolup.addons.flashlight;

import blusunrize.immersiveengineering.api.tool.IUpgradeableTool;
import cat.joni.engtoolup.Engtoolup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * There is so much deprecated/rubbish code here. Pending of cleaning this absolute mess.
 * Original Idea: generate a powerfull light at the end and place less powerfull lightsources in the middle.
 * Final implementation: just place a lightsource in every block in between the max distance and the player.
 */
@Mod.EventBusSubscriber(modid = Engtoolup.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FlashlightRenderer
{
    private static final double MAX_DIST = 16.0;
    private static final int MAX_LIGHTS = 16;
    private static final int HIGH_LIGHT_LEVEL = 9;
    private static final int LOW_LIGHT_LEVEL = 8;

    private static final Map<BlockPos, Integer> activeLights = new HashMap<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if(event.phase!=TickEvent.Phase.END)
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if(player==null||level==null)
        {
            clearAllLights(null);
            return;
        }

        Map<BlockPos, Integer> desired = isFlashlightActive(player)?sampleBeam(player, level): Map.of();
        applyLights(level, desired);
    }

    private static Map<BlockPos, Integer> sampleBeam(LocalPlayer player, ClientLevel level)
    {
        Vec3 eye = player.getEyePosition();
        Vec3 dir = player.getLookAngle();
        Vec3 farPoint = eye.add(dir.scale(MAX_DIST));

        ClipContext ctx = new ClipContext(eye, farPoint, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player);
        HitResult hit = level.clip(ctx);
        Vec3 target = hit.getType()==HitResult.Type.BLOCK?hit.getLocation(): farPoint;

        double dist = eye.distanceTo(target);
        Map<BlockPos, Integer> desired = new LinkedHashMap<>();

        if(dist < 1.0)
        {
            BlockPos pos = BlockPos.containing(eye);
            if(isSafeToLight(level, pos))
                desired.put(pos, HIGH_LIGHT_LEVEL);
            return desired;
        }

        int lightCount = Math.max(1, Math.min(MAX_LIGHTS, (int)Math.floor(dist)));
        for(int k = 1; k <= lightCount; k++)
        {
            double stepDist = Math.min(k, dist);
            BlockPos pos = BlockPos.containing(eye.add(dir.scale(stepDist)));
            if(!isSafeToLight(level, pos))
                continue; // never overwrite water, lava, or anything else that isn't genuinely empty air
            int lightLevel = (k==lightCount)?HIGH_LIGHT_LEVEL: LOW_LIGHT_LEVEL;
            desired.merge(pos, lightLevel, Math::max);
        }

        return desired;
    }

    private static boolean isSafeToLight(ClientLevel level, BlockPos pos)
    {
        BlockState state = level.getBlockState(pos);
        return state.isAir()||state.is(Blocks.LIGHT);
    }

    private static void applyLights(ClientLevel level, Map<BlockPos, Integer> desired)
    {
        Iterator<Map.Entry<BlockPos, Integer>> it = activeLights.entrySet().iterator();
        while(it.hasNext())
        {
            Map.Entry<BlockPos, Integer> entry = it.next();
            if(!desired.containsKey(entry.getKey()))
            {
                if(level.getBlockState(entry.getKey()).is(Blocks.LIGHT))
                    level.setBlock(entry.getKey(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                it.remove();
            }
        }

        for(Map.Entry<BlockPos, Integer> entry: desired.entrySet())
        {
            Integer current = activeLights.get(entry.getKey());
            if(current==null||!current.equals(entry.getValue()))
            {
                level.setBlock(
                        entry.getKey(),
                        Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, entry.getValue()),
                        Block.UPDATE_CLIENTS
                );
                activeLights.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void clearAllLights(ClientLevel level)
    {
        if(activeLights.isEmpty())
            return;
        if(level!=null)
            for(BlockPos pos: activeLights.keySet())
                if(level.getBlockState(pos).is(Blocks.LIGHT))
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        activeLights.clear();
    }

    private static boolean isFlashlightActive(LocalPlayer player)
    {
        return isUpgradedToolWithFlashlight(player.getMainHandItem())||isUpgradedToolWithFlashlight(player.getOffhandItem());
    }

    private static boolean isUpgradedToolWithFlashlight(ItemStack stack)
    {
        return stack.getItem() instanceof IUpgradeableTool tool
                && tool.getUpgrades(stack).getBoolean(FlashlightItem.UPGRADE_KEY);
    }
}