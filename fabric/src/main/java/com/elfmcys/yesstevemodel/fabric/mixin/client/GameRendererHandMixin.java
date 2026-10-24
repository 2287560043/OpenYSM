package com.elfmcys.yesstevemodel.fabric.mixin.client;

import com.elfmcys.yesstevemodel.fabric.client.GameRendererHandState;
import com.elfmcys.yesstevemodel.geckolib3.geo.NativeModelRenderer;

import com.elfmcys.yesstevemodel.client.event.ReplacePlayerHandRenderEvent;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.GameType;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererHandMixin implements GameRendererHandState {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private GameRenderState gameRenderState;
    @Shadow @Final public ItemInHandRenderer itemInHandRenderer;
    @Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;
    @Shadow protected abstract void bobHurt(CameraRenderState camera, PoseStack pose);
    @Shadow protected abstract void bobView(CameraRenderState camera, PoseStack pose);
    @Unique private final SubmitNodeStorage ysm$hands = new SubmitNodeStorage();
    @Unique private final Matrix4f ysm$projection = new Matrix4f();
    @Unique private float ysm$partialTick;
    @Unique private boolean ysm$frozen;

    @Override public Matrix4f ysm$handProjection() { return ysm$projection; }
    @Override public float ysm$handPartialTick() { return ysm$partialTick; }

    @Inject(method = "extract", at = @At("TAIL"))
    private void ysm$extractHands(DeltaTracker delta, boolean renderLevel, CallbackInfo ci) {
        ysm$frozen = false;
        ysm$hands.getSubmitsPerOrder().clear();
        LocalPlayer player = minecraft.player;
        if (!renderLevel || !minecraft.isGameLoadFinished() || minecraft.level == null || player == null || !ReplacePlayerHandRenderEvent.hasCustomArms(player)) return;
        CameraRenderState camera = gameRenderState.levelRenderState.cameraRenderState;
        ysm$frozen = true;
        if (camera.isPanoramicMode || !gameRenderState.optionsRenderState.cameraType.isFirstPerson() || camera.entityRenderState.isSleeping || gameRenderState.guiRenderState.isHudHidden || minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) return;
        ysm$partialTick = delta.getGameTimeDeltaPartialTick(false);
        ysm$projection.setPerspective(camera.hudFov * 0.017453292f, (float) gameRenderState.windowRenderState.width / gameRenderState.windowRenderState.height, 0.05f, 100.0f).mul(camera.viewRotationMatrix);
        PoseStack pose = new PoseStack();
        pose.mulPose(camera.viewRotationMatrix.invert(new Matrix4f()));
        bobHurt(camera, pose);
        if (gameRenderState.optionsRenderState.bobView) bobView(camera, pose);
        itemInHandRenderer.submitHandsWithItems(ysm$partialTick, pose, ysm$hands, player, minecraft.getEntityRenderDispatcher().getPackedLightCoords(player, ysm$partialTick));
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void ysm$renderFrozenHands(CameraRenderState camera, float partialTick, Matrix4fc viewRotation, CallbackInfo ci) {
        if (!ysm$frozen) return;
        RenderSystem.getModelViewStack().pushMatrix().mul(viewRotation);
        featureRenderDispatcher.renderAllFeatures(ysm$hands);
        RenderSystem.getModelViewStack().popMatrix();
        ci.cancel();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void ysm$releaseFrameMemory(DeltaTracker delta, boolean renderLevel, CallbackInfo ci) { NativeModelRenderer.endFrame(); }

    @Inject(method = "close", at = @At("TAIL"))
    private void ysm$closeFrameMemory(CallbackInfo ci) { NativeModelRenderer.closeFrameMemory(); }

}
