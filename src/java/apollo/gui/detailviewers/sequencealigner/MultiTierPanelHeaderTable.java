package apollo.gui.detailviewers.sequencealigner;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;

import apollo.gui.detailviewers.sequencealigner.SeqFeatureTableModel.ColumnTypes;
import apollo.gui.detailviewers.sequencealigner.TierI.Level;

import apollo.datamodel.SeqFeatureI;
/**
 * Need to figure out how to integrate mouse selection.
 * 
 * Maybe make a mapping object that will transform pixel positions to tier positions
 * there are a lot of places where pixel position is used... how will this
 * change affect that?
 * 
 *
 */


public class MultiTierPanelHeaderTable extends JTable {
  private MultiTierPanel multiTierPanel;
  private JViewport view;
  SeqFeatureTableModel model;
  
  /** The view should have a MultiTierPanel */
  public MultiTierPanelHeaderTable(JViewport view, List<ColumnTypes> cols) {
    this.view = view;
    this.multiTierPanel = (MultiTierPanel) view.getView();
    this.model = new SeqFeatureTableModel(multiTierPanel.numTiers(), cols);
    for (int i = 0; i < multiTierPanel.numTiers(); i++) {
      model.add(null);
    }
    this.setModel(model);
    setFont(apollo.config.Config.getExonDetailEditorSequenceFont());
    setRowHeight(multiTierPanel.getBaseHeight());
    setForeground(Color.black);
    setBackground(Color.white);
    setOpaque(true);
    setBorder(null);

    //this.rowHeight = panel.getModelTierPanel().getBaseHeight();
    //this.rowMargin = 0;
    update(0);
  }
  
  public void reformat() {
    model.clear();
    for (int i = 0; i < multiTierPanel.numTiers(); i++) {
      model.add(null);
    }
  }
  
  
  /** 
   * Figures out which rows have visible content and updates
   * their display if need be. This sets the visibility of rows
   * without anything to display to be invisible (this should probably be done
   * somewhere else)
   * 
   * displayPosition is in pixel position 
   */
  public void update(int position) {
    List<AbstractTierPanel> panels = multiTierPanel.getPanels();
    int numInvisible = 0;

    for (int i = 0; i < panels.size(); i++) {
      
      SeqFeatureI feature = panels.get(i).featureAt(position, Level.TOP);
      if (feature == null) {
          feature = panels.get(i).getNextFeature(position, Level.TOP);
      }
      
      boolean isVisible = false;
      
      if (feature != null) {
        isVisible = isVisible(feature);
      }
      panels.get(i).setVisible(true);
      
      if (numInvisible > 0) {
        this.model.set(i, null);
      }
      
      if (feature != model.get(i-numInvisible) && isVisible) {
        this.model.set(i-numInvisible, feature);
      } else if (!isVisible) {
        this.model.set(i-numInvisible, null);
        panels.get(i).setVisible(false); // will this have everything repaint?
        numInvisible++;
      }
    }
    
    if (numInvisible == panels.size()) {
      panels.get(0).setVisible(true);
    }
  }
  

  /**
   * Calculates the tier number for a given point
   * 
   * @param p the pixel coordinates of a point on the annotation panel
   * @return the tier that the point is on
   */
  public int calculateTierNumber(Point p) {
    int nonAdjustedTier = multiTierPanel.tierForPixel(p);
    int invisibleTiers = 0;
    
    for (int i = 0; i <= nonAdjustedTier + invisibleTiers && i < multiTierPanel.numTiers(); i++) {
      AbstractTierPanel tierPanel = multiTierPanel.getPanel(i);
      if (!tierPanel.isVisible()) {
        invisibleTiers++;
      }
    }
    
    int tierNumber = nonAdjustedTier + invisibleTiers;
    if (tierNumber >= multiTierPanel.numTiers()) {
      // The mouse is not over an actual tier
      tierNumber = 0;
    }
    
    return tierNumber;
  }
 
  private int viewStart() {
    Rectangle rect = view.getViewRect();
    Point lowPixel = new Point(rect.x, 0);
    return multiTierPanel.getPositionForPixel(lowPixel);
  }
  
  private int viewEnd() {
    Rectangle rect = view.getViewRect();
    Point highPixel = new Point(rect.x + rect.width, 0);
    return multiTierPanel.getPositionForPixel(highPixel);
  }
  
  public boolean isVisible(SeqFeatureI feature) {
    
    int viewStart = viewStart();
    int viewEnd = viewEnd();
    
    int s = feature.getStart();
    int e = feature.getEnd();
    
    // The start of the feature in PixelPosition
    int featureStart = Math.min(
      multiTierPanel.tierPositionToPixelPosition(
          multiTierPanel.basePairToTierPosition(feature.getStart())),
      multiTierPanel.tierPositionToPixelPosition(
          multiTierPanel.basePairToTierPosition(feature.getEnd())) );
    
    // The end of the feature in PixelPosition
    int featureEnd = Math.max(
        multiTierPanel.tierPositionToPixelPosition(
            multiTierPanel.basePairToTierPosition(feature.getStart())),
        multiTierPanel.tierPositionToPixelPosition(
            multiTierPanel.basePairToTierPosition(feature.getEnd())));
    
    boolean result = !((featureStart < viewStart && featureEnd < viewStart) ||
               (featureStart > viewEnd && featureEnd > viewEnd));
    
    return result;
  }
  
}
