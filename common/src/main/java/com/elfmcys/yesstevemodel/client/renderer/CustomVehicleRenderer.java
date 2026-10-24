package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.capability.VehicleCapability;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.core.processor.IBone;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.entity.player.Player;
import rip.ysm.compat.touhoulittlemaid.TouhouLittleMaidCompat;
import java.util.List;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.phys.Vec3;
import rip.ysm.api.entity.EntityDataBridge;

public class CustomVehicleRenderer {
    public static GeoEntityRenderState extractVehicle(Entity entity, float partialTick) {
        return VehicleCapability.get(entity).map(cap -> {
            if (cap.isModelInitialized() && cap.isModelReady()) {
                return RendererManager.getVehicleRenderer().createRenderState(entity, partialTick);
            }
            return null;
        }).orElse(null);
    }

    public static PoseStack.Pose extractPassengerPose(Entity entity, float partialTick) {
        Entity vehicle = entity.getVehicle();
        if (vehicle == null) {
            return null;
        }
        return VehicleCapability.get(vehicle).map(cap -> {
            int index;
            AnimatedGeoModel model;
            List<IBone> list;
            if (!cap.isModelInitialized() || !cap.isModelReady() || (index = vehicle.getPassengers().indexOf(entity)) < 0 || (model = cap.getCurrentModel()) == null || model.passengerGroupChains().isEmpty() || index >= model.passengerGroupChains().size() || (list = model.passengerGroupChains().get(index)) == null) {
                return null;
            }
            PoseStack poseStack = new PoseStack();
            poseStack.translate(Mth.lerp(partialTick, vehicle.xOld, vehicle.getX()) - Mth.lerp(partialTick, entity.xOld, entity.getX()),
                    Mth.lerp(partialTick, vehicle.yOld, vehicle.getY()) - Mth.lerp(partialTick, entity.yOld, entity.getY()),
                    Mth.lerp(partialTick, vehicle.zOld, vehicle.getZ()) - Mth.lerp(partialTick, entity.zOld, entity.getZ()));
            float bodyRotation = getBodyRotation(vehicle, Mth.lerp(partialTick, vehicle.yRotO, vehicle.getYRot()), partialTick);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - bodyRotation));
            RenderUtils.prepMatrixForLocator(poseStack, list);
            poseStack.mulPose(Axis.YN.rotationDegrees(180.0f - bodyRotation));
            if (((entity instanceof Player) && PlayerCapability.get(entity).isPresent()) || TouhouLittleMaidCompat.isMaidRideable(entity)) {
                poseStack.translate(0.0d, -0.5d, 0.0d);
            }
            return poseStack.last();
        }).orElse(null);
    }

    public static float getBodyRotation(Entity entity, float entityYaw, float partialTick) {
        float bodyRotation = entityYaw;
        if (entity instanceof LivingEntity) {
            bodyRotation = getLivingBodyRotation((LivingEntity) entity, partialTick);
        } else if (entity instanceof AbstractMinecart) {
            bodyRotation = getMinecartBodyRotation((AbstractMinecart) entity, partialTick, bodyRotation);
        }
        return bodyRotation;
    }

    private static float getLivingBodyRotation(LivingEntity entity, float partialTick) {
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);

        if (entity.isPassenger() && entity.getVehicle() != null && EntityDataBridge.shouldRiderSit(entity.getVehicle())) {
            Entity vehicle = entity.getVehicle();
            if (vehicle instanceof LivingEntity livingVehicle) {
                float vehicleBodyYaw = Mth.rotLerp(partialTick, livingVehicle.yBodyRotO, livingVehicle.yBodyRot);
                float yawDiff = Mth.clamp(Mth.wrapDegrees(headYaw - vehicleBodyYaw), -85.0f, 85.0f);
                bodyYaw = headYaw - yawDiff;

                if (yawDiff * yawDiff > 2500.0f) {
                    bodyYaw += yawDiff * 0.2f;
                }
            }
        }
        return bodyYaw;
    }

    private static float getMinecartBodyRotation(AbstractMinecart minecart, float partialTick, float defaultYaw) {
        if (minecart.getBehavior() instanceof NewMinecartBehavior behavior) {
            return behavior.cartHasPosRotLerp() ? behavior.getCartLerpYRot(partialTick) : minecart.getYRot();
        }
        OldMinecartBehavior behavior = (OldMinecartBehavior) minecart.getBehavior();
        double interpX = Mth.lerp(partialTick, minecart.xOld, minecart.getX());
        double interpY = Mth.lerp(partialTick, minecart.yOld, minecart.getY());
        double interpZ = Mth.lerp(partialTick, minecart.zOld, minecart.getZ());
        Vec3 interpPos = behavior.getPos(interpX, interpY, interpZ);

        float calculatedYaw = defaultYaw;

        if (interpPos != null) {
            Vec3 frontOffsetPos = behavior.getPosOffs(interpX, interpY, interpZ, 0.30000001192092896d);
            Vec3 backOffsetPos = behavior.getPosOffs(interpX, interpY, interpZ, -0.30000001192092896d);

            if (frontOffsetPos == null) {
                frontOffsetPos = interpPos;
            }
            if (backOffsetPos == null) {
                backOffsetPos = interpPos;
            }

            Vec3 directionVec = backOffsetPos.add(-frontOffsetPos.x, -frontOffsetPos.y, -frontOffsetPos.z);
            if (directionVec.length() != 0.0d) {
                calculatedYaw = (float) ((Math.atan2(directionVec.z, directionVec.x) * 180.0d) / Math.PI);
            }
        }
        return calculatedYaw;
    }
}
