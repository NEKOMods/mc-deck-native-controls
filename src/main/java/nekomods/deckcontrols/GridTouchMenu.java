package nekomods.deckcontrols;

import net.minecraft.client.Minecraft;

import static org.lwjgl.glfw.GLFW.*;

public class GridTouchMenu implements ITouchMenu {
    private final int[][] WIDTHS;
    private final int[] HEIGHTS;
    private final int[] KEYS;

    private static final int HYSTERESIS = 1000;

    public GridTouchMenu(int[][] widths, int[] heights, int[] keys) {
        WIDTHS = widths;
        HEIGHTS = heights;
        KEYS = keys;
        assert widths.length == heights.length;
    }

    public GridTouchMenu(int rows, int cols, int[] keys) {
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
        KEYS = keys;
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
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.keyboardHandler.keyPress(
                minecraft.getWindow().getWindow(),
                KEYS[option],
                glfwGetKeyScancode(KEYS[option]),
                GLFW_PRESS,
                0);
    }

    @Override
    public void onRelease(int option) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.keyboardHandler.keyPress(
                minecraft.getWindow().getWindow(),
                KEYS[option],
                glfwGetKeyScancode(KEYS[option]),
                GLFW_RELEASE,
                0);
    }
}
