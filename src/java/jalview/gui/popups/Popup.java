package jalview.gui.popups;

import jalview.gui.*;
import jalview.gui.menus.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * This is the base of the Popup dialog hierarchy. 
 */
public class Popup extends JDialog implements JalActionListener {
  JFrame parent;

  AlignViewport av;
  Controller controller;

  JPanel doingPane;
  JPanel applyPane;

  JButton apply;
  JButton close;

  PopupStatusLabel status;

  GridBagLayout gb;
  GridBagConstraints gbc;

  PopupActionListener pal;
  BasicWindowListener bwl;

  JalPopupAction applyAction  = null;


  /**
   * Sets basic layout properties, sets the layout manager, and creates the
   * standard buttons (close and apply) and labels (status), and shows it.
   * @param parent The parent frame of this popup.
   * @param title  The title for this popup
   */
  public Popup(JFrame parent, AlignViewport av, Controller c,String title) {
    super(parent,title,false);
    this.parent = parent;
    this.av = av;
    this.controller = c;

    doingPane = new JPanel();
    applyPane = new JPanel();
    apply     = new JButton("Apply");
    close     = new JButton("Cancel");

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add("Center",doingPane);
    getContentPane().add("South",applyPane);

    gb  = new GridBagLayout();
    gbc = new GridBagConstraints();

    gbc.fill = GridBagConstraints.NONE;
    gbc.insets = new Insets(10,10,10,10);
    applyPane.setLayout(gb);
    
    add(apply,gb,gbc,0,0,1,1,applyPane);
    add(close,gb,gbc,1,0,1,1,applyPane);

    doingPane.setLayout(gb);

    status = new PopupStatusLabel("Status:",Label.LEFT);
    status.setHorizontalAlignment(Label.LEFT);

    pal = new PopupActionListener(this);
    bwl = new BasicWindowListener(this);
  }

  public void setApplyAction(JalPopupAction jpa) {
    if (apply != null) {
      if (applyAction != null) {
        if (av != null && av.getCommandLog() != null) {
          av.getCommandLog().remove(applyAction);
        }
        applyAction.removeListener(this);
        apply.removeActionListener(applyAction);
        applyAction.removeListener(status);
      }
      apply.addActionListener(jpa);
      if (av != null && av.getCommandLog() != null) {
        av.getCommandLog().add(jpa);
      }
      this.applyAction = jpa;
      applyAction.addListener(this);
      if (contains(status)) {
        applyAction.addListener(status);
      }
    }
  }

  protected boolean contains(Component comp) {
    Component [] comps = getComponents();
    for (int i=0; i<comps.length; i++) {
      if (comps[i] == comp) {
        return true;
      }
    }
    return false;
  }

  public void handleJalAction(JalActionEvent evt) {
    //System.out.println("handleJalAction type = " + evt.getActionType());
    if (evt.getActionType() == JalActionEvent.DONE) {
      this.hide();
      this.dispose();
    }
  }

  public void addBasic() {
      applyPane.add(apply,gbc);
      applyPane.add(close,gbc);
      doingPane.add(status,gbc);
    pack();
    validate();
    show();
  }

  class PopupActionListener implements ActionListener {
    Popup p;
    public PopupActionListener(Popup p) {
      this.p = p;
      apply.addActionListener(this);
      close.addActionListener(this);
    }
    public void actionPerformed(ActionEvent evt) {
      if (evt.getSource() == apply) {
        applyAction(evt);
      } else if (evt.getSource() == close) {
        closeAction(evt);
      } else {
        otherAction(evt);
      }
    }
  }

  /**
   * The method called when the apply button is pressed.
   */
  protected void applyAction(ActionEvent evt) {
    if (applyAction == null) {
       System.out.println("Default Popup applyAction - no functionality. You should do a setApplyAction\n");
    }
  }

  protected void closeAction(ActionEvent evt) {
    this.hide();
    this.dispose();
  }

  protected void otherAction(ActionEvent evt) {
    System.out.println("Default Popup otherAction - no functionality - override in your class\n");
  }

  class BasicWindowListener extends WindowAdapter {
    Popup p;
    public BasicWindowListener(Popup p) {
      this.p = p;
      p.addWindowListener(this);
    }
    public void windowClosing(WindowEvent evt) {
      if (Config.DEBUG) System.out.println("Calling windowClosing");
      status.setText("Closing window...");
      status.validate();
      p.hide();
      p.dispose();
    }
  }

  /**
   * add a component to the popup, with specified constraints.
   * @param c   The component to add
   * @param gbl The layout manager
   * @param gbc The constraints object (this is modified within the method
   * @param x   Becomes gbc.gridx
   * @param y   Becomes gbc.gridy
   * @param w   Becomes gbc.gridwidth
   * @param h   Becomes gbc.gridheight
   */
  public void add(Component c,GridBagLayout gbl, GridBagConstraints gbc,
                  int x, int y, int w, int h,JPanel p) {
    gbc.gridx = x;
    gbc.gridy = y;
    gbc.gridwidth = w;
    gbc.gridheight = h;
    gbl.setConstraints(c,gbc);
    p.add(c);
  }
  public void add(Component c,GridBagLayout gbl, GridBagConstraints gbc,
                  int x, int y, int w, int h) {
      add(c,gb,gbc,x,y,w,h,doingPane);
  }

  public void dispose() {
    super.dispose();
    if (Config.DEBUG) System.out.println("dispose called on " + this);
  }
}











