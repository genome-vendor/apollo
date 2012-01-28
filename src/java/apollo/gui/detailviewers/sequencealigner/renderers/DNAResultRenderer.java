package apollo.gui.detailviewers.sequencealigner.renderers;

import java.awt.Color;
import java.awt.Component;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bdgp.util.DNAUtils;

import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.datamodel.FeaturePair;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;
import apollo.gui.detailviewers.sequencealigner.Orientation;
import apollo.gui.detailviewers.sequencealigner.ReadingFrame;
import apollo.gui.detailviewers.sequencealigner.SequenceType;
import apollo.gui.detailviewers.sequencealigner.Strand;
import apollo.gui.detailviewers.sequencealigner.TierI;
import apollo.gui.detailviewers.sequencealigner.TierI.Level;

public class DNAResultRenderer extends TestBaseRenderer {

  private static final int STATE_MISS = 1;
  private static final int STATE_MATCH = 2;
  private static final int STATE_OUT = 3;
  private static final int STATE_X = 4;
  private static final int STATE_NO_ALIGNMENT = 5;

  private static final Map<Integer, Color> stateColorMap = new HashMap<Integer, Color>();
  private static final Map<String, Color> baseColorMap = new HashMap<String, Color>();

  static {
    stateColorMap.put(STATE_MISS, Color.black);
    stateColorMap.put(STATE_MATCH, Color.magenta);
    stateColorMap.put(STATE_OUT, Color.black);
    stateColorMap.put(STATE_X, Color.white);
    stateColorMap.put(STATE_NO_ALIGNMENT, Color.blue);
  };
  
  // Colors based on: http://www.jalview.org/help.html zappos
  static {
    baseColorMap.put("I", new Color(250, 128, 114));
    baseColorMap.put("L", new Color(250, 128, 114));
    baseColorMap.put("V", new Color(250, 128, 114));
    baseColorMap.put("A", new Color(250, 128, 114));
    baseColorMap.put("M", new Color(250, 128, 114));
    
    baseColorMap.put("F", new Color(255, 165, 0));
    baseColorMap.put("W", new Color(255, 165, 0));
    baseColorMap.put("Y", new Color(255, 165, 0));
    
    baseColorMap.put("K", new Color(131, 111, 255));
    baseColorMap.put("R", new Color(131, 111, 255));
    baseColorMap.put("H", new Color(131, 111, 255));
    
    baseColorMap.put("D", new Color(192, 0, 0));
    baseColorMap.put("E", new Color(192, 0, 0));
    
    baseColorMap.put("S", new Color(9, 249, 17));
    baseColorMap.put("T", new Color(9, 249, 17));
    baseColorMap.put("N", new Color(9, 249, 17));
    baseColorMap.put("Q", new Color(9, 249, 17));
    
    baseColorMap.put("P", new Color(205, 0, 205));
    baseColorMap.put("G", new Color(205, 0, 205));
    
    baseColorMap.put("C", new Color(255, 255, 0));
    
    baseColorMap.put("-", new Color(205, 133, 0));
  }
  
  private static Color insertionColor = new Color(255, 255, 0);

  private Integer state;
  private int outlineType;
  private SeqFeatureI feature;
  private boolean easyRead;

  public DNAResultRenderer() {
    super();
    this.state = STATE_OUT;
    this.outlineType = NO_OUTLINE;
    this.easyRead = false;
  }
  
  public DNAResultRenderer(boolean easyRead) {
    super();
    this.state = STATE_OUT;
    this.outlineType = NO_OUTLINE;
    this.easyRead = easyRead;
  }

  public Color getBackgroundBoxColor() {
    Color bgColor = stateColorMap.get(state);
    
    if (state.equals(STATE_MATCH) && !easyRead) {
      FeatureProperty fp =
        Config.getPropertyScheme().getFeatureProperty(feature.getTopLevelType());
        bgColor = fp.getColour();
    }
    
    return bgColor;
  }

  public int getOutlineType() {
    return outlineType;
  }

  public Color getOutlineColor() {
    return insertionColor;
  }
  
  public Color getTextColor() {
    Color txColor = Color.white;
    if (state.equals(STATE_MISS)) {
      txColor = baseColorMap.get(String.valueOf(getCharacter()));
      if (txColor == null) { txColor = Color.pink; }
    }
    return txColor;
  }
  
  /* position is the tier position */
  public Component getBaseComponent(int position, TierI tier, Orientation o) {
    
    this.outlineType = NO_OUTLINE;
    char base = tier.charAt(position);
    int bp = tier.getBasePair(position);

    feature = null;
    if (tier.featureExsitsAt(position, TierI.Level.BOTTOM)) {
      feature = tier.featureAt(position, TierI.Level.BOTTOM);
      this.state = STATE_MATCH;
    } else {
      this.state = STATE_OUT;
    }
    
    if (this.state == STATE_MATCH && feature instanceof FeaturePair) {
      FeaturePair fp = (FeaturePair) feature;
      int basePair = tier.getBasePair(position);
      
      if (tier.getType() == SequenceType.AA) {
        ReadingFrame exonFrame = ReadingFrame.valueOf(feature.getFrame());
        ReadingFrame tierFrame = tier.getReadingFrame();
        int offset = 0;
        
        if (tierFrame != exonFrame) {
          if (tierFrame == ReadingFrame.THREE) {
            offset = exonFrame == ReadingFrame.ONE ? -2 : -1;
          } else if (tierFrame == ReadingFrame.TWO) {
            offset = exonFrame == ReadingFrame.ONE ? -1 : 1;
          } else if (tierFrame == ReadingFrame.ONE) {
            offset = exonFrame == ReadingFrame.TWO ? 1 : 2; 
          }
        }
        
        basePair += (offset*feature.getStrand());
      }
      
      int hitIndex = fp.getHitIndex(basePair,
                                    tier.getReference().getResidueType());
      int hi = fp.getHitIndex(bp,
          tier.getReference().getResidueType());
      
      // Check for insertions
      if (hitIndex >= 0 && fp.getQueryFeature().getExplicitAlignment() != null) {
        
        hitIndex += fp.insertionsBefore(hitIndex, fp.getQueryFeature().getExplicitAlignment());
        
        // do i need to modify this based on the tier orientation?
        if (hitIndex-1 > 0 && hitIndex-1 < fp.getQueryFeature().getExplicitAlignment().length()
            && '-' == fp.getQueryFeature().getExplicitAlignment().charAt(hitIndex-1)) {
          if (o == Orientation.FIVE_TO_THREE) {
            this.outlineType = this.LEFT_OUTLINE;
          } else {
            this.outlineType = this.RIGHT_OUTLINE;
          }
        }
        
        if (hitIndex+1 < fp.getQueryFeature().getExplicitAlignment().length() 
            && '-' == fp.getQueryFeature().getExplicitAlignment().charAt(hitIndex+1)) {
          if (o == Orientation.FIVE_TO_THREE) {
            this.outlineType = this.RIGHT_OUTLINE;
          } else {
            this.outlineType = this.LEFT_OUTLINE;
          }
        }
      }
      
      if (fp.getHitFeature().getExplicitAlignment() != null) {
        
        // get the basepair position for the position in the tier
        char hitBase = base;
        
        if (hitIndex > -1 && 
            hitIndex < fp.getHitFeature().getExplicitAlignment().length()) {
          hitBase = fp.getHitFeature().getExplicitAlignment().charAt(hitIndex);
        } else {
          this.state = STATE_OUT;
        }
        if (hitBase != base) {
          this.state = STATE_MISS;
          base = hitBase;
        }
      } else {
        this.state = STATE_NO_ALIGNMENT;
      }

    } else { // what else could this be?
      if (state.equals(STATE_MATCH)) {
        this.state = STATE_OUT;
      }
    }

    if (this.state.equals(this.STATE_OUT)) {
      base = '\0';
    }

    init(base);
    return this;
  }
  

}
