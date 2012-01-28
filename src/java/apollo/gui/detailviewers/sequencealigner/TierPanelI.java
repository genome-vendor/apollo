package apollo.gui.detailviewers.sequencealigner;

import java.awt.Graphics;
import java.awt.Point;

import javax.swing.Scrollable;

import apollo.datamodel.SeqFeatureI;
import apollo.editor.AnnotationChangeListener;
import apollo.gui.BaseScrollable;
import apollo.gui.detailviewers.sequencealigner.TierI.Level;
import apollo.gui.detailviewers.sequencealigner.renderers.BaseRendererI;

public interface TierPanelI extends Scrollable, BaseScrollable, 
  AnnotationChangeListener  {

  public SeqFeatureI featureAt(int p, Level level);

  /** pixel size */
  public int getBaseHeight();

  /** pixel size */
  public int getBaseWidth();

  public int getHigh(SeqFeatureI f);

  public int getLow(SeqFeatureI f);

  public SeqFeatureI getNextFeature(int p, Level l);

  public Orientation getOrientation();

  public Point getPixelForPosition(int p);

  public int getPositionForPixel(Point p);

  public SeqFeatureI getPrevFeature(int p, Level level);

  public BaseRendererI getRenderer();
  
  public TierI getTier();
  
  public void paint(Graphics g);
  
  public int pixelPositionToTierPosition(int p);
  
  public void reformat(boolean isRecursive);
  
  public void setOrientation(Orientation orientation);
  
  public void setRenderer(BaseRendererI r);
  
  int tierPositionToPixelPosition(int p);

}
