package nekomods;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

public class OverlayRenderer {
    public static void renderOverlay() {
        PoseStack ps = new PoseStack();
        Minecraft.getInstance().font.draw(ps, "hewwo world!", 0, 0, 0x00ff00);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, new ResourceLocation("textures/gui/book.png"));

        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.vertex(ps.last().pose(), 10, 10, 0).uv(0, 0).endVertex();
        b.vertex(ps.last().pose(), 10, 60, 0).uv(0, 1).endVertex();
        b.vertex(ps.last().pose(), 60, 60, 0).uv(1, 1).endVertex();
        b.vertex(ps.last().pose(), 60, 10, 0).uv(1, 0).endVertex();
        BufferUploader.drawWithShader(b.end());
    }
}
