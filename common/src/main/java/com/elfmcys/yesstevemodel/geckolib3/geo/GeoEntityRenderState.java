package com.elfmcys.yesstevemodel.geckolib3.geo;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public class GeoEntityRenderState extends EntityRenderState {
    public final EntityRenderer<?, GeoEntityRenderState> renderer;
    public NativeModelRenderer.Frame model;
    public RenderType renderType;
    public final Quaternionf rotation = new Quaternionf();
    public final Vector4f color = new Vector4f(1.0f);
    public int packedOverlay;
    public net.minecraft.world.phys.Vec3 fishingLineOrigin;
    public float[] fishingLineColor;
    public float fishingLineWidth;

    public GeoEntityRenderState(EntityRenderer<?, GeoEntityRenderState> renderer) { this.renderer = renderer; }
}
