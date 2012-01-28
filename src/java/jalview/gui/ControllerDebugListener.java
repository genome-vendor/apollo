package jalview.gui;

import jalview.gui.event.*;

public class ControllerDebugListener implements AlignViewportListener,
                                                ColumnSelectionListener,
                                                ControlledObjectI,
                                                EditListener,
                                                FontChangeListener,
                                                SchemeChangeListener,
                                                SequenceSelectionListener {
  Controller controller;
  public ControllerDebugListener(Controller c) {
    setController(c);
  }

  public void setController(Controller c) {
    controller = c;
    controller.addListener(this);
  }
 
  public Controller getController() {
    return controller;
  }
 
  public Object getControllerWindow() {
    return null;
  }

  public boolean handleAlignViewportEvent(AlignViewportEvent evt) {
    return true;
  }
  public boolean handleColumnSelectionEvent(ColumnSelectionEvent evt) {
    return true;
  }
  public boolean handleEditEvent(EditEvent evt) {
    System.out.println("handleEditEvent with " + evt.getEdit());
    return true;
  }
  public boolean handleFontChangeEvent(FontChangeEvent evt) {
    return true;
  }
  public boolean handleSchemeChangeEvent(SchemeChangeEvent evt) {
    return true;
  }
  public boolean handleSequenceSelectionEvent(SequenceSelectionEvent evt) {
    if (Config.DEBUG)
      System.out.println("handleSequenceSelectionEvent with " + evt.getSelection());
    return true;
  }
}
