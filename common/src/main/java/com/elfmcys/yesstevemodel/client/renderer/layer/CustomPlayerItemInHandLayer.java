package com.elfmcys.yesstevemodel.client.renderer.layer;

import rip.ysm.compat.slashblade.SlashBladeRenderer;
import rip.ysm.compat.slashblade.SlashBladeCompat;
import rip.ysm.compat.gun.swarfare.SWarfareCompat;
import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoLayerRenderer;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import rip.ysm.compat.gun.tacz.TacCompat;
import com.elfmcys.yesstevemodel.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import java.util.List;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.math.Axis;

public class CustomPlayerItemInHandLayer extends GeoLayerRenderer<CustomPlayerEntity> {

    private final ItemModelResolver itemRenderer;

    public CustomPlayerItemInHandLayer(ItemModelResolver itemInHandRenderer) {
        this.itemRenderer = itemInHandRenderer;
    }

    @Override
    public void extract(PoseStack poseStack, List<Frame> frames, int packedLightIn, CustomPlayerEntity entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        LivingEntity entity = entityLivingBaseIn.getEntity();
        AnimatedGeoModel animatedGeoModel = entityLivingBaseIn.getCurrentModel();
        if (animatedGeoModel == null) {
            return;
        }
        ItemStack offhandItem = entity.getOffhandItem();
        ItemStack mainHandItem = entity.getMainHandItem();
        if (!offhandItem.isEmpty() || !mainHandItem.isEmpty()) {
            poseStack.pushPose();
            if (!animatedGeoModel.rightHandBones().isEmpty()) {
                if (SlashBladeCompat.isSlashBladeItem(mainHandItem)) {
                    SlashBladeRenderer.renderOnEntity(entity, animatedGeoModel, poseStack, frames, packedLightIn, mainHandItem, partialTick);
                } else {
                    TacCompat.handleGunSound(entity, mainHandItem);
                    extractItem(animatedGeoModel, entity, mainHandItem, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT, poseStack, frames);
                    TacCompat.handleItemSound(mainHandItem);
                }
            }
            if (!animatedGeoModel.leftHandBones().isEmpty()) {
                if (SlashBladeCompat.isSlashBladeItem(offhandItem)) {
                    SlashBladeRenderer.renderRightWaist(animatedGeoModel, poseStack, frames, packedLightIn, offhandItem);
                } else {
                    if (!SWarfareCompat.isGunItem(offhandItem)) {
                        extractItem(animatedGeoModel, entity, offhandItem, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT, poseStack, frames);
                    }
                }
            }
            poseStack.popPose();
            TacCompat.applyItemTransform(offhandItem, animatedGeoModel, entity, poseStack, packedLightIn, partialTick);
            SWarfareCompat.applyGunTransform(offhandItem, animatedGeoModel, entity, poseStack, packedLightIn, partialTick);
        }
    }

    public void extractItem(AnimatedGeoModel model, LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext itemDisplayContext, HumanoidArm humanoidArm, PoseStack poseStack, List<Frame> frames) {
        if (!itemStack.isEmpty()) {
            boolean isLeftHand = humanoidArm == HumanoidArm.LEFT;
            ItemStackRenderState item = new ItemStackRenderState();
            this.itemRenderer.updateForLiving(item, itemStack, itemDisplayContext, livingEntity);
            poseStack.pushPose();
            if (!applyItemBoneTransform(humanoidArm, poseStack, model)) {
                poseStack.translate(0.0d, -0.0625d, -0.1d);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
                if (SWarfareCompat.isGunItem(itemStack)) {
                    poseStack.translate(0.1d, 0.0d, 0.0d);
                    poseStack.scale(1.25f, 1.25f, 1.25f);
                }
                frames.add(new ItemFrame(poseStack.last().copy(), item));
            }
            poseStack.popPose();
            (isLeftHand ? model.rightHandChain() : model.leftHandChains()).forEach(list -> {
                poseStack.pushPose();
                if (!RenderUtils.prepMatrixForLocator(poseStack, list)) {
                    poseStack.translate(0.0d, -0.0625d, -0.1d);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
                    if (SWarfareCompat.isGunItem(itemStack)) {
                        poseStack.scale(1.25f, 1.25f, 1.25f);
                    }
                    frames.add(new ItemFrame(poseStack.last().copy(), item));
                }
                poseStack.popPose();
            });
        }
    }

    public boolean applyItemBoneTransform(HumanoidArm humanoidArm, PoseStack poseStack, AnimatedGeoModel model) {
        if (humanoidArm == HumanoidArm.LEFT) {
            return RenderUtils.prepMatrixForLocator(poseStack, model.leftHandBones());
        }
        return RenderUtils.prepMatrixForLocator(poseStack, model.rightHandBones());
    }
}