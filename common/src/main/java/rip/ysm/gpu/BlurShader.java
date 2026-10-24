package rip.ysm.gpu;

import com.elfmcys.yesstevemodel.mixin.client.RenderPipelinesAccessor;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

public final class BlurShader {
    private static RenderPipeline pipeline;
    private static GpuSampler sampler;
    private static final List<Capture> captures = new ArrayList<>();
    private static final Map<GpuTextureView, Capture> byView = new IdentityHashMap<>();
    private static int nextCapture;

    private BlurShader() {}

    public static RenderPipeline pipeline() {
        if (pipeline == null) {
            var builder = RenderPipeline.builder().withLocation(Identifier.fromNamespaceAndPath("yes_steve_model", "pipeline/blur"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("yes_steve_model", "core/blur")).withFragmentShader(Identifier.fromNamespaceAndPath("yes_steve_model", "core/blur"))
                    .withVertexBinding(0, PieShader.vertexFormat()).withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.ONE, BlendFactor.ZERO)))
                    .withDepthStencilState(Optional.ofNullable(RenderPipelines.GUI.getDepthStencilState())).withCull(false);
            for (var layout : RenderPipelines.GUI_TEXTURED.getBindGroupLayouts()) builder.withBindGroupLayout(layout);
            pipeline = RenderPipelinesAccessor.ysm$register(builder.build());
        }
        return pipeline;
    }

    static Capture nextCapture() {
        var window = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState;
        if (sampler == null) sampler = RenderSystem.getDevice().createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, 1, OptionalDouble.empty());
        if (nextCapture == captures.size()) captures.add(new Capture(window.width, window.height));
        Capture capture = captures.get(nextCapture);
        if (capture.texture.getWidth(0) != window.width || capture.texture.getHeight(0) != window.height) {
            byView.remove(capture.view);
            Capture previous = capture;
            RenderSystem.queueFencedTask(previous::close);
            capture = new Capture(window.width, window.height);
            captures.set(nextCapture, capture);
        }
        nextCapture++;
        capture.captured = false;
        byView.put(capture.view, capture);
        return capture;
    }

    public static Capture captureFor(GpuTextureView view) { return byView.get(view); }
    public static void endFrame() { nextCapture = 0; }
    public static void close() {
        for (Capture capture : captures) capture.close();
        captures.clear();
        byView.clear();
        if (sampler != null) { sampler.close(); sampler = null; }
        nextCapture = 0;
    }

    public static final class Capture {
        private final GpuTexture texture;
        private final GpuTextureView view;
        private final GpuTextureView[] levels;
        public boolean captured;

        Capture(int width, int height) {
            var device = RenderSystem.getDevice();
            int mipLevels = 32 - Integer.numberOfLeadingZeros(Math.max(width, height));
            texture = device.createTexture("YSM blur capture", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, GpuFormat.RGBA8_UNORM, width, height, 1, mipLevels);
            view = device.createTextureView(texture);
            levels = new GpuTextureView[mipLevels];
            for (int i = 0; i < mipLevels; i++) levels[i] = device.createTextureView(texture, i, 1);
        }
        TextureSetup textureSetup() { return TextureSetup.singleTexture(view, sampler); }
        public void capture(RenderTarget main) {
            var encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.copyTextureToTexture(main.getColorTexture(), texture, 0, 0, 0, 0, 0, texture.getWidth(0), texture.getHeight(0));
            for (int i = 1; i < levels.length; i++) {
                try (var pass = encoder.createRenderPass(() -> "YSM blur mipmap", levels[i], Optional.empty())) {
                    pass.setPipeline(RenderPipelines.TRACY_BLIT);
                    RenderSystem.bindDefaultUniforms(pass);
                    pass.bindTexture("InSampler", levels[i - 1], sampler);
                    pass.draw(3, 1, 0, 0);
                }
            }
            captured = true;
        }
        void close() {
            for (GpuTextureView level : levels) level.close();
            view.close();
            texture.close();
        }
    }
}
