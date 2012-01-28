package jalview.gui.menus;

public abstract class CommandAdapter {
  public abstract void writeLog(CommandLog log);
  public abstract JalCommandVector getCommands();
  public abstract String getType();
  public abstract String getName();
}
