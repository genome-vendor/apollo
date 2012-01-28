package apollo.gui.menus;

import apollo.config.Config;
import apollo.gui.*;
import apollo.gui.annotinfo.FeatureEditorDialog;
import apollo.gui.synteny.GuiCurationState;

import java.awt.Point;
import java.awt.Color;
//import java.util.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.*;

import apollo.datamodel.*;
import apollo.gui.featuretree.*;

public class TreeMenu extends JPopupMenu implements ActionListener {

  FeatureTreePanel   ftp;
  JMenuItem          show;
  JMenuItem          hide;
  JMenuItem          edit;
  Point              pos;
  AnnotatedFeatureI annot = null;
  private GuiCurationState curationState;

  public TreeMenu(FeatureTreePanel ftp, Point pos, GuiCurationState curationState) {
    super("Tree operations");
    this.ftp  = ftp;
    this.pos  = pos;
    this.curationState = curationState;

    DefaultMutableTreeNode dmtn = this.ftp.getSelectedNode();
    if (dmtn != null) {
      if (dmtn.getUserObject() instanceof AnnotatedFeatureI) {
        this.annot = (AnnotatedFeatureI)dmtn.getUserObject();
      }
    } else {
      this.annot = null;
    }
    menuInit();
  }

  public void menuInit() {
    show = new JMenuItem("Show annotation");
    hide = new JMenuItem("Hide annotation");
    if (Config.isEditingEnabled())
      edit             = new JMenuItem("Annotation info editor...");
    else
      edit             = new JMenuItem("Annotation info...");

    if (Config.isEditingEnabled()) {
      add(edit);
    }

    // hide and show not yet implemented--commenting out for now
    //    add(hide);
    //    add(show);

    hide .addActionListener(this);
    show .addActionListener(this);
    edit .addActionListener(this);

    hide .setMnemonic('H');
    show .setMnemonic('S');
    edit .setMnemonic('E');

    hide.setEnabled(false);
    show.setEnabled(false);
    if (annot == null) {
      edit.setEnabled(false);
    }

    hide .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H,
                         ActionEvent.SHIFT_MASK));
    show .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                         ActionEvent.SHIFT_MASK));
    edit .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
                         ActionEvent.SHIFT_MASK));
  }

  public void actionPerformed(ActionEvent e) {

    if (e.getSource() == hide) {
      System.out.println("hide annotation");
    } else if (e.getSource() == show) {
      System.out.println("show annotation");
    } else if (e.getSource() == edit) {
      FeatureEditorDialog.showTextEditor(annot,getCurationState());
    }
      //      System.out.println("edit annotation");
//       if (ftp.getFeatureTreeFrame() != null && ftp.getFeatureTreeFrame().getApolloFrame() != null) {
// 	// ??
// 	if (annot instanceof Transcript)
// 	  ftp.getFeatureTreeFrame().getApolloFrame().showTextEditor (annot, (Transcript)annot);
// 	else
// 	  ftp.getFeatureTreeFrame().getApolloFrame().showTextEditor (annot, null);
//       }
//    }
  }
  
  private GuiCurationState getCurationState() { return curationState; }
}
