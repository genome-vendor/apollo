package jalview.gui.menus;

public interface ToggleI {
  public void setState(boolean state);
  public void setStateQuietly(boolean state);
  public boolean getState();
  public void setGroup(JalToggleGroup group);
}
