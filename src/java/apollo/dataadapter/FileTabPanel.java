package apollo.dataadapter;

import java.io.File;
import java.awt.Component;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;

import apollo.dataadapter.DataInputType;
import apollo.gui.GenericFileAdapterGUI;
import apollo.config.Style;
import apollo.util.GuiUtil;

/** A guiTabPanel (panel in JTabbed pane of ApolloAdapterUI) that has a browse
 * button that brings up a file browser
 */

public class FileTabPanel extends GuiTabPanel {

  private Component parentComponent;

  public FileTabPanel(Component parentComponent, Color bkgnd) {
    super("File", "Filename or URL", bkgnd);
    this.parentComponent = parentComponent;
  }

  public DataInputType getInputType() {
    if (getCurrentInput().startsWith("http"))
      return DataInputType.URL;
    else
      return DataInputType.FILE;
  }

  /** Add in browse button */
  protected void buildGUI() {
    super.buildGUI();
    JButton browseButton = new JButton("Browse...");
    browseButton.addActionListener( new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          browseFiles();
        }
      }
                                    );
    getInnerPanel().add(browseButton, getConstraints().nextColumn());
  }

  public void addCheckbox(JCheckBox checkbox) {
    getConstraints().nextRow();
    getInnerPanel().add(checkbox, getConstraints().nextColumn());
  }

  /** This is called from the browse button if it exists, brings up
   * a file browser and puts the selected file in the combo box
   * calls GenericFileAdapterGUI for file browser */
  private void browseFiles() {
    File selectedFile = null;
    if (getCurrentInput()!=null)
      selectedFile = new File(getCurrentInput());
    File browsedFile =
      GenericFileAdapterGUI.fileBrowser(selectedFile, parentComponent);
    if (browsedFile==null)
      return;
    // Stick file name in combo box
    comboBox.configureEditor(comboBox.getEditor(),browsedFile.toString());
  }

  /** No database for file access */
  protected boolean showDatabaseList() {
    return false;
  }

  /** This won't be called */
  protected Style getAdapterStyle() {
    return null;
  }

}
