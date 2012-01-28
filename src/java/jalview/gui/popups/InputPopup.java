package jalview.gui.popups;

import jalview.datamodel.*;
import jalview.gui.*;
import jalview.gui.event.*;
import jalview.gui.menus.*;
import jalview.io.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class InputPopup extends Popup {
  JTextArea ta;
  JLabel format;
  JComboBox f;

  public InputPopup(JFrame parent,AlignViewport av,Controller c,String title) {
    super(parent,av,c,title);

    ta = new JTextArea(10,60);
    ta.setFont(new Font("Courier",Font.PLAIN,10));
    format = new JLabel("Alignment format");
    
    f = new JComboBox();

    for (int i = 0; i < FormatProperties.getFormats().size(); i++) {
      f.addItem((String)FormatProperties.getFormats().elementAt(i));
    }

    gbc.fill = GridBagConstraints.BOTH;
    gbc.weighty = 1.0;

    add(ta,    gb,gbc,0,0,2,2);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(format,gb,gbc,0,2,1,1);
    add(f,     gb,gbc,1,2,1,1);

    
    add(status,gb,gbc,0,3,2,2);

    setApplyAction(new InputBoxAction("align box",av,c));

    pack();

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    setLocation((screenSize.width - getSize().width) / 2,
		(screenSize.height - getSize().height) / 2);



    show();
  }

  public class InputBoxAction extends JalPopupAction {
    public InputBoxAction(String name,AlignViewport av,Controller c) {
      super(name,av,c);
      addRequiredArg("format");
    }

    public void getArgsFromSource(JButton but, ActionEvent evt) {
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
      AlignSequenceI[] s = null;
      String inStr = ta.getText();
      if (FormatProperties.contains(getArg("format"))) {
        s = FormatAdapter.read(getArg("format"),inStr);
      } else {
        fireStatusEvent("Format not supported",StatusEvent.ERROR);
      }
      if (s != null) {
        AlignFrame af = new AlignFrame(parent.getParent(),new Alignment(s));
        int size = av.getAlignment().getSequences().size();
        System.out.println("Size = " + size);
        if (size > 0) {
          af.resize(700,500);
          af.show();
//          ConsThread ct = new ConsThread(af);
//          ct.start();
          fireStatusEvent("done");
          fireJalActionEvent(new JalActionEvent(this,this,JalActionEvent.DONE));
        } else {
          af.dispose();
          fireStatusEvent("Can't read input - check format",StatusEvent.ERROR);
        }
      } else {
        fireStatusEvent("No sequences found",StatusEvent.ERROR);
      }
    }
  }
}
