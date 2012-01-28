package apollo.gui;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;

import apollo.datamodel.*;
import apollo.dataadapter.DataLoadEvent;
import apollo.dataadapter.DataLoadListener;
import apollo.gui.genomemap.StrandedZoomableApolloPanel;
import apollo.gui.synteny.GuiCurationState;
import apollo.gui.event.*;
import apollo.seq.*;
import apollo.util.*;

import org.apache.log4j.*;

/**
 * A ControlledPanel which displays the find dialog 
 * (for name, base position and sequence).
 *
 * 10/2003:  David Goodstein added regular expression searching.  He mentioned the following:
 * 
 * 0) all searches are case insensitive
 * 1) if regular expression is NOT selected:
 * -it is always a prefix match
 * 2) if regular expression IS selected, it a "contains" match, meaning you should prepend "^" to your search string for a prefix match, append "$" to your search string for a suffix match, and sandwich between "^" and "$" for an exact match
 * 3)for NAME searches, even if regular expression is NOT selected, you can use "*" as a wildcard character (i convert it to ".*" internally)
 * 4) avoid nesting parenthesis on regexp SEQUENCE searches...the reverse-complementing of the regexp string will likely get funky.  I don't expect anyone will want to do this, however.
 * 5) 
 * i)on SEQUENCE searches, avoid like the plague constructs that can match an arbitrary number of times:
 *     atcg[atgc]+ggadd OR atcg.+ggadd
 * This will greedily grab for the longest sequence that is flanked by atcg and ggadd, so you'll likely get only 2 or 3 hits (it does not search within a region that has already been matched). You're better off with
 *     atcg[atgc]{1,25}ggadd
 * while will pull out all the matches flanked by atcg and ggadd that have at least one and up to 25 flanking characters
 * 6) for Regexp searches, no need to flank with wildcards, as these are "contains" searches.  For SEQUENCE searches, flanking with "^" and "$" doesn't really make sense.
 **/

public class FindPanel extends ControlledPanel implements ActionListener,
  ControlledObjectI, DataLoadListener {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(FindPanel.class);

  protected static Color color = new Color (255,255,204);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------
  
  JTextField    seqField;
  JTextField    nameField;
  JTextField    positionField;
  JLabel        seqLabel;
  JLabel        nameLabel;
  JLabel        positionLabel;
  JButton       seqButton;
  JButton       nameButton;
  JButton       positionButton;
  JButton       clearButton;
  JButton       cancelButton;
  JCheckBox     revStrand, regExp;
  BaseValidator seqValidator = new BaseValidator();
  //StrandedZoomableApolloPanel szap;
  GuiCurationState curationState;
  JTable        resultTable;
  JScrollPane   resultTableScroller;
  JPanel        mainPanel;
  JPanel        closePanel;
  Vector        columns;
  //Controller    controller;
  SeqFeatureI   sf;
  Vector        features = new Vector();
  int           currentFeatureType;

  private StatusPane resultsPane;

  //public FindPanel(StrandedZoomableApolloPanel szap,Controller c) {
  public FindPanel(GuiCurationState cs) {
    curationState = cs;
    setController(getController());
    //this.szap = szap;

    SequenceI seq = getSZAP().getAnnotations().getRefSequence();
    setSequence(seq,getSZAP().getCurationSet());

    componentInit();
  }

  private StrandedZoomableApolloPanel getSZAP() {
    return curationState.getSZAP();
  }

  private void setSequence(SequenceI seq, RangeI range) {
    if (range != null) {
      setSequence(seq,range.getStart(),range.getEnd());
    } else if (seq != null) {
      setSequence(seq,1,seq.getLength());
    }
  }

  private void setSequence(SequenceI seq,int start, int end) {
    sf = new SeqFeature(start,end,"sequence");
    sf.setRefSequence(seq);
  }

  public void setController(Controller c) {
    //controller = c;
    c.addListener(this);
  }

  public Controller getController() {
    return curationState.getController();
  }


  public void componentInit() {
    setBackground (color);

    mainPanel = new JPanel();
    mainPanel.setBackground (color);
    mainPanel.setForeground (Color.black);

    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    mainPanel.setPreferredSize(new Dimension(400, 130));

    positionLabel = new JLabel(" Position ");
    positionLabel.setForeground (Color.black);
    nameLabel     = new JLabel(" Name ");
    nameLabel.setForeground (Color.black);
    seqLabel      = new JLabel(" Sequence ");
    seqLabel.setForeground (Color.black);

    positionField = new JTextField();
    nameField     = new JTextField();
    seqField      = new JTextField();

    positionButton = new JButton("Goto");
    nameButton     = new JButton("Find");
    seqButton      = new JButton("Find");
    clearButton    = new JButton("Clear");
    cancelButton    = new JButton("Cancel");
    Dimension buttonSize = new Dimension(90,20);
    positionButton.setPreferredSize(buttonSize);
    nameButton.setPreferredSize(buttonSize);
    seqButton.setPreferredSize(buttonSize);
    clearButton.setPreferredSize(buttonSize);
    cancelButton.setPreferredSize(buttonSize);

    revStrand = new JCheckBox("Search reverse strand?");
    revStrand.setHorizontalAlignment(JLabel.RIGHT);
    revStrand.setBackground(color);

    regExp    = new JCheckBox("Use Regular Expressions?");
    regExp.setHorizontalAlignment(JLabel.RIGHT);
    regExp.setBackground(color);

    resultsPane    = new StatusPane(" Results ","", color);
    resultsPane.setBackground (color);
    resultsPane.setPreferredSize(new Dimension(400, 24));
    resultsPane.setFont(getFont());

    columns = new Vector();
    columns.addElement("Position");
    columns.addElement("Sequence");

    resultTableScroller = new JScrollPane();

    Box posBox = new Box(BoxLayout.X_AXIS);
    posBox.add(positionLabel);
    posBox.add(positionField);
    posBox.add(positionButton);
    posBox.add(posBox.createHorizontalGlue());

    Box nameBox = new Box(BoxLayout.X_AXIS);
    nameBox.add(nameLabel);
    nameBox.add(nameField);
    nameBox.add(nameButton);
    nameBox.add(nameBox.createHorizontalGlue());

    Box seqBox = new Box(BoxLayout.X_AXIS);
    seqBox.add(seqLabel);
    seqBox.add(seqField);
    seqBox.add(seqButton);
    seqBox.add(seqBox.createHorizontalGlue());

    mainPanel.add(posBox);
    mainPanel.add(nameBox);
    mainPanel.add(seqBox);
    mainPanel.add(revStrand);
    mainPanel.add(regExp);
    mainPanel.add(Box.createVerticalStrut(50));
    mainPanel.add(Box.createVerticalGlue());

    positionButton.addActionListener(this);
    nameButton.    addActionListener(this);
    clearButton.   addActionListener(this);
    cancelButton.  addActionListener(this);
    seqButton.     addActionListener(this);
    positionField. addActionListener(this);
    nameField.     addActionListener(this);
    seqField.      addActionListener(this);
    revStrand.     addActionListener(this);
    regExp.        addActionListener(this);

    positionField. addKeyListener(NumericKeyFilter.getFilter());
    seqField.      addKeyListener(seqValidator);

    boolean got_seq = getSZAP().haveSequence();
    seqField.   setEnabled(got_seq);
    seqButton.  setEnabled(got_seq);

    revStrand.setSelected(getSZAP().isReverseComplement());

    closePanel = new JPanel();
    closePanel.setBackground (color);
    closePanel.setForeground (Color.black);
    closePanel.setLayout(new BorderLayout());
    closePanel.setPreferredSize(new Dimension(400, 30));
    closePanel.add(clearButton, BorderLayout.WEST);
    closePanel.add(cancelButton, BorderLayout.EAST);

    //    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setLayout(new BorderLayout());
    add(mainPanel, BorderLayout.NORTH);
    add(resultsPane, BorderLayout.CENTER);
    //    mainPanel.add(Box.createVerticalStrut(50));
    add(closePanel, BorderLayout.SOUTH);
  }

  /**
   * Handles all buttons and fields, does name, position, and sequence searching
   */
  public void actionPerformed(ActionEvent evt) {

    // Name search
    if (evt.getSource() == nameButton ||
        evt.getSource() == nameField) {

      // Clear result table
      if (resultTable != null)
        resultTable.setVisible(false);

      // It isn't really necessary to clear these fields, is it?  A user
      // complained about that.
      //      seqField.setText("");
      //      positionField.setText("");

      // 0 is (extra) window to show around selected feature(s)
      boolean selectionFound = getSZAP().selectFeaturesByName(nameField.getText(), 0, regExp.isSelected());

      // lost the base info - is that problematic - if so will have to get that
      // from szap somehow
      if (selectionFound) {
        resultsPane.setText("Found " + nameField.getText());
      }
      else {
        resultsPane.setText("No features match pattern " + nameField.getText());
      }

      // Position search
    }
    else if (evt.getSource() == positionButton ||
             evt.getSource() == positionField) {
      if (!positionField.getText().equals("")) {
        // Clear result table
        if (resultTable!=null)
          resultTable.setVisible(false);
        // need to disable scroll as it brings back table - this doesnt do it though
        resultTableScroller.setEnabled(false);
        // That empties result table, but the panel is not getting small again.
        fireBaseFocusEvent(positionField.getText());
      }
      
      // Sequence search
    }
    else if (evt.getSource() == seqButton ||
             evt.getSource() == seqField) {
      findSequence();
    }
    else if (evt.getSource() == cancelButton) {
      getController().removeListener(this);
      Window win = SwingUtilities.windowForComponent(this);
      win.hide();
      win.dispose();
    }
    else if (evt.getSource() == clearButton) {
      if (resultTable != null)
        resultTable.setVisible(false);
      positionField.setText("");
      nameField.setText("");
      seqField.setText("");
    }
  }

  private void findSequence() {
    final String tmpString = seqField.getText().toUpperCase();
    if (tmpString.equals(""))  // don't search for empty string
      return;
    resultsPane.setText("Searching for sequence on " + (revStrand.isSelected() ? "reverse" : "forward") + " strand");
    SeqSelectorTable seqTab =
      new SeqSelectorTable(sf,tmpString,revStrand.isSelected(),
                           getSZAP(),resultsPane,regExp.isSelected());
    resultTable = seqTab.getTable();
    resultTableScroller.setViewportView(resultTable);
    resultTable.setVisible(true);
    mainPanel.setPreferredSize(new Dimension(400, 300));
    mainPanel.remove(resultTableScroller);
    mainPanel.add(resultTableScroller, BorderLayout.CENTER);
    mainPanel.validate();

    Window win = SwingUtilities.windowForComponent(resultTableScroller);
    win.validate();
    win.pack();
  }


  public boolean handleDataLoadEvent(DataLoadEvent evt) {
    getController().removeListener(this);
    Window win = SwingUtilities.windowForComponent(this);
    win.hide();
    win.dispose();
    /*
        mainPanel.setPreferredSize(new Dimension(400, 150));
        Window win = SwingUtilities.windowForComponent(resultTableScroller);
        mainPanel.remove(resultTableScroller);
        mainPanel.validate();
        win.validate();
        win.pack();
    */

    return true;
  }

  public void addNotify() {
    super.addNotify();
  }

  /** Doesnt need to check if its numeric because of NumericKeyFilter.
      6/2005: Actually, that's not always true--the NumericKeyFilter prevents
      you from typing non-numbers in the box, but if you copy and paste a
      string from somewhere else into the box, it lets you paste letters
      and then throws an exception on parseInt. */
  public void fireBaseFocusEvent(String pos) {
    try {
      int position = Integer.parseInt(pos);
      fireBaseFocusEvent(position);
    } catch (Exception e) {
      logger.error("Not an integer: " + pos);
    }
  }

  public void fireBaseFocusEvent(int pos) {
    BaseFocusEvent evt = new BaseFocusEvent(this, pos, new SeqFeature());
    getController().handleBaseFocusEvent(evt);
    resultsPane.setText("Centering on base " + positionField.getText());
  }

  class BaseValidator extends KeyAdapter {
    public void keyTyped(KeyEvent evt) {
      if(regExp.isSelected()){
	switch (evt.getKeyChar()) {
	case 'A':
	case 'a':
	case 'G':
	case 'g':
	case 'C':
	case 'c':
	case 'T':
	case 't':
	case 'N':
	case 'n':
	case '[':
	case ']':
	case '(':
	case ')':
	case '|':
	case '$':
	case '^':
	case '.':
	case '*':
	case '?':
	case '+':
	case '\\':
	case '{':
	case '}':
	case ',':
	case '0':
	case '1':
	case '2':
	case '3':
	case '4':
	case '5':
	case '6':
	case '7':
	case '8':
	case '9':
	  break;
	default:
	  if (!Character.isISOControl(evt.getKeyChar())) {
	    evt.consume();
	    _beep();
	  } else {}
	  break;
	}
      }else{
	switch (evt.getKeyChar()) {
	case 'A':
	case 'a':
	case 'G':
	case 'g':
	case 'C':
	case 'c':
	case 'T':
	case 't':
	case 'N':
	case 'n':
	  break;
	default:
	  if (!Character.isISOControl(evt.getKeyChar())) {
	    evt.consume();
	    _beep();
	  } else {}
	  break;
	}
      }
    }
  }

  // TODO - factor this out; I think there are other occurrences of the same method elsewhere
  private void _beep() {
      byte []  beep = new byte[1];
      beep[0] = 007;
      System.out.print(new String(beep));
  }

}
