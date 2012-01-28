package jalview.io;

import jalview.datamodel.*;
import jalview.analysis.*;
import jalview.gui.*;
import jalview.util.*;

import java.io.*;
import java.awt.*;
import java.util.*;

public class TreeFile extends FileParse {
  int  ch = 0;
  int line;

  NJTree tree;

  // These are needed to parse the tree
  int ycount = 0;
  SequenceNode maxdist;
  SequenceNode top;

  public TreeFile(SequenceNode sn) {
    top = sn;
  }

  public TreeFile(String inStr) {
    readLines(inStr);
    parse();
  }

  public TreeFile(String inFile, String type)  throws IOException {
    super(inFile,type);
    System.out.print("Reading file....");
    long tstart = System.currentTimeMillis();
    readLines();
    long tend = System.currentTimeMillis();
    System.out.println("done");
    System.out.println("Total time taken = " + (tend-tstart) + "ms");

    System.out.println("Parsing file....");
    parse();
    }

    public NJTree getTree() {
	return tree;
    }
    public void parse() {
    if (!((String)lineArray.elementAt(0)).equals("(")) {
      System.out.println("Invalid tree file: must start with (");
    } else {
      top = new SequenceNode();
      SequenceNode tmp = top;

      ycount = 0;

      createTree(tmp,getChar());
      tree = new NJTree(top);
      tree.setMaxDist(maxdist);

      System.out.println("Done creating tree " + top);
    }
  }

  public String getChar() {
    if (ch >= ((String)lineArray.elementAt(line)).length()) {
      line++;
      ch = 0;
    }

    String str = ((String)lineArray.elementAt(line)).substring(ch,ch+1);
    ch++;
    return str;
  }

  public String createTree(BinaryNode node,String str) {
    String name = "";
    String dist = "";

    if (str.equals("(")) {

      System.out.println("New left node on " + node);

      SequenceNode sn = new SequenceNode();
      sn  .setParent(node);
      node.setLeft(sn);

      str = createTree(sn,getChar());

      if (str.equals(",")) {

        System.out.println("New right node on " + node);
        SequenceNode sn2 = new SequenceNode();
        sn2 .setParent(node);
        node.setRight(sn2);

        str = createTree(sn2,getChar());

        // This means an unrooted tree - adding a parent to the top node
        if (str.equals(",")) {
          System.out.println("Here " + node.parent());
          System.out.println(((SequenceNode)node).dist + " " + ((SequenceNode)node.right()).dist);
          System.out.println(((SequenceNode)sn2).dist);
          //New node for top
          SequenceNode sn3 = new SequenceNode();
          top = sn3;
          sn3.setLeft(node);
          node.setParent(sn3);

          // dummy distance?
          ((SequenceNode)node).dist = (float).001;
          SequenceNode sn4 = new SequenceNode();
          sn4.setParent(sn3);
          sn3.setRight(sn4);
          createTree(sn4,getChar());
        }

        // Tot up the height
        SequenceNode l = (SequenceNode)node.left();
        SequenceNode r = (SequenceNode)node.right();

        ((SequenceNode)node).count = l.count + r.count;
        ((SequenceNode)node).ycount = (l.ycount + r.ycount)/2;
        str = getChar();
      }
    } else {
      // Leaf node
      while (!(str.equals(":") || str.equals(",") ||str.equals(")"))) {
        name = name + str;
        str = getChar();
      }

      node.setElement(new AlignSequence(name,"",0,0));
      node.setName(name);
      ((SequenceNode)node).count = 1;
      ((SequenceNode)node).ycount = ycount++;
    }

    // Read distance
    if (!str.equals(";")) {
      String bootstrap = "";
      while (!str.equals(":")) {
        bootstrap = bootstrap + str;
        str = getChar();
      }
      System.out.println("Boot :" + bootstrap + ":");
      if (!bootstrap.equals("")) {
        System.out.println("Setting bootstrap " + bootstrap);
        node.setBootstrap(Integer.parseInt(bootstrap));
        System.out.println("Getting bootstrap " + node.getBootstrap());
      }
      str=getChar();
      while (!(str.equals(":") || str.equals(",") ||str.equals(")"))) {
        dist = dist + str;
        str = getChar();
      }

      if (node instanceof SequenceNode) {
        ((SequenceNode)node).dist  = Float.valueOf(dist).floatValue();
        System.out.println("Distance = " + ((SequenceNode)node).dist);

        if ( maxdist == null || Float.valueOf(dist).floatValue() > maxdist.dist) {
	    maxdist = (SequenceNode)node;
        }

      }
    }
    return str;
  }

  public float max(float l, float r) {
    if (l > r) {
      return l;
    } else {
      return r;
    }
  }
    public static void main(String[] args) {
	try {
	TreeFile tf = new TreeFile(args[0],"File");
	NJTree tree = tf.getTree();
        tree.printNode(tree.getTopNode());

	TreePanel p = new TreePanel(null,tree);
	Frame f = new Frame("Tree : " + args[0]);
	f.setLayout(new BorderLayout());
	f.add("Center",p);
	f.setSize(500,500);
	f.show();

	} catch (IOException e) {
	    System.out.println("Exception parsing treefile " + args[0] + " " + e);
	}
    }
}




