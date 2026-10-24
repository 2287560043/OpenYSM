package com.elfmcys.yesstevemodel.capability.fabric;

import com.elfmcys.yesstevemodel.capability.VehicleModelCapability;
import org.ladysnake.cca.api.v3.component.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class VehicleModelComponent implements Component {

    private final VehicleModelCapability capability = new VehicleModelCapability();

    public VehicleModelCapability capability() {
        return capability;
    }

    @Override
    public void readData(ValueInput input) {
        input.read("VehicleModel", CompoundTag.CODEC).ifPresent(capability::deserializeNBT);
    }

    @Override
    public void writeData(ValueOutput output) {
        output.store("VehicleModel", CompoundTag.CODEC, capability.serializeNBT());
    }
}
