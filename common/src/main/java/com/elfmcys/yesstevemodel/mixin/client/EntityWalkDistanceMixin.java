package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.util.accessors.EntityWalkDistanceAccessor;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityWalkDistanceMixin implements EntityWalkDistanceAccessor {
    @Unique private float ysm$walkDistance;

    @Shadow protected abstract Entity.MovementEmission getMovementEmission();
    @Shadow public abstract boolean isPassenger();

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getBlockSpeedFactor()F"))
    private void ysm$recordWalkDistance(MoverType mover, Vec3 delta, CallbackInfo ci, @Local(index = 4) Vec3 movement) {
        if (getMovementEmission().emitsAnything() && !isPassenger()) ysm$walkDistance += (float) movement.horizontalDistance() * 0.6f;
    }

    @Override
    public float ysm$getWalkDistance() { return ysm$walkDistance; }
}
