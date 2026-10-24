package com.elfmcys.yesstevemodel.client.renderer.layer;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLayerRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import java.util.List;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.world.entity.player.Player;
import com.mojang.math.Axis;

public class CustomPlayerParrotLayer extends GeoLayerRenderer<CustomPlayerEntity> {

    private final ParrotModel parrotModel;

    public CustomPlayerParrotLayer(EntityRendererProvider.Context context) {
        this.parrotModel = new ParrotModel(context.bakeLayer(ModelLayers.PARROT));
    }

    @Override
    public void extract(PoseStack poseStack, List<Frame> frames, int packedLightIn, CustomPlayerEntity entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        Player player = entityLivingBaseIn.getEntity();
        AnimatedGeoModel model = entityLivingBaseIn.getCurrentModel();
        if (model == null) {
            return;
        }
        if (!model.leftShoulderBones().isEmpty()) {
            extractParrot(poseStack, frames, model, player, limbSwing, limbSwingAmount, netHeadYaw, headPitch, true);
        }
        if (!model.rightShoulderBones().isEmpty()) {
            extractParrot(poseStack, frames, model, player, limbSwing, limbSwingAmount, netHeadYaw, headPitch, false);
        }
    }

    private void extractParrot(PoseStack poseStack, List<Frame> frames, AnimatedGeoModel model, Player player, float limbSwing, float limbSwingAmount, float netHeadYaw, float headPitch, boolean isLeftShoulder) {
        (isLeftShoulder ? player.getShoulderParrotLeft() : player.getShoulderParrotRight()).ifPresent(variant -> {
            poseStack.pushPose();
            applyParrotTransform(poseStack, model, isLeftShoulder);
            poseStack.translate(0.0d, 1.5d, 0.0d);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
            ParrotRenderState state = new ParrotRenderState();
            state.pose = ParrotModel.Pose.ON_SHOULDER;
            state.ageInTicks = player.tickCount;
            state.walkAnimationPos = limbSwing;
            state.walkAnimationSpeed = limbSwingAmount;
            state.yRot = netHeadYaw;
            state.xRot = headPitch;
            frames.add(new ModelFrame<>(poseStack.last().copy(), this.parrotModel, state, this.parrotModel.renderType(ParrotRenderer.getVariantTexture(variant)), false));
            poseStack.popPose();
        });
    }

    public void applyParrotTransform(PoseStack poseStack, AnimatedGeoModel model, boolean isLeftShoulder) {
        if (isLeftShoulder) {
            RenderUtils.prepMatrixForLocator(poseStack, model.leftShoulderBones());
        } else {
            RenderUtils.prepMatrixForLocator(poseStack, model.rightShoulderBones());
        }
    }
}