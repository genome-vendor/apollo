package apollo.config;

import java.io.*;
import java.awt.Color;
import java.util.*;
import javax.swing.JOptionPane;
import apollo.config.Config;
import apollo.datamodel.SeqFeatureI;
import apollo.util.*;

import org.apache.log4j.*;

/** PropertyScheme is basically the java version of the tiers file.
 * Changed from static/singleton to instance as we now have different instances of
 * tiers for different data adapters 
 * Holds a list of TierProperties, TierProperties hold FeatureProperties
 * Fires PropSchemeChangeEvent (to controller) when anything changes.
*/
public class PropertyScheme /*extends Observable*/ 
  implements /*Observer,*/ Serializable {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(PropertyScheme.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  /** vector of TierProperties (which hold FeatureProperties) */
  private Vector    tiersVect = new Vector();
  private /*static*/ Hashtable tiersHash = new Hashtable();
  private CaseInsensitiveStringHash fpTypeToPropHash = new CaseInsensitiveStringHash();
  private CaseInsensitiveStringHash fpAnalNameToPropHash = new CaseInsensitiveStringHash();
  private boolean cachesNeedUpdate = true;
  private boolean   groupedUpdate = false;
  private Vector comments;
  private Vector propSchemeChangeListeners;
  private boolean changed = false;
  // Feature props for all legal annotation types
  private Vector annotationFeatureProps;

  private FeatureProperty defaultFeatureProperty = new FeatureProperty();

  /** Parses tiers file returned by Config.getTiersFile(), which returns the current
      Style's tiers file. */
  public PropertyScheme() throws IOException, FileNotFoundException {
    this(Config.getTiersFile());
  }

  /** Parses and creates prop scheme from tiers file passed in */
  public PropertyScheme(String tiersFile) throws IOException, FileNotFoundException {
    if (tiersFile == null) {
      throw new IOException("No tiers file name!");
    }
    this.propSchemeChangeListeners = new Vector();
    readDefaultTypes(tiersFile);
    initBuiltInTypes();
    setChanged();
    firePropSchemeChangeEvent();
  }

  public void addPropSchemeChangeListener(PropSchemeChangeListener pcl) {
   if (!propSchemeChangeListeners.contains(pcl)) {
      propSchemeChangeListeners.addElement(pcl);
    }
  }

  // steve searle changed from public to package - mg reverted
  public void firePropSchemeChangeEvent() {
    setChanged(); // in case group update - changed = true
    if (propSchemeChangeListeners.size() == 0)
      return;
    if (groupedUpdate) // if we are amidst group update dont fire til group done
      return;

    PropSchemeChangeEvent e = new PropSchemeChangeEvent(this,this);

    for (int i=0; i < propSchemeChangeListeners.size(); i++) {
      PropSchemeChangeListener l = (PropSchemeChangeListener)propSchemeChangeListeners.elementAt(i);
      logger.debug("Firing PropSchemeChangeEvent from PropertyScheme " + this + " listener = " + l);

      l.handlePropSchemeChangeEvent(e);
    }

    changed = false; // no group update, event fired, unset changed
  }

  public int size() {
    return tiersVect.size();
  }

  // SUZ made this public so that analyses users could specify
  // new tiers on demand
  public void addTierType(TierProperty tierProp) {
    if (tiersHash.get (tierProp.getLabel().toLowerCase()) == null) {
      tiersVect.addElement(tierProp);
    }
    tiersHash.put(tierProp.getLabel().toLowerCase(), tierProp);
    //tp.addObserver(this);
    tierProp.setPropertyScheme(this); 
    // should a TierChangeEvent be fired or is that being taken care of?
    // tp.setTiersChangedListener wont work as may not have one yet
  }

  public void expandAllTiers(boolean state) {
    setGroupedUpdate(true);
    for (int i=0; i< size(); i++) 
      getTierProperty(i).setExpanded(state);
    setGroupedUpdate(false); // fires prop scheme event
  }

  public void setAllTiersVisible(boolean state) {
    setGroupedUpdate(true);
    for (int i = 0; i < size(); i++) 
      getTierProperty(i).setVisible(state); // tries to fire event, sets changed.
    setGroupedUpdate(false); // fires prop scheme event
  }
  
  private void setChanged() {
    changed = true;
    cachesNeedUpdate = true;
  }
  private boolean hasChanged() { return changed; }

  private void setGroupedUpdate(boolean state) {
    groupedUpdate = state;
    if (state == false && hasChanged()) {
      firePropSchemeChangeEvent();
      //notifyObservers();
    }
  }

  public void moveTier(String fromTier, String toTier) {

    int toInd   = (toTier != null) ? getTierInd(toTier) : size()-1;
    int fromInd = getTierInd(fromTier);

    logger.debug("toInd = " + toInd + " fromInd = " + fromInd);
    if (toInd == -1 || fromInd == -1) {
      logger.error("moveTier failed to find fromTier and/or toTier");
    } else {
      TierProperty from = (TierProperty) tiersVect.elementAt(fromInd);
      tiersVect.removeElementAt(fromInd);
      tiersVect.insertElementAt(from,toInd);
    }
    setChanged();
    firePropSchemeChangeEvent();

    return;
  }

  public Enumeration getTypes() {
    return fpTypeToPropHash.keys();
  }
  
  public Enumeration getAnalNames() {
    return fpAnalNameToPropHash.keys();
  }
  
  private void regenerateFeaturePropCaches() {
    fpTypeToPropHash.clear();
    fpAnalNameToPropHash.clear();
    
    for (int i = 0; i < tiersVect.size() ; i++) {
      TierProperty tp = (TierProperty) tiersVect.elementAt (i);
      Vector fps = tp.getFeatureProperties ();

      Iterator iter = fps.iterator();
      
      while (iter.hasNext()) {
        FeatureProperty fp = (FeatureProperty)iter.next();
        if (!fpTypeToPropHash.containsKey(fp.getDisplayType())) {
          fpTypeToPropHash.put(fp.getDisplayType(),fp);
        }
        Iterator atIter = fp.getAnalysisTypes().iterator();
        while (atIter.hasNext()) {
          String analType = (String)atIter.next();
          if (!fpAnalNameToPropHash.containsKey(analType)) {
            fpAnalNameToPropHash.put(analType,fp);
          }
        }
      }
    }
  }

  /**
   * Returns the FeatureProperty associated with analysis_type.
   * <P>
   * The FeatureProperty will be the registered property if type is known.
   * Otherwise, if createProperty is true, it will create a new
   * FeatureProperty object with the default values, otherwise it will
   * return null.
   *
   * @param type  the String type of a feature
   * @param createProperty - whether to create a new property if not found
   * @return the FeatureProperty object for that type, or null
   * @throws NullPointerException if type is null
   */
  public FeatureProperty getFeatureProperty(String analysis_type,
      boolean createProperty) {
    FeatureProperty fp = null;

    if (cachesNeedUpdate) {
      regenerateFeaturePropCaches();
      cachesNeedUpdate = false;
    }

    if (analysis_type == null) {
      throw new NullPointerException("PropertyScheme.getFeatureProperty: type can't be null");
    }

    fp = (FeatureProperty)fpTypeToPropHash.get(analysis_type);

    if (fp == null) {
      fp = (FeatureProperty)fpAnalNameToPropHash.get(analysis_type);
    }

//    for (int i = 0; i < tiersVect.size() && fp == null; i++) {
//      TierProperty tp = (TierProperty) tiersVect.elementAt (i);
//      fp = tp.getFeatureProperty (analysis_type);
//    }
//
//    if (fp == null) {
//      for (int i = 0; i < tiersVect.size() && fp == null; i++) {
//        TierProperty tp = (TierProperty) tiersVect.elementAt (i);
//        fp = tp.featureForAnalysisType(analysis_type);
//      }
//    }
    
    if (fp == null && createProperty) {
      logger.info ("creating new feature property scheme for " + analysis_type);
      TierProperty tp = createTierProperty (analysis_type, true, false);
      fp = createFeatureProperty(tp, analysis_type, 
         FeatureProperty.DEFAULT_COLOR,
         FeatureProperty.DEFAULT_STYLE);
    }
    return fp;
  }
  
  /**
   * Returns the FeatureProperty associated with analysis_type.
   * <P>
   * The FeatureProperty will be the registered property if type is known, and
   * null otherwise.
   *
   * @param type  the String type of a feature
   * @return the FeatureProperty object for that type, or null
   * @throws NullPointerException if type is null
   */
  public FeatureProperty getFeatureProperty(String analysis_type) {
    return getFeatureProperty(analysis_type, true);
  }

  /**
   * Returns the TierProperty associated with either label or analysis_type.
   * <P>
   * The FeatureProperty will be the registered property if type is known, and
   * null otherwise.
   *
   * @param type  the String type of a feature
   * @return the FeatureProperty object for that type, or null
   * @throws NullPointerException if type is null
   */
  public TierProperty getTierProperty(String type, boolean dig) {
    TierProperty tp = (TierProperty) tiersHash.get (type.toLowerCase());
    if (tp == null && dig) {
      FeatureProperty fp = getFeatureProperty (type);
      tp = fp.getTier();
    }
    return tp;
  }

  /* 
   * SUZ
   * added this for backwards compatibility, but needed to add
   * a flag so that I could see if the tier existed without the
   * unwanted side-effect of creating it if it did not exist
   */
  public TierProperty getTierProperty(String type) {
    return getTierProperty (type, true);
  }

  public int getTierInd(String label) {
    for (int i=0; i < size(); i++) {
      TierProperty tp = (TierProperty) tiersVect.elementAt(i);
      if (tp.getLabel().equalsIgnoreCase(label)) {
        return i;
      }
    }
    FeatureProperty fp = getFeatureProperty (label);
    TierProperty tp = fp.getTier();
    return (tiersVect.indexOf (tp));
  }

  public Vector getAllTiers() {
    return tiersVect;
  }

  public Vector getCopyOfTiers(Vector source) {
    Vector result = (Vector) source.clone();
    return result;
  }

  /**
   * A method to read default type data from a file. This data is read 
   * on startup into a default types vector, 
   * which is copied when new FeatureTypes
   * objects are created.
   */
  private /*static*/ void readDefaultTypes(String fileName) {
    TiersIO reader = new TiersIO (this);
    comments = reader.doParse(fileName);
  }

  private void initBuiltInTypes() {
    /* there are 7 built-in types 
       start and stop codons in all 3 frames
       and the dragable bundle */
    String type;
    TierProperty tp;
    FeatureProperty fp;
    Color feature_color = FeatureProperty.DEFAULT_COLOR;
    
    type = "Start Codon";
    tp = createTierProperty (type, true, true);
    type = "startcodon_frame";
    feature_color = new Color(34,220,34);
    fp = createFeatureProperty (tp, type+"1", feature_color,
				"apollo.gui.drawable.SiteCodon");
    fp = createFeatureProperty (tp, type+"2", feature_color,
				"apollo.gui.drawable.SiteCodon");
    fp = createFeatureProperty (tp, type+"3", feature_color,
				"apollo.gui.drawable.SiteCodon");

    type = "Stop Codon";
    tp = createTierProperty (type, true, true);
    type = "stopcodon_frame";
    feature_color = Color.red;
    fp = createFeatureProperty (tp, type+"1", feature_color,
				"apollo.gui.drawable.SiteCodon");
    fp = createFeatureProperty (tp, type+"2", feature_color,
				"apollo.gui.drawable.SiteCodon");
    fp = createFeatureProperty (tp, type+"3", feature_color, 
				"apollo.gui.drawable.SiteCodon");
  }

  private TierProperty createTierProperty (String analysis_type,
					   boolean visible,
					   boolean expanded) {
    /* double check that it doesn't already exist before creating it */
    TierProperty tp = getTierProperty(analysis_type, false);
    if (tp == null) {
      tp = new TierProperty(analysis_type, visible, expanded);
      addTierType(tp);
    }
    return tp;
  }

  private FeatureProperty createFeatureProperty (TierProperty tp,
						 String analysis_type,
						 Color color,
						 String style) {
    FeatureProperty fp = tp.getFeatureProperty (analysis_type);
    if (fp == null) {
      Vector types = new Vector();
      types.addElement (analysis_type);
      fp = createDefaultFeatureProperty();
      //fp = new FeatureProperty(tp, analysis_type, types, color, style);
      fp.init(tp,analysis_type,types,color,style);
      // this needs to be ammended at some point to make sure getting style for
      // this property scheme - in multi curation may not be config.getStyle
      fp.setNameAdapterStringIfNotSet(Config.getStyle().getNameAdapter());
      setChanged();
      firePropSchemeChangeEvent();
    }


    return fp;
  }

  public void setDefaultFeatureProperty(FeatureProperty fp) {
    defaultFeatureProperty = fp;
  }

  public FeatureProperty createDefaultFeatureProperty() {
    return defaultFeatureProperty.cloneFeatureProperty();
  }

  /** list of linked props - presently this doesnt change over lifetime of prop scheme
      if that changes this can no longer be cached - or has to be invalidated. One
      possible scenario would be doing a layover with lined features. */
  private List linkedFeatProps=null;
  /** Retrieves all feature props that are synteny links 
      At some point we may want to include species as a parameter and only retrieve
      links between specified species. */
  public List getSyntenyLinkedFeatProps() {
    if (linkedFeatProps == null) {
      linkedFeatProps = new ArrayList(5);
      for (int i=0; i < getAllTiers().size(); i++) {
        linkedFeatProps.addAll(getTierProperty(i).getSyntenyLinkedFeatProps());
      }
    }
    return linkedFeatProps;
  }
  public int getLinkedFeatPropsSize() {
    return getSyntenyLinkedFeatProps().size();
  }
  public FeatureProperty getLinkedFeatProp(int i) {
    return (FeatureProperty)getSyntenyLinkedFeatProps().get(i);
  }

  /** by type we mean apollo feature type not result/analysis type */
  public boolean isTypeSyntenyLink(String type) {
    //List links = getSyntenyLinkedFeatProps();
    for (int i=0; i<getLinkedFeatPropsSize(); i++) {
      // Display type is the apollo feature type (not the result/analysis type)
      if (getLinkedFeatProp(i).getDisplayType().equals(type))
        return true;
    }
    return false;
  }

  /** list of all TierProperties that contain at least one linked FeatureProperty */
  private List linkedTierProps;
  public List getLinkedTierProps() {
    if (linkedTierProps==null) {
      linkedTierProps = new ArrayList(6);
      for (int i=0; i < getAllTiers().size(); i++) {
        TierProperty tp = getTierProperty(i);
        if (tp.hasLinkedFeatProps()) 
          linkedTierProps.add(tp);
      }
    }
    return linkedTierProps;
  }

  public TierProperty getLinkedTierProp(int i) {
    return (TierProperty)linkedTierProps.get(i);
  }
  

  private TierProperty getTierProperty(int i) {
    return (TierProperty)tiersVect.get(i);
  }

  public void write(File file) 
    throws IOException {
    TiersIO writer = new TiersIO (this);
    writer.doSave(file, comments);
  }

  /** Feature properties for annotation tier(s) are cached in the annotationFeatureProps
      vector */
  /** Moved from Style */
  public Vector getAnnotationFeatureProps() {
    // Set up annotationFeatureProps if it hasn't already been filled in
    if (annotationFeatureProps == null) {
      // There might be more than one annotation tier.
      // This should be specified in the tiers file--a tier should indicate
      // whether it contains curated annotations (rather than computational results).
      // Go through the tiers and see if any of them have isCurated==true.
      // But always count tier "Annotation" as curated, regardless.
      fillInAnnotationFeatureProps("Annotation");
      // Now look at other tiers and see if they are curated annots, too
      for (int i=0; i<size(); i++) {
        TierProperty tp = getTierProperty(i);
        logger.debug("Tier prop " + i + " = " + tp.getLabel());
        // Did "Annotation" already
        if (tp.isCurated() && !tp.getLabel().equalsIgnoreCase("Annotation"))
          fillInAnnotationFeatureProps(tp.getLabel());
      }

      if (annotationFeatureProps == null)  // No types belong to any annotation tier
        /* not sure what to do in this case
           suppose the 'right' thing would be to create
           a default "gene" annotation feature property
           ......later */
        annotationFeatureProps = new Vector(1);
    }
    return annotationFeatureProps;
  }

  /** The annotationFeatureProps vector will contain all the types (FeatureProperties)
      representing curated annots (rather than results) */
  private void fillInAnnotationFeatureProps(String tier) {
    TierProperty tp = getTierProperty(tier, false);
    if (tp != null) {
      Vector annot_types = tp.getFeatureProperties();
      if (annotationFeatureProps == null)
        annotationFeatureProps = new Vector(annot_types.size());
      for (int i = 0; i < annot_types.size(); i++) {
        FeatureProperty fp = (FeatureProperty) annot_types.elementAt(i);
        // Check first whether it's already in the vector?
        annotationFeatureProps.add(fp);
        logger.debug("added feature property " + fp.getDisplayType() + " to annotationFeatureProps");
      }
    }
  }

  /** Returns true if this tiername contains annotations rather than results.
      We check this by looking in the vector of annotationFeatureProps,
      which was filled in by a previous call to getAnnotationFeatureProps.
      Note: this doesn't seem to actually get called! yes it does by FeatProp */
  public boolean isAnnotationTier(String tier) {
    logger.trace("isAnnotationTier? tier = " + tier);
    Vector annot_types = getAnnotationFeatureProps();
    for (int k = 0; k < annot_types.size(); k++) {
      FeatureProperty annot_prop = (FeatureProperty) annot_types.elementAt(k);
      if (annot_prop.getTierName().equals(tier)) {
        logger.trace("is annot tier true");
        return true;
      }
    }
    logger.trace("is annot tier false");
    return false;
  }

  // Test main function
  public static void main(String [] Args) throws Exception {
    System.out.println("Before ft");
    PropertyScheme ft = new PropertyScheme();
    Vector types = ft.getAllTiers();

    System.out.println("\nft object");
    for (int i=0;i<types.size();i++) {
      TierProperty fp = (TierProperty)types.elementAt(i);
      System.out.println(fp.getLabel() + " " +
                         fp.isVisible() + " " +
                         fp.isExpanded());
    }
    System.out.println("\nft2 object");
    PropertyScheme ft2 = new PropertyScheme();
    types = ft2.getAllTiers();
    for (int i=0;i<types.size();i++) {
      TierProperty fp = (TierProperty)types.elementAt(i);
      System.out.println(fp.getLabel() + " " +
                         fp.isVisible() + " " +
                         fp.isExpanded());
    }
  }
}
