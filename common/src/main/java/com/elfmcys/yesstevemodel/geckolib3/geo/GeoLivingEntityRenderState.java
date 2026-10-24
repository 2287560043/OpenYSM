package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.joml.Vector4f;
import java.util.List;

public class GeoLivingEntityRenderState extends AvatarRenderState {
    public final GeoReplacedEntityRenderer<?, ?> renderer;
    public final List<GeoLayerRenderer.Frame> layers = new ObjectArrayList<>();
    public final Vector4f color = new Vector4f(1);
    public NativeModelRenderer.Frame model;
    public PoseStack.Pose bodyPose;
    public RenderType renderType;
    public Identifier texture;
    public int packedOverlay;
    public boolean layersFirst;
    public boolean cancelled;

    public GeoLivingEntityRenderState(GeoReplacedEntityRenderer<?, ?> renderer) { this.renderer = renderer; }
}
