package rip.ysm.gpu;

import com.elfmcys.yesstevemodel.mixin.client.RenderPipelinesAccessor;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public final class BoneSkinShader {
    public static final RenderPipeline opaque = pipeline(1);
    public static final RenderPipeline translucent = pipeline(2);
    public static final RenderPipeline outline = pipeline(3);

    private static RenderPipeline pipeline(int pass) {
        RenderPipeline original = pass == 2 ? RenderPipelines.ENTITY_TRANSLUCENT : pass == 3 ? RenderPipelines.OUTLINE_NO_CULL : RenderPipelines.ENTITY_CUTOUT;
        return RenderPipelinesAccessor.ysm$register(RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("yes_steve_model", "pipeline/bone_skin_" + pass))
                .withVertexShader(Identifier.fromNamespaceAndPath("yes_steve_model", "core/bone_skin"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("yes_steve_model", "core/bone_skin"))
                .withShaderDefine("YSM_ALPHA_MODE", pass)
                .withBindGroupLayout(BindGroupLayout.builder().withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Fog", UniformType.UNIFORM_BUFFER).withUniform("Lighting", UniformType.UNIFORM_BUFFER).build())
                .withBindGroupLayout(BindGroupLayout.builder().withSampler("Sampler0").withSampler("Sampler1").withSampler("Sampler2").withUniform("Bones", UniformType.TEXEL_BUFFER, GpuFormat.RGBA32_UINT).withUniform("ModelData", UniformType.UNIFORM_BUFFER).build())
                .withVertexBinding(0, VertexFormat.builder(0).addAttribute("Position", GpuFormat.RGB32_FLOAT).addAttribute("UV0", GpuFormat.RG32_FLOAT).addAttribute("Normal", GpuFormat.RGB32_FLOAT).addAttribute("BoneIndex", GpuFormat.R32_UINT).addAttribute("Cullable", GpuFormat.R32_UINT).build())
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withColorTargetState(original.getColorTargetState())
                .withDepthStencilState(Optional.ofNullable(original.getDepthStencilState()))
                .withCull(false)
                .build());
    }
}
