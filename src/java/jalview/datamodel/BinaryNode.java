package jalview.datamodel;

public class BinaryNode {

  Object element;
  String name;
  BinaryNode left;
  BinaryNode right;
  BinaryNode parent;
  public int bootstrap;

  public BinaryNode() {
    left = right = parent = null;
  }

  public BinaryNode(Object element, BinaryNode parent,String name) {
    this.element = element;
    this.parent  = parent;
    this.name    = name;

    left=right=null;
  }

  public Object element() {
    return element;
  }

  public Object setElement(Object v) {
    return element=v;
  }

  public BinaryNode left() {
    return left;
  }

  public BinaryNode setLeft(BinaryNode n) {
    return left=n;
  }

  public BinaryNode right() {
    return right;
  }

  public BinaryNode setRight(BinaryNode n) {
    return right=n;
  }

  public BinaryNode parent() {
    return parent;
  }

  public BinaryNode setParent(BinaryNode n) {
    return parent=n;
  }

  public boolean isLeaf() {
    return (left == null) && (right == null);
  }

    public void setName(String name) {
	  this.name = name;
    }
    public String getName() {
	return this.name;
    }
	public void setBootstrap(int boot) {
    this.bootstrap = boot;
    System.out.println("Node bootstrap :" + bootstrap + ":");
	}
  public int getBootstrap() {
    System.out.println("Getting bootstrap :" + bootstrap + ":");
    return bootstrap;
  }
}

