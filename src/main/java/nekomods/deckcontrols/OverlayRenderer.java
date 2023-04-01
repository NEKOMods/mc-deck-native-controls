package nekomods.deckcontrols;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

public class OverlayRenderer {
    private static final int UIATLAS_BTN_A = 0;
    private static final int UIATLAS_BTN_B = 1;
    private static final int UIATLAS_BTN_X = 2;
    private static final int UIATLAS_BTN_Y = 3;
    private static final int UIATLAS_MOUSE_L = 16;
    private static final int UIATLAS_MOUSE_R = 17;
    private static final int UIATLAS_SNEAKING = 32;

    private static void drawUiAtlasElem(BufferBuilder b, PoseStack ps, float x, float y, int atlasElem) {
        int atlasRow = atlasElem / 16;
        int atlasCol = atlasElem % 16;

        b.vertex(ps.last().pose(), x, y, 0)
                .uv(atlasCol * 16.0f / 256, atlasRow * 16.0f / 256).endVertex();
        b.vertex(ps.last().pose(), x, y + 16, 0)
                .uv(atlasCol * 16.0f / 256, (atlasRow + 1) * 16.0f / 256).endVertex();
        b.vertex(ps.last().pose(), x + 16, y + 16, 0)
                .uv((atlasCol + 1) * 16.0f / 256, (atlasRow + 1) * 16.0f / 256).endVertex();
        b.vertex(ps.last().pose(), x + 16, y, 0)
                .uv((atlasCol + 1) * 16.0f / 256, atlasRow * 16.0f / 256).endVertex();
    }

    public static void renderOverlay(float pPartialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack ps = new PoseStack();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int screenW = minecraft.getWindow().getGuiScaledWidth();
        int screenH = minecraft.getWindow().getGuiScaledHeight();

        RenderSystem.setShaderTexture(0, new ResourceLocation("deckcontrols", "textures/ui/uiatlas.png"));
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        if (DeckControls.HOOKS != null && DeckControls.INPUT != null && DeckControls.INPUT.isAlive()) {
            if (minecraft.screen == null) {
                if (!DeckControls.HOOKS.btn_b_is_right_click) {
                    drawUiAtlasElem(b, ps, screenW - 16 * 4, screenH - 16, UIATLAS_BTN_X);
                    drawUiAtlasElem(b, ps, screenW - 16 * 3, screenH - 16, UIATLAS_MOUSE_R);
                    drawUiAtlasElem(b, ps, screenW - 16 * 2, screenH - 16, UIATLAS_BTN_B);
                    drawUiAtlasElem(b, ps, screenW - 16 * 1, screenH - 16, UIATLAS_MOUSE_L);
                } else {
                    drawUiAtlasElem(b, ps, screenW - 16 * 4, screenH - 16, UIATLAS_BTN_X);
                    drawUiAtlasElem(b, ps, screenW - 16 * 3, screenH - 16, UIATLAS_MOUSE_L);
                    drawUiAtlasElem(b, ps, screenW - 16 * 2, screenH - 16, UIATLAS_BTN_B);
                    drawUiAtlasElem(b, ps, screenW - 16 * 1, screenH - 16, UIATLAS_MOUSE_R);
                }
                if (DeckControls.HOOKS.sneak_is_latched) {
                    drawUiAtlasElem(b, ps, screenW - 16, screenH - 16 * 2, UIATLAS_SNEAKING);
                }
            }
        }
        BufferUploader.drawWithShader(b.end());

        if (DeckControls.HOOKS != null) {
            if (DeckControls.HOOKS.lpad_menu_selection != -1 && DeckControls.HOOKS.lpad_menu != null) {
                DeckControls.HOOKS.lpad_menu.render(DeckControls.HOOKS.lpad_menu_selection, pPartialTicks);
            }
        }
    }
}