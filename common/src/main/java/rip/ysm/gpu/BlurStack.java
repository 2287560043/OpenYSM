package rip.ysm.gpu;

import com.elfmcys.yesstevemodel.mixin.client.GuiGraphicsExtractorAccessor;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import java.util.ArrayList;
import java.util.List;

public final class BlurStack {
    private static final List<Region> regions = new ArrayList<>();

    private BlurStack() {
    }

    public static void pushBlur(float x, float y, float w, float h, float cornerRadius, float blurRadius) {
        pushBlur(x, y, w, h, cornerRadius, blurRadius, 0xFFFFFFFF);
    }

    public static void pushBlur(float x, float y, float w, float h, float cornerRadius, float blurRadius, int tintRgba) {
        Region r = new Region();
        r.isPie = false;
        r.x = x;
        r.y = y;
        r.w = w;
        r.h = h;
        r.cornerRadius = cornerRadius;
        r.blurRadius = blurRadius;
        r.tintRgba = tintRgba;
        regions.add(r);
    }

    public static void pushBlurPie(float centerX, float centerY, float innerRadius, float outerRadius, float startAngle, float endAngle, float blurRadius) {
        pushBlurPie(centerX, centerY, innerRadius, outerRadius, startAngle, endAngle, blurRadius, 0xFFFFFFFF);
    }

    public static void pushBlurPie(float centerX, float centerY, float innerRadius, float outerRadius, float startAngle, float endAngle, float blurRadius, int tintRgba) {
        float pad = 1.0f;
        Region r = new Region();
        r.isPie = true;
        r.x = centerX - outerRadius - pad;
        r.y = centerY - outerRadius - pad;
        r.w = (outerRadius + pad) * 2.0f;
        r.h = (outerRadius + pad) * 2.0f;
        r.pieInner = innerRadius;
        r.pieStart = startAngle;
        r.pieEnd = endAngle;
        r.blurRadius = blurRadius;
        r.tintRgba = tintRgba;
        regions.add(r);
    }

    public static void popBlur() {
        if (!regions.isEmpty()) regions.remove(regions.size() - 1);
    }

    public static void clear() {
        regions.clear();
    }

    public static boolean isEmpty() {
        return regions.isEmpty();
    }

    public static void flush(GuiGraphicsExtractor graphics) {
        if (regions.isEmpty()) return;
        var texture = BlurShader.nextCapture().textureSetup();
        graphics.nextStratum();
        var gui = ((GuiGraphicsExtractorAccessor) graphics).ysm$getGuiRenderState();
        for (Region region : regions) gui.addGuiElement(new State(new Matrix3x2f(graphics.pose()), graphics.textRenderer().defaultParameters().scissor(), texture, region));
        graphics.nextStratum();
        regions.clear();
    }

    record State(Matrix3x2fc pose, ScreenRectangle scissorArea, TextureSetup textureSetup, Region region) implements GuiElementRenderState {
        @Override public RenderPipeline pipeline() { return BlurShader.pipeline(); }
        @Override public ScreenRectangle bounds() {
            var bounds = new ScreenRectangle((int) Math.floor(region.x), (int) Math.floor(region.y), (int) Math.ceil(region.x + region.w) - (int) Math.floor(region.x), (int) Math.ceil(region.y + region.h) - (int) Math.floor(region.y)).transformMaxBounds(pose);
            return scissorArea == null ? bounds : bounds.intersection(scissorArea);
        }
        @Override public void buildVertices(VertexConsumer consumer) {
            Vector2f position = new Vector2f();
            vertex(consumer, position, 0, region.h);
            vertex(consumer, position, region.w, region.h);
            vertex(consumer, position, region.w, 0);
            vertex(consumer, position, 0, 0);
        }
        private void vertex(VertexConsumer consumer, Vector2f position, float x, float y) {
            pose.transformPosition(region.x + x, region.y + y, position);
            int a = Float.floatToRawIntBits(region.isPie ? region.pieStart : region.w);
            int b = Float.floatToRawIntBits(region.isPie ? region.pieEnd : region.h);
            consumer.addVertex(position.x, position.y, region.isPie ? -region.pieInner - 1 : region.cornerRadius).setColor(region.tintRgba)
                    .setUv(x - region.w * 0.5f, y - region.h * 0.5f).setUv1(a & 65535, a >>> 16).setUv2(b & 65535, b >>> 16).setLineWidth(Math.max(1, region.blurRadius));
        }
    }

    private static final class Region {
        boolean isPie;
        float x;
        float y;
        float w;
        float h;
        float cornerRadius;
        float pieInner;
        float pieStart;
        float pieEnd;
        float blurRadius;
        int tintRgba;
    }
}
