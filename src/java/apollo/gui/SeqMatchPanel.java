package apollo.gui;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import java.awt.event.*;
import java.awt.*;
import java.util.*;

import apollo.datamodel.*;
import apollo.gui.genomemap.StrandedZoomableApolloPanel;
import apollo.gui.drawable.DrawableSeqFeature;
import apollo.gui.event.*;
//import apollo.seq.ResidueProperties; --> DNAUtils

import org.bdgp.util.DNAUtils;

/**
 * A Panel to display sequence matches from the find panel.
 */
public class SeqMatchPanel extends ControlledPanel 
  implements ControlledObjectI {

  StrandedZoomableApolloPanel szap;
  boolean                     reverseComplement = false;
  Controller                  controller;
  JTable                      table;
  DefaultTableModel           model;
  String                      seqString    = "";
  String                      refSeqString = "";
  String                      searchString = "";
  JPanel                      slp;
  Vector                      tableData;

  public SeqMatchPanel(StrandedZoomableApolloPanel szap,
                       String searchString,
                       String seqString,
                       Controller c) {
    setController(c);
    this.szap = szap;
    if (szap.getAnnotations().getRefSequence() != null &&
        szap.getAnnotations().getRefSequence().getResidues() != null) {
      refSeqString = szap.getAnnotations().getRefSequence().getResidues();
      if (szap.isReverseComplement()) {
        // DNAUtils is the central place for seq stuff
        //this.refSeqString = ResidueProperties.reverseComplement(refSeqString);
        this.refSeqString = DNAUtils.reverseComplement(refSeqString);
        this.reverseComplement = true;
      }
    }
    this.searchString = searchString;
    tableData = findSeqMatches();
    componentInit();
  }

  public void setController(Controller c) {
    controller = c;
  }

  public Controller getController() {
    return controller;
  }

  public void componentInit() {

    setPreferredSize(new Dimension(100,300));
    setLayout(new BorderLayout());

    model = new SeqMatchModel();

    table = new JTable(model);
    table.setDefaultRenderer(Sequence.class, new SequenceRenderer());
    table.setDefaultEditor(Sequence.class, new DisabledEditor());
    table.setDefaultEditor(Object.class, new DisabledEditor());

    model.addColumn("Position");
    model.addColumn("Sequence");
    model.addColumn("Features");

    ListSelectionModel rsm = table.getSelectionModel();
    SeqMatchTableListener stl = new SeqMatchTableListener(rsm);
    rsm.addListSelectionListener(stl);

    table.addMouseListener(stl);

    JScrollPane sp = new JScrollPane(table);
    //slp = new SetListPanel(getController());
    slp = new JPanel();
    JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,false,sp,slp);
    split.setDividerLocation(150);
    add(split, BorderLayout.CENTER);

    setMinimumSize(new Dimension(100,150));

    populateTable();
  }

  class SeqMatchModel extends DefaultTableModel {
    public Class getColumnClass(int c) {
      if (c == 1) {
        return Sequence.class;
      } else {
        return super.getColumnClass(c);
      }
    }
  };

  class SeqMatchTableListener extends MouseAdapter
        implements ListSelectionListener,
    MouseListener {

    ListSelectionModel lsm;
    public SeqMatchTableListener(ListSelectionModel lsm) {
      this.lsm = lsm;
    }

    public void mouseClicked(MouseEvent evt) {
      if (evt.getClickCount() > 1) {
        // System.out.println("Got multiclick in table");
        //        int [] selIndices = getSelectedIndices(lsm.getMinSelectionIndex(),
        //                                               lsm.getMaxSelectionIndex());
        //fireBaseFocusEvent(getSelected(selIndices,true));
      }

    }

    public void valueChanged(ListSelectionEvent lse) {
      if (!lse.getValueIsAdjusting()) {
        //        int [] selIndices = getSelectedIndices(lsm.getMinSelectionIndex(),
        //                                               lsm.getMaxSelectionIndex());
        //slp.setSetAndSelection((FeatureSetI)getSelected(selIndices,false).getData(0));
      }

    }

    protected Selection getSelected(int [] selIndices, boolean expand) {
      Selection selection = new Selection();
      for (int i=0; i<selIndices.length; i++) {
        //SeqFeatureI sf = (SeqFeatureI)(tableData.elementAt(selIndices[i]));
        SeqFeatureI sf = null;
        if (sf instanceof DrawableSeqFeature) {
          selection.add(new SelectionItem(this,
                                          ((DrawableSeqFeature)sf).getFeature()));
        } else {
          if (sf.canHaveChildren() && expand) {
            FeatureSetI fs = (FeatureSetI)sf;
            for (int j=0;j<fs.size();j++) {
              selection.add(new SelectionItem(this,fs.getFeatureAt(j)));
            }
          } else {
            selection.add(new SelectionItem(this,sf));
          }
        }
      }
      return selection;
    }

    protected int [] getSelectedIndices(int start, int stop) {
      if ((start == -1) || (stop == -1)) {
        return new int[0];
      }

      int guesses[] = new int[stop - start + 1];
      int index = 0;

      for (int i=start; i<=stop; i++) {
        if (lsm.isSelectedIndex(i)) {
          guesses[index++] = i;
        }
      }
      int realthing[] = new int[index];
      System.arraycopy(guesses, 0, realthing, 0, index);
      return realthing;
    }
  }

  protected Vector findSeqMatches() {
    Vector matches = new Vector();

    int length = searchString.length();

    int seqPosition = 0;

    while ((seqPosition = refSeqString.indexOf(searchString,(int)seqPosition+1)) != -1) {
      SeqFeatureI match = new SeqFeature(seqPosition, seqPosition+length,
                                         "match" + seqPosition);
      matches.addElement(match);
    }
    return matches;
  }

  protected void populateTable() {
    //    int    nrow     = model.getRowCount();
    model.setNumRows(0);

    // Sort the features
    tableData = sortFeatures(tableData);

    for (int i=0;i<tableData.size();i++) {
      SeqFeatureI sf      = (SeqFeatureI)tableData.elementAt(i);

      model.addRow(new Object [] {  Long.toString(sf.getStart()),
                                    sf,
                                    sf });
    }

    if (tableData.size() > 0) {
      //   sdp.setSetAndSelection((FeatureSetI)tableData.elementAt(0));
    }

    // This appears to be needed with JDK 1.2.2 on Sun.


    repaint();

  }


  protected Vector sortFeatures(Vector unsorted) {
    return unsorted;
  }

  class SequenceRenderer extends JLabel implements TableCellRenderer {
    public SequenceRenderer() {
      super();
    }

    public Component getTableCellRendererComponent(JTable table,
        Object value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column) {
      System.out.println("Rendering sequence component");
      if (value == null) {
        setBackground(Color.white);
        setForeground(Color.black);
        setText("NO SEQ!!!!!");
        return this;
      }
      if (value instanceof SeqFeatureI) {
        String seqStr = ((SeqFeatureI)value).getResidues();
        if (seqStr != null) {
          setText(seqStr);
        } else {
          setText("NO SEQ!!!!");
        }
      }
      return this;
    }
  }

  public void fireBaseFocusEvent(String pos) {
    int position = Integer.parseInt(pos);
    fireBaseFocusEvent(position);
  }

  public void fireBaseFocusEvent(int pos) {
    BaseFocusEvent evt = new BaseFocusEvent(this,pos,new SeqFeature());
    controller.handleBaseFocusEvent(evt);
  }
}
