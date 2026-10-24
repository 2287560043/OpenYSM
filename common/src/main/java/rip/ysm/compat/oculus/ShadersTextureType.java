package rip.ysm.compat.oculus;

import net.minecraft.resources.Identifier;

public enum ShadersTextureType {
    NORMAL("_n"),
    SPECULAR("_s");

    public static final ShadersTextureType[] VALUES = values();

    private final String suffix;

    ShadersTextureType(String str) {
        this.suffix = str;
    }

    public Identifier appendSuffix(Identifier resourceLocation) {
        return Identifier.fromNamespaceAndPath(resourceLocation.getNamespace(), resourceLocation.getPath() + this.suffix);
    }
}
