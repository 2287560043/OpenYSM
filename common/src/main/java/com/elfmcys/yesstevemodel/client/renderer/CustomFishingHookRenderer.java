package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.ProjectileCapability;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoEntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;
import rip.ysm.api.item.ToolActionBridge;
import org.spongepowered.asm.mixin.Unique;

public class CustomFishingHookRenderer {
    public static GeoEntityRenderState extractHook(FishingHook fishingHook, float partialTick) {
        return ProjectileCapability.get(fishingHook).map(cap -> {
            if (cap.isModelInitialized() && cap.isModelReady()) {
                fishingHook.setXRot(0.0f);
                fishingHook.xRotO = 0.0f;
                GeoEntityRenderState state = RendererManager.getProjectileRenderer().createRenderState(fishingHook, partialTick);
                Player playerOwner = fishingHook.getPlayerOwner();
                if (playerOwner != null) {
                    state.fishingLineOrigin = extractFishingLine(fishingHook, partialTick, playerOwner);
                    state.fishingLineColor = lineColor(fishingHook);
                    state.fishingLineWidth = Minecraft.getInstance().getWindow().getAppropriateLineWidth();
                }
                return state;
            }
            return null;
        }).orElse(null);
    }

    private static Vec3 extractFishingLine(FishingHook fishingHook, float partialTick, Player player) {
        int hand = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        if (!ToolActionBridge.canFishingRodCast(player.getMainHandItem())) {
            hand = -hand;
        }
        float swingProgressSqrt = Mth.sin(Mth.sqrt(player.getAttackAnim(partialTick)) * 3.1415927f);
        float yawOffset = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * 0.017453292f;
        double dSin = Mth.sin(yawOffset);
        double dCos = Mth.cos(yawOffset);
        double handOffset = hand * 0.35d;
        double anglerX;
        double anglerY;
        double anglerZ;
        float anglerEye;
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        Options options = entityRenderDispatcher.options;
        if (options == null || !options.getCameraType().isFirstPerson() || player != Minecraft.getInstance().player) {
            anglerX = (Mth.lerp(partialTick, player.xo, player.getX()) - (dCos * handOffset)) - (dSin * 0.8d);
            anglerY = ((player.yo + player.getEyeHeight()) + ((player.getY() - player.yo) * partialTick)) - 0.45d;
            anglerZ = (Mth.lerp(partialTick, player.zo, player.getZ()) - (dSin * handOffset)) + (dCos * 0.8d);
            anglerEye = player.isCrouching() ? -0.1875f : 0.0f;
        } else {
            Vec3 vec3XRot = entityRenderDispatcher.camera.getNearPlane(options.fov().get().floatValue()).getPointOnPlane(hand * 0.525f, -0.1f).scale(960.0d / options.fov().get().intValue()).yRot(swingProgressSqrt * 0.5f).xRot((-swingProgressSqrt) * 0.7f);
            anglerX = Mth.lerp(partialTick, player.xo, player.getX()) + vec3XRot.x;
            anglerY = Mth.lerp(partialTick, player.yo, player.getY()) + vec3XRot.y;
            anglerZ = Mth.lerp(partialTick, player.zo, player.getZ()) + vec3XRot.z;
            anglerEye = player.getEyeHeight();
        }
        float startX = (float) (anglerX - Mth.lerp(partialTick, fishingHook.xo, fishingHook.getX()));
        float startY = ((float) (anglerY - (Mth.lerp(partialTick, fishingHook.yo, fishingHook.getY()) + 0.25d))) + anglerEye;
        float startZ = (float) (anglerZ - Mth.lerp(partialTick, fishingHook.zo, fishingHook.getZ()));
        return new Vec3(startX, startY, startZ);
    }

    public static void submitFishingLine(GeoEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        float x = (float) state.fishingLineOrigin.x;
        float y = (float) state.fishingLineOrigin.y;
        float z = (float) state.fishingLineOrigin.z;
        float red = state.fishingLineColor[0], green = state.fishingLineColor[1], blue = state.fishingLineColor[2];
        float width = state.fishingLineWidth;
        collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, consumer) -> {
            for (int size = 0; size < 16; size++) {
                stringVertex(x, y, z, consumer, pose, fraction(size), fraction(size + 1), red, green, blue, width);
                stringVertex(x, y, z, consumer, pose, fraction(size + 1), fraction(size), red, green, blue, width);
            }
        });
    }

    @Unique
    private static float[] lineColor(FishingHook fishingHook) {
        return new float[]{0.0f, 0.0f, 0.0f};
    }

    @Unique
    private static float fraction(int i) {
        return i / 16.0f;
    }

    @Unique
    private static void stringVertex(float x, float y, float z, VertexConsumer vertexConsumer, PoseStack.Pose pose, float startFrac, float endFrac, float red, float green, float blue, float width) {
        float vx = x * startFrac;
        float vy = (y * ((startFrac * startFrac) + startFrac) * 0.5f) + 0.25f;
        float vz = z * startFrac;
        float dx = (x * endFrac) - vx;
        float dy = (((y * ((endFrac * endFrac) + endFrac)) * 0.5f) + 0.25f) - vy;
        float dz = (z * endFrac) - vz;
        float length = Mth.sqrt((dx * dx) + (dy * dy) + (dz * dz));
        vertexConsumer.addVertex(pose, vx, vy, vz).setColor(red, green, blue, 1.0f).setNormal(pose, dx / length, dy / length, dz / length).setLineWidth(width);
    }
}
