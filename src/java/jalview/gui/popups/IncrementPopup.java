package jalview.gui.popups;

import jalview.gui.*;
import jalview.datamodel.*;
import jalview.gui.schemes.*;
import jalview.gui.event.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.net.*;

import javax.swing.*;

public class IncrementPopup extends ThresholdPopup {

  public IncrementPopup(JFrame parent, AlignViewport av, Controller c,String title, String label, int low, int high, int value) {
    super(parent,av,c,title,label,low,high,value);

    setValue(findIncrement());

    status.setText("Enter the step by which to decrease colour intensity");
    gbc.fill = GridBagConstraints.NONE;

    gbc.insets = new Insets(20,20,20,20);

    add(tp,gb,gbc,0,0,2,1);
    add(status,gb,gbc,0,1,2,1);
    add(apply,gb,gbc,0,2,1,1);
    add(close,gb,gbc,1,2,1,1);

    setApplyAction(new IncrementAction("percent identity",av,c));

    pack();
    show();
  }

  public class IncrementAction extends ThresholdAction {
    public IncrementAction(String name, AlignViewport av,Controller c) {
      super(name,av,c);
    }

    public void applyAction(ActionEvent evt) {
      int increment = getThreshold();

      av.setIncrement(increment);
      for (int j=0; j < av.getAlignment().getGroups().size(); j++) {
        SequenceGroup sg =  ((SequenceGroup)av.getAlignment().getGroups().elementAt(j));

	//        if (sg.colourScheme instanceof ConservationColourScheme) {
        //  ((ConservationColourScheme)sg.colourScheme).setInc(av.getIncrement());
        //  sg.colourScheme.setColours(sg);
        //  fireStatusEvent("Changing group colour increment");
        //}
      }
      controller.handleAlignViewportEvent(new AlignViewportEvent(this,av,AlignViewportEvent.COLOURING));
    }

  }
  protected int findIncrement() {

    for (int j=0; j < av.getAlignment().getGroups().size(); j++) {
      SequenceGroup sg =  ((SequenceGroup)av.getAlignment().getGroups().elementAt(j));
//        if (sg.colourScheme instanceof ConservationColourScheme) {
//          av.setIncrement(((ConservationColourScheme)sg.colourScheme).getInc());
//          return ((ConservationColourScheme)sg.colourScheme).getInc();
//        }
    }
    return 30;
  }
}






