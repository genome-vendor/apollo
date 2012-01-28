package jalview.gui;

import jalview.datamodel.*;
import jalview.analysis.*;
import jalview.io.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class TreeFrame extends Frame {

  Object parent;
  TreePanel treePanel;

  public TreeFrame(Object parent,TreePanel p) {
      super("Jalview Tree");
      this.parent    = parent;
      this.treePanel = p;

      setLayout(new BorderLayout());
      add("Center",treePanel);
      addWindowListener(new TreeWindowListener());
  }

  public class TreeWindowListener extends WindowAdapter {
      public void windowClosing(WindowEvent evt) {
	  if (parent != null || !AlignFrame.exitOnClose()) {
	      TreeFrame.this.hide();
	      TreeFrame.this.dispose();
	  } else {
	      System.exit(0);
	  }
      }
  }
    public static void main(String[] args) {
	try {
	TreeFile tf = new TreeFile(args[0],"File");

	NJTree tree = tf.getTree();

	tree.printNode(tree.getTopNode());

	TreePanel treePanel = new TreePanel(null, tree);
	TreeFrame frame     = new TreeFrame(null,treePanel);

	frame.show();
	} catch (IOException e) {
	    System.out.println(e);
	}
    }
}





