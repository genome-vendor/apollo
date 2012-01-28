package jalview.gui.popups;

import jalview.io.*;
import jalview.gui.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;

import javax.swing.*;

public class PostscriptPopup extends BrowsePopup {
  JLabel fontLabel;
  JComboBox font;
  JLabel fontsizeLabel;
  JComboBox fontSize;
  JLabel orientLabel;
  JComboBox orient;
  JLabel sizeLabel;
  JComboBox size;
  OutputGenerator og;

  public PostscriptPopup(JFrame parent,AlignViewport av,Controller c,String title, OutputGenerator og) {
    super(parent,av,c,title);
    this.og = og;

    createInterface();
  }

  protected void createChoices() {
    fontLabel = new JLabel("Font");
    font = new JComboBox();
    font.addItem("Times-Roman");
    font.addItem("Courier");
    font.addItem("Helvetica");

    fontsizeLabel = new JLabel("Font size");
    fontSize = new JComboBox();
    fontSize.addItem("1");
    fontSize.addItem("2");
    fontSize.addItem("4");
    fontSize.addItem("6");
    fontSize.addItem("8");
    fontSize.addItem("10");
    fontSize.addItem("12");
    fontSize.addItem("14");
    fontSize.addItem("16");
    fontSize.addItem("20");
    fontSize.addItem("24");

    fontSize.setSelectedItem(new Integer(8).toString());

    orientLabel = new JLabel("Orientation");
    orient = new JComboBox();
    orient.addItem("Portrait");
    orient.addItem("Landscape");

    sizeLabel = new JLabel("Paper size");
    size = new JComboBox();
    size.addItem("A4");
    size.addItem("US letter");
    size.addItem("US letter small");
  }

  public void createInterface() {
    createChoices();
  }

  public abstract class PostscriptAction extends JalPopupAction {
    public PostscriptAction(String name, AlignViewport av,Controller c) {
      super(name,av,c);
      addRequiredArg("font");
      addRequiredArg("fsize");
      addRequiredArg("orientation");
      addRequiredArg("psize");
    }

    public void getArgsFromSource(Button but, ActionEvent evt) {
      super.getArgsFromSource(but,evt);
      putArg("font",(String)font.getSelectedItem());
      putArg("fsize",(String)fontSize.getSelectedItem());
      putArg("orientation",(String)orient.getSelectedItem());
      putArg("psize",(String)size.getSelectedItem());
    }
 
    public void updateActionState(ActionEvent evt) {
      super.updateActionState(evt);
 
      if (containsArg("font")) {
        font.setSelectedItem(getArg("font"));
      }
      if (containsArg("fsize")) {
        fontSize.setSelectedItem(getArg("fsize"));
      }
      if (containsArg("orientation")) {
        orient.setSelectedItem(getArg("orientation"));
      }
      if (containsArg("psize")) {
        size.setSelectedItem(getArg("psize"));
      }
    }
    
    public abstract void applyAction(ActionEvent evt);

    public void setPostscriptOptions() {
      og.getPostscriptProperties().font = getArg("font");
      og.getPostscriptProperties().fsize = (Integer.valueOf(getArg("fsize")).intValue());
 
      if (getArg("orientation").equals("Landscape")) {
        og.getPostscriptProperties().orientation = PostscriptProperties.LANDSCAPE;
      } else {
        og.getPostscriptProperties().orientation = PostscriptProperties.PORTRAIT;
      }
 
      if (getArg("psize").equals("US letter")) {
        og.getPostscriptProperties().width = 576;
        og.getPostscriptProperties().height = 776;
      } else if (getArg("psize").equals("US letter small")) {
        og.getPostscriptProperties().width = 552;
        og.getPostscriptProperties().height = 730;
      }
    }
  }
}
