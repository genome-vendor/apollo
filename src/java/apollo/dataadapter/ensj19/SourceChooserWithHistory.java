package apollo.dataadapter.ensj19;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.GridBagLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.util.*;
import java.io.*;

import apollo.util.GuiUtil;

/**
 * Presents a textfield and combobox: a choice
 * of combobox item is moved into the text field.
 * When asked for history, the current choice of text field
 * is appended to the list already held by the combobox
**/
public class SourceChooserWithHistory extends JPanel{
  
  private JComboBox sourceHistoryList;
  private Vector sourceHistory = new Vector();
  private ItemListener historyListAction;
  private java.util.List listenerList = new ArrayList();

  private class HistoryAction implements ItemListener{
    public void itemStateChanged(ItemEvent event){
      Iterator listeners = getListenerList().iterator();
      while(listeners.hasNext()){
        ((ItemListener)listeners.next()).itemStateChanged(event);
      }
    }//end actionPerformed
  }
  
  public SourceChooserWithHistory(){
    sourceHistoryList = new JComboBox();
    sourceHistoryList.setPreferredSize(new Dimension(200,15));
    sourceHistoryList.setEditable(true);

    setLayout(new GridBagLayout());
    add(sourceHistoryList, GuiUtil.makeConstraintAt(0,0,1));    
  }//end SourceChooserWithHistory

  public SourceChooserWithHistory(Vector sourceHistory){
    this();
    DefaultComboBoxModel model = new DefaultComboBoxModel(sourceHistory);
    sourceHistoryList.setModel(model);
    historyListAction = new HistoryAction();
    sourceHistoryList.addItemListener(historyListAction);
  }//end SourceChooserWithHistory
  
  private ItemListener getHistoryListAction(){
    return historyListAction;
  }//end getHistoryListAction
  
  public JComboBox getSourceHistoryList() {
    return this.sourceHistoryList;
  }//end getSourceHistoryList
  
  public void setSourceHistoryList(JComboBox sourceHistoryList) {
    this.sourceHistoryList = sourceHistoryList;
  }//end setSourceHistoryList
  
  public Vector getSourceHistory() {
    return this.sourceHistory;
  }//end getSourceHistory
  
  public void setSourceHistory(Vector sourceHistory) {
    this.sourceHistory = sourceHistory;
    getSourceHistoryList().removeItemListener(getHistoryListAction());
    getSourceHistoryList().setModel(new DefaultComboBoxModel(sourceHistory));
    getSourceHistoryList().addItemListener(getHistoryListAction());
  }//end setSourceHistory
  
  public String getSelectedSource(){
    return (String)getSourceHistoryList().getSelectedItem();
  }//end getSelectedSource
  
  public static void main(String[] args){
    JFrame testFrame = new JFrame("test");
    Vector history = new Vector();
    history.add("One");
    history.add("Two");
    testFrame.getContentPane().add(
      new SourceChooserWithHistory(
        history
      ), 
      BorderLayout.CENTER
    );
    testFrame.pack();
    testFrame.show();
  }
  
  private List getListenerList(){
    return listenerList;
  }//end getListenerList
  
  public void addItemListener(ItemListener listener){
    getListenerList().add(listener);
  }//end addActionListener
}
