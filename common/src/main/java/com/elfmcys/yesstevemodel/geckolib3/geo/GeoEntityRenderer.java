package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.util.Color;
import com.elfmcys.yesstevemodel.geckolib3.util.EModelRenderCycle;
import com.elfmcys.yesstevemodel.geckolib3.util.IRenderCycle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public abstract class GeoEntityRenderer<TEntity extends Entity, T extends AnimatableEntity<TEntity>> extends EntityRenderer<TEntity, GeoEntityRenderState> implements IGeoRenderer<T> {

    public Matrix4f worldMatrix;

    public Matrix4f modelMatrix;

    private IRenderCycle renderState;

    public OrderedSubmitNodeCollector bufferSource;

    public GeoEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.worldMatrix = new Matrix4f();
        this.modelMatrix = new Matrix4f();
        this.renderState = EModelRenderCycle.INITIAL;
        this.bufferSource = null;
    }

    @Override
    public GeoEntityRenderState createRenderState() { return new GeoEntityRenderState(this); }

    public void extractEntity(T animatable, GeoEntityRenderState state, float bodyYaw, float partialTick) {
        AnimationEvent<?> event = animatable.processAnimation(partialTick);
        Minecraft minecraft = Minecraft.getInstance();
        if (event != null && minecraft.player != null) {
            Entity entity = animatable.getEntity();
            boolean visible = !entity.isInvisibleTo(minecraft.player);
            boolean glowing = minecraft.shouldEntityAppearGlowing(entity);
            state.renderType = getRenderType(animatable.getTextureLocation(), visible, glowing, animatable.getCurrentModel().getGeoModel().isTranslucentTexture(0));
            if (state.renderType != null && (visible || glowing)) {
                Color color = getRenderColor(animatable, partialTick, new PoseStack(), null, null, state.lightCoords);
                state.color.set(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
                setCurrentModelRenderCycle(EModelRenderCycle.INITIAL);
                state.rotation.rotationY((180.0f - bodyYaw) * 0.017453292f);
                state.packedOverlay = packOverlayCoords(entity, 0.0f);
                state.model = extractWithBone(animatable.getCurrentModel(), animatable, 0, 0, state.lightCoords);
            }
        }
    }

    @Override
    public void submit(GeoEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.model != null) {
            this.worldMatrix.set(poseStack.last().pose());
            poseStack.pushPose();
            poseStack.mulPose(state.rotation);
            renderWithBoneAndRenderType(state.model, state.renderType, poseStack, collector.order(0), camera.projectionMatrix, 0, state.packedOverlay, state.color, state.outlineColor);
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    public void renderEarly(T animatable, PoseStack poseStack, float partialTick, OrderedSubmitNodeCollector bufferSource, VertexConsumer buffer, int packedLight, int packedOverlayIn, float red, float green, float blue, float alpha) {
        this.modelMatrix = new Matrix4f(poseStack.last().pose());
        IGeoRenderer.super.renderEarly(animatable, poseStack, partialTick, bufferSource, buffer, packedLight, packedOverlayIn, red, green, blue, alpha);
    }

    public static int packOverlayCoords(Entity entity, float f) {
        return OverlayTexture.pack(OverlayTexture.u(f), OverlayTexture.v(false));
    }

    @Override
    @NotNull
    public IRenderCycle getCurrentModelRenderCycle() {
        return this.renderState;
    }

    @Override
    public void setCurrentModelRenderCycle(IRenderCycle cycle) {
        this.renderState = cycle;
    }

    @Override
    public void setCurrentRTB(OrderedSubmitNodeCollector bufferSource) {
        this.bufferSource = bufferSource;
    }

    @Override
    public OrderedSubmitNodeCollector getCurrentRTB() {
        return this.bufferSource;
    }
}