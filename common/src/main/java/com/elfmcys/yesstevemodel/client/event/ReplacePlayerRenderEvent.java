package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.util.CameraUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLivingEntityRenderState;
import net.minecraft.world.entity.player.Player;
import rip.ysm.compat.firstperson.FirstPersonCompat;
import rip.ysm.compat.playeranimator.PlayerAnimatorCompat;
import rip.ysm.compat.realcamera.RealCameraCompat;

public class ReplacePlayerRenderEvent {

    private ReplacePlayerRenderEvent() {
    }

    public static GeoLivingEntityRenderState extractPlayer(Player entity, float partialTick) {
        if (!YesSteveModel.isAvailable()) {
            return null;
        }
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (entity.equals(localPlayer) && GeneralConfig.DISABLE_SELF_MODEL.get().booleanValue()) {
            return null;
        }
        if ((!entity.equals(localPlayer) && GeneralConfig.DISABLE_OTHER_MODEL.get().booleanValue()) || entity.isSpectator()) {
            return null;
        }
        return PlayerCapability.get(entity).map(cap -> {
            if (cap.isModelActive()) {
                if (!CameraUtil.isFirstPerson(cap)
                        || FirstPersonCompat.isFirstPersonActive()
                        || RealCameraCompat.isActive()
                        || GeneralConfig.DISABLE_EXTERNAL_FP_ANIM.get().booleanValue()
                        || !PlayerAnimatorCompat.isPlayerAnimated(localPlayer)) {
                    return RendererManager.getPlayerRenderer().createRenderState(entity, partialTick);
                }
            }
            return null;
        }).orElse(null);
    }
}
