package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.joml.Matrix4f;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

public class ReplacePlayerHandRenderEvent {

    private ReplacePlayerHandRenderEvent() {
    }

    public static boolean onExtractArm(Player player, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector collector, Matrix4f projection, int packedLight, float partialTick) {
        if (!YesSteveModel.isAvailable() || GeneralConfig.DISABLE_SELF_MODEL.get() || GeneralConfig.DISABLE_SELF_HANDS.get()) {
            return false;
        }
        if (!(player instanceof LocalPlayer localPlayer)) {
            return false;
        }
        boolean[] cancelled = {false};
        PlayerCapability.get(localPlayer).ifPresent(cap -> {
            if (!cap.isModelActive()) {
                return;
            }
            ModelAssembly context = cap.getModelAssembly();
            if (context == null || !hasArmBone(arm, context.getAnimationBundle().getArmModel())) {
                return;
            }
            RendererManager.getHandRenderer().extractHandItem(localPlayer, context, cap, arm, poseStack, collector, projection, packedLight, partialTick);
            cancelled[0] = true;
        });
        return cancelled[0];
    }

    public static boolean hasCustomArms(LocalPlayer player) {
        if (!YesSteveModel.isAvailable() || GeneralConfig.DISABLE_SELF_MODEL.get() || GeneralConfig.DISABLE_SELF_HANDS.get()) return false;
        PlayerCapability cap = PlayerCapability.get(player).orElse(null);
        ModelAssembly model = cap != null && cap.isModelActive() ? cap.getModelAssembly() : null;
        if (model == null) return false;
        GeoModel arms = model.getAnimationBundle().getArmModel();
        return arms.hasCustomLeftHand || arms.hasCustomRightHand;
    }

    private static boolean hasArmBone(HumanoidArm humanoidArm, GeoModel meshData) {
        if (humanoidArm == HumanoidArm.LEFT) {
            return meshData.hasCustomLeftHand;
        }
        return meshData.hasCustomRightHand;
    }
}
