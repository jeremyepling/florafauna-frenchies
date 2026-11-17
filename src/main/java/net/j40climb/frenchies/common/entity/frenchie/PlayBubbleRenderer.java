package net.j40climb.frenchies.common.entity.frenchie;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.j40climb.frenchies.Frenchies;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class PlayBubbleRenderer extends EntityRenderer<PlayBubbleEntity, EntityRenderState> {
    private static final RenderType FILM = RenderTypes.entityTranslucentCullItemTarget(
            Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "textures/entity/play_bubble.png"));

    public PlayBubbleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poses, SubmitNodeCollector nodes, CameraRenderState camera) {
        poses.pushPose();
        poses.translate(0, 0.09, 0);
        poses.mulPose(camera.orientation);
        nodes.submitCustomGeometry(poses, FILM, (pose, vertices) -> {
            vertex(vertices, pose, state.lightCoords, -0.11F, -0.11F, 0, 1);
            vertex(vertices, pose, state.lightCoords, 0.11F, -0.11F, 1, 1);
            vertex(vertices, pose, state.lightCoords, 0.11F, 0.11F, 1, 0);
            vertex(vertices, pose, state.lightCoords, -0.11F, 0.11F, 0, 0);
        });
        poses.popPose();
        super.submit(state, poses, nodes, camera);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, int light, float x, float y, int u, int v) {
        buffer.addVertex(pose, x, y, 0).setColor(-1).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
    }
}
