package apollo.gui;

import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import javax.swing.*;
import apollo.gui.*;
import apollo.config.*;

public class TableColumnChooser extends JPanel implements ActionListener {
  ArrayListTransferHandler arrayListHandler;
  FeatureProperty featureProperty;

  DefaultListModel includedProps, excludedProps;

  public TableColumnChooser(FeatureProperty fp) {
    arrayListHandler = new ArrayListTransferHandler();
    JList list1, list2;
    JButton applyButton;

    featureProperty = fp;

    includedProps = new DefaultListModel();
    excludedProps = new DefaultListModel();

    dividePropertyStrings();

    list1 = new JList(excludedProps);
    list1.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    list1.setTransferHandler(arrayListHandler);
    list1.setDragEnabled(true);
    JScrollPane list1View = new JScrollPane(list1);
    list1View.setPreferredSize(new Dimension(200, 400));
    JLabel header1 = new JLabel("Other columns");
    JPanel panel1 = new JPanel();
    panel1.setLayout(new BorderLayout());
    panel1.add(header1, BorderLayout.NORTH);
    panel1.add(list1View, BorderLayout.CENTER);
    panel1.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

    list2 = new JList(includedProps);
    list2.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    list2.setTransferHandler(arrayListHandler);
    list2.setDragEnabled(true);
    JScrollPane list2View = new JScrollPane(list2);
    list2View.setPreferredSize(new Dimension(200, 400));
    JLabel header2 = new JLabel("Displayed columns");
    JPanel panel2 = new JPanel();
    panel2.setLayout(new BorderLayout());
    panel2.add(header2, BorderLayout.NORTH);
    panel2.add(list2View, BorderLayout.CENTER);
    panel2.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

    applyButton = new JButton("Apply");

    applyButton.addActionListener(this);

    setLayout(new BorderLayout());
    JLabel info = new JLabel("Drag items between lists to add/remove and within list to reorder columns");
    add(info, BorderLayout.NORTH);
    add(panel1, BorderLayout.LINE_START);
    add(panel2, BorderLayout.LINE_END);
    add(applyButton, BorderLayout.SOUTH);
    setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
  }

  public void actionPerformed(ActionEvent e) {

    Vector columns = new Vector();

    Enumeration includedPropsEnum = includedProps.elements();
    while(includedPropsEnum.hasMoreElements()) {
      String name = (String)includedPropsEnum.nextElement();
      //System.out.println("column= " + name);
      columns.addElement(new ColumnProperty(name));
    }
    
    featureProperty.setColumns(columns);
  }

  private void dividePropertyStrings() { 
    Enumeration allColumnNameEnum = DetailInfo.getAllPropertyStrings();
    Vector includedColumns = featureProperty.getColumns();

    HashSet includedColumnNames = new HashSet();
    for (int i=0;i<includedColumns.size();i++) {
      ColumnProperty cp = (ColumnProperty)includedColumns.elementAt(i);
      includedColumnNames.add(cp.getHeading());
    }
    

    while(allColumnNameEnum.hasMoreElements()) {
      String name = (String)allColumnNameEnum.nextElement();
      if (!includedColumnNames.contains(name)) {
        //System.out.println("Excluded: " + name);
        excludedProps.addElement(name);
      }
    }

    // Add them separately so they are in order
    Iterator incIter = includedColumns.iterator();
    while(incIter.hasNext()) {
      String name = ((ColumnProperty)incIter.next()).getHeading();
      //System.out.println("Included: " + name);
      includedProps.addElement(name);
    }
  }
}
