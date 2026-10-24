package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.YesSteveModel;
import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import rip.ysm.api.PlatformAPI;
import rip.ysm.compat.sbackpack.SBackpackCompat;

public class RendererManager {

    private static EntityRendererProvider.Context context;

    private static CustomPlayerRenderer playerRenderer;

    private static ProjectileRenderer projectileRenderer;

    private static HandItemRenderer handRenderer;

    private static VehicleRenderer vehicleRenderer;

    private static ModelPreviewRenderer previewRenderer;

    private RendererManager() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        ResourceManagerReloadListener listener = resourceManager -> resetRenderers();
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, listener, Identifier.fromNamespaceAndPath(YesSteveModel.MOD_ID, "renderer_manager"));
    }

    private static void resetRenderers() {
        playerRenderer = null;
        projectileRenderer = null;
        handRenderer = null;
        vehicleRenderer = null;
        previewRenderer = null;
    }

    public static void setContext(EntityRendererProvider.Context rendererContext) { context = rendererContext; }

    private static void initRenderers() {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        playerRenderer = new CustomPlayerRenderer(context);
        projectileRenderer = new ProjectileRenderer(context);
        handRenderer = new HandItemRenderer();
        vehicleRenderer = new VehicleRenderer(context);
        previewRenderer = new ModelPreviewRenderer(context);
        SBackpackCompat.setupRenderLayers();
    }

    public static CustomPlayerRenderer getPlayerRenderer() {
        if (playerRenderer == null) {
            initRenderers();
        }
        return playerRenderer;
    }

    public static ProjectileRenderer getProjectileRenderer() {
        if (projectileRenderer == null) {
            initRenderers();
        }
        return projectileRenderer;
    }

    public static HandItemRenderer getHandRenderer() {
        if (handRenderer == null) {
            initRenderers();
        }
        return handRenderer;
    }

    public static VehicleRenderer getVehicleRenderer() {
        if (vehicleRenderer == null) {
            initRenderers();
        }
        return vehicleRenderer;
    }
    public static ModelPreviewRenderer getPreviewRenderer() {
        if (previewRenderer == null) initRenderers();
        return previewRenderer;
    }

}
