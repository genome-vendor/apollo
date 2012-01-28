package jalview.gui.popups;

import jalview.io.*;
import jalview.gui.*;
import jalview.gui.event.*;
import jalview.gui.menus.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;

import javax.swing.*;

public class MailPostscriptPopup extends PostscriptPopup {
  JTextField tfm;
  JLabel tfmLabel;
  Mail mail;

  public MailPostscriptPopup(JFrame parent,AlignViewport av,Controller c,String title, OutputGenerator og) {
    super(parent,av,c,title,og);
    setApplyAction(new MailPostscriptAction("mail postscript",av,c));
  }

  public void createInterface() {
    super.createInterface();

    tfm = new JTextField(40);
    tfmLabel = new JLabel("Mail address : ");
    //tfm.setText(og.getMailProperties().address);

    gbc.fill = GridBagConstraints.NONE;

    gbc.insets = new Insets(10,10,10,10);

    add(tfmLabel,gb,gbc,0,0,1,1);
    add(tfm,gb,gbc,1,0,4,1);

    add(fontLabel,gb,gbc,0,1,1,1);
    add(font,gb,gbc,1,1,1,1);

    add(fontsizeLabel,gb,gbc,0,2,1,1);
    add(fontSize,gb,gbc,1,2,1,1);
    add(orientLabel,gb,gbc,0,3,1,1);
    add(orient,gb,gbc,1,3,1,1);

    add(sizeLabel,gb,gbc,0,4,1,1);
    add(size,gb,gbc,1,4,1,1);

    add(status,gb,gbc,0,5,1,1);
    add(apply,gb,gbc,0,6,1,1);
    add(close,gb,gbc,1,6,1,1);

    this.pack();
    this.show();
  }

  public class MailPostscriptAction extends PostscriptAction {
    public MailPostscriptAction(String name, AlignViewport av,Controller c) {
      super(name,av,c);
      addRequiredArg("address");
    }

    public void getArgsFromSource(Button but, ActionEvent evt) {
      super.getArgsFromSource(but,evt);
      putArg("address",tfm.getText());
    }
 
    public void updateActionState(ActionEvent evt) {
      super.updateActionState(evt);
 
      if (containsArg("address")) {
        tfm.setText(getArg("address"));
      }
    }
    
    public void applyAction(ActionEvent evt) {
      String address = getArg("address");

      fireStatusEvent("Checking email address");

      // Do some checks on the mail address
      if (address.indexOf('@') == -1) {
        fireStatusEvent("Invalid mail address (enter name@my.email.server )",StatusEvent.ERROR);
        fireJalActionEvent(new JalActionEvent(this,this,JalActionEvent.DONE));
      } else {
        fireStatusEvent("done");
  
        fireStatusEvent("Connecting to mail server");
  
        // Send to the recipient running the applet
        String recipient = address;
        String author = "michele@ebi.ac.uk";
        String subject = "Jalview alignment";
        String text = "";
  
        try {
          //mail = new Mail(og.getMailProperties().server,recipient,author,subject);
  
          //fireStatusEvent("Mail Server = " + og.getMailProperties().server);

          //mail.send(text);
  
          //sendText();
          //mail.finish();
        } catch (Exception e) {
          e.printStackTrace();
        }
  
        fireJalActionEvent(new JalActionEvent(this,this,JalActionEvent.DONE));
      }
    }

    protected void sendText() throws Exception {
      // Set the options
      setPostscriptOptions();
  
      // Now generate postscript
      fireStatusEvent("Generating postscript...(this takes a while)");

      mail.send(og.getPostscript().toString());
  
      fireStatusEvent("done");
    }
  }
}
