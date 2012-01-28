package apollo.gui.evidencepanel;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.util.*;
import java.lang.Math;
import java.beans.*;

import apollo.datamodel.*;
import apollo.gui.ApolloFrame;
import apollo.config.Config;
import apollo.config.ColumnProperty;
import apollo.gui.ControlledPanel;
import apollo.gui.Controller;
import apollo.gui.DetailInfo;
import apollo.gui.Selection;
import apollo.gui.SelectionItem;
import apollo.gui.SelectionManager;
import apollo.gui.drawable.DrawableSeqFeature;
import apollo.gui.event.*;
import apollo.util.*;
import apollo.util.ClipboardUtil;

import org.bdgp.util.VectorUtil;
import org.bdgp.util.*;

import misc.JIniFile;

import org.apache.log4j.*;


/**
 * A panel to show a table of the individual features in a single feature set.
 * (isa ControlledPanel isa JPanel) should this be abstract - super class is not
 * actually instanitiated just sub classes
 */
public class TablePanel extends ControlledPanel {
  private static final Logger      logger = LogManager.getLogger(Config.class);

  Controller     controller = null;
  EvidenceTable  table;
  JScrollPane    pane;
  FilteredFeatureModel model; // inner class
  //Vector         tableData; // dont think this is used
  private SelectionManager selectionManager;

  private boolean listSelectionComesFromSelf = false;
  /** Made it an instance variable as it is a source of selection events that
   * needs to be tested for */
  private RowSelectionListener rowSelectionListener;


  protected class MegaComparator implements org.bdgp.util.Comparator {

    protected String sortKey;

    public MegaComparator(String sortKey) {
      this.sortKey = sortKey;
    }

    public int compare(Object fa, Object fb) {
      if (sortKey == null)
        return org.bdgp.util.Comparator.EQUAL_TO;
      SeqFeatureI feature_a = (SeqFeatureI) fa;
      SeqFeatureI feature_b = (SeqFeatureI) fb;
      Object a = DetailInfo.getPropertyForFeature(sortKey, feature_a);
      Object b = DetailInfo.getPropertyForFeature(sortKey, feature_b);
      if (a instanceof String && b instanceof String)
        return compareStrings((String) a, (String) b);
      else if (a instanceof org.bdgp.util.Range && b instanceof org.bdgp.util.Range)
        return compareRanges((org.bdgp.util.Range) a, (org.bdgp.util.Range) b);
      else if (a instanceof Double && b instanceof Double)
        return compareDoubles((Double) a, (Double) b);
      else if (a instanceof Integer && b instanceof Integer)
        return compareIntegers((Integer) a, (Integer) b);
      else
        return compareStrings(a.toString(), b.toString());
    }

    public int compareDoubles(Double a, Double b) {
      double thisVal = a.doubleValue();
      double anotherVal = b.doubleValue();

      if (thisVal < anotherVal)
        return -1;           // Neither val is NaN, thisVal is smaller
      if (thisVal > anotherVal)
        return 1;            // Neither val is NaN, thisVal is larger

      long thisBits = Double.doubleToLongBits(thisVal);
      long anotherBits = Double.doubleToLongBits(anotherVal);

      return (thisBits == anotherBits ?  0 : // Values are equal
              (thisBits < anotherBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
               1));                          // (0.0, -0.0) or (NaN, !NaN)
    }

    public int compareIntegers(Integer a, Integer b) {
      int thisVal = a.intValue();
      int anotherVal = b.intValue();
      return (thisVal<anotherVal ? -1 : (thisVal==anotherVal ? 0 : 1));
    }

    public int compareRanges(org.bdgp.util.Range a, org.bdgp.util.Range b) {
      if (a.getLow() < b.getLow())
        return -1;
      else if (a.getLow() > b.getLow())
        return 1;
      else
        return 0;
    }

    public int compareStrings(String a, String b) {
      int comparison = a.compareTo(b);
      if (comparison < 0)
        comparison = ComparisonConstants.LESS_THAN;
      else if (comparison > 0)
        comparison = ComparisonConstants.GREATER_THAN;
      else
        comparison = ComparisonConstants.EQUAL_TO;
      return comparison;
    }
  }

  protected class SortMouseListener extends MouseInputAdapter {

    public void mouseClicked(MouseEvent e) {
      TableColumnModel columnModel = table.getColumnModel();
      int viewColumn = columnModel.getColumnIndexAtX(e.getX());
      int column = table.convertColumnIndexToModel(viewColumn);
      //      int row = table.rowAtPoint(new Point(e.getX(), e.getY()));
      if (e.getClickCount() == 1 && column != -1) {
        int shiftPressedInt = e.getModifiers()&InputEvent.SHIFT_MASK;
        boolean shiftPressed = (shiftPressedInt != 0);
        // If shift pressed make descending
        boolean descending = shiftPressed;
        // if column default sorting is descennding then reverse it
        if (model.defaultSortingIsDescending(column))
          descending = !descending;
        model.setSortKey(column, descending);
        
        ColumnProperty columnProperty = (ColumnProperty)model.getPropertyList().elementAt(column);
        saveSortKey(model.getType(),columnProperty.getHeading(),descending);
        table.requestFocus();
      }
    }
  }

  protected class CopyMouseListener extends MouseAdapter {
    public void mouseClicked(MouseEvent e) {
      if (MouseButtonEvent.isRightMouseClick(e)) {
        Point p = new Point(e.getX(), e.getY());
        int col = table.columnAtPoint(p);
        int row = table.rowAtPoint(p);
        ClipboardUtil.copyTextToClipboard(table.getValueAt(row,col).toString());
      }
    }
  }

  protected class RowSelectionListener implements ListSelectionListener {
    ListSelectionModel lsm;

    public RowSelectionListener(ListSelectionModel lsm) {
      this.lsm = lsm;
    }

    public void valueChanged(ListSelectionEvent e) {
      // If we were called due to internally changing the list selection
      // (like in response to external selection) do not send out a selection
      // event. Do we need to call setToFeature? if so do in selectFeature()
      if (listSelectionComesFromSelf)
        return;
      int row = table.getSelectedRow();
      if (row != -1) {
        SeqFeatureI feature = model.getRow(row);
        //Selection selected = getSelected(feature);
        if (feature!=null) {//selected.size() > 0) {
          //FeatureSelectionEvent evt
          //  = new FeatureSelectionEvent(this, selected);
          //controller.handleFeatureSelectionEvent(evt);
          selectionManager.select(feature,this);
          setToFeature (feature);
        }

        // !! Should we pull up info in Web browser about the selected feature?
        // See FeatureEditorDialog.jbInit()
      }
    }



    protected Selection getSelected(SeqFeatureI feature) {
      Selection selection = new Selection();

      SeqFeatureI sf = ((feature instanceof DrawableSeqFeature) ?
                        ((DrawableSeqFeature) feature).getFeature() :
                        feature);
      if (sf.canHaveChildren()) {
        FeatureSetI fs = (FeatureSetI) sf;
        for (int i = 0; i < fs.size(); i++) {
          selection.add(new SelectionItem(this, fs.getFeatureAt(i)));
        }
      } else {
        selection.add(new SelectionItem(this,sf));
      }
      return selection;
    }
  }


  /*
   * This is the TableModel for the table 
   * Takes a Vector of SeqFeatures in setData.
   * Each SeqFeature represents a row
   * propertyList is a Vector of strings representing the columns.
   * The strings are used to call methods on the SeqFeature. 
   * The mapping from string to method call is done in DetailInfo.
   */
  protected class FilteredFeatureModel extends AbstractTableModel {
    private Vector propertyList = new Vector();
    private Vector data = new Vector();
    private String sortKey = null;
    private boolean reverseSort = true;
    private String type = "";
    private Vector keysWithDefaultSortingDescending;

    public void setSortKey(String key) {
      sortKey = key;
      sort();
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getType() {
      return type;
    }

    public String getSortKey() {
      return sortKey;
    }

    public boolean getReverseSort() {
      return reverseSort;
    }

    public void setSortKey(int key, boolean reverse) {
      sortKey = ((ColumnProperty)propertyList.elementAt(key)).getHeading();
      reverseSort = reverse;
      sort();
    }

    public void setReverseSort(boolean reverse) {
      reverseSort = reverse;
      sort();
    }

    public void sort() {
      VectorUtil.sort(data,
                      new MegaComparator(sortKey),
                      reverseSort);
      fireTableDataChanged();
    }

    public void setPropertyList(Vector propertyList) {
      
      boolean fireChange = false;
      if (propertyList.size() != this.propertyList.size()) {
        fireChange = true;
      } else if (propertyList.size() == 0) {
        fireChange = false; // If we've gone from 0 to 0 nothing much has happened - don't fire change
      } else {
        //for(int i=0; i < propertyList.size(); i++) {
        //  String column_name = (String) propertyList.elementAt(i);
        //  if (!column_name.equals(this.propertyList.elementAt(i))) {
        //    fireChange = true;
        //    break;
        //  }
        //}
        fireChange = true; // SMJS Try always firing a structure change when switch types. This is
                           //      to enable the tableChanged method in EvidenceTable to save old
                           //      column widths and set new column widths
      }

      this.propertyList = propertyList;
      if (fireChange)
        fireTableStructureChanged();
    }

    protected Vector getPropertyList() {
      return propertyList;
    }

    public void setData(Vector data) {
      this.data = data;
      sort();
    }

    public void setData(FeatureList featList) {
      // for now just grab vector - would be cool if model used feat list
      setData(featList.toVector());
    }

    /**
     * data is a Vector of SeqFeatureIs, each SeqFeatureI represents
     * a row in the table corresponding to the index in the Vector
     */
    public void setData(Vector data, String key, boolean reverse) {
      this.data = data;
      sortKey = key;
      reverseSort = reverse;
      sort();
    }

    public void setData(FeatureList data, String key, boolean reverse) {
      setData(data.toVector(),key,reverse);
    }

    public int getRowCount() {
      return data.size();
    }

    public int getColumnCount() {
      return propertyList.size();
    }

    /** Retrieve Object to be displayed at row,column.
        DetailInfo.getPropertyForFeat is the workhorse here. */
    public Object getValueAt(int row, int column) {
      SeqFeatureI sf = (SeqFeatureI) data.elementAt(row);
      String prop = ((ColumnProperty)propertyList.elementAt(column)).getHeading();
      return DetailInfo.getPropertyForFeature(prop, sf);
    }

    public SeqFeatureI getRow(int row) {
      if (row < 0 || row >= data.size())
        return null;
      return (SeqFeatureI) data.elementAt(row);
    }

    /** Get row number of passed in feature. returns -1 if not found */
    private int getRowNumberForFeature(SeqFeatureI sf) {
      for (int i=0; i<getRowCount(); i++)
        if (getRow(i) == sf)
          return i; // will == work? cloning?
      return -1; // not found
    }

    public String getColumnName(int col) {
      String col_name = ((ColumnProperty)propertyList.elementAt(col)).getHeading();
      return DetailInfo.getPrettyNameForString(col_name);
    }

    /** Set key as sorting descending on left click (ascend on shift click) */
    void setColumnDefaultSortingDescending(String key) {
      if (keysWithDefaultSortingDescending==null)
        keysWithDefaultSortingDescending = new Vector(2);
      keysWithDefaultSortingDescending.addElement(key);
    }

    /** Whether default sorting for key is descending */
    private boolean defaultSortingIsDescending(int col) {
      if (keysWithDefaultSortingDescending == null)
        return false;
      String key = ((ColumnProperty)propertyList.elementAt(col)).getHeading();
      if (keysWithDefaultSortingDescending.contains(key))
        return true;
      return false;
    }
  } // end FiteredFeatureModel inner class


  protected class CellRenderer extends DefaultTableCellRenderer {
    public Component getTableCellRendererComponent(JTable table,
        Object value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column) {
      setForeground(Color.black);
      setBackground(Color.white);

      Component out = super.getTableCellRendererComponent(table,
                      value,
                      isSelected,
                      hasFocus,
                      row,
                      column);
      setToolTipText(getText());
      return out;
    }
  }


  protected class TypeCellRenderer extends JButton
    implements TableCellRenderer {

    public TypeCellRenderer() {
      super();
    }

    public Component getTableCellRendererComponent(JTable table,
        Object value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column) {
      if (value == null) {
        setBackground(Color.white);
        setForeground(Color.black);
        setText("");
      } else {
        SeqFeatureI item
        = ((FilteredFeatureModel) table.getModel()).getRow(row);
        apollo.config.FeatureProperty property =
          Config.getPropertyScheme().getFeatureProperty(item.getTopLevelType());
        // This sets the background color to the generic color for this feature type--it
        // does not color annotations by owner color.  If we decide to do that, we'll
        // need to pass the owner (which is associated with TRANSCRIPTS, not annots)
        // down to this method.
        setBackground(property.getColour());
	// This gets the visual type for the logical type
        setText (DetailInfo.getPropertyType(item));
        if (property.getColour() == Color.black) {
          setForeground(Color.white);
        } else {
          setForeground(Color.black);
        }

        setToolTipText(DetailInfo.getPropertyType(item));
      }
      return this;
    }
  }

  public TablePanel(Controller c,SelectionManager sm) {
    model = new FilteredFeatureModel();
    table = new EvidenceTable (model);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    table.setDefaultRenderer(Object.class,
                             new CellRenderer());
    table.addMouseListener(new CopyMouseListener());
    table.getColumnModel().getSelectionModel().
    setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    table.getSelectionModel().
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    ListSelectionModel rsm = table.getSelectionModel();
    rowSelectionListener = new RowSelectionListener(rsm);
    rsm.addListSelectionListener(rowSelectionListener);

    pane = new JScrollPane();
    pane.setViewportView (table);

    setController (c);
    selectionManager = sm;

    JTableHeader th = table.getTableHeader();
    SortMouseListener sml = new SortMouseListener();
    th.addMouseListener(sml);
    th.addMouseMotionListener(sml);
  }


  class EvidenceColumnListener implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent e) {
      String name = e.getPropertyName();

      if (name.equals("preferredWidth")) {
        //System.out.println("Property change of name " + name + " event " + e);
        if (e.getSource() instanceof TableColumn) { // it really should be a TableColumn
          table.saveSizes();
        }
      }
    }
  }

  class EvidenceTable extends JTable {
    // initialised is to stop inner class access to outer class variables in constructor 
    // which causes null pointer exceptions
    boolean initialised = false;

    public EvidenceTable(TableModel t) {
      super(t);
      initialised = true;
    }

    public void tableChanged(TableModelEvent e) { 
      // Can't access outer class variables until out of constructor
      if (!initialised) {
        super.tableChanged(e);
        return;
      }

      //System.out.println("tableChanged with TableModelEvent " + e); 
      // If its a structure change event
      if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
        //System.out.println(" IS a Structure change event"); 
        removeEvidenceColumnListeners();
      }
      
      // This will rebuild the columns 
      super.tableChanged(e); 

      FilteredFeatureModel model = (FilteredFeatureModel)getModel();

      if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
        setWidthsFromColumnProperties();

        doLayout();
      }
    }

    protected void saveSizes() {
      //System.out.println("In Saving sizes");
      TableColumnModel columnModel = getColumnModel();

      int numColumn = columnModel.getColumnCount();
      if (numColumn == 0) {
        logger.warn("No columns so not saving");
        return;
      }

      //System.out.println("Saving sizes for " + model.getType());
  
      int [] widths = new int[columnModel.getColumnCount()];
      //System.out.print(" widths:  ");
      for (int i=0;i<numColumn;i++) {
        TableColumn column = columnModel.getColumn(i);
        widths[column.getModelIndex()] = column.getWidth();
        //System.out.print(" " + widths[i]);
      }
      //System.out.println("");
      storeWidthsForType(model.getType(),widths);
    }

    private void storeWidthsForType(String type, int [] widths) {
      if (type.equals("")) {
        logger.error("TRYING TO STORE WIDTHS FOR empty type string");
        new Throwable().printStackTrace();
        return;
      }

      for (int i=0;i<widths.length;i++) {
        ColumnProperty cp = (ColumnProperty)model.getPropertyList().elementAt(i);
        cp.setPreferredWidth(widths[i]);
      }
    }

    private void removeEvidenceColumnListeners() {
      //System.out.println("Removing listeners");
      for (int i=0;i<columnModel.getColumnCount();i++) {
        PropertyChangeListener [] listeners = columnModel.getColumn(i).getPropertyChangeListeners();
        for (int j=0; j<listeners.length; j++) {
          if (listeners[j] instanceof EvidenceColumnListener) {
            columnModel.getColumn(i).removePropertyChangeListener(listeners[j]);
          }
        }
      }
    }

    private void addEvidenceColumnListeners() {
      //System.out.println("Adding listeners");
      TableColumnModel columnModel = getColumnModel();
      for (int i=0;i<columnModel.getColumnCount();i++) {
        columnModel.getColumn(i).addPropertyChangeListener(new EvidenceColumnListener());
      }
    }



    public void setWidthsFromColumnProperties() {
      if (model.getPropertyList().size() > 0) {

        removeEvidenceColumnListeners();

        TableColumnModel columnModel = getColumnModel();

        if (columnModel.getColumnCount() == model.getPropertyList().size()) {
          for (int i=0;i<columnModel.getColumnCount();i++) {
            TableColumn column = columnModel.getColumn(i);
            ColumnProperty cp = (ColumnProperty)model.getPropertyList().elementAt(column.getModelIndex());
            column.setPreferredWidth(cp.getPreferredWidth());
          }
        } else {
          logger.warn("Programming error - different number of column widths saved for " + model.getType());
          new Throwable().printStackTrace();
        }
        addEvidenceColumnListeners();
      }
    }

    protected JTableHeader createDefaultTableHeader() {
      return new JTableHeader(columnModel) {
        public String getToolTipText(MouseEvent e) {
          java.awt.Point p = e.getPoint();
          int index = columnModel.getColumnIndexAtX(p.x);
          int realIndex = columnModel.getColumn(index).getModelIndex();
          return ((FilteredFeatureModel)model).getColumnName(realIndex);
        }
      };
    }
  }

  protected void saveSortKey(String type, String key, boolean isDescending) {
    // Default is not to save it. EvidencePanel doesn't need to because its columns are fixed.
    // If that changes then implement saving here
  }


  protected boolean isSelectionSource(FeatureSelectionEvent e) {
    return e.getSource() == rowSelectionListener;
  }

  /**
   * Selects the feature. Triggers valueChanged in RowSelectionListener
   * so it sets a flag saying this selection is internal. Also vertically
   * scrolls to selection
   */
  protected void selectFeature (SeqFeatureI feature) {
    // This should not be necasary - drawables should not be passed in selections
    if (feature instanceof DrawableSeqFeature)
      feature = ((DrawableSeqFeature)feature).getFeature();
    int row = model.getRowNumberForFeature(feature);
    // yikes this will call RowSelectionListener.valueChanged which will
    // send out a selectionEvent and end up in an endless loop
    // setting this variable to stop that - the only other way would be
    // to write our own ListSelectionModel to pass info along, but I think
    // thats overkill
    listSelectionComesFromSelf = true;
    if (row!=-1)
      table.getSelectionModel().setSelectionInterval(row,row);
    listSelectionComesFromSelf = false; // set back to false
    scrollToRow(row);
  }

  private void scrollToRow(int row) {
    if (! isRowVisible(row))
      pane.getVerticalScrollBar().setValue(row * table.getRowHeight());
  }

  private boolean isRowVisible(int row) {
    JScrollBar sb = pane.getVerticalScrollBar();
    int rowPixel = row * table.getRowHeight();
    return (rowPixel > sb.getValue())
           && (rowPixel < sb.getValue() + sb.getVisibleAmount());
  }

  protected void setToFeature(SeqFeatureI feature) { // public?
    try {
      throw new Exception ("Raw table called for " + feature.getName() +
                           " class " + feature.getClass().getName());
    } catch (Exception e) {
      System.out.println (e.getMessage());
      e.printStackTrace();
    }
  }

  public void setController(Controller c) {
    controller = c;
  }

  public Controller getController() {
    return controller;
  }

  protected SelectionManager getSelectionManager() {
    return selectionManager;
  }
}
