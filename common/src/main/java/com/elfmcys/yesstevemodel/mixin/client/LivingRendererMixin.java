package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.geckolib3.extended.LivingEntityRendererAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({LivingEntityRenderer.class})
public abstract class LivingRendererMixin extends EntityRenderer<LivingEntity, EntityRenderState> implements LivingEntityRendererAccessor {
    public LivingRendererMixin(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    @Unique
    public void tlm$renderNameTag(EntityRenderState state, PoseStack poseStack, OrderedSubmitNodeCollector collector, CameraRenderState camera) {
        if (state.leashStates != null) {
            for (EntityRenderState.LeashState leash : state.leashStates) collector.submitLeash(poseStack, leash);
        }
        poseStack.pushPose();
        if (state.scoreText != null) {
            collector.submitNameTag(poseStack, state.nameTagAttachment, 0, state.scoreText, !state.isDiscrete, state.lightCoords, camera);
            poseStack.translate(0.0d, 0.25875d, 0.0d);
        }
        if (state.nameTag != null) {
            collector.submitNameTag(poseStack, state.nameTagAttachment, 0, state.nameTag, !state.isDiscrete, state.lightCoords, camera);
        }
        poseStack.popPose();
    }
}