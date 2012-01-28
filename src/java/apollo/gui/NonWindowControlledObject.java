package apollo.gui;

import java.util.*;

import apollo.gui.event.*;
import apollo.gui.*;
import apollo.datamodel.*;

import java.io.*;

/** This class was solely being used by AbstractLazySequence, and actually is no longer
    as it didnt seem to serve any real purpose - it added ALS as listener to controller
    but doesnt actually listen for any event - ControlledObjectI is used by Controller
    solely for its window - and there is no window here - so im thinking this class
    could get tossed - steve? */
public class NonWindowControlledObject implements ControlledObjectI,
  EventListener {

  protected Controller controller;

  public NonWindowControlledObject(Controller controller) {
    setController(controller);
  }

  // special implementation of Serialization to stop controller
  // from being saved - shouldnt the controller just be a transient?
  private void writeObject(java.io.ObjectOutputStream out)
  throws IOException {
    Controller temp = controller;
    controller = null;
    out.defaultWriteObject();
    controller = temp;
  }

  public void setController(Controller controller) {

    if (getController() != null) {
      getController().removeListener(this);
    }

    this.controller  = controller;

    if (controller != null) {
      // need to get this removed later! does not happen presently
      // also it is unclear to me why this needs to be added as listener?
      // or what the purpose of NonWindowControlledObject is in general
      getController().addListener(this);
    }
  }
  public Controller getController() {
    return this.controller;
  }

  public Object getControllerWindow() {
    return null;
  }
  public boolean needsAutoRemoval() {
    return false;
  }

  /** This has to be called when the object is no longer relevant
      or this will remain as a lingering ref/mem leak via contollers listeners */
  public void removeFromControllerListeners() {
    getController().removeListener(this);
  }
}
