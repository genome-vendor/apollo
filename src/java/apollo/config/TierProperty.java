package apollo.config;

import apollo.gui.DetailInfo;
import apollo.util.*;
import java.awt.Color;
import java.util.*;
import java.io.*;

/** TierProperty holds a list of FeatureProperties. This corresponds to a "Tier" in
    the tiers file. Tiers are what is displayed in the Types panel (should probably be
    called TiersPanel, you have to right click on a tier you see all of its 
    types/feature properties)
    A Tier holds a bunch of "Types" (visual types), a Type is 
    captured by a FeatureProperty. Each FeatureProperty can have a different color, 
    etc... A "Tier" is the unit for collapsing and hiding, all FeatureProperties
    in a Tier expand and collapse together, hide and show together. 
    TierProperty is an Observable so it does a setChanged() and notifyObservers() when
    its changed. Observers add themselves with addObserver.
    PropertyScheme observes TierProperties. 
    Thats now changed. Observer stuff replaced with PropSchemeChangeEvent. */
public class TierProperty /*extends Observable*/
  implements Cloneable, /*Observer,*/ java.io.Serializable {

  protected String  label;
  protected boolean expanded = false;
  protected boolean visible = false;
  protected boolean sorted = false;
  protected boolean labeled = false;
  protected int     maxRow = 0;
  // Whether this tier represents curated data that should be displayed in the
  // annotation view rather than the result view
  protected boolean curated = false;
  /** warnOnEdit can be set at the Tier level (here) or at the individual FeatureProperty level */
  protected boolean warnOnEdit = false;
  protected Vector  comments = new Vector();

  protected CaseInsensitiveStringHash analysis_types
    = new CaseInsensitiveStringHash();
  private CaseInsensitiveStringHash   typesHash
    = new CaseInsensitiveStringHash();

  private PropertyScheme propertyScheme;

  /** A list of FeatureProperties */
  private Vector featProps = new Vector();

  public static boolean VISIBLE     = true;
  public static boolean NOT_VISIBLE = false;

  protected boolean isFirstTier = false;

  public TierProperty () {
    this("",true,false,false,0,false);
  }

  public TierProperty(String label, boolean visible, boolean expanded,
                      boolean sorted, int maxRow, boolean labeled) {
    this(label,visible,expanded,sorted,maxRow);
    setLabeled(labeled);
  }
  public TierProperty(String label, boolean visible, boolean expanded,
                      boolean sorted, int maxRow) {
    this(label,visible,expanded,sorted);
    setMaxRow(maxRow);
  }
  public TierProperty(String label, boolean visible, boolean expanded,
                      boolean sorted) {
    this(label,visible,expanded);
    setSorted(sorted);
  }
  public TierProperty(String label, boolean visible, boolean expanded) {
    this(label,visible);
    setExpanded(expanded);
  }

  public TierProperty(String label, boolean visible) {
    setLabel(label);
    setVisible(visible);
  }

  public TierProperty(String label) {
    this(label,true,false,false,0,false);
  }

  public TierProperty(TierProperty from) {
    this(from.getLabel(), from.isVisible(), from.isExpanded(),
         from.isSorted(), from.getMaxRow(), from.isLabeled());
  }

  void setPropertyScheme(PropertyScheme propertyScheme) {
    this.propertyScheme = propertyScheme;
  }

  /** Replaces notifyObservers() from Observable. Observable and PropSchemeChange
      seemed redundant and confusing */
  void firePropSchemeChangeEvent() {
    if (propertyScheme != null)
      propertyScheme.firePropSchemeChangeEvent();
  }

//   /** Update comes from observing FeatureProperty */
//   public void update(Observable obs,Object obj) {
//     //setChanged();
//     firePropSchemeChangeEvent();
//   }

  public void setLabel(String label) {
    if (label == null) {
      throw new NullPointerException("TierProperty.setLabel: can't accept null label");
    }
    this.label = label;
    //setChanged();
    firePropSchemeChangeEvent();
  }

  public String getLabel() {
    return this.label;
  }

  public void setVisible(boolean visible, boolean isTemporary)
  {
    this.visible = visible;
    if (!isTemporary) {
      firePropSchemeChangeEvent();
    }
  }
  
  public void setVisible(boolean visible) {
    setVisible(visible, false);
  }

  public boolean isVisible() {
    return this.visible;
  }

  public void setExpanded(boolean expanded, boolean isTemporary) {
    this.expanded = expanded;
    if (!isTemporary) {
      firePropSchemeChangeEvent();
    }
  }
  
  public void setExpanded(boolean expanded) {
    setExpanded(expanded, false);
  }

  public boolean isExpanded() {
    return expanded;
  }

  public void setSorted(boolean sorted) {
    this.sorted = sorted;
    firePropSchemeChangeEvent();
  }

  public boolean isSorted() {
    return sorted;
  }

  public void setMaxRow(int maxRow) {
    this.maxRow = maxRow;
    firePropSchemeChangeEvent();
  }

  public int getMaxRow() {
    return maxRow;
  }

  public void setLabeled(boolean labeled, boolean isTemporary) {
    this.labeled = labeled;
    if (!isTemporary) {
      firePropSchemeChangeEvent();
    }
  }
  
  public void setLabeled(boolean labeled) {
    setLabeled(labeled, false);
  }

  public boolean isLabeled() {
    return isLabeled(true);
  }

  public boolean isLabeled(boolean checkExpanded)
  {
    // 2/17/04: If tier is collapsed, don't want to show labels.  But we also
    // don't want to change the value of labeled, because if user expands tier,
    // should show label.  So check whether we're expanded before answering.
    if (checkExpanded && !expanded) {
      return false;
    }
    return labeled;
  }
  
  /** Whether this tier represents curated annotations (to be shown in the
      annotation view rather than result view).  Default is false. */
  public void setCurated(boolean curated) {
    this.curated = curated;
    firePropSchemeChangeEvent();
  }

  public boolean isCurated() {
    return curated;
  }
  
  /** Whether the user should be warned if they try to edit features of a given type.
      Default is false.  Applies to all types belonging to this tier; can be overridden
      by individual types. */
  public void setWarnOnEdit(boolean warn) {
    warnOnEdit = warn;
//     // Set for each type belonging to this tier
    // (Can't do, don't have types yet when tier is first set up)
//     for (int j = 0; j < featProps.size(); j++) {
//       FeatureProperty fp = (FeatureProperty)featProps.elementAt(j);
//       fp.setWarnOnEdit(warn);
//     }
    firePropSchemeChangeEvent();
  }

  public boolean warnOnEdit() {
    return warnOnEdit;
  }

  protected String quoteIfSpace(String str) {
    if (str.indexOf(" ") > -1 || str.indexOf("/") > -1) {
      return new String("\"" + str + "\"");
    } else {
      return str;
    }
  }

  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  public FeatureProperty getFeatureProperty(String type) {
    // SMJS Hash is case insensitive so shouldn't need toLowerCase
    // return (FeatureProperty) typesHash.get(type.toLowerCase());
    return (FeatureProperty) typesHash.get(type);
  }

  public Vector getFeatureProperties() {
    return featProps;
  }

  public void addFeatureProperty (FeatureProperty fp) {
    if ( ! holdsType (fp.getDisplayType()) ) {
      //      System.out.println ("Adding type " + fp.getDisplayType() +
      // " to tier " + getLabel());
      typesHash.put(fp.getDisplayType().toLowerCase(), fp);
      featProps.addElement (fp);
      fp.setTier (this);
      //fp.addObserver(this);
      fp.inheritWarnOnEdit(warnOnEdit());  // must call fp.setTier first
    }
    addAnalysisTypes (fp, fp.getAnalysisTypes ());
  }

  private void addAnalysisTypes (FeatureProperty fp, Vector analysis_types) {
    for (int i = 0; i < analysis_types.size(); i++) {
      String analysis = (String) analysis_types.elementAt (i);
      addAnalysisType (fp, analysis);
    }
  }

  public void addAnalysisType (FeatureProperty fp, String analysis) {
    analysis_types.put (analysis, fp);
  }

  public FeatureProperty featureForAnalysisType (String analysis_type) {
    //return ((FeatureProperty)
    //        analysis_types.get (analysis_type.toLowerCase()));
    return ((FeatureProperty)
            analysis_types.get (analysis_type));
  }

  public String getTypesAsString() {
    StringBuffer buff = new StringBuffer();
    for (int i = 0; i < featProps.size(); i++) {
      FeatureProperty fp = (FeatureProperty)featProps.elementAt(i);
      buff.append ("\ttype: " + fp.getDisplayType() + " {");
      Vector types = fp.getAnalysisTypes();
      for (int k = 0; k < types.size(); k++) {
        buff.append (" " + (String) types.elementAt(k));
      }
      buff.append ("}\n");
    }
    buff.append("\n");
    return buff.toString();
  }

  public boolean labelEquals(String name) {
    return (label.equalsIgnoreCase(name));
  }

  public boolean holdsType(String type) {
    return (typesHash.containsKey(type.toLowerCase()));
  }

  public Color getColour () {
    if (featProps.size() > 0)
      return (((FeatureProperty) featProps.elementAt(0)).getColour());
    else
      return FeatureProperty.DEFAULT_COLOR;
  }

  private boolean isProtein=false;
  void setIsProtein(boolean isProtein) {
    this.isProtein = isProtein;
  }
  /** Default is false */
  public boolean isProtein() { 
    return isProtein;
  }

  protected void addComment (String comment) {
    comments.addElement (comment);
  }

  protected Vector getComments () {
    return comments;
  }

  protected void setIsFirstTier(boolean firstTier) {
    isFirstTier = firstTier;
  }
  public boolean isFirstTier() {
    return isFirstTier;
  }

  private FeatureProperty getFeatureProperty(int i) {
    return (FeatureProperty)featProps.get(i);
  }

  /** Return list of all feat props that are synteny links */
  List getSyntenyLinkedFeatProps() {
    List linkedFeatProps = new ArrayList();
    for (int i=0; i<featProps.size(); i++) {
      FeatureProperty fp = getFeatureProperty(i);
      if (fp.isSyntenyLinked()) {
        linkedFeatProps.add(fp);
      }
    }
    return linkedFeatProps;
  }
  boolean hasLinkedFeatProps() {
    return getSyntenyLinkedFeatProps().size() != 0; // inefficient? cache?
  }
  
  public String toString()
  {
    return getLabel();
  }
}
