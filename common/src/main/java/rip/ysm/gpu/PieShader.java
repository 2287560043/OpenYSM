package rip.ysm.gpu;

import com.elfmcys.yesstevemodel.mixin.client.RenderPipelinesAccessor;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public final class PieShader {
    private static RenderPipeline pipeline;

    private PieShader() {}

    public static RenderPipeline pipeline() {
        if (pipeline == null) {
            RenderPipeline.Builder builder = RenderPipeline.builder().withLocation(Identifier.fromNamespaceAndPath("yes_steve_model", "pipeline/pie"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("yes_steve_model", "core/pie")).withFragmentShader(Identifier.fromNamespaceAndPath("yes_steve_model", "core/pie"))
                    .withVertexBinding(0, vertexFormat()).withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.ONE, BlendFactor.ZERO))).withDepthStencilState(Optional.ofNullable(RenderPipelines.GUI.getDepthStencilState())).withCull(false);
            for (var layout : RenderPipelines.GUI.getBindGroupLayouts()) builder.withBindGroupLayout(layout);
            pipeline = RenderPipelinesAccessor.ysm$register(builder.build());
        }
        return pipeline;
    }

    static VertexFormat vertexFormat() {
        return VertexFormat.builder(0).addAttribute("Position", GpuFormat.RGB32_FLOAT).addAttribute("Color", GpuFormat.RGBA8_UNORM)
                .addAttribute("UV0", GpuFormat.RG32_FLOAT).addAttribute("UV1", GpuFormat.RG16_UINT)
                .addAttribute("UV2", GpuFormat.RG16_UINT).addAttribute("LineWidth", GpuFormat.R32_FLOAT).build();
    }
}
