package jalview.gui.popups;

import jalview.gui.*;
import jalview.gui.schemes.*;
import jalview.gui.event.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.net.*;

import javax.swing.*;

public abstract class ThresholdPopup extends Popup {
  ThresholdPanel tp;

  public ThresholdPopup(JFrame        parent, 
			AlignViewport av, 
			Controller    c, 
			String        title, 
			String        label, 
			int           low, 
			int           high, 
			int value) {

    super(parent,av,c,title);

    tp = new ThresholdPanel(parent,label,low,high, value);
  }


  public void setValue(int value) {
    tp.setValue(value);
  }


  public abstract class ThresholdAction extends JalPopupAction {
    public ThresholdAction(String name, AlignViewport av,Controller c) {
      super(name,av,c);
      addRequiredArg("threshold");
    }

    public void getArgsFromSource(Button but, ActionEvent evt) {
      int threshold = 0;
      super.getArgsFromSource(but,evt);
      try {
        threshold = Integer.valueOf(tp.getText()).intValue();
      } catch (Exception ex) {
        threshold = tp.getSBValue();
        tp.setText(threshold);
      }
      putArg("threshold",tp.getText());
    }
 
    public void updateActionState(ActionEvent evt) {
      super.updateActionState(evt);
 
      if (containsArg("threshold")) {
        tp.setText(getArg("threshold"));
      }
    }

    public abstract void applyAction(ActionEvent evt);

    protected int getThreshold() {

      int threshold = 0;

      try {
        //threshold = Integer.valueOf(getArg("threshold")).intValue();
	threshold = Integer.parseInt(tp.getText());
      } catch (Exception ex) {
        threshold = 0;
      }
      return threshold;
    }
  }
}
