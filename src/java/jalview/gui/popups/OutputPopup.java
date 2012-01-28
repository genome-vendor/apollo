package jalview.gui.popups;

import jalview.io.*;
import jalview.gui.*;
import jalview.gui.event.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.net.*;

import javax.swing.*;

public class OutputPopup extends Popup {
  JTextArea ta;
  JScrollPane sp;
  JLabel format;
  JComboBox f;

  public OutputPopup(JFrame parent,AlignViewport av,Controller c, String title) {
    super(parent,av,c,title);

    sp = new JScrollPane();
    ta = new JTextArea(10,60);
    f  = new JComboBox();

    ta.setFont(new Font("Courier",Font.PLAIN,10));
    format = new JLabel("Alignment format");

    for (int i = 0; i < FormatProperties.getFormats().size(); i++) {
      f.addItem((String)FormatProperties.getFormats().elementAt(i));
    }

    sp.add(ta);

    gbc.fill = GridBagConstraints.BOTH;
    gbc.weighty = 1.0;

    add(sp,    gb,gbc,0,0,2,2);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(format,gb,gbc,0,2,1,1);
    add(f,     gb,gbc,1,2,1,1);

    
    add(status,gb,gbc,0,3,2,2);


    setApplyAction(new AreaOutputAction("area output",av,c));

    pack();

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    setLocation((screenSize.width - getSize().width) / 2,
		(screenSize.height - getSize().height) / 2);


    show();
  }

  public class AreaOutputAction extends JalPopupAction {
    public AreaOutputAction(String name, AlignViewport av,Controller c) {
      super(name,av,c);
      addRequiredArg("format");
    }
    public void getArgsFromSource(Button but, ActionEvent evt) {
      super.getArgsFromSource(but,evt);
      putArg("format",(String)f.getSelectedItem());
    }
 
    public void updateActionState(ActionEvent evt) {
      super.updateActionState(evt);
 
      if (containsArg("format")) {
        f.setSelectedItem((String)getArg("format"));
      }
    }
    public void applyAction(ActionEvent evt) {
      if (FormatProperties.contains(getArg("format"))) {
        fireStatusEvent("Formatting...");
        String outStr = FormatAdapter.get(getArg("format").toUpperCase(),av.getAlignment().getSequences());
        ta.setText(outStr);
        fireStatusEvent("done");
      } else {
        fireStatusEvent("Not yet supported",StatusEvent.ERROR);
      }
    }
  }
}
