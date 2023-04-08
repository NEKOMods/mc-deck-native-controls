package nekomods.deckcontrols;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;

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
            }, (_option) -> {
            }, (option) -> {
                // keyup
                if (option == 15) {
                    sym_mode = true;
                } else {
                    ONKEY.accept(new int[] {
                            'q', 'w', 'e', 'r', 't',
                            'a', 's', 'd', 'f', 'g',
                            'z', 'x', 'c', 'v', 'b',
                            0, '\t', ' ',
                    }[option]);
                }
            }, (_old_option, _new_option) -> {
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
            screenH -= 16;  // don't overlap chat

            // background
            RenderSystem.setShaderTexture(0, new ResourceLocation("deckcontrols", "textures/ui/uiatlas.png"));
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

            if (!DeckControls.HOOKS.shift_pressed) {
                b.vertex(ps.last().pose(), 0, screenH - 45, 0)
                        .uv(0f / 256, 128f / 256).endVertex();
                b.vertex(ps.last().pose(), 0, screenH, 0)
                        .uv(0f / 256, (128f + 45) / 256).endVertex();
                b.vertex(ps.last().pose(), 56, screenH, 0)
                        .uv(56f / 256, (128f + 45) / 256).endVertex();
                b.vertex(ps.last().pose(), 56, screenH - 45, 0)
                        .uv(56f / 256, 128f / 256).endVertex();
            } else {
                b.vertex(ps.last().pose(), 0, screenH - 45, 0)
                        .uv(0f / 256, 192f / 256).endVertex();
                b.vertex(ps.last().pose(), 0, screenH, 0)
                        .uv(0f / 256, (192f + 45) / 256).endVertex();
                b.vertex(ps.last().pose(), 56, screenH, 0)
                        .uv(56f / 256, (192f + 45) / 256).endVertex();
                b.vertex(ps.last().pose(), 56, screenH - 45, 0)
                        .uv(56f / 256, 192f / 256).endVertex();
            }

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
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }

        @Override
        public boolean useInitialKeydownOption() {
            return false;
        }
    }

    class Right extends GridTouchMenu {
        public Right() {
            super(new int[][] {
                    new int[] {13107, 13107, 13107, 13107, 13108},  // yuiop
                    new int[] {13107, 13107, 13107, 13107, 13108},  // hjkl backspace
                    new int[] {13107, 13107, 13107, 13107, 13108},  // nm,. enter
                    new int[] {39322, 13107, 13107},                // spacebar []
            }, new int[] {
                    16384,
                    16384,
                    16384,
                    16384,
            }, (_option) -> {
            }, (option) -> {
                // keyup
                ONKEY.accept(new int[] {
                        'y', 'u', 'i', 'o', 'p',
                        'h', 'j', 'k', 'l', '\b',
                        'n', 'm', ',', '.', '\n',
                        ' ', '[', ']',
                }[option]);
            }, (_old_option, _new_option) -> {
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
            screenH -= 16;  // don't overlap chat

            // background
            RenderSystem.setShaderTexture(0, new ResourceLocation("deckcontrols", "textures/ui/uiatlas.png"));
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

            if (!DeckControls.HOOKS.shift_pressed) {
                b.vertex(ps.last().pose(), screenW - 56, screenH - 45, 0)
                        .uv(64f / 256, 128f / 256).endVertex();
                b.vertex(ps.last().pose(), screenW - 56, screenH, 0)
                        .uv(64f / 256, (128f + 45) / 256).endVertex();
                b.vertex(ps.last().pose(), screenW, screenH, 0)
                        .uv((64f + 56) / 256, (128f + 45) / 256).endVertex();
                b.vertex(ps.last().pose(), screenW, screenH - 45, 0)
                        .uv((64f + 56) / 256, 128f / 256).endVertex();
            } else {
                b.vertex(ps.last().pose(), screenW - 56, screenH - 45, 0)
                        .uv(64f / 256, 192f / 256).endVertex();
                b.vertex(ps.last().pose(), screenW - 56, screenH, 0)
                        .uv(64f / 256, (192f + 45) / 256).endVertex();
                b.vertex(ps.last().pose(), screenW, screenH, 0)
                        .uv((64f + 56) / 256, (192f + 45) / 256).endVertex();
                b.vertex(ps.last().pose(), screenW, screenH - 45, 0)
                        .uv((64f + 56) / 256, 192f / 256).endVertex();
            }

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
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }

        @Override
        public boolean useInitialKeydownOption() {
            return false;
        }
    }

    class LeftSym extends GridTouchMenu {
        public LeftSym() {
            super(new int[][] {
                    new int[] {21845, 21845, 21846},                // F1, F2, F3
                    new int[] {21845, 21845, 21846},                // F4, F5, F6
                    new int[] {13107, 13107, 13107, 13107, 13108},  // 12345
                    new int[] {13107, 13107, 13107, 13107, 13108},  // dummy -=/\
                    new int[] {13107, 13107, 39322},                // special, dummy, spacebar
            }, new int[] {
                    13107,
                    13107,
                    13107,
                    13107,
                    13108,
            }, (_option) -> {
            }, (option) -> {
                // keyup
                if (option == 16) {
                    sym_mode = false;
                } else if (option == 11 || option == 17) {
                    // dummy slot, do nothing
                } else {
                    ONKEY.accept(new int[] {
                            GLFW_KEY_F1, GLFW_KEY_F2, GLFW_KEY_F3,
                            GLFW_KEY_F4, GLFW_KEY_F5, GLFW_KEY_F6,
                            '1', '2', '3', '4', '5',
                            0, '-', '=', '/', '\\',
                            0, 0, ' ',
                    }[option]);
                }
            }, (_old_option, _new_option) -> {
            });
        }

        @Override
        public void render(int option, float pPartialTicks) {
            int option_row;
            int option_col;

            if (option < 6) {
                option_row = option / 3;
                option_col = option % 3;
            } else {
                option_row = 2 + ((option - 6) / 5);
                option_col = (option - 6) % 5;
            }

            Minecraft minecraft = Minecraft.getInstance();
            PoseStack ps = new PoseStack();
            int screenW = minecraft.getWindow().getGuiScaledWidth();
            int screenH = minecraft.getWindow().getGuiScaledHeight();
            screenH -= 16;  // don't overlap chat

            // background
            RenderSystem.setShaderTexture(0, new ResourceLocation("deckcontrols", "textures/ui/uiatlas.png"));
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

            if (!DeckControls.HOOKS.shift_pressed) {
                b.vertex(ps.last().pose(), 0, screenH - 56, 0)
                        .uv(128f / 256, 128f / 256).endVertex();
                b.vertex(ps.last().pose(), 0, screenH, 0)
                        .uv(128f / 256, (128f + 56) / 256).endVertex();
                b.vertex(ps.last().pose(), 56, screenH, 0)
                        .uv((128f + 56) / 256, (128f + 56) / 256).endVertex();
                b.vertex(ps.last().pose(), 56, screenH - 56, 0)
                        .uv((128f + 56) / 256, 128f / 256).endVertex();
            } else {
                b.vertex(ps.last().pose(), 0, screenH - 56, 0)
                        .uv(128f / 256, 192f / 256).endVertex();
                b.vertex(ps.last().pose(), 0, screenH, 0)
                        .uv(128f / 256, (192f + 56) / 256).endVertex();
                b.vertex(ps.last().pose(), 56, screenH, 0)
                        .uv((128f + 56) / 256, (192f + 56) / 256).endVertex();
                b.vertex(ps.last().pose(), 56, screenH - 56, 0)
                        .uv((128f + 56) / 256, 192f / 256).endVertex();
            }

            BufferUploader.drawWithShader(b.end());

            // selection
            // skip dummy
            if (!(option == 11 || option == 17)) {
                RenderSystem.setShaderColor(223f / 256, 113f / 256, 38f / 256, 0.5f);
                RenderSystem.setShader(GameRenderer::getPositionShader);
                b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

                if (option_row < 2) {
                    // F keys
                    if (option_col == 0) {
                        b.vertex(ps.last().pose(), 1, screenH - 56 + 1 + option_row * 11, 0).endVertex();
                        b.vertex(ps.last().pose(), 1, screenH - 56 + 1 + option_row * 11 + 10, 0).endVertex();
                        b.vertex(ps.last().pose(), 1 + 17, screenH - 56 + 1 + option_row * 11 + 10, 0).endVertex();
                        b.vertex(ps.last().pose(), 1 + 17, screenH - 56 + 1 + option_row * 11, 0).endVertex();
                    } else if (option_col == 1) {
                        b.vertex(ps.last().pose(), 19, screenH - 56 + 1 + option_row * 11, 0).endVertex();
                        b.vertex(ps.last().pose(), 19, screenH - 56 + 1 + option_row * 11 + 10, 0).endVertex();
                        b.vertex(ps.last().pose(), 19 + 18, screenH - 56 + 1 + option_row * 11 + 10, 0).endVertex();
                        b.vertex(ps.last().pose(), 19 + 18, screenH - 56 + 1 + option_row * 11, 0).endVertex();
                    } else {
                        b.vertex(ps.last().pose(), 38, screenH - 56 + 1 + option_row * 11, 0).endVertex();
                        b.vertex(ps.last().pose(), 38, screenH - 56 + 1 + option_row * 11 + 10, 0).endVertex();
                        b.vertex(ps.last().pose(), 38 + 17, screenH - 56 + 1 + option_row * 11 + 10, 0).endVertex();
                        b.vertex(ps.last().pose(), 38 + 17, screenH - 56 + 1 + option_row * 11, 0).endVertex();
                    }
                } else if (option != 18) {
                    // not spacebar
                    b.vertex(ps.last().pose(), 1 + option_col * 11, screenH - 56 + 1 + option_row * 11, 0).endVertex();
                    b.vertex(ps.last().pose(), 1 + option_col * 11, screenH - 56 + 1 + option_row * 11 + 10, 0).endVertex();
                    b.vertex(ps.last().pose(), 1 + option_col * 11 + 10, screenH - 56 + 1 + option_row * 11 + 10, 0).endVertex();
                    b.vertex(ps.last().pose(), 1 + option_col * 11 + 10, screenH - 56 + 1 + option_row * 11, 0).endVertex();
                } else {
                    // spacebar
                    b.vertex(ps.last().pose(), 23, screenH - 56 + 1 + option_row * 11, 0).endVertex();
                    b.vertex(ps.last().pose(), 23, screenH - 56 + 1 + option_row * 11 + 10, 0).endVertex();
                    b.vertex(ps.last().pose(), 23 + 32, screenH - 56 + 1 + option_row * 11 + 10, 0).endVertex();
                    b.vertex(ps.last().pose(), 23 + 32, screenH - 56 + 1 + option_row * 11, 0).endVertex();
                }

                BufferUploader.drawWithShader(b.end());
                RenderSystem.setShaderColor(1, 1, 1, 1);
            }
        }

        @Override
        public boolean useInitialKeydownOption() {
            return false;
        }
    }

    class RightSym extends GridTouchMenu {
        public RightSym() {
            super(new int[][] {
                    new int[] {21845, 21845, 21846},                // F7, F8, F9
                    new int[] {21845, 21845, 21846},                // F10, F11, F12
                    new int[] {13107, 13107, 13107, 13107, 13108},  // 67890
                    new int[] {13107, 13107, 13107, 13107, 13108},  // ` ; ' backspace enter
                    new int[] {39322, 26214},                       // spacebar, dummy
            }, new int[] {
                    13107,
                    13107,
                    13107,
                    13107,
                    13108,
            }, (_option) -> {
            }, (option) -> {
                // keyup
                if (option != 17) {
                    ONKEY.accept(new int[]{
                            GLFW_KEY_F7, GLFW_KEY_F8, GLFW_KEY_F9,
                            GLFW_KEY_F10, GLFW_KEY_F11, GLFW_KEY_F12,
                            '6', '7', '8', '9', '0',
                            '`', ';', '\'', '\b', '\n',
                            ' ', 0,
                    }[option]);
                }
            }, (_old_option, _new_option) -> {
            });
        }

        @Override
        public void render(int option, float pPartialTicks) {
            int option_row;
            int option_col;

            if (option < 6) {
                option_row = option / 3;
                option_col = option % 3;
            } else {
                option_row = 2 + ((option - 6) / 5);
                option_col = (option - 6) % 5;
            }

            Minecraft minecraft = Minecraft.getInstance();
            PoseStack ps = new PoseStack();
            int screenW = minecraft.getWindow().getGuiScaledWidth();
            int screenH = minecraft.getWindow().getGuiScaledHeight();
            screenH -= 16;  // don't overlap chat

            // background
            RenderSystem.setShaderTexture(0, new ResourceLocation("deckcontrols", "textures/ui/uiatlas.png"));
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

            if (!DeckControls.HOOKS.shift_pressed) {
                b.vertex(ps.last().pose(), screenW - 56, screenH - 56, 0)
                        .uv(192f / 256, 128f / 256).endVertex();
                b.vertex(ps.last().pose(), screenW - 56, screenH, 0)
                        .uv(192f / 256, (128f + 56) / 256).endVertex();
                b.vertex(ps.last().pose(), screenW, screenH, 0)
                        .uv((192f + 56) / 256, (128f + 56) / 256).endVertex();
                b.vertex(ps.last().pose(), screenW, screenH - 56, 0)
                        .uv((192f + 56) / 256, 128f / 256).endVertex();
            } else {
                b.vertex(ps.last().pose(), screenW - 56, screenH - 56, 0)
                        .uv(192f / 256, 192f / 256).endVertex();
                b.vertex(ps.last().pose(), screenW - 56, screenH, 0)
                        .uv(192f / 256, (192f + 56) / 256).endVertex();
                b.vertex(ps.last().pose(), screenW, screenH, 0)
                        .uv((192f + 56) / 256, (192f + 56) / 256).endVertex();
                b.vertex(ps.last().pose(), screenW, screenH - 56, 0)
                        .uv((192f + 56) / 256, 192f / 256).endVertex();
            }

            BufferUploader.drawWithShader(b.end());

            // selection
            // skip dummy
            if (option != 17) {
                RenderSystem.setShaderColor(223f / 256, 113f / 256, 38f / 256, 0.5f);
                RenderSystem.setShader(GameRenderer::getPositionShader);
                b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

                if (option_row < 2) {
                    // F keys
                    if (option_col == 0) {
                        b.vertex(ps.last().pose(), screenW - 56 + 1, screenH - 56 + 1 + option_row * 11, 0).endVertex();
                        b.vertex(ps.last().pose(), screenW - 56 + 1, screenH - 56 + 1 + option_row * 11 + 10, 0).endVertex();
                        b.vertex(ps.last().pose(), screenW - 56 + 1 + 17, screenH - 56 + 1 + option_row * 11 + 10, 0).endVertex();
                        b.vertex(ps.last().pose(), screenW - 56 + 1 + 17, screenH - 56 + 1 + option_row * 11, 0).endVertex();
                    } else if (option_col == 1) {
                        b.vertex(ps.last().pose(), screenW - 56 + 19, screenH - 56 + 1 + option_row * 11, 0).endVertex();
                        b.vertex(ps.last().pose(), screenW - 56 + 19, screenH - 56 + 1 + option_row * 11 + 10, 0).endVertex();
                        b.vertex(ps.last().pose(), screenW - 56 + 19 + 18, screenH - 56 + 1 + option_row * 11 + 10, 0).endVertex();
                        b.vertex(ps.last().pose(), screenW - 56 + 19 + 18, screenH - 56 + 1 + option_row * 11, 0).endVertex();
                    } else {
                        b.vertex(ps.last().pose(), screenW - 56 + 38, screenH - 56 + 1 + option_row * 11, 0).endVertex();
                        b.vertex(ps.last().pose(), screenW - 56 + 38, screenH - 56 + 1 + option_row * 11 + 10, 0).endVertex();
                        b.vertex(ps.last().pose(), screenW - 56 + 38 + 17, screenH - 56 + 1 + option_row * 11 + 10, 0).endVertex();
                        b.vertex(ps.last().pose(), screenW - 56 + 38 + 17, screenH - 56 + 1 + option_row * 11, 0).endVertex();
                    }
                } else if (option != 16) {
                    // not spacebar
                    b.vertex(ps.last().pose(), screenW - 56 + 1 + option_col * 11, screenH - 56 + 1 + option_row * 11, 0).endVertex();
                    b.vertex(ps.last().pose(), screenW - 56 + 1 + option_col * 11, screenH - 56 + 1 + option_row * 11 + 10, 0).endVertex();
                    b.vertex(ps.last().pose(), screenW - 56 + 1 + option_col * 11 + 10, screenH - 56 + 1 + option_row * 11 + 10, 0).endVertex();
                    b.vertex(ps.last().pose(), screenW - 56 + 1 + option_col * 11 + 10, screenH - 56 + 1 + option_row * 11, 0).endVertex();
                } else {
                    // spacebar
                    b.vertex(ps.last().pose(), screenW - 56 + 1, screenH - 56 + 1 + option_row * 11, 0).endVertex();
                    b.vertex(ps.last().pose(), screenW - 56 + 1, screenH - 56 + 1 + option_row * 11 + 10, 0).endVertex();
                    b.vertex(ps.last().pose(), screenW - 56 + 1 + 32, screenH - 56 + 1 + option_row * 11 + 10, 0).endVertex();
                    b.vertex(ps.last().pose(), screenW - 56 + 1 + 32, screenH - 56 + 1 + option_row * 11, 0).endVertex();
                }

                BufferUploader.drawWithShader(b.end());
                RenderSystem.setShaderColor(1, 1, 1, 1);
            }
        }

        @Override
        public boolean useInitialKeydownOption() {
            return false;
        }
    }

    private final Left left = new Left();
    private final LeftSym leftSym = new LeftSym();
    private final Right right = new Right();
    private final RightSym rightSym = new RightSym();

    private final Consumer<Integer> ONKEY;

    private boolean sym_mode = false;

    public TouchKeyboard(Consumer<Integer> onKey) {
        ONKEY = onKey;
    }

    public ITouchMenu getLeft() {
        if (!sym_mode)
            return left;
        else
            return leftSym;
    }

    public ITouchMenu getRight() {
        if (!sym_mode)
            return right;
        else
            return rightSym;
    }

    public void resetState() {
        sym_mode = false;
    }
}
