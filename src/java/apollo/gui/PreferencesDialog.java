package apollo.gui;

import java.awt.Color;
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;

import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JOptionPane;

import apollo.config.Config;
import apollo.config.Style;

import apollo.dataadapter.DataLoadEvent;
import apollo.dataadapter.DataLoadListener;

//import apollo.gui.genomemap.StrandedZoomableApolloPanel;
import apollo.gui.synteny.GuiCurationState;

import org.apache.log4j.*;

/**
 *  A dialog to let the user change the style parameters.
 *  Uses PrefsPanel for text panel that shows contents of style file.
 Im not totally sure how this should work with multi-curations. 
 */
 
public class PreferencesDialog extends JFrame 
  implements DataLoadListener,
  ControlledObjectI {
  
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(PreferencesDialog.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private JButton helpButton;
  private JButton restoreButton;
  private JButton saveButton;
  private JButton cancelButton;
  private PrefsPanel prefsPanel;
  //private StrandedZoomableApolloPanel szap;
  private Color bgColor = new Color(255,255,150);
  private String origText = "";
  private String defaultStyleFile;
  private String styleFileName;
  private Controller controller;

  //public PreferencesDialog(StrandedZoomableApolloPanel szap) {
  public PreferencesDialog() {
    //this.szap = szap;
    setController(Controller.getMasterController()); // regionChange
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

    // i could be wrong but i think Controller automatically does this
    addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  e.getWindow().setVisible(false);
	  PreferencesDialog.this.getController().removeListener(this);
	} } );

    try {
      defaultStyleFile = Config.getStyle().getFileName();
      styleFileName = personalStyle(defaultStyleFile);
      if (styleFileName == null) {
        this.setVisible(false);
        getController().removeListener(this);
        this.dispose();
        return;
      }
      jbInit();
      readPrefs(styleFileName);
      copyStyleFile(styleFileName);
    } catch(Exception e) {
      logger.error(e.getMessage(), e);
    }

    setTitle("Preferences");
  }

  /* Initialize layout and buttons */
  private void jbInit() {
    setSize(new Dimension(600,635));
    this.getContentPane().setLayout(new BoxLayout(this.getContentPane(),
						  BoxLayout.Y_AXIS));

    prefsPanel = new PrefsPanel(this, bgColor);

    this.getContentPane().add (prefsPanel);

    helpButton = new JButton("Help");
    helpButton.setBackground (Color.white);
    helpButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
          String rootdir = System.getProperty("APOLLO_ROOT");
          String help = rootdir + "/doc/html/userguide.html#StyleFiles";
	  apollo.gui.menus.HelpMenu.showHelpInBrowser(help);
	}
      }
				 );

    // Should we disable until they make some changes?
    restoreButton = new JButton("Restore original");
    restoreButton.setBackground (Color.white);
    restoreButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  restoreOriginal(styleFileName, styleFileName+".orig");
	}
      }
				    );

    // Should we disable Save until they make some changes?
    saveButton = new JButton("Save");
    saveButton.setBackground (Color.white);
    saveButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  save();
	}
      }
				 );
    saveButton.setEnabled(styleFileName != null);

    cancelButton = new JButton("Cancel");
    cancelButton.setBackground (Color.white);
    cancelButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  close();
	}
      }
				   );

    JPanel buttonPanel = new JPanel();
    buttonPanel.setBackground (bgColor);
    buttonPanel.add(helpButton);
    buttonPanel.add(restoreButton);
    buttonPanel.add(saveButton);
    buttonPanel.add(cancelButton);
    this.getContentPane().add(buttonPanel);
  }

  /* Puts the text of the style file into the preferences text box
   * but doesn't try to actually parse the style */
  private void readPrefs(String styleFile) {
    try {
      origText = apollo.util.IOUtil.readFile(styleFile);
    } catch ( Exception ex ) {
      logger.info("PreferencesDialog.readPrefs: couldn't read personal style file " + styleFile);
      return;
    }
    String confStyleFile = "";
    String exampleText = "";
    try {
      confStyleFile = styleFile;
      if (confStyleFile.indexOf("/") >= 0)
        confStyleFile = confStyleFile.substring(confStyleFile.lastIndexOf("/")+1);
      confStyleFile = apollo.util.IOUtil.findFile(apollo.util.IOUtil.getRootDir() + "/conf/" + confStyleFile);
      exampleText = apollo.util.IOUtil.readFile(confStyleFile);
    } catch ( Exception ex ) {
      logger.info("PreferencesDialog.readPrefs: couldn't read default style file " + confStyleFile);
      return;
    }
    if (origText != null) {
      prefsPanel.setText(exampleText, "example");
      prefsPanel.setText(origText, "personal");
      prefsPanel.addMessage("Default preferences file: " + confStyleFile, "default");
      prefsPanel.addMessage("Your personal preferences file: " + styleFile, "personal");
    }
  }

  /**
   * Save the changes to the style file, after first saving the original as
   * stylefilename.orig (if it doesn't already exist)
   */
  private void save() {
    // See if anything has changed
    String newText = prefsPanel.getText();
    if (newText.equals(origText)) {
      JOptionPane.showMessageDialog(null, "No changes to save");
      return;
    }

    String orig = styleFileName + ".orig";
    if (!copyStyleFile(styleFileName))
      return;

    // Save the new style file
    try {
      apollo.util.IOUtil.writeFile(styleFileName, newText);
    } catch ( Exception ex ) {
      String message = "Couldn't write to " + styleFileName + "\n.  Sorry--you will not be able to edit preferences.\nGive your sysadmin this message and see if they can fix the permissions for you.";
      logger.error(message);
      JOptionPane.showMessageDialog(null,message,"Warning",JOptionPane.WARNING_MESSAGE);
      saveButton.setEnabled(false);
      return;
    }

    // Read in the new style.  Restore original if new one is bad.
    setNewStyle(styleFileName, orig);  // orig is in case new one is bad
    JOptionPane.showMessageDialog(null, "Saved " + styleFileName + "\nYou may need to restart Apollo in order to see all your changes take effect.");
    close();
  }

  private boolean copyStyleFile(String styleFileName) {
    // First copy the original style file to stylefilename.orig 
    String orig = styleFileName + ".orig";
    if (!apollo.util.IOUtil.copyFile(styleFileName, orig)) {
      String message = "The directory where " + styleFileName + " lives is unwriteable.\nSorry--you will not be able to edit preferences.\nGive your sysadmin this message and see if they can fix the permissions for you.";
      logger.error(message);
      JOptionPane.showMessageDialog(null,message,"Warning",JOptionPane.WARNING_MESSAGE);
      return false;
    }
    logger.info("Original style file saved in " + orig);
    restoreButton.setEnabled(true);
    return true;
  }

  // Not used
//   private boolean origFileExists(String fileName) {
//     String orig = fileName + ".orig";
//     File handle = new File(orig);
//     if (handle.exists())
//       return true;
//     else
//       return false;
//   }

  private void close() {
    setVisible(false);
  }

  private void restoreOriginal(String current, String orig) {
    logger.debug("Restoring original style file " + orig);
    if (!(current.equals(orig))) {
      // Restore orig file
      if (!apollo.util.IOUtil.copyFile(orig, current)) {
        String msg = "Couldn't write to " + current;
        if (orig.endsWith("orig"))
          msg = msg + "\nUnable to restore " + current + " from " + orig;
        logger.error(msg);
        JOptionPane.showMessageDialog(null, msg);
        saveButton.setEnabled(false);
        restoreButton.setEnabled(false);
        return;
      }
      setNewStyle(current, orig);  // could this result in an infinite loop?
      logger.info("Restored style file " + current + " from original " + orig);
    }
  }

  // Read in the new style.  Restore original if new one is bad.
  private void setNewStyle(String newStyleFile, String origStyleFile) {
    logger.debug("setNewStyle: new = " + newStyleFile + ", orig = " + origStyleFile);
    checkStyle(newStyleFile, origStyleFile);
    // If new style file is bad, checkStyle will restore original

    // Now really set the new style
    Config.getStyle().readStyle(newStyleFile);
    Config.setStyle(Config.getStyle(), true);  // True forces an update of the data-adapter-to-style hashtable
    readPrefs(newStyleFile);

    // Hacky way to force a repaint of the parent (main window)
    //szap.setReverseComplement(!szap.isReverseComplement());
    //szap.setReverseComplement(!szap.isReverseComplement());
    ApolloFrame.getFrame().repaint();
  }

  /** If new style file is bad, checkStyle will restore original */
  private void checkStyle(String newStyleFile, String origStyleFile) {
    // Parse the new style file, checking for errors
    Style newStyle = new Style(newStyleFile);

    // If there were errors, restore the original style from origStyleFile
    if (!(newStyle.getErrors().equals(""))) {
      String msg = "\nNOT SAVING your changes to " + newStyleFile + "--restoring original style file " + origStyleFile;
      logger.warn(msg);
      JOptionPane.showMessageDialog(null, newStyle.getErrors()+"\n"+msg);
      // Uh oh, if the orig is also bad, we end up looping.
      restoreOriginal(newStyleFile, origStyleFile);
    }
  }

  private String personalStyle(String defaultStyle) {
    String homeDotApolloDir = System.getProperty("user.home") + "/.apollo/";
    File dotApollo = new File(homeDotApolloDir);
    String message;
    if (!dotApollo.exists()) {
      if (!dotApollo.mkdir()) {
        message = "The directory where " + styleFileName + " lives is unwriteable.\nYou don't have a " + homeDotApolloDir + ".\nTried and failed to create one.  Sorry--you will not be able to edit preferences.\nGive your sysadmin this message and see if they can fix the permissions for you.";
        logger.error(message);
        JOptionPane.showMessageDialog(null,message,"Warning",JOptionPane.WARNING_MESSAGE);
        return null;
      }
      else
        logger.info("Created " + homeDotApolloDir);
    }

    // Now change styleFileName to point to ~/.apollo
    //    String pathSeparator = apollo.util.IOUtil.isWindows() ? "\\" : "/";
    // Even on windows, it said conf/game.style, not conf\\game.style
    String pathSeparator = "/";
    String name = defaultStyle;
    if (defaultStyle.lastIndexOf(pathSeparator) > 0) {
      name = defaultStyle.substring(defaultStyle.lastIndexOf(pathSeparator)+1);
    }

    styleFileName = homeDotApolloDir + name;
    logger.debug("personalStyle: " + defaultStyle + " -> " + styleFileName);

    File personalStyle = new File(styleFileName);
    // If personal style file doesn't exist yet, create it
    if (!personalStyle.exists()) {
      String comments = "// This is your personal " + name + ".\n// All that needs to go in it is the parameters that you want to change from the default.\n// The default preferences file above shows you most of the parameters you can set.\n// Press \"Help\" for more information about the parameters.\n\n";
      try {
        apollo.util.IOUtil.writeFile(styleFileName, comments);
      } catch ( Exception ex ) {
        message = "Tried and failed to create personal style file " + styleFileName + "\nSorry--you will not be able to edit preferences.\nGive your sysadmin this message and see if they can fix the permissions for you.";
        logger.error(message);
        JOptionPane.showMessageDialog(null,message,"Warning",JOptionPane.WARNING_MESSAGE);
        return null;
      }
      logger.info("Created new personal style file " + styleFileName);
    }

    return styleFileName;
  }

  /* Notice when we've loaded a new style and load new style file. */
  public boolean handleDataLoadEvent(DataLoadEvent evt) {
    String newStyleFileName = Config.getStyle().getFileName();
    newStyleFileName = personalStyle(newStyleFileName);
    if (!(newStyleFileName.equals(styleFileName))) {
      logger.debug("PrefsDialog: style changed from " + styleFileName + " to " + newStyleFileName);
      styleFileName = newStyleFileName;
      try {
	readPrefs(styleFileName);
      } catch(Exception e) {
        logger.error(e.getMessage(), e);
      }
    }
    return true;
  }
  
  /**
   * Sets the Controller for the object
   * @param controller The new controller 
   */
  public void setController(Controller controller) {
    this.controller = controller;
    controller.addListener(this); // for regionChangeEvents
    // this is to get the window to show up in the windows menu
  }

  /**
   * Gets the Controller for the object
   * @return This PreferencesDialog's controller 
   */
  public Controller getController() {
    return controller;
  }

  /**
   * Gets the window to be controlled: This PreferencesDialog
   * @return The controlled window: this
   */
  public Object getControllerWindow() {
    return this;
  }

  /**
   * Whether controller should remove as listener on window closing
   *@return true 
   */
  public boolean needsAutoRemoval() {
    return true;
  }

}
