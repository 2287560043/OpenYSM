package com.elfmcys.yesstevemodel.mixin.client;

import com.elfmcys.yesstevemodel.util.accessors.VehiclePassengerRenderState;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.renderer.CustomFishingHookRenderer;
import com.elfmcys.yesstevemodel.client.renderer.CustomVehicleRenderer;
import com.elfmcys.yesstevemodel.client.renderer.CustomProjectileRenderer;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.client.entity.EntityRenderCache;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoEntityRenderState;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLivingEntityRenderState;
import com.elfmcys.yesstevemodel.client.event.ReplacePlayerRenderEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @ModifyExpressionValue(method = "onResourceManagerReload", at = @At(value = "NEW", target = "net/minecraft/client/renderer/entity/EntityRendererProvider$Context"))
    private EntityRendererProvider.Context ysm$captureContext(EntityRendererProvider.Context context) {
        RendererManager.setContext(context);
        return context;
    }

    @WrapOperation(method = "extractEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"))
    private EntityRenderState ysm$extract(EntityRenderer<?, ?> renderer, Entity entity, float partialTick, Operation<EntityRenderState> original) {
        if (!YesSteveModel.isAvailable()) {
            return original.call(renderer, entity, partialTick);
        }
        if (entity instanceof Projectile projectile && !GeneralConfig.DISABLE_PROJECTILE_MODEL.get()) {
            EntityRenderState state = projectile instanceof FishingHook hook ? CustomFishingHookRenderer.extractHook(hook, partialTick) : CustomProjectileRenderer.extractProjectile(projectile, partialTick);
            return state != null ? state : original.call(renderer, entity, partialTick);
        }
        EntityRenderState state = !GeneralConfig.DISABLE_VEHICLE_MODEL.get() ? CustomVehicleRenderer.extractVehicle(entity, partialTick) : null;
        if (state == null && entity instanceof Player player) {
            state = ReplacePlayerRenderEvent.extractPlayer(player, partialTick);
        }
        if (state == null) {
            state = original.call(renderer, entity, partialTick);
        }
        if (!GeneralConfig.DISABLE_VEHICLE_MODEL.get()) {
            EntityRenderCache.extractPassenger(entity, state, partialTick);
        }
        return state;
    }

    @Inject(method = "getRenderer(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)Lnet/minecraft/client/renderer/entity/EntityRenderer;", at = @At("HEAD"), cancellable = true)
    private void ysm$selectRenderer(EntityRenderState state, CallbackInfoReturnable<EntityRenderer<?, ?>> cir) {
        if (state instanceof GeoEntityRenderState geoState) {
            cir.setReturnValue(geoState.renderer);
        } else if (state instanceof GeoLivingEntityRenderState livingState) {
            cir.setReturnValue(livingState.renderer);
        } else if (state instanceof ModelPreviewRenderer.RenderState previewState) {
            cir.setReturnValue(previewState.renderer);
        }
    }

    @WrapOperation(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"))
    private void ysm$submit(EntityRenderer<?, ?> renderer, EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, Operation<Void> original) {
        PoseStack.Pose passengerPose = ((VehiclePassengerRenderState) state).ysm$getPassengerPose();
        if (passengerPose != null) {
            poseStack.mulPose(passengerPose.pose());
        }
        original.call(renderer, state, poseStack, collector, camera);
    }
}
