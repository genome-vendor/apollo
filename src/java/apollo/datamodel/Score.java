package apollo.datamodel;

import java.io.Serializable;

public class Score implements Serializable {

  String name;
  double value;

  public Score(String name, double value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public double getValue() {
    return value;
  }

  public void setValue (double value) {
    this.value = value;
  }

  public String toString() {
    return "score: "+name+" = "+value;
  }

}
