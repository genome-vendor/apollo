package apollo.gui.drawable;

import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.*;

import apollo.datamodel.*;
import apollo.util.SeqFeatureUtil;
import apollo.editor.AnnotationChangeEvent;
import apollo.editor.AnnotationCompoundEvent;
import apollo.editor.FeatureChangeEvent;
import apollo.editor.TransactionSubpart;
import apollo.config.Config;
import apollo.gui.TierManagerI;
import apollo.gui.genomemap.PixelMaskI;
import apollo.gui.Transformer;
import apollo.gui.event.*;
import apollo.seq.*;
import apollo.util.FeatureList;

import org.apache.log4j.*;

/**
* A drawable for drawing a feature set.
* It implements FeatureSetI. While this can be convenient, it is also inconvienient
because it blurs model and view. When you have a FeatureSetI, you dont know if its
in fact a DrawableFeatureSet or a FeatureSet from the datamodel. One has to
do instanceof's to figure this out. My suggestion would be to remove
DrawableFeatureSet implementing FeatureSetI to have a cleaner split between
model and view. Other opinions? It can still retain all the wrapped functions
if thats handy, it just wouldnt parade as a FeatureSetI any longer.
*/
public class DrawableFeatureSet extends DrawableSeqFeature
  implements DrawableSetI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(DrawableFeatureSet.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  /** This is the drawable representing this feature set. This
      drawable_set holds DrawablesSeqFeatures, not model SeqFeatures. */
  Vector drawable_set;

  /** Since this is not an abstract class, allow it to be used as
      a visible drawn feature */
  public DrawableFeatureSet() {
    this(true);
  }

  public DrawableFeatureSet(boolean drawn) {
    super(drawn);
    draw_level = 2;
    // added this otherwise doesnt get created if creating a DrawableFeatureSet
    // from scratch as TierManagerI does via FeatureTier
    drawable_set = new Vector();
  }

  public DrawableFeatureSet(FeatureSetI feature, boolean drawn) {
    super(feature, drawn);  // has to be the first thing in method
    draw_level = 2;
    if (feature == null) {
      // This is what ends up happening when we delete a single-exon feature.
      // Doesn't seem to lead to any further problems if we just return.
      return;
    }
    initSet();
  }

  /** feature holds a reference to the real feature. This is used to
   // generate the tree of DrawableFeatureSets and DrawableSeqFeatures
   // to represent it.
   //
   // Note that by using createDrawableFeature and createDrawableSet
   // classes inherited from DrawableFeatureSet can generate a correctly
   // typed tree using the initSet in DrawableFeatureSet.
   //
   // createDrawableSet will produce an indirect recursive call to initSet
   // in the descent of the FeatureSet tree.
   //
   // Only features which are to be drawn have their FeatureProperty set.
   */

  public void initSet() {
    // this holds all of the drawables for the features
    drawable_set = new Vector();

    int feature_size = ((FeatureSetI) feature).size();
    for (int i=0; i < feature_size; i++) {
      SeqFeatureI sf   = ((FeatureSetI) feature).getFeatureAt(i);
      addFeatureDrawable (sf);
    }
  }

  /** Create a drawable for sf(createDrawable) and add it as a child (addDrawable).
      Added new restriction - only add if dont already have a child drawable for sf.
      On splits an sf can actually get added twice (for add gene and move trans/exon)
      this may be ammended in the future. But anyways theres no reason a set should
      have the same child twice (at least that i can think of) so its not an 
      unreasonable restriction to enforce. 
      the findDrawable really slows things down - especially noticeable for large datasets 
      so this should be fixed up stream in the split not here - taking it out - 
      another alternative would be to have 2 separate methods (or a flag) since the split
      problem comes from repairFeatSet i think */
  public Drawable addFeatureDrawable(SeqFeatureI sf) {
    // check if have drawable for sf

    // SMJS Added condition round this - model not likely to change if editing not enabled
    //      and this should save a huge number of cycles
    // yes - take this out for all cases - fix the editing problem elsewhere
//     if (Config.isEditingEnabled()) {
//       Drawable existingChild = findDrawable(sf);
//       if (existingChild != null)
//         return existingChild;
//     }

    Drawable dsf = createDrawable(sf);
    addDrawable(dsf);
    return dsf;
  }

  public Drawable createDrawable(SeqFeatureI sf) {
    /* This uses the standard method to create its
       child drawables. But it needs to be a separate
       method so that subclasses for featureset may
       return child drawables that are particular
       to that specialized drawableset */
    return (DrawableUtil.createDrawable(sf));
  }

  public void addDrawable (Drawable dsf) {
    drawable_set.addElement(dsf);
    dsf.setRefDrawable(this);
  }

  public void sort(int sortStrand) {
    int setSize = size();

    if (setSize == 0) {
      return;
    }

    int[] coord = new int[setSize];
    Drawable[] obj  = new Drawable[setSize];

    for (int i=0; i < setSize; i++) {
      Drawable sf = getDrawableAt(i);
      coord[i] = sf.getStart();
      obj  [i] = sf;
    }

    apollo.util.QuickSort.sort(coord,obj);

    if (sortStrand == 1) {
      for (int i=0; i < setSize;i++) {
        drawable_set.setElementAt(obj[i], i);
      }
    }
    else {
      for (int i=0; i < setSize;i++) {
        drawable_set.setElementAt(obj[i], (setSize - i - 1));
      }
    }
  }

  public void setFeature(SeqFeatureI feature) {
    super.setFeature(feature);
    initSet();
  }

  /** Need to change cached feature property - resynch with feature
      and synch up descendants */
//  public void synchFeatureProperty() {
//    super.synchFeatureProperty();
//    for (int i=0; i<size(); i++) {
//      getDrawableAt(i).synchFeatureProperty();
//    }
//  }
  

  // For speed this method actually reimplements the DrawableSeqFeature method
  // so it has access to the boxBounds
  public boolean draw(Graphics g, Transformer transformer,
                      TierManagerI manager, PixelMaskI mask) {
    // Bodge - should be done in response to a types change event
    setLabeled(getFeatureProperty().getTier().isLabeled());
    DrawableUtil.setBoxBounds(this, transformer, manager);

    if (!wantToDraw(g,manager,transformer,boxBounds)) {
      return false;
    }

    int setSize = size();
    boolean fullDraw = (((float)boxBounds.width / (float)setSize) >
                        Config.getFastDrawLimit());
    // there is a bug here in that decorated is not getting set to true when it
    // is decorated - im trying to figure out the intent of the code here
    // at the moment the only way decorated can become true is if it is not a
    // fullDraw - so what is this full draw thing about
    // below it will only draw if not obscured or its decorated - and by this
    // logic it can only be decorated if its also NOT a full draw
    // i believe this is saying if its being masked then it shouldnt be
    // selected.
    // i dont think this is correct as you can still select the item from
    // the text selection area
    boolean decorated = false;

    if (!fullDraw) {
      fullDraw = super.isDecorated();
    } // change of logic - will do dec even if fullDraw - MG
    decorated = super.isDecorated();
    for (int i=0; i < setSize && !fullDraw;i++) {
      Drawable  sf = getDrawableAt(i);
      if (sf.isDecorated()) {
        fullDraw = true;
        decorated = true;
        break;
      }
    }

    if (!mask.isCompletelyObscured(boxBounds.x,
                                   boxBounds.x+boxBounds.width,
                                   getTierIndex(manager)) || decorated) {
      if (fullDraw) {

        feature_draw(g,transformer,manager,boxBounds);

        for (int i=0; i < setSize;i++) {
          Drawable sf = getDrawableAt(i);
          // Now call the draw function for each feature.
          sf.draw(g,transformer,manager,mask);
        }
      }
      else {
        DrawableSeqFeature sf = (DrawableSeqFeature) getDrawableAt(0);
        DrawableUtil.setBoxBounds(this, transformer, manager);

        mask.setPixelRangeState(boxBounds.x,
                                boxBounds.x+boxBounds.width,
                                true,
                                getTierIndex(manager));
        sf.feature_draw(g, transformer, manager, boxBounds);
      }
    }
    return true;
  }

  /**
   * draw method using the Transformer to do coord conversions.
   *
   */
  public boolean draw(Graphics g,
                      Transformer transformer,
                      TierManagerI manager) {

    boolean drawn = false;
    if (!super.draw(g,transformer,manager)) {
      drawn = false;
    }
    else {
      int setSize = size();
      for (int i=0; i < setSize;i++) {
        // Now call the draw function for each feature.
        getDrawableAt(i).draw(g,transformer,manager);
      }
    }
    return drawn;
  }

  public void addHighlights(Graphics g,
                            Rectangle boxBounds,
                            Transformer transformer,
                            TierManagerI manager) {}


  public void drawSelected(Graphics g,
                           Rectangle boxBounds,
                           Transformer transformer,
                           TierManagerI manager) {
    drawUnselected(g,boxBounds,transformer,manager);
  }

  protected boolean drawDashedLines(Graphics g,
                                    Rectangle boxBounds,
                                    Transformer transformer,
                                    TierManagerI manager,
                                    Color color,
                                    int y_center) {
    return false;
  }

  public void drawUnselected(Graphics g,
                             Rectangle boxBounds,
                             Transformer transformer,
                             TierManagerI manager) {
    if (size() > 1) {
      Color color = getDrawableColor();
      g.setColor(color);
      int y_center = getYCentre(boxBounds);

      if (!drawDashedLines(g,
                           boxBounds,
                           transformer,
                           manager,
                           color,
                           y_center)) {
        g.drawLine(boxBounds.x < 0 ? 0 : boxBounds.x,
                   y_center,
                   // Hack for crap Java 1.1 which must use a short somewhere
                   // If displays get larger than 3000 pixels width
                   // this will fail
                   (boxBounds.x + boxBounds.width > 3000 ?
                    3000 : boxBounds.x + boxBounds.width),
                   y_center);//
      }
    }
    /* Draw a rounded box around feature set if it's tagged with a result tag
       (Eventually want to cross-hatch these tagged results,
       but that's harder.)

       This draw method does not belong in the general drawable. It must
       be in the result drawable (either set or individual)
    if (((FeatureSet)feature).tagged()) {
      g.setColor(Config.getStyle().getTaggedColor());
      g.drawRoundRect(boxBounds.x-2,boxBounds.y-2,
                      boxBounds.width+3,boxBounds.height+3,
                      4, 4);
    }
    */
  }

  public void  setSelected(boolean state) {
    int setSize = size();
    for (int i = 0; i < setSize; i++) {
      getDrawableAt(i).setSelected(state);
    }
    super.setSelected(state);
  }

  public void  setVisible(boolean state) {
    int setSize = size();
    for (int i = 0; i < setSize; i++) {
      getDrawableAt(i).setVisible(state);
    }
    super.setVisible(state);
  }

  public void  setLabeled(boolean state) {
    super.setLabeled(state);
  }

  public void setTierIndex(int index) {
    int setSize = size();
    for (int i = 0; i < setSize; i++) {
      getDrawableAt(i).setTierIndex(index);
    }
    super.setTierIndex(index);
  }

  public int size() {
    return drawable_set.size();
  }

  public void      deleteDrawable(Drawable sf) {
    if (!drawable_set.removeElement(sf))
      logger.warn ("Did not have drawable for " + sf.getName());
  }

  /** Returns a DrawableSeqFeature (not model SeqFeature) */
  public Drawable getDrawableAt(int i) {
    return (Drawable) drawable_set.elementAt(i);
  }

  /** A Vector of DrawableSeqFeatures is returned, not model SeqFeatures.
      (the DrawableSeqFeatures all point to model of course)*/
  public Vector     getDrawables() {
    return drawable_set;
  }

  public FeatureList findFeaturesByAllNames(String name) {
    return ((FeatureSetI) feature).findFeaturesByAllNames(name);
  }

  public FeatureList findFeaturesByAllNames(String name, boolean useRegExp) {
    //this is never used for user-initiated searching, so setuseRegExp
    //to false, always!
    return ((FeatureSetI) feature).findFeaturesByAllNames(name, false);
  }

  // Actually returns a drawable, not the feature itself
  public Drawable findDrawable(SeqFeatureI sf) {
    Drawable found = super.findDrawable(sf);
    if (found == null) {
      int setSize = this.size();
      for (int i = 0; i < setSize && found == null; i++) {
        Drawable check = getDrawableAt (i);
        found = check.findDrawable(sf);
      }
    }
    return found;
  }

  /** Compound events dig out children */
  public void repairFeatureSet(AnnotationCompoundEvent ace) {
    for (int i=0; i<ace.getNumberOfChildren(); i++)
      repairFeatureSet(ace.getChildChangeEvent(i));
  }

  /** this handles several types of repairs.
   *  ADD, MERGE, SPLIT, DELETE, LIMITS, and SYNC
   *  the purpose of the repairs  is to keep the drawables in sync
   *  with the additions and deletes coming from the annotation editor 
   *  Takes in both AnnotationChangeEvents and ResultChangeEvents */
  public void repairFeatureSet(FeatureChangeEvent ce) {
    SeqFeatureI parentModel = ce.getParentFeature(); // check that 1st is parent always
    // in the case of REPLACE this is not the parent - this is the old
    // model/drawable being replaced
    // also in the case that we do need the parent cant we just query the
    // child for its parentDrawable - NO - it might be deleted already!
    DrawableSetI parentDrawable = (DrawableSetI) findDrawable(parentModel);
    Drawable changedDrawable = findDrawable(ce.getChangedFeature());

    // Type change
    if (ce.isUpdate() && ce.getSubpart() == TransactionSubpart.TYPE) {
      // this wont cut it - need a whole new drawable object for new type - 
      // need to delete and add new (like replace did)
      //changedDrawable.synchFeatureProperty();
      parentDrawable.deleteDrawable(changedDrawable);
      Drawable newDrawable = parentDrawable.addFeatureDrawable(ce.getChangedFeature());
      // depending on type change draw level may be different
      // shouldnt this be handled in addFeatureDrawable?
      int go_up = newDrawable.getDrawLevel() - changedDrawable.getDrawLevel();
      if (go_up < 0)
        newDrawable.setDrawLevel(changedDrawable.getDrawLevel());
    }

    // only care about adding whole annots NOT adding subparts (syns,comments)
    // splits and merges pase?
    else if ((ce.isAdd() && !ce.hasSubpart()) || ce.isMerge() || ce.isSplit()) {
      // evidence events are now supressed as they are presently irrelevant
      //if (ce.getObjectClass() != AnnotationChangeEvent.EVIDENCE) {
        // there must always be a parentDrawable to add to and delete from
      if (drawableNotNull (parentDrawable, ce)) {
        // if this is new then this may not be found
        if (changedDrawable != null) {
          // remove the old version, if any
          parentDrawable.deleteDrawable(changedDrawable);
        }
        parentDrawable.addFeatureDrawable(ce.getChangedFeature());
      }
    }

    else if (ce.isMove()) { // UPDATE PARENT
      // move is an update event, thus getUpdateDetails is non-null
      // parent has changed, need to get previous parent
      SeqFeatureI oldParentModel = ce.getUpdateDetails().getOldParent();
      Drawable oldParentDrawable = findDrawable(oldParentModel);
      if (!drawableNotNull(oldParentDrawable,ce)) // prints error msg
        return;
      if (changedDrawable != null) {
        oldParentDrawable.deleteDrawable(changedDrawable);
      }
      parentDrawable.addFeatureDrawable(ce.getChangedFeature());
    }

    // only care about deletes of whole annots NOT subpart deletes(comments,syns)
    else if (ce.isDelete() && !ce.hasSubpart()) {
      //if (ce.getObjectClass() != AnnotationChangeEvent.EVIDENCE) { no longer
      // there must always be a a parentDrawable to add to and delete from
      if (drawableNotNull (parentDrawable, ce)) {
        // if this is new then this may not be found
        if (changedDrawable != null) {
          // remove the old version, if any
          parentDrawable.deleteDrawable(changedDrawable);
        } else {
          logger.error("Could not delete " +
                       ce.getChangedFeature().getName() +
                       " from " +
                       ce.getParentFeature().getName() +
                       " because drawable was not found");
        }
      }
    }
    // this better be happening in AnnotEditor if it isnt its gotta be ammended
    // DrawableFeatureSet should not be responsible for modifying the model
//     else if (ce.limitsChanged()) {
//       //case AnnotationChangeEvent.LIMITS:
//       model.adjustEdges(); // does this really need to happen here???
//     }
      //break;
    //case FeatureChangeEvent.SYNC:
    else if (ce.isSync()) {
      syncDrawableToModel (parentModel, this); // adds drawables for new model
      eraseDrawables (parentModel, this); // erase drawables no longer in model
      //break;
    }
    //DrawableFeatureSet dfs = (DrawableFeatureSet) findDrawable(ce.getChangeTop());
    Drawable d = findDrawable(ce.getChangeTop());
    if (d != null) {
      d.setVisible(true);
    }
  }

  // TODO - this method doesn't appear to be called anywhere.  Remove it?
  private void debugDrawable(Drawable d, String indent) {
    logger.debug (indent + "Using a " + d.getClass().getName() +
                  " to draw " + d.getFeature().getName() +
                  " a " + d.getFeature().getFeatureType() +
                  " draw level=" + d.getDrawLevel() +
                  " isDrawn=" + d.isDrawn());
    if (d instanceof DrawableSetI) {
      DrawableSetI ds = (DrawableSetI) d;
      for (int i = 0; i < ds.size(); i++) {
        debugDrawable(ds.getDrawableAt(i), indent+"\t");
      }
    }
  }

  private void syncDrawableToModel (SeqFeatureI fs,
                                    DrawableFeatureSet dfs) {
    int feature_count = fs.getNumberOfChildren();
    for (int i = 0; i < feature_count; i++) {
      SeqFeatureI sf = fs.getFeatureAt (i);
      DrawableSeqFeature dsf = (DrawableSeqFeature) findDrawable(sf);
      if (dsf == null) {
        // This will add all child drawables as well
        dfs.addFeatureDrawable(sf);
      }
      else if (sf.canHaveChildren()) {
        // keep looking in case any children need drawables
        syncDrawableToModel ((FeatureSetI) sf, (DrawableFeatureSet) dsf);
      }
      // else at the bottom and a drawable exists
    }
  }

  private void eraseDrawables (SeqFeatureI fs,
                               DrawableFeatureSet dfs) {
    int feature_count = dfs.size();
    for (int i = feature_count - 1; i >= 0; i--) {
      Drawable dsf = dfs.getDrawableAt (i);
      SeqFeatureI sf = dsf.getFeature();
      //if (!fs.findFeature(sf)) {
      if (!SeqFeatureUtil.containsFeature(fs,sf)) {
        // This will remove all child drawables as well
        dfs.deleteDrawable(dsf);
      }
      else if (dsf instanceof DrawableFeatureSet) {
        // keep looking in case any children need drawables
        eraseDrawables ((FeatureSetI) sf, (DrawableFeatureSet) dsf);
      }
    }
  }

  /** This checks if drawble is null - if it is it prints an error message and a stack
      trace */
  private boolean drawableNotNull(Drawable drawable,FeatureChangeEvent ce) {
    if (drawable == null) {
      try {
        SeqFeatureI feat = ce.getParentFeature();
//         if (ce.isReplace())
//           feat = ce.getReplacedFeature();
        throw new Exception ("Failed to find annotation to repair on "+feat.getName()+
                             " of class " + feat.getClass().getName() +
                             " for event " + ce);
      } catch (Exception e) {
        logger.error (e.getMessage(), e);
      }
      return false; // this was suspicously missing
    }
    return true;
  }

  // SeqFeature methods
  public String toString_mv() {
    return drawable_set.toString();
  }

  public void setEdgeHighlights(int [] edges, boolean state,
                                Transformer transformer) {
    super.setEdgeHighlights(edges,state,transformer);

    for (int i = 0; i < size(); i++) {
      getDrawableAt(i).setEdgeHighlights(edges,state,transformer);
    }
  }

  public void setHighlighted(boolean state) {
    super.setHighlighted(false);
    int setSize = size();
    for (int i=0; i<setSize; i++) {
      if (getDrawableAt(i) instanceof DrawableSeqFeature) {
        getDrawableAt(i).setHighlighted(state);
      }
    }
  }

  /** returns the underlying FeatureSetI model. getFeature returns the underlying
      FeatureSetI as well but it returns it as a SeqFeatureI.
      This returns model NOT drawable. */
  public FeatureSetI getFeatureSet() {
    return (FeatureSetI)feature;
  }

}
