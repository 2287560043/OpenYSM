package com.elfmcys.yesstevemodel.client.texture;

import rip.ysm.compat.oculus.ShadersTextureType;
import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

public class OuterFileTexture extends ReloadableTexture implements ITextureMap {
    private final byte[] data;

    private Map<ShadersTextureType, OuterFileTexture> suffixTextures = Reference2ReferenceMaps.emptyMap();

    public OuterFileTexture(byte[] data) {
        super(Identifier.fromNamespaceAndPath("yes_steve_model", "external"));
        this.data = data;
    }

    @Override
    public TextureContents loadContents(@NotNull ResourceManager resourceManager) throws IOException {
        return new TextureContents(NativeImage.read(new ByteArrayInputStream(data)), new TextureMetadataSection(false, true, MipmapStrategy.AUTO, 0.0f));
    }

    public void setSuffixTextures(Map<ShadersTextureType, OuterFileTexture> map) {
        this.suffixTextures = Reference2ReferenceMaps.unmodifiable(new Reference2ReferenceOpenHashMap<>(map));
    }

    public Map<ShadersTextureType, ? extends ReloadableTexture> getSuffixTextures() {
        return this.suffixTextures;
    }
}
