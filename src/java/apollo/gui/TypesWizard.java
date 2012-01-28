package apollo.gui;

import javax.swing.*;
import javax.swing.event.*;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.awt.*;
import java.awt.event.*;

import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;

import apollo.config.Config;
import apollo.config.PropSchemeChangeEvent;
import apollo.config.PropSchemeChangeListener;
import apollo.config.Style;
import apollo.config.TierProperty;
import apollo.config.FeatureProperty;
import apollo.config.PropertyScheme;
import apollo.gui.synteny.CurationManager;
import apollo.util.IOUtil;
import apollo.util.CollectionUtil;

/** GUI for handling the configuration of Apollo tiers/types.  Note that it only
 *  handles a subset of the many options for tiers/types.  This is done to not
 *  overwhelm the average user (power users will probably be editing the tiers
 *  files by hand anyway).  Suzi and I sat down and looked over the many options
 *  and came up with these as being the most useful ones.
 * 
 * @author elee
 *
 */

public class TypesWizard extends JPanel
{

  protected final static Logger logger = LogManager.getLogger(Style.class);
  
  private JComboBox tierComboBox;
  private JCheckBox visibleCheckBox;
  private JCheckBox labeledCheckBox;
  private JCheckBox expandedCheckBox;
  private JComboBox typeComboBox;
  private JComboBox shapeComboBox;
  private JButton colorButton;
  private JPanel colorButtonPanel;
  private JCheckBox scaleCheckBox;
  private JTextField minScoreField;
  private JTextField maxScoreField;
  private JTextField urlField;
  private JTextArea dataTypeArea;
  private JButton newTierButton;
  private JButton newTypeButton;
  private JCheckBox matchUtrColorToBackgroundCheckBox;
  private JLabel matchUtrColorToBackgroundLabel;
  private JSeparator separator;
  
  private Map<String, FeatureProperty> origFeatureProperties;
  
  // array containing the labels for the different drawable shapes
  private final static String []shapeLabels = { "DrawableGeneFeatureSet", "DrawableResultFeatureSet", "DoubleHeadedArrow", "Triangle",
    "Zigzag", "ThinRectangle" };
  // array containing the images for the different drawable shapes
  private final static Icon []shapeIcons;
  static
  {
    shapeIcons = new Icon[shapeLabels.length];
    String dir = "data/images/";
    for (int i = 0; i < shapeLabels.length; ++i) {
      String icon = dir + shapeLabels[i] + ".gif";
      icon = IOUtil.findFile(icon);
      if (icon == null || Config.isJavaWebStartApplication()) {
        icon = Config.getRootDir() + "/" + dir + shapeLabels[i] + ".gif";
        Config.ensureExists(new File(icon), shapeLabels[i] + ".gif", true);
      }
      shapeIcons[i] = new ImageIcon(icon);
    }
  }
  
  /** Constructor.
   *
   */
  public TypesWizard()
  {
    init();
  }
  
  /** Initialize and place the components.
  *
  */
  private void init()
  {
    Controller.getMasterController().addListener(new PropSchemeChangeListener() {
      public boolean handlePropSchemeChangeEvent(PropSchemeChangeEvent e)
      {
        setTierValues();
        setTypeValues();
        return true;
      }
    });
    
    origFeatureProperties = new HashMap<String, FeatureProperty>();

    tierComboBox = new JComboBox();
    DefaultComboBoxModel tiers = new DefaultComboBoxModel(Config.getPropertyScheme().getAllTiers());
    tierComboBox.setModel(tiers);
    tierComboBox.addActionListener(new TierComboBoxListener());
    newTierButton = new JButton("New");
    newTierButton.addActionListener(new NewTierButtonListener());
    JPanel tierPanel = new JPanel();
    tierPanel.add(tierComboBox);
    tierPanel.add(newTierButton);
    
    visibleCheckBox = new JCheckBox();
    labeledCheckBox = new JCheckBox();
    expandedCheckBox = new JCheckBox();
    
    typeComboBox = new JComboBox();
    typeComboBox.setRenderer(new TypeComboBoxRenderer());
    typeComboBox.addActionListener(new TypeComboBoxListener());
    newTypeButton = new JButton("New");
    newTypeButton.addActionListener(new NewTypeButtonListener());
    JPanel typePanel = new JPanel();
    typePanel.add(typeComboBox);
    typePanel.add(newTypeButton);
    
    Integer []shapeIndexes = new Integer[shapeLabels.length];
    for (int i = 0; i < shapeLabels.length; ++i) {
      shapeIndexes[i] = i;
    }
    shapeComboBox = new JComboBox(shapeIndexes);
    shapeComboBox.setRenderer(new ShapeComboBoxRenderer());
    shapeComboBox.addActionListener(new ShapeComboBoxListener());

    colorButton = new JButton("Change");
    colorButton.addActionListener(new ColorButtonListener());
    colorButtonPanel = new JPanel(new GridBagLayout());
    colorButtonPanel.add(colorButton, new GridBagConstraints());
    
    scaleCheckBox = new JCheckBox();
    
    minScoreField = new JTextField(5);
    minScoreField.setEditable(true);

    maxScoreField = new JTextField(5);
    maxScoreField.setEditable(true);
    
    urlField = new JTextField(25);
    urlField.setEditable(true);

    dataTypeArea = new JTextArea();
    dataTypeArea.setRows(3);
    dataTypeArea.setColumns(25);
    
    separator = new JSeparator(JSeparator.HORIZONTAL);
    
    matchUtrColorToBackgroundCheckBox = new JCheckBox();
    matchUtrColorToBackgroundLabel = new JLabel("Match UTR color to background?");
    
    setTierValues();

    setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(5, 5, 5, 5);
    c.anchor = GridBagConstraints.WEST;

    c.gridx = 0;
    add(new JLabel("Tier"), c);
    c.gridx = 1;
    c.gridwidth = GridBagConstraints.REMAINDER;
    add(tierPanel, c);

    c.gridwidth = 1;
    c.anchor = GridBagConstraints.WEST;
    c.gridy = 1;
    c.gridx = 1;
    add(new JLabel("Visible?"), c);
    c.gridx = 2;
    add(visibleCheckBox, c);
    
    c.gridy = 2;
    c.gridx = 1;
    add(new JLabel("Labeled?"), c);
    c.gridx = 2;
    add(labeledCheckBox, c);

    c.gridy = 3;
    c.gridx = 1;
    add(new JLabel("Expanded?"), c);
    c.gridx = 2;
    add(expandedCheckBox, c);

    c.gridy = 4;
    c.gridx = 0;
    c.gridwidth = GridBagConstraints.REMAINDER;
    add(separator, c);

    c.gridwidth = 1;
    c.gridy = 5;
    c.gridx = 0;
    add(new JLabel("Drawing group"), c);
    c.gridx = 1;
    c.gridwidth = GridBagConstraints.REMAINDER;
    add(typePanel, c);

    c.gridy = 6;
    c.gridx = 1;
    c.gridwidth = 1;
    add(new JLabel("Shape"), c);
    c.gridx = 2;
    add(shapeComboBox, c);

    c.gridy = 7;
    c.gridx = 1;
    c.gridwidth = 1;
    add(matchUtrColorToBackgroundLabel , c);
    c.gridx = 2;
    add(matchUtrColorToBackgroundCheckBox, c);
    
    c.gridy = 8;
    c.gridx = 1;
    add(new JLabel("Color"), c);
    c.gridx = 2;
    add(colorButtonPanel, c);

    c.gridy = 9;
    c.gridx = 1;
    add(new JLabel("Scale height by score?"), c);
    c.gridx = 2;
    add(scaleCheckBox, c);

    c.gridy = 10;
    c.gridx = 1;
    add(new JLabel("Minimum score for scaling height"), c);
    c.gridx = 2;
    add(minScoreField, c);

    c.gridy = 11;
    c.gridx = 1;
    add(new JLabel("Maximum score for scaling height"), c);
    c.gridx = 2;
    add(maxScoreField, c);
    
    c.gridy = 12;
    c.gridx = 1;
    add(new JLabel("URL"), c);
    c.gridx = 2;
    add(urlField, c);

    c.gridy = 13;
    c.gridx = 1;
    add(new JLabel("Apply to following data types"), c);
    c.gridx = 2;
    add(new JScrollPane(dataTypeArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS), c);

    separator.setPreferredSize(new Dimension(getPreferredSize().width, 1));
  }
  
  /** Given the currently loaded tier, set the values for the different components
   *  in the GUI.
   *
   */
  private void setTierValues()
  {
    TierProperty tp = (TierProperty)tierComboBox.getSelectedItem();
    visibleCheckBox.setSelected(tp.isVisible());
    labeledCheckBox.setSelected(tp.isLabeled(false));
    expandedCheckBox.setSelected(tp.isExpanded());
    typeComboBox.setModel(new DefaultComboBoxModel(tp.getFeatureProperties()));
    typeComboBox.setPreferredSize(tierComboBox.getPreferredSize());

    setTypeValues();
  }
  
  /** Given the currently loaded type, set the values for the different components
   *  in the GUI.
   *
   */
  private void setTypeValues()
  {
    FeatureProperty fp = (FeatureProperty)typeComboBox.getSelectedItem();
    if (fp == null) {
      return;
    }
    shapeComboBox.setSelectedIndex(getShapeIndex(fp.getStyle()));
    setColorButtonColor(fp.getColour());
    scaleCheckBox.setSelected(fp.getSizeByScore());
    minScoreField.setText(Float.toString(fp.getMinScore()));
    maxScoreField.setText(Float.toString(fp.getMaxScore()));
    urlField.setText(fp.getURLString());
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < fp.getAnalysisTypes().size(); ++i) {
      if (i > 0) {
        sb.append("\n");
      }
      sb.append(fp.getAnalysisType(i));
    }
    dataTypeArea.setText(sb.toString());
    setMatchUtrColorToBackgroundCheckBox();
  }

  /** Sets the background color for a button and its background panel.
   * 
   * @param c - Color to set the button/panel's background.
   */
  private void setColorButtonColor(Color c)
  {
    colorButton.setBackground(c);
    colorButtonPanel.setBackground(c);
    if (c == null) {
      return;
    }
    if (c.equals(Color.BLACK)) {
      colorButton.setForeground(Color.WHITE);
    }
    else {
      colorButton.setForeground(Color.BLACK);
    }
  }
  
  /** Sets the UTR color to match the background color (simple way for
   *  Apollo to make UTR seems transparent (what a hack =P ).
   *  Checks to see that the box is checked, and if so, sets the
   *  colors to match.  Note that this is only applicable to
   *  DrawableGeneFeatureSet, so if another drawable shape is selected
   *  the UTR color checkbox is disabled.
   *
   */
  private void setMatchUtrColorToBackgroundCheckBox()
  {
    FeatureProperty fp = (FeatureProperty)typeComboBox.getSelectedItem();
    if (fp == null) {
      return;
    }
    if (selectedShapeSupportsUtrColor()) {
      matchUtrColorToBackgroundCheckBox.setEnabled(true);
      matchUtrColorToBackgroundLabel.setEnabled(true);
      matchUtrColorToBackgroundCheckBox.setSelected(fp.getUtrColor().equals(Config.getStyle().getAnnotationBackground()));
    }
    else {
      matchUtrColorToBackgroundCheckBox.setEnabled(false);
      matchUtrColorToBackgroundLabel.setEnabled(false);
    }
  }
  
  /** Checks to see if the selected shape support UTR coloring (only DrawableGeneFeatureSet supports it).
   * 
   * @return true if the current selected shape support UTR coloring.
   */
  private boolean selectedShapeSupportsUtrColor()
  {
    return shapeComboBox.getSelectedItem().equals(new Integer(0));
  }
  
  /** Gets the index for a shape name from the static array.
   * 
   * @param shapeLabel - label of the shape to get the index for
   * @return index of the shape label
   */
  private int getShapeIndex(String shapeLabel)
  {
    for (int i = 0; i < shapeLabels.length; ++i) {
      if (shapeLabels[i].equals(shapeLabel)) {
        return i;
      }
    }
    return -1;
  }

  /** Revert changes.
   * 
   */
  public void revert()
  {
    for (Map.Entry<String, FeatureProperty> i : origFeatureProperties.entrySet()) {
      FeatureProperty origFp = i.getValue();
      FeatureProperty fp = Config.getPropertyScheme().getFeatureProperty(origFp.getDisplayType());
      CollectionUtil.copyElements(origFp.getAnalysisTypes(), fp.getAnalysisTypes(), true);
      fp.setStyle(origFp.getStyle());
      fp.setColour(origFp.getColour(), true);
      fp.getTier().setVisible(origFp.getTier().isVisible(), true);
      fp.getTier().setLabeled(origFp.getTier().isLabeled(false), true);
      fp.getTier().setExpanded(origFp.getTier().isExpanded(), true);
      fp.setSizeByScore(origFp.getSizeByScore(), true);
      fp.setMinScore(origFp.getMinScore(), true);
      fp.setMaxScore(origFp.getMaxScore(), true);
      fp.setURLString(origFp.getURLString());
      fp.setUtrColor(origFp.getUtrColor());
    }
  }
  
  /** Store the different supported component values into the currently loaded
   *  tier/type objects.
   *  
   */
  public void saveComponentValues()
  {
    FeatureProperty fp = (FeatureProperty)typeComboBox.getSelectedItem();
    if (origFeatureProperties.get(fp.getDisplayType()) == null) {
      try {
        origFeatureProperties.put(fp.getDisplayType(), new FeatureProperty(fp, true));
      }
      catch (CloneNotSupportedException e) {
      }
    }
    if (dataTypeArea.getText().length() > 0) {
      fp.getAnalysisTypes().clear();
      for (String dataType : dataTypeArea.getText().split("\n")) {
        if (dataType.length() > 0) {
          fp.addAnalysisType(dataType);
        }
      }
    }
    if (!fp.getStyle().equals(shapeLabels[((Integer)shapeComboBox.getSelectedItem()).intValue()])) {
      fp.setStyle(shapeLabels[((Integer)shapeComboBox.getSelectedItem()).intValue()]);
    }
    if (!fp.getColour().equals(colorButton.getBackground())) {
      fp.setColour(colorButton.getBackground(), true);
    }
    if (fp.getTier().isVisible() != visibleCheckBox.isSelected()) {
      fp.getTier().setVisible(visibleCheckBox.isSelected(), true);
    }
    if (fp.getTier().isLabeled(false) != labeledCheckBox.isSelected()) {
      fp.getTier().setLabeled(labeledCheckBox.isSelected(), true);
    }
    if (fp.getTier().isExpanded() != expandedCheckBox.isSelected()) {
      fp.getTier().setExpanded(expandedCheckBox.isSelected(), true);
    }
    if (fp.getSizeByScore() != scaleCheckBox.isSelected()) {
      fp.setSizeByScore(scaleCheckBox.isSelected(), true);
    }
    if (minScoreField.getText().length() > 0 && fp.getMinScore() != Float.parseFloat(minScoreField.getText())) {
      fp.setMinScore(Float.parseFloat(minScoreField.getText()), true);
    }
    if (maxScoreField.getText().length() > 0 && fp.getMaxScore() != Float.parseFloat(maxScoreField.getText())) {
      fp.setMaxScore(Float.parseFloat(maxScoreField.getText()), true);
    }
    if (urlField.getText().length() > 0) {
      fp.setURLString(urlField.getText());
    }
    else {
      fp.setURLString(null);
    }
    if (selectedShapeSupportsUtrColor()) {
      if (matchUtrColorToBackgroundCheckBox.isSelected()) {
        if (!fp.getUtrColor().equals(Config.getStyle().getAnnotationBackground())) {
          fp.setUtrColor(Config.getStyle().getAnnotationBackground());
        }
      }
      else {
        fp.setUtrColor(fp.getColour());
      }
    }
    
  }

  /** Writes the types/tiers data to a file.  Suggests writing to the user home directory,
   *  with the same filename as the currently loaded tiers file. 
   *
   */
  public String writeTypes()
  {
    saveComponentValues();
    if (needsSave()) {
      String fname = Config.getStyle().getTiersFile();
      //FileMenu.saveTiers(null);
      String userHomeDir = System.getProperty("user.home") + "/.apollo";
      JFileChooser fc = new JFileChooser(userHomeDir);
      fc.setDialogTitle("Save Tiers file");
      fc.setSelectedFile(new File(userHomeDir + "/" + fname));
      if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        logger.info("Saving tiers file to " + fc.getSelectedFile());
        try {
          Config.getPropertyScheme().write(fc.getSelectedFile());
          fname = fc.getSelectedFile().getName();
        }
        catch (IOException e) {
          String errMsg = "Error writing tiers file " + fc.getSelectedFile() + ": " + e.getMessage(); 
          logger.error(errMsg);
          JOptionPane.showMessageDialog(this, errMsg, "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
      else {
        return null;
      }
      return fname;
    }
    return null;
  }

  /** Sets the selected tier/feature given a FeatureProperty.
   * 
   * @param fp - FeatureProperty to set the feature to and its corresponding tier.
   */
  public void setSelectedType(FeatureProperty fp)
  {
    tierComboBox.setSelectedItem(fp.getTier());
    typeComboBox.setSelectedItem(fp);
  }
  
  /** Sets the different tier values to "empty".  Used when creating a new tier.
   *
   */
  private void resetTierValues()
  {
    visibleCheckBox.setSelected(false);
    labeledCheckBox.setSelected(false);
    expandedCheckBox.setSelected(false);
  }
  
  /** Sets the different type values to "empty".  Used when creating a new type.
  *
  */
  private void resetTypeValues()
  {
    dataTypeArea.setText(null);
    shapeComboBox.setSelectedIndex(0);
    setColorButtonColor(null);
    minScoreField.setText(null);
    maxScoreField.setText(null);
    urlField.setText(null);
    scaleCheckBox.setSelected(false);
  }
  
  /** Checks to see if any changes were made that need to be saved.
   * 
   * @return true if saving is needed.
   */
  private boolean needsSave()
  {
    for (Map.Entry<String, FeatureProperty> i : origFeatureProperties.entrySet()) {
      FeatureProperty origFp = i.getValue();
      FeatureProperty fp = Config.getPropertyScheme().getFeatureProperty(origFp.getDisplayType());
      if (!origFp.getAnalysisTypes().equals(fp.getAnalysisTypes())) {
        return true;
      }
      if (!origFp.getStyle().equals(fp.getStyle())) {
        return true;
      }
      if (!origFp.getColour().equals(fp.getColour())) {
        return true;
      }
      if (origFp.getTier().isVisible() != fp.getTier().isVisible()) {
        return true;
      }
      if (origFp.getTier().isLabeled(false) != fp.getTier().isLabeled(false)) {
        return true;
      }
      if (origFp.getTier().isExpanded() != fp.getTier().isExpanded()) {
        return true;
      }
      if (origFp.getSizeByScore() != fp.getSizeByScore()) {
        return true;
      }
      if (origFp.getMinScore() != fp.getMinScore()) {
        return true;
      }
      if (origFp.getMaxScore() != fp.getMaxScore()) {
        return true;
      }
      if (origFp.getURLString() == null ^ fp.getURLString() == null) {
        return true;
      }
      if (origFp.getURLString() != null && !origFp.getURLString().equals(fp.getURLString())) {
        return true;
      }
      if (!origFp.getUtrColor().equals(fp.getUtrColor())) {
        return true;
      }
    }
    return false;
  }
  
  /** ActionListener for handling creation of new tiers.
   *
   */
  private class NewTierButtonListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      String tierName = JOptionPane.showInputDialog(null, "Enter new tier name", "Enter new tier name", JOptionPane.QUESTION_MESSAGE);
      if (tierName != null) {
        TierProperty tp = new TierProperty(tierName);
        Config.getPropertyScheme().addTierType(tp);
        tierComboBox.setSelectedItem(tp);
        resetTierValues();
        resetTypeValues();
      }
    }
  }

  /** ActionListener for handling creation of new types.
   *
   */
  private class NewTypeButtonListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      String typeName = JOptionPane.showInputDialog(null, "Enter new drawing group name", "Enter new drawing group name", JOptionPane.QUESTION_MESSAGE);
      if (typeName != null) {
        TierProperty tp = (TierProperty)tierComboBox.getSelectedItem();
        Vector<String> analysisTypes = new Vector<String>();
        analysisTypes.add(typeName);
        FeatureProperty fp = new FeatureProperty(tp, typeName, analysisTypes);
        tp.addFeatureProperty(fp);
        typeComboBox.setSelectedItem(fp);
      }
    }
  }

  /** ActionListener for handling selection of a new tier from
   *  the combo box.  Updates the tier display with the loaded
   *  values.
   *  
   */
  private class TierComboBoxListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      saveComponentValues();
      setTierValues();
    }
  }
  
  /** ActionListener for handling selection of a type from
   *  the combo box.  Updates the type display with the loaded
   *  values.
   *
   */
  private class TypeComboBoxListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      setTypeValues();
    }
  }
  
  /** ActionListener for handling changing the color for a given
   *  type.
   *
   */
  private class ColorButtonListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      String title = "Choose color for " + ((FeatureProperty)typeComboBox.getSelectedItem()).getDisplayType();
      Color c = JColorChooser.showDialog(null, title, colorButton.getBackground());
      if (c != null) {
        setColorButtonColor(c);
      }          
    }
  }

  /** ActionListener for handling selection of a different drawable shape
   *  for a given type.
   *  
   */
  private class ShapeComboBoxListener implements ActionListener
  {
    public void actionPerformed (ActionEvent e)
    {
      setMatchUtrColorToBackgroundCheckBox();
    }
  }
  
  /** ListCellRenderer for displaying the different types available.  This is
   *  used since the default toString() method for a FeatureProperty returns
   *  quite a verbose string.  Instead this only displays the FeatureProperty
   *  name.
   *
   */
  private class TypeComboBoxRenderer extends JLabel implements ListCellRenderer
  {
    public TypeComboBoxRenderer()
    {
      setOpaque(true);
    }
    
    public Component getListCellRendererComponent(JList list, Object value, int index,
        boolean isSelected, boolean cellHasFocus)
    {
      if (value != null) {
        FeatureProperty fp = (FeatureProperty)value;
        if (isSelected) {
          setBackground(list.getSelectionBackground());
          setForeground(list.getSelectionForeground());
        }
        else {
          setBackground(list.getBackground());
          setForeground(list.getForeground());
        }
        setText(fp.getDisplayType());
      }
      else {
        setText(null);
      }
      return this;
    }
  }

  /** ListCellRenderer for displaying text and an image in a JComboBox.  Used
   *  for displaying the available drawable shapes.
   *
   */
  private class ShapeComboBoxRenderer extends JLabel implements ListCellRenderer
  {
    public ShapeComboBoxRenderer()
    {
      setOpaque(true);
      setHorizontalAlignment(LEFT);
      setVerticalAlignment(CENTER);
      setHorizontalTextPosition(LEFT);
    }

    public Component getListCellRendererComponent(JList list, Object value, int index,
        boolean isSelected, boolean cellHasFocus)
    {
      int idx = ((Integer)value).intValue();
      if (isSelected) {
        setBackground(list.getSelectionBackground());
        setForeground(list.getSelectionForeground());
      }
      else {
        setBackground(list.getBackground());
        setForeground(list.getForeground());
      }
      setText(shapeLabels[idx]);
      setIcon(shapeIcons[idx]);
      return this;
    }
  }
  
}