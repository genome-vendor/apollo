package apollo.gui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.*;
import org.bdgp.io.*;
import org.bdgp.util.*;
import org.bdgp.swing.*;
import apollo.dataadapter.*;

public class ProgressFrame extends JFrame {

  protected ProgressListener progressListener;
  protected JProgressBar progressBar;
  protected VisualDataAdapter adapter;


  public ProgressFrame(JFrame parent, String title) {
    super(title);
    buildGUI();
    installListeners();
  }

  public ProgressFrame(JFrame parent,VisualDataAdapter adapter, String title) {
    this(parent,title);
    setAdapter(adapter);
  }



  public void installListeners() {
    try {
      progressListener = new ProgressBarProgressListener(progressBar);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public ProgressListener getListener() {
    return progressListener;
  }

  public void setAdapter(VisualDataAdapter adapter) {
    if (this.adapter != null) {
      this.adapter.removeProgressListener(progressListener);
    }
    this.adapter = adapter;
    adapter.addProgressListener(progressListener);
  }

  public void buildGUI() {
    progressBar = new JProgressBar();
    progressBar.setStringPainted(true);
    progressBar.setFont(getFont());
    progressBar.setPreferredSize(new Dimension(300,30));

    getContentPane().add(progressBar,BorderLayout.CENTER);
    progressBar.setStringPainted(true);
    progressBar.setString("Loading data...");
    progressBar.setValue(progressBar.getMinimum());

    pack();
    //setVisible(true);
  }

  class ProgressBarProgressListener implements ProgressListener {
    JProgressBar bar;

    public ProgressBarProgressListener(JProgressBar bar) {
      this.bar = bar;
    }

    public void progressMade(ProgressEvent e) {
      // System.out.println("progressMade called - value = " + e.getValue());
      String displayMe = null;
      if (e.getDescription() == null)
        bar.setStringPainted(false);
      else
        displayMe = e.getDescription();
      if (e.getValue() == null) {
        bar.setValue(bar.getMaximum());
        adapter.removeProgressListener(progressListener);
        ProgressFrame.this.hide();
        ProgressFrame.this.dispose();
      } else {
        int val = (int) e.getValue().doubleValue();
        bar.setValue(val);
        if (displayMe == null)
          displayMe = val+"%";
        else
          displayMe += " "+val+"%";
      }
      if (displayMe != null)
        bar.setString(displayMe);
    }
  }
}
