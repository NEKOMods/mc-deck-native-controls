package nekomods.deckcontrols;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HotbarGridTouchMenu extends GridTouchMenu {
    HotbarGridTouchMenu(Consumer<Integer> onPress, Consumer<Integer> onRelease, BiConsumer<Integer, Integer> onChangeWhileClicked) {
        super(3, 3, onPress, onRelease, onChangeWhileClicked);
    }

    @Override
    public void render(int option, float pPartialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack ps = new PoseStack();
        int screenW = minecraft.getWindow().getGuiScaledWidth();
        int screenH = minecraft.getWindow().getGuiScaledHeight();

        int xbase = 0;
        if (Settings.SWAP_PADS)
            xbase = screenW - 62;
        ps.translate(xbase, 0, 0);

        // Render black outline manually
        RenderSystem.setShaderColor(0, 0, 0, 1);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        b.vertex(ps.last().pose(), 0, screenH - 60 - 2, 0).endVertex();
        b.vertex(ps.last().pose(), 0, screenH, 0).endVertex();
        b.vertex(ps.last().pose(), 1, screenH, 0).endVertex();
        b.vertex(ps.last().pose(), 1, screenH - 60 - 2, 0).endVertex();

        b.vertex(ps.last().pose(), 61, screenH - 60 - 2, 0).endVertex();
        b.vertex(ps.last().pose(), 61, screenH, 0).endVertex();
        b.vertex(ps.last().pose(), 62, screenH, 0).endVertex();
        b.vertex(ps.last().pose(), 62, screenH - 60 - 2, 0).endVertex();

        b.vertex(ps.last().pose(), 0, screenH - 60 - 2, 0).endVertex();
        b.vertex(ps.last().pose(), 0, screenH - 60 - 1, 0).endVertex();
        b.vertex(ps.last().pose(), 62, screenH - 60 - 1, 0).endVertex();
        b.vertex(ps.last().pose(), 62, screenH - 60 - 2, 0).endVertex();

        b.vertex(ps.last().pose(), 0, screenH - 1, 0).endVertex();
        b.vertex(ps.last().pose(), 0, screenH, 0).endVertex();
        b.vertex(ps.last().pose(), 62, screenH, 0).endVertex();
        b.vertex(ps.last().pose(), 62, screenH - 1, 0).endVertex();

        BufferUploader.drawWithShader(b.end());

        // Render the hotbar (rearranged)
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, new ResourceLocation("minecraft", "textures/gui/widgets.png"));
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        b.vertex(ps.last().pose(), 1, screenH - 20*3 - 1, 0)
                .uv(1.0f / 256, 1.0f / 256).endVertex();
        b.vertex(ps.last().pose(), 1, screenH - 20*2 - 1, 0)
                .uv(1.0f / 256, 21.0f / 256).endVertex();
        b.vertex(ps.last().pose(), 1 + 20*3, screenH - 20*2 - 1, 0)
                .uv(61.0f / 256, 21.0f / 256).endVertex();
        b.vertex(ps.last().pose(), 1 + 20*3, screenH - 20*3 - 1, 0)
                .uv(61.0f / 256, 1.0f / 256).endVertex();

        b.vertex(ps.last().pose(), 1, screenH - 20*2 - 1, 0)
                .uv(61.0f / 256, 1.0f / 256).endVertex();
        b.vertex(ps.last().pose(), 1, screenH - 20*1 - 1, 0)
                .uv(61.0f / 256, 21.0f / 256).endVertex();
        b.vertex(ps.last().pose(), 1 + 20*3, screenH - 20*1 - 1, 0)
                .uv(121.0f / 256, 21.0f / 256).endVertex();
        b.vertex(ps.last().pose(), 1 + 20*3, screenH - 20*2 - 1, 0)
                .uv(121.0f / 256, 1.0f / 256).endVertex();

        b.vertex(ps.last().pose(), 1, screenH - 20*1 - 1, 0)
                .uv(121.0f / 256, 1.0f / 256).endVertex();
        b.vertex(ps.last().pose(), 1, screenH - 20*0 - 1, 0)
                .uv(121.0f / 256, 21.0f / 256).endVertex();
        b.vertex(ps.last().pose(), 1 + 20*3, screenH - 20*0 - 1, 0)
                .uv(181.0f / 256, 21.0f / 256).endVertex();
        b.vertex(ps.last().pose(), 1 + 20*3, screenH - 20*1 - 1, 0)
                .uv(181.0f / 256, 1.0f / 256).endVertex();

        // Render the selection
        int sel_row = option / 3;
        int sel_col = option % 3;
        b.vertex(ps.last().pose(), sel_col * 20 - 1, screenH - 20*(3 - sel_row) - 3, 0)
                .uv(0, 22f / 256).endVertex();
        b.vertex(ps.last().pose(), sel_col * 20 - 1, screenH - 20*(3 - sel_row) - 3 + 24, 0)
                .uv(0, 46f / 256).endVertex();
        b.vertex(ps.last().pose(), sel_col * 20 - 1 + 24, screenH - 20*(3 - sel_row) - 3 + 24, 0)
                .uv(24f / 256, 46f / 256).endVertex();
        b.vertex(ps.last().pose(), sel_col * 20 - 1 + 24, screenH - 20*(3 - sel_row) - 3, 0)
                .uv(24f / 256, 22f / 256).endVertex();

        BufferUploader.drawWithShader(b.end());

        // Render the items
        Player player = minecraft.gui.getCameraPlayer();
        if (player != null) {
            int thingy = 1;
            for (int slot = 0; slot < 9; slot++, thingy++) {
                int row = slot / 3;
                int col = slot % 3;
                minecraft.gui.renderSlot(
                        xbase + 2 + 1 + col * 20,
                        2 + screenH - 61 + row * 20,
                        pPartialTicks,
                        player,
                        player.getInventory().items.get(slot),
                        thingy);
            }
        }
    }
}
