package com.elfmcys.yesstevemodel.client.renderer.layer;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import rip.ysm.compat.simplehats.SimpleHatsHelper;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLayerRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import java.util.List;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CustomPlayerArmorLayer extends GeoLayerRenderer<CustomPlayerEntity> {

    private final ItemModelResolver itemRenderer;

    public CustomPlayerArmorLayer(EntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemModelResolver();
    }

    @Override
    public void extract(PoseStack poseStack, List<Frame> frames, int packedLightIn, CustomPlayerEntity entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        Player player = entityLivingBaseIn.getEntity();
        AnimatedGeoModel model = entityLivingBaseIn.getCurrentModel();
        if (model != null && !model.headBones().isEmpty()) {
            ItemStack itemBySlot = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!itemBySlot.isEmpty() && !isArmorItem(itemBySlot)) {
                extractArmorPiece(poseStack, frames, model, player, itemBySlot);
            }
            ItemStack stack = SimpleHatsHelper.getHatItem(player);
            if (stack != null && !stack.isEmpty()) {
                extractArmorPiece(poseStack, frames, model, player, stack);
            }
        }
    }

    private boolean isArmorItem(ItemStack stack) {
        return HumanoidArmorLayer.shouldRender(stack, EquipmentSlot.HEAD);
    }

    private void extractArmorPiece(PoseStack poseStack, List<Frame> frames, AnimatedGeoModel model, Player player, ItemStack stack) {
        poseStack.pushPose();
        RenderUtils.prepMatrixForLocator(poseStack, model.headBones());
        poseStack.scale(0.625f, 0.625f, 0.625f);
        poseStack.translate(0.0f, 0.25f, 0.0f);
        ItemStackRenderState item = new ItemStackRenderState();
        this.itemRenderer.updateForLiving(item, stack, ItemDisplayContext.HEAD, player);
        frames.add(new ItemFrame(poseStack.last().copy(), item));
        poseStack.popPose();
    }
}