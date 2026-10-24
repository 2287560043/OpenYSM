package com.elfmcys.yesstevemodel.capability.fabric;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.HashSet;
import java.util.List;
import com.elfmcys.yesstevemodel.capability.AuthModelsCapability;
import org.ladysnake.cca.api.v3.component.Component;

public final class AuthModelsComponent implements Component {

    private final AuthModelsCapability capability = new AuthModelsCapability();

    public AuthModelsCapability capability() {
        return capability;
    }

    @Override
    public void readData(ValueInput input) {
        capability.setAuthModels(new HashSet<>(input.read("AuthModels", Codec.STRING.listOf()).orElse(List.of())));
    }

    @Override
    public void writeData(ValueOutput output) {
        output.store("AuthModels", Codec.STRING.listOf(), List.copyOf(capability.getAuthModels()));
    }
}
