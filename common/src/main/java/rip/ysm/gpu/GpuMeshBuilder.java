package rip.ysm.gpu;

import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class GpuMeshBuilder {
    public static GpuMesh build(GeoModel model) {
        if (model.bakedBones == null || model.bakedBones.isEmpty()) return null;
        RenderSystem.assertOnRenderThread();
        ByteBuffer modelBuf = serializeModel(model);
        int[] meta = new int[9];
        long handle = GeoModel.nBuildGpuMesh(modelBuf, meta);
        if (handle == 0) {
            return null;
        }
        int vertexCount = meta[0];

        ByteBuffer vbuf = GeoModel.nGetGpuMeshVertexBuffer(handle);
        ByteBuffer ibuf = GeoModel.nGetGpuMeshIndexBuffer(handle);
        if (vbuf == null || ibuf == null) {
            GeoModel.nFreeGpuMesh(handle);
            return null;
        }
        vbuf.order(ByteOrder.nativeOrder());
        ibuf.order(ByteOrder.nativeOrder());

        ByteBuffer vertices = packVertices(vbuf, vertexCount);
        GpuBuffer vertexBuffer;
        try {
            vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "YSM static vertices", GpuBuffer.USAGE_VERTEX, vertices);
        } finally {
            MemoryUtil.memFree(vertices);
        }
        GpuBuffer indexBuffer = RenderSystem.getDevice().createBuffer(() -> "YSM static indices", GpuBuffer.USAGE_INDEX, ibuf);
        GeoModel.nReleaseGpuMeshScratch(handle);
        return new GpuMesh(handle, vertexBuffer, indexBuffer, meta);
    }

    static ByteBuffer packVertices(ByteBuffer source, int vertexCount) {
        ByteBuffer target = MemoryUtil.memAlloc(vertexCount * 40).order(ByteOrder.nativeOrder());
        for (int i = 0; i < vertexCount; i++) {
            int offset = i * 32;
            for (int component = 0; component < 5; component++) target.putFloat(source.getFloat(offset + component * 4));
            int normal = source.getInt(offset + 20);
            for (int component = 0; component < 3; component++) {
                int signed = (normal << (22 - component * 10)) >> 22;
                target.putFloat(Math.max(-1.0f, signed / 511.0f));
            }
            target.putInt(Short.toUnsignedInt(source.getShort(offset + 24)));
            target.putInt(Byte.toUnsignedInt(source.get(offset + 27)));
        }
        return target.flip();
    }

    private static ByteBuffer serializeModel(GeoModel model) {
        int totalBones = model.bakedBones.size();
        int totalCubes = 0;
        int totalQuads = 0;
        for (GeoModel.BakedBone bone : model.bakedBones) {
            totalCubes += bone.cubes.size();
            for (GeoModel.BakedCube cube : bone.cubes) {
                totalQuads += cube.quads.size();
            }
        }
        int sz = 4 + (totalBones * 25) + (totalCubes * 5) + (totalQuads * 92);
        ByteBuffer buf = ByteBuffer.allocateDirect(sz).order(ByteOrder.nativeOrder());
        buf.putInt(totalBones);
        for (GeoModel.BakedBone bone : model.bakedBones) {
            buf.putInt(bone.parentIdx);
            buf.putInt(bone.partMask);
            buf.put((byte) (bone.glow ? 1 : 0));
            buf.putFloat(bone.pivotX);
            buf.putFloat(bone.pivotY);
            buf.putFloat(bone.pivotZ);
            buf.putInt(bone.cubes.size());
            for (GeoModel.BakedCube cube : bone.cubes) {
                buf.put((byte) (cube.cullable ? 1 : 0));
                buf.putInt(cube.quads.size());
                for (GeoModel.BakedQuad quad : cube.quads) {
                    for (float position : quad.positions) {
                        buf.putFloat(position);
                    }
                    for (float uv : quad.uvs) {
                        buf.putFloat(uv);
                    }
                    buf.putFloat(quad.normal[0]);
                    buf.putFloat(quad.normal[1]);
                    buf.putFloat(quad.normal[2]);
                }
            }
        }
        buf.position(0);
        return buf;
    }
}
