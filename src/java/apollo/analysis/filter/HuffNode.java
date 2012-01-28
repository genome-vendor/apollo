package apollo.analysis.filter;

import java.util.*;

public class HuffNode {
  protected String word;
  protected int count;
  protected HuffNode parent = null;
  protected HuffNode child_0;
  protected HuffNode child_1;

  public HuffNode(String word)
  {
    this.word = word;
    this.count = 0;
  }

  public HuffNode (HuffNode child_0, HuffNode child_1) {
    this.child_0 = child_0;
    this.child_1 = child_1;
    this.count = child_0.getOccurance() + child_1.getOccurance();
    this.word = child_0.getWord() + child_1.getWord();
    child_0.setParent (this);
    child_1.setParent (this);
  }

  public String getWord() {
    return this.word;
  }

  public void addOccurance() {
    this.count++;
  }

  public int getOccurance () {
    return this.count;
  }

  public void setParent (HuffNode parent) {
    this.parent = parent;
  }

  public HuffNode getParent () {
    return (this.parent);
  }

  public HuffNode getChild_0 () {
    return (this.child_0);
  }

  public HuffNode getChild_1 () {
    return (this.child_1);
  }

}

