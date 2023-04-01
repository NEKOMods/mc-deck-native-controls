package nekomods.deckcontrols;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.function.Consumer;

public class GridTouchMenu implements ITouchMenu {
    private final int[][] WIDTHS;
    private final int[] HEIGHTS;
    private final Consumer<Integer> ONPRESS;
    private final Consumer<Integer> ONRELEASE;

    private static final int HYSTERESIS = 1000;

    public GridTouchMenu(int[][] widths, int[] heights, Consumer<Integer> onPress, Consumer<Integer> onRelease) {
        WIDTHS = widths;
        HEIGHTS = heights;
        ONPRESS = onPress;
        ONRELEASE = onRelease;
        assert widths.length == heights.length;
    }

    public GridTouchMenu(int rows, int cols, Consumer<Integer> onPress, Consumer<Integer> onRelease) {
        assert rows > 0;
        assert cols > 0;
        int[][] widths = new int[rows][];
        int[] heights = new int[rows];
        for (int row = 0; row < rows; row++) {
            widths[row] = new int[cols];
            // FIXME: error spreading?
            for (int col = 0; col < cols; col++) {
                widths[row][col] = 65536 / cols;
            }
            heights[row] = 65536 / rows;
        }
        WIDTHS = widths;
        HEIGHTS = heights;
        ONPRESS = onPress;
        ONRELEASE = onRelease;
    }

    @Override
    public int padToOption(int x, int y) {
        int option = 0;
        int row_min_y = 0;
        int row_max_y = HEIGHTS[0];
        for (int row = 0; row < HEIGHTS.length; row++) {
            if (y >= row_min_y && y < row_max_y) {
                int col_min_x = 0;
                int col_max_x = WIDTHS[row][0];
                for (int col = 0; col < WIDTHS[row].length; col++, option++) {
                    if (x >= col_min_x && x < col_max_x) {
                        return option;
                    }

                    col_min_x = col_max_x;
                    if (col >= WIDTHS[row].length - 2)
                        col_max_x = 65536;
                    else
                        col_max_x += WIDTHS[row][col + 1];
                }
            } else {
                option += WIDTHS[row].length;
            }
            row_min_y = row_max_y;
            if (row >= HEIGHTS.length - 2)
                row_max_y = 65536;
            else
                row_max_y += HEIGHTS[row + 1];
        }

        // should not get here?
        assert false;
        return -1;
    }

    @Override
    public boolean hysteresisExceeded(int option, int x, int y) {
        int row = 0;
        int col = option;
        while (col > WIDTHS[row].length - 1) {
            row++;
            col -= WIDTHS[row].length;
        }

        int min_y = 0;
        for (int i = 0; i < row; i++)
            min_y += HEIGHTS[i];
        int max_y = min_y + HEIGHTS[row];
        min_y -= HYSTERESIS;
        max_y += HYSTERESIS;

        int min_x = 0;
        for (int i = 0; i < col; i++)
            min_x += WIDTHS[row][i];
        int max_x = min_x + WIDTHS[row][col];
        min_x -= HYSTERESIS;
        max_x += HYSTERESIS;

        return !(x >= min_x && x < max_x && y >= min_y && y < max_y);
    }

    @Override
    public void onPress(int option) {
        ONPRESS.accept(option);
    }

    @Override
    public void onRelease(int option) {
        ONRELEASE.accept(option);
    }

    @Override
    public void render(int option, float pPartialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack ps = new PoseStack();
        int screenW = minecraft.getWindow().getGuiScaledWidth();
        int screenH = minecraft.getWindow().getGuiScaledHeight();

        // Render black outline manually
        RenderSystem.setShaderColor(0, 0, 0, 1);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.lineWidth(1);
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
                        2 + 1 + col * 20,
                        2 + screenH - 61 + row * 20,
                        pPartialTicks,
                        player,
                        player.getInventory().items.get(slot),
                        thingy);
            }
        }
    }
}
