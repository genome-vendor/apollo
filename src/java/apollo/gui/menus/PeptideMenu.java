package apollo.gui.menus;

import apollo.config.Config;
import apollo.config.PeptideStatus;
import apollo.gui.*;
import apollo.gui.event.*;
import apollo.gui.synteny.GuiCurationState;
import apollo.datamodel.*;
import apollo.editor.AnnotSessionDoneEvent;
import apollo.editor.FeatureChangeEvent;
import apollo.dataadapter.*;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Hashtable;
import java.util.Enumeration;

public class PeptideMenu extends JMenu implements ActionListener {
  private AnnotatedFeatureI gene;
  //private Controller controller;
  private GuiCurationState curationState;

  public PeptideMenu(GuiCurationState curState) {
    super();
    curationState = curState;
    //curationState.getController().addListener(this);
  }

  public void setGene(AnnotatedFeatureI g) {
    this.gene = g;
    if (g == null)
      return;

    if (!g.isProteinCodingGene()) {
      setText("NOT A PEPTIDE");
      setEnabled (false);
    } else {
      setEnabled (true);

      Hashtable curator_values = Config.getPeptideStates();

      String pep_status = g.getProperty ("sp_status");
      PeptideStatus this_status = Config.getPeptideStatus (pep_status);

      JMenuItem item;

      /* these are the computed values that cannot be over-ridden
         by a curator
      */
      item = new JMenuItem (this_status.getText());
      add (item);
      item.addActionListener(this);
      /* these are the possible values to be selected
         by a curator */
      Enumeration e = curator_values.keys();
      while (e.hasMoreElements()) {
        String key = (String) e.nextElement();
        PeptideStatus a_status
        = (PeptideStatus) curator_values.get (key);
        if (a_status.getPrecedence() >= this_status.getPrecedence() &&
            !a_status.getText().equals (this_status.getText()) &&
            a_status.getCurated()) {
          item = new JMenuItem (a_status.getText());
          add (item);
          item.addActionListener(this);
        }
      }
      setText (this_status.getText());
    }
  }

//   public void setController(Controller c) {
//     controller = c;
//     controller.addListener(this);
//   }

  private Controller getController() {
    return curationState.getController();
  }

  void clear() {
    //getController().removeListener(this);
    gene = null;
  }

  public void actionPerformed (ActionEvent e) {
    JMenuItem item = (JMenuItem) e.getSource();
    // This should probably be done in AnnotationEditor changeSpStatus(gene,item...
    gene.addProperty ("sp_status", item.getText());
    //AnnotationChangeEvent ace = AnnotationChangeEvent.getSessionDoneEvent(this);
    AnnotSessionDoneEvent de = new AnnotSessionDoneEvent(this);
//     = new AnnotationChangeEvent(this,
//                                 gene,
//                                 FeatureChangeEvent.REDRAW,
//                                 AnnotationChangeEvent.ANNOTATION,
//                                 gene);
    getController().handleAnnotationChangeEvent(de);
  }
}

