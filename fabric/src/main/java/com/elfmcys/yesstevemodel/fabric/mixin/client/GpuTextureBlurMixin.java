package com.elfmcys.yesstevemodel.fabric.mixin.client;

import com.mojang.blaze3d.textures.GpuTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GpuTexture.class)
public abstract class GpuTextureBlurMixin {
    @Shadow public abstract String getLabel();

    @Inject(method = {"getWidth", "getHeight"}, at = @At("RETURN"), cancellable = true)
    private void ysm$clampBlurMipSize(int mipLevel, CallbackInfoReturnable<Integer> ci) {
        if ("YSM blur capture".equals(getLabel())) ci.setReturnValue(Math.max(1, ci.getReturnValueI()));
    }
}
