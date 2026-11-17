package net.j40climb.frenchies.common.entity.frenchie;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

final class FrenchieMouthLayer extends RenderLayer<FrenchieRenderState, FrenchieModel> {
    FrenchieMouthLayer(RenderLayerParent<FrenchieRenderState, FrenchieModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, FrenchieRenderState state,
                       float yaw, float pitch) {
        if (state.isInvisible || (state.mouthItem.isEmpty() && state.noseBubbleFacing == null)) return;
        pose.pushPose();
        getParentModel().setupAnim(state);
        getParentModel().translateToMouth(pose);
        if (state.noseBubbleFacing != null) {
            // Use the animated muzzle for position, retaining the same camera-facing film as a free bubble.
            pose.translate(0, -4.9 / 16, 0.9 / 16);
            var center = pose.last().pose().getTranslation(new org.joml.Vector3f());
            pose.last().set(state.noseBubbleFacing);
            pose.last().pose().setTranslation(center);
            PlayBubbleRenderer.submitFilm(pose, collector, light, state.isBaby ? 0.04F : 0.08F);
        } else {
            pose.mulPose(Axis.ZP.rotationDegrees(180));
            pose.scale(0.72F, 0.72F, 0.72F);
            state.mouthItem.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
        pose.popPose();
    }
}
