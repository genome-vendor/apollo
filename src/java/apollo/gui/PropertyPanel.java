package apollo.gui;

import apollo.datamodel.*;
import apollo.config.FeatureProperty;
import apollo.config.TierProperty;
import apollo.gui.genomemap.ApolloPanel;
import apollo.gui.event.*;
import apollo.gui.menus.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.Vector;

/**
 * A panel which renders a single type row in the TypePanel.
 */
public class PropertyPanel extends JPanel
      implements ActionListener,
  MouseInputListener {
  JCheckBox    visible;
  JCheckBox    expanded;
  JCheckBox    sorted;
  JCheckBox    labeled;
  JPanel       jp;
  ApolloPanel  ap;
  TierProperty tierProperty;

  boolean      beginFlash;
  javax.swing.Timer flashTimer = null;

  public PropertyPanel(ApolloPanel ap, TierProperty tierProperty) {
    this.ap       = ap;
    this.tierProperty       = tierProperty;

    jbInit();
  }

  public void jbInit() {

    JLabel label = new JLabel(tierProperty.getLabel(), SwingConstants.CENTER);
    label.setForeground (Color.black);

    // Checkboxes
    visible  = new JCheckBox("Show");
    visible.setHorizontalTextPosition(JLabel.LEFT);
    visible.setHorizontalAlignment(JLabel.RIGHT);
    expanded = new JCheckBox("Expand");
    expanded.setHorizontalTextPosition(JLabel.LEFT);
    expanded.setHorizontalAlignment(JLabel.RIGHT);
    sorted   = new JCheckBox("Sort");
    labeled  = new JCheckBox("Label");
    jp = new JPanel();

    Color colour = tierProperty.getColour();

    setColours (this, colour, Color.black);
    setColours (visible, colour, Color.black);
    setColours (expanded, colour, Color.black);
    setColours (sorted, colour, Color.black);
    setColours (labeled, colour, Color.black);
    setColours (jp, colour, Color.black);

    visible .setSelected(tierProperty.isVisible());
    expanded.setSelected(tierProperty.isExpanded());
    sorted.setSelected(tierProperty.isSorted());
    labeled.setSelected(tierProperty.isLabeled());

    // Put the checkboxes in a panel
    //jp.setLayout(new GridLayout(2,1));
    jp.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.NORTHEAST;
    gbc.gridwidth = GridBagConstraints.REMAINDER;

    jp.add(visible,gbc);
    gbc.anchor = GridBagConstraints.SOUTHEAST;

    jp.add(expanded,gbc);
    JPanel jp2 = new JPanel();
    jp2.setLayout(new GridLayout(2,1));
    jp2.add(sorted);
    jp2.add(labeled);

    setLayout(new BorderLayout());

    Font smallFont = new Font("Dialog", 0, 10);

    setFont(smallFont);
    visible.setFont(smallFont);
    expanded.setFont(smallFont);
    sorted.setFont(smallFont);
    labeled.setFont(smallFont);

    add("West",jp2);
    add("Center", label);
    add("East",jp);

    addMouseListener(this);
    label.addMouseListener(this);
    visible .addActionListener(this);
    expanded.addActionListener(this);
    sorted.addActionListener(this);
    labeled.addActionListener(this);
  }

  public TierProperty getProperty() {
    return this.tierProperty;
  }

  public void flash() {
    Color colour = tierProperty.getColour();

    // System.out.println("Flashing " + tierProperty.getColour());
    setBackground(colour.darker());

    beginFlash = true;
  }

  public void paintComponent(Graphics g) {
    if (beginFlash) {
      flashTimer    = new javax.swing.Timer(750,this);
      flashTimer.start();
      beginFlash = false;
    }
    super.paintComponent(g);
  }

  public void updateValues() {
    setColours (this, tierProperty.getColour(), Color.black);
    setColours (visible, tierProperty.getColour(), Color.black);
    setColours (expanded, tierProperty.getColour(), Color.black);
    setColours (sorted, tierProperty.getColour(), Color.black);
    setColours (labeled, tierProperty.getColour(), Color.black);
    setColours (jp, tierProperty.getColour(), Color.black);
    visible .setSelected(tierProperty.isVisible());
    expanded.setSelected(tierProperty.isExpanded());
    sorted.setSelected(tierProperty.isSorted());
    labeled.setSelected(tierProperty.isLabeled());
  }

  public void actionPerformed(ActionEvent evt) {
    if (evt.getSource() == visible) {
      tierProperty.setVisible(visible.isSelected());
    } else if (evt.getSource() == expanded) {
      tierProperty.setExpanded(expanded.isSelected());
    } else if (evt.getSource() == sorted) {
      tierProperty.setSorted(sorted.isSelected());
    } else if (evt.getSource() == labeled) {
      // If user asks to turn on labeling, then automatically expand tier
      // (or else it will look ugly).
      if (labeled.isSelected() == true) {
        tierProperty.setExpanded(true);
        expanded.setSelected(true);
        tierProperty.setLabeled(true);
      }
      tierProperty.setLabeled(labeled.isSelected());
      // When labels are turned on, things scroll weirdly, so set scrollbars back to starting positions.
      if (tierProperty.isLabeled()) {
	if (ap == null) {
	  return;
	}
	else
	  ap.putVerticalScrollbarsAtStart();
      }
    } else if (evt.getSource() == flashTimer) {
      Color colour = tierProperty.getColour();
      setColours (this, colour, Color.black);
      //System.out.println("Timer action");
      flashTimer.stop();
      flashTimer = null;
    }
  }

  class MaxRowListener implements ChangeListener {
    public void stateChanged(ChangeEvent evt) {
      if (evt.getSource() instanceof DefaultBoundedRangeModel) {
        tierProperty.setMaxRow(((DefaultBoundedRangeModel)evt.getSource()).getValue());
      }
    }
  }

  /**
   * MouseInputListener routines
   */
  public void mouseEntered(MouseEvent evt) {
    requestFocusInWindow();
  }

  public void mouseExited(MouseEvent evt) {}

  public void mouseClicked(MouseEvent evt) {}

  // Right mouse and middle mouse both bring up the subtypes popup menu.
  // If you use right mouse, then selecting a subtype will bring up the color chooser.
  // If you use middle mouse, then selecting a subtype will bring up the threshold slider.

  public void mousePressed(MouseEvent evt) {
    // Middle click
    if (MouseButtonEvent.isMiddleMouseClick(evt)) {
      JPopupMenu popup = new ThresholdMenu(this,
                                           tierProperty,
                                           new Point(evt.getX(),
                                                     evt.getY()));
      popup.show((Component)evt.getSource(),evt.getX(),evt.getY());
    }
    // Right click
    else if (MouseButtonEvent.isRightMouseClick(evt)) {
      JPopupMenu popup = new PropertyMenu(this,
                                          tierProperty,
                                          new Point(evt.getX(),
                                                    evt.getY()));
      popup.show((Component)evt.getSource(),evt.getX(),evt.getY());
    }
    // Left click ("max rows" slider comes up only if "Sort" box is ticked
    else if (MouseButtonEvent.isLeftMouseClick(evt)) {
      if (tierProperty.isSorted()) {
        DefaultBoundedRangeModel model = new DefaultBoundedRangeModel(tierProperty.getMaxRow(),0,0,100);
        new SliderWindow("Set max rows (" + tierProperty.getLabel() + " tier)","Max rows in tier",model,getLocationOnScreen());
        model.addChangeListener(new MaxRowListener());
      }
    }
  }



  public void mouseReleased(MouseEvent evt) {}

  public void mouseMoved(MouseEvent evt) {}

  public void mouseDragged (MouseEvent evt) {}

  private void setColours (JComponent comp, Color bg, Color fg) {
    comp.setBackground(bg);
    if (bg == Color.black) {
      setForeground(Color.white);
    } else {
      comp.setForeground (fg);
    }
  }

  public static void main(String[] args) {
    JFrame          f   = new JFrame("TypePanel test");

    String analysis_type = "Genscan";
    Vector types = new Vector();
    types.addElement (analysis_type);
    TierProperty tierProperty = new TierProperty(analysis_type, true, false);
    FeatureProperty fp = new FeatureProperty(tierProperty, analysis_type, types);
    PropertyPanel   tp1 = new PropertyPanel(null,tierProperty);

    analysis_type = "RepeatMasker";
    types = new Vector();
    types.addElement (analysis_type);
    tierProperty = new TierProperty(analysis_type, true, false);
    fp = new FeatureProperty(tierProperty, analysis_type, types);

    PropertyPanel   tp2 = new PropertyPanel(null,tierProperty);

    f.getContentPane().setLayout(new GridLayout(1,2));
    f.getContentPane().add(tp1);
    f.getContentPane().add(tp2);
    f.setSize(300,100);
    f.show();
  }
}



