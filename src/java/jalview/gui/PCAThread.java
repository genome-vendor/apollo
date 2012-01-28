package jalview.gui;

import jalview.datamodel.*;
import jalview.analysis.PCA;
import jalview.gui.event.*;

import java.awt.*;
import java.util.*;

public class PCAThread extends Thread {
  AlignSequenceI[] s;
  Object parent;
  PCA pca;
  PCAPanel p;

  Controller controller;
  AlignViewport av;

  boolean calculated = false;

  public PCAThread(Object parent, AlignViewport av, Controller c,Vector seqs) {
    s = new AlignSequenceI[seqs.size()];
    for (int i=0; i<seqs.size(); i++) {
      s[i] = (AlignSequenceI)seqs.elementAt(i);
    }
    init(parent,av,c,s);
  }
  public PCAThread(Object parent, AlignViewport av, Controller c,AlignSequenceI[] s) {
    init(parent,av,c,s);
  }
  protected void init(Object parent, AlignViewport av, Controller c,AlignSequenceI[] s) {
    this.s = s;
    this.parent = parent;
    this.av = av;
    this.controller = c;
  }

  public void run() {
    pca = new PCA(s);
    pca.run();
    calculated = true;

    controller.handleStatusEvent(new StatusEvent(this,"Finished PCA calculation",StatusEvent.INFO));

    // Now find the component coordinates
    int ii=0;
    while (ii < s.length && s[ii] != null) {
      ii++;
    }

    double[][] comps = new double[ii][ii];

    for (int i=0; i < ii; i++ ) {
      if (pca.getEigenvalue(i) > 1e-4) {
        comps[i]  = pca.component(i);
      }
    }

    PCAFrame f = new PCAFrame("PCA results",parent);
    f.setLayout(new BorderLayout());
    p  = new PCAPanel(parent,av,controller,pca,s);
    f.add("Center",p);
    f.resize(400,400);

    f.show();
  }
}
