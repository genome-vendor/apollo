package apollo.gui;

import java.util.Vector;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SeqFeature;
import apollo.gui.event.BaseFocusEvent;
import apollo.gui.genomemap.StrandedZoomableApolloPanel;

import org.apache.oro.text.regex.*;

/** CallsSeqSelector on separate thread and populates table with
    results */
public class SeqSelectorTable {
  
  Perl5Compiler p5Compiler;
  Perl5Pattern pattern; //to extract numeric patterns
  
  private SeqFeatureI sf;
  private String searchSeqString;
  private boolean revComp;
  private Vector columns = new Vector(2);
  private Vector features = new Vector();
  private JTable        resultTable;
  //  private static int tableSize;
  private StrandedZoomableApolloPanel szap;
  private SelectionManager selectionManager;
  private StatusPane statusPane;
  private boolean useRegExp;
  
  public SeqSelectorTable(SeqFeatureI sf, String searchSeqString,
                          boolean revComp, StrandedZoomableApolloPanel szap,
                          StatusPane statusPane) {
    this(sf, searchSeqString, revComp, szap, statusPane, false);
  }
  
  public SeqSelectorTable(SeqFeatureI sf, String searchSeqString,
                          boolean revComp, StrandedZoomableApolloPanel szap,
                          StatusPane statusPane, boolean useRegExp) {
    this.sf = sf;
    this.searchSeqString = searchSeqString.toUpperCase();
    this.revComp = revComp;
    this.szap = szap;
    this.useRegExp = useRegExp;
    selectionManager = szap.getSelectionManager();
    this.statusPane = statusPane;
    initTable();
    SeqSelSwingWorker w = new SeqSelSwingWorker();
    w.start();
    // funny - selects all features but nothing selected in table
    // should the whole table be selected or should select not happen?
    selectionManager.select(features, this);
    
    p5Compiler = new Perl5Compiler();
    try{
      pattern =
	(Perl5Pattern)p5Compiler.compile("([0-9]+)",
					 p5Compiler.CASE_INSENSITIVE_MASK);
    } catch(MalformedPatternException ex){
      System.out.println("couldn't make Perl5 pattern");
    }
  }
  
  public JTable getTable() {
    return resultTable;
  }
  
  private void initTable() {
    columns.addElement("Position");
    columns.addElement("Sequence");
    resultTable    = new JTable(new Vector(), columns);
    // Currently doesnt deal with multiselect - might be nice if did
    resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    resultTable.getSelectionModel().addListSelectionListener(new TableSelector());
  }
  
  private class SeqSelSwingWorker extends SwingWorker {
    /**
     * Compute the value to be returned by the <code>get</code> method. 
     */
    public Object construct() {
      SequenceSelector sel = new SequenceSelector(sf,
						  searchSeqString,
						  revComp,
						  useRegExp);
      return sel;
    }

    /**
     * Called on the event dispatching thread (not on the worker thread)
     * after the <code>construct</code> method has returned.
     */
    public void finished() {
      // get() is a SwingWorker method that returns an Object that is
      // constructed by construct() which is a SequenceSelector
      SequenceSelector sel = (SequenceSelector)get();
      
      features = sel.getMatches();
      SequenceMatchModel model = new SequenceMatchModel();
      model.setData(features);
      model.setColumns(columns);
      resultTable.setModel(model);
      if (statusPane != null)
	statusPane.setText("Found sequence " + features.size() + " times");
    }
  }

  private void fireBaseFocusEvent(String posString) {
    int pos = Integer.parseInt(posString);
    //if (revComp)
    //  pos = pos - searchSeqString.length();
    BaseFocusEvent evt = new BaseFocusEvent(this, pos, new SeqFeature());
    selectionManager.getController().handleBaseFocusEvent(evt);
  }
  
  
  //modified to extract positions from strings in form "(\d+)-(\d+)"
  private class TableSelector implements ListSelectionListener {
    public void valueChanged(ListSelectionEvent e) {
      
      Perl5Matcher matcher = new Perl5Matcher();
      PatternMatcherInput pmInput = null;
      
      int row = resultTable.getSelectedRow();
      if (row >= 0 && !e.getValueIsAdjusting() ) {
        Object position = resultTable.getValueAt(row, 0);
	pmInput = new PatternMatcherInput(position.toString());
	matcher.contains(pmInput,pattern);
	fireBaseFocusEvent(matcher.getMatch().toString());
        SeqFeature feat = (SeqFeature)features.elementAt(row);
        selectionManager.select(feat,this);
        // zoom in on new selection (with some padding)
	szap.zoomToSelectionWithWindow(feat.length()*2 - 2);
      }
    }
  }

  /**
   * This is the TableModel for the table 
   * Takes a Vector of SequenceMatch in setData.
   * Each SequenceMatch represents a row
   */
  protected class SequenceMatchModel extends AbstractTableModel {
    private Vector data;
    private Vector columns;

    protected void setData (Vector data) {
      this.data = data;
    }
    
    protected void setColumns (Vector columns) {
      this.columns = columns;
    }

    public int getRowCount() {
      return data.size();
    }
    
    public int getColumnCount() {
      return columns.size();
    }

    public String getColumnName(int column) {
      return (String) columns.elementAt(column);
    }

    public Object getValueAt(int row, int column) {
      SeqFeatureI match = (SeqFeatureI) data.elementAt(row);
      if (column == 0)
        return (match.getLow() + "-" + match.getHigh());
      else
        return match.getResidues();
    }

  } // end SequenceMatchModel inner class


}

