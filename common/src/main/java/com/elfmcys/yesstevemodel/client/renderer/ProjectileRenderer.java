package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.ProjectileCapability;
import com.elfmcys.yesstevemodel.client.entity.GeckoProjectileEntity;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoEntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.Projectile;
import org.jetbrains.annotations.NotNull;

public class ProjectileRenderer extends AbstractProjectileRenderer<Projectile, GeckoProjectileEntity> {
    public ProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void extractRenderState(Projectile projectile, GeoEntityRenderState state, float partialTick) {
        super.extractRenderState(projectile, state, partialTick);
        ProjectileCapability.get(projectile).ifPresent(cap -> extractProjectile(cap, state, partialTick));
    }

    @Override
    public void submit(GeoEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        if (state.fishingLineOrigin != null) CustomFishingHookRenderer.submitFishingLine(state, poseStack, collector);
    }

    @NotNull
    public Identifier getTextureLocation(Projectile projectile) {
        return ProjectileCapability.get(projectile).map((cap) -> cap.getTextureLocation()).orElse(MissingTextureAtlasSprite.getLocation());
    }
}
