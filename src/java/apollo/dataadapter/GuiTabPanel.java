package apollo.dataadapter;

import java.util.Vector;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import javax.swing.*;

import java.util.Properties;
import java.awt.Color;

import apollo.dataadapter.DataInputType;
import apollo.util.GuiUtil;
import apollo.config.Config;
import apollo.config.Style;


/**
 * This is only used by GAMEAdapterGUI. It was an inner class of it, but
 * it seems to have outgrown innerclassness, so I broke it out into its own
 * class
 * GamePanel class represents a panel within the JTabbedPane
 * For now this is hardwired with one JComboBox to get input from
 * change later if have different kinds of panels 
 */
public abstract class GuiTabPanel extends JPanel {
  private String label;
  private DataInputType inputType;
  protected JComboBox comboBox;
  private Vector history;
  private JPanel innerPanel;
  private String example;
  private JComboBox dbComboBox;
  // this should be in AbstractDataAdapterUI?
  static final short DEFAULT_MAX_HISTORY_LENGTH = 10;
  private String colorScheme = "gray";  // bkgnd colors are ignored if colorScheme is gray
  
  public GuiTabPanel(String nm, String label, Color bkgnd) {
    this(nm,label,null,bkgnd);
  }
  
  public GuiTabPanel(String nm,String label,DataInputType type, Color bkgnd) {
    this(nm,label,type,null,bkgnd);
  }

  public GuiTabPanel(String nm,String label,DataInputType type,String example,
                     Color bkgnd) {
    this.setName(nm);
    this.label = label;
    inputType = type;
    this.example = example;
    history =  new Vector(getMaxHistoryLength());
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    if (!(colorScheme.equalsIgnoreCase("gray")))
      this.setBackground(bkgnd);
    buildGUI();
  }

  void addActionListener(ActionListener l) {
    if (comboBox != null)
      comboBox.addActionListener(l);
  }
  /** Only ready for commit if combobox has an item that isnt "" */
  boolean isReadyForCommit() {
    if (comboBox==null)
      return false;
    if (comboBox.getEditor().getItem() == null)
      return false;
    String editItem = (String)comboBox.getEditor().getItem();
    if(editItem.equals(""))
      return false;
    // was hoping this would distinguish between edit and selection - not so
    //String selItem = (String)comboBox.getSelectedItem();
    //if(editItem.equals(selItem)) return false; // selection
    return true;
  }

  protected short getMaxHistoryLength() {
    return DEFAULT_MAX_HISTORY_LENGTH;
  }

  /** This bundles/replaces getCurrentInput and getInputType */
  public DataInput getDataInput() {
    return new DataInput(getInputType(),getCurrentInput());
  }

  /** The default editing component is jcombo box so
   * it just grabs it from that - if getEditingComponent is overridden then 
   * this has to be overriden as well
   */
  public String getCurrentInput() {
    return (String)comboBox.getEditor().getItem();
  }

  /** hack for testing synteny - synteny is currently tied in with gui */
  public void setCurrentInput(String input) {
    comboBox.getEditor().setItem(input);
  }

  public DataInputType getInputType() {
    return inputType;
  }

  /** Tool tip is the same as label - is that lame? */
  private String getToolTip() {
    return label;
  }

  protected JPanel getPanel() {
    return this;
  }

  protected void buildGUI() {
    innerPanel = new JPanel(new GridBagLayout());
    if (!colorScheme.equalsIgnoreCase("gray"))
      innerPanel.setBackground(getBackground());

    if (showDatabaseList()) { 
      // nextColumn -> middle column
      innerPanel.add(getDatabasePanel(),
		     getConstraints().nextColumn());
    }
    JLabel jLabel = GuiUtil.makeJLabelWithFont(label,example);
    //++constraint.gridy;
    innerPanel.add(jLabel, getConstraints().nextRow());
    //++constraint.gridx;
    JComponent editComponent = getEditingComponent();
    innerPanel.add(editComponent, getConstraints().nextColumn());
    add(innerPanel);
  }
  
  /** makes JPanel with Database label and database list JComboBox */
  protected JPanel getDatabasePanel() {
    JPanel dbPanel = new JPanel(new GridBagLayout());
    if (!colorScheme.equalsIgnoreCase("gray"))
      dbPanel.setBackground (getBackground());
    JLabel dbLabel = GuiUtil.makeJLabelWithFont("Database ");
    ApolloGridBagConstraints dbCon = newConstraints();
    dbPanel.add(dbLabel,dbCon);
    dbComboBox = new JComboBox(getDatabaseList());
    dbPanel.add(dbComboBox,nextColumn(dbCon));
    return dbPanel;
  }

  protected ApolloGridBagConstraints constraints;

  protected ApolloGridBagConstraints getConstraints() {
    if (constraints==null) constraints = newConstraints();
    return constraints;
  }

  protected ApolloGridBagConstraints newConstraints() {
    return new ApolloGridBagConstraints();
  }

  public GridBagConstraints nextColumn(GridBagConstraints gbc) {
    ++gbc.gridx;
    return gbc;
  }
  protected GridBagConstraints nextRow(GridBagConstraints gbc) {
    ++gbc.gridy;
    gbc.gridx = 0;
    return gbc;
  }

  protected abstract Style getAdapterStyle();

  protected JPanel getInnerPanel(){
    return innerPanel;
  }//end getInnerPanel
  
  /** Default editing component is JComboBox - override if need different */
  protected JComponent getEditingComponent() {
    if (comboBox==null) {
      comboBox = new JComboBox();
      comboBox.setPreferredSize(new Dimension(500,25));
      comboBox.setEditable(true);
    }
    return comboBox;
  }

  /** String for history size in properties */
  private String historySizeStr() {
    return getName()+"Size";
  }
  /** String for history item in props */
  private String historyItemStr(int i) {
    return getName()+"Item"+i;
  }

  /** Puts the size of history and all the history items into
     * Properties prop
     */
  public void putHistoryInProperties(Properties prop) {
    // set size of history
    int s =
      history.size() > getMaxHistoryLength() ? getMaxHistoryLength() : history.size();
    prop.put(historySizeStr(),s+"");

    // add history items
    for (int i=0; i<history.size() ; i++) {
      prop.put(historyItemStr(i),(String)history.elementAt(i));
    }
  }

  /** Puts currently selected item at top of history list */
  public void addSelectedToHistory() {
    history.removeElement(getCurrentInput());
    history.insertElementAt(getCurrentInput(),0);
  }

  /** Retrieve history items from Properties
     * and add them to combo box model if combo box not null */
  public void retrieveHistoryFromProperties(Properties props) {
    // clear out old history from previous retrieval - if dont do this 
    // history will duplicate itself
    history.clear(); 
    String sizeString = props.getProperty(historySizeStr());
    if (sizeString == null)
      return;
    int sizeInt = Integer.parseInt(sizeString);
    for (int i=0; i<sizeInt; i++)
      history.addElement(props.getProperty(historyItemStr(i)));
    setEditorsHistory(history);
  }

  /** Override if editor component is not combo box */
  protected void setEditorsHistory(Vector history) {
    comboBox.setModel(new DefaultComboBoxModel(history));
  }

  /** Inserts panel into tabbed pane with name and tooltip at index */
  public void insertIntoTabbedPane(JTabbedPane pane,int index) {
    pane.insertTab(getName(),null,getPanel(),getToolTip(),index);
    if (!colorScheme.equalsIgnoreCase("gray"))
      pane.setBackgroundAt (index, getBackground());
  }

  protected boolean showDatabaseList() { 
    return numberOfDatabases() > 1; 
  }
  
  private int numberOfDatabases() {
    return getDatabaseList().size();
  }

  protected Vector getDatabaseList() {
    Style style = getAdapterStyle();
    Vector dblist = null;
    if (style != null)
      dblist =style.getDatabaseList();
    if (dblist == null)
      dblist = new Vector();
    return dblist;
  }

  /** Returns null if no databases(FileGAMEPanel) */
  public String getDatabase() {
    // no jcombobox if just 1
    if (numberOfDatabases() == 1)
      return (String)getDatabaseList().get(0);
    if (dbComboBox == null) 
      return null;  // no databases
    else
      return (String)dbComboBox.getSelectedItem(); 
  }

  // util class eventually? - just fiddling for now
  protected class ApolloGridBagConstraints extends GridBagConstraints {
    ApolloGridBagConstraints() {
      gridx = 0;
      gridy = 0;
    }
    public ApolloGridBagConstraints nextColumn() { 
      ++gridx; 
      return this; // cheesy?
    } 
    public ApolloGridBagConstraints nextRow() {
      gridx = 0;
      ++gridy;
      return this; 
    }
  }

}

