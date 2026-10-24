package com.elfmcys.yesstevemodel.client.entity;

import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.client.renderer.CustomVehicleRenderer;
import com.elfmcys.yesstevemodel.util.accessors.VehiclePassengerRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

import java.lang.ref.WeakReference;

public class EntityRenderCache {

    private static final ReferenceArrayList<WeakReference<GeoEntity<?>>> weakRefs = new ReferenceArrayList<>(64);

    private static final ReferenceArrayList<GeoEntity<?>> strongRefs = new ReferenceArrayList<>(16);

    private static final ReferenceArrayList<Passenger> passengers = new ReferenceArrayList<>();

    private static boolean extracting;

    private record Passenger(Entity entity, EntityRenderState state, float partialTick) {}

    public static void extractPassenger(Entity entity, EntityRenderState state, float partialTick) {
        if (entity.getVehicle() == null) {
            return;
        }
        if (extracting) {
            passengers.add(new Passenger(entity, state, partialTick));
        } else {
            ((VehiclePassengerRenderState) state).ysm$setPassengerPose(CustomVehicleRenderer.extractPassengerPose(entity, partialTick));
        }
    }

    public static void register(GeoEntity<?> entity) {
        weakRefs.add(new WeakReference<>(entity));
    }

    public static void tick(float partialTick) {
        extracting = true;
        if (Minecraft.getInstance().player == null) {
            return;
        }
        ObjectListIterator<WeakReference<GeoEntity<?>>> it = weakRefs.iterator();
        while (it.hasNext()) {
            GeoEntity geoEntity = (GeoEntity) ((WeakReference<?>) it.next()).get();
            if (geoEntity == null) {
                it.remove();
            } else if (!geoEntity.isDebugMode()) {
                it.remove();
            } else {
                geoEntity.tickModel();
                if (geoEntity.supportsAsync() && geoEntity.isModelInitialized() && geoEntity.isModelReady()) {
                    Entity entity = geoEntity.getEntity();
                    if (entity instanceof AbstractClientPlayer) {
                        if (entity instanceof LocalPlayer) {
                            if (!GeneralConfig.DISABLE_SELF_MODEL.get()) {
                                geoEntity.submitAsyncUpdate(partialTick);
                                strongRefs.add(geoEntity);
                            }
                        } else if (!GeneralConfig.DISABLE_OTHER_MODEL.get()) {
                            geoEntity.submitAsyncUpdate(partialTick);
                            strongRefs.add(geoEntity);
                        }
                    } else if (entity instanceof Projectile) {
                        if (!GeneralConfig.DISABLE_PROJECTILE_MODEL.get()) {
                            geoEntity.submitAsyncUpdate(partialTick);
                            strongRefs.add(geoEntity);
                        }
                    } else if (!GeneralConfig.DISABLE_VEHICLE_MODEL.get()) {
                        geoEntity.submitAsyncUpdate(partialTick);
                        strongRefs.add(geoEntity);
                    }
                }
            }
        }
    }

    public static void clear() {
        ObjectListIterator<GeoEntity<?>> it = strongRefs.iterator();
        while (it.hasNext()) {
            try {
                it.next().awaitAsyncResult();
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
        strongRefs.clear();
        for (Passenger passenger : passengers) {
            ((VehiclePassengerRenderState) passenger.state()).ysm$setPassengerPose(CustomVehicleRenderer.extractPassengerPose(passenger.entity(), passenger.partialTick()));
        }
        passengers.clear();
        extracting = false;
    }
}