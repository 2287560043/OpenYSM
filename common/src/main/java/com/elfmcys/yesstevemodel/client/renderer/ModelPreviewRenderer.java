package com.elfmcys.yesstevemodel.client.renderer;

import rip.ysm.compat.firstperson.FirstPersonCompat;
import rip.ysm.compat.oculus.OculusCompat;
import com.elfmcys.yesstevemodel.client.animation.AnimationTracker;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoReplacedEntityRenderer;
import com.elfmcys.yesstevemodel.client.entity.IPreviewAnimatable;
import com.elfmcys.yesstevemodel.client.entity.PlayerPreviewEntity;
import com.elfmcys.yesstevemodel.util.AnimatableCacheUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLivingEntityRenderState;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.joml.Quaternionf;

import java.util.concurrent.ExecutionException;
import com.mojang.math.Axis;

public final class ModelPreviewRenderer extends EntityRenderer<Entity, ModelPreviewRenderer.RenderState> {

    private static boolean isPreviewMode = false;

    private static boolean isExtraPlayerMode = false;

    private static boolean isFirstPersonMode = false;

    public static void setPreviewMode(boolean previewMode) {
        isPreviewMode = previewMode;
    }

    public static boolean isPreview() {
        return isPreviewMode;
    }

    public static void setExtraPlayerMode(boolean extraPlayerMode) {
        isExtraPlayerMode = extraPlayerMode;
    }

    public static boolean isExtraPlayer() {
        return isExtraPlayerMode;
    }

    public static void setFirstPersonMode(boolean firstPersonMode) {
        isFirstPersonMode = firstPersonMode;
    }

    public static boolean isFirstPerson() {
        return isFirstPersonMode || OculusCompat.isPBRActive() || FirstPersonCompat.isFirstPersonActive();
    }

    public static boolean isFirstPersonOnRenderThread() {
        RenderSystem.assertOnRenderThread();
        return isFirstPersonMode && !FirstPersonCompat.isFirstPersonActive();
    }

    private final SubmitNodeStorage background = new SubmitNodeStorage();

    public ModelPreviewRenderer(EntityRendererProvider.Context context) { super(context); }

    public static final class RenderState extends EntityRenderState {
        public final ModelPreviewRenderer renderer;
        public EntityRenderState model;
        public PoseStack.Pose modelPose;
        public boolean directModel;
        public ScreenRectangle area;
        public final List<VehicleFrame> vehicles = new ArrayList<>();
        public final List<BlockFrame> blocks = new ArrayList<>();

        public RenderState(ModelPreviewRenderer renderer) { this.renderer = renderer; }
    }

    public record VehicleFrame(EntityRenderState state, PoseStack.Pose pose) {}
    public record BlockFrame(BlockModelRenderState state, PoseStack.Pose pose) {}

    @Override
    public RenderState createRenderState() { return new RenderState(this); }

    @Override
    public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        camera.projectionMatrix.setOrtho(0, state.area.width(), state.area.height(), 0, 1000, -1000);
        for (VehicleFrame frame : state.vehicles) {
            poseStack.pushPose();
            poseStack.mulPose(frame.pose().pose());
            this.entityRenderDispatcher.submit(frame.state(), camera, 0, 0, 0, poseStack, this.background);
            poseStack.popPose();
        }
        for (BlockFrame frame : state.blocks) {
            poseStack.pushPose();
            poseStack.mulPose(frame.pose().pose());
            frame.state().submit(poseStack, this.background, 15728880, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        if (!state.vehicles.isEmpty() || !state.blocks.isEmpty()) {
            Minecraft.getInstance().gameRenderer.featureRenderDispatcher().renderAllFeatures(this.background);
        }
        poseStack.pushPose();
        poseStack.mulPose(state.modelPose.pose());
        if (state.directModel) {
            GeoLivingEntityRenderState model = (GeoLivingEntityRenderState) state.model;
            model.renderer.submit(model, poseStack, collector, camera);
        } else {
            this.entityRenderDispatcher.submit(state.model, camera, 0, 0, 0, poseStack, collector);
        }
        poseStack.popPose();
    }

    public static void renderEntityPreview(GuiGraphicsExtractor guiGraphics, ScreenRectangle area, float x, float y, float scale, float pitch, float yaw, float partialTick, AnimatableEntity animatableEntity, GeoReplacedEntityRenderer renderer, boolean renderGround) {
        LivingEntity livingEntity = (LivingEntity) animatableEntity.getEntity();
        RenderState state = RendererManager.getPreviewRenderer().createRenderState();
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0d, 0.8d, 0.0d);
        Quaternionf rotationX = Axis.XP.rotationDegrees(-10.0f + pitch);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f).mul(rotationX));
        float oldBodyRot = livingEntity.yBodyRot;
        float oldBodyRotO = livingEntity.yBodyRotO;
        float oldYRot = livingEntity.getYRot();
        float oldYRotO = livingEntity.yRotO;
        float oldXRot = livingEntity.getXRot();
        float oldXRotO = livingEntity.xRotO;
        float oldHeadRotO = livingEntity.yHeadRotO;
        float oldHeadRot = livingEntity.yHeadRot;
        Pose oldPose = livingEntity.getPose();
        setPreviewMode(true);
        try {
            livingEntity.yBodyRot = -yaw;
            livingEntity.yBodyRotO = -yaw;
            livingEntity.setYRot(180.0f);
            livingEntity.yRotO = 180.0f;
            livingEntity.setXRot(0.0f);
            livingEntity.xRotO = 0.0f;
            livingEntity.yHeadRot = -yaw;
            livingEntity.yHeadRotO = -yaw;
            AnimationTracker animationTracker = ((IPreviewAnimatable) animatableEntity).getAnimationStateMachine();
            if (animationTracker.isCurrentAnimation("sleep")) {
                poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0f));
                poseStack.translate(0.5d, 0.5625d, 0.0d);
                livingEntity.setPose(Pose.SLEEPING);
            }
            if (animationTracker.isCurrentAnimation("swim") || animationTracker.isCurrentAnimation("swim_stand")) {
                livingEntity.setPose(Pose.SWIMMING);
            }
            if (animationTracker.isCurrentAnimation("sneak") || animationTracker.isCurrentAnimation("sneaking")) {
                livingEntity.setPose(Pose.CROUCHING);
            }
            if (animationTracker.isCurrentAnimation("sit")) poseStack.translate(0.0d, -0.5d, 0.0d);
            if (animationTracker.isCurrentAnimation("ride")) poseStack.translate(0.0d, 0.85d, 0.0d);
            if (animationTracker.isCurrentAnimation("ride_pig")) poseStack.translate(0.0d, 0.3125d, 0.0d);
            if (animationTracker.isCurrentAnimation("boat")) poseStack.translate(0.0d, -0.45d, 0.0d);
            try {
                renderVehicleForAnimation(yaw, animatableEntity, partialTick, poseStack, state);
            } catch (ExecutionException exception) {
                throw new RuntimeException(exception);
            }
            if (animationTracker.isCurrentAnimation("sleep")) renderBedPreview(pitch, yaw, state);
            if (renderGround) renderGroundPreview(pitch, yaw, state);
            state.model = renderer.extractPreviewState((LivingAnimatable) animatableEntity, partialTick);
            state.modelPose = poseStack.last().copy();
            state.directModel = true;
        } finally {
            livingEntity.yBodyRot = oldBodyRot;
            livingEntity.yBodyRotO = oldBodyRotO;
            livingEntity.setYRot(oldYRot);
            livingEntity.yRotO = oldYRotO;
            livingEntity.setXRot(oldXRot);
            livingEntity.xRotO = oldXRotO;
            livingEntity.yHeadRotO = oldHeadRotO;
            livingEntity.yHeadRot = oldHeadRot;
            livingEntity.setPose(oldPose);
            setPreviewMode(false);
        }
        submitPreview(guiGraphics, area, state, x, y, scale, rotationX.conjugate());
    }

    private static void renderBedPreview(float pitch, float yaw, RenderState state) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0d, 0.8d, 0.0d);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f).mul(Axis.XP.rotationDegrees(-10.0f + pitch)));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw + 180.0f));
        poseStack.translate(-0.5d, 0.0d, 0.5d);
        extractBlock(Blocks.BED.red().defaultBlockState(), poseStack, state);
    }

    private static void renderGroundPreview(float pitch, float yaw, RenderState state) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0d, 0.8d, 0.0d);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f).mul(Axis.XP.rotationDegrees(-10.0f + pitch)));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.translate(-1.5d, -1.0d, -2.5d);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                poseStack.translate(0.0f, 0.0f, 1.0f);
                extractBlock(Blocks.GRASS_BLOCK.defaultBlockState(), poseStack, state);
            }
            poseStack.translate(1.0f, 0.0f, -3.0f);
        }
        poseStack.translate(-1.0f, 1.0f, 1.0f);
        extractBlock(Blocks.SHORT_GRASS.defaultBlockState(), poseStack, state);
        poseStack.translate(0.0f, 0.0f, 1.0f);
        extractBlock(Blocks.RED_TULIP.defaultBlockState(), poseStack, state);
    }

    private static void extractBlock(BlockState block, PoseStack poseStack, RenderState state) {
        BlockModelRenderState model = new BlockModelRenderState();
        Minecraft.getInstance().getModelManager().getBlockModelSet().get(block).update(model, block, BlockDisplayContext.create(), 42);
        state.blocks.add(new BlockFrame(model, poseStack.last().copy()));
    }

    private static void renderVehicleForAnimation(float yaw, AnimatableEntity animatableEntity, float partialTick, PoseStack poseStack, RenderState state) throws ExecutionException {
        Entity entity = animatableEntity.getEntity();
        AnimationTracker animationTracker = ((IPreviewAnimatable) animatableEntity).getAnimationStateMachine();
        if (animationTracker.isCurrentAnimation("ride")) {
            renderVehicleEntity(yaw, entity, poseStack, state, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityTypes.HORSE), () -> PlayerPreviewEntity.assignPreviewId(EntityTypes.HORSE.create(entity.level(), EntitySpawnReason.LOAD))), partialTick);
        } else if (animationTracker.isCurrentAnimation("ride_pig")) {
            renderVehicleEntity(yaw, entity, poseStack, state, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityTypes.PIG), () -> PlayerPreviewEntity.assignPreviewId(EntityTypes.PIG.create(entity.level(), EntitySpawnReason.LOAD))), partialTick);
        } else if (animationTracker.isCurrentAnimation("boat")) {
            renderVehicleEntity(yaw, entity, poseStack, state, AnimatableCacheUtil.ENTITIES_CACHE.get(EntityType.getKey(EntityTypes.OAK_BOAT), () -> PlayerPreviewEntity.assignPreviewId(EntityTypes.OAK_BOAT.create(entity.level(), EntitySpawnReason.LOAD))), partialTick);
        }
    }

    private static void renderVehicleEntity(float yaw, Entity riderEntity, PoseStack poseStack, RenderState state, Entity vehicleEntity, float partialTick) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        Vec3 seat = vehicleEntity.getPassengerRidingPosition(riderEntity).subtract(vehicleEntity.position()).subtract(riderEntity.getVehicleAttachmentPoint(vehicleEntity));
        poseStack.translate(0.0d, -seat.y, 0.0d);
        EntityRenderState vehicle = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(vehicleEntity, partialTick);
        vehicle.lightCoords = 15728880;
        vehicle.shadowPieces.clear();
        state.vehicles.add(new VehicleFrame(vehicle, poseStack.last().copy()));
        poseStack.popPose();
    }

    public static <T extends LivingEntity, TAnimatable extends LivingAnimatable<T>> void renderLivingEntityPreview(GuiGraphicsExtractor guiGraphics, ScreenRectangle area, float x, float y, float scale, float partialTick, TAnimatable animatable, GeoReplacedEntityRenderer<T, TAnimatable> renderer, boolean disablePreviewRotation, boolean hideEquipment) {
        LivingEntity livingEntity = animatable.getEntity();
        RenderState state = RendererManager.getPreviewRenderer().createRenderState();
        PoseStack poseStack = new PoseStack();
        Quaternionf rotationX = Axis.XP.rotationDegrees(disablePreviewRotation ? 0.0f : -10.0f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f).mul(rotationX));
        float oldBodyRot = livingEntity.yBodyRot;
        float oldBodyRotO = livingEntity.yBodyRotO;
        float oldYRot = livingEntity.getYRot();
        float oldYRotO = livingEntity.yRotO;
        float oldXRot = livingEntity.getXRot();
        float oldXRotO = livingEntity.xRotO;
        float oldHeadRotO = livingEntity.yHeadRotO;
        float oldHeadRot = livingEntity.yHeadRot;
        int[] slots = hideEquipment && livingEntity instanceof Player player ? new int[]{player.getInventory().getSelectedSlot(), 40, 36, 37, 38, 39} : null;
        ItemStack[] savedEquipment = slots != null ? new ItemStack[slots.length] : null;
        if (savedEquipment != null) {
            for (int i = 0; i < slots.length; i++) savedEquipment[i] = ((Player) livingEntity).getInventory().getItem(slots[i]);
        }
        setPreviewMode(true);
        try {
            if (savedEquipment != null) {
                for (int slot : slots) ((Player) livingEntity).getInventory().setItem(slot, ItemStack.EMPTY);
            }
            float previewYaw = disablePreviewRotation ? 180.0f : 200.0f;
            livingEntity.yBodyRot = previewYaw;
            livingEntity.yBodyRotO = previewYaw;
            livingEntity.setYRot(previewYaw);
            livingEntity.yRotO = previewYaw;
            livingEntity.setXRot(0.0f);
            livingEntity.xRotO = 0.0f;
            livingEntity.yHeadRot = livingEntity.getYRot();
            livingEntity.yHeadRotO = livingEntity.getYRot();
            Entity vehicle = livingEntity.getVehicle();
            if (vehicle instanceof LivingEntity) {
                float vehicleYaw = vehicle.getYRot();
                poseStack.mulPose(Axis.YP.rotationDegrees(vehicleYaw - previewYaw));
                livingEntity.yHeadRot = vehicleYaw;
                livingEntity.yHeadRotO = vehicleYaw;
            }
            state.model = renderer.extractPreviewState(animatable, partialTick);
            state.modelPose = poseStack.last().copy();
            state.directModel = true;
        } finally {
            livingEntity.yBodyRot = oldBodyRot;
            livingEntity.yBodyRotO = oldBodyRotO;
            livingEntity.setYRot(oldYRot);
            livingEntity.yRotO = oldYRotO;
            livingEntity.setXRot(oldXRot);
            livingEntity.xRotO = oldXRotO;
            livingEntity.yHeadRotO = oldHeadRotO;
            livingEntity.yHeadRot = oldHeadRot;
            if (savedEquipment != null) {
                for (int i = 0; i < slots.length; i++) ((Player) livingEntity).getInventory().setItem(slots[i], savedEquipment[i]);
            }
            setPreviewMode(false);
        }
        submitPreview(guiGraphics, area, state, x, y + (disablePreviewRotation ? 5.5f : 0), scale, rotationX.conjugate());
    }

    public static void renderInventoryPreview(GuiGraphicsExtractor guiGraphics, ScreenRectangle area, int x, int y, int scale, float mouseX, float mouseY, LivingEntity entity) {
        float yaw = (float) Math.atan(mouseX / 40.0f);
        float pitch = (float) Math.atan(mouseY / 40.0f);
        Quaternionf rotationX = new Quaternionf().rotateX(pitch * 20.0f * 0.017453292f);
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(new Quaternionf().rotateZ((float) Math.PI).mul(rotationX));
        float oldBodyRot = entity.yBodyRot;
        float oldYRot = entity.getYRot();
        float oldXRot = entity.getXRot();
        float oldHeadRotO = entity.yHeadRotO;
        float oldHeadRot = entity.yHeadRot;
        RenderState state = RendererManager.getPreviewRenderer().createRenderState();
        try {
            entity.yBodyRot = 180.0f + yaw * 20.0f;
            entity.setYRot(180.0f + yaw * 40.0f);
            entity.setXRot(-pitch * 20.0f);
            entity.yHeadRot = entity.getYRot();
            entity.yHeadRotO = entity.getYRot();
            state.model = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(entity, 1);
            state.model.lightCoords = 15728880;
            state.model.shadowPieces.clear();
            state.modelPose = poseStack.last().copy();
        } finally {
            entity.yBodyRot = oldBodyRot;
            entity.setYRot(oldYRot);
            entity.setXRot(oldXRot);
            entity.yHeadRotO = oldHeadRotO;
            entity.yHeadRot = oldHeadRot;
        }
        submitPreview(guiGraphics, area, state, x, y, scale, rotationX.conjugate());
    }

    public static void renderPlayerOverlay(GuiGraphicsExtractor guiGraphics, LocalPlayer localPlayer, double x, double y, float scale, float yawOffset, int zDepth, float partialTick) {
        RenderState state = RendererManager.getPreviewRenderer().createRenderState();
        Quaternionf rotationY = Axis.YP.rotationDegrees(Mth.lerp(partialTick, localPlayer.yBodyRotO, localPlayer.yBodyRot) + yawOffset - 180.0f);
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.1f).mul(rotationY));
        setExtraPlayerMode(true);
        try {
            state.model = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(localPlayer, partialTick);
            state.model.lightCoords = 15728880;
            state.model.shadowPieces.clear();
            state.modelPose = poseStack.last().copy();
        } finally {
            setExtraPlayerMode(false);
        }
        guiGraphics.nextStratum();
        submitPreview(guiGraphics, new ScreenRectangle(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight()), state, (float) (x + scale * 0.5d), (float) (y + scale * 2.0f), scale, rotationY.conjugate());
    }

    public static void submitPreview(GuiGraphicsExtractor guiGraphics, ScreenRectangle area, RenderState state, float x, float y, float scale, Quaternionf camera) {
        if (scale == 0) return;
        state.area = area;
        Vector3f translation = new Vector3f((x - (area.left() + area.width() / 2.0f)) / scale, (y - (area.top() + area.height() / 2.0f)) / scale, 0);
        Quaternionf cameraAngle = new Quaternionf(camera).rotateY(-(float) Math.PI).conjugate();
        guiGraphics.entity(state, scale, translation, new Quaternionf(), cameraAngle, area.left(), area.top(), area.right(), area.bottom());
    }
}
