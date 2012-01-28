package jalview.gui;

import jalview.datamodel.*;
import jalview.analysis.*;

import java.util.*;
import java.awt.*;

public class TreeThread extends Thread {
  AlignSequenceI[] s;
  String type;
  String pwtype;
  Controller c;
  AlignViewport av;

  public TreeThread(Controller c,AlignViewport av,Vector vect, String type, String pwtype) {
      this.c = c;
      this.av = av;

    s = new AlignSequenceI[vect.size()];

    for (int i=0; i < vect.size(); i++) {
      s[i] = (AlignSequenceI)vect.elementAt(i);
    }

    init(s,type,pwtype);
  }

  public TreeThread(AlignSequenceI[] s, String type, String pwtype) {
    init(s,type,pwtype);
  }
  protected void init(AlignSequenceI[] s, String type, String pwtype) {
    this.s = s;
    this.type = type;
    this.pwtype = pwtype;
  }

  public void run() {
      NJTree tree = new NJTree(s,type,pwtype);

      if (Config.DEBUG) {
	System.out.println("Top node " + tree.getTopNode());
	System.out.println("Height " + tree.getMaxHeight());
	System.out.println("Dist " + tree.getMaxDist());
      }

      tree.printNode(tree.getTopNode());

      TreePanel treePanel = new TreePanel(null,av,c,tree);
      TreeFrame treeFrame = new TreeFrame(null,treePanel);

      treePanel.setParent(treeFrame);
      treeFrame.setLayout(new BorderLayout());
      treeFrame.add("Center",treePanel);

      treeFrame.setTitle("Jalview Tree");
      treeFrame.setSize(500,500);

      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

      treeFrame.setLocation((screenSize.width - treeFrame.getSize().width) / 2,
		  (screenSize.height - treeFrame.getSize().height) / 2);

      treeFrame.show();

  }
}



