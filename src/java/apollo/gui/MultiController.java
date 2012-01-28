package apollo.gui;

import apollo.datamodel.*;
import apollo.dataadapter.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import apollo.gui.event.*;

/**
 * An event controller forwarding multiple types of event to registered listeners.
 The MultiController is all the species controllers "master controller". It receives all
 events from the species controllers, but it does not propigate the events back to the
 species. So the master controller sees all events from all species, the individual
 species controllers only see events that occur with that species. This is set up with
 CompositeApolloFrame.getSpeciesController(i) and MultiController.getSpeciesController(i)

Im trying out making the "master" controller just another controller, with the clear flag
set to false. If that works then this class is pase and should probably be deleted.
 */
public class MultiController extends Controller {
  Vector    speciesControllers;
  EventObject currentEvent;
  boolean    inEvent = false;

  public MultiController() {
    this.speciesControllers = new Vector();
  }

  public void addController (Controller c) {
    if (!speciesControllers.contains(c)) {
      speciesControllers.addElement(c);
      c.addListener(this);
      // c.setMasterController() - dont need to can get from singeton method
    }
  }

  public void removeController (Controller c) {
    if (speciesControllers.contains(c)) {
      speciesControllers.removeElement(c);
    }
    c.removeListener(this);
  }

  public boolean handleDataLoadEvent (DataLoadEvent evt) {
    // for now just clear out all subcontrollers - in future may want to
    // be more sphisticated about reusing old speciesControllers?
    super.handleDataLoadEvent(evt); // ??
    clearControllers();
    return true;
  }

  private void clearControllers() {
    for (int i=0; i<speciesControllers.size(); i++) {
      //getSpeciesController(i).removeListener(this);
      getSpeciesController(i).clear(true);
      // read self as it just got removed - or do this in get?
      getSpeciesController(i).addListener(this);
    }
    //speciesControllers.clear();
  }

  public Controller getSpeciesController(int i) {
    if (i >= speciesControllers.size()) {
      Controller c = new Controller();
      c.addListener(this);
      speciesControllers.add(i,c);
      return c;
    }
    else {
      return (Controller)speciesControllers.get(i);
    }
  }

  public Vector getWindowList(Window skipMe) {
    Vector winList = super.getWindowList(skipMe);
    for (int i=0; i<speciesControllers.size(); i++) {
      winList.addAll(getSpeciesController(i).getWindowList(skipMe));
    }
    return winList;
  }

  /*
    public boolean handleFeatureSelectionEvent (FeatureSelectionEvent evt) {
       boolean ignoreEvents;
      if (inEvent) return false;
      inEvent = true;
      for (int i=0; i < controllers.size(); i++) {
        Controller c = (Controller)controllers.elementAt(i);
        if (!c.getListeners().contains(evt.getSource()) &&
            currentEvent != evt) {
          currentEvent = evt;
          c.handleFeatureSelectionEvent(evt);
        }
      }
      super.handleFeatureSelectionEvent(evt);
      inEvent = false;
      return true;
    }
  */
}
