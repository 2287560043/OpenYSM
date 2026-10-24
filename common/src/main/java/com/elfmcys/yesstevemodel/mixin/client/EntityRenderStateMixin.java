package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.util.accessors.VehiclePassengerRenderState;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements VehiclePassengerRenderState {
    @Unique private PoseStack.Pose ysm$passengerPose;

    @Override
    public PoseStack.Pose ysm$getPassengerPose() { return ysm$passengerPose; }

    @Override
    public void ysm$setPassengerPose(PoseStack.Pose pose) { ysm$passengerPose = pose; }
}
