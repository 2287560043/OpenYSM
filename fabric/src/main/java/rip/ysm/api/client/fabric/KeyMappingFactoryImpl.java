package rip.ysm.api.client.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import java.util.HashMap;
import java.util.Map;

public final class KeyMappingFactoryImpl {

    private static final Map<String, KeyMapping.Category> categories = new HashMap<>();

    private KeyMappingFactoryImpl() {
    }

    public static KeyMapping createInGameAlt(String name, InputConstants.Type type, int keyCode, String category) {
        return new KeyMapping(name, type, keyCode, categories.computeIfAbsent(category, key -> KeyMapping.Category.register(Identifier.withDefaultNamespace(key))));
    }

    public static KeyMapping createInGameNone(String name, InputConstants.Type type, int keyCode, String category) {
        return new KeyMapping(name, type, keyCode, categories.computeIfAbsent(category, key -> KeyMapping.Category.register(Identifier.withDefaultNamespace(key))));
    }

    public static boolean isActiveAndMatches(KeyMapping keyMapping, int keyCode, int scanCode) {
        return keyMapping.matches(new KeyEvent(keyCode, scanCode, 0));
    }
}
