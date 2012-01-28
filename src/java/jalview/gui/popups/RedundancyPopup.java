package jalview.gui.popups;

import jalview.gui.*;
import jalview.gui.event.*;
import jalview.datamodel.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.net.*;
import java.util.*;

import javax.swing.*;

public class RedundancyPopup extends ThresholdPopup {

  public RedundancyPopup(JFrame parent, AlignViewport av, Controller c, String title, String label, int low, int high, int value) {
    super(parent,av,c,title,label,low,high,value);

    status.setText("Enter the redundancy threshold");
    gbc.fill = GridBagConstraints.NONE;

    gbc.insets = new Insets(20,20,20,20);
    add(tp,gb,gbc,0,0,2,1);
    add(status,gb,gbc,0,1,2,1);
    add(apply,gb,gbc,0,2,1,1);
    add(close,gb,gbc,1,2,1,1);

    setApplyAction(new RemoveRedundancyAction("remove redundancy",av,c));

    pack();
    show();
  }

  public class RemoveRedundancyAction extends ThresholdAction {
    public RemoveRedundancyAction(String name, AlignViewport av,Controller c) {
      super(name,av,c);
    }

    public void applyAction(ActionEvent evt) {
      int threshold = getThreshold();

      Vector del;

      if (Config.DEBUG) System.out.println(parent);
      if (parent instanceof AlignFrame) {
        AlignFrame af = (AlignFrame)parent;
        fireStatusEvent("Removing redundancy...");
  
        if (av.getSelection() != null && av.getSelection().size() != 0) {
          System.out.println("Sel = " + av.getSelection());
          if (av.getSelection().size() == 1) {
            fireStatusEvent("Removing redundancy...",StatusEvent.ERROR);
          } else {
            fireStatusEvent("Removing redundancy for " + av.getSelection().size() + " sequences");

            del = av.getAlignment().removeRedundancy(threshold,av.getSelection().asVector());
            for (int i=0; i < del.size(); i++) {
              if (av.getSelection().contains((DrawableSequence)del.elementAt(i))) {
                av.getSelection().removeElement((DrawableSequence)del.elementAt(i));
              }
            }
          }
        } else {
          System.out.println("Creating vector");
          Vector s = new Vector();
          int i=0;
          while(i < av.getAlignment().getHeight() &&  av.getAlignment().getSequenceAt(i) != null) {
            s.addElement( av.getAlignment().getSequenceAt(i));
            i++;
          }
          fireStatusEvent("Removing redundancy for " + s.size() + " sequences");

          del = av.getAlignment().removeRedundancy(threshold,s);
          for (int j=0; j < del.size(); j++) {
            fireStatusEvent("Removing sequence " + ((AlignSequenceI)del.elementAt(j)).getName());

            if (av.getSelection().contains((DrawableSequence)del.elementAt(j))) {
              av.getSelection().removeElement((DrawableSequence)del.elementAt(j));
            }
          }
        }
        fireStatusEvent("done");
        av.resetSeqLimits();
        controller.handleAlignViewportEvent(new AlignViewportEvent(this,av,AlignViewportEvent.DELETE));
      }
    }
  }
}
