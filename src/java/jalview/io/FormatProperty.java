package jalview.io;

public class FormatProperty {
  String description;
  String className;

  public FormatProperty(String description, String className) {
    this.description = new String(description);
    this.className   = new String(className);
  }

  public String getClassName() {
    return className;
  }
  public String getDescription() {
    return description;
  }
}
