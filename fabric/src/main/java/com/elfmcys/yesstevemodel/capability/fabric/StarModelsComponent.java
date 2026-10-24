package com.elfmcys.yesstevemodel.capability.fabric;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.HashSet;
import java.util.List;
import com.elfmcys.yesstevemodel.capability.StarModelsCapability;
import org.ladysnake.cca.api.v3.component.Component;

public final class StarModelsComponent implements Component {

    private final StarModelsCapability capability = new StarModelsCapability();

    public StarModelsCapability capability() {
        return capability;
    }

    @Override
    public void readData(ValueInput input) {
        capability.setStarModels(new HashSet<>(input.read("StarModels", Codec.STRING.listOf()).orElse(List.of())));
    }

    @Override
    public void writeData(ValueOutput output) {
        output.store("StarModels", Codec.STRING.listOf(), List.copyOf(capability.getStarModels()));
    }
}
