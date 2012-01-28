package apollo.datamodel.seq;

import java.util.*;
import java.io.IOException;
import java.lang.String;

import apollo.datamodel.AbstractSequence;
import apollo.datamodel.Range;
import apollo.datamodel.RangeI;
import apollo.datamodel.SequenceI;
//import apollo.gui.NonWindowControlledObject;
import apollo.config.Config;
import apollo.gui.Controller;
import apollo.gui.event.LazyLoadEvent; // move out of gui? -> dataadap?

/** i wonder if this should move to dataadapter package as its a sequence loader
    basically - so it loads stuff just like a data adapter */

public abstract class AbstractLazySequence extends AbstractSequence implements LazySequenceI {

  protected LazyLoadControlledObject llco;
  protected CacheSequenceLoader      cacher;

  public AbstractLazySequence(String id, Controller c) {
    super(id);
    setController(c);
    setCacher(createCacher());
  }

  /** CacheSequenceLoader requires a Range - getRange will set one up from 1 to length
      if there has not been a range set yet - should this go in AbstractSeq? */
  public RangeI getRange() {
    if (genomicRange == null) {
      setRange(new Range(getName(),1,getLength()));
    }
    return genomicRange;
  }  

  // i see no reason to extend NonWindowControlledObject. it causes this to be
  // added to controller as a listener yet i dont think theres anything it 
  // actually listens to! and furthermore it then can lead to a mem leak if its
  // not removed from the controller as a listener - so im taking out 
  // NonWindowControlledObject
  protected class LazyLoadControlledObject { //extends NonWindowControlledObject {
    private Controller controller; // LazyLoadListener?
    public LazyLoadControlledObject(Controller c) {
      // adds self as listener to controller(see NonWindowConObj)
      // this doesnt appear to remove itself later
      //super(c);
      controller = c;
    }
    public void fireLazyLoadEvent(int type) {
      // this controller can be out of synch with latest controller
      // how does that happen? would be nice to get Config out of here
      //controller.handleLazyLoadEvent(new LazyLoadEvent(this,getDisplayId(),type,LazyLoadEvent.SEQUENCE));
      Config.getController().handleLazyLoadEvent(new LazyLoadEvent(this,getName(),type,LazyLoadEvent.SEQUENCE));
    }

    // should this return Config controller or controller passed in - and shouldnt
    // they be the same?
    public Controller getController() {
      return controller; // Config.getController?
    }
  }

  /** Clean up dangling reference as listener to controller that will make
      old instances persist - no longer needed as doesnt listen to controller
      anymore */
//   public void cleanup() {
//     // no longer needed - no longer listening to controller (for nothing)
//     //llco.removeFromControllerListeners();
//   }

  public abstract SequenceI getSubSequence(int start, int end);

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    LazyLoadControlledObject tmpl = llco;
    CacheSequenceLoader    tmpc = cacher;

    llco   = null;
    // No important state in cacher to serialize
    cacher = null;

    out.defaultWriteObject();

    llco   = tmpl;
    cacher = tmpc;
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();

    // This is nasty but its difficult to reset the controller
    setController(Config.getController());
    //setCacher(new CacheSequenceLoader(this));
    // createCacher gets proper CacheSequenceLoader
    setCacher(createCacher()); 
  }

  /** Overriden by GAMESequence */
  protected CacheSequenceLoader createCacher() {
    return new CacheSequenceLoader(this);
  }

  public void setController(Controller c) {
    llco = new LazyLoadControlledObject(c);
  }

  public void setCacher(CacheSequenceLoader csl) {
    cacher = csl;
  }

  public CacheSequenceLoader getCacher() {
    return cacher;
  }

  protected String getResiduesImpl(int start, int end) {
    return cacher.getResidues(start, end);
  }

  protected String getResiduesImpl(int start) {
    return cacher.getResidues(start, getLength());
  }

  public String getResiduesFromSource(int low, int high) {

    llco.fireLazyLoadEvent(LazyLoadEvent.BEFORE_LOAD);


    String seq = getResiduesFromSourceImpl(low,high);

    llco.fireLazyLoadEvent(LazyLoadEvent.AFTER_LOAD);

    return seq;
  }


  /** Whether the end needs adding to, to make it inclusive. EnsCGISequence does not
      need an inclusive end and overrides this to return false. */
  protected boolean needInclusiveEnd() { return false; }

  protected abstract String getResiduesFromSourceImpl(int low, int high);

  public boolean isLazy() { return true; }
}
