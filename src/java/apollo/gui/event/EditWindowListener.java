package apollo.gui.event;


import java.awt.event.*;
import java.awt.Component;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import apollo.gui.EditWindowI;
import apollo.config.Config;

/** Move to annotinfo package. For annotinfo.FeatureEditorDialog. On window closing
 * checks if there are any edits of EditWindowI(FeatureEditorDialog) that need saving
 Actually this is no longer being used by the FED and should be deleted once its 
 confirmed that we are going with undo and getting rid of clone/replace/commit
 (this was only used by FED)
 */


public class EditWindowListener extends WindowAdapter {

  private static Object[] options = { "Close anyway",
                                      "Keep edits, then close",
                                      "Cancel" };

  private static String message = "Save your changes?";

  public void windowClosing(WindowEvent e) {
    boolean close_up = ! (e.getWindow() instanceof EditWindowI);
    EditWindowI editing_window = null;
    if (!close_up) {
      editing_window = (EditWindowI) e.getWindow();
      close_up = confirmSaved(editing_window);
    }
    if (close_up) {
      if (editing_window != null)
        editing_window.close();
      else
        e.getWindow().dispose();
    }
  }

  private boolean confirmSaved (EditWindowI win) {
    boolean okay = ! win.isChanged ();
    if (!okay) {
      JOptionPane pane
        = new JOptionPane (message,
                           JOptionPane.QUESTION_MESSAGE,
                           JOptionPane.YES_NO_CANCEL_OPTION,
                           null, // icon
                           options, // all choices
                           options[1]); // initial value
      JDialog dialog = pane.createDialog((Component) win,
                                         "Please Confirm");
      // This colors the perimeter but not the inside, which stays gray.
      // Better to have it all gray, because it looks weird this way.
      //      pane.setBackground (Config.getAnnotationBackground());
      //      dialog.setBackground (Config.getAnnotationBackground());
      dialog.show();
      Object result = pane.getValue();
      if (result != null) {
        //If there is an array of option buttons:
        if (options[0].equals(result))
          okay = true;
        else if (options[1].equals(result)) {
          // save it first and then proceed
          okay = true;
          win.save();
        }
      }
    }
    return okay;
  }


}
