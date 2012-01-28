package jalview.gui;

import jalview.datamodel.*;
import jalview.analysis.*;
import jalview.io.*;

import java.awt.*;
import java.io.*;

public class TreeFrameDriver  {

  public static void main(String[] args) {
    try {

      //MSFfile msf = new MSFfile(args[0],"File");
      //DrawableSequence[] s = new DrawableSequence[msf.getSeqs().size()];

      //for (int i=0; i < msf.getSeqs().size();i++) {
	    //    s[i] = new DrawableSequence((Sequence)msf.getSeqs().elementAt(i));
      //}

      //DrawableAlignment da = new DrawableAlignment(s);

      //Controller    c  = new Controller();
      //AlignViewport av = new AlignViewport(da,false,false,false,false);

      //NJTree tree1 = new NJTree(s,"AV","PID");
      TreeFile tf = new TreeFile("C:\\alex2.phb","File");

      NJTree tree2 = tf.getTree();

      //tree1.printNode(tree1.getTopNode());
      tree2.printNode(tree2.getTopNode());

      //TreePanel treePanel1 = new TreePanel(null, av,c,tree1);
      //TreeFrame frame1     = new TreeFrame(null,treePanel1);
      TreePanel treePanel2 = new TreePanel(null, tree2);
      TreeFrame frame2     = new TreeFrame(null,treePanel2);

      //frame1.setSize(500,500);
      frame2.setSize(500,500);

      //frame1.show();
      frame2.show();
	} catch (IOException e) {
	    System.out.println(e);
	}
    }

}
