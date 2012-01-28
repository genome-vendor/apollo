package apollo.gui;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.util.*;
import java.io.File;

import org.bdgp.io.*;
import org.bdgp.swing.AbstractDataAdapterUI;
import java.util.Properties;

public abstract class GenericFileAdapterGUI extends AbstractDataAdapterUI {//JPanel implements DataAdapterUI {

  public static final int MAX_HISTORY_LENGTH = 5;

  protected DataAdapter driver;
  protected IOOperation op;
  protected JComboBox pathList;

  protected JPanel     panel;
  protected JPanel     panel2;
  protected JLabel     typeLabel;
  protected Vector     filePaths;
  protected JButton    browseButton;
  protected Properties props;
  protected FileFilter filter;

  /** How much space to display file names - changed from 300 to 500 as
   * 300 was just too small for really long path-filenames. Readjust if need be.
   * Maybe it could even be smart and grow itself according to the size of the
   * pathname. wouldnt that be nifty
   */

  int pathListWidth=500;//300;

  public GenericFileAdapterGUI() {}

  public GenericFileAdapterGUI(IOOperation op) {
	  this(op, null);
  }
  
  public GenericFileAdapterGUI(IOOperation op, FileFilter filter)
  {
	    this.op = op;
	    initGUI();
	    buildGUI();
	    attachListeners();
	    this.filter = filter;
  }

  public void initGUI () {
    pathList     = new JComboBox();
    filePaths    = new Vector();
    browseButton = new JButton("Browse...");
    typeLabel    = new JLabel("File type not set");
    pathList.setEditable(true);
  }

  public void buildGUI() {
    panel = new JPanel();

    pathList    .setFont(getFont());
    browseButton.setFont(getFont());

    pathList    .setAlignmentY((float) .5);
    browseButton.setAlignmentY((float) .5);

    // increased to 500 - some filenames+path are long

    pathList.setPreferredSize(new Dimension(pathListWidth,10));

    setLayout(new BorderLayout());

    //    panel.setLayout(new GridLayout(1,1));
    panel.setLayout(new BorderLayout());
    
    panel2 = new JPanel();

    panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));
    panel2.add(pathList);
    panel2.add(browseButton);

    panel.add(panel2);

    add("South", panel);
  }

  public void attachListeners() {
    browseButton.addActionListener( new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          browseFiles(pathList);
        }
      }
                                    );
  }

  /** Called from browse button, sets browser with currently selected
   * path if there is one, and then calls browser to get a file
   */
  public void browseFiles(JComboBox list) {
    String selectedPath = getSelectedPath();
    File   currentFile  = new File(selectedPath);
    File   browseFile   = fileBrowser(currentFile,this,filter,op);

    if (browseFile == null) {
      return;
    }

    list.configureEditor(list.getEditor(),browseFile.toString());
  }

  public static File fileBrowser(File currentFile,Component parent) {
	  return fileBrowser(currentFile, parent, null, null);
  }
  
  /** Brings up JFileChooser, returns null on cancel or null file */
  public static File fileBrowser(File currentFile,Component parent, FileFilter filter, IOOperation op) {
    String startPath;

    if (currentFile!=null && currentFile.exists()) {
      startPath = currentFile.getPath();
    } else {
      startPath = System.getProperty("user.home");
    }

    JFileChooser chooser = new JFileChooser(startPath);
    if (filter != null) {
    	chooser.setFileFilter(filter);
    }
    
    int returnVal = isWriteOperation(op) ? chooser.showSaveDialog(parent) : chooser.showOpenDialog(parent);
    File file = chooser.getSelectedFile();

    if (file==null) {
      JOptionPane.showMessageDialog(parent,"No file selected","Error",
                                    JOptionPane.ERROR_MESSAGE);
      return null;
    }
    if (returnVal != JFileChooser.APPROVE_OPTION)
      return null;
    return file;
  }

  private static boolean isWriteOperation(IOOperation op)
  {
    if (op == null) {
      return false;
    }
    return op.getName().startsWith("Write");
  }
  
  public void setDataAdapter(DataAdapter driver) {
    this.driver = driver;
    typeLabel.setText(driver.getType());
    //    System.out.println("GenericFileAdapterGUI: set typelabel to " + typeLabel.getText()); // DEL
  }

  /** Populates filePaths Vector from historyItems Properties
      "in" and sets the model of the pathList ComboBox
   */
  public void setProperties(Properties in) {
    props = in;
    if (props == null)
      return;

    filePaths = new Vector();
    String historyItems = props.getProperty("historyItems");
    if (historyItems == null || Integer.parseInt(historyItems) == 0) {
      System.out.println("No history for this file adapter");
      return;
    }
    try {
      int items = Integer.parseInt(historyItems);
      for(int i=0; i < items; i++) {
        filePaths.addElement(props.getProperty("historyItem"+i));
      }
    } catch (NumberFormatException e) {
      System.out.println("Can't parse history file");
    }
    pathList.setModel(new DefaultComboBoxModel(filePaths));
  }

  /** Returns Properties with history items of all the files in
   * filePaths Vector, puts currently selected path at front of list
   */
  public Properties getProperties() {
    String selectedPath = getSelectedPath();
    filePaths.removeElement(selectedPath);
    filePaths.insertElementAt(selectedPath, 0);

    Properties out = new Properties();
    if (filePaths.size() > MAX_HISTORY_LENGTH)
      out.put("historyItems", MAX_HISTORY_LENGTH+"");
    else
      out.put("historyItems", filePaths.size()+"");
    for(int i=0; i < filePaths.size() && i < MAX_HISTORY_LENGTH; i++) {
      out.put("historyItem"+i, (String) filePaths.elementAt(i));
    }
    return out;
  }

  public String getSelectedPath() {
    String selectedPath = (String) pathList.getSelectedItem();
    if (selectedPath == null ||
        !selectedPath.equals(pathList.getEditor().getItem())) {
      selectedPath = (String) pathList.getEditor().getItem();
    }
    return selectedPath;
  }

  // This doesnt seem to be used
  public boolean validOperation () throws DataAdapterException {
    return true;
  }

    public JPanel getPanel() {
	return panel;
    }
    public int getPathListWidth() {
	return pathListWidth;
    }

    public void setPathListWidth(int width) {
      this.pathListWidth = width;
    }
    
    public FileFilter getFileFilter()
    {
    	return filter;
    }
    
    public void setFileFilter(FileFilter filter)
    {
    	this.filter = filter;
    }

}
