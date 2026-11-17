package net.j40climb.frenchies.common.entity.frenchie;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.AnimationState;

public class FrenchieRenderState extends LivingEntityRenderState {
    public boolean captain;
    public int travelOwnerId;
    public boolean hasVest;
    public float turnLean;
    public final ItemStackRenderState mouthItem = new ItemStackRenderState();
    public boolean presenting;
    public float toyEarLift;
    public FrenchieVariant variant;
    public int celebrationStage;
    public PlayPose playPose = PlayPose.NONE;
    public DangerReaction dangerReaction = DangerReaction.NONE;
    public float dangerAge;
    public PoseStack.Pose noseBubbleFacing;
    public boolean isSleeping;
    public boolean isSwimming;
    public boolean isSitting;
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState sleepAnimationState = new AnimationState();
    public final AnimationState swimAnimationState = new AnimationState();
    public final AnimationState standToSitAnimationState = new AnimationState();
    public final AnimationState sitToStandAnimationState = new AnimationState();
    public final AnimationState sitPoseAnimationState = new AnimationState();
    public final AnimationState standToSleepAnimationState = new AnimationState();
    public final AnimationState sleepToStandAnimationState = new AnimationState();

}
