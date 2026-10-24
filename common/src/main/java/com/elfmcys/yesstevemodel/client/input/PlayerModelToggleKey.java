package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.gui.DisclaimerScreen;
import com.elfmcys.yesstevemodel.client.gui.ExtraPlayerConfigScreen;
import com.elfmcys.yesstevemodel.client.gui.PlayerModelScreen;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.util.InputUtil;
import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientRawInputEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import rip.ysm.api.PlatformAPI;
import rip.ysm.api.client.KeyMappingFactory;

public final class PlayerModelToggleKey {

    public static final KeyMapping KEY_MAPPING = KeyMappingFactory.createInGameAlt("key.yes_steve_model.player_model.desc", InputConstants.Type.KEYSYM, 89, "key.category.yes_steve_model");

    private PlayerModelToggleKey() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        ClientRawInputEvent.KEY_PRESSED.register((client, action, event) -> {
            onKeyInput(action, event.key(), event.scancode());
            return EventResult.pass();
        });
    }

    private static void onKeyInput(int action, int keyCode, int scanCode) {
        if (InputUtil.isPlayerReady() && action == 1 && InputUtil.isKeyPressed(keyCode, scanCode, KEY_MAPPING)) {
            if (!YesSteveModel.isAvailable()) {
                YesSteveModel.sendUnavailableMessage();
                return;
            }
            if (NetworkHandler.isClientConnected() && !ServerConfig.CAN_SWITCH_MODEL.get()) {
                Minecraft.getInstance().setScreenAndShow(new ExtraPlayerConfigScreen(null));
            } else if (GeneralConfig.DISCLAIMER_SHOW.get()) {
                Minecraft.getInstance().setScreenAndShow(new DisclaimerScreen());
            } else {
                Minecraft.getInstance().setScreenAndShow(new PlayerModelScreen());
            }
        }
    }
}