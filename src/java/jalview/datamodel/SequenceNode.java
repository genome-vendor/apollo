package jalview.datamodel;

import java.awt.Color;

public class SequenceNode extends BinaryNode {

  public float dist;
  public int count;
  public float height;
  public float ycount;
  public Color color = Color.black;

  public SequenceNode() {
    super();
    if (jalview.gui.Config.DEBUG) System.out.println("Creating node");
  }
  
  public SequenceNode(Object val, SequenceNode parent, float dist,String name) {
    super(val,parent,name);
    this.dist = dist;
    System.out.println("Creating full node");
  }
}
