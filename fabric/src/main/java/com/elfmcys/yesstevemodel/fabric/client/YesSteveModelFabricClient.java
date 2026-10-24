package com.elfmcys.yesstevemodel.fabric.client;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.renderer.AnimationDebugOverlay;
import com.elfmcys.yesstevemodel.client.renderer.ExtraPlayerOverlay;
import com.elfmcys.yesstevemodel.client.renderer.ModelSyncStateOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import rip.ysm.api.client.HudOverlay;

public final class YesSteveModelFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HudOverlay debugOverlay = AnimationDebugOverlay.createOverlay();
        HudOverlay loadingOverlay = new ExtraPlayerOverlay();
        HudOverlay syncOverlay = new ModelSyncStateOverlay();
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("yes_steve_model", "overlays"), (guiGraphics, deltaTracker) -> {
            float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(false);
            Minecraft mc = Minecraft.getInstance();
            int w = guiGraphics.guiWidth();
            int h = guiGraphics.guiHeight();
            debugOverlay.render(guiGraphics, mc.font, tickDelta, w, h);
            loadingOverlay.render(guiGraphics, mc.font, tickDelta, w, h);
            syncOverlay.render(guiGraphics, mc.font, tickDelta, w, h);
        });

        ClientModelManager.loadDefaultModel();
    }
}
