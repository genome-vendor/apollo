package apollo.dataadapter.ensj.view;

import javax.swing.*;
import javax.swing.plaf.metal.*;
import javax.swing.plaf.basic.*;

//
// The reason for this class is to work around a java
// bug when the combo box updates its Popup menu
// data just before display. This causes the wrong data
// to be initially displayed in the popup until it has
// been mouse overed (redrawn). The only thing this class
// does is to allow access to the popup member variable from 
// the // MetalComboBoxUI so that the user can add a 
// PopupPropertyListener to it (see PopupPropertyListener)
//
// Note: Where this class is used it will of course
// make the look and feel for the combo box be like
// Metal rather than what ever the current l&f is but
// it doesn't look that bad and it does mean the 
// component actually works properly which I think is
// a bit more important than it looking extra pretty.
//
class UpdateWorkRoundComboBoxUI extends MetalComboBoxUI {
  public ComboPopup getPopup() {
    return popup;
  }
}

public class UpdateWorkRoundComboBox extends JComboBox {
  public UpdateWorkRoundComboBox() {
    setUI( new UpdateWorkRoundComboBoxUI() );
  }

  public JPopupMenu getPopup() {
    return (JPopupMenu)((UpdateWorkRoundComboBoxUI)getUI()).getPopup();
  }
}

