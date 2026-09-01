package cat.joni.engtoolup.addons.fluidSensor;

import cat.joni.engtoolup.Engtoolup;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Set;

@Mod.EventBusSubscriber(modid = Engtoolup.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FluidSensorRenderer {
    private static final double LINE_INFLATION = 0.002;
    private static final float RED = 1.0F;
    private static final float GREEN = 0.0F;
    private static final float BLUE = 0.0F;
    private static final float ALPHA = 1.0F;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;

        Set<BlockPos> highlighted = FluidSensorHandler.highlightedPositions;
        if (highlighted.isEmpty())
            return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        for (BlockPos pos : highlighted) {
            AABB box = new AABB(pos).inflate(LINE_INFLATION);
            LevelRenderer.renderLineBox(poseStack, consumer, box, RED, GREEN, BLUE, ALPHA);
        }

        poseStack.popPose();
        bufferSource.endBatch(RenderType.lines());
    }

    private FluidSensorRenderer() {
    }
}