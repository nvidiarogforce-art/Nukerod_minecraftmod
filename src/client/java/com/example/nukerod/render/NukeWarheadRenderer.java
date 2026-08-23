package com.example.nukerod.render;

import com.example.nukerod.entity.NukeWarheadEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Renders the {@link NukeWarheadEntity}.
 *
 * <p>Minecraft 1.21.10 replaced the immediate-mode entity render path
 * ({@code VertexConsumerProvider} + manual {@code vertex(...)} calls) with the
 * batched {@link OrderedRenderCommandQueue} system. The previous hand-drawn
 * textured box is therefore not portable as-is, so this renderer keeps only the
 * default behaviour and leaves the warhead invisible while it is in flight
 * (it exists for only a fraction of a second before detonating).
 *
 * <p>To give it a visible body again, register a proper entity model
 * ({@code EntityModel} + {@code TexturedModelData}) and submit it through the
 * render command queue in {@link #render}.
 */
public class NukeWarheadRenderer extends EntityRenderer<NukeWarheadEntity, EntityRenderState> {

    public NukeWarheadRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void render(EntityRenderState state, MatrixStack matrices,
                       OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
        super.render(state, matrices, queue, cameraState);
    }
}
