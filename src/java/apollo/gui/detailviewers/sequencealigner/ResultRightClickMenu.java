package apollo.gui.detailviewers.sequencealigner;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.datamodel.FeaturePair;
import apollo.datamodel.SeqFeatureI;
import apollo.gui.BaseScrollable;
import apollo.gui.detailviewers.sequencealigner.actions.AddFilterAction;
import apollo.gui.detailviewers.sequencealigner.actions.AddResultFilterAction;
import apollo.gui.detailviewers.sequencealigner.filters.Filter;
import apollo.gui.detailviewers.sequencealigner.filters.FilterFactory;
import apollo.gui.detailviewers.sequencealigner.menus.InsertionDialog;
import apollo.gui.synteny.GuiCurationState;

/**
 * This is the popup menu that gets displayed when you right click on an 
 * result.
 */
public class ResultRightClickMenu extends JPopupMenu {

  /** This should be the Result window */
  private MultiTierPanel panel;

  /** The curation state */
  private GuiCurationState curationState;

  // TODO fix how this is done! 
  // It would be nice to remove this dependency if possible
  private MultiSequenceAlignerPanel multiSequenceAlignerPanel;
  
  private String insertion;
  private AbstractAction viewInsertion;
  
  private AddResultFilterAction filterName;
  private AddResultFilterAction filterType;
  private AddResultFilterAction filterFrame;
  
  

  
  /**
   * Constructor
   * 
   * @param curationState
   * @param panel should be the result panel
   */
  public ResultRightClickMenu(GuiCurationState curationState,
      MultiTierPanel panel) {
    super();
    this.multiSequenceAlignerPanel = null;
    this.curationState = curationState;
    this.panel = panel;
    this.insertion = "";
    init();
  }
  
  private void init() {
    removeAll();
    createActions();
    
    add(viewInsertion);
    addSeparator();
    add(filterName);
    add(filterType);
    add(filterFrame);
  }
  
  private void createActions() {
    viewInsertion = new AbstractAction("Insertion: " + insertion) {
      public void actionPerformed(ActionEvent arg0) {
        InsertionDialog id = new InsertionDialog(insertion);
        id.show();
      }
    };
    
    filterName = new AddResultFilterAction(multiSequenceAlignerPanel);
    filterName.putValue(filterName.NAME, "Filter Name");
    
    filterType = new AddResultFilterAction(multiSequenceAlignerPanel);
    filterType.putValue(filterName.NAME, "Filter Type");
    
    filterFrame = new AddResultFilterAction(multiSequenceAlignerPanel);
    filterFrame.putValue(filterName.NAME, "Filter Frame");
  }
  
  /**
   * Gets the MultiSequenceAlignerPanel object for this menu
   * 
   * @return
   */
  public MultiSequenceAlignerPanel getMultiSequenceAlignerPanel() {
    return this.multiSequenceAlignerPanel;
  }

  /**
   * Sets the MultiSequenceAlignerPanel object for this menu
   * 
   * @param MultiSequenceAlignerPanel this is the object that will be affected 
   * this menu
   */
  public void setMultiSequenceAlignerPanel(MultiSequenceAlignerPanel multiSequenceAlignerPanel) {
    this.multiSequenceAlignerPanel = multiSequenceAlignerPanel;
  }
  
  public void setInsertion(String i) {
    insertion = i;
    init();
  }
  
  
  /**
   * Updates menu to reflect the options available at the given position and
   * tier.
   * 
   * @param pos - tier position
   * @param tier
   */
  public void formatRightClickMenu(int pos, int t) {
    
    SeqFeatureI feature = null;
    TierI tier = panel.getTier(t);
    if (tier.featureExsitsAt(pos, TierI.Level.BOTTOM)) {
      feature = tier.featureAt(pos, TierI.Level.BOTTOM);
    }
    
    this.filterName.setEnabled(false);
    this.filterFrame.setEnabled(false);
    this.filterType.setEnabled(false);
    
    if (feature instanceof FeaturePair) {
      FeaturePair fp = (FeaturePair) feature;
      
      FeatureProperty prop = Config.getPropertyScheme()
      .getFeatureProperty(feature.getTopLevelType());
      
      Filter filter = null;
      
      String name = fp.getName();
      filter = FilterFactory.makeInverse(
          FilterFactory.makeFilterName(name));
      this.filterName.setFilter(filter);
      this.filterName.setEnabled(true);
      filterName.putValue(filterName.NAME, "Filter Name: " + name);
      
      
      if (prop != null) {
        String type = prop.getDisplayType();
        filter = FilterFactory.makeInverse(
            FilterFactory.makeFilterDisplayType(type.toUpperCase()));
        this.filterType.setFilter(filter);
        this.filterType.setEnabled(true);
        filterType.putValue(filterType.NAME, "Filter Type: " + type.toUpperCase());
      }
    
      
      ReadingFrame frame = ReadingFrame.valueOf(fp.getFrame());
      filter = FilterFactory.makeInverse(
          FilterFactory.makeFilter(frame));
      this.filterFrame.setFilter(filter);
      this.filterFrame.setEnabled(true);
      filterFrame.putValue(filterFrame.NAME, "Filter Frame: " + frame);
      
      //Strand strand = Strand.valueOf(fp.getStrand());
    }
    
    if ("".equals(insertion)) {
      viewInsertion.setEnabled(false);
    }
    
  }
  
}
