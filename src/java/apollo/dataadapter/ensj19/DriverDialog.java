package apollo.dataadapter.ensj19;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.net.*;
import java.awt.event.*;
import java.util.*;

import apollo.datamodel.*;

import org.ensembl19.gui.*;


public class DriverDialog extends JDialog implements ActionListener {

  boolean useCustom = false;

  private DriverConfigPanel seqDriverConfigPanel;

  JCheckBox  customBox;
  JButton    ok;
  JButton    cancel;

  Properties stateInformation;

  public DriverDialog(JFrame frame, Properties props) {
    super(frame,"Sequence DB settings",true);

    
    stateInformation = props;

    init();

    if (frame==null) {
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      setLocation((screenSize.width - getSize().width) / 2,
                  (screenSize.height - getSize().height) / 2); 
    }
  }

  public void actionPerformed(ActionEvent evt) {
    if (evt.getSource() == customBox) {
      useCustom = customBox.isSelected();
    } else if (evt.getSource() == ok) {
      commitIfComplete();
    } else if (evt.getSource() == cancel) {
      hide();
      dispose();
    }
  }

  private void commitIfComplete() {
    if (customBox.isSelected()) {
      String seqDriverConfFiles = seqDriverConfigPanel.getServer() + ":" +
                               seqDriverConfigPanel.getDriver();
      stateInformation.put("seqDriverConfFiles",seqDriverConfFiles);
      System.out.println("Driver string = " + seqDriverConfFiles);
    } else {
    }
    hide();
    dispose();
  }

  public void init() {
    
    seqDriverConfigPanel = new DriverConfigPanel();

    customBox  = new JCheckBox();
    customBox.setSelected(useCustom);
    customBox.addActionListener(this);

    JPanel mainP = new JPanel();
    mainP.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    //gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.gridwidth = 1;
    mainP.add(new JLabel("Use custom database"),gbc);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    mainP.add(customBox,gbc);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    mainP.add(seqDriverConfigPanel,gbc);

    JPanel enclosureP = new JPanel();
    enclosureP.setLayout(new BorderLayout());
    enclosureP.add(mainP,BorderLayout.CENTER);

    ok = new JButton("OK");
    ok.addActionListener(this);
    cancel = new JButton("Cancel");
    cancel.addActionListener(this);
    JPanel buttonP = new JPanel();
    buttonP.add(ok);
    buttonP.add(cancel);
    
    getContentPane().add(enclosureP,BorderLayout.CENTER);
    getContentPane().add(buttonP,BorderLayout.SOUTH);
    pack();
  }

  public static void main(String [] args) {
    final JFrame frame = new JFrame();
    JButton show = new JButton("Show");
    JButton set = new JButton("Set");

    show.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        DriverDialog dd = new DriverDialog(frame, new Properties());
        dd.setVisible(true);
      }  
    });

    set.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
      }  
    });
       
    frame.getContentPane().setLayout(new GridLayout(2,1));
    frame.getContentPane().add(show);
    frame.getContentPane().add(set);
    frame.pack();
    frame.setVisible(true);
  }
}
