package rip.ysm.gpu;

import com.elfmcys.yesstevemodel.geckolib3.geo.render.built.GeoModel;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;


public final class GpuMesh {
    public final long pointer;
    public final GpuBuffer vertexBuffer;
    public final GpuBuffer indexBuffer;
    public final int vertexCount;
    public final int indexCount;
    public final int boneCount;
    public final int partMask1Start, partMask1Count;
    public final int partMask2Start, partMask2Count;
    public final int partMask3Start, partMask3Count;

    private boolean disposed;

    GpuMesh(long pointer, GpuBuffer vertexBuffer, GpuBuffer indexBuffer, int[] meta) {
        this.pointer = pointer;
        this.vertexBuffer = vertexBuffer;
        this.indexBuffer = indexBuffer;
        this.vertexCount = meta[0];
        this.indexCount = meta[1];
        this.boneCount = meta[2];
        this.partMask1Start = meta[3];
        this.partMask1Count = meta[4];
        this.partMask2Start = meta[5];
        this.partMask2Count = meta[6];
        this.partMask3Start = meta[7];
        this.partMask3Count = meta[8];
    }

    public void draw(RenderPass pass, int renderPartMask) {
        if (renderPartMask == 0 || renderPartMask == 3) {
            if (indexCount > 0) pass.drawIndexed(indexCount, 1, 0, 0, 0);
            return;
        }
        int start = renderPartMask == 1 ? partMask1Start : partMask2Start;
        int count = renderPartMask == 1 ? partMask1Count : partMask2Count;
        if (count > 0) pass.drawIndexed(count, 1, start, 0, 0);
        if (partMask3Count > 0) pass.drawIndexed(partMask3Count, 1, partMask3Start, 0, 0);
    }

    public void dispose() {
        if (this.disposed) return;
        this.disposed = true;
        this.vertexBuffer.close();
        this.indexBuffer.close();
        GeoModel.nFreeGpuMesh(this.pointer);
    }
}
