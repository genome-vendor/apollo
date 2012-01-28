package apollo.dataadapter;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import org.bdgp.io.*;
import java.util.Properties;
import org.bdgp.swing.AbstractDataAdapterUI;

import apollo.datamodel.*;
import apollo.datamodel.*;
import apollo.gui.*;

import apollo.dataadapter.mysql.*;
import apollo.dataadapter.gbrowse.*;

public class GbrowseAdapterGUI extends AbstractDataAdapterUI {

  JComboBox chrStartEndList;

  JTextField chrTextBox;
  JTextField startTextBox;
  JTextField endTextBox;

  JLabel chrLabel;
  JLabel startLabel;
  JLabel endLabel;
  JLabel chrStartEndLabel;

  JPanel      rangePanel;
  JButton     proxyButton;

  ActionListener chrAction;

  Vector chrStartEndVector = new Vector();

  public static final int MAX_HISTORY_LENGTH = 5;

  protected DataAdapter driver;
  protected IOOperation op;

  private Properties props;

  public GbrowseAdapterGUI(IOOperation op) {
    this.op = op;

    chrTextBox   = new JTextField();
    startTextBox = new JTextField();
    endTextBox   = new JTextField();

    chrStartEndList = new JComboBox();

    chrLabel     = new JLabel("Chr");
    startLabel   = new JLabel("Start");
    endLabel     = new JLabel("End");

    chrStartEndLabel = new JLabel("History");

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
    chrStartEndVector = new Vector();

    if (props.getProperty("chrStartEndItems") != null) {
	int items = Integer.parseInt(props.getProperty("chrStartEndItems"));

	for(int i=0; i < items; i++) {
	    chrStartEndVector.addElement(props.getProperty("chrStartEndItem"+i));
	}
	
	chrStartEndList.removeActionListener(chrAction);
	chrStartEndList.setModel(new DefaultComboBoxModel(chrStartEndVector));
	chrStartEndList.addActionListener(chrAction);
    }
  }

  public Properties getProperties() {
    chrStartEndList.removeActionListener(chrAction);

    String selectedChrStartEnd = getSelectedChrStartEnd();
    chrStartEndVector.removeElement(selectedChrStartEnd);
    chrStartEndVector.insertElementAt(selectedChrStartEnd, 0);

    Properties out = new Properties();

    if (chrStartEndVector.size() > MAX_HISTORY_LENGTH)
	out.put("chrStartEndItems", MAX_HISTORY_LENGTH+"");
    else
	out.put("chrStartEndItems", chrStartEndVector.size()+"");


    for(int i=0; i < chrStartEndVector.size() && i < MAX_HISTORY_LENGTH; i++) {
      out.put("chrStartEndItem"+i, (String) chrStartEndVector.elementAt(i));
    }

    chrStartEndList.addActionListener(chrAction);
    return out;
  }


  public void buildGUI() {
    setPreferredSize(new Dimension(450,100));

    chrTextBox     .setPreferredSize(new Dimension(150,10));
    chrTextBox     .setEditable(true);
    startTextBox   .setPreferredSize(new Dimension(150,10));
    startTextBox   .setEditable(true);
    endTextBox     .setPreferredSize(new Dimension(150,10));
    endTextBox     .setEditable(true);
    chrStartEndList.setPreferredSize(new Dimension(150,20));
    chrStartEndList.setEditable(false);

    chrLabel        .setPreferredSize(new Dimension(130,12));
    startLabel      .setPreferredSize(new Dimension(130,12));
    endLabel        .setPreferredSize(new Dimension(130,12));
    chrStartEndLabel.setPreferredSize(new Dimension(130,12));

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

    Box proxyBox = new Box(BoxLayout.X_AXIS);
    proxyBox.add(Box.createHorizontalGlue());
    proxyBox.add(proxyButton);

    setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

    add(chrBox);
    add(startBox);
    add(endBox);
    add(chrStartEndBox);
    add(proxyBox);

  }

  public void setDataAdapter(DataAdapter driver) {
    this.driver = driver;
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
      return !(chrTextBox.getText()   == null || 
	       startTextBox.getText() == null || 
	       endTextBox.getText()   == null ||
               chrTextBox.getText().equals("") || 
	       startTextBox.getText().equals("") || 
	       endTextBox.getText().equals(""));

  }

  public Object doOperation(Object values) throws ApolloAdapterException {
    //Config.newDataAdapter(driver);

    //      GbrowseAdapter gba = (GbrowseAdapter)driver;

      if (op.equals(ApolloDataAdapterI.OP_READ_DATA)) {
        CurationSet out = ((ApolloDataAdapterI) driver).getCurationSet();
        return out;
      } else {
        return null;
      }
  
  }
}
