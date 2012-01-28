package jalview.gui.schemes;

public class ColourSchemeProperty {
  String description;
  String className;
  String menuString = null;

  public ColourSchemeProperty(String description, String className, String menuString) {
    this.description = new String(description);
    this.className   = new String(className);
    if (menuString != null) {
      this.menuString = new String(menuString);
    }
  }

  public String getClassName() {
    return className;
  }
  public String getDescription() {
    return description;
  }
  public String getMenuString() {
    return menuString;
  }
  public boolean isMenuItem() {
    return (menuString != null);
  }
}
