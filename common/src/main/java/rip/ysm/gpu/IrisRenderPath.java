package rip.ysm.gpu;

import com.elfmcys.yesstevemodel.geckolib3.geo.NativeModelRenderer;

import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;

public final class IrisRenderPath {
    public record Frame(ByteBuffer vertices, int vertexCount) {}

    public static Frame extract(GeoModel model, ByteBuffer bones, int renderPartMask, boolean fullBody) {
        int vertexCount = 0;
        for (int i = 0; i < model.bakedBones.size(); i++) {
            GeoModel.BakedBone bone = model.bakedBones.get(i);
            if (bones.getInt(i * 144 + 132) != 0 || (!fullBody && bone.partMask != renderPartMask && bone.partMask != 3)) continue;
            for (GeoModel.BakedCube cube : bone.cubes) vertexCount += cube.quads.size() * 4;
        }
        ByteBuffer vertices = NativeModelRenderer.allocateFrameMemory(vertexCount / 4 * 100L);
        Matrix4f transform = new Matrix4f();
        Matrix4f normal4 = new Matrix4f();
        Matrix3f normalMatrix = new Matrix3f();
        Vector3f position = new Vector3f();
        Vector3f normal = new Vector3f();
        for (int i = 0; i < model.bakedBones.size(); i++) {
            GeoModel.BakedBone bone = model.bakedBones.get(i);
            int offset = i * 144;
            if (bones.getInt(offset + 132) != 0 || (!fullBody && bone.partMask != renderPartMask && bone.partMask != 3)) continue;
            transform.set(offset, bones);
            normal4.set(offset + 64, bones).get3x3(normalMatrix);
            int light = bones.getInt(offset + 128);
            for (GeoModel.BakedCube cube : bone.cubes) {
                for (GeoModel.BakedQuad quad : cube.quads) {
                    normal.set(quad.normal[0], quad.normal[1], quad.normal[2]).mul(normalMatrix);
                    for (int v = 0; v < 4; v++) {
                        position.set(quad.positions[v * 3], quad.positions[v * 3 + 1], quad.positions[v * 3 + 2]);
                        transform.transformPosition(position);
                        vertices.putFloat(position.x).putFloat(position.y).putFloat(position.z);
                        vertices.putFloat(quad.uvs[v * 2]).putFloat(quad.uvs[v * 2 + 1]);
                    }
                    vertices.putFloat(normal.x).putFloat(normal.y).putFloat(normal.z);
                    vertices.putInt(light).putInt(cube.cullable ? 1 : 0);
                }
            }
        }
        return new Frame(vertices.flip(), vertexCount);
    }

    public static void emit(Frame frame, PoseStack.Pose pose, Matrix4f projection, VertexConsumer consumer, int packedOverlay, Vector4f color) {
        Matrix4f projectedPose = new Matrix4f(projection).mul(pose.pose());
        Vector4f p1 = new Vector4f();
        Vector4f p2 = new Vector4f();
        Vector4f p3 = new Vector4f();
        Vector3f normal = new Vector3f();
        Vector3f position = new Vector3f();
        ByteBuffer vertices = frame.vertices();
        for (int quad = 0; quad < frame.vertexCount() / 4; quad++) {
            int offset = quad * 100;
            if (vertices.getInt(offset + 96) != 0) {
                p1.set(vertices.getFloat(offset), vertices.getFloat(offset + 4), vertices.getFloat(offset + 8), 1).mul(projectedPose);
                p2.set(vertices.getFloat(offset + 20), vertices.getFloat(offset + 24), vertices.getFloat(offset + 28), 1).mul(projectedPose);
                p3.set(vertices.getFloat(offset + 40), vertices.getFloat(offset + 44), vertices.getFloat(offset + 48), 1).mul(projectedPose);
                float det = p1.x * (p2.y * p3.w - p3.y * p2.w) - p2.x * (p1.y * p3.w - p3.y * p1.w) + p3.x * (p1.y * p2.w - p2.y * p1.w);
                if (det <= 0) continue;
            }
            normal.set(vertices.getFloat(offset + 80), vertices.getFloat(offset + 84), vertices.getFloat(offset + 88)).mul(pose.normal()).normalize();
            for (int v = 0; v < 4; v++) {
                int index = offset + v * 20;
                pose.pose().transformPosition(vertices.getFloat(index), vertices.getFloat(index + 4), vertices.getFloat(index + 8), position);
                consumer.addVertex(position.x, position.y, position.z)
                        .setColor(color.x, color.y, color.z, color.w)
                        .setUv(vertices.getFloat(index + 12), vertices.getFloat(index + 16))
                        .setOverlay(packedOverlay).setLight(vertices.getInt(offset + 92))
                        .setNormal(normal.x, normal.y, normal.z);
            }
        }
    }
}
