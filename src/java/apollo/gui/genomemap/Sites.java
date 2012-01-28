package apollo.gui.genomemap;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;

import javax.swing.*;

import apollo.datamodel.*;
import apollo.gui.drawable.SiteCodon;
import apollo.gui.event.*;

import org.apache.log4j.*;

/** 
 * Sites holds an array of integers which are the base positions for each site
 * in a given frame and strand. Nothing about vertical placement is stored here. 
 * The positions are ordered in the order that they are added (addSite).
 * Currently SiteView._createSites adds forward strand sites in ascending order,
 * reverse strand sites in desceding order.
 * No state as to whether start or stops stored here. That's in SiteView.
 */

public class Sites {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(Sites.class);

  static int initialSize = 100;
  static int growAmount = 100;

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  /** Positions in basepairs of all the codons */
  private int [] positions;
  private HashSet selected = new HashSet(5);
  private Hashtable siteCodons = new Hashtable(3); // for dragging
  //Hashtable highlighted; not used

  int nSite = 0;
  int currentSize = initialSize;

  public Sites() {
    clearSites();
  }

  public void addSite(int pos) {
    if (nSite == currentSize-1) {
      int [] tmpPos = new int[currentSize + growAmount];
      System.arraycopy(positions,0,tmpPos,0,currentSize);
      currentSize = currentSize + growAmount;
      positions = tmpPos;
    }
    positions[nSite++] = pos;
  }

  public int size() {
    return nSite;
  }

  public int elementAt(int ind) {
    if (ind >=0 && ind < nSite) {
      return (int)positions[ind];
    } else {
      logger.error("Index " + ind + " not valid in Sites");
      return -1;
    }
  }

  /** Clear out positions array, doesnt touch selection state */
  void clearPositions() {
    positions = new int[initialSize];
    nSite = 0;
    currentSize = initialSize;
  }

  public void clearSites() {
    clearPositions();
    selected.clear();
    // highlighted = new Hashtable(); // not used
  }

//   public void clearHighlights() { // not used
//     highlighted = new Hashtable();
//   }

  /** int of site to mark selected 
      Presently there is no checking if pos actually exists as a site should there be?
      SiteView.handleFeatSel calls selectSite without knowing if site exists.
      Should it check or should Sites check? separate method with checking?
   */
  public void selectSite(int pos,boolean state) {
    //if (!siteExistsAtPostion(pos)) return; ???
    if (state)
      selected.add(new Integer(pos));
    else
      selected.remove(new Integer(pos));
  }

  /** returns whether a codon exists at position */
  boolean siteExistsAtPosition(int position) {
    // linear search inefficient? binary search?
    for (int i=0; i<size(); i++) {
      if (elementAt(i) == position) return true;
    }
    return false;
  }

  boolean isSelected(int pos) {
    return selected.contains(new Integer(pos));
  }

  public void clearSelected() {
    selected.clear();
  }

  public SiteCodon getSiteCodon (SeqFeatureI sf) {
    return getSiteCodon (sf.getName());
  }

  public SiteCodon getSiteCodon (String name) {
    return (SiteCodon) siteCodons.get (name);
  }

  /** low is not start. low is absolute low. low is lower than low on both 
      strands */
  public SiteCodon getSiteCodon(int low,int high,
                                String type, int strand,
                                String namePrefix) {
    String name = namePrefix + " at " + low;
    SiteCodon site = getSiteCodon (name);
    if (site == null) {
      SeqFeature sf = new SeqFeature(low,high,type,strand);
      sf.setName(name);
      sf.setScore(100.0);
      site = new SiteCodon(sf,this);
      // This is so that we can locate the drawable
      // when all that is in the selection is the feature
      // so that dragging will work
      siteCodons.put(sf.getName(), site);
    }
    return site;
  }
}

