package apollo.dataadapter;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

//import org.apollo.datamodel.*;

import org.bdgp.io.*;
import java.util.Properties;
import org.bdgp.swing.AbstractDataAdapterUI;

import apollo.datamodel.CurationSet;
import apollo.datamodel.Range;
import apollo.datamodel.RangeI;
import apollo.config.Config;
import apollo.gui.ProxyDialog;

public class EnsCGIAdapterGUI extends AbstractDataAdapterUI {//JPanel implements DataAdapterUI {

  JComboBox regionList;
  JComboBox pathList;
  JComboBox chrStartEndList;

  JTextField chrTextBox;
  JTextField startTextBox;
  JTextField endTextBox;

  JLabel regionLabel;
  JLabel chrLabel;
  JLabel startLabel;
  JLabel endLabel;
  JLabel pathLabel;
  JLabel chrStartEndLabel;

  JTabbedPane pane;
  JPanel      rangePanel;
  JPanel      regionPanel;

  JButton     proxyButton;

  ActionListener chrAction;


  Vector pathVector = new Vector();
  Vector regionVector = new Vector();
  Vector chrStartEndVector = new Vector();

  public static final int MAX_HISTORY_LENGTH = 5;

  public static final int REGION = 1;
  public static final int RANGE  = 2;

  protected DataAdapter driver;
  protected IOOperation op;

  private Properties props;

  public EnsCGIAdapterGUI(IOOperation op) {
    this.op = op;
    regionList = new JComboBox();
    pathList = new JComboBox();
    chrTextBox = new JTextField();
    startTextBox = new JTextField();
    endTextBox = new JTextField();
    chrStartEndList = new JComboBox();

    regionLabel = new JLabel("Contig");

    chrLabel = new JLabel("Chr");
    startLabel = new JLabel("Start");
    endLabel = new JLabel("End");
    chrStartEndLabel = new JLabel("History");

    pathLabel = new JLabel("EnsCGI Server");

    proxyButton = new JButton("Proxy settings");
    proxyButton.addActionListener( new ActionListener() {
                                     public void actionPerformed(ActionEvent evt) {
                                       ProxyDialog pd = new ProxyDialog(null);
                                       pd.setVisible(true);
                                     }
                                   }
                                 );

    buildGUI();
    chrAction = new ChrStartEndListener();
    chrStartEndList.addActionListener(chrAction);
  }

  public class ChrStartEndListener implements ActionListener {
    public void actionPerformed(ActionEvent evt) {
      // System.out.println("chr state change");
      RangeI loc = parseChrStartEndString((String)chrStartEndList.getSelectedItem());
      if (loc != null) {
        chrTextBox.setText(loc.getName());
        startTextBox.setText(loc.getStart() + "");
        endTextBox.setText(loc.getEnd() + "");
      }
    }
  }

  public static apollo.datamodel.RangeI parseChrStartEndString(String str) {
    StringTokenizer tokenizer = new StringTokenizer(str);
    if (!tokenizer.nextToken().equals("Chr")) {
      System.out.println("Failed parsing location string " + str);
      return null;
    }
    String chr = tokenizer.nextToken();
    int start = Integer.parseInt(tokenizer.nextToken());
    int end = Integer.parseInt(tokenizer.nextToken());

    return new apollo.datamodel.Range(chr, start, end);
  }

  public void setProperties(Properties in) {
    //System.out.println("Setting properties");
    props = in;
    pathVector = new Vector();
    regionVector = new Vector();
    chrStartEndVector = new Vector();
    try {
      int items = Integer.parseInt(props.getProperty("pathItems"));
      if (items != 0) {
        for(int i=0; i < items; i++)
          pathVector.addElement(props.getProperty("pathItem"+i));
      } else {
        pathVector.addElement(Config.getCGIHost());
      }


      items = Integer.parseInt(props.getProperty("regionItems"));
      for(int i=0; i < items; i++)
        regionVector.addElement(props.getProperty("regionItem"+i));

      items = Integer.parseInt(props.getProperty("chrStartEndItems"));
      for(int i=0; i < items; i++)
        chrStartEndVector.addElement(props.getProperty("chrStartEndItem"+i));

    } catch (Exception e) {
      // This isn't very nice
      if (pathVector.size() == 0) {
        pathVector.addElement(Config.getCGIHost());
      }
    }
    chrStartEndList.removeActionListener(chrAction);
    pathList.setModel(new DefaultComboBoxModel(pathVector));
    regionList.setModel(new DefaultComboBoxModel(regionVector));
    chrStartEndList.setModel(new DefaultComboBoxModel(chrStartEndVector));
    chrStartEndList.addActionListener(chrAction);
  }

  public Properties getProperties() {
    chrStartEndList.removeActionListener(chrAction);
    String selectedPath = getSelectedPath();
    pathVector.removeElement(selectedPath);
    pathVector.insertElementAt(selectedPath, 0);

    if (pane.getSelectedComponent() == regionPanel) {
      String selectedRegion = getSelectedRegion();
      regionVector.removeElement(selectedRegion);
      regionVector.insertElementAt(selectedRegion, 0);
    } else {
      String selectedChrStartEnd = getSelectedChrStartEnd();
      chrStartEndVector.removeElement(selectedChrStartEnd);
      chrStartEndVector.insertElementAt(selectedChrStartEnd, 0);
    }

    Properties out = new Properties();
    if (pathVector.size() > MAX_HISTORY_LENGTH)
      out.put("pathItems", MAX_HISTORY_LENGTH+"");
    else
      out.put("pathItems", pathVector.size()+"");

    if (regionVector.size() > MAX_HISTORY_LENGTH)
      out.put("regionItems", MAX_HISTORY_LENGTH+"");
    else
      out.put("regionItems", regionVector.size()+"");

    if (chrStartEndVector.size() > MAX_HISTORY_LENGTH)
      out.put("chrStartEndItems", MAX_HISTORY_LENGTH+"");
    else
      out.put("chrStartEndItems", chrStartEndVector.size()+"");


    for(int i=0; i < pathVector.size() && i < MAX_HISTORY_LENGTH; i++) {
      out.put("pathItem"+i, (String) pathVector.elementAt(i));
    }

    for(int i=0; i < regionVector.size() && i < MAX_HISTORY_LENGTH; i++) {
      out.put("regionItem"+i,
              (String) regionVector.elementAt(i));
    }

    for(int i=0; i < chrStartEndVector.size() && i < MAX_HISTORY_LENGTH; i++) {
      out.put("chrStartEndItem"+i, (String) chrStartEndVector.elementAt(i));
    }

    chrStartEndList.addActionListener(chrAction);
    return out;
  }

  public void setFont(Font font) {
    super.setFont(font);
    if (pathLabel != null)
      pathLabel.setFont(font);
    if (regionLabel != null)
      regionLabel.setFont(font);
    if (regionList != null)
      regionList.setFont(font);
    if (pathList != null)
      pathList.setFont(font);
  }

  public void buildGUI() {
    setPreferredSize(new Dimension(450,200));
    pathList.setPreferredSize(new Dimension(150,20));
    pathList.setEditable(true);
    regionList.setPreferredSize(new Dimension(250,10));
    regionList.setEditable(true);
    chrTextBox.setPreferredSize(new Dimension(150,10));
    chrTextBox.setEditable(true);
    startTextBox.setPreferredSize(new Dimension(150,10));
    startTextBox.setEditable(true);
    endTextBox.setPreferredSize(new Dimension(150,10));
    endTextBox.setEditable(true);
    chrStartEndList.setPreferredSize(new Dimension(150,20));
    chrStartEndList.setEditable(false);


    regionLabel.setPreferredSize(new Dimension(130,12));
    pathLabel.setPreferredSize(new Dimension(130,12));
    chrLabel.setPreferredSize(new Dimension(130,12));
    startLabel.setPreferredSize(new Dimension(130,12));
    endLabel.setPreferredSize(new Dimension(130,12));
    chrStartEndLabel.setPreferredSize(new Dimension(130,12));

    Box regionBox = new Box(BoxLayout.X_AXIS);
    regionBox.add(regionLabel);
    regionBox.add(regionList);
    regionBox.add(Box.createHorizontalGlue());
    //
    Box pathBox = new Box(BoxLayout.X_AXIS);
    pathBox.add(pathLabel);
    pathBox.add(pathList);

    Box chrBox = new Box(BoxLayout.X_AXIS);
    chrBox.add(chrLabel);
    chrBox.add(chrTextBox);
    chrBox.add(Box.createHorizontalGlue());

    Box startBox = new Box(BoxLayout.X_AXIS);
    startBox.add(startLabel);
    startBox.add(startTextBox);
    startBox.add(Box.createHorizontalGlue());

    Box endBox = new Box(BoxLayout.X_AXIS);
    endBox.add(endLabel);
    endBox.add(endTextBox);
    endBox.add(Box.createHorizontalGlue());

    Box chrStartEndBox = new Box(BoxLayout.X_AXIS);
    chrStartEndBox.add(chrStartEndLabel);
    chrStartEndBox.add(chrStartEndList);
    chrStartEndBox.add(Box.createHorizontalGlue());

    pane = new JTabbedPane();
    rangePanel = new JPanel();
    pane.setPreferredSize(new Dimension(600,70));
    rangePanel.setLayout(new BoxLayout(rangePanel, BoxLayout.Y_AXIS));
    rangePanel.setPreferredSize(new Dimension(450,70));
    rangePanel.add(chrBox);
    rangePanel.add(startBox);
    rangePanel.add(endBox);
    rangePanel.add(chrStartEndBox);
    pane.insertTab("Range",null,rangePanel,"Select by base range",0);

    regionPanel = new JPanel();
    regionPanel.setLayout(new BoxLayout(regionPanel, BoxLayout.Y_AXIS));
    regionPanel.add(regionBox);
    pane.insertTab("Contig",null,regionPanel,"Select by region name",1);

    Box proxyBox = new Box(BoxLayout.X_AXIS);
    proxyBox.add(Box.createHorizontalGlue());
    proxyBox.add(proxyButton);

    setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

    add(pathBox);
    add(pane);
    add(proxyBox);
    //add(Box.createVerticalGlue());
  }

  public void setDataAdapter(DataAdapter driver) {
    this.driver = driver;
  }

  public String getSelectedPath() {
    String selectedPath = (String) pathList.getSelectedItem();
    if (selectedPath == null ||
        !selectedPath.equals(pathList.getEditor().getItem())) {
      selectedPath = (String) pathList.getEditor().getItem();
    }
    return selectedPath;
  }

  public String getSelectedRegion() {
    String selectedRegion = (String) regionList.getSelectedItem();
    if (selectedRegion == null ||
        !selectedRegion.equals(regionList.getEditor().getItem())) {
      selectedRegion = (String) regionList.getEditor().getItem();
    }
    return selectedRegion;
  }

  public String getSelectedChrStartEnd() {
    return new String("Chr " + getSelectedChr() + " " + getSelectedStart() +
                      " " + getSelectedEnd());
  }

  public String getSelectedChr() {
    String selectedChr = (String) chrTextBox.getText();
    return selectedChr;
  }

  public String getSelectedStart() {
    String selectedStart = (String) startTextBox.getText();

    return selectedStart;
  }

  public String getSelectedEnd() {
    String selectedEnd = (String) endTextBox.getText();
    return selectedEnd;
  }

  private boolean areAllTextBoxesFilled() {
    if (pane.getSelectedComponent() == regionPanel) {
      return ((!getSelectedRegion().equals("")) && getSelectedRegion() != null);

    } else {
      //System.out.println("chr = |" + chrTextBox.getText() + "|");
      //System.out.println("start = |" + startTextBox.getText() + "|");
      //System.out.println("end = |" + endTextBox.getText() + "|");
      return !(chrTextBox.getText() == null || startTextBox.getText() == null || endTextBox.getText() == null ||
               chrTextBox.getText().equals("") || startTextBox.getText().equals("") || endTextBox.getText().equals(""));
    }
  }

  public Object doOperation(Object values) throws ApolloAdapterException {
    //Config.newDataAdapter(driver);

    EnsCGIAdapter eca = (EnsCGIAdapter)driver;
    eca.setupAdapter(getSelectedPath());

    if (areAllTextBoxesFilled()) {
      if (pane.getSelectedComponent() == regionPanel) {

        eca.setRegion(getSelectedRegion());
        eca.setGetMode(REGION);

      } else {

        eca.setRegion(getSelectedChrStartEnd());
        eca.setGetMode(RANGE);
      }


      if (op.equals(ApolloDataAdapterI.OP_READ_DATA)) {
        CurationSet out = ((ApolloDataAdapterI) driver).getCurationSet();
        return out;
      } else {
        return null;
      }
    } else {
      throw new ApolloAdapterException("Didn't specify enough information");
    }
  }
}
