package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.mixin.client.RenderTypeAccessor;
import net.minecraft.util.Util;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

public class CustomEntityTranslucentRenderType {

    private static final Function<Identifier, RenderType> CACHE = Util.memoize(resourceLocation -> RenderTypeAccessor.ysm$create("entity_translucent_ysm", RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT).withTexture("Sampler0", resourceLocation).useLightmap().useOverlay().affectsCrumbling().setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).createRenderSetup()));

    public static RenderType get(Identifier resourceLocation) {
        return CACHE.apply(resourceLocation);
    }
}
