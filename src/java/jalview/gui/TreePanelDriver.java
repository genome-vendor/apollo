package jalview.gui;

import jalview.analysis.*;
import jalview.io.*;
import jalview.datamodel.*;
import jalview.gui.popups.*;

import java.awt.*;
import java.util.*;

public class TreePanelDriver extends Driver  {
  public static void main(String[] args) {
    try {

      MSFfile msf = new MSFfile(args[0],"File");

//MG      Sequence[] s = new Sequence[msf.seqs.size()];

//MG      for (int i=0; i < msf.seqs.size();i++) {
//MG        s[i] = (Sequence)msf.seqs.elementAt(i);
//MG      }
      Frame tf = new Frame("Average distance tree");
//MG      TreePanel tp = new TreePanel(tf,s);
      tf.setLayout(new BorderLayout());
//MG      tf.add("Center",tp);
      tf.resize(500,500);
      tf.show();



    } catch (java.io.IOException e) {
      System.out.println("Exception : " + e);
    }
  }
}
