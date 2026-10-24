package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.client.renderer.CustomEntityTranslucentRenderType;
import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.util.Color;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.EModelRenderCycle;
import com.elfmcys.yesstevemodel.geckolib3.util.IRenderCycle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IGeoRenderer<T extends AnimatableEntity<?>> {
    OrderedSubmitNodeCollector getCurrentRTB();

    default void setCurrentRTB(OrderedSubmitNodeCollector bufferSource) {
    }

    default void renderWithBone(AnimatedGeoModel model, T animatable, float partialTick, PoseStack poseStack, @Nullable OrderedSubmitNodeCollector bufferSource, @Nullable VertexConsumer vertexConsumer, int packedLight, int packedOverlayIn, float red, float green, float blue, float alpha) {
        setCurrentRTB(bufferSource);
        renderEarly(animatable, poseStack, partialTick, bufferSource, vertexConsumer, packedLight, packedOverlayIn, red, green, blue, alpha);
        renderLate(animatable, poseStack, partialTick, bufferSource, vertexConsumer, packedLight, packedOverlayIn, red, green, blue, alpha);
    }

    default NativeModelRenderer.Frame extractWithBone(AnimatedGeoModel model, T animatable, int textureIndex, int renderPartMask, int packedLight) {
        animatable.resetAnimationState();
        NativeModelRenderer.Frame frame = NativeModelRenderer.extract(model.getGeoModel(), model.getMatrixData(), model.getAbsPivotData(), textureIndex, renderPartMask, packedLight);
        setCurrentModelRenderCycle(EModelRenderCycle.REPEATED);
        return frame;
    }

    default void renderWithBoneAndRenderType(NativeModelRenderer.Frame frame, RenderType renderType, PoseStack poseStack, OrderedSubmitNodeCollector collector, org.joml.Matrix4f projection, int renderPartMask, int packedOverlay, org.joml.Vector4f color, int outlineColor) {
        NativeModelRenderer.submit(frame, poseStack, collector, projection, renderType, renderPartMask, packedOverlay, color, outlineColor);
    }

    default void renderEarly(T animatable, PoseStack poseStack, float partialTick,
                             @Nullable OrderedSubmitNodeCollector bufferSource, @Nullable VertexConsumer buffer, int packedLight,
                             int packedOverlayIn, float red, float green, float blue, float alpha) {
        if (getCurrentModelRenderCycle() == EModelRenderCycle.INITIAL) {
            float width = animatable.getHeightScale();
            float height = animatable.getWidthScale();
            poseStack.scale(width, height, width);
        }
    }

    default void renderLate(T animatable, PoseStack poseStack, float partialTick, OrderedSubmitNodeCollector bufferSource,
                            @Nullable VertexConsumer buffer, int packedLight, int packedOverlayIn, float red, float green, float blue,
                            float alpha) {
    }

    @Nullable
    default RenderType getRenderType(Identifier resourceLocation, boolean z, boolean z2, boolean z3) {
        if (z) {
            if (z3) {
                return CustomEntityTranslucentRenderType.get(resourceLocation);
            }
            return net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(resourceLocation);
        }
        if (z2) {
            return net.minecraft.client.renderer.rendertype.RenderTypes.outline(resourceLocation);
        }
        return null;
    }

    default Color getRenderColor(T animatable, float partialTick, PoseStack poseStack, @Nullable OrderedSubmitNodeCollector bufferSource, @Nullable VertexConsumer buffer, int packedLight) {
        return Color.WHITE;
    }

    @NotNull
    default IRenderCycle getCurrentModelRenderCycle() {
        return EModelRenderCycle.INITIAL;
    }

    default void setCurrentModelRenderCycle(IRenderCycle cycle) {
    }
}