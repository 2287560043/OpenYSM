package com.elfmcys.yesstevemodel.fabric.mixin.client;

import com.mojang.blaze3d.GpuFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlDevice")
public abstract class GlDeviceBlurMixin {
    @ModifyArgs(method = "createTexture(Ljava/lang/String;ILcom/mojang/blaze3d/GpuFormat;IIII)Lcom/mojang/blaze3d/textures/GpuTexture;", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_texImage2D(IIIIIIIILjava/nio/ByteBuffer;)V"))
    private void ysm$clampBlurMipAllocation(Args args, String label, int usage, GpuFormat format, int width, int height, int depthOrLayers, int mipLevels) {
        if ("YSM blur capture".equals(label)) {
            args.set(3, Math.max(1, (int) args.get(3)));
            args.set(4, Math.max(1, (int) args.get(4)));
        }
    }
}
