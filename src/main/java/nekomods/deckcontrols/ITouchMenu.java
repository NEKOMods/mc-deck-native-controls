package nekomods.deckcontrols;

public interface ITouchMenu {
    int padToOption(int x, int y);
    boolean hysteresisExceeded(int option, int x, int y);
    void onPress(int option);
    void onRelease(int option);
}
