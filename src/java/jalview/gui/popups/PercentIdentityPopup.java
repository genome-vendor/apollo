package jalview.gui.popups;

import jalview.gui.*;
import jalview.gui.schemes.*;
import jalview.gui.event.*;


import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.net.*;

import javax.swing.*;

public class PercentIdentityPopup extends ThresholdPopup {

  public PercentIdentityPopup(JFrame parent, AlignViewport av, Controller c, String title, String label, int low, int high, int value) {

    super(parent,av,c,title,label,low,high,value);

    status.setText("Enter the threshold above which to colour sequences");
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.0;

    gbc.insets = new Insets(20,20,20,20);
    add(tp,gb,gbc,0,0,2,1);
    add(status,gb,gbc,0,1,2,1);

    setApplyAction(new PercentIdentityAction("percent identity",av,c));

    pack();
    setSize(400,300);

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    setLocation((screenSize.width - getSize().width) / 2,
		(screenSize.height - getSize().height) / 2);

    show();




  }

  public class PercentIdentityAction extends ThresholdAction {
    public PercentIdentityAction(String name, AlignViewport av,Controller c) {
      super(name,av,c);
    }

    public void applyAction(ActionEvent evt) {
      int threshold = getThreshold();
      av.setThreshold(threshold);
      controller.handleAlignViewportEvent(new AlignViewportEvent(this,av,AlignViewportEvent.THRESHOLD));
    }
  }
}
