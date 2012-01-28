package apollo.gui.tweeker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.util.Random;
import java.util.Vector;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Window;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.ListCellRenderer;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import apollo.datamodel.SequenceI;
import apollo.datamodel.Range;
import apollo.datamodel.RangeI;
import apollo.datamodel.SeqFeature;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.CurationSet;
import apollo.gui.SequenceSelector;
import apollo.gui.Selection;
import apollo.gui.StatusPane;
import apollo.gui.drawable.Drawable;
import apollo.gui.drawable.DrawableSeqFeature;
import apollo.gui.event.BaseFocusEvent;
import apollo.gui.genomemap.StrandedZoomableApolloPanel;
import apollo.util.SeqFeatureUtil;
import apollo.util.FeatureList;
import apollo.gui.detailviewers.seqexport.SeqExport;
import apollo.gui.synteny.CurationManager;
import apollo.gui.synteny.GuiCurationState;

import org.apache.log4j.*;

/** selects restriction enzymes in active curation. I dont know if this is going too 
    far but the selector will actually work with whatever curation set has been made
    active, in other words if the user changes the active cur set the already existing
    selector will work with the newly active cur set. (SliderWindow doesnt do this
    so perhaps this makes the tweeker inconsistent.) */
class RestrictionEnzymeSelector extends JPanel implements ActionListener {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(RestrictionEnzymeSelector.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private Vector restrictionEnzymes = new Vector();
  private JList jList;
  private JButton digestButton = new JButton("Digest");
  private JButton seqButton = new JButton("Show selected restriction fragment sequences");
  private JButton clearButton = new JButton("Clear");
  private JLabel instrucs
    = new JLabel("Use the control key to select multiple restriction enzymes");
  private Drawable drawable;
  private Tweeker parent; // Component?
  private SiteSelectorTable siteTable;
  private JScrollPane siteTableScroller = new JScrollPane();

  RestrictionEnzymeSelector(Tweeker parent) { //SZAP szap,boolean revComp) {
    this.parent = parent;
    
    Dimension buttonSize = new Dimension(90,20);
    digestButton.setPreferredSize(buttonSize);
    digestButton.addActionListener(this);
    digestButton.setBackground(Color.white);
    seqButton.setPreferredSize(new Dimension(180, 20));
    seqButton.addActionListener(this);
    seqButton.setBackground(Color.white);
    seqButton.setEnabled(false);
    clearButton.setPreferredSize(buttonSize);
    clearButton.addActionListener(this);
    clearButton.setBackground(Color.white);
    Box buttonsBox = new Box(BoxLayout.X_AXIS);
    buttonsBox.add(Box.createHorizontalStrut(20));
    buttonsBox.add(digestButton);
    buttonsBox.add(Box.createHorizontalStrut(20));
    buttonsBox.add(clearButton);
    
    // This will default to allow multiple enzymes to be selected
    jList = new JList(getRestrictionEnzymes());
    jList.setCellRenderer(new RestrictionCell());

    JScrollPane scrollPane = new JScrollPane(jList);
    
    setBackground(Color.white);
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    add(instrucs);
    add(Box.createVerticalStrut(10));
    add(scrollPane);
    add(Box.createVerticalStrut(10));
    add(buttonsBox);
    add(Box.createVerticalStrut(10));
    add(siteTableScroller);
    add(Box.createVerticalStrut(10));
    add(seqButton);
    add(Box.createVerticalStrut(10));
  }

  //void setCurationState(CurationState cs) { curationState = cs; }
  private GuiCurationState getActiveCurState() {
    return CurationManager.getCurationManager().getActiveCurState();
  }
  private CurationSet getCurSet() { return getActiveCurState().getCurationSet(); }
  private StrandedZoomableApolloPanel getSZAP() { 
    return getActiveCurState().getSZAP(); 
  }
  private SequenceI getSeq() { return getCurSet().getRefSequence(); }

  public void actionPerformed(ActionEvent e) {
    boolean changed = false;
    if (e.getSource() == digestButton) {
      getSZAP().getScaleView().clearCutSites();
      enzymeSelected();
      if (siteTable != null) {
        siteTableScroller.setEnabled(true);
        siteTable.setVisible(true);
      }
      changed = true;
    }
    else if (e.getSource() == seqButton) {
      showFragments();
      changed = true;
    } 
    else if (e.getSource() == clearButton) {
      clearSites();
      changed = true;
    }
    if (changed) {
      parent.validate();
      parent.pack();
    }
  }

  private void clearSites() {
    jList.clearSelection();  // deselect everything in list
    seqButton.setEnabled(false);
    getSZAP().getScaleView().clearCutSites();
    if (siteTable != null) {
      siteTableScroller.setEnabled(false);
      siteTable.setVisible(false);
    }
  }

  /** Convenience method for adding to tweekers tabbed pane */
  void addToTabbedPane(JTabbedPane pane) {
    pane.addTab(getTitle(), null, this, getTip());
  }

  private String getTitle() {
    return "Restriction Enzymes";
  }

  private String getTip() {
    return "Search for restriction enzyme sites";
  }

  /** Parse restriction enzyme file into vector */
  private Vector getRestrictionEnzymes() {
    String root = System.getProperty("APOLLO_ROOT");
    File resEnzFile = new File(root+"/data/restriction_enzymes.dat");
    try {
      // would be nice to file in relative way
      // this might just work for jar files? thought it would work for 
      // classpath
      BufferedReader reader = new BufferedReader(new FileReader(resEnzFile));
      String line = reader.readLine();

      Random randomizer = new Random();

      while (line != null) {
        int spaceIndex = line.indexOf(" ");
        String resEnzymeName = line.substring(0,spaceIndex);
        String resEnzymeSequence = line.substring(spaceIndex+1);
        Color re_color = getNewColor(randomizer);
        RestrictionEnzyme re=new RestrictionEnzyme(resEnzymeName,
                                                   resEnzymeSequence,
                                                   getSeq(),
                                                   re_color);
        restrictionEnzymes.addElement(re);
        line = reader.readLine();
      }
    } catch (java.io.FileNotFoundException e) {
      logger.error("Can't find restriction enzyme file: "+
                   resEnzFile + "\n" + e, e);
    } catch (java.io.IOException e) {
      String m = "IOException occured while reading restriction enzyme file: ";
      logger.error(m+e, e);
    }
    return restrictionEnzymes;
  }

  private Color getNewColor(Random randomizer) {
    int red = 0;
    int green = 0;
    int blue = 0;
    boolean tooPale = true;
    // this is to try and ensure that the colors aren't too pale
    while (tooPale) {
      red = randomizer.nextInt(255);
      green = randomizer.nextInt(255);
      blue = randomizer.nextInt(255);
      if ((red + green + blue) <= 510)
        tooPale = false;
    }
    return (new Color(red, green, blue));
  }
    
  /** Enzyme selected in list - update SeqSelectorTable */
  private void enzymeSelected() {
    int [] ind = jList.getSelectedIndices();
    Vector all_positions = new Vector();
    /* we're going to build an array of arrays.
       The outer array has one entry per selected enzyme
       The inner array list every position. */
    for (int i = 0; i < ind.length; i++) {
      RestrictionEnzyme re 
        = (RestrictionEnzyme)restrictionEnzymes.elementAt(ind[i]);
      String cut_site = re.getCutSite();
      SequenceSelector ss = new SequenceSelector(getCurSet(),
                                                 cut_site,
                                                 false,
                                                 false);
      Vector forward_positions = ss.getZones();
      ss = new SequenceSelector(getCurSet(),
                                cut_site,
                                true,
                                false);
      Vector reverse_positions = ss.getZones();
      logger.info (re.toString() + " adding " +
                   (forward_positions.size()+reverse_positions.size()) +
                   " cut sites, color = " + re.getColor().toString());
      getSZAP().getScaleView().addCutSites(forward_positions, 
                                      reverse_positions,
                                      re.getColor());
      collectSites(re, forward_positions, all_positions, 1);
      collectSites(re, reverse_positions, all_positions, -1);
    }
    if (all_positions.size() > 0) {
      CutSiteModel model = new CutSiteModel();
      /* add one last CutSite to the end so that all fragments are
         accounted for */
      addTail(all_positions);
      model.setData(all_positions);
      if (siteTable == null) {
        siteTable = new SiteSelectorTable();
        siteTableScroller.setViewportView(siteTable);
      }
      // model must be set before renderer
      siteTable.setModel(model);
      // Something special for setting the background color
      TableColumn siteColumn = siteTable.getColumnModel().getColumn(0);
      CutSiteRenderer siteRenderer = new CutSiteRenderer();
      siteRenderer.setToolTipText("Click to center on this site");
      siteColumn.setCellRenderer(siteRenderer);
      
      TableColumn bpColumn = siteTable.getColumnModel().getColumn(2);
      DefaultTableCellRenderer bpRenderer = new DefaultTableCellRenderer();
      bpRenderer.setToolTipText("Upstream from this site to previous site");
      bpColumn.setCellRenderer(bpRenderer);

      initColumnSizes(siteTable);
      seqButton.setEnabled(true);
    }
    else { // No sites found
      seqButton.setEnabled(false);
      clearSites();
      siteTable = null;
    }
  }

  /** Clear up dangling refs */
  void clear() {
    getSZAP().getScaleView().clearCutSites();
  }

  protected void showFragments() {
    CutSiteModel model = (CutSiteModel) siteTable.getModel();
    Vector cutSites = model.getData();
    Vector fragments = new Vector();
    int rows = cutSites.size();
    for (int i = 0; i < rows; i++) {
      CutSite site = (CutSite) cutSites.elementAt(i);
      if (site.exportFragment()) {
        int high;
        int low;
        String name;
        if (i == 0) {
          high = site.getHigh();
          low = getCurSet().getLow();
          name = "Upstream_"+site.getRestrictionEnzyme().getName();
        }
        else {
          CutSite preceding = (CutSite) cutSites.elementAt(i - 1);
          high = site.getHigh();
          low = preceding.getLow();
          if (site.getRestrictionEnzyme() != null)
            name = (preceding.getRestrictionEnzyme().getName()+ "_" +
                    site.getRestrictionEnzyme().getName());
          else
            name = (preceding.getRestrictionEnzyme().getName()+ "_" +
                    "Downstream");
        }
        SeqFeatureI frag = new SeqFeature(low, high, 
                                          "restriction_fragment", 1);
        frag.setName(name);
        frag.setRefSequence(getSeq());
        fragments.addElement(frag);
      }
    }
    if (fragments.size() > 0) {
      Selection selection = new Selection();
      selection.add(new FeatureList(fragments), this);
      new SeqExport(selection,getActiveCurState().getController());
    }
  }

  protected void collectSites(RestrictionEnzyme re, 
                              Vector new_positions,
                              Vector all_positions,
                              int strand) {
    int match_cnt = new_positions.size();
    for (int i = 0; i < match_cnt; i++) {
      int [] match_positions = (int []) new_positions.elementAt(i);
      /* This is a hack--to highlight the sequence selection,
         a bogus sequence feature of type "String" is created.
         Since there is no type "String" defined in tiers.dat,
         the sequence selection is colored the default feature color,
         which used to be green but is now pale gray.
         To get around this, I'm defining a type "Sequence selection"
         in tiers.dat. This is also a hack, and should be removed
         from tiers.dat when we come up with a better way of doing
         sequence selection.
      */
      /*
        Don't add this match if it is palindromic, just 
        indicate that it is */
      int cut_count = all_positions.size();
      boolean palindrome = false;
      // since these are sorted, could exit the loop earlier for speed
      // (later, pressed for time)
      for (int j = 0; j < cut_count && !palindrome; j++) {
        CutSite cutsite = (CutSite) all_positions.elementAt(j);
        palindrome = (match_positions[0] == cutsite.getLow() &&
                      match_positions[1] == cutsite.getHigh());
      }
      if (!palindrome) {
        CutSite match = (strand == 1 ?
                         new CutSite(match_positions[0],
                                     match_positions[1],
                                     re) :
                         new CutSite(match_positions[1],
                                     match_positions[0],
                                     re));
        match.setRefSequence(re.getRefSequence());
        all_positions.add(match);
        SeqFeatureUtil.sort(all_positions, 1);
      }
    }
  }

  /* add one last CutSite to the end so that all fragments are
     accounted for */
  private void addTail(Vector all_positions) {
    all_positions.addElement(new CutSite(getCurSet().getEnd(),
                                         getCurSet().getEnd(), 
                                         null));
  }

  /*
   * This method picks good column sizes.
   * If all column heads are wider than the column's cells'
   * contents, then you can just use column.sizeWidthToFit().
   */
  private void initColumnSizes(JTable table) {
    CutSiteModel model = (CutSiteModel)table.getModel();
    TableColumn column = null;
    Component comp = null;
    int headerWidth = 0;
    int cellWidth = 0;
    Object[] longValues = model.longValues();
    TableCellRenderer headerRenderer =
      table.getTableHeader().getDefaultRenderer();

    for (int i = 0; i < model.getColumnCount(); i++) {
      column = table.getColumnModel().getColumn(i);
      comp 
        = headerRenderer.getTableCellRendererComponent(null, 
                                                       column.getHeaderValue(),
                                                       false, false, 0, 0);
      headerWidth = comp.getPreferredSize().width;
      
      comp = table.getDefaultRenderer(model.getColumnClass(i)).
        getTableCellRendererComponent(
                                      table, longValues[i],
                                      false, false, 0, i);
      cellWidth = comp.getPreferredSize().width;

      //XXX: Before Swing 1.1 Beta 2, use setMinWidth instead.
      column.setPreferredWidth(Math.max(headerWidth, cellWidth));
    }
  }


  protected class RestrictionCell extends JLabel implements ListCellRenderer {
    
    public Component getListCellRendererComponent(JList list,
                                                  Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
      setText(value.toString());
      setForeground(((RestrictionEnzyme) value).getColor());
      if (isSelected)
        setBackground(list.getSelectionBackground());
      else
        setBackground(list.getBackground());
      setEnabled(list.isEnabled());
      setFont(list.getFont());
      setOpaque(true);
      return this;
    }
  }

  /** CallsSeqSelector on separate thread and populates table with
      results */
  protected class SiteSelectorTable extends JTable {
    
    SiteSelectorTable() {
      // Currently doesnt deal with multiselect - might be nice if did
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      getSelectionModel().addListSelectionListener(new TableSelector());
    }

    private void fireBaseFocusEvent(String posString) {
      int pos = Integer.parseInt(posString);
      //if (revComp)
      //  pos = pos - searchSeqString.length();
      BaseFocusEvent evt = new BaseFocusEvent(this, pos, new SeqFeature());
      getActiveCurState().getController().handleBaseFocusEvent(evt);
    }
  
    /** inner inner class 
        modified to extract positions from strings in form "(\d+)-(\d+)" */
    private class TableSelector implements ListSelectionListener {
      public void valueChanged(ListSelectionEvent e) {
        int row = getSelectedRow();
        if (row >= 0 && !e.getValueIsAdjusting() ) {
          String position = (String) getValueAt(row, 1);
          int index = position.indexOf("-");
          fireBaseFocusEvent(position.substring(0, index));

          CutSiteModel model = (CutSiteModel) siteTable.getModel();
          Vector cutSites = model.getData();
          CutSite site = (CutSite) cutSites.elementAt(row);
          int high;
          int low;
          int strand = 1;
          if (row == 0) {
            high = site.getHigh();
            low = getCurSet().getLow();
          }
          else {
            CutSite preceding = (CutSite) cutSites.elementAt(row - 1);
            high = site.getHigh();
            low = preceding.getLow();
            strand = (site.getStart() > site.getEnd()) ? 1 : -1;  // ?
          }
          SeqFeatureI frag = new SeqFeature(low, high, 
                                            "Sequence selection", 1);
          getActiveCurState().getSelectionManager().select(frag,this);
          // zoom in on new selection
          // If fragment is >100, we won't be able to see the bases in the window,
          // so zoom to the beginning of it and show 100 bases
          int maxWindow = 100;
          if (high-low > maxWindow) {
            // zoomToSelectionWithWindow wants TOTAL to be added to range,
            // so if you want, say, +/-5000, you'd use a window of 10000.
            // That's why we're not dividing by 2, as you might otherwise expect.
            int subtract = -1*((high-low) - maxWindow);
            // Set center base so that it's near the left side of the window but not too far left
            int centerBase = low + maxWindow/2;

            if (logger.isDebugEnabled()) {
              int fragSize = high-low;
              int ww = high-low+(2*subtract);
              logger.debug("low = " + low + ", high-low = " + fragSize + ", subtract = " + 
                           subtract + ", center = " + centerBase + ", total window width = " + ww);
            }
            getSZAP().zoomToSelectionWithWindow(subtract, centerBase);
          }
          else
            getSZAP().zoomToSelection();
        }
      }
    }
  }

  /**
   * This is the TableModel for the table 
   * Takes a Vector of SequenceMatch in setData.
   * Each SequenceMatch represents a row
   make outer class? its kinda big
   */
  protected class CutSiteModel extends AbstractTableModel {
    private Vector data;
    private Vector columns = new Vector();

    protected CutSiteModel() {
      columns.addElement("Enzyme");
      columns.addElement("Restriction site position");
      columns.addElement("Fragment position");
      columns.addElement("Fragment length");
      columns.addElement("Select");
    }

    protected void setData (Vector data) {
      this.data = data;
    }
    
    protected Vector getData () {
      return this.data;
    }
    
    public int getRowCount() {
      return data.size();
    }
    
    public int getColumnCount() {
      return columns.size();
    }

    public Object[] longValues() {
      Object [] longestValues = new Object [getColumnCount()];
      for (int col = 0; col < columns.size() - 1; col++) {
        longestValues[col]
          = (col == 0 ? 
             ((RestrictionEnzyme) getValueAt(0, col)).toString() :
             (String) getValueAt(0, col));
        for (int row = 1; row < data.size(); row++) {
          String obj = (col == 0 ? 
                        ((RestrictionEnzyme) getValueAt(0, col)).toString() :
                        (String) getValueAt(row, col));
          if (obj.length() > ((String) longestValues[col]).length())
            longestValues[col] = obj;
        }
      }
      longestValues[getColumnCount()-1] = Boolean.TRUE;
      return longestValues;
    }

    public String getColumnName(int column) {
      return (String) columns.elementAt(column);
    }

    public Object getValueAt(int row, int column) {
      CutSite site = (CutSite) data.elementAt(row);
      switch (column) {
      case 0:
        return site.getRestrictionEnzyme();
      case 1:  // Restriction site position
        return site.getStart() + "-" + site.getEnd();
      case 2:  // Fragment position
        CutSite preceding = (row > 0 ?
                             (CutSite) data.elementAt(row - 1) : null);
        // Lengths are inclusive of the cut sites themselves
        return (preceding == null ?
                getCurSet().getLow() + "-" + site.getHigh() :
                preceding.getLow() + "-" + site.getHigh());
      case 3:  // Fragment length
        preceding = (row > 0 ?
                     (CutSite) data.elementAt(row - 1) : null);
        // Lengths are inclusive of the cut sites themselves
        return (preceding == null ? 
                // The inexplicable ""+ is an easy way to cast the number to a string
                "" + (site.getHigh() - getCurSet().getLow() + 1) :
                "" + (site.getHigh() - preceding.getLow() + 1));
      case 4:  // Select to see sequence
        return new Boolean(site.exportFragment());
      }
      return "";
    }

    /*
     * JTable uses this method to determine the default renderer/
     * editor for each cell.  If we didn't implement this method,
     * then the last column would contain text ("true"/"false"),
     * rather than a check box.
     */
    public Class getColumnClass(int c) {
      return getValueAt(0, c).getClass();
    }

    /* The last column can be changed by the user */
    public boolean isCellEditable(int row, int col) {
      return (col == getColumnCount()-1);
    }

    /*
     * If the fragment should be exported
     */
    public void setValueAt(Object value, int row, int col) {
      if (col == getColumnCount()-1) {
        CutSite site = (CutSite) data.elementAt(row);
        site.exportFragment(((Boolean)value).booleanValue());
        fireTableCellUpdated(row, col);
      }
    }

  } // end CutSiteModel inner class
  
  protected static class CutSiteRenderer extends DefaultTableCellRenderer {
    
    public CutSiteRenderer() {
      super();
    }

    public void setValue(Object value) {
      if (value != null) {
        RestrictionEnzyme re = (RestrictionEnzyme) value;
        setForeground(Color.white);
        setBackground(re.getColor());
        setText(re.toString());
      } else {
        setForeground(Color.black);
        setBackground(Color.white);
        setText("Downstream");
      }
    }
  }

}
