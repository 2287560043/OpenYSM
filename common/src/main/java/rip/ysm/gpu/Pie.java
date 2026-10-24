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

public final class Pie {
    public static final float tau = (float) (Math.PI * 2.0);

    public static void draw(GuiGraphicsExtractor graphics, float centerX, float centerY, float innerRadius, float outerRadius, float startAngle, float endAngle, int rgba) {
        draw(graphics, centerX, centerY, innerRadius, outerRadius, startAngle, endAngle, rgba, 1.0f);
    }

    public static void draw(GuiGraphicsExtractor graphics, float centerX, float centerY, float innerRadius, float outerRadius, float startAngle, float endAngle, int rgba, float feather) {
        var state = new State(PieShader.pipeline(), new Matrix3x2f(graphics.pose()), graphics.textRenderer().defaultParameters().scissor(), centerX, centerY, Math.max(0.0f, innerRadius), outerRadius, startAngle, endAngle, rgba, feather);
        ((GuiGraphicsExtractorAccessor) graphics).ysm$getGuiRenderState().addGuiElement(state);
    }

    record State(RenderPipeline pipeline, Matrix3x2fc pose, ScreenRectangle scissorArea, float centerX, float centerY, float innerRadius, float outerRadius, float startAngle, float endAngle, int rgba, float feather) implements GuiElementRenderState {
        @Override
        public TextureSetup textureSetup() { return TextureSetup.noTexture(); }

        @Override
        public ScreenRectangle bounds() {
            float extent = outerRadius + (feather + 1.0f);
            int left = (int) Math.floor(centerX - extent), top = (int) Math.floor(centerY - extent);
            var bounds = new ScreenRectangle(left, top, (int) Math.ceil(centerX + extent) - left, (int) Math.ceil(centerY + extent) - top).transformMaxBounds(pose);
            return scissorArea == null ? bounds : bounds.intersection(scissorArea);
        }

        @Override
        public void buildVertices(VertexConsumer consumer) {
            float extent = outerRadius + (feather + 1.0f);
            int start = Float.floatToRawIntBits(startAngle), end = Float.floatToRawIntBits(endAngle);
            Vector2f position = new Vector2f();
            vertex(consumer, position, -extent, extent, start, end);
            vertex(consumer, position, extent, extent, start, end);
            vertex(consumer, position, extent, -extent, start, end);
            vertex(consumer, position, -extent, -extent, start, end);
        }

        private void vertex(VertexConsumer consumer, Vector2f position, float x, float y, int start, int end) {
            pose.transformPosition(centerX + x, centerY + y, position);
            // GUI geometry uses z=0 in the shader; this field carries the original inner radius.
            consumer.addVertex(position.x, position.y, innerRadius).setColor(rgba).setUv(x, y)
                    .setUv1(start & 65535, start >>> 16).setUv2(end & 65535, end >>> 16).setLineWidth(feather);
        }
    }
}
