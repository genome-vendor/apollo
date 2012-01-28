package apollo.config;

import java.io.*;
import java.util.*;

public class ColumnProperty
  implements Cloneable, java.io.Serializable {

  protected String  heading;
  protected int     preferredWidth = 1;

  public ColumnProperty (String heading) {
    this(heading,1);
  }

  public ColumnProperty(String heading, int prefWidth) {
    this.heading = heading;
    this.preferredWidth = prefWidth;
  }

  public ColumnProperty(ColumnProperty from) {
    this.heading = from.getHeading();
    this.preferredWidth = from.getPreferredWidth();
  }

  public void setHeading(String heading) {
    if (heading == null) {
      throw new NullPointerException("ColumnProperty.setHeading: can't accept null heading");
    }
    this.heading = heading;
  }

  public String getHeading() {
    return this.heading;
  }

  public void setPreferredWidth(int prefWidth) {
    this.preferredWidth = prefWidth;
  }

  public int getPreferredWidth() {
    return preferredWidth;
  }

  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

}
