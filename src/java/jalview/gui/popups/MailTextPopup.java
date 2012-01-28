package jalview.gui.popups;

import jalview.io.*;
import jalview.gui.*;
import jalview.gui.event.*;
import jalview.gui.menus.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;

import javax.swing.*;

public class MailTextPopup extends Popup {
  JTextField tf;
  JLabel tfLabel;
  Mail mail;
  JLabel format;
  JComboBox f;
  OutputGenerator og;

  public MailTextPopup(JFrame parent, AlignViewport av, Controller c,String title, OutputGenerator og) {
    super(parent,av,c,title);
    this.og = og;

    setApplyAction(new MailTextAction("mail alignment",av,c));
    tf = new JTextField(40);
    tf.setText(og.getMailProperties().address);

    tfLabel = new JLabel("Mail address : ");

    format = new JLabel("Alignment format");
    f = new JComboBox();

    for (int i = 0; i < FormatProperties.getFormats().size(); i++) {
      f.addItem((String)FormatProperties.getFormats().elementAt(i));
    }

    gbc.fill = GridBagConstraints.NONE;

    gbc.insets = new Insets(20,20,20,20);

    add(tfLabel,gb,gbc,0,0,1,1);
    add(tf,gb,gbc,1,0,4,1);
    add(format,gb,gbc,0,1,1,1);
    add(f,gb,gbc,1,1,1,1);
    add(status,gb,gbc,0,2,5,1);
    add(apply,gb,gbc,0,3,1,1);
    add(close,gb,gbc,1,3,1,1);

    this.pack();
    this.show();
  }

  public class MailTextAction extends JalPopupAction {
    public MailTextAction(String name, AlignViewport av, Controller c) {
      super(name,av,c);
      addRequiredArg("address");
      addRequiredArg("format");
    }
    public void getArgsFromSource(Button but, ActionEvent evt) {
      super.getArgsFromSource(but,evt);
      putArg("address",(String)tf.getText());
      putArg("format",(String)f.getSelectedItem());
    }
 
    public void updateActionState(ActionEvent evt) {
      super.updateActionState(evt);
 
      if (containsArg("format")) {
        f.setSelectedItem(getArg("format"));
      }
      if (containsArg("address")) {
        tf.setText(getArg("address"));
      }
    }
    public void applyAction(ActionEvent evt) {
      String address = getArg("address");
  
      // Do some checks on the mail address
      if (address.indexOf('@') == -1) {
        fireStatusEvent("Invalid mail address (enter name@my.email.server )",StatusEvent.ERROR);
        fireJalActionEvent(new JalActionEvent(this,this,JalActionEvent.DONE));
      } else {
        System.out.println("Mail address " + address) ;
        og.getMailProperties().address = address;
  
  
        //mail = new Mail();
        // Send to the recipient running the applet
        String recipient = og.getMailProperties().address;
        String author = "michele@ebi.ac.uk";
        String subject = "Jalview alignment";
        String text = "";

        try {
          if (og.getMailProperties().server != null && !(og.getMailProperties().server.equals(""))) {
            System.out.println("Sending from server " + og.getMailProperties().server + " to " + recipient + " from " + author + " about " + subject);
            mail = new Mail(og.getMailProperties().server,recipient,author,subject);
  
            sendText();
  
            mail.finish();
  
            // Send a copy to me
            java.net.InetAddress ia = null;
            String pog = "";
            try  {
              ia = java.net.InetAddress.getLocalHost();
              pog = ia.getHostName();
            }  catch( Exception e ) {
              System.err.println( e.toString() );
            }
            //          subject = "Jalview sent to " + og.getMailProperties().address + " (" + pog + ")";
            //          System.out.println("Mail server = " + og.getMailProperties().server);
            //          mail = new Mail(og.getMailProperties().server,"michele@ebi.ac.uk",author,subject);
            //          //mail.send(og.getMailProperties().server,"michele@ebi.ac.uk",author,subject,text);
            //
            //          sendText();
            //
            //          mail.finish();
            fireStatusEvent("done");
          }
  
        } catch (Exception e) {
          fireStatusEvent("Failed sending mail. Error " + e,StatusEvent.ERROR);
        }
        //  mail.finish();
        fireJalActionEvent(new JalActionEvent(this,this,JalActionEvent.DONE));
      }
    }
    public void sendText() {
      String text = "";
  
      text = og.getText(getArg("format"));
      mail.send(text);
    }
  }
}
