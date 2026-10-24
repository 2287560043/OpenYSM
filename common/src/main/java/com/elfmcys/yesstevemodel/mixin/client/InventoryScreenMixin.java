package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InventoryScreen.class})
public class InventoryScreenMixin {
    @Inject(at = {@At("HEAD")}, method = {"extractEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V"}, remap = false)
    private static void renderEntityInInventoryFollowsAnglePre(GuiGraphicsExtractor guiGraphics, int x1, int y1, int x2, int y2, int scale, float offset, float mouseX, float mouseY, LivingEntity entity, CallbackInfo ci) {
        ModelPreviewRenderer.setPreviewMode(true);
    }

    @Inject(at = {@At("RETURN")}, method = {"extractEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V"}, remap = false)
    private static void renderEntityInInventoryFollowsAnglePost(GuiGraphicsExtractor guiGraphics, int x1, int y1, int x2, int y2, int scale, float offset, float mouseX, float mouseY, LivingEntity entity, CallbackInfo ci) {
        ModelPreviewRenderer.setPreviewMode(false);
    }
}