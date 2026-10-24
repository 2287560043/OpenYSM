package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import rip.ysm.compat.touhoulittlemaid.TouhouLittleMaidCompat;
import rip.ysm.compat.gun.swarfare.SWarfareCompat;
import com.elfmcys.yesstevemodel.client.entity.PlayerPreviewEntity;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerArmorLayer;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerElytraLayer;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerItemInHandLayer;
import com.elfmcys.yesstevemodel.client.renderer.layer.CustomPlayerParrotLayer;
import com.elfmcys.yesstevemodel.event.api.SpecialPlayerRenderEvent;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoReplacedEntityRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLivingEntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

public class CustomPlayerRenderer extends GeoReplacedEntityRenderer<Player, CustomPlayerEntity> {

    public CustomPlayerRenderer(EntityRendererProvider.Context context) {
        super(context);
        addLayerRenderer(new CustomPlayerItemInHandLayer(context.getItemModelResolver()));
        addLayerRenderer(new CustomPlayerElytraLayer(context));
        addLayerRenderer(new CustomPlayerParrotLayer(context));
        addLayerRenderer(new CustomPlayerArmorLayer(context));
    }

    @Override
    public void extractRenderState(Player player, GeoLivingEntityRenderState state, float partialTick) {
        super.extractRenderState(player, state, partialTick);
        PlayerCapability capability;
        if (SWarfareCompat.isPlayerAiming(player) || (capability = PlayerCapability.get(player).orElse(null)) == null) {
            state.cancelled = true;
            return;
        }
        capability.tickModel();
        SpecialPlayerRenderEvent renderEvent = new SpecialPlayerRenderEvent(player, capability, capability.getModelId());
        if (SpecialPlayerRenderEvent.post(renderEvent).isFalse()) {
            state.cancelled = true;
            return;
        }
        extractEntityWithTexture(capability, renderEvent.getTextureLocation(), state, partialTick);
        state.scoreText = null;
        if (PlayerPreviewEntity.isPreviewPlayer(player)) {
            state.nameTag = null;
        } else if (state.nameTag != null && state.distanceToCameraSq < 100.0d) {
            Scoreboard scoreboard = player.level().getScoreboard();
            Objective displayObjective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);
            if (displayObjective != null) {
                state.scoreText = Component.literal(Integer.toString(scoreboard.getOrCreatePlayerScore(player, displayObjective).get())).append(" ").append(displayObjective.getDisplayName());
            }
        }
    }

    @Override
    public boolean shouldShowName(Player entity, double dDistanceToSqr) {
        Minecraft minecraft;
        LocalPlayer localPlayer;
        float nameRenderDistance = entity.isDiscrete() ? 32.0f : 64.0f;
        if (dDistanceToSqr >= nameRenderDistance * nameRenderDistance || (localPlayer = (minecraft = Minecraft.getInstance()).player) == null) {
            return false;
        }
        boolean isVisible = !entity.isInvisibleTo(localPlayer);
        if (entity != localPlayer) {
            Team team = entity.getTeam();
            Team team2 = localPlayer.getTeam();
            if (team != null) {
                switch (team.getNameTagVisibility()) {
                    case ALWAYS:
                        return isVisible;
                    case NEVER:
                        return false;
                    case HIDE_FOR_OTHER_TEAMS:
                        return team2 == null ? isVisible : team.isAlliedTo(team2) && (team.canSeeFriendlyInvisibles() || isVisible);
                    case HIDE_FOR_OWN_TEAM:
                        return team2 == null ? isVisible : !team.isAlliedTo(team2) && isVisible;
                    default:
                        throw new IncompatibleClassChangeError();
                }
            }
        }
        return !Minecraft.getInstance().gui.hud.isHidden() && entity != minecraft.getCameraEntity() && isVisible && !entity.isVehicle();
    }

    @Override
    public void setupRotations(Player player, GeoLivingEntityRenderState state, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(player, state, poseStack, ageInTicks, rotationYaw, partialTicks);
        Entity vehicle = player.getVehicle();
        if (TouhouLittleMaidCompat.isSimplePlanesEntity(vehicle) || TouhouLittleMaidCompat.isImmersiveAircraftEntity(vehicle)) {
            poseStack.translate(0.0d, 0.5d, 0.0d);
        }
    }
}