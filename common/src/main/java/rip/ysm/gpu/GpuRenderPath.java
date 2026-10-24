package rip.ysm.gpu;

import com.elfmcys.yesstevemodel.geckolib3.geo.NativeModelRenderer;

import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.FabricOrderedSubmitNodeCollector;
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhases;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class GpuRenderPath {
    private static final ConcurrentHashMap<Long, GpuMesh> meshMap = new ConcurrentHashMap<>();
    private static final AtomicLong ref = new AtomicLong(1);
    private static final Matrix4f pivotAbsScratchMat = new Matrix4f();
    private static final Matrix4f bonePoseScratch = new Matrix4f();
    private static final Matrix3f boneNormalScratch = new Matrix3f();
    private static final Matrix4f boneNormal4Scratch = new Matrix4f();
    private static int[] pivotAbsPathScratch = new int[64];

    public static final class Frame {
        public final GpuMesh mesh;
        public final ByteBuffer bones;
        GpuBufferSlice gpuBones;

        Frame(GpuMesh mesh, ByteBuffer bones) {
            this.mesh = mesh;
            this.bones = bones;
        }
    }

    public static Frame extract(GeoModel model, float[] boneParams, float[] stateBuffer, int packedLight) {
        GpuMesh mesh = getOrBuildMesh(model);
        if (mesh == null) return null;
        updatePivotAbsStateBuffer(model, boneParams, stateBuffer);
        ByteBuffer bones = NativeModelRenderer.allocateFrameMemory(mesh.boneCount * 144L);
        GeoModel.nComputeBoneMatricesLocal(mesh.pointer, boneParams, packedLight, bones);
        updateNormals(bones, mesh.boneCount);
        return new Frame(mesh, bones);
    }

    private static void updateNormals(ByteBuffer bones, int boneCount) {
        for (int i = 0; i < boneCount; i++) {
            int offset = i * 144;
            if (bones.getInt(offset + 132) != 0) continue;
            bonePoseScratch.set(offset, bones).normal(boneNormalScratch);
            boneNormal4Scratch.set(boneNormalScratch).get(offset + 64, bones);
        }
    }

    public static void submit(Frame frame, PoseStack.Pose pose, OrderedSubmitNodeCollector collector, RenderType renderType, int renderPartMask, int packedOverlay, Vector4f color, boolean translucentTexture, int outlineColor) {
        FabricOrderedSubmitNodeCollector fabric = (FabricOrderedSubmitNodeCollector)collector;
        PoseStack.Pose snapshot = pose.copy();
        if (renderType.isOutline()) {
            fabric.submitCustom(SubmitRenderPhases.OUTLINE, new GpuSkinFeatureRenderer.Submit(frame, snapshot, renderType, renderPartMask, packedOverlay, outlineColor != 0 ? ARGB.vector4fFromARGB32(outlineColor) : new Vector4f(color), 3));
            return;
        }
        fabric.submitCustom(SubmitRenderPhases.SOLID, new GpuSkinFeatureRenderer.Submit(frame, snapshot, renderType, renderPartMask, packedOverlay, new Vector4f(color), 1));
        if (translucentTexture) fabric.submitCustom(SubmitRenderPhases.TRANSLUCENT_MODELS, new GpuSkinFeatureRenderer.Submit(frame, snapshot, renderType, renderPartMask, packedOverlay, new Vector4f(color), 2));
        if (outlineColor != 0) renderType.outline().ifPresent(outline -> {
            Vector4f tint = ARGB.vector4fFromARGB32(outlineColor);
            fabric.submitCustom(SubmitRenderPhases.OUTLINE, new GpuSkinFeatureRenderer.Submit(frame, snapshot, outline, renderPartMask, packedOverlay, tint, 3));
        });
    }

    public static void disposeMesh(GeoModel model) {
        if (model.gpuMeshHandle == 0) return;
        GpuMesh mesh = meshMap.remove(model.gpuMeshHandle);
        if (mesh != null) RenderSystem.queueFencedTask(mesh::dispose);
        model.gpuMeshHandle = 0;
    }

    public static GpuMesh getOrBuildMesh(GeoModel model) {
        if (model.gpuMeshHandle == 0) {
            GpuMesh mesh = GpuMeshBuilder.build(model);
            if (mesh == null) return null;
            long handle = ref.getAndIncrement();
            meshMap.put(handle, mesh);
            model.gpuMeshHandle = handle;
        }
        return meshMap.get(model.gpuMeshHandle);
    }

    private static void updatePivotAbsStateBuffer(GeoModel model, float[] boneParams, float[] stateBuffer) {
        if (stateBuffer == null) return;

        int boneCount = model.bakedBones.size();

        for (int i = 0; i < boneCount; i++) {
            int pOffset = i * 12;

            float unk3 = boneParams[pOffset + 11];
            if (unk3 != 1.0f) continue;

            int sOffset = i * 4;

            computeOnePivotAbs(i, model.bakedBones, boneParams, stateBuffer, sOffset);
        }
    }

    private static void computeOnePivotAbs(int targetIdx, List<GeoModel.BakedBone> bones, float[] boneParams, float[] stateBuffer, int stateOffset) {
        int depth = 0;
        int idx = targetIdx;

        while (idx != -1) {
            if (depth >= pivotAbsPathScratch.length) {
                int[] newPath = new int[pivotAbsPathScratch.length * 2];
                System.arraycopy(pivotAbsPathScratch, 0, newPath, 0, pivotAbsPathScratch.length);
                pivotAbsPathScratch = newPath;
            }

            pivotAbsPathScratch[depth++] = idx;
            idx = bones.get(idx).parentIdx;
        }

        Matrix4f localMat = pivotAbsScratchMat.identity();
        boolean isVisible = true;

        for (int p = depth - 1; p >= 0; p--) {
            int boneIdx = pivotAbsPathScratch[p];
            GeoModel.BakedBone bone = bones.get(boneIdx);

            int pOffset = boneIdx * 12;

            float animRx = boneParams[pOffset];
            float animRy = boneParams[pOffset + 1];
            float animRz = boneParams[pOffset + 2];
            float animTx = boneParams[pOffset + 3];
            float animTy = boneParams[pOffset + 4];
            float animTz = boneParams[pOffset + 5];
            float animSx = boneParams[pOffset + 6];
            float animSy = boneParams[pOffset + 7];
            float animSz = boneParams[pOffset + 8];

            if (animSx == 0.0f && animSy == 0.0f && animSz == 0.0f) {
                isVisible = false;
            }

            if (!isVisible) {
                return;
            }

            localMat.translate((bone.pivotX - animTx) * 0.0625f, (bone.pivotY + animTy) * 0.0625f, (bone.pivotZ + animTz) * 0.0625f);

            localMat.rotateZ(animRz);
            localMat.rotateY(animRy);
            localMat.rotateX(animRx);

            if (animSx != 1.0f || animSy != 1.0f || animSz != 1.0f) {
                localMat.scale(animSx, animSy, animSz);
            }

            if (boneIdx == targetIdx) {
                stateBuffer[stateOffset] = -localMat.m30() * 16.0f;
                stateBuffer[stateOffset + 1] = localMat.m31() * 16.0f;
                stateBuffer[stateOffset + 2] = localMat.m32() * 16.0f;
                return;
            }

            localMat.translate(-bone.pivotX / 16.0f, -bone.pivotY / 16.0f, -bone.pivotZ / 16.0f);
        }
    }
}
