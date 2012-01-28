package apollo.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;
import java.io.File;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;

import apollo.config.Config;
import apollo.config.Style;
import apollo.util.CollectionUtil;

import org.apache.log4j.*;

/** GUI for handling the configuration of Apollo styles.  Note that it only
 *  handles a subset of the many options for Style.  This is done to not
 *  overwhelm the average user (power users will probably be editing the style
 *  files by hand anyway).  Suzi and I sat down and looked over the many options
 *  and came up with these as being the most useful ones.
 * 
 * @author elee
 *
 */

public class StyleWizard extends JPanel
{

  protected final static Logger logger = LogManager.getLogger(Style.class);
  
  private JTextField tiersFileField;
  private JCheckBox showAnnotationsCheckBox;
  private JCheckBox showResultsCheckBox;
  private JCheckBox enableEditingCheckBox;
  private JButton annotationBackgroundColorButton;
  private JButton featureBackgroundColorButton;
  private JButton annotationLabelColorButton;
  private JButton featureLabelColorButton;

  private JPanel annotationBackgroundColorButtonPanel;
  private JPanel featureBackgroundColorButtonPanel;
  private JPanel annotationLabelColorButtonPanel;
  private JPanel featureLabelColorButtonPanel;
  
  private JTextArea annotationCommentsArea;
  private JTextArea transcriptCommentsArea;
  private JScrollPane annotationCommentsAreaScrollPane;
  private JScrollPane transcriptCommentsAreaScrollPane;
  
  private ConfigurableStyleOptions origOptions;
  
  /** Constructor.
   *
   */
  public StyleWizard()
  {
    init();
  }

  /** Initialize and place the components.
  *
  */
  private void init()
  {
    origOptions = new ConfigurableStyleOptions(getStyle());
    
    tiersFileField = new JTextField(25);
    tiersFileField.setEditable(false);

    showAnnotationsCheckBox = new JCheckBox("Show");
    showResultsCheckBox = new JCheckBox("Show");
    enableEditingCheckBox = new JCheckBox();
    
    ColorButtonListener cbl = new ColorButtonListener();
    annotationBackgroundColorButton = new JButton("Change");
    annotationBackgroundColorButton.addActionListener(cbl);
    featureBackgroundColorButton = new JButton("Change");
    featureBackgroundColorButton.addActionListener(cbl);
    annotationLabelColorButton = new JButton("Change");
    annotationLabelColorButton.addActionListener(cbl);
    featureLabelColorButton = new JButton("Change");
    featureLabelColorButton.addActionListener(cbl);

    GridBagConstraints gc = new GridBagConstraints();
    annotationBackgroundColorButtonPanel = new JPanel(new GridBagLayout());
    annotationBackgroundColorButtonPanel.add(annotationBackgroundColorButton, gc);
    featureBackgroundColorButtonPanel = new JPanel(new GridBagLayout());
    featureBackgroundColorButtonPanel.add(featureBackgroundColorButton, gc);
    annotationLabelColorButtonPanel = new JPanel(new GridBagLayout());
    annotationLabelColorButtonPanel.add(annotationLabelColorButton, gc);
    featureLabelColorButtonPanel = new JPanel(new GridBagLayout());
    featureLabelColorButtonPanel.add(featureLabelColorButton, gc);

    annotationCommentsArea = new JTextArea();
    annotationCommentsArea.setRows(5);
    annotationCommentsArea.setColumns(25);
    transcriptCommentsArea = new JTextArea();
    transcriptCommentsArea.setRows(5);
    transcriptCommentsArea.setColumns(25);

    setValues();

    JPanel panel = new JPanel();
    panel.setLayout(new GridBagLayout());
    
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(5, 5, 5, 5);
    c.anchor = GridBagConstraints.WEST;
    panel.add(new JLabel("Tiers file"), c);
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.gridx = 1;
    panel.add(tiersFileField, c);

    c.gridwidth = 1;
    c.gridx = 0;
    c.gridy = 1;
    panel.add(new JLabel("Enable annotation editing?"), c);
    c.gridx = 1;
    panel.add(enableEditingCheckBox, c);
    
    c.gridwidth = 1;
    c.gridx = 1;
    c.gridy = 2;
    panel.add(new JLabel("Annotations"), c);

    c.gridx = 2;
    c.gridy = 2;
    panel.add(new JLabel("Results"), c);

    c.gridx = 1;
    c.gridy = 3;
    panel.add(showAnnotationsCheckBox, c);
    
    c.gridx = 2;
    c.gridy = 3;
    panel.add(showResultsCheckBox, c);

    c.gridx = 0;
    c.gridy = 4;
    panel.add(new JLabel("Background color"), c);
    c.gridx = 1;
    //panel.add(annotationBackgroundColorButton, c);
    panel.add(annotationBackgroundColorButtonPanel, c);
    c.gridx = 2;
    //panel.add(featureBackgroundColorButton, c);
    panel.add(featureBackgroundColorButtonPanel, c);
    
    c.gridx = 0;
    c.gridy = 5;
    panel.add(new JLabel("Label color"), c);
    c.gridx = 1;
    //panel.add(annotationLabelColorButton, c);
    panel.add(annotationLabelColorButtonPanel, c);
    c.gridx = 2;
    //panel.add(featureLabelColorButton, c);
    panel.add(featureLabelColorButtonPanel, c);
    
    c.gridx = 0;
    c.gridy = 6;
    panel.add(new JLabel("Canned annotation comments"), c);
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.gridx = 1;
    annotationCommentsAreaScrollPane = new JScrollPane(annotationCommentsArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); 
    panel.add(annotationCommentsAreaScrollPane, c);

    c.gridwidth = 1;
    c.gridx = 0;
    c.gridy = 7;
    panel.add(new JLabel("Canned transcript comments"), c);
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.gridx = 1;
    transcriptCommentsAreaScrollPane = new JScrollPane(transcriptCommentsArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); 
    panel.add(transcriptCommentsAreaScrollPane, c);
    
    add(panel);
  }

  /** Store the different supported component values into the currently loaded
   *  Style object.
   *  
   */
  public void saveComponentValues()
  {
    Style style = getStyle();
    if (style.getShowAnnotations() != showAnnotationsCheckBox.isSelected()) {
      style.setShowAnnotations(showAnnotationsCheckBox.isSelected());
    }
    if (style.getShowResults() != showResultsCheckBox.isSelected()) {
      style.setShowResults(showResultsCheckBox.isSelected());
    }
    if (style.isEditingEnabled() != enableEditingCheckBox.isSelected()) {
      style.setEditingEnabled(enableEditingCheckBox.isSelected());
    }
    if (!style.getAnnotationBackground().equals(annotationBackgroundColorButton.getBackground())) {
      style.setAnnotationBackground(annotationBackgroundColorButton.getBackground());
    }
    if (!style.getFeatureBackground().equals(featureBackgroundColorButton.getBackground())) {
      style.setFeatureBackground(featureBackgroundColorButton.getBackground());
    }
    if (!style.getAnnotationLabelColor().equals(annotationLabelColorButton.getForeground())) {
      style.setAnnotationLabelColor(annotationLabelColorButton.getForeground());
    }
    if (!style.getFeatureLabelColor().equals(featureLabelColorButton.getForeground())) {
      style.setFeatureLabelColor(featureLabelColorButton.getForeground());
    }
    // add annotation comments
    saveText(annotationCommentsArea, style, true);
    // add transcript comments
    saveText(transcriptCommentsArea, style, false);
  }

  /** Revert changes.
   * 
   */
  public void revert()
  {
    Style style = getStyle();
    style.setShowAnnotations(origOptions.getShowAnnotations());
    style.setShowResults(origOptions.getShowResults());
    style.setEditingEnabled(origOptions.isEditingEnabled());
    style.setAnnotationBackground(origOptions.getAnnotationBackgroundColor());
    style.setFeatureBackground(origOptions.getFeatureBackgroundColor());
    style.setAnnotationLabelColor(origOptions.getAnnotationLabelColor());
    style.setFeatureLabelColor(origOptions.getFeatureLabelColor());
    copyVector(origOptions.getAnnotationComments(), style.getAnnotationComments());
    copyVector(origOptions.getTranscriptComments(), style.getTranscriptComments());
  }
  
  /** Writes the style data to a file.  Suggests writing to the user home directory,
   *  with the same filename as the currently loaded style file. 
   *
   *  @return String for the name of the written Style file.
   */
  public String writeStyle()
  {
    saveComponentValues();
    if (needsSave()) {
      String userHomeDir = System.getProperty("user.home") + "/.apollo";
      JFileChooser fc = new JFileChooser(userHomeDir);
      fc.setDialogTitle("Save Style file");
      fc.setSelectedFile(new File(userHomeDir + "/" + getStyle().getFileName()));
      if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        logger.info("Saving style file to " + fc.getSelectedFile());
        if (getStyle().writeStyle(fc.getSelectedFile())) {
          return fc.getSelectedFile().getAbsolutePath();
        }
      }
      else {
        return null;
      }
    }
    return getStyle().getFileName();
  }
  
  /** Given the currently loaded style, set the values for the different components
   *  in the GUI.
   *
   */
  private void setValues()
  {
    Style style = getStyle();
    tiersFileField.setText(new File(style.getTiersFile()).getName());
    showAnnotationsCheckBox.setSelected(style.getShowAnnotations());
    showResultsCheckBox.setSelected(style.getShowResults());
    enableEditingCheckBox.setSelected(style.isEditingEnabled());
    setButtonColor(annotationBackgroundColorButton, style.getAnnotationBackground());
    setButtonColor(featureBackgroundColorButton, style.getFeatureBackground());
    setButtonColor(annotationLabelColorButton, style.getAnnotationLabelColor());
    setButtonColor(featureLabelColorButton, style.getFeatureLabelColor());
    setText(annotationCommentsArea, style.getAnnotationComments());
    setText(transcriptCommentsArea, style.getTranscriptComments());
  }
  
  /** Method for setting the button colors for annotatation background/label
   *  and result background/label.  We decided to make the background/label
   *  color buttons interact with each other, so that changing the label from
   *  the label button also affects the label in the background button
   *  and changing the background from the background button changes the background
   *  in the label button.  This is useful to let the user get a "preview" of what
   *  the colors will look like.
   *  
   * @param button - JButton generating the event (whose color has changed)
   * @param color - Color chosen by the user
   */
  private void setButtonColor(JButton button, Color color)
  {
    if (color == null) {
      return;
    }
    if (button == annotationBackgroundColorButton) {
      button.setBackground(color);
      annotationLabelColorButton.setBackground(color);
      annotationBackgroundColorButtonPanel.setBackground(color);
      annotationLabelColorButtonPanel.setBackground(color);
    }
    else if (button == featureBackgroundColorButton) {
      button.setBackground(color);
      featureLabelColorButton.setBackground(color);
      featureBackgroundColorButtonPanel.setBackground(color);
      featureLabelColorButtonPanel.setBackground(color);
      }
    else if (button == annotationLabelColorButton) {
      button.setForeground(color);
      annotationBackgroundColorButton.setForeground(color);
    }
    else if (button == featureLabelColorButton) {
      button.setForeground(color);
      featureBackgroundColorButton.setForeground(color);
    }
  }
  
  /** Sets the text for a text component object.  For each object in
   *  the Vector, the toString() method is called and appended
   *  to the component's text (one per line).
   * 
   * @param component - JTextComponent where to set the text set
   * @param text - Vector of objects to add to the text
   */
  private void setText(JTextComponent component, Vector text)
  {
    if (text == null) {
      return;
    }
    StringBuffer sb = new StringBuffer();
    int startIndex = 1;
    for (int i = startIndex; i < text.size(); ++i) {
      if (i > startIndex) {
        sb.append("\n");
      }
      sb.append(text.get(i).toString());
    }
    component.setText(sb.toString());
    // for some reason the caret position was being set to the end of the text
    // so the JScrollPane was scrolling to the end...I wanted the text to display
    // at the beginning, so I'm manually setting the caret to 0 here.
    component.setCaretPosition(0);
  }
  
  /** Takes each line of text from the text component and stores it as a comment.
   * 
   * @param component - JTextComponent where to get the text from
   * @param style - Style object to add comments to
   * @param isAnnotationComment - whether the comment is an annotation comment (true) or a transcript comment (false)
   */
  private void saveText(JTextComponent component, Style style, boolean isAnnotationComment)
  {
    for (String s : component.getText().split("\n")) {
      if (s.length() == 0) {
        continue;
      }
      if (isAnnotationComment) {
        style.addAnnotationComment(s);
      }
      else {
        style.addTranscriptComment(s);
      }
    }
  }

  /** Utility method for copying the elements from one vector to another.  The destination is cleared before
   *  the copy.
   *  
   * @param src - Vector containing data to be copied.
   * @param dest - Vector to have data copied to.
   */
  private void copyVector(Vector src, Vector dest)
  {
    if (dest == null) {
      return;
    }
    dest.clear();
    if (src == null) {
      return;
    }
    CollectionUtil.copyElements(src, dest, false);
  }

  /** Utility method for comparing equality of vectors.  Handles the case where
   *  one or both are null.
   *  
   * @param v1 - Vector being compared
   * @param v2 - Vector being compared to
   * @return true if both v1 and v2 are null or both v1 and v2 have the same contents (including order)
   */
  private boolean areVectorsEqual(Vector v1, Vector v2)
  {
    if (v1 == null && v2 == null) {
      return true;
    }
    if (v1 == null || v2 == null) {
      return false;
    }
    return v1.equals(v2);
  }
  
  /** Checks to see if any changes were made that need to be saved.
   * 
   * @return true if saving is needed.
   */
  private boolean needsSave()
  {
    Style style = getStyle();

    String oldTiersFile = new File(origOptions.getTiersFile()).getName();
    String newTiersFile = new File(style.getTiersFile()).getName();
    
    return !oldTiersFile.equals(newTiersFile) ||
            origOptions.getShowAnnotations() != style.getShowAnnotations() ||
            origOptions.getShowResults() != style.getShowResults() ||
            origOptions.isEditingEnabled() != style.isEditingEnabled() ||
            !origOptions.getAnnotationBackgroundColor().equals(style.getAnnotationBackground()) ||
            !origOptions.getFeatureBackgroundColor().equals(style.getFeatureBackground()) ||
            !origOptions.getAnnotationLabelColor().equals(style.getAnnotationLabelColor()) ||
            !origOptions.getFeatureLabelColor().equals(style.getFeatureLabelColor()) ||
            !areVectorsEqual(origOptions.getAnnotationComments(), style.getAnnotationComments()) ||
            !areVectorsEqual(origOptions.getTranscriptComments(), style.getTranscriptComments());
  }
  
  /** Convenience method for getting the currently active style.
   * 
   * @return the currently active Style object
   */
  private Style getStyle()
  {
    return Config.getStyle();
  }

  /** ActionListener for handling changing the colors for the annotation
   *  background/label and result background/label buttons.  Given the event
   *  it figures out whether to update the background or label color.
   */
  private class ColorButtonListener implements ActionListener
  {
    public void actionPerformed(ActionEvent ae)
    {
      String title = "Choose color";
      JButton button = (JButton)ae.getSource();
      Color oldColor = null;
      if (button == annotationBackgroundColorButton ||
          button == featureBackgroundColorButton) {
        oldColor = button.getBackground();
      }
      else if (button == annotationLabelColorButton ||
          button == featureLabelColorButton) {
        oldColor = button.getForeground();
      }
      Color newColor = JColorChooser.showDialog(null, title, oldColor);
      if (newColor != null && !oldColor.equals(newColor)) {
        setButtonColor(button, newColor);
      }
    }
  }
  
  /** Auxiliary class for storing configurable style options.  Style
   *  doesn't support clone() and to copy all of the style data when
   *  we can only configure a handful of them is overkill anyway.
   *
   */
  private class ConfigurableStyleOptions
  {
    private String tiersFile;
    private boolean showAnnotations;
    private boolean showResults;
    private boolean editingEnabled;
    private Color annotationBackgroundColor;
    private Color featureBackgroundColor;
    private Color annotationLabelColor;
    private Color featureLabelColor;
    private Vector<String> annotationComments;
    private Vector<String> transcriptComments;
    
    public ConfigurableStyleOptions(Style style)
    {
      setTiersFile(style.getTiersFile());
      setShowAnnotations(style.getShowAnnotations());
      setShowResults(style.getShowResults());
      setEditingEnabled(style.isEditingEnabled());
      setAnnotationBackgroundColor(style.getAnnotationBackground());
      setFeatureBackgroundColor(style.getFeatureBackground());
      setAnnotationLabelColor(style.getAnnotationLabelColor());
      setFeatureLabelColor(style.getFeatureLabelColor());
      if (style.getAnnotationComments() != null) {
        annotationComments = new Vector<String>();
        copyVector(style.getAnnotationComments(), annotationComments);
      }
      if (style.getTranscriptComments() != null) {
        transcriptComments = new Vector<String>();
        copyVector(style.getTranscriptComments(), transcriptComments);
      }
    }

    public Color getAnnotationBackgroundColor() {
      return annotationBackgroundColor;
    }

    public void setAnnotationBackgroundColor(Color annotationBackgroundColor) {
      this.annotationBackgroundColor = annotationBackgroundColor;
    }

    public Vector<String> getAnnotationComments() {
      return annotationComments;
    }

    public void setAnnotationComments(Vector<String> annotationComments) {
      this.annotationComments = annotationComments;
    }

    public Color getAnnotationLabelColor() {
      return annotationLabelColor;
    }

    public void setAnnotationLabelColor(Color annotationLabelColor) {
      this.annotationLabelColor = annotationLabelColor;
    }

    public boolean isEditingEnabled() {
      return editingEnabled;
    }

    public void setEditingEnabled(boolean editingEnabled) {
      this.editingEnabled = editingEnabled;
    }

    public Color getFeatureBackgroundColor() {
      return featureBackgroundColor;
    }

    public void setFeatureBackgroundColor(Color featureBackgroundColor) {
      this.featureBackgroundColor = featureBackgroundColor;
    }

    public Color getFeatureLabelColor() {
      return featureLabelColor;
    }

    public void setFeatureLabelColor(Color featureLabelColor) {
      this.featureLabelColor = featureLabelColor;
    }

    public boolean getShowAnnotations() {
      return showAnnotations;
    }

    public void setShowAnnotations(boolean showAnnotations) {
      this.showAnnotations = showAnnotations;
    }

    public boolean getShowResults() {
      return showResults;
    }

    public void setShowResults(boolean showResults) {
      this.showResults = showResults;
    }

    public String getTiersFile() {
      return tiersFile;
    }

    public void setTiersFile(String tiersFile) {
      this.tiersFile = tiersFile;
    }

    public Vector<String> getTranscriptComments() {
      return transcriptComments;
    }

    public void setTranscriptComments(Vector<String> transcriptComments) {
      this.transcriptComments = transcriptComments;
    }

  }

}
