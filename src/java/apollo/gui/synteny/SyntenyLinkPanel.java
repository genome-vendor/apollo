package apollo.gui.synteny;

import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;

import apollo.dataadapter.*;
import apollo.datamodel.*;
import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.config.PropertyScheme;
import apollo.config.PropSchemeChangeEvent;
import apollo.config.PropSchemeChangeListener;
import apollo.gui.Controller;
import apollo.gui.ControlledObjectI;
import apollo.gui.SelectionManager;
import apollo.gui.StatusBar;
import apollo.gui.Transformer;
import apollo.gui.genomemap.*;
import apollo.gui.event.*;
import apollo.gui.menus.*;
import apollo.gui.drawable.*;
import apollo.util.*;

import java.io.*;
import gov.sandia.postscript.*;

import org.apache.log4j.*;

public class 
  SyntenyLinkPanel 
extends 
  JPanel
implements 
  BaseFocusListener,
  ChainedRepaintListener,
  FeatureSelectionListener,
  //MouseListener,
  MouseMotionListener,
  RubberbandListener,
  PropSchemeChangeListener,
  ZoomListener,
  ScrollListener
{

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(SyntenyLinkPanel.class);

  /** Is there some way to query this from tiers file?? or pick this off of link 
      features? */
  private final static String dnaLinkTypeString = "dna-dna-align";
  private final static String protLinkTypeString = "protein-protein-align";

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  LinkSet links;

  Transformer trans1;
  Transformer trans2;

  //int maxDist;
  boolean autoStraighten = true;
  // int minSize; -- not used

  RubberbandRectangle rubberband;

  //int group; // not used - what was this for?

  StrandedZoomableApolloPanel szap1;
  StrandedZoomableApolloPanel szap2;

  Hashtable linkHash;
  Vector    polyHolder;
  private ArrayList drawables = new ArrayList();
  // drawables sorted in order of area. 
  // Does this need to be a separate data structure from drawables?
  // Cant the drawables just be sorted for size? and why sort by size?
  //private ArrayList sortedDrawables;
  
  boolean useOpaqueLinks = Config.getStyle().useOpaqueLinks();
  boolean shadeByPercId;
  
  StatusBar statusbar;

  /** A LinkSet which are the selected links */
  //private FeatureList selectedLinks;
  private LinkSet selectedLinks;
  /** The SyntenyPanel that holds this SyntenyLinkPanel */
  private SyntenyPanel syntenyPanel;

  /** Maps color to PropertyScorePaints */
  private HashMap colorToScorePaint = new HashMap(2);

  java.util.Comparator polygonComparator = 
    new java.util.Comparator(){
      public int compare(Object polygon1, Object polygon2){
        double result = ((SyntenyLinkPolygon)polygon1).getSize() - ((SyntenyLinkPolygon)polygon2).getSize();
        if(result > 0){
          return 1;
        }else if(result < 0){
          return -1;
        }else{
          return 0;
        }
      }
    };
      
  public SyntenyLinkPanel(StrandedZoomableApolloPanel szap1,
                          StrandedZoomableApolloPanel szap2) {
    this.trans1 = szap1.getTransformer();
    this.trans2 = szap2.getTransformer();
    //setMaxDist(maxDist); -- pase
    //this.minSize = size;
    //this.group = group; not used
    this.linkHash = new Hashtable();
    //this.links = groupLinks(fset,maxDist);
    this.szap1 = szap1;
    this.szap2 = szap2;
    addMouseListener(new LinkMouseListener());
    addMouseMotionListener(this);

    rubberband  = new RubberbandRectangle(this);
    rubberband.setActive(true);
    rubberband.addListener(this);
    
    //sortedDrawables = new ArrayList();
  }
  
  Transformer getQueryTransformer() { return trans1; }
  Transformer getHitTransformer() { return trans2; }

  void setSyntenyPanel(SyntenyPanel syntenyPanel) { 
    this.syntenyPanel = syntenyPanel; 
  }

  public void print(File file, String orientation, String scale) {
    try {
      PrintWriter fw = new PrintWriter(new FileWriter(file));
      Graphics psg = new PSGr2(fw);

      double scaleVal = 1.0;
      try {
        scaleVal = new Double(scale).doubleValue();
      } catch (Exception e) {
        logger.error("Invalid scale factor", e);
        return;
      }

      if (orientation.equals("landscape")) {
        fw.println("-30 30 translate");
        fw.println("90 rotate");
        //fw.println("30 -642 translate");
        fw.println("" + ((int)(30.0*scaleVal)) + " " + ((int)(-822.0*scaleVal)) + " translate");
        fw.println("" + scale + " " + scale + " scale");
      } else {
        fw.println("-30 30 translate");
        int yOffset = (int)(762.0 - 792.0*scaleVal );
        fw.println("30 " + yOffset + " translate");
        fw.println("" + scale + " " + scale + " scale");
      }

      invalidate();
      psg.setClip(new Rectangle(0,0,getSize().width,getSize().height));
      logger.debug("clip bounds in print = " + psg.getClipBounds());
      paintComponent(psg);
      psg.dispose();
      fw.close();
    } catch (Exception e) {
      logger.error("Failed printing to file " + file, e);
    }
  }


 /**
   * We will be fed SeqFeature's which are either genes, transcripts
   * or exons. Our best guess -for now- as to when we have found the
   * gene feature is when the biotype of the SeqFeature is no_type
   This returns the highest level(ancsestor) seq feature that has a biotype.
   gene,transcript, and exon does not refer to Gene,Transcript, and Exons 
   annotation objects, as these very well may be results.
  **/
  private SeqFeatureI getGeneFeature(SeqFeatureI childFeature) {
    //    SeqFeatureI returnFeature = null;

    while(
      childFeature.getRefFeature() != null &&
      childFeature.getRefFeature().getTopLevelType() != null &&
      !childFeature.getRefFeature().getTopLevelType().equals(Range.NO_TYPE)
    ) {
      childFeature = childFeature.getRefFeature();
    }//end while

    return childFeature;
  }//end getGeneFeature



  private void setPanelCentres(Point pos1, Point pos2, boolean straighten) {
    // Adjust pos2 to straighten polys
    if (straighten) {
      straightenLinks(pos1,pos2);
    }
    if (szap1 != null) {
      szap1.getController().handleBaseFocusEvent(new BaseFocusEvent(this,pos1.x,null));
    }

    if (szap2 != null) {
      szap2.getController().handleBaseFocusEvent(new BaseFocusEvent(this,pos2.x,null));
    }
    // repaint();??
  }

  private void straightenLinks(Point pos1, Point pos2) {
    Transformer tmpTrans1 = null;
    Transformer tmpTrans2 = null;
    try {
      tmpTrans1 = (Transformer)trans1.clone();
      tmpTrans1.setXCentre(pos1.x);
      tmpTrans2 = (Transformer)trans2.clone();
      tmpTrans2.setXCentre(pos2.x);
    } catch (CloneNotSupportedException e) {
      logger.error(e.getMessage(), e);
    }

    Rectangle bounds1 = tmpTrans1.getPixelBounds();
    Rectangle bounds2 = tmpTrans2.getPixelBounds();

    double avAng = 0.0;
    int nAng = 0;

    PropertyScheme pscheme = Config.getPropertyScheme();

    for (int i = 0; i < links.size(); i++) {
      Link link = links.getLink(i);

      boolean isVis = true;
      if (link.getType() != null) {
        FeatureProperty property = pscheme.getFeatureProperty(link.getType());
        isVis = property.getTier().isVisible();
      } else {
        logger.warn("Type is null for link " + link);
      }
      if (isVis) {

        int startpixel1 = tmpTrans1.toPixelX(link.getLow1());
        int endpixel1   = tmpTrans1.toPixelX(link.getHigh1());

        int startpixel2 = tmpTrans2.toPixelX(link.getLow2());
        int endpixel2   = tmpTrans2.toPixelX(link.getHigh2());

        int lowpixel1;
        int highpixel1;
        if (startpixel1 > endpixel1) {
          lowpixel1 = endpixel1;
          highpixel1 = startpixel1;
        } else {
          lowpixel1 = startpixel1;
          highpixel1 = endpixel1;
        }

        int lowpixel2;
        int highpixel2;
        if (startpixel2 > endpixel2) {
          lowpixel2 = endpixel2;
          highpixel2 = startpixel2;
        } else {
          lowpixel2 = startpixel2;
          highpixel2 = endpixel2;
        }


        if (!(highpixel1 < bounds1.x) && !(lowpixel1 > (bounds1.x + bounds1.width)) ||
            !(highpixel2 < bounds2.x) && !(lowpixel2 > (bounds2.x + bounds2.width))) {
          int midpixel1 = (highpixel1-lowpixel1)/2 + lowpixel1;
          int midpixel2 = (highpixel2-lowpixel2)/2 + lowpixel2;
          double angle = Math.atan2((midpixel2-midpixel1),getBounds().y);
          avAng += angle;
          nAng++;
        }
      }
    }
    if (nAng != 0) {
      avAng = avAng/nAng;
      double moveDist = Math.tan(avAng)*(getBounds().y);
      Point pnt = tmpTrans2.toUser((int)(moveDist+tmpTrans2.getPixelBounds().x + tmpTrans2.getPixelBounds().width/2),0);
      pos2.x = pnt.x;
    }
  }

  /**
   * <p>If we had a FeatureSelectionEvent fired AT us, then we </p>
   * <ol>
   * <li> For ensembl: Find the stable gene id corresponding to the feature selected -
   * I expect the selection(s) will be either exons (for a 
   * selection in each ResultView) or a FeaturePair's worth of 
   * stable geneid's (if the selection was made in the link panel). </li>
   *
   * <li> In the case of selected exons, search the FeatureSet of cross-species
   * Homology links to find the stable gene id(s) MATCHING the selected 
   * stable gene id's. </li>
   *
   * <li> Find the exons corresponding to the homologous gene we just retrieved
   * and fire a selection event on each of them.
   * </ol>
  **/
  public boolean handleFeatureSelectionEvent(FeatureSelectionEvent event) {
    //dont do anything if we are the source of the event
    if(event.getSource().equals(this)) {
      return false;
    }
    // Deselect currently selected links
    deselectLinks();
    if (links.linkedByName()) {
      doNamedFeatureSelection(event);
    }
    else {
      doRegularFeatureSelection(event);
    }
    repaint(); // repaint for selected link change
    return true;
  }// end of handleFeatureSelectionEvent

  // a lot of this could be done in LinkSet
  private void doNamedFeatureSelection(FeatureSelectionEvent event) {
    ArrayList foundFeatures = new ArrayList();
    HashSet linkNameSet = new HashSet();
    FeatureList selectedFeatures = event.getFeatures();
    for (int i=0; i<selectedFeatures.size(); i++) {
      SeqFeatureI selectedFeature = selectedFeatures.getFeature(i);
      // If the links are gene to gene, we need to get the gene name to search the
      // links with
      if (Config.getStyle().syntenyLinksAreGeneToGene()) {
        selectedFeature = getGeneFeature(selectedFeature);
      }

      //We don't want to fire events on two exons belonging to the same parent gene, 
      // so only take action on distinct genes here.
      if(selectedFeature != null && !foundFeatures.contains(selectedFeature) && 
         selectedFeature.getName() != RangeI.NO_NAME) {
        foundFeatures.add(selectedFeature);
        // Search links for the feat that has been selected
	logger.debug("Name for selected link to look for is " + selectedFeature.getName());  
        LinkSet linkedLinks = links.findLinksWithName(selectedFeature.getName());

        int linkedLinksSize = linkedLinks.size();

        for (int j=0; j<linkedLinksSize; j++) {
          Link link = linkedLinks.getLink(j);
          linkNameSet.add(link.getName1());
          linkNameSet.add(link.getName2());
          addToSelection(link);
        }

        String[] linkNamesArray = (String [])linkNameSet.toArray(new String[linkNameSet.size()]);

        //Iterator iterator = linkNameSet.iterator();
        //String[] linkNamesArray = new String[linkNameSet.size()];
        //int counter = 0;

        //while(iterator.hasNext()) {
        //  linkNamesArray[counter] = (String)iterator.next();
        //  counter++;
        //}//end while

        fireNamedFeatureSelectionEvent(this, event.getSource(), linkNamesArray);
      }//end if
    }//end for loop on selectedFeatures
  }

  private void doRegularFeatureSelection(FeatureSelectionEvent event) {
    FeatureList selectedFeats = event.getFeatures();
    FeatureList linkedFeats = getSelectedLinkedSpeciesFeats(selectedFeats);
    if (linkedFeats.isEmpty()) {
      return;
    }
    // should source be this or source of original event?
    //FeatureSelectionEvent e = new FeatureSelectionEvent(this,linkedFeats);
    // fire to controller of linked feats
    // all feats should be from same species
    SelectionManager s = getSelectionManagerForFeature(linkedFeats.getFeature(0));
    s.select(linkedFeats,true,false,this);
  }

  
  /** For the species feat passed in find all (if any) of the feats of the other species
      that is linked to. This is for links that have species features, links by names
      have to do this linking with a name search of course.
      Returns a list of linked features.
      Links involved in selection are added to internal selection list via
      addToSelection.
   */
  private FeatureList getSelectedLinkedSpeciesFeats(FeatureList selectedFeats) {
    FeatureList linkedFeats = new FeatureList();
    for (int i=0; i<selectedFeats.size(); i++) {
      SeqFeatureI selectedFeat = selectedFeats.getFeature(i);
      // have to go through all links as can be many to one
      for (int j=0; j<links.size(); j++) {
        Link link = links.getLink(j);
        SeqFeatureI linkedFeat = link.getLinkedSpeciesFeature(selectedFeat);
        if (linkedFeat!=null) { // if non-null we connected
          linkedFeats.addFeature(linkedFeat);
          addToSelection(link);
        }
      }
    }
    return linkedFeats;
  }

  private SelectionManager getSelectionManagerForFeature(SeqFeatureI feat) {
    if (featFromSzap1(feat)) {
      return szap1.getSelectionManager();
    }
    else {
      return szap2.getSelectionManager();
    }
  }

  private boolean featFromSzap1(SeqFeatureI feat) {
    String species1 = szap1.getCurationSet().getOrganism();
    return feat.getRefSequence().getOrganism().equals(species1);
  }

  /**
   * Fire the event at both controllers: the relevant panels should ignore it if
   * they fired the event in the same place.
   Actually this queries the originalEventSource, if its a ControlledObjectI then
   it gets it doesnt fire at the ControlledObjectI's controller. 
  **/
  private void fireNamedFeatureSelectionEvent(
    Object source,
    Object originalEventSource,
    String[] featureNames
  ) {
    Controller controllerToOmit = null;
    boolean propagateToSZAP1 = true, propagateToSZAP2 = true;

    if(originalEventSource instanceof ControlledObjectI) {
      controllerToOmit = ((ControlledObjectI)originalEventSource).getController();

      propagateToSZAP1 = !szap1.getController().equals(controllerToOmit);
      propagateToSZAP2 = !szap2.getController().equals(controllerToOmit);

    } else if (originalEventSource instanceof Controller) {
      controllerToOmit = (Controller)originalEventSource;

      propagateToSZAP1 = !szap1.getController().equals(controllerToOmit);
      propagateToSZAP2 = !szap2.getController().equals(controllerToOmit);

    } else if (originalEventSource instanceof SyntenyLinkPanel) {

// In this case this must have been a resourced event which is bouncing
// up the SZAPs in a 3(or more)way synteny view
// We don't want to propagate back to the panels 

      propagateToSZAP1 = !((SyntenyLinkPanel)originalEventSource).isManagingSZAP(szap1);
      propagateToSZAP2 = !((SyntenyLinkPanel)originalEventSource).isManagingSZAP(szap2);
    }

    if(propagateToSZAP1) {
      szap1.getController().handleNamedFeatureSelectionEvent(
        new NamedFeatureSelectionEvent(source, featureNames)
      );
    }

    if(propagateToSZAP2) {
      szap2.getController().handleNamedFeatureSelectionEvent(
        new NamedFeatureSelectionEvent(source, featureNames)
      );
    }
  }//end fireNamedFeatureSelectionEvent

  public boolean isManagingSZAP(StrandedZoomableApolloPanel szap) {
    return (szap1 == szap || szap2 == szap);
  }

  private void fireZoomEvent(
    ZoomEvent event
  ){
    Controller controllerToOmit = null;
    Object originalEventSource = event.getSource();
    boolean propagateToSZAP1 = true, propagateToSZAP2 = true;

    if(originalEventSource instanceof ControlledObjectI) {
      controllerToOmit = ((ControlledObjectI)originalEventSource).getController();

      propagateToSZAP1 = !szap1.getController().equals(controllerToOmit);
      propagateToSZAP2 = !szap2.getController().equals(controllerToOmit);

    } else if (originalEventSource instanceof SyntenyLinkPanel) {

// In this case this must have been a resourced event which is bouncing
// up the SZAPs in a 3(or more)way synteny view
// We don't want to propagate back to the panels 

      propagateToSZAP1 = !((SyntenyLinkPanel)originalEventSource).isManagingSZAP(szap1);
      propagateToSZAP2 = !((SyntenyLinkPanel)originalEventSource).isManagingSZAP(szap2);
    }

    logger.debug("controllerToOmit = " + controllerToOmit);
    logger.debug("Event source = " + originalEventSource);

    if (propagateToSZAP1) {
      logger.debug("Doing szap1 hZE");
      szap1.getController().handleZoomEvent(new ZoomEvent(this, true, event.getZoomFactor(),event.getFactorMultiplier()));
    }

    if (propagateToSZAP2) {
      logger.debug("Doing szap2 hZE");
      szap2.getController().handleZoomEvent(new ZoomEvent(this, true, event.getZoomFactor(),event.getFactorMultiplier()));
    }

    logger.debug("Link panel hZE for panel " + this + " is done with the event");
  }//end fireZoomEvent
    
  /** Rubberbanding a link centers on the link (no selection) */
  public boolean handleRubberbandEvent(RubberbandEvent evt) {
    Rubberband rb = (Rubberband) evt.getSource();
    DrawableLink foundLink = null;

    if (rb.getComponent() == this) {
      foundLink = findDrawableLinkInRect(evt.getBounds());
    }
    Point pos1;
    Point pos2;
    if (foundLink != null) {
      pos1 = foundLink.getQueryMidUserPoint();
      pos2 = foundLink.getHitMidUserPoint();
      fireSelectionForSeqHighlight(foundLink);
      setPanelCentres(pos1,pos2,false);
    }
    repaint();
    return true;
  }

  public boolean syncScales() {

    int curWidth1 = szap1.getApolloPanel().getVisibleBasepairWidth();
    int curWidth2 = szap2.getApolloPanel().getVisibleBasepairWidth();

    if (curWidth1 > curWidth2) {
      int [] limits = new int[2];
      limits[0] = szap1.getApolloPanel().getLinearCentre() - curWidth2/2;
      limits[1] = szap1.getApolloPanel().getLinearCentre() + curWidth2/2;
      szap1.zoomToWidth(curWidth2,limits);
    } else {
      int[] limits = new int[2];
      limits[0] = szap2.getApolloPanel().getLinearCentre() - curWidth1/2;
      limits[1] = szap2.getApolloPanel().getLinearCentre() + curWidth1/2;
      szap2.zoomToWidth(curWidth1,limits);
    }


    repaint();

    return true;
  }

  public boolean getAutoStraighten() {
    return autoStraighten;
  }
  public void setAutoStraighten(boolean state) {
    autoStraighten = state;
  }

  public void setStatusBar(StatusBar bar) {
    this.statusbar = bar;
  }

  private AlphaComposite makeComposite(float alpha) {
    int type = AlphaComposite.SRC_OVER;
    return AlphaComposite.getInstance(type,alpha);
  }

  /** A whole new set of SyntenyLinkPolygons get created with every paint.
      Is this inefficient? */
  public void paintComponent(Graphics g) {
    g.setColor(Config.getCoordBackground());
    g.fillRect(0,0,getSize().width,getSize().height);

    //g.setColor(Color.white);
    //g.drawRect(0,0,getSize().width-1,getSize().height-1);

    g.setColor(Color.red);

    Rectangle bounds1 = trans1.getPixelBounds();
    Rectangle bounds2 = trans2.getPixelBounds();

    
    Graphics2D g2d = null;
    if (g instanceof Graphics2D) {
      g2d = (Graphics2D)g;

      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
      g2d.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_SPEED);
      g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);
      //g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,RenderingHints.VALUE_COLOR_RENDER_SPEED);
    }

    //drawables = new ArrayList();
    drawables.clear();
    polyHolder = new Vector();
    //getSortedDrawables().clear();
    // make feat list for drawables?
    ArrayList selectedDrawableLinks = new ArrayList();

    logger.debug("Making links");
    for (int i = 0; i < links.size(); i++) {
      //FeaturePairI fp = links.getLink(i);
      Link link = links.getLink(i);

      //      int[] range1 = (trans1.getXVisibleRange());
      //      int[] range2 = (trans2.getXVisibleRange());

      //      int r1 = 0;//(int)(range1[1] - range1[0] + 1);
      //      int r2 = 0;//(int)(range2[1] - range2[0] + 1);

      int startpixel1 = trans1.toPixelX(link.getLow1());
      int endpixel1   = trans1.toPixelX(link.getHigh1());

      int startpixel2 = trans2.toPixelX(link.getLow2());
      int endpixel2   = trans2.toPixelX(link.getHigh2());

      int lowpixel1;
      int highpixel1;
      if (startpixel1 > endpixel1) {
        lowpixel1 = endpixel1;
        highpixel1 = startpixel1;
      } else {
        lowpixel1 = startpixel1;
        highpixel1 = endpixel1;
      }

      int lowpixel2;
      int highpixel2;
      if (startpixel2 > endpixel2) {
        lowpixel2 = endpixel2;
        highpixel2 = startpixel2;
      } else {
        lowpixel2 = startpixel2;
        highpixel2 = endpixel2;
      }

      //            if (endpixel1 > bounds1.x && endpixel1 < (bounds1.x + bounds1.width) ||
      //                endpixel2 > bounds2.x && endpixel2 < (bounds2.x + bounds2.width)) {
      if (!(highpixel1 < bounds1.x) && !(lowpixel1 > (bounds1.x + bounds1.width)) ||
          !(highpixel2 < bounds2.x) && !(lowpixel2 > (bounds2.x + bounds2.width))) {
        logger.debug("lowpix1 = " + lowpixel1 + " highpix1 = " + highpixel1 +
                     " lowpix2 = " + lowpixel2 + " highpix2 = " + highpixel2);

        // Should put an os check in here for linux -- why?
        //g.setColor(Color.red);
        //g.drawLine(endpixel1,0,endpixel2,getSize().height);
        //g.setColor(Color.green);
        //g.drawLine(startpixel1,0,startpixel2,getSize().height);

        DrawableLink drawableLink=null;

        String name = link.getName1() + " " + 
          link.getLow1() + "-" +
          link.getHigh1() + " " + 
          link.getName2() + " " +
          link.getLow2() + "-" + 
          link.getHigh2() + " " +
          link.getStrand1();
        drawableLink = new SyntenyLinkPolygon(link,name,this);

        drawables.add(drawableLink);
        //        boolean success = getSortedDrawables().add(drawableLink);
        
        drawableLink.setQueryPixels(lowpixel1,highpixel1,0);

        /* We don't respond to strand information: just draw everything as a 
         * parallelogram and leave it to the user to work out when orientation has changed.
         */
        drawableLink.setHitPixels(highpixel2,lowpixel2,getSize().height);


        if (g2d != null){
          g2d.setColor(drawableLink.getColour());
          if (!getUseOpaqueLinks()) {
            g2d.setComposite(makeComposite((float)0.6));
          }
        }
        if (getSelectedLinks().contains(link)) {
          drawableLink.setSelected(true);
          selectedDrawableLinks.add(drawableLink);
        }
//         else { // only draw unselected - selected drawn later
//           drawableLink.draw(g2d);
//         }
      }
      
    } // end of for loop on links
    logger.debug("Done making link drawables (made " + drawables.size() + ")");
    
    // sort from smallest to largest
    logger.debug("Sorting link drawables");
    Collections.sort(drawables,polygonComparator);
    logger.debug("Done sorting link drawables");


    if (g2d != null) {
      logger.debug("Current rendering hints");
      logger.debug(g2d.getRenderingHints());
    }

    logger.debug("Drawing link drawables");
    // draw the unselected from largest to smallest
    double minP2CForFullDraw = 0.005;
    for (int i=drawables.size()-1; i>=0; i--) {
      DrawableLink dl = getDrawableLink(i);
      if (!selectedDrawableLinks.contains(dl)) {
        //dl.draw(g2d);
        dl.draw(g,(trans1.getXPixelsPerCoord() > minP2CForFullDraw && trans2.getXPixelsPerCoord() > minP2CForFullDraw));
      }
    }

    // Draw selected links on top
    for (int i=0; i<selectedDrawableLinks.size(); i++) {
      //((DrawableLink)selectedDrawableLinks.get(i)).draw(g2d);
      ((DrawableLink)selectedDrawableLinks.get(i)).draw(g,true);
    }

    logger.debug("Done drawing link drawables");
    // sorts by size - pase? delete? smallest to largest?
    //Collections.sort(getSortedDrawables(),polygonComparator);
      
  } // end of paintComponent()

  public boolean handleBaseFocusEvent(BaseFocusEvent evt) {
    repaint();
    return true;
  }

  public boolean handlePropSchemeChangeEvent(PropSchemeChangeEvent evt) {
    repaint();
    return true;
  }

  /**
   * Zoom events are delivered by one of the two controllers I listen to. They trigger
   * - a propagation (if the user zoomed with the SHIFT key held)
   * of the zoom event to the "other" controller (in turn causing a redraw
   * of a stranded-zoomable-panel etc), and 
   * - a repaint in this panel: that will re-display the appropriate links given the szap's have 
   * zoomed.
  **/
  public boolean handleZoomEvent(ZoomEvent evt) {
    if(evt.isPropagated()){
      fireZoomEvent(evt);
    }//end if
    repaint();
    return true;
  }

  /** Creates 2 new features from the ranges of hit and query of links feature 
      pair - the purpose of this is to highlight sequence in the axis but its 
      currently not working */
  private void fireSelectionForSeqHighlight(DrawableLink foundDrawableLink) {
    Link link = foundDrawableLink.getLink();
    SeqFeature fake1 = new SeqFeature (link.getLow1(), link.getHigh1(),
                                       link.getType(), link.getStrand1());
    Vector features1 = new Vector();
    features1.addElement (fake1);
    szap1.getSelectionManager().select(features1, this);

    SeqFeature fake2 = new SeqFeature (link.getLow2(),link.getHigh2(),
                                       link.getType(), link.getStrand2());
    Vector features2 = new Vector();  
    features2.add(fake2);
    szap2.getSelectionManager().select(features2, this);
  }

  public void mouseMoved(MouseEvent evt) {
    int pos1 = trans1.toUser(evt.getX(),0).x;
    int pos2 = trans2.toUser(evt.getX(),0).x;
    //SyntenyLinkPolygon foundPoly = _findPolygon(evt.getPoint());
    DrawableLink foundLink = _findLink(evt.getPoint());

    if (statusbar != null) {
      statusbar.setPositionPane(new String((new Integer(pos1)).toString() + " " +
                                           (new Integer(pos2)).toString()));
      if (foundLink!=null) {
        statusbar.setFeaturePane(foundLink.getStatusBarString());
      } else {
        statusbar.setFeaturePane("");
      }
    }
  }

  public void mouseDragged(MouseEvent evt) {}


  /** Returns LinkSet of selected links */
  private LinkSet getSelectedLinks() {
    if (selectedLinks==null) {
      selectedLinks = new LinkSet();
    }
    return selectedLinks;
  }

  private void deselectLinks() {
    getSelectedLinks().clear();
  }

  /** This will cause a link to be drawn selected at paint time */
  private void addToSelection(Link link) {
    getSelectedLinks().addLink(link);
  }
  private boolean nothingSelected() {
    return getSelectedLinks().size() == 0;
  }

  public Link findFeaturePair(Point pos) {
    DrawableLink foundDrawableLink = _findLink(pos);
    if (foundDrawableLink != null) {
      return foundDrawableLink.getLink();
    } else {
      return null;
    }
  }

  private DrawableLink _findLink(Point pos) {
    //int x = pos.x; int y = pos.y;
    //Iterator sizeOrderedDrawables = getSortedDrawables().iterator();
    //while(sizeOrderedDrawables.hasNext()){
    for (int i=0; i<drawables.size(); i++) {
      DrawableLink link = getDrawableLink(i);//(DrawableLink)sizeOrderedDrawables.next();
      if (link.isVisible() && link.contains(pos)) {
        return link;
      }
    }
    
    return null;
  }

  private DrawableLink findDrawableLinkInRect(Rectangle bounds) {
    // Cant we just use sortedDrawables for this - doesnt seem like we need to 
    // have separate drawables list just for this method
    for (int i=0; i < drawables.size(); i++) {
      DrawableLink dl = getDrawableLink(i);

      if (dl.isVisible() && dl.intersects(bounds)) {
        return dl;
      }
    }
    return null;
  }

  /** ith DrawableLink from the drawables vector */
  private DrawableLink getDrawableLink(int i) { 
    return (DrawableLink)drawables.get(i); 
  }

//   /** Bring up the synteny fine view for the currently selected link */
//   public void showSyntenyFineView() {
//     if (selectedFeaturePairLink == null) return;
//     // (fp,species1,spc2) or (fp,cur1,cur2) or (fp,linkCS)
//     syntenyPanel.showFineViewForFeaturePair(selectedFeaturePairLink,links);
//   }



  public void setLinks(LinkSet links){
    this.links = links;
  }
  
  public boolean handleChainedRepaint(ChainedRepaintEvent event) {
    repaint();
    return true;
  }
  
//   public java.util.List getSortedDrawables() {
//     return sortedDrawables;
//   }
  
  TexturePaint getLowMatchPaint(FeatureProperty prop,boolean selected){
    return getPropertyScorePaint(prop,selected).getLowMatchPaint();
  }
  
  TexturePaint getMediumMatchPaint(FeatureProperty prop,boolean selected){
    return getPropertyScorePaint(prop,selected).getMediumMatchPaint();
  }
  
  TexturePaint getHighMatchPaint(FeatureProperty prop,boolean selected){
    return getPropertyScorePaint(prop,selected).getHighMatchPaint();
  }
  
  TexturePaint getVeryHighMatchPaint(FeatureProperty prop,boolean selected){
    return getPropertyScorePaint(prop,selected).getVeryHighMatchPaint();
  }
  
  /** The PropertyScorePaint for the FeatureProperty and selection state is 
      returned. If one doesnt exist yet it is created. There are separate 
      PropertyScorePaints for selected and deselected property since they are
      2 different colors. */
  private PropertyScorePaint getPropertyScorePaint(FeatureProperty prop,boolean select){
    Color color = getColor(prop,select);
    PropertyScorePaint scorePaint = (PropertyScorePaint)colorToScorePaint.get(color);
    if (scorePaint == null) {
      scorePaint = new PropertyScorePaint(color);
      colorToScorePaint.put(color,scorePaint);
    }
    return scorePaint;
  }
  
  public void setUseOpaqueLinks(boolean value){
    useOpaqueLinks = value;
  }
  
  public void setShadeByPercId(boolean value){
    shadeByPercId = value;
  }

  public boolean getUseOpaqueLinks(){
    return useOpaqueLinks;
  }
  
  public boolean getShadeByPercId(){
    return shadeByPercId;
  }

  public boolean handleScrollEvent(ScrollEvent event) {
    Object eventSource = event.getSource();
    boolean propagateToSZAP1 = true, propagateToSZAP2 = true;
    Controller controllerToOmit = null;

    if(eventSource instanceof ControlledObjectI) {
      controllerToOmit = ((ControlledObjectI)eventSource).getController();
    }else if(eventSource instanceof Controller){
      controllerToOmit = (Controller)eventSource;
    }

    if (controllerToOmit != null) {

      propagateToSZAP1 = !szap1.getController().equals(controllerToOmit);
      propagateToSZAP2 = !szap2.getController().equals(controllerToOmit);

    } else if (eventSource instanceof SyntenyLinkPanel) {

// In this case this must have been a resourced event which is bouncing
// up the SZAPs in a 3(or more)way synteny view
// We don't want to propagate back to the panels 

      propagateToSZAP1 = !((SyntenyLinkPanel)eventSource).isManagingSZAP(szap1);
      propagateToSZAP2 = !((SyntenyLinkPanel)eventSource).isManagingSZAP(szap2);
    }

    event.setSource(this);

    if (propagateToSZAP1) {
      szap1.getController().handleScrollEvent(event);
    }

    if (propagateToSZAP2) {
      szap2.getController().handleScrollEvent(event);
    }

    return true;
  }

  public void displayJustDnaLinks() {
    setDnaLinkVisibility(true);
    setProteinLinkVisibility(false);
  }
  public void displayJustProteinLinks() {
    setDnaLinkVisibility(false);
    setProteinLinkVisibility(true);
  }
  public void displayBothLinks() {
    setDnaLinkVisibility(true);
    setProteinLinkVisibility(true);
  }
  private void setDnaLinkVisibility(boolean vis) {
    Config.getPropertyScheme().getTierProperty(dnaLinkTypeString).setVisible(vis);
  }
  private void setProteinLinkVisibility(boolean vis) {
    Config.getPropertyScheme().getTierProperty(protLinkTypeString).setVisible(vis);
  }

  /** Puts both szaps in a single strand state. If szap1Forward is true, set forward
      strand visible in szap1, reverse hidden. Vice verse if false. Same for 
      szap2Forward with szap2(species2) 
  */
  public void setSingleStrandVisibility(boolean szap1Forward,boolean szap2Forward) {
    szap1.setForwardVisible(szap1Forward);
    szap1.setReverseVisible(!szap1Forward);
    szap2.setForwardVisible(szap2Forward);
    szap2.setReverseVisible(!szap2Forward);
  }
  public void showAllStrands() {
    szap1.setForwardVisible(true);
    szap1.setReverseVisible(true);
    szap2.setForwardVisible(true);
    szap2.setReverseVisible(true);
  }   
  public boolean isTopForwardVisible() { return szap1.isForwardStrandVisible(); }
  public boolean isTopReverseVisible() { return szap1.isReverseStrandVisible(); }
  public boolean isBottomForwardVisible() { return szap2.isForwardStrandVisible(); }
  public boolean isBottomReverseVisible() { return szap2.isReverseStrandVisible(); }
  
  /** Zoom to features of link and hide strands not linked */
  public void homeInOnSelectedLink() {
    if (getSelectedLinks().size()!=1) {
      return; // shouldnt happen
    }
    //FeaturePairI selectedFeaturePairLink = getSelectedLinks().getFeaturePair(0);
    Link link = getSelectedLinks().getLink(0);
    setSingleStrandVisibility(link.isForwardStrand1(),link.isForwardStrand2());
    // This assumes link is selected and nothing else
    szap1.zoomToSelection();
    szap2.zoomToSelection();
    repaint();
  }

  /** I wanted to put in one central method where the decision was made how 
      a color was turned into a selected color. Used both by texture paints(perc id)
      and regular painting. */
  Color getSelectColor(Color c) { return c.brighter(); }
  Color getColor(FeatureProperty prop, boolean selected) {
    // check for null property?
    Color c = prop.getColour();
    if (!selected) return c;
    else return getSelectColor(c);
  }


  /** LinkMouseListener 
   *  Left mouse click on link -> link selection, Right -> menu, Middle -> center */
  private class LinkMouseListener implements MouseListener {
    
    /** Right click - popup menu, Left click selection, Middle mouse - center */
    public void mouseClicked(MouseEvent evt) {
    
      DrawableLink selectedDrawLink = _findLink(evt.getPoint());

      // RIGHT MOUSE - popup menu
      if (MouseButtonEvent.isRightMouseClick(evt)) {
        // right click adds to selection if nothing else selected
        // does not deselect if not on a link (same as main apollo)
        if (nothingSelected() && selectedDrawLink!=null) {
          handleDrawableLinkSelection(selectedDrawLink);
        }
        showPopupMenu(evt);
      } 
      
      // MIDDLE MOUSE Center on position 
      else if (MouseButtonEvent.isMiddleMouseClick(evt)) { 
        Point pos1 = trans1.toUser(evt.getX(),0);
        Point pos2 = trans2.toUser(evt.getX(),0);
        setPanelCentres(pos1,pos2,autoStraighten);
      }
      
      // LEFT MOUSE - select
      else if (MouseButtonEvent.isLeftMouseClick(evt)) {
        // Deselect previously selected links - for now no shift multi
        deselectLinks();
        
        if (selectedDrawLink != null) {
          
          handleDrawableLinkSelection(selectedDrawLink);
          
          // center on feature
          boolean straighten = false; // what does straighten mean?
          Point pos1 = selectedDrawLink.getQueryMidUserPoint();
          Point pos2 = selectedDrawLink.getHitMidUserPoint();
          setPanelCentres(pos1,pos2,straighten);
        }
        
      } //end else if left mouse
      
      repaint();
      
    } //end mouseClicked()

    private void handleDrawableLinkSelection(DrawableLink drawableLink) {
      Link selectedLink = drawableLink.getLink();
      addToSelection(selectedLink);
      if (links.linkedByName()) {
        if (drawableLink.hasNames()) { // link method?
          // this fires a selection with the names
          handleLinkNameSelection(selectedLink);
        }
        else {
          // since we dont have names fire off event with ranges for seq highlight
          fireSelectionForSeqHighlight(drawableLink);// dont think this currently works
        }
      }
      // Link by feature
      else { 
        // Fire each species feat at its species/szap controller
        fireFeatureSelectionEvent(selectedLink.getSpeciesFeature1());
        fireFeatureSelectionEvent(selectedLink.getSpeciesFeature2());
      }
    }
    
    /** Fires FeatureSelectionEvent with feat at feats controller */
    private void fireFeatureSelectionEvent(SeqFeatureI feat) {
      SelectionManager s = getSelectionManagerForFeature(feat);
      s.select(feat,this);
//       FeatureSelectionEvent e = new FeatureSelectionEvent(this,feat);
//       Controller c = getSpeciesControllerForFeature(feat);
//       c.handleFeatureSelectionEvent(e);
    }
    
    /**
     * If the user clicks on a polygon (representing a link between two features)
     * then we will highlight the linked features (possibly genes) in each panel.
     This is for links linked by name fire NamedFeatureSelectionEvent
     **/
    private void handleLinkNameSelection(Link link) {
      String[] geneNameArray = new String[] { link.getName1(), link.getName2() };
      // source:this, null original event source
      fireNamedFeatureSelectionEvent(this, null, geneNameArray);
    }

    private void showPopupMenu(MouseEvent evt) {
      JPopupMenu popup = 
        new SyntenyLinkMenu(SyntenyLinkPanel.this,getSelectedLinks());
      popup.show((Component)evt.getSource(),evt.getX(),evt.getY());
    }
    public void mousePressed(MouseEvent evt) {}
    public void mouseReleased(MouseEvent evt) {}
    public void mouseEntered(MouseEvent evt) {}
    public void mouseExited(MouseEvent evt) {}
    
  } // end of LinkMouseListener inner class



  /** Inner class to deal with texture paints for perc id shading */
  private class PropertyScorePaint {
    private Color color;
    private TexturePaint veryHighMatchPaint; //80+% identity
    private TexturePaint highMatchPaint; //65-80% identity
    private TexturePaint mediumMatchPaint; //50-65% identity
    private TexturePaint lowMatchPaint;  //0-50% identity

    /** Just need the color of the prop */
    private PropertyScorePaint(Color color) {
      // should we construct the paints now or in the getters?
      // createPaints()?
      this.color = color;
    }

    private TexturePaint getVeryHighMatchPaint() {
      if (veryHighMatchPaint == null) veryHighMatchPaint = makeTexturePaint(1);
      return veryHighMatchPaint;
    }
    private TexturePaint getHighMatchPaint() {
      if (highMatchPaint == null) highMatchPaint = makeTexturePaint(3);
      return highMatchPaint;
    }
    private TexturePaint getMediumMatchPaint() {
      if (mediumMatchPaint == null) mediumMatchPaint = makeTexturePaint(5);
      return mediumMatchPaint;
    }
    private TexturePaint getLowMatchPaint() {
      if (lowMatchPaint == null) lowMatchPaint = makeTexturePaint(7);
      return lowMatchPaint;
    }

    private TexturePaint makeTexturePaint(int ovalSize) {
      BufferedImage image = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
      Graphics imageGraphics = image.createGraphics();
      imageGraphics.setColor(color);
      imageGraphics.fillRect(0, 0, 5, 5);
      imageGraphics.setColor(Color.lightGray);
      imageGraphics.fillOval(0, 0, ovalSize, ovalSize);
      Rectangle rectangle = new Rectangle(0,0,5,5);
      return new TexturePaint(image, rectangle);
    }
  } // end of PropertyScorePaint inner class
}




