package nekomods.deckcontrols;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

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
            LOGGER.debug("KEYBOARD LEFT OPTION IS " + option);
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
            LOGGER.debug("KEYBOARD RIGHT OPTION IS " + option);
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
