package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.ProjectileCapability;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoEntityRenderState;
import net.minecraft.world.entity.projectile.Projectile;

public class CustomProjectileRenderer {
    public static GeoEntityRenderState extractProjectile(Projectile projectile, float partialTick) {
        return ProjectileCapability.get(projectile).map(cap -> {
            if (cap.isModelInitialized() && cap.isModelReady()) {
                return RendererManager.getProjectileRenderer().createRenderState(projectile, partialTick);
            }
            return null;
        }).orElse(null);
    }
}
