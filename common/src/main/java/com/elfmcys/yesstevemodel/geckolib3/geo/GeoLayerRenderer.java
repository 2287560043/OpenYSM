package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.geckolib3.core.AnimatableEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import java.util.List;

public abstract class GeoLayerRenderer<T extends AnimatableEntity<?>> {
    public abstract void extract(PoseStack poseStack, List<Frame> frames, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch);

    public interface Frame {
        void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight);
    }

    public record ItemFrame(PoseStack.Pose pose, ItemStackRenderState item) implements Frame {
        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight) {
            poseStack.pushPose();
            poseStack.mulPose(pose.pose());
            item.submit(poseStack, collector, packedLight, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    public record ModelFrame<S>(PoseStack.Pose pose, net.minecraft.client.model.Model<? super S> model, S state, RenderType renderType, boolean foil) implements Frame {
        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight) {
            poseStack.pushPose();
            poseStack.mulPose(pose.pose());
            collector.submitModel(model, state, poseStack, renderType, packedLight, OverlayTexture.NO_OVERLAY, -1, null, 0, null);
            if (foil) collector.order(1).submitModel(model, state, poseStack, RenderTypes.armorEntityGlint(), packedLight, OverlayTexture.NO_OVERLAY, -1, null, 0, null);
            poseStack.popPose();
        }
    }
}