package com.elfmcys.yesstevemodel.fabric.mixin.client;

import com.elfmcys.yesstevemodel.fabric.client.GameRendererHandState;

import com.elfmcys.yesstevemodel.client.event.ReplacePlayerHandRenderEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class PlayerRendererHandMixin {

    @Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true)
    private void ysm$onRenderRightHand(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, Identifier texture, boolean sleeve, CallbackInfo ci) {
        GameRendererHandState state = (GameRendererHandState) Minecraft.getInstance().gameRenderer;
        if (ReplacePlayerHandRenderEvent.onExtractArm(Minecraft.getInstance().player, HumanoidArm.RIGHT, poseStack, collector, state.ysm$handProjection(), packedLight, state.ysm$handPartialTick())) {
            ci.cancel();
        }
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"), cancellable = true)
    private void ysm$onRenderLeftHand(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, Identifier texture, boolean sleeve, CallbackInfo ci) {
        GameRendererHandState state = (GameRendererHandState) Minecraft.getInstance().gameRenderer;
        if (ReplacePlayerHandRenderEvent.onExtractArm(Minecraft.getInstance().player, HumanoidArm.LEFT, poseStack, collector, state.ysm$handProjection(), packedLight, state.ysm$handPartialTick())) {
            ci.cancel();
        }
    }
}
