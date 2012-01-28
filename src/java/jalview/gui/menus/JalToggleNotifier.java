package jalview.gui.menus;

public interface JalToggleNotifier {
  public void signalToggle(ToggleI changed);
  public void signalActionToggle(ToggleI changed);
}
