package com.elfmcys.yesstevemodel.fabric.mixin.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rip.ysm.gpu.BlurShader;
import java.util.List;
import java.util.function.Supplier;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererBlurMixin {
    @Shadow @Final private List<?> draws;
    @Shadow protected abstract void executeDrawRange(Supplier<String> label, RenderTarget target, GpuBufferSlice transforms, int from, int to);

    @Inject(method = "executeDrawRange", at = @At("HEAD"), cancellable = true)
    private void ysm$captureBlur(Supplier<String> label, RenderTarget target, GpuBufferSlice transforms, int from, int to, CallbackInfo ci) {
        int start = from;
        boolean captured = false;
        for (int i = from; i < to; i++) {
            var capture = BlurShader.captureFor(((GuiDrawAccessor) draws.get(i)).ysm$textureSetup().texure0());
            if (capture == null || capture.captured) continue;
            if (i > start) executeDrawRange(label, target, transforms, start, i);
            capture.capture(target);
            start = i;
            captured = true;
        }
        if (captured) {
            executeDrawRange(label, target, transforms, start, to);
            ci.cancel();
        }
    }

    @Inject(method = "endFrame", at = @At("TAIL"))
    private void ysm$endBlurFrame(CallbackInfo ci) { BlurShader.endFrame(); }

    @Inject(method = "close", at = @At("TAIL"))
    private void ysm$closeBlur(CallbackInfo ci) { BlurShader.close(); }
}
