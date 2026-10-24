package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.NativeLibLoader;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.mojang.blaze3d.util.TransientBlockAllocator;
import org.lwjgl.system.MemoryUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.ARGB;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import rip.ysm.compat.oculus.OculusCompat;
import rip.ysm.gpu.GpuCapability;
import rip.ysm.gpu.GpuRenderPath;
import rip.ysm.gpu.IrisRenderPath;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

public class NativeModelRenderer {
    private static final TransientBlockAllocator<Long> frameMemory = new TransientBlockAllocator<>(524288, 16, TransientBlockAllocator.Allocator.create(MemoryUtil::nmemAlloc, MemoryUtil::nmemFree));

    public static ByteBuffer allocateFrameMemory(long size) {
        var allocation = frameMemory.allocate(size, 16, size, 1);
        return MemoryUtil.memByteBuffer(allocation.block() + allocation.offset(), Math.toIntExact(allocation.size())).order(ByteOrder.nativeOrder());
    }

    public static void endFrame() { frameMemory.rotate().run(); }
    public static void closeFrameMemory() { frameMemory.close(); }

    public record Frame(GpuRenderPath.Frame gpu, IrisRenderPath.Frame vertices, boolean translucentTexture) {}

    public static final class BoneMatrices {
        final Matrix4f[] transforms;
        final boolean[] visible;
        final boolean[] calculated;
        final Matrix4f identity = new Matrix4f();
        final Matrix3f normal = new Matrix3f();
        final Matrix4f normal4 = new Matrix4f();

        BoneMatrices(int boneCount) {
            transforms = new Matrix4f[boneCount];
            for (int i = 0; i < boneCount; i++) transforms[i] = new Matrix4f();
            visible = new boolean[boneCount];
            calculated = new boolean[boneCount];
        }
    }

    public static Frame extract(GeoModel model, float[] boneParams, float[] stateBuffer, int textureIndex, int renderPartMask, int packedLight) {
        if (model.bakedBones == null || model.bakedBones.isEmpty()) return null;
        OculusCompat.updatePBRState();
        boolean nativeCalculation = NativeLibLoader.isLoaded() && !GeneralConfig.USE_COMPATIBILITY_RENDERER.get();
        GpuRenderPath.Frame gpu = nativeCalculation ? GpuRenderPath.extract(model, boneParams, stateBuffer, packedLight) : null;
        if (gpu != null && GeneralConfig.USE_GPU_RENDERER.get() && GpuCapability.isAvailable() && (!OculusCompat.isShaderPackInUse() || ModelPreviewRenderer.isPreview() || ModelPreviewRenderer.isExtraPlayer())) {
            return new Frame(gpu, null, model.isTranslucentTexture(textureIndex));
        }
        ByteBuffer bones = gpu != null ? gpu.bones : extractJavaBones(model, boneParams, stateBuffer, packedLight);
        IrisRenderPath.Frame vertices = IrisRenderPath.extract(model, bones, renderPartMask, renderPartMask == 0 || (gpu != null && renderPartMask == 3));
        return new Frame(null, vertices, model.isTranslucentTexture(textureIndex));
    }

    public static void submit(Frame frame, PoseStack poseStack, OrderedSubmitNodeCollector collector, Matrix4f projection, RenderType renderType, int renderPartMask, int packedOverlay, Vector4f color, int outlineColor) {
        if (frame.gpu() != null) {
            GpuRenderPath.submit(frame.gpu(), poseStack.last(), collector, renderType, renderPartMask, packedOverlay, color, frame.translucentTexture(), outlineColor);
        } else {
            Matrix4f snapshot = new Matrix4f(projection);
            Vector4f tint = renderType.isOutline() && outlineColor != 0 ? ARGB.vector4fFromARGB32(outlineColor) : new Vector4f(color);
            collector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> IrisRenderPath.emit(frame.vertices(), pose, snapshot, consumer, packedOverlay, tint));
            if (outlineColor != 0) renderType.outline().ifPresent(outline -> {
                Vector4f outlineTint = ARGB.vector4fFromARGB32(outlineColor);
                collector.submitCustomGeometry(poseStack, outline, (pose, consumer) -> IrisRenderPath.emit(frame.vertices(), pose, snapshot, consumer, packedOverlay, outlineTint));
            });
        }
    }

    private static ByteBuffer extractJavaBones(GeoModel model, float[] boneParams, float[] stateBuffer, int packedLight) {
        int boneCount = model.bakedBones.size();
        if (model.boneMatrices == null) model.boneMatrices = new BoneMatrices(boneCount);
        BoneMatrices matrices = model.boneMatrices;
        Arrays.fill(matrices.calculated, false);
        ByteBuffer bones = allocateFrameMemory(boneCount * 144L);
        for (int i = 0; i < boneCount; i++) {
            Matrix4f transform = calculateBoneMatrix(i, model.bakedBones, boneParams, matrices, matrices.identity, stateBuffer);
            transform.get(i * 144, bones);
            transform.normal(matrices.normal);
            matrices.normal4.set(matrices.normal).get(i * 144 + 64, bones);
            bones.putInt(i * 144 + 128, model.bakedBones.get(i).glow ? LightCoordsUtil.pack(15, 15) : packedLight);
            bones.putInt(i * 144 + 132, matrices.visible[i] ? 0 : 1).putInt(i * 144 + 136, 0).putInt(i * 144 + 140, 0);
        }
        return bones;
    }

    private static Matrix4f calculateBoneMatrix(int idx, java.util.List<GeoModel.BakedBone> bones, float[] boneParams, BoneMatrices matrices, Matrix4f rootPose, float[] stateBuffer) {
        if (matrices.calculated[idx]) return matrices.transforms[idx];

        GeoModel.BakedBone bone = bones.get(idx);
        Matrix4f parentMatrix = rootPose;
        boolean isVisible = true;

        if (bone.parentIdx != -1) {
            parentMatrix = calculateBoneMatrix(bone.parentIdx, bones, boneParams, matrices, rootPose, stateBuffer);
            // 如果父骨骼不可見，子骨骼必然跟著不可見
            if (!matrices.visible[bone.parentIdx]) {
                isVisible = false;
            }
        }

        Matrix4f localMat = matrices.transforms[idx].set(parentMatrix);

        int pOffset = idx * 12;
        float animRx = boneParams[pOffset];
        float animRy = boneParams[pOffset + 1];
        float animRz = boneParams[pOffset + 2];
        float animTx = boneParams[pOffset + 3];
        float animTy = boneParams[pOffset + 4];
        float animTz = boneParams[pOffset + 5];
        float animSx = boneParams[pOffset + 6];
        float animSy = boneParams[pOffset + 7];
        float animSz = boneParams[pOffset + 8];

        float unk1 = boneParams[pOffset + 9];
        float unk2 = boneParams[pOffset + 10];
        float unk3 = boneParams[pOffset + 11];

        if (unk1 != 0.0F && unk2 != 0.0F && unk3 != 0.0F) {
            //"".hashCode();
        }

        if (animSx == 0.0f && animSy == 0.0f && animSz == 0.0f) {
            isVisible = false;
        }/* else if (unk1 == 1 || unk2 == 1) isVisible = false;*/

        localMat.translate(
                (bone.pivotX - animTx) * 0.0625f,
                (bone.pivotY + animTy) * 0.0625f,
                (bone.pivotZ + animTz) * 0.0625f
        );
        localMat.rotateZ(animRz);
        localMat.rotateY(animRy);
        localMat.rotateX(animRx);

//        if (bone.name.equals("gun")) {
//            //"".hashCode();
//        }

        if (animSx != 1.0f || animSy != 1.0f || animSz != 1.0f) {
            localMat.scale(animSx, animSy, animSz);
        }

        if (unk3 == 1.0F && stateBuffer != null && isVisible) {
            int offset = idx * 4;
            // bone pivot abs
            if (offset + 2 < stateBuffer.length) {
                stateBuffer[offset + 0] =-localMat.m30() * 16;
                stateBuffer[offset + 1] = localMat.m31() * 16;
                stateBuffer[offset + 2] = localMat.m32() * 16;
            }
        }

        localMat.translate(-bone.pivotX / 16f, -bone.pivotY / 16f, -bone.pivotZ / 16f);

        matrices.calculated[idx] = true;
        matrices.visible[idx] = isVisible; // 保存當前骨骼的可見性
        return localMat;
    }

    // Required by the bundled native library's JNI registration; the migrated path only calls pure-data methods.
    @SuppressWarnings("unused")
    public static void submitVertices(Object v, int vertexCount, ByteBuffer fBuf, ByteBuffer iBuf) {
        FloatBuffer f = fBuf.order(ByteOrder.nativeOrder()).asFloatBuffer();
        IntBuffer in = iBuf.order(ByteOrder.nativeOrder()).asIntBuffer();
        VertexConsumer consumer = (VertexConsumer)v;
        for (int n = 0; n < vertexCount; n++) {
            int fi = n * 12;
            int ii = n * 2;
            consumer.addVertex(f.get(fi), f.get(fi + 1), f.get(fi + 2))
                    .setColor(f.get(fi + 3), f.get(fi + 4), f.get(fi + 5), f.get(fi + 6))
                    .setUv(f.get(fi + 7), f.get(fi + 8)).setOverlay(in.get(ii)).setLight(in.get(ii + 1))
                    .setNormal(f.get(fi + 9), f.get(fi + 10), f.get(fi + 11));
        }
    }
}
