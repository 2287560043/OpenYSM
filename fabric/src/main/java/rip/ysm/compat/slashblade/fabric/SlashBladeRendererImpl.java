package rip.ysm.compat.slashblade.fabric;

import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLayerRenderer;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class SlashBladeRendererImpl {

    private SlashBladeRendererImpl() {
    }

    public static void renderOnEntity(LivingEntity entity, AnimatedGeoModel model, PoseStack poseStack, List<GeoLayerRenderer.Frame> frames, int packedLight, ItemStack stack, float partialTick) {
    }

    public static void renderRightWaist(AnimatedGeoModel model, PoseStack poseStack, List<GeoLayerRenderer.Frame> frames, int packedLight, ItemStack stack) {
    }
}
