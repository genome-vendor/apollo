package apollo.gui.detailviewers.sequencealigner;

import apollo.gui.detailviewers.sequencealigner.renderers.BaseRenderer;
import apollo.gui.detailviewers.sequencealigner.renderers.BaseRendererI;

public abstract class PanelFactory {
  
  private Orientation orientation;
  private TierFactory tierFactory;
  
  public PanelFactory(Orientation o, TierFactory tf) {
    this.orientation = o;
    this.tierFactory = tf;
  }
  
  public AbstractTierPanel makePanel() {
      AbstractTierPanel tp = 
        new TierPanel(tierFactory.makeTier(),
                      orientation,
                      getRenderer());
      
      return tp;
  }
  
  public AbstractTierPanel makePanel(TierI t, Orientation o, BaseRendererI br) {
    return new TierPanel(t, o, br);
  }
  
  public Orientation getOrientation() {
    return this.orientation;
  }
  
  public void setOrientation(Orientation o) {
    this.orientation = o;
  }
  
  public Strand getStrand() {
    return this.tierFactory.getStrand();
  }
  
  public void setStrand(Strand s) {
    this.tierFactory.setStrand(s);
  }
  
  public ReadingFrame getReadingFrame() {
    return this.tierFactory.getReadingFrame();
  }
  
  public void setReadingFrame(ReadingFrame rf) {
    this.tierFactory.setReadingFrame(rf);
  }
  
  public abstract BaseRendererI getRenderer();
  
}
