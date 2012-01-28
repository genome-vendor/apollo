package jalview.analysis;

import jalview.io.*;
import jalview.datamodel.*;

import java.util.*;
import java.awt.*;

public class NJTreeDriver { //MG extends Driver {

  public static void main(String[] args) {
    try {
      MSFfile msf = new MSFfile(args[0],"File");
//MG      Sequence[] s = new Sequence[msf.seqs.size()];
//MG      for (int i=0; i < msf.seqs.size();i++) {
//MG        s[i] = (Sequence)msf.seqs.elementAt(i);
//MG      }

//MG      NJTree njt = new NJTree(s,args[1],args[2]);

      Frame f = new Frame();
      f.setLayout(new BorderLayout());
      Panel p = new Panel();
      p.setLayout(new BorderLayout());
//MG      MyCanvas mc = new MyCanvas(njt.tf);
//MG      p.add("Center",mc);

      f.resize(600,600);
      f.add("Center",p);
      f.show();

//MG      njt.tf.reCount(njt.tf.top);
//MG      njt.tf.findHeight(njt.tf.top);

      //      System.out.println("Preorder");
//MG      njt.tf.printNode(njt.tf.top);
//MG      njt.tf.draw(p.getGraphics(),500,500);

//MG      njt.tf.groupNodes(njt.tf.top,Float.valueOf(args[3]).floatValue());

    } catch (java.io.IOException e) {
      System.out.println("Exception : " + e);
    }
  }
}
