package jalview.gui;

import jalview.analysis.*;
import jalview.io.*;

import java.awt.*;
import java.awt.event.*;

public class ProgressFrame extends Frame {
  Object              parent;
  ProgressPanel       pp;
  TextArea            ta;
  TextAreaPrintStream taps;
  ProgressWindowListener pal;

  public ProgressFrame(String title,Object parent,Thread th) {
    super(title);
    this.parent = parent;
    pp = new ProgressPanel(this,th);
    setLayout(new GridLayout(2,1));
    ta = new TextArea(50,20);
    taps = new TextAreaPrintStream(System.out,ta);
    resize(400,500);
    add(pp);
    add(ta);
    pal = new ProgressWindowListener();
    addWindowListener(pal);
  }

  public TextArea getTextArea() {
    return ta;
  }

  public void setCommandThread(Thread t) {
    pp.setThread(t);
  }

  public Thread createProgressThread() {
    return new Thread(pp);
  }

  class ProgressWindowListener extends WindowAdapter {
    public void windowClosing(WindowEvent evt) {
      if (pp.ct instanceof CommandThread) {
        pp.status.setText("Destroying process");
        ((CommandThread)pp.ct).getProcess().destroy();
      }
      if (parent != null || !AlignFrame.exitOnClose()) {
        ProgressFrame.this.hide();
        ProgressFrame.this.dispose();
      } else if (parent == null) {
        System.exit(0);
      }
    }
  }
}
