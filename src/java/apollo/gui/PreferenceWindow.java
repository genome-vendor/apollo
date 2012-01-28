package apollo.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;

import javax.swing.*;

import apollo.gui.genomemap.StrandedZoomableApolloPanel;
import apollo.gui.synteny.CurationManager;
import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.datamodel.SeqFeatureI;

/** Window for displaying preference options.  The preferences uses tabbed panes,
 *  so new preference panels can be added as needed.
 * 
 * @author elee
 *
 */

public class PreferenceWindow extends JFrame {

  private static PreferenceWindow instance;
  private JTabbedPane tabbedPane;
  private StyleWizard styleWizard;
  private TypesWizard typesWizard;
  private JButton previewButton;
  private JButton cancelButton;
  private JButton saveButton;

  /** Static method for retrieving singleton instance.
   * 
   * @return PreferenceWindow singleton instance.
   */
  public static PreferenceWindow getInstance()
  {
    if (instance == null) {
      instance = new PreferenceWindow();
    }
    return instance;
  }

  /** Static method for retrieving singleton instance with selected type.
   * 
   * @return PreferenceWindow singleton instance.
   */
  public static PreferenceWindow getInstance(Selection s)
  {
    if (instance == null) {
      instance = new PreferenceWindow();
    }
    instance.tabbedPane.setSelectedComponent(instance.typesWizard);
    SeqFeatureI sf = s.getSelectedData(s.size()-1);
    FeatureProperty fp = Config.getPropertyScheme().getFeatureProperty(sf.getTopLevelType());
    instance.typesWizard.setSelectedType(fp);
    return instance;
  }
  
  /** Constructor.
   * 
   */
  private PreferenceWindow()
  {
    super("Preferences");
    init();
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    pack();
    setVisible(true);
    addWindowListener(new PreferenceWindowListener());
  }

  /** Initialize and place the components.
   *
   */
  private void init()
  {
    tabbedPane = new JTabbedPane();
    styleWizard = new StyleWizard();
    tabbedPane.addTab("Style", null, styleWizard, "Preferences for style");
    tabbedPane.setMnemonicAt(0, KeyEvent.VK_S);
    typesWizard = new TypesWizard();
    tabbedPane.addTab("Types", null, typesWizard, "Preferences for Tiers/Types");
    tabbedPane.setMnemonicAt(1, KeyEvent.VK_T);

    BottomPanelButtonListener bpbl = new BottomPanelButtonListener();
    previewButton = new JButton("Preview");
    previewButton.addActionListener(bpbl);
    cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(bpbl);
    saveButton = new JButton("Save");
    saveButton.addActionListener(bpbl);
    JPanel bottomButtonPanel = new JPanel();
    bottomButtonPanel.add(previewButton);
    bottomButtonPanel.add(cancelButton);
    bottomButtonPanel.add(saveButton);

    JPanel panel = new JPanel();
    panel.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();

    c.gridwidth = GridBagConstraints.REMAINDER;
    panel.add(tabbedPane, c);

    c.gridwidth = 1;
    c.gridx = 0;
    c.gridy = 1;
    panel.add(bottomButtonPanel, c);
    
    add(panel);
  }

  /** ActionListener for handling Preview/Cancel/Save buttons.
   * 
   */
  private class BottomPanelButtonListener implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      if (e.getSource() == previewButton) {
        styleWizard.saveComponentValues();
        typesWizard.saveComponentValues();
        //updateDisplay();
      }
      else if (e.getSource() == cancelButton) {
        styleWizard.revert();
        typesWizard.revert();
        dispose();
        instance = null;
      }
      else if (e.getSource() == saveButton) {
        String tierFname = typesWizard.writeTypes();
        if (tierFname != null && 
            !new File(Config.getStyle().getTiersFile()).getName().equals(tierFname)) {
          Config.getStyle().setTiersFile(tierFname);
        }
        String styleFname = styleWizard.writeStyle();
        if (styleFname == null) {
          styleWizard.revert();
        }
        else if (!Config.getStyle().getFileName().equals(styleFname)) {
          Config.getStyle().setStyleFile(styleFname);
        }
        if (tierFname == null) {
          typesWizard.revert();
        }
        dispose();
        instance = null;
      }
      updateDisplay();
      updateTypesPanel();
    }
  }

  /** Forcefully redraws the display.  When making changes to tiers/types, for some
   *  reason the display would not redraw itself reflecting the changes (such as
   *  changing the drawable for a feature), even though events were being fired
   *  (probably some issue with the handling of these events).  As for the style, since
   *  it is loaded during Apollo initialization, changes to it did not take effect
   *  until the next time Apollo was run.  By forcefully reloading the current
   *  CurationSet, we force Apollo to pick up the changes immediately (useful for
   *  the preview function).  A bit of a hack, but definitely the path of least
   *  resistance =P
   *
   */
  private void updateDisplay()
  {
    StrandedZoomableApolloPanel szap = CurationManager.getActiveCurationState().getSZAP();  
    //szap.setCurationSet(szap.getCurationSet());
    szap.setViewColours();
    szap.setAnnotations(szap.getCurationSet());
    szap.setFeatureSet(szap.getCurationSet());

    szap.setAnnotationViewsVisible(Config.getStyle().getShowAnnotations());
    szap.setResultViewsVisible(Config.getStyle().getShowResults());
  }
  
  private void updateTypesPanel()
  {
    TypePanel.getTypePanelInstance().handlePropSchemeChangeEvent(null);
  }
  
  /** Listener for handling the PreferenceWindown being closed.
   * 
   */
  private class PreferenceWindowListener extends WindowAdapter
  {
    public void windowClosing(WindowEvent e)
    {
      styleWizard.revert();
      typesWizard.revert();
      updateDisplay();
      instance = null;
    }
  }

}
