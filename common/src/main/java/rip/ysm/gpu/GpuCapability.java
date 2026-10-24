package rip.ysm.gpu;

import com.elfmcys.yesstevemodel.NativeLibLoader;

public final class GpuCapability {
    public static boolean isAvailable() {
        return System.getProperty("OYSM_DISABLE_GPU") == null && NativeLibLoader.isLoaded();
    }

    public static String getReason() {
        if (System.getProperty("OYSM_DISABLE_GPU") != null) return "gpu renderer has been disabled";
        if (!NativeLibLoader.isLoaded()) return "native ysm-core not loaded";
        return "Blaze3D vertex skinning available";
    }
}
