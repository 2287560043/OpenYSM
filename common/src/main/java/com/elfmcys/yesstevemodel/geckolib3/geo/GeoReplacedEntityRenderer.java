package com.elfmcys.yesstevemodel.geckolib3.geo;

import com.elfmcys.yesstevemodel.capability.VehicleCapability;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.util.Color;
import com.elfmcys.yesstevemodel.geckolib3.extended.LivingEntityRendererAccessor;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.geckolib3.model.provider.data.EntityModelData;
import com.elfmcys.yesstevemodel.geckolib3.util.EModelRenderCycle;
import com.elfmcys.yesstevemodel.geckolib3.util.IRenderCycle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import rip.ysm.api.client.RenderLivingBridge;

import java.util.List;
import java.util.Optional;

public abstract class GeoReplacedEntityRenderer<TEntity extends LivingEntity, T extends LivingAnimatable<TEntity>> extends LivingEntityRenderer<TEntity, GeoLivingEntityRenderState, PlayerModel> implements IGeoRenderer<T> {

    public final List<GeoLayerRenderer<T>> layerRenderers = new ObjectArrayList<>();

    public Matrix4f dispatchedMat = new Matrix4f();

    public Matrix4f renderEarlyMat = new Matrix4f();

    public OrderedSubmitNodeCollector rtb;

    private IRenderCycle currentModelRenderCycle = EModelRenderCycle.INITIAL;

    public GeoReplacedEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5f);
        this.rtb = null;
    }

    public static int packOverlayCoords(LivingEntity entity, float u) {
        return OverlayTexture.pack(OverlayTexture.u(u), OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0));
    }

    @Override
    @NotNull
    public IRenderCycle getCurrentModelRenderCycle() {
        return this.currentModelRenderCycle;
    }

    @Override
    public void setCurrentModelRenderCycle(IRenderCycle cycle) {
        this.currentModelRenderCycle = cycle;
    }

    @Override
    public void renderEarly(T animatable, PoseStack poseStack, float partialTick, OrderedSubmitNodeCollector bufferSource, VertexConsumer buffer, int packedLight, int packedOverlayIn, float red, float green, float blue, float alpha) {
        // 使用 .set 来避免每次渲染创建新的 Matrix4f, 减少 allocation rate
        this.renderEarlyMat.set(poseStack.last().pose());
        IGeoRenderer.super.renderEarly(animatable, poseStack, partialTick, bufferSource, buffer, packedLight, packedOverlayIn, red, green, blue, alpha);
    }

    @Override
    public GeoLivingEntityRenderState createRenderState() { return new GeoLivingEntityRenderState(this); }

    @Override
    public Identifier getTextureLocation(GeoLivingEntityRenderState state) { return state.texture; }

    public GeoLivingEntityRenderState extractPreviewState(T t, float partialTick) {
        GeoLivingEntityRenderState state = createRenderState();
        super.extractRenderState(t.getEntity(), state, partialTick);
        state.lightCoords = 15728880;
        state.nameTag = null;
        state.leashStates = null;
        extractEntityWithTexture(t, null, state, partialTick);
        return state;
    }

    public void extractEntityWithTexture(T t, @Nullable Identifier resourceLocation, GeoLivingEntityRenderState state, float partialTick) {
        Direction bedOrientation;
        PoseStack poseStack = new PoseStack();
        boolean fireRenderEvents = !ModelPreviewRenderer.isPreview();
        if (fireRenderEvents && RenderLivingBridge.firePre(t.getEntity(), this, partialTick, poseStack, null, state.lightCoords)) {
            state.cancelled = true;
            return;
        }
        AnimationEvent<?> event = t.processAnimation(partialTick);
        TEntity entity = t.getEntity();
        Minecraft minecraft = Minecraft.getInstance();
        if (event != null && minecraft.player != null) {
            EntityModelData modelData = event.getModelData();
            this.dispatchedMat.set(poseStack.last().pose());
            setCurrentModelRenderCycle(EModelRenderCycle.INITIAL);
            poseStack.pushPose();
            if (entity.getPose() == Pose.SLEEPING && (bedOrientation = entity.getBedOrientation()) != null) {
                float eyeHeight = entity.getEyeHeight(Pose.STANDING) - 0.1f;
                poseStack.translate((-bedOrientation.getStepX()) * eyeHeight, 0.0f, (-bedOrientation.getStepZ()) * eyeHeight);
            }
            setupRotations(entity, state, poseStack, modelData.lerpedAge, modelData.lerpBodyRot, partialTick);
            if (entity.getVehicle() != null) {
                VehicleCapability.get(entity.getVehicle()).ifPresent(cap -> {
                    Vector3f vector3f = cap.getExpressionOffset();
                    if (vector3f != null) {
                        poseStack.mulPose(new Quaternionf().rotateZYX(vector3f.z, 0.0f, vector3f.x).invert());
                    }
                });
            }
            preRenderCallback(entity, poseStack, partialTick);
            poseStack.translate(0.0f, 0.01f, 0.0f);
            AnimatedGeoModel animatedGeoModel = t.getCurrentModel();
            int textureIndex = resourceLocation == null ? t.getTextureIndex() : 0;
            state.texture = resourceLocation == null ? t.getTextureLocation() : resourceLocation;
            state.renderType = getRenderType(state.texture, isBodyVisible(state) && !entity.isInvisibleTo(minecraft.player), minecraft.shouldEntityAppearGlowing(entity), animatedGeoModel.getGeoModel().isTranslucentTexture(textureIndex));
            state.layersFirst = t.isRenderLayersFirst();
            Color color = getRenderColor(t, partialTick, poseStack, null, null, state.lightCoords);
            state.color.set(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
            state.packedOverlay = packOverlayCoords(entity, getHurtOverlayProgress(entity, partialTick));
            renderWithBone(animatedGeoModel, t, partialTick, poseStack, null, null, state.lightCoords, state.packedOverlay, state.color.x, state.color.y, state.color.z, state.color.w);
            state.bodyPose = poseStack.last().copy();
            if (state.renderType != null) {
                state.model = extractWithBone(animatedGeoModel, t, textureIndex, 0, state.lightCoords);
            }
            if (!entity.isSpectator()) {
                extractLayers(t, state, partialTick, poseStack, event, modelData);
            }
            poseStack.popPose();
        }
        if (fireRenderEvents) {
            RenderLivingBridge.firePost(entity, this, partialTick, poseStack, null, state.lightCoords);
        }
    }

    public void extractLayers(T entity, GeoLivingEntityRenderState state, float partialTick, PoseStack poseStack, AnimationEvent<?> event, EntityModelData data) {
        for (GeoLayerRenderer<T> layerRenderer : this.layerRenderers) {
            layerRenderer.extract(poseStack, state.layers, state.lightCoords, entity, event.getLimbSwing(), event.getLimbSwingAmount(), partialTick, data.lerpedAge, data.rawNetHeadYaw, data.rawHeadPitch);
        }
    }

    @Override
    public void submit(GeoLivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.cancelled) return;
        if (state.model != null) {
            poseStack.pushPose();
            poseStack.mulPose(state.bodyPose.pose());
            renderWithBoneAndRenderType(state.model, state.renderType, poseStack, collector.order(state.layersFirst ? 2 : -1), camera.projectionMatrix, 0, state.packedOverlay, state.color, state.outlineColor);
            poseStack.popPose();
        }
        for (GeoLayerRenderer.Frame layer : state.layers) layer.submit(poseStack, collector, state.lightCoords);
        ((LivingEntityRendererAccessor) this).tlm$renderNameTag(state, poseStack, collector.order(3), camera);
    }

    public float getHurtOverlayProgress(TEntity entity, float partialTick) {
        return 0.0f;
    }

    public void preRenderCallback(TEntity entity, PoseStack poseStack, float partialTick) {
    }

    public void setupRotations(TEntity entity, GeoLivingEntityRenderState state, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
        float deathTime = state.deathTime;
        boolean autoSpinAttack = state.isAutoSpinAttack;
        state.deathTime = 0;
        state.isAutoSpinAttack = false;
        state.ageInTicks = ageInTicks;
        if (entity.onClimbable()) {
            Optional<BlockPos> lastClimbablePos = entity.getLastClimbablePos();
            if (lastClimbablePos.isPresent()) {
                Optional<Direction> optionalValue = entity.level().getBlockState(lastClimbablePos.get()).getOptionalValue(HorizontalDirectionalBlock.FACING);
                if (optionalValue.isPresent()) {
                    rotationYaw = optionalValue.get().getOpposite().get2DDataValue() * 90;
                }
            }
        }
        super.setupRotations(state, poseStack, rotationYaw, 1);
        state.deathTime = deathTime;
        state.isAutoSpinAttack = autoSpinAttack;
    }

    @Override
    public boolean shouldShowName(TEntity entity, double distanceToCameraSq) {
        double d = entity.isDiscrete() ? 32.0d : 64.0d;
        return distanceToCameraSq < d * d && entity == this.entityRenderDispatcher.crosshairPickEntity && entity.hasCustomName() && !Minecraft.getInstance().gui.hud.isHidden();
    }

    public final boolean addLayerRenderer(GeoLayerRenderer<T> layerRenderer) {
        return this.layerRenderers.add(layerRenderer);
    }

    @Override
    public OrderedSubmitNodeCollector getCurrentRTB() {
        return this.rtb;
    }

    @Override
    public void setCurrentRTB(OrderedSubmitNodeCollector bufferSource) {
        this.rtb = bufferSource;
    }
}
