/*
 * SeqAnalysisGUI
 *
 */

package apollo.analysis;

/**
 * GUI class for all SeqAnalysisI conforming implementations
 *
 * This is a panel to be presented to the user to allow them
 * to set various parameters which are pertinent to the analysis
 * they wish to run.
 *
 * Depending on the SeqAnalysisI implementing class, this
 * could involve detailed specification of local system file
 * paths, or just giving the name of a dataset (eg nr)
 *
 * -----------------------------
 *
 * This conforms to org.bdgp.io.DataAdapterUI in order to
 * use a DataAdapterChooser object to help build a GUI frame
 * for running analyses.
 *
 * It differs from a standard DataAdapter in that a SeqAnalysisI
 * object is stateful, and in that there is only one GUI class
 * for all objects that implement SeqAnalysisI.
 * The idea here is to allow for dynamic run time extensibility
 * purely through "soft" configuration (SeqAnalysisI allows detailed
 * meta level introspection through the operations detailed in its
 * interface)
 *
 * @see TestUI
 * @see DataAdapter
 * @see DataAdapterUI
 *
 * @author Chris Mungall
 *
 **/


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.File;

import apollo.main.Apollo;
import apollo.datamodel.*;

import org.bdgp.io.*;

public class SeqAnalysisGUI
  extends JPanel implements DataAdapterUI {

  public static final int MAX_HISTORY_LENGTH = 5;

  protected SeqAnalysisI seqAnalysis;
  protected IOOperation op;

  JComboBox pathList;
  JLabel typeLabel;
  Vector filePaths;
  JButton browseButton;
  Properties props;
  Hashtable jboxes;


  public SeqAnalysisGUI(SeqAnalysisBase seqAnalysis,
                        IOOperation op) {
    this.op = op;
    setPreferredSize(new Dimension(400,400));
    typeLabel = new JLabel("File type not set");
    setDataAdapter(seqAnalysis);
    buildGUI();
    attachListeners();
    System.out.println("seqA = "+seqAnalysis);
    //	validate();
  }

  protected class LaunchRunnable implements Runnable {

    public void run() {
      Apollo.setLog4JDiagnosticContext();
      yo();
      Apollo.clearLog4JDiagnosticContext();
    }
  }

  public void yo() {
    if (seqAnalysis.launch()) {
      System.out.println("Launched analysis!");
      while (!seqAnalysis.isFinished()) {
        System.out.println("waiting....");
        //	    System.sleep(3);
      }
      if (seqAnalysis.hasResults()) {
        CurationSet cs = seqAnalysis.getCurationSet();
        String raw = seqAnalysis.getAllRawResults();
        System.out.println("Output = " + raw);

        JFrame jf  =
          new JFrame("Analysis Results");
        JTextArea ta = new JTextArea(40, 85);
        ta.setFont(new Font("Courier", 0, 12));
        ta.setText(raw);
        ta.setWrapStyleWord(true);
        ta.setLineWrap(true);
        jf.getContentPane().add(new JScrollPane(ta));
        jf.pack();
        jf.show();
      }
    } else {}
  }

  public Object doOperation(Object values) throws DataAdapterException {
    System.out.println("Doing op "+values);

    Vector proplist = ((SeqAnalysisI)seqAnalysis).getAllowedProperties();
    for (int i=0; i < proplist.size(); i++) {
      String p = (String)proplist.elementAt(i);
      JComboBox jbox = (JComboBox)jboxes.get(p);
      String item = (String)jbox.getSelectedItem();
      System.out.println(p+" selected "+item);
      seqAnalysis.addProperty(p, item);
    }

    LaunchRunnable runnable = new LaunchRunnable();
    Thread launcher = new Thread(runnable);
    launcher.start();

    return null;
  }

  public void buildGUI() {
    jboxes = new Hashtable();
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    add(typeLabel);

    JPanel seqPanel = new JPanel();
    seqPanel.setLayout(new BoxLayout(seqPanel, BoxLayout.X_AXIS));
    JComboBox seqBox = new JComboBox();
    Vector v2 = new Vector();
    SequenceI sequence =
      seqAnalysis.getInputSequence();
    if (sequence != null) {
      v2.addElement(sequence.getName());
    } else {
      v2.addElement("unknown seq for "+seqAnalysis);
    }
    JLabel seqLabel = new JLabel("Input Sequence");
    add(seqLabel);
    seqBox.setEditable(true);
    seqBox.setModel(new DefaultComboBoxModel(v2));
    seqPanel.add(seqBox);
    add(seqPanel);

    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    //panel.add(Box.createHorizontalGlue());
    //	add("South", panel);
    add(panel);

    System.out.println("seqAnalysis = " + (SeqAnalysisI)seqAnalysis);
    System.out.println("seqAnalysis = " + ((SeqAnalysisI)seqAnalysis).getAllowedProperties());

    // now iterate through all the configurable/allowed
    // properties adding a config panel for each
    Vector proplist = ((SeqAnalysisI)seqAnalysis).getAllowedProperties();
    for (int i=0; i < proplist.size(); i++) {
      String p = (String)proplist.elementAt(i);
      String type = seqAnalysis.getPropertyType(p);
      final String desc = seqAnalysis.getPropertyDescription(p);

      JLabel pLabel;
      pLabel = new JLabel(p);
      add(pLabel);

      // Make a new panel for putting
      // everything pertinent to this parameter, p
      JPanel ppanel = new JPanel();
      ppanel.setLayout(new BoxLayout(ppanel, BoxLayout.X_AXIS));

      // if the property has a description
      // then we can add a popup help
      if (desc != null) {
        JButton help = new JButton("?");
        help.setFont(getFont());
        ppanel.add(help);
        SeqAnalysisGUI me = this;
        try {
          help.addActionListener(new ActionListener() {
                                   public void actionPerformed(ActionEvent evt) {
                                     System.out.println("DESC="+desc);
                                     JFrame jf  =
                                       new JFrame("Property Description");
                                     JTextArea ta = new JTextArea(10, 85);
                                     ta.setFont(new Font("Courier", 0, 12));
                                     ta.setText(desc);
                                     ta.setWrapStyleWord(true);
                                     ta.setLineWrap(true);
                                     jf.getContentPane().add(new JScrollPane(ta));
                                     jf.pack();
                                     jf.show();
                                   }
                                 }
                                );

        } catch (Exception e) {
          e.printStackTrace();
        }

      }

      // relevant data from user goes in jbox
      JComboBox jbox = new JComboBox();
      jbox.setEditable(true);
      Vector v = ((SeqAnalysisI)seqAnalysis).getAllowedValues(p);
      if (v == null) {
        v = new Vector();
      }
      jbox.setModel(new DefaultComboBoxModel(v));
      ppanel.add(jbox);

      // add a browse button if the parameter
      // type is a unix / system path
      if (type != null &&
          type.equals("path")) {
        JButton browse = new JButton("Browse...");
        browse.setFont(getFont());
        ppanel.add(browse);
        SeqAnalysisGUI me = this;
        try {
          browse.addActionListener(new ActionListener() {
                                     public void actionPerformed(ActionEvent evt) {
                                       // do something
                                     }
                                   }
                                  );

        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      add(ppanel);
      jboxes.put(p, jbox);
    }
  }

  public void attachListeners() {}

  public void setDataAdapter(DataAdapter seqAnalysis) {
    this.seqAnalysis = (SeqAnalysisI)seqAnalysis;
    typeLabel.setText(seqAnalysis.getType());
  }

  public void setInput (Object o) {}

  public void setProperties(Properties in) {
    props = in;
    filePaths = new Vector();
    if (props == null) {
      props = new Properties();
    }
    String historyItems = props.getProperty("historyItems");
    try {
      int items = Integer.parseInt(historyItems);
      for(int i=0; i < items; i++) {
        filePaths.addElement(props.getProperty("historyItem"+i));
      }
    } catch (NumberFormatException e) {
      System.out.println("Can't parse " + historyItems);
    }
  }

  public Properties getProperties() {

    Properties out = new Properties();
    if (filePaths.size() > MAX_HISTORY_LENGTH)
      out.setProperty("historyItems", MAX_HISTORY_LENGTH+"");
    else
      out.setProperty("historyItems", filePaths.size()+"");
    for(int i=0; i < filePaths.size() && i < MAX_HISTORY_LENGTH; i++) {
      out.setProperty("historyItem"+i, (String) filePaths.elementAt(i));
    }
    return out;
  }

  public boolean validOperation () throws DataAdapterException {
    return true;
  }

}
