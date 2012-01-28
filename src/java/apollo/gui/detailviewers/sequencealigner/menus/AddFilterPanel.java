package apollo.gui.detailviewers.sequencealigner.menus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeSet;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.gui.detailviewers.sequencealigner.MultiSequenceAlignerPanel;
import apollo.gui.detailviewers.sequencealigner.ReadingFrame;
import apollo.gui.detailviewers.sequencealigner.Strand;
import apollo.gui.detailviewers.sequencealigner.comparators.ComparatorFactory;
import apollo.gui.detailviewers.sequencealigner.comparators.ComparatorFactory.TYPE;
import apollo.gui.detailviewers.sequencealigner.filters.FilterFactory;

public class AddFilterPanel extends JOptionPane {
  
  private MultiSequenceAlignerPanel panel;
  private List<Object> values;
  private TYPES type;
  
  public enum TYPES { STRAND, FRAME, TYPE, SCORE, NAME };
  
  public AddFilterPanel(MultiSequenceAlignerPanel p, TYPES t) {
    super();
    this.panel = p;
    this.type = t;
    this.values = new ArrayList<Object>();
    
    this.reformat();
  }

  public void reformat() {
    
    this.setMessage("Choose a filter:");
    this.setMessageType(JOptionPane.PLAIN_MESSAGE);
    this.setOptionType(OK_CANCEL_OPTION);
    
    updateValues();
    this.setWantsInput(true);
    this.setSelectionValues(values.toArray());
    this.setInitialSelectionValue(values.get(0));
    
    this.selectInitialValue();


    this.revalidate();
    this.repaint();
  }
  
  public JDialog createDialog() {
    return this.createDialog(panel.getParent(), "Filter Alignment Dialog");
  }

  private void updateValues() {
    values.clear();
    switch(type) {
    case FRAME:
      values.add(FilterFactory.makeInverse(
          FilterFactory.makeFilter(ReadingFrame.ONE)));
      values.add(FilterFactory.makeInverse(
          FilterFactory.makeFilter(ReadingFrame.TWO)));
      values.add(FilterFactory.makeInverse(
          FilterFactory.makeFilter(ReadingFrame.THREE)));
      return;
    case STRAND:
      values.add(FilterFactory.makeInverse(
          FilterFactory.makeFilter(Strand.FORWARD)));
      values.add(FilterFactory.makeInverse(
          FilterFactory.makeFilter(Strand.REVERSE)));
      return;
    case TYPE:
      TreeSet<String> types = new TreeSet<String>();
      
      Enumeration e = Config.getPropertyScheme().getTypes();
      while (e.hasMoreElements()) {
        String type = (String) e.nextElement();
        types.add(type.toUpperCase());
      }
      
      for (String type : types) {
      values.add(FilterFactory.makeInverse(
          FilterFactory.makeFilterDisplayType(type)));
      }
      return;
    }
    
  }
}
