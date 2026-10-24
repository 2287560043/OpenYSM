package rip.ysm.gpu;

import com.elfmcys.yesstevemodel.geckolib3.geo.NativeModelRenderer;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.systems.TransientMemory;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.FeatureRendererRegistry;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderer;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public final class GpuSkinFeatureRenderer implements FeatureRenderer<GpuSkinFeatureRenderer.Submit> {
    public static final FeatureRendererType<Submit> TYPE = FeatureRendererType.create("ysm_bone_skin");
    private final List<List<Prepared>> groups = new ArrayList<>();

    public record Submit(GpuRenderPath.Frame frame, PoseStack.Pose pose, RenderType renderType, int renderPartMask, int packedOverlay, Vector4f color, int alphaPass) implements TranslucentSubmit {
        @Override
        public FeatureRendererType<Submit> featureType() { return TYPE; }

        @Override
        public float distanceToCameraSq() { return TranslucentSubmit.computeDistanceToCameraSq(pose.pose()); }
    }

    private record Prepared(Submit submit, PreparedRenderType renderType, GpuBufferSlice modelData, GpuBufferSlice transforms) {}

    public static void register() {
        FeatureRendererRegistry.register(TYPE, GpuSkinFeatureRenderer::new);
    }

    @Override
    public void prepareGroup(FeatureFrameContext context, List<Submit> submits, boolean outline) {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        TransientMemory memory = encoder.transientMemory();
        int alignment = RenderSystem.getDevice().getDeviceInfo().limits().minUniformOffsetAlignment();
        List<Prepared> prepared = new ArrayList<>(submits.size());
        for (Submit submit : submits) {
            GpuRenderPath.Frame frame = submit.frame();
            if (frame.gpuBones == null) frame.gpuBones = memory.uploadGpu(frame.bones, 16, GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER);
            ByteBuffer data = NativeModelRenderer.allocateFrameMemory(144);
            submit.pose().pose().get(0, data);
            new Matrix4f().set(submit.pose().normal()).get(64, data);
            data.putInt(128, submit.packedOverlay() & 65535).putInt(132, submit.packedOverlay() >>> 16);
            // Texel buffers bind the entire arena; each frame addresses its allocation by texel offset.
            data.putInt(136, Math.toIntExact(frame.gpuBones.offset() / 16)).putInt(140, 0);
            GpuBufferSlice modelData = memory.uploadGpu(data, alignment, GpuBuffer.USAGE_UNIFORM);
            PreparedRenderType renderType = submit.renderType().prepare();
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy(), submit.color());
            prepared.add(new Prepared(submit, renderType, modelData, transforms));
        }
        groups.add(prepared);
    }

    @Override
    public void executeGroup(FeatureFrameContext context, int groupIndex, List<Submit> submits, boolean outline) {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        for (Prepared prepared : groups.get(groupIndex)) {
            Submit submit = prepared.submit();
            PreparedRenderType renderType = prepared.renderType();
            RenderTarget target = renderType.outputTarget().getRenderTarget();
            GpuTextureView color = RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride : target.getColorTextureView();
            GpuTextureView depth = target.useDepth ? RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : target.getDepthTextureView() : null;
            RenderPipeline pipeline = submit.alphaPass() == 2 ? BoneSkinShader.translucent : submit.alphaPass() == 3 ? BoneSkinShader.outline : BoneSkinShader.opaque;
            try (RenderPass pass = encoder.createRenderPass(() -> "YSM bone skin", color, Optional.empty(), depth, OptionalDouble.empty())) {
                pass.setPipeline(pipeline);
                ScissorState scissor = renderType.scissorState();
                if (scissor.enabled()) pass.enableScissor(scissor.x(), scissor.y(), scissor.width(), scissor.height());
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", prepared.transforms());
                pass.setUniform("ModelData", prepared.modelData());
                pass.setUniform("Bones", new GpuBufferSlice(submit.frame().gpuBones.buffer(), 0, submit.frame().gpuBones.buffer().size()));
                for (PreparedRenderType.Texture texture : renderType.textures()) pass.bindTexture(texture.name(), texture.textureView(), texture.sampler());
                pass.setVertexBuffer(0, submit.frame().mesh.vertexBuffer.slice());
                pass.setIndexBuffer(submit.frame().mesh.indexBuffer, IndexType.INT);
                submit.frame().mesh.draw(pass, submit.renderPartMask());
            }
        }
    }

    @Override
    public void finishExecute(FeatureFrameContext context) {
        groups.clear();
    }
}
