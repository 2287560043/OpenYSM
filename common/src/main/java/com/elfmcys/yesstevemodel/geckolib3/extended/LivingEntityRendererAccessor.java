package com.elfmcys.yesstevemodel.geckolib3.extended;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public interface LivingEntityRendererAccessor {
    void tlm$renderNameTag(EntityRenderState state, PoseStack poseStack, OrderedSubmitNodeCollector collector, CameraRenderState camera);
}