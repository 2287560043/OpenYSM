package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.VehicleCapability;
import com.elfmcys.yesstevemodel.client.entity.GeckoVehicleEntity;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoEntityRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class VehicleRenderer extends GeoEntityRenderer<Entity, GeckoVehicleEntity> {
    public VehicleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void extractRenderState(Entity entity, GeoEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        VehicleCapability.get(entity).ifPresent(cap -> {
            extractEntity(cap, state, CustomVehicleRenderer.getBodyRotation(entity, entity.getYRot(partialTick), partialTick), partialTick);
        });
    }

    @NotNull
    public Identifier getTextureLocation(Entity entity) {
        return VehicleCapability.get(entity).map((cap) -> cap.getTextureLocation()).orElse(MissingTextureAtlasSprite.getLocation());
    }
}
