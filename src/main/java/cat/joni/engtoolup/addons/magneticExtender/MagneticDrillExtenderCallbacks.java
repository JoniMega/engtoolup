package cat.joni.engtoolup.addons.magneticExtender;

import blusunrize.immersiveengineering.api.tool.upgrade.IUpgradeableTool;
import blusunrize.immersiveengineering.client.models.obj.callback.item.DrillCallbacks;
import cat.joni.engtoolup.Engtoolup;
import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

import java.util.List;

/**
 * Welcome to the worst class in this project :D
 * This class extends whatever IE has done. It mimics like teh drill head is magnetically lauched to the block being
 * mined, as well as some momentum if the player moves the cursor.
 * Also, there is an iddle movement where the drill head moves forward and backwards.
 */
@EventBusSubscriber(modid = Engtoolup.MODID, value = Dist.CLIENT)
public class MagneticDrillExtenderCallbacks extends DrillCallbacks {
    public static final MagneticDrillExtenderCallbacks INSTANCE = new MagneticDrillExtenderCallbacks();

    private static final float MAX_LAUNCH_DISTANCE = 0.35f; //DEPRECATED
    private static final float MAX_REACH_DISTANCE = 9f;
    private static final float EASING = 0.8f;
    private static final float SWING_IMPULSE = 0.02f;
    private static final float SWING_STIFFNESS = 0.10f;
    private static final float SWING_DAMPING = 0.22f;
    private static final float MAX_SWING = 0.4f;
    private static final float MAX_DELTA_DEGREES = 25f;
    private static final float IDLE_BOB_AMPLITUDE = 0.15f;
    private static final float IDLE_BOB_SPEED = 0.06f;
    private static final float IDLE_WRAP = (float) (Math.PI * 2000);

    private static float prevExtension = 0f;
    private static float extension = 0f;

    private static float swingRight = 0f, swingUp = 0f;
    private static float prevSwingRight = 0f, prevSwingUp = 0f;
    private static float swingRightVel = 0f, swingUpVel = 0f;
    private static float lastYaw = Float.NaN, lastPitch = Float.NaN;

    private static float idleTicks = 0f;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        idleTicks = (idleTicks + 1f) % IDLE_WRAP;

        prevExtension = extension;
        float target = computeTargetExtension();
        extension += (target - extension) * EASING;
        if (Math.abs(target - extension) < 0.001f)
            extension = target;

        prevSwingRight = swingRight;
        prevSwingUp = swingUp;
        tickSwing();
    }

    private static void tickSwing() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            lastYaw = Float.NaN;
            lastPitch = Float.NaN;
        } else {
            float yaw = mc.player.getYRot();
            float pitch = mc.player.getXRot();
            if (!Float.isNaN(lastYaw)) {
                float deltaYaw = Mth.clamp(Mth.wrapDegrees(yaw - lastYaw), -MAX_DELTA_DEGREES, MAX_DELTA_DEGREES);
                float deltaPitch = Mth.clamp(pitch - lastPitch, -MAX_DELTA_DEGREES, MAX_DELTA_DEGREES);

                swingRightVel -= deltaYaw * SWING_IMPULSE;
                swingUpVel += deltaPitch * SWING_IMPULSE;
            }
            lastYaw = yaw;
            lastPitch = pitch;
        }

        swingRightVel += -swingRight * SWING_STIFFNESS;
        swingRightVel *= (1f - SWING_DAMPING);
        swingRight = Mth.clamp(swingRight + swingRightVel, -MAX_SWING, MAX_SWING);

        swingUpVel += -swingUp * SWING_STIFFNESS;
        swingUpVel *= (1f - SWING_DAMPING);
        swingUp = Mth.clamp(swingUp + swingUpVel, -MAX_SWING, MAX_SWING);
    }

    private static float computeTargetExtension() {
        Minecraft mc = Minecraft.getInstance();
        MultiPlayerGameMode gameMode = mc.gameMode;
        if (mc.player == null || gameMode == null || !gameMode.isDestroying())
            return 0f;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult) || hit.getType() != HitResult.Type.BLOCK)
            return 0f;

        double distance = mc.player.getEyePosition(1f).distanceTo(hit.getLocation());

        return (float) Math.min(distance, MAX_REACH_DISTANCE);
    }

    private static boolean hasMagneticExtender(ItemStack stack) {
        return stack.getItem() instanceof IUpgradeableTool tool
                && tool.getUpgrades(stack).has(MagneticDrillExtenderItem.UPGRADE_KEY);
    }

    @Override
    public Transformation getTransformForGroups(
            ItemStack stack, List<String> groups, ItemDisplayContext transform, LivingEntity entity, float partialTicks) {

        Transformation base = super.getTransformForGroups(stack, groups, transform, entity, partialTicks);

        if (groups.isEmpty() || !"drill_head".equals(groups.get(0)))
            return base;

        if (entity == null || entity != Minecraft.getInstance().player)
            return base;

        if (!hasMagneticExtender(stack))
            return base;

        float push = Mth.lerp(partialTicks, prevExtension, extension) * 1.5f;
        if (push <= 0f) {
            float t = idleTicks + partialTicks;
            float idleBob = Mth.sin(t * IDLE_BOB_SPEED) * IDLE_BOB_AMPLITUDE + 0.15f;
            Transformation idle = new Transformation(new Vector3f(idleBob, 0, 0), null, null, null);
            return idle.compose(base);
        }

        float right = Mth.lerp(partialTicks, prevSwingRight, swingRight) + Math.min(0.5f, extension * 0.05f);
        float up = Mth.lerp(partialTicks, prevSwingUp, swingUp) + Math.min(0.2f, extension * 0.04f);

        Transformation launch = new Transformation(new Vector3f(push, up, -right), null, null, null);
        return launch.compose(base);
    }
}