package apollo.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import apollo.datamodel.*;
import apollo.editor.AnnotationChangeEvent;
import apollo.editor.AnnotationChangeListener;
import apollo.editor.ResultChangeEvent;
import apollo.editor.ResultChangeListener;
import apollo.dataadapter.DataLoadEvent;
import apollo.dataadapter.DataLoadListener;
import apollo.config.PropSchemeChangeEvent;
import apollo.config.PropSchemeChangeListener;
import apollo.gui.event.*;

import org.apache.log4j.*;

/**
 * An event controller forwarding multiple types of event to registered listeners.
 */
public class Controller implements BaseFocusListener,
      AnnotationChangeListener,
      FeatureSelectionListener,
      LazyLoadListener,
      NamedFeatureSelectionListener,
      SetActiveCurStateListener,
      OrientationListener,
      DataLoadListener,
      ResultChangeListener,
      ReverseComplementListener,
      PropSchemeChangeListener,
      WindowListener,
      ScrollListener,
      ZoomListener {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(Controller.class);

  /** Singleton master controller - has all the species controllers as 
      sub controllers - actuall this is just another controller now 
      - MultiController is now pase - really its a master listener */
  //private static MultiController masterController;
  private static Controller masterController;

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  Vector    listeners;
  Hashtable windowHash = new Hashtable();
  boolean   annotations_changed = false;
  // SUZ This is to indicate whether this is a first time ever
  // or a curation set has previously been loaded
  boolean   curation_loaded = false;

  public Controller() {
    this.listeners = new Vector();
  }

  /** static master controller - should be renamed master listener. Listens
      to all curation controllers. Does not fire to them. Should there be 
      another controller (called masterController?) that fires to all curations
      but doesnt listen to them? Cant do both in one or all events would go 
      everywhere - in fact youd have an endless loop. */
  public static Controller getMasterController() {
    if (masterController == null) {
      masterController = new Controller();
    }
    return masterController;
  }

  public boolean hasListener(EventListener l) {
    return listeners.contains(l);
  }

  // addListener adds a listener and if the listener is a known type
  // which is displayed in a window it adds a WindowListener to
  // to allow automatic removal of the listeners when the Window closes
  public void addListener (EventListener l) {
    Window w = null;
    Object wobj = null;

    if (!listeners.contains(l)) {
      listeners.addElement(l);
    }

    if (l instanceof ControlledObjectI) {
      wobj = ((ControlledObjectI)l).getControllerWindow();
      if (wobj instanceof Window) {
        w = (Window)wobj;
      }
      else if (wobj != null) {
        logger.warn(l.getClass().getName() +
                    ".getControllerWindow() returned Non Window for " +
                    l.getClass().getName() + " is " + wobj);
      }
    }
    if (w != null) {
      ControlledWindow cw = null;
      if (windowHash.containsKey(w)) {
        cw = (ControlledWindow)windowHash.get(w);
      } else {
        w.addWindowListener(this);
        cw = new ControlledWindow(w);
        windowHash.put(w,cw);
      }
      if (cw != null) {
        cw.addChild(l);
      }

    }
  }

  public Vector getWindowList() {
    return getWindowList(null);
  }

  public Vector getWindowList(Window skipMe) {
    Vector out = new Vector();
    Enumeration e = windowHash.keys();
    while(e.hasMoreElements()) {
      Window win = (Window) e.nextElement();
      if (!win.equals(skipMe))
        out.addElement(win);
    }
    return out;
  }

  /** Clear called by handleDataLoadEvent with clearOnDataLoadEvent
      set to true, for species controllers not master controllers 
      also called by CurationState when removed */
  public void clear(boolean keepListenerToMaster) {
    listeners.clear();
    windowHash.clear();
    
    if (keepListenerToMaster && this != getMasterController())// re-add master
      addListener(getMasterController());
  }

  /** Remove from listeners vector. Also if a ControlledObjectI with a 
      non-null ControllerWindow, then remove as child of ControlledWindow
  */
  public void removeListener (EventListener l) {
    if (listeners.contains(l)) {
      listeners.removeElement(l);
    }
    if (l instanceof ControlledObjectI) {
      ControlledObjectI coi = (ControlledObjectI)l;
      Object win = coi.getControllerWindow(); // Window
      if (win == null) return;
      ControlledWindow cw = (ControlledWindow)windowHash.get(win);
      if (cw != null) cw.removeChild(l);
    }
  }
  
  public Vector getListeners() {
    return listeners;
  }

  public boolean isAnnotationChanged () {
    return annotations_changed;
  }

  public boolean isCurationSetLoaded () {
    return curation_loaded;
  }

  public void setAnnotationChanged (boolean un_saved) {
    annotations_changed = un_saved;
  }

  public void curationSetIsLoaded (boolean loaded) {
    // never let this be set to false, once anything has
    // loaded then this is true
    if (loaded)
      curation_loaded = loaded;
  }

  // These implement the listener interfaces
  public boolean handleZoomEvent (ZoomEvent evt) {
    for (int i=0; i < listeners.size(); i++) {
      EventListener l = (EventListener)listeners.elementAt(i);

      if (l instanceof ZoomListener && l != evt.getSource()) {
        ((ZoomListener)l).handleZoomEvent(evt);
      }
    }
    return true;
  }

  public boolean handleAnnotationChangeEvent (AnnotationChangeEvent evt) {
    for (int i=0; i < listeners.size(); i++) {
      EventListener l = (EventListener)listeners.elementAt(i);
      if (l instanceof AnnotationChangeListener && l != evt.getSource()) {
        ((AnnotationChangeListener)l).handleAnnotationChangeEvent(evt);
      }
    }
    annotations_changed = true;
    return annotations_changed;
  }

  public boolean handleSetActiveCurStateEvent (SetActiveCurStateEvent evt) {
    for (int i=0; i < listeners.size(); i++) {
      EventListener l = (EventListener)listeners.elementAt(i);
      if (l instanceof SetActiveCurStateListener && l != evt.getSource()) {
        ((SetActiveCurStateListener)l).handleSetActiveCurStateEvent(evt);
      }
    }
    return true;
  }

  /**
   * <p> A NamedFeatureSelectionEvent nominates only the name of the feature
   * to be selected - the panel responds by (1) finding the feature of
   * interest and rebouncing the event as a feature selection event. Note
   * that we will preserve the source of the original event in the new events
   * we throw. </p>
   *
   * <p> This is being written to allow a SyntenyLinkPanel to throw feature 
   * selections at the linked StrandedZoomableApolloPanels, even though they
   * don't actually have the features (genes) to select. </p>
  **/
  public boolean handleNamedFeatureSelectionEvent(NamedFeatureSelectionEvent evt) {
    for (int i=0; i < listeners.size(); i++) {
      EventListener l = (EventListener)listeners.elementAt(i);

      if (l instanceof NamedFeatureSelectionListener && l != evt.getSource()) {
        ((NamedFeatureSelectionListener)l).handleNamedFeatureSelectionEvent(evt);
      }//end if
    }//end for
    return true;
  }//end handleNamedFeatureSelectionEvent

  public boolean handleFeatureSelectionEvent (FeatureSelectionEvent evt) {
    FeatureSelectionEvent newEvent = null;
    for (int i=0; i < listeners.size(); i++) {
      EventListener l = (EventListener)listeners.elementAt(i);
      if (l instanceof FeatureSelectionListener && l != evt.getSource()) {
        if(!(l instanceof apollo.gui.synteny.SyntenyLinkPanel)){
          ((FeatureSelectionListener)l).handleFeatureSelectionEvent(evt);
        } else {
          /* If I'm firing at a synteny link panel I have to switch the
             source of the event, so I dont' get the event back fired at ME.
             This is too messy for words: just don't ask...
             Can synteny check source of event? 
             What?? what is this about - i may need to change this -
             so the source is used by SynLinkPan to figure out which species the
             event came from - but this can also be figured out by examining the feature
             itself - should change this - mg */
          newEvent = new FeatureSelectionEvent(this, evt.getSelection());
          ((FeatureSelectionListener)l).handleFeatureSelectionEvent(newEvent);
        }//end if
      }
    }
    return true;
  }

  public boolean handleDataLoadEvent (DataLoadEvent evt) {
    // Have to clone listeners because handleDataLoadEvent can cause
    // a removeListener which alters the listeners vector

    Vector listenersClone = (Vector)listeners.clone();
    for (int i=0; i < listenersClone.size(); i++) {
      EventListener l = (EventListener)listenersClone.elementAt(i);

      if (l instanceof DataLoadListener && l != evt.getSource()) {
        ((DataLoadListener)l).handleDataLoadEvent(evt);
      }
    }
    // CurSet controllers clear out listeners (except master) on region change
    // event, master controller does not (should it?)
    // This approach is no good as then everyone has to readd themselves on region
    // change. the opposite approach is better (though more leaky). listeners
    // stay listeners on region change. if they shouldnt be they need to remove
    // themselves.
    //if (clearOnDataLoadEvent) clear(true);

    return true;
  }

  public boolean handleReverseComplementEvent (ReverseComplementEvent evt) {
    for (int i=0; i < listeners.size(); i++) {
      EventListener l = (EventListener)listeners.elementAt(i);

      if (l instanceof ReverseComplementListener && l != evt.getSource()) {
        ((ReverseComplementListener)l).handleReverseComplementEvent(evt);
      }
    }
    return true;
  }

  public boolean handleOrientationEvent (OrientationEvent evt) {
    for (int i=0; i < listeners.size(); i++) {
      EventListener l = (EventListener)listeners.elementAt(i);

      if (l instanceof OrientationListener && l != evt.getSource()) {
        ((OrientationListener)l).handleOrientationEvent(evt);
      }
    }
    return true;
  }

  public boolean handleLazyLoadEvent (LazyLoadEvent evt) {
    for (int i=0; i < listeners.size(); i++) {
      EventListener l = (EventListener)listeners.elementAt(i);

      if (l instanceof LazyLoadListener && l != evt.getSource()) {
        ((LazyLoadListener)l).handleLazyLoadEvent(evt);
      }
    }
    return true;
  }

  public boolean handleResultChangeEvent (ResultChangeEvent evt) {
    for (int i=0; i < listeners.size(); i++) {
      EventListener l = (EventListener)listeners.elementAt(i);

      if (l instanceof ResultChangeListener && l != evt.getSource()) {
        ((ResultChangeListener)l).handleResultChangeEvent(evt);
      }
    }
    return true;
  }

  public boolean handlePropSchemeChangeEvent(PropSchemeChangeEvent evt) {
    
    for (int i=0; i < listeners.size(); i++) {
      EventListener l = (EventListener)listeners.elementAt(i);

      if (l instanceof PropSchemeChangeListener && l != evt.getSource()) {
        ((PropSchemeChangeListener)l).handlePropSchemeChangeEvent(evt);
      }
    }
    return true;
  }

  public boolean handleBaseFocusEvent (BaseFocusEvent evt) {
    for (int i=0; i < listeners.size(); i++) {
      EventListener l = (EventListener)listeners.elementAt(i);

      if (l instanceof BaseFocusListener && l != evt.getSource()) {
        ((BaseFocusListener)l).handleBaseFocusEvent(evt);
      }
    }
    return true;
  }

  public boolean handleChainedRepaintEvent(ChainedRepaintEvent event){
    for (int i=0; i < listeners.size(); i++) {
      EventListener l = (EventListener)listeners.elementAt(i);

      if (l instanceof ChainedRepaintListener && l != event.getSource()) {
        ((ChainedRepaintListener)l).handleChainedRepaint(event);
      }
    }
    return true;
  }//end handleChainedRepaintEvent
  
  public boolean handleScrollEvent(ScrollEvent event) {
    ScrollEvent newScrollEvent = new ScrollEvent(this, event.getValue());
    for (int i=0; i < listeners.size(); i++) {
      Object l = listeners.elementAt(i);

      if (l instanceof ScrollListener && l != event.getSource()) {
        ((ScrollListener)l).handleScrollEvent(newScrollEvent);
      }
    }
    
    return true;
  }

  // Other events that we'd want to handle in this class
  //  - QuitEvent
  //  - WindowCloseEvent
  //  - NewWindowEvent


  // WindowListener methods
  /** windowClosed happens on dispose */
  public void windowClosed(WindowEvent e) {
    windowClosing(e);
  }

  /** WindowClosing happens when user closes a window, not dispose 
      This removes ControlledWindow of event source as window listener
      unless needsAutoRemoval returns false
      It also removes all children of ControlledWindow as listeners. 
      Children are ControlledObjectIs that have the window as its 
      getControlledWindow.
      Children of ControlledWindow are usually but not always subparts of that window.
      TierManagers are non-gui ControlledObject thet list the ancestor of its view
      (ApolloFrame) as its controller window
      This is nice but most listeners are children of ApolloFrame which currently
      when it closes apollo exits so who cares if the children are being removed
      as listeners. Presently this is really only useful
      for non-ApolloFrame children (EDE?,AnnotEditor?,...?)
      If apollo lives on beyond a frame closing this would be useful.
      Also what might be cool is to somehow automate the removing of 
      listeners to a curationController when the curation has been downsized
      (e.g. go from 2 to 1 curation).
  */
  public void windowClosing(WindowEvent e) {
    logger.debug("Controller got WindowClosing event");
    ControlledWindow cw = (ControlledWindow)windowHash.get(e.getSource());

    if (cw != null) {
      Vector children = cw.getChildren();

      // If any children have needsAutoRemoval=false then return - ???
      for (int i=0; i<children.size(); i++) {
        EventListener l = (EventListener)children.elementAt(i);
        if (l instanceof ControlledObjectI) {
          ControlledObjectI co = (ControlledObjectI)l;
          if (!co.needsAutoRemoval()) {
            logger.debug("Not doing auto removal because of " + co);
	    // shouldnt this be continue? this means if one child doesnt want
            // to be removed as a listener, none of the children will - that
            // seems wrong
            return;
          }
        }
      }

      // if the above is changed to gather all auto removed kids this should
      // just go through those - or even better just remove them above.
      for (int i=0; i<children.size(); i++) {
        // Very heavy call for FeatureTierManagers logger.debug("Removing listener " + children.elementAt(i));
        removeListener((EventListener)children.elementAt(i));
      }
      windowHash.remove(e.getSource());
      cw.getWindow().removeWindowListener(this);
    }
  }

  public void windowOpened(WindowEvent e) {}

  public void windowIconified(WindowEvent e) {}

  public void windowDeiconified(WindowEvent e) {}

  public void windowActivated(WindowEvent e) {}

  public void windowDeactivated(WindowEvent e) {}

  class ControlledWindow {
    Window window;
    Vector children;

    public ControlledWindow(Window w) {
      this.window = w;
      children = new Vector();
    }

    public void addChild(Object child) {
      children.addElement(child);
    }

    public void removeChild(Object child) {
      if (child  == null) return;
      children.remove(child);
    }

    public Vector getChildren() {
      return children;
    }

    public Window getWindow() {
      return window;
    }
    // What does this method do other than the println?  Was it just for debugging?  --NH
    public void finalize() {
      logger.debug("Finalized ControlledWindow " + window.getName());
    }
  
  }
}
