package net.j40climb.frenchies.common.entity.frenchie;

import com.mojang.blaze3d.vertex.PoseStack;
import net.j40climb.frenchies.Frenchies;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class FrenchieModel extends EntityModel<FrenchieRenderState> {

    public static final ModelLayerLocation FRENCHIE = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "frenchie"), "frenchie"); // is path is folder under textures/entity?

    private final KeyframeAnimation walkingAnimation;
    private final KeyframeAnimation swimmingAnimation;
    private final KeyframeAnimation sleepingAnimation;
    private final KeyframeAnimation idlingAnimation;
    private final KeyframeAnimation standToSitAnimation;
    private final KeyframeAnimation sitToStandAnimation;
    private final KeyframeAnimation sitPoseAnimation;
    private final KeyframeAnimation standToSleepAnimation;
    private final KeyframeAnimation sleepToStandAnimation;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;

    public FrenchieModel(ModelPart root) {
        super(root);
        this.root = root.getChild("root");
        this.body = this.root.getChild("body");
        this.head = this.body.getChild("head");

        this.walkingAnimation = FrenchieAnimations.ANIM_WALK.bake(root);
        this.swimmingAnimation = FrenchieAnimations.ANIM_SWIM.bake(root);
        this.sleepingAnimation = FrenchieAnimations.ANIM_SLEEP.bake(root);
        this.idlingAnimation = FrenchieAnimations.ANIM_IDLE.bake(root);
        this.standToSitAnimation = FrenchieAnimations.ANIM_STAND_TO_SIT.bake(root);
        this.sitToStandAnimation = FrenchieAnimations.ANIM_SIT_TO_STAND.bake(root);
        this.sitPoseAnimation = FrenchieAnimations.ANIM_SIT.bake(root);
        this.standToSleepAnimation = FrenchieAnimations.ANIM_STAND_TO_SLEEP.bake(root);
        this.sleepToStandAnimation = FrenchieAnimations.ANIM_SLEEP_TO_STAND.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 18.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 29).addBox(-2.983F, -3.3875F, -3.875F, 6.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(30, 21).addBox(-1.983F, -0.3875F, -4.875F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.483F, -1.6125F, -4.125F));

        PartDefinition earR = head.addOrReplaceChild("earR", CubeListBuilder.create(), PartPose.offset(-2.733F, -3.0498F, -3.8337F));

        earR.addOrReplaceChild("earR_behind_r1", CubeListBuilder.create().texOffs(1, 21).mirror().addBox(-1.0F, -2.8377F, 2.9587F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(1, 24).mirror().addBox(-1.0F, -2.8377F, 2.9087F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.2618F));

        PartDefinition earL = head.addOrReplaceChild("earL", CubeListBuilder.create(), PartPose.offset(0.767F, -3.3498F, -3.8337F));

        earL.addOrReplaceChild("earL_behind_r1", CubeListBuilder.create().texOffs(1, 21).addBox(0.0F, -2.8377F, 2.9587F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(1, 24).addBox(0.0F, -2.8377F, 2.9087F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2618F));

        FrenchieTravelGesture.addTongue(head);

        PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(1, 1).addBox(-3.5F, -4.25F, -5.5F, 7.0F, 7.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 1.25F, 1.5F));

        chest.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(30, 8).addBox(-1.0F, -0.5F, 0.25F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -2.75F, 5.25F));

        body.addOrReplaceChild("leg_front_left", CubeListBuilder.create().texOffs(21, 21).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, 3.0F, -2.0F));

        body.addOrReplaceChild("leg_front_right", CubeListBuilder.create().texOffs(21, 36).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 3.0F, -2.0F));

        body.addOrReplaceChild("leg_back_left", CubeListBuilder.create().texOffs(21, 26).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, 3.0F, 5.0F));

        body.addOrReplaceChild("leg_back_right", CubeListBuilder.create().texOffs(21, 31).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 3.0F, 5.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(FrenchieRenderState renderState) {
        super.setupAnim(renderState);
        head.getChild("tongue").visible = renderState.captain;

        if (renderState.playPose == PlayPose.SLIME_BOW || renderState.playPose == PlayPose.BUBBLE_BOW) {
            // Pivot the chest down, stretch the forepaws out and keep the hind paws planted.
            body.xRot = 0.42F;
            body.y += 0.3F;
            ModelPart left = body.getChild("leg_front_left");
            ModelPart right = body.getChild("leg_front_right");
            left.xRot = right.xRot = -1.12F;
            left.z -= 0.65F;
            right.z -= 0.65F;
            body.getChild("leg_back_left").y += 1.9F;
            body.getChild("leg_back_right").y += 1.9F;
            applyHeadRotation(renderState.yRot, renderState.xRot * 0.4F);
            head.xRot -= 0.25F;
            return;
        }
        if (renderState.celebrationStage == CelebrationGoal.BOW) {
            body.xRot = 0.32F;
            body.y += 1.2F;
            head.xRot = -0.24F;
            body.getChild("leg_front_left").xRot = -1.05F;
            body.getChild("leg_front_right").xRot = -1.05F;
            return;
        }
        if (renderState.celebrationStage == CelebrationGoal.SPLOOT) {
            body.y += 1.9F;
            head.y += 1.0F;
            head.xRot = -0.1F;
            body.getChild("leg_front_left").xRot = -1.25F;
            body.getChild("leg_front_right").xRot = -1.25F;
            // Splay the hind paws beyond the hips so the resting silhouette reads from either side.
            ModelPart left = body.getChild("leg_back_left");
            ModelPart right = body.getChild("leg_back_right");
            left.xRot = right.xRot = 1.4F;
            left.yRot = 0.38F;
            right.yRot = -0.38F;
            left.z += 0.9F;
            right.z += 0.9F;
            return;
        }

        if (renderState.captain) {
            sitPoseAnimation.applyStatic();
            body.zRot += renderState.turnLean;
            head.xRot -= 0.12F;
            FrenchieTravelGesture.applyInBoat(head, renderState.ageInTicks, renderState.travelOwnerId);
            return;
        }

        // Don't rotate head/body when sleeping or sitting
        if (!renderState.isSleeping && (!renderState.isSitting || renderState.presenting)) {
            this.applyHeadRotation(renderState.yRot, renderState.xRot);
        }

        // Apply walk animation (swimming uses its own animation state below)
        if (!renderState.isSwimming) {
            this.walkingAnimation.applyWalk(renderState.walkAnimationPos, renderState.walkAnimationSpeed, 2.5F, 2.5F);
        }

        // Apply swim animation when in water
        if (renderState.isSwimming) {
            this.swimmingAnimation.apply(renderState.swimAnimationState, renderState.ageInTicks, 1.0F);
        }

        // Apply sleeping transitions and pose
        this.standToSleepAnimation.apply(renderState.standToSleepAnimationState, renderState.ageInTicks, 1.0F);
        this.sleepToStandAnimation.apply(renderState.sleepToStandAnimationState, renderState.ageInTicks, 1.0F);
        this.sleepingAnimation.apply(renderState.sleepAnimationState, renderState.ageInTicks, 1.0F);

        // Apply sitting transitions and pose
        this.standToSitAnimation.apply(renderState.standToSitAnimationState, renderState.ageInTicks, 1.0F);
        this.sitToStandAnimation.apply(renderState.sitToStandAnimationState, renderState.ageInTicks, 1.0F);
        this.sitPoseAnimation.apply(renderState.sitPoseAnimationState, renderState.ageInTicks, 1.0F);

        // Apply idle animation
        this.idlingAnimation.apply(renderState.idleAnimationState, renderState.ageInTicks, 1.0F);
        float attention = renderState.toyEarLift;
        if (renderState.playPose == PlayPose.SLIME_NOTICE || renderState.playPose == PlayPose.BUBBLE_NOSE) {
            head.zRot += renderState.playPose == PlayPose.SLIME_NOTICE ? 0.23F : -0.15F;
            attention = 1;
        }
        if (renderState.playPose == PlayPose.BUBBLE_WATCH && (renderState.isSleeping || renderState.isSitting)) {
            head.yRot += Mth.clamp(renderState.yRot, -25F, 25F) * Mth.DEG_TO_RAD;
            head.xRot -= 0.1F;
        }
        if (attention > 0 && !renderState.isSleeping && !renderState.isSitting) {
            float lift = attention * (renderState.isBaby ? 0.15F : 0.3F);
            head.getChild("earL").zRot -= lift;
            head.getChild("earR").zRot += lift;
            head.getChild("earL").y -= lift;
            head.getChild("earR").y -= lift;
        }
    }

    void translateToChest(PoseStack pose) {
        root.translateAndRotate(pose);
        body.translateAndRotate(pose);
        body.getChild("chest").translateAndRotate(pose);
    }

    void translateToMouth(PoseStack pose) {
        root.translateAndRotate(pose);
        body.translateAndRotate(pose);
        head.translateAndRotate(pose);
        pose.translate(0, 3.0 / 16, -5.5 / 16);
    }

    private void applyHeadRotation(float headYaw, float headPitch) {
        headYaw = Mth.clamp(headYaw, -30f, 30f);
        headPitch = Mth.clamp(headPitch, -32.5f, 45f);

        this.head.yRot = headYaw * ((float)Math.PI / 180f);
        this.head.xRot = headPitch *  ((float)Math.PI / 180f);
    }
}
