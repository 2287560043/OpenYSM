package com.elfmcys.yesstevemodel.capability.fabric;

import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import org.ladysnake.cca.api.v3.component.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ModelInfoComponent implements Component {

    private final ModelInfoCapability capability = new ModelInfoCapability();

    public ModelInfoCapability capability() {
        return capability;
    }

    @Override
    public void readData(ValueInput input) {
        input.read("ModelInfo", CompoundTag.CODEC).ifPresent(capability::deserializeNBT);
    }

    @Override
    public void writeData(ValueOutput output) {
        output.store("ModelInfo", CompoundTag.CODEC, capability.serializeNBT());
    }
}
