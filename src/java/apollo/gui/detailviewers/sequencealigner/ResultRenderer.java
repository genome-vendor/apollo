package apollo.gui.detailviewers.sequencealigner;

import java.awt.Color;
import java.awt.Component;
import java.util.Map;
import java.util.TreeMap;

import org.bdgp.util.DNAUtils;

import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.FeaturePair;
import apollo.datamodel.SequenceI;

public class ResultRenderer extends DefaultBaseRenderer {

  private static final int STATE_MISS = 1;
  private static final int STATE_MATCH = 2;
  private static final int STATE_OUT = 3;

  private SeqAlignPanel baseEditor;
  private Map<Integer, Color> colorMap;
  private Integer state;

  public ResultRenderer(SeqAlignPanel baseEditor, int width, int height) {
    super(width, height);
    this.baseEditor = baseEditor;
    this.state = STATE_OUT;

    this.colorMap = new TreeMap();
    this.colorMap.put(STATE_MISS, Color.orange);
    this.colorMap.put(STATE_MATCH, Color.magenta);
    this.colorMap.put(STATE_OUT, null);

  }

  public Color getBackgroundBoxColor() {
    return colorMap.get(state);
  }

  public Component getBaseRendererComponent(char base, int pos, int tier,
      SequenceI seq) {

    SeqFeatureI currentFeature = baseEditor.getFeatureAtPosition(pos, tier);
    double[] range = baseEditor.getRangeAtPosition(tier, pos);

    if (currentFeature != null && pos >= range[0] && pos <= range[1]) {
      this.state = STATE_MATCH;
    } else {
      this.state = STATE_OUT;
    }

    if (this.state == STATE_MATCH && currentFeature instanceof FeaturePair) {
      FeaturePair fp = (FeaturePair) currentFeature;

      if (fp.getHitFeature().getExplicitAlignment() != null) {
        int genomicPosition = baseEditor.posToBasePair(pos);
        int hitIndex = fp.getHitIndex(genomicPosition);
        char hitBase = fp.getHitFeature().getExplicitAlignment().charAt(
            hitIndex);

        if (hitBase != base) {
          this.state = STATE_MISS;
          base = hitBase;
        }

      } else { // what else could this be?
        boolean t = true;
      }
    }

    init(base, pos, tier, seq);
    return this;
  }

  public boolean canRender(SeqFeatureI feature) {
    return !feature.isAnnot();
  }

}
