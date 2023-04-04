package nekomods.deckcontrols;

public interface ITouchMenu {
    int padToOption(int x, int y);
    boolean hysteresisExceeded(int option, int x, int y);
    void onPress(int option);
    void onRelease(int option);
    void onChangeWhileClicked(int old_option, int new_option);
    void render(int option, float pPartialTicks);
    boolean useInitialKeydownOption();
}
