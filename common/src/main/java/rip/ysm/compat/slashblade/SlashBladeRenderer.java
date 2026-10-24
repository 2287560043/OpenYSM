package rip.ysm.compat.slashblade;

import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.injectables.annotations.ExpectPlatform;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLayerRenderer;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class SlashBladeRenderer {

    private SlashBladeRenderer() {
    }

    @ExpectPlatform
    public static void renderOnEntity(LivingEntity entity, AnimatedGeoModel model, PoseStack poseStack, List<GeoLayerRenderer.Frame> frames, int packedLight, ItemStack stack, float partialTick) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void renderRightWaist(AnimatedGeoModel model, PoseStack poseStack, List<GeoLayerRenderer.Frame> frames, int packedLight, ItemStack stack) {
        throw new AssertionError();
    }
}
