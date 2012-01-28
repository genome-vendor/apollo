package apollo.gui.menus;

import apollo.config.Config;
import apollo.main.Version;
import apollo.gui.*;
import apollo.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import java.io.IOException;
import java.io.File;

import org.apache.log4j.*;

public class HelpMenu extends JMenu implements ActionListener {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(HelpMenu.class);

  static String HELP_URL = "http://apollo.berkeleybop.org/current/userguide.html";
  static String BUG_URL = "http://sourceforge.net/tracker/?func=add&group_id=27707&atid=462763";
  static String UPDATE_URL = "http://apollo.berkeleybop.org/current/install.html";

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  JMenuItem about;
  JMenuItem help;
  JMenuItem bug;
  JMenuItem update;

  ApolloFrame frame;
  JDialog     aboutBox;

  public HelpMenu(ApolloFrame frame) {
    super("Help");
    this.frame = frame;
    menuInit();
  }

  public void menuInit() {
    about = new JMenuItem("About...");
    help = new JMenuItem("Apollo userguide...");
    bug = new JMenuItem("Report bug...");
    update = new JMenuItem("Check for software updates...");

    add(help);
    addSeparator();
    add(about);
    add(bug);
    add(update);

    about.addActionListener(this);

    help.addActionListener(this);
    help.setMnemonic('H');

    bug.addActionListener(this);

    help   .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H,
        ActionEvent.CTRL_MASK));

    update.addActionListener(this);
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == about) {
      showAboutBox();
    } else if (e.getSource() == help) {
      showHelpInBrowser();
    } else if (e.getSource() == bug) {
      logger.info("Loading bug tracker " + BUG_URL + " into web browser");
      HTMLUtil.loadIntoBrowser(BUG_URL);
    } else if (e.getSource() == update) {
      logger.info("Loading software update page " + UPDATE_URL + " into web browser");
      HTMLUtil.loadIntoBrowser(UPDATE_URL);
    }
  }

  public static void showHelpInBrowser() {
    String rootdir = System.getProperty("APOLLO_ROOT");
    String help = null;

    if (System.getProperty("os.name").indexOf("Windows") == 0) {
      rootdir = rootdir.replace('\\', '/');
    }
    if (!rootdir.endsWith("/")) {
      rootdir = rootdir + "/";
    }

    help = rootdir + "doc/html/userguide.html";
    showHelpInBrowser(help);
  }

  /** help is URL for userguide */
  public static void showHelpInBrowser(String help_url) {
    // Make sure requested help file exists locally
    String file = help_url;
    if (help_url.indexOf("#") > 0)
      file = file.substring(0, file.indexOf("#"));
    if (System.getProperty("os.name").indexOf("Windows") == 0) {
      file = file.replace('/', '\\');
    }
    File handle = new File(file);
    String url;
    if (handle.exists())
      url = "file://" + help_url;
    else {
      logger.info("Can't find local help file " + file + "--trying " + HELP_URL);
      url = HELP_URL;
    }

    logger.info("Loading help document " + help_url + " into web browser " + apollo.config.Config.getBrowserProgram());
    if (IOUtil.isUnix())
      logger.warn("If you're on Solaris and don't already have a web browser running, you may need to start one up yourself.  See the userguide for more info.");
    HTMLUtil.loadIntoBrowser(url);
  }

  public void showAboutBox() {
    if (aboutBox == null) {
      String    message;
      /*
      String [] orgs         = new String[3];
      String [] authors      = new String[4];
      String [] debuggers    = new String[6];
      */
      JPanel    groupPanel   = new JPanel   (new BorderLayout());
      JPanel    buttonPanel  = new JPanel   (true);

      //      ImageIcon   groupPicture = new ImageIcon(Config.getDataDir() + "images/apollo.gif",message);
      //      JLabel      groupLabel   = new JLabel   (groupPicture);
      JTextPane   messBox      = new JTextPane();
      JScrollPane messPane     = new JScrollPane(messBox);

      message = "Apollo is a collaborative open source project between ";
      String [] orgs = new String[] {
        "Berkeley Drosophila Genome Project",
        "TAIR",
        "EBI"
        };

      String [] authors = new String[] {
        "Steve Searle",
        "John Richter",
        "Suzanna Lewis",
        "Michele Clamp"
      };

      String [] debuggers = new String[] {
        "Ed Lee",
        "Nomi Harris",
        "Mark Gibson",
        "Steve Searle"
      };

      messBox.setEditable(false);
      messBox.setBackground(Color.white);

      messPane.setPreferredSize(new Dimension(620,300));
      messPane.setMinimumSize(new Dimension(620,300));

      SimpleAttributeSet attrs = new SimpleAttributeSet();

      StyleConstants.setAlignment(attrs,StyleConstants.ALIGN_CENTER);
      StyleConstants.setForeground(attrs,Color.blue);

      StyledDocument doc = messBox.getStyledDocument();
      try {
        doc.setParagraphAttributes(0,1000,attrs,false);
        StyleConstants.setForeground(attrs, new Color(0,200,20));
        StyleConstants.setBold(attrs,true);
        doc.insertString(0, Version.getVersion() + "\n\n", attrs);
        if (Config.internalMode()) {
          StyleConstants.setForeground(attrs, new Color(153,0,153));
          doc.insertString(0, "Project Internal Version\n\n", attrs);
        }
        StyleConstants.setBold(attrs,false);
        StyleConstants.setForeground(attrs,Color.blue);
        doc.insertString(doc.getLength(),message,attrs);
        for (int i=0;i<orgs.length;i++) {
          doc.insertString(doc.getLength(),"the ",attrs);
          StyleConstants.setForeground(attrs,Color.red);
          StyleConstants.setBold(attrs,true);
          doc.insertString(doc.getLength(),orgs[i],attrs);
          StyleConstants.setBold(attrs,false);
          StyleConstants.setForeground(attrs,Color.blue);
          if (i == orgs.length-2 || (i == orgs.length-1 && orgs.length == 2)) {
            doc.insertString(doc.getLength()," and ",attrs);
          } else if (i < orgs.length - 1 ) {
            doc.insertString(doc.getLength(),", ",attrs);
          } else {
            doc.insertString(doc.getLength(),".",attrs);
          }
        }
        doc.insertString(doc.getLength(),"\n\nThe first version of Apollo was written by ",attrs);
        for (int i=0;i<authors.length;i++) {
          StyleConstants.setForeground(attrs,Color.red);
          StyleConstants.setBold(attrs,true);
          doc.insertString(doc.getLength(),authors[i],attrs);
          StyleConstants.setBold(attrs,false);
          StyleConstants.setForeground(attrs,Color.blue);
          if (i == authors.length-2) {
            doc.insertString(doc.getLength()," and ",attrs);
          } else if ((i < authors.length - 1) && (authors.length >= 2)) {
            doc.insertString(doc.getLength(),", ",attrs);
          } else {
            doc.insertString(doc.getLength(),".",attrs);
          }
        }
        doc.insertString(doc.getLength(),"\n\nThis version was revised and debugged by ",attrs);
        for (int i=0;i<debuggers.length;i++) {
          StyleConstants.setForeground(attrs,Color.red);
          StyleConstants.setBold(attrs,true);
          doc.insertString(doc.getLength(),debuggers[i],attrs);
          StyleConstants.setBold(attrs,false);
          StyleConstants.setForeground(attrs,Color.blue);
          if (i == debuggers.length-2) {
            doc.insertString(doc.getLength()," and ",attrs);
          } else if (i < debuggers.length - 1 ) {
            doc.insertString(doc.getLength(),", ",attrs);
          } else {
            doc.insertString(doc.getLength(),".",attrs);
          }
        }

        doc.insertString(doc.getLength(),"\n\nAcknowledgements:\nErnest J. Friedman-Hill and Sandia National Laboratories for PSGr\n" +
            "David Goodstein for adding regular expression parsing to Find\n" +
            "Jonathan Crabtree for work on the Chado JDBC adapter\n" +
            "Chris Wilks\n" +
            "Olivier Arnaiz\n" +
            "and others who have sent patches and suggestions.\n\n" +
            "The Apollo project is supported by NIH grant 1R01GM080203-01 from NIGMS.",attrs);
        // E.L.L. Sonnhammer and R. Durbin for Blixem
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
      messBox.setCaretPosition(0);  // Scroll text box up to top (default is scrolled to bottom)

      JButton   button       = new JButton  ("OK");
      //      groupLabel.getAccessibleContext().setAccessibleName("Apollo Copyright");
      //      groupLabel.getAccessibleContext().setAccessibleDescription(message);

      aboutBox = new JDialog(frame, "About Apollo", false);
      aboutBox.getContentPane().add(groupPanel, BorderLayout.CENTER);

      //      groupPanel.add(groupLabel,  BorderLayout.WEST);
      groupPanel.add(messPane,    BorderLayout.CENTER);
      groupPanel.add(buttonPanel, BorderLayout.SOUTH);

      buttonPanel.add(button);

      button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          aboutBox.setVisible(false);
        }
      }
      );
    }
    aboutBox.pack();
    aboutBox.show();
  }

}
