package apollo.gui.drawable;

import java.util.Vector;

import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.FeatureSetI;
import apollo.editor.FeatureChangeEvent;
import apollo.datamodel.EvidenceFinder;
/**
 * An interface defining methods necessary for a drawable object
 Merge this with Drawable? Drawables optionally have children?
 It would make descending the heirarchy easier.
 */
public interface DrawableSetI extends Drawable {

  public FeatureSetI getFeatureSet();
  public int size();
  public Drawable addFeatureDrawable(SeqFeatureI sf);
  public void addDrawable(Drawable dsf);
  public Vector getDrawables();
  public Drawable getDrawableAt(int i);
  public void deleteDrawable(Drawable sf);
  public void repairFeatureSet(FeatureChangeEvent ce);

}
