package jalview.gui.popups;

import jalview.io.*;
import jalview.gui.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;

import javax.swing.*;

public class MailTextPopupDriver extends Driver {
  public static void main(String[] args) {
     
    AlignFrame af = new AlignFrame(null,"lipase.msf","File","MSF");
    af.resize(700,300);
    af.show();
    MailTextPopup mp = new MailTextPopup((JFrame)af,af.getAlignViewport(),af.getController(),"Popup",(OutputGenerator)af);
  }
}
