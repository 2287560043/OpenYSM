package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.animation.AnimationRegister;
import com.elfmcys.yesstevemodel.client.input.AnimationRouletteKey;
import com.elfmcys.yesstevemodel.client.input.DebugAnimationKey;
import com.elfmcys.yesstevemodel.client.input.ExtraAnimationKey;
import com.elfmcys.yesstevemodel.client.input.ExtraPlayerRenderKey;
import com.elfmcys.yesstevemodel.client.input.PlayerModelToggleKey;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import rip.ysm.gpu.GpuSkinFeatureRenderer;

public final class ClientSetupEvent {

    private ClientSetupEvent() {
    }

    public static void register() {
        registerKeyMappings();
        if (YesSteveModel.isAvailable()) {
            AnimationRegister.registerAnimationState();
        }
        if (YesSteveModel.isAvailable()) {
            GpuSkinFeatureRenderer.register();
        }
    }

    private static void registerKeyMappings() {
        KeyMappingRegistry.register(PlayerModelToggleKey.KEY_MAPPING);
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        KeyMappingRegistry.register(AnimationRouletteKey.KEY_ROULETTE);
        KeyMappingRegistry.register(AnimationRouletteKey.KEY_LOCK);
        KeyMappingRegistry.register(DebugAnimationKey.KEY_MAPPING);
        KeyMappingRegistry.register(ExtraPlayerRenderKey.KEY_MAPPING);
        for (KeyMapping mapping : ExtraAnimationKey.getKeyMappings()) {
            KeyMappingRegistry.register(mapping);
        }
    }

}
