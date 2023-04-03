package nekomods.deckcontrols;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

// TODO ALT KEYS
// F1 - F12
// 1-9, 0
// ` - = \ ; '

public class TouchKeyboard {
    private static final Logger LOGGER = LogUtils.getLogger();

    class Left extends GridTouchMenu {
        public Left() {
            super(new int[][] {
                    new int[] {13107, 13107, 13107, 13107, 13108},  // qwert
                    new int[] {13107, 13107, 13107, 13107, 13108},  // asdfg
                    new int[] {13107, 13107, 13107, 13107, 13108},  // zxcvb
                    new int[] {13107, 13107, 39322},                // special, tab, space
            }, new int[] {
                    16384,
                    16384,
                    16384,
                    16384,
            }, (option) -> {
                // TODO
            }, (option) -> {
                // TODO
            }, (old_option, new_option) -> {
                // TODO
            });
        }

        @Override
        public void render(int option, float pPartialTicks) {
            int option_row = option / 5;
            int option_col = option % 5;

            Minecraft minecraft = Minecraft.getInstance();
            PoseStack ps = new PoseStack();
            int screenW = minecraft.getWindow().getGuiScaledWidth();
            int screenH = minecraft.getWindow().getGuiScaledHeight();

            // background
            RenderSystem.setShaderTexture(0, new ResourceLocation("deckcontrols", "textures/ui/uiatlas.png"));
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

            b.vertex(ps.last().pose(), 0, screenH - 45, 0)
                    .uv(0f / 256, 128f / 256).endVertex();
            b.vertex(ps.last().pose(), 0, screenH, 0)
                    .uv(0f / 256, (128f + 45) / 256).endVertex();
            b.vertex(ps.last().pose(), 56, screenH, 0)
                    .uv(56f / 256, (128f + 45) / 256).endVertex();
            b.vertex(ps.last().pose(), 56, screenH - 45, 0)
                    .uv(56f / 256, 128f / 256).endVertex();

            BufferUploader.drawWithShader(b.end());

            // selection
            RenderSystem.setShaderColor(223f / 256, 113f / 256, 38f / 256, 0.5f);
            RenderSystem.setShader(GameRenderer::getPositionShader);
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

            if (option != 17) {
                b.vertex(ps.last().pose(), 1 + option_col * 11, screenH - 45 + 1 + option_row * 11, 0).endVertex();
                b.vertex(ps.last().pose(), 1 + option_col * 11, screenH - 45 + 1 + option_row * 11 + 10, 0).endVertex();
                b.vertex(ps.last().pose(), 1 + option_col * 11 + 10, screenH - 45 + 1 + option_row * 11 + 10, 0).endVertex();
                b.vertex(ps.last().pose(), 1 + option_col * 11 + 10, screenH - 45 + 1 + option_row * 11, 0).endVertex();
            } else {
                b.vertex(ps.last().pose(), 23, screenH - 45 + 1 + option_row * 11, 0).endVertex();
                b.vertex(ps.last().pose(), 23, screenH - 45 + 1 + option_row * 11 + 10, 0).endVertex();
                b.vertex(ps.last().pose(), 23 + 32, screenH - 45 + 1 + option_row * 11 + 10, 0).endVertex();
                b.vertex(ps.last().pose(), 23 + 32, screenH - 45 + 1 + option_row * 11, 0).endVertex();
            }

            BufferUploader.drawWithShader(b.end());

            // labels
            RenderSystem.setShaderColor(1, 1, 1, 1);
            MultiBufferSource.BufferSource bs = MultiBufferSource.immediate(b);
            for (int i = 0; i < 5; i++) {
                minecraft.font.drawInBatch(
                        new String[]{"Q", "W", "E", "R", "T"}[i],
                        3 + 11 * i + (i == 0 ? 1 : 0), screenH - 45 + 3, 0xffffff,
                        false, ps.last().pose(), bs, false, 0, 0xf000f0);
                minecraft.font.drawInBatch(
                        new String[]{"A", "S", "D", "F", "G"}[i],
                        3 + 11 * i + (i == 0 ? 1 : 0), screenH - 45 + 3 + 11, 0xffffff,
                        false, ps.last().pose(), bs, false, 0, 0xf000f0);
                minecraft.font.drawInBatch(
                        new String[]{"Z", "X", "C", "V", "B"}[i],
                        3 + 11 * i + (i == 0 ? 1 : 0), screenH - 45 + 3 + 11 * 2, 0xffffff,
                        false, ps.last().pose(), bs, false, 0, 0xf000f0);
            }
            minecraft.font.drawInBatch(
                    "?",
                    4, screenH - 45 + 3 + 11 * 3, 0xffffff,
                    false, ps.last().pose(), bs, false, 0, 0xf000f0);
            bs.endBatch();
        }
    }

    class Right extends GridTouchMenu {
        public Right() {
            super(new int[][] {
                    new int[] {13107, 13107, 13107, 13107, 13108},  // yuiop
                    new int[] {13107, 13107, 13107, 13107, 13108},  // hjkl enter
                    new int[] {13107, 13107, 13107, 13107, 13108},  // nm,./
                    new int[] {39322, 13107, 13107},                // spacebar []
            }, new int[] {
                    16384,
                    16384,
                    16384,
                    16384,
            }, (option) -> {
                // TODO
            }, (option) -> {
                // TODO
            }, (old_option, new_option) -> {
                // TODO
            });
        }

        @Override
        public void render(int option, float pPartialTicks) {
            int option_row = option / 5;
            int option_col = option % 5;

            Minecraft minecraft = Minecraft.getInstance();
            PoseStack ps = new PoseStack();
            int screenW = minecraft.getWindow().getGuiScaledWidth();
            int screenH = minecraft.getWindow().getGuiScaledHeight();

            // background
            RenderSystem.setShaderTexture(0, new ResourceLocation("deckcontrols", "textures/ui/uiatlas.png"));
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

            b.vertex(ps.last().pose(), screenW - 56, screenH - 45, 0)
                    .uv(0f / 256, 176f / 256).endVertex();
            b.vertex(ps.last().pose(), screenW - 56, screenH, 0)
                    .uv(0f / 256, (176f + 45) / 256).endVertex();
            b.vertex(ps.last().pose(), screenW, screenH, 0)
                    .uv(56f / 256, (176f + 45) / 256).endVertex();
            b.vertex(ps.last().pose(), screenW, screenH - 45, 0)
                    .uv(56f / 256, 176f / 256).endVertex();

            BufferUploader.drawWithShader(b.end());

            // selection
            RenderSystem.setShaderColor(223f / 256, 113f / 256, 38f / 256, 0.5f);
            RenderSystem.setShader(GameRenderer::getPositionShader);
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

            if (option != 15) {
                if (option > 15) option_col += 2;
                b.vertex(ps.last().pose(), screenW - 56 + 1 + option_col * 11, screenH - 45 + 1 + option_row * 11, 0).endVertex();
                b.vertex(ps.last().pose(), screenW - 56 + 1 + option_col * 11, screenH - 45 + 1 + option_row * 11 + 10, 0).endVertex();
                b.vertex(ps.last().pose(), screenW - 56 + 1 + option_col * 11 + 10, screenH - 45 + 1 + option_row * 11 + 10, 0).endVertex();
                b.vertex(ps.last().pose(), screenW - 56 + 1 + option_col * 11 + 10, screenH - 45 + 1 + option_row * 11, 0).endVertex();
            } else {
                b.vertex(ps.last().pose(), screenW - 56 + 1, screenH - 45 + 1 + option_row * 11, 0).endVertex();
                b.vertex(ps.last().pose(), screenW - 56 + 1, screenH - 45 + 1 + option_row * 11 + 10, 0).endVertex();
                b.vertex(ps.last().pose(), screenW - 56 + 1 + 32, screenH - 45 + 1 + option_row * 11 + 10, 0).endVertex();
                b.vertex(ps.last().pose(), screenW - 56 + 1 + 32, screenH - 45 + 1 + option_row * 11, 0).endVertex();
            }

            BufferUploader.drawWithShader(b.end());

            // labels
            RenderSystem.setShaderColor(1, 1, 1, 1);
            MultiBufferSource.BufferSource bs = MultiBufferSource.immediate(b);
            for (int i = 0; i < 5; i++) {
                minecraft.font.drawInBatch(
                        new String[]{"Y", "U", "I", "O", "P"}[i],
                        screenW - 56 + 3 + 11 * i + (i == 0 ? 1 : 0), screenH - 45 + 3, 0xffffff,
                        false, ps.last().pose(), bs, false, 0, 0xf000f0);
                minecraft.font.drawInBatch(
                        new String[]{"H", "J", "K", "L", ""}[i],
                        screenW - 56 + 3 + 11 * i + (i == 0 ? 1 : 0), screenH - 45 + 3 + 11, 0xffffff,
                        false, ps.last().pose(), bs, false, 0, 0xf000f0);
                minecraft.font.drawInBatch(
                        new String[]{"N", "M", ",", ".", "/"}[i],
                        screenW - 56 + 3 + 11 * i + (i == 0 ? 1 : 0), screenH - 45 + 3 + 11 * 2 - (i == 2 || i == 3 ? 2 : 0), 0xffffff,
                        false, ps.last().pose(), bs, false, 0, 0xf000f0);
                minecraft.font.drawInBatch(
                        new String[]{"", "", "", "[", "]"}[i],
                        screenW - 56 + 3 + 11 * i + (i == 0 ? 1 : 0), screenH - 45 + 3 + 11 * 3, 0xffffff,
                        false, ps.last().pose(), bs, false, 0, 0xf000f0);
            }
            bs.endBatch();
        }
    }

    private final Left left = new Left();
    private final Right right = new Right();

    public ITouchMenu getLeft() {
        return left;
    }

    public ITouchMenu getRight() {
        return right;
    }
}
