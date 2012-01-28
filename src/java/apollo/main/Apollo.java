/**
 * I am the class that's called to start Apollo. I present the user with
 * preliminaries - splashes etc - as well as the initial data loading 
 * dialogues. Depending on the data the user chooses, I forward to 
 * the actual display components.
 **/
package apollo.main;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.editor.UserName;
import apollo.seq.io.*;

import org.apache.log4j.*;
import org.apache.log4j.spi.*;

import org.bdgp.swing.BackgroundImagePanel;
import org.bdgp.util.*;

import apollo.config.Config;
import apollo.main.Version;
import apollo.gui.*;
import apollo.gui.menus.*;
import apollo.gui.genomemap.*;
import apollo.gui.event.*;
import apollo.gui.synteny.CompositeApolloFrame;

public class Apollo {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger;
  // tracks which threads have made a call to setLog4JDiagnosticContext();
  protected static HashMap threadNames = new HashMap(20);

  // Attempt to detect whether the standard Log4J configuration has failed
  // (e.g., because the conf file in the log4j.configuration variable was not
  // found.)  If it has failed then fall back to the default configuration 
  // implemented in BasicConfigurator.
  static {
    // Check whether the root logger has an appender; there does not appear
    // to be a more direct way to check whether Log4J has already been 
    // initialized.  Note that the root logger's appender can always be
    // set to a NullAppender, so this does not restrict the utility of
    // the logging in any way.
    Logger rl = LogManager.getRootLogger();
    Enumeration appenders = rl.getAllAppenders();

    if (!appenders.hasMoreElements()) {
      System.err.println("Log4J configuration failed, using default configuration settings");
      BasicConfigurator.configure(); 
      rl.setLevel(Level.INFO);
    }
    logger = LogManager.getLogger(Apollo.class);
  }

  static String [] args;

  // -----------------------------------------------------------------------
  // Apollo
  // -----------------------------------------------------------------------

  public static void main(String[] argsStrings) {

    /*
    //always use the Java L&F - the Mac L&F doesn't behave right with buttons' background colors
    try {
      UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
    } 
    catch (UnsupportedLookAndFeelException e) {
    }
    catch (ClassNotFoundException e) {
    }
    catch (InstantiationException e) {
    }
    catch (IllegalAccessException e) {
    }
    */

    //if Apollo is launched on a Mac, set the system property to setup the correct
    //application menu name
    if (System.getProperty("os.name").startsWith("Mac OS X")) {
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Apollo");
    }
    setLog4JDiagnosticContext();

    // can throw exception
    CommandLine commandLine = CommandLine.getCommandLine();
    try {
      commandLine.setArgs(argsStrings);
    }
    catch (ApolloAdapterException e) {
      if (ApolloAdapterException.getRealException(e)!= null){
        logger.fatal("ApolloAdapterException in CommandLine.setArgs", ApolloAdapterException.getRealException(e));
      } else {
        logger.fatal("ApolloAdapterException in CommandLine.setArgs", e);
      }
      System.exit(2);
    }

    // If user was just asking for help, print the help and exit--don't
    // bother putting up a splashscreen and all that.
    if (argsStrings.length > 0 && 
        (argsStrings[0].startsWith("--help") || argsStrings[0].startsWith("-h"))) {
      commandLine.printHelp();
      System.exit(0);
    } 

    Apollo theRunner = new Apollo();

    theRunner.setArguments(argsStrings);

    boolean commandLineMode = commandLine.isInCommandLineMode();

    JWindow splashScreen=null;
    if (!commandLineMode) {
      splashScreen = displaySplashScreen();
      splashScreen.setVisible(true);
    }
    logger.debug("Config.supressMacJvmPopupMessage() = " + Config.supressMacJvmPopupMessage());
    if (apollo.util.IOUtil.isMac())
      setMacJVM(!commandLineMode && !Config.supressMacJvmPopupMessage());

    Config.initializeConfiguration();
    logger.info("this is " + Version.getVersion());

    try {
      logger.debug("main thread sleeping");
      Thread.currentThread().sleep(750);
    } catch (Exception e) {
      logger.error("exception during sleep() call" + e);
    }

    if (!commandLineMode)
      theRunner.removeSplashScreen(splashScreen);

    logger.debug("main thread invoking mainRun via SwingUtilities.invokeLater");
    SwingUtilities.invokeLater(theRunner.mainRun);
    logger.debug("main thread exiting");
    clearLog4JDiagnosticContext();
  }
  
  private void removeSplashScreen(JWindow splashScreen){
    splashScreen.setVisible(false);
    splashScreen.dispose();
  }//end disposeSplashScreen

  // Not currently used
  private String[] getArguments(){
    return args;
  }//end getArguments

  private void setArguments(String[] newValue){
    args = newValue;
  }//end setArguments

  Runnable mainRun =
    new Runnable() {
      // this thread runs in the AWT event queue
      public void run() {
        setLog4JDiagnosticContext();
        logger.trace("Apollo.mainRun started, thread = " + Thread.currentThread().getName());

        CompositeDataHolder compositeDataHolder = null;

        try {
          // can throw exception
          CommandLine commandLine = CommandLine.getCommandLine();
          commandLine.setArgs(args); //new CommandLine(args,adapterRegistry);

          // shouldnt we be going through LoadUtil?
          DataLoader loader = new DataLoader();

          if (commandLine.isBatchMode()) {
            readAndWriteViaBatchFile(loader,commandLine);
            System.exit(0);
          }

          compositeDataHolder = loader.getCompositeDataHolder(commandLine);

          if (compositeDataHolder == null) {
            logger.info("nothing loaded so quitting");
            System.exit(0);
          }

          // why is getSequence a separate call? shouldnt it just be called by getCDH?
          // loader.getSequence(); -- getcdh is calling this
          
          // if command line specifies output then write to output, dont bring up
          // gui (no-gui apollo run)
          if (commandLine.writeIsSpecified()) {
            write(loader,commandLine,compositeDataHolder);
            logger.info("exiting");
            System.exit(0);
          }

          // GUI (output not specified on command line)
          // send out DataLoadEvent post-data-load/pre-gui-load
          // initializes and loads data into CAF
          CompositeApolloFrame frame = ApolloFrame.getApolloFrame();
          //= new CompositeApolloFrame(loader.getDataAdapter(), compositeDataHolder);
          frame.loadData(loader.getDataAdapter(),compositeDataHolder);
          frame.completeGUIInitialization();
          // send out DataLoadEvent post-gui-load?
          
          //frame.addAsDataAdapterListener();

          // Added by NH, 12/15/2003
          // This was done by the now-removed method
          // initializeSingleSpeciesApolloFrame.
          // new controller - moved to AF.setController
          //Controller controller = Config.getController();
          //AnnotationChangeLog.getAnnotationChangeLog().setController(controller);
        }
        catch (ApolloAdapterException e) {
          if (ApolloAdapterException.getRealException(e)!=null){
            logger.fatal("ApolloAdapterException in Apollo.mainRun() thread", ApolloAdapterException.getRealException(e));
          } else {
            logger.fatal("ApolloAdapterException in Apollo.mainRun() thread", e);
          }
          System.exit(2);
        }
        catch (HeadlessException e) {
          // Added by MW, Aug 24 2007
          // When using Apollo with headless command-line-parameter
          // -Djava.awt.headless=true
          // This exception handler gracefully traps calls to show dialog windows
          // etc., when strictly in headless mode
        }
        catch (Exception e) { // should catch RuntimeException
          if (!Config.isHeadlessApplication()) {
            JOptionPane.showMessageDialog(
                null,
                e,
                "Warning",
                JOptionPane.WARNING_MESSAGE
            );
          }
          logger.fatal("Exception in Apollo.mainRun() thread", e);
          System.exit(2);
        }
        //        clearLog4JDiagnosticContext();
      }
  };


  private void write(DataLoader loader, CommandLine cmdLine, CompositeDataHolder cdh) 
    throws ApolloAdapterException {
    logger.info("read done; writing...");
    ApolloDataAdapterI writer = cmdLine.getWriteAdapter(); // throws adap ex
    loader.saveCompositeDataHolder(writer,cdh);
    logger.info("done writing");
  }

  /** If command line specifies a batch file listing of entries, iterate through
      the entries & read & write them out */
  private void readAndWriteViaBatchFile(DataLoader loader, CommandLine commandLine) 
    throws Exception {
    
    logger.info("loading from batchfile...");

    CompositeDataHolder compData;

    while (commandLine.hasMoreBatchEntries()) {
      commandLine.setToNextBatchEntry();

      // read
      compData = loader.getCompositeDataHolder(commandLine);
      logger.info("load done. writing...");
      // loader.getSequence() ??? --> now done in getCDH

      // write
      write(loader,commandLine,compData);
      logger.info("writing done, loading next entry in file...");
    }
    logger.info("batch file done. exiting.");
    System.exit(0);
  }

  /**
   * Sets the Log4J Nested Diagnostic Context (NDC) for a Thread.
   * Should be called as the first action in each Apollo Thread.
   */
  public static void setLog4JDiagnosticContext() {
    Thread t = Thread.currentThread();
    String tname = t.getName();
    Integer count = (Integer)threadNames.get(tname);

    // increment counter that records how many times the method has been called on this Thread
    if (count != null) {
      // not the first call; increment counter and return
      threadNames.put(tname, new Integer(count.intValue() + 1));
      return;
    } else {
      // first call
      threadNames.put(tname, new Integer(1));
    }
    
    // Log4J logging - push hostname and username onto the diagnostic stack
    // hostname of machine running Apollo
    String hostname = "unknown";
    InetAddress localhost = null;

    try { 
      localhost = InetAddress.getLocalHost(); 
      hostname = localhost.getHostName();
    } catch (java.net.UnknownHostException uhe) {
      logger.warn("UnknownHostException calling getLocalHost()", uhe);
    }

    NDC.push(hostname);
    NDC.push(UserName.getUserName());
    logger.trace("setLog4JDiagnosticContext() called in thread " + Thread.currentThread().getName(), new Throwable());
  }

  /**
   * Clears/removes the Log4J Nested Diagnostic Context (NDC) for a Thread.
   * Should be called as the last action in each Apollo Thread.
   */
  public static void clearLog4JDiagnosticContext() {
    logger.trace("clearLog4JDiagnosticContext() called in thread " + Thread.currentThread().getName(), new Throwable());

    Thread t = Thread.currentThread();
    String tname = t.getName();
    Integer count = (Integer)threadNames.get(tname);
    
    if (count == null) {
      logger.debug("clearLog4JDiagnosticContext() has no matching call to setLog4JDiagnosticContext() in thread " + tname, new Throwable());
    } else {
      // decrement counter
      int cv = count.intValue();
      threadNames.put(tname, new Integer(cv - 1));

      // don't do anything else until counter hits 1
      if (cv > 1) {
        return;
      }      
    }
    
    threadNames.remove(tname);
    // this allows the Thread to be garbage-collected
    NDC.remove();
  }
  
  public static URL getSplashURL(String apollosplash) {
//     String path = System.getProperty("java.class.path");
//     StringTokenizer st = new StringTokenizer(path, File.pathSeparator);
//     URL[] urls = new URL[st.countTokens()];
//     int count = 0;
//     while (st.hasMoreTokens()) {
//       try {
//         File nonCanonFile = new File(st.nextToken());
//         urls[count++] = toURL(nonCanonFile);

//         // try to canonicalize the filename
//         urls[count - 1] =
//           toURL(new File(nonCanonFile.getCanonicalPath()));
//       } catch (IOException ioe) {
//         // use the non-canonicalized filename
//       }

//     }
//     if (urls.length != count) {
//       URL[] tmp = new URL[count];
//       System.arraycopy(urls, 0, tmp, 0, count);
//       urls = tmp;
//     }
//     // Look around for splashscreen (why?)
//     for (int i=0;i<urls.length;i++) {
//       //	logger.debug("Class path URL = " + urls[i]);
//       try {
//         URL tmp = new URL(urls[i] + apollosplash);
//         if (new File(tmp.getFile()).exists()) {
//           return tmp;
//         }
//       } catch (Exception e) {}
//     }

    // Why all that fancy stuff?  Look where we expect to find it
    // (APOLLO_ROOT/data/apollosplash), and if it's not there, just
    // get it from the jar.
    try {
      URL splash = toURL(new File(System.getProperty("APOLLO_ROOT") + "/data/" + apollosplash));
      if (new File(splash.getFile()).exists()) {
        return splash;
      }
    } catch (Exception e) {}

    // If we can't find it, just make it from the one that's in the jar  --NH
    try {
      // Suzi put in the lines below--I'm not sure why.  The result of those
      // is 12 messages "Warning: couldn't find apollosplash.jpg anywhere"
      // when Apollo starts up, although it does find it.
      //		String splash_path = Config.findFile(apollosplash);
      //		File splash = new File(splash_path);
      logger.warn("couldn't find splash image " + apollosplash + "--retrieving from jar");
      String splashfile = apollo.util.IOUtil.findFile(apollosplash, true);
      //      File splash = new File(System.getProperty("APOLLO_ROOT") + "/data/" + apollosplash);
      File splash = new File(splashfile);
      if (!splash.exists()) {
        splash = new File(apollo.util.IOUtil.getRootDir() + "/data/" + splash.getName());
      }
      Config.ensureExists(splash, apollosplash); // gets from jar
      logger.info("created splashfile " + splashfile + " from jar");
      return(toURL(splash));
    } 
    catch (Exception e) {
      logger.error("couldn't get splash screen from jar" + e.getMessage());
    }
    return null; // couldn't find it
  }//end getSplashURL

  /**
   * converts input file's path to an absolute path and makes
   * sure the separator character is a forward slash - move to util?
  **/
  private static URL toURL(File file) throws MalformedURLException {
    String path = file.getAbsolutePath();
    if (File.separatorChar != '/') {
      path = path.replace(File.separatorChar, '/');
    }
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    if (!path.endsWith("/") && file.isDirectory()) {
      path = path + "/";
    }
    return new URL("file", "", path);
  }

  public static JWindow displaySplashScreen() {
    Dimension screenSize   = Toolkit.getDefaultToolkit().getScreenSize();
    BackgroundImagePanel backgroundImagePanel;
    // This should probably be configurable
    String splashscreen = "apollosplash.gerry.jpg";
    URL url = null;
    int       windowHeight = 240;
    int       windowWidth  = 529;
    CompoundBorder border;

    JWindow   window = new JWindow();

    Rectangle windowBounds =
      new Rectangle(
        (screenSize.width - windowWidth) / 2,
        (screenSize.height - windowHeight) /2,
        windowWidth,
        windowHeight
      );

    window.setBounds(windowBounds);

    try {
      url = getSplashURL(splashscreen);
      backgroundImagePanel = new BackgroundImagePanel(url, false);
      border =
        new CompoundBorder(
          new LineBorder(Color.black, 1),
          new BevelBorder(BevelBorder.RAISED)
        );

      backgroundImagePanel.setBorder(border);
      window.setContentPane(backgroundImagePanel);
      backgroundImagePanel.repaint();
    } catch (Exception e) {
      logger.error("error loading splashscreen " + splashscreen, e);
    }

    return window;
  }

  /** By default, applications that use .app run under JDK1.3.1 on older Macs,
   *  even if a more recent JDK is installed.  This script makes Apollo use JDK1.5+,
   *  assuming it's supported by the OS version.
  */
  private static void setMacJVM(boolean doPopups) {
    String command = System.getProperty("APOLLO_ROOT") + "/bin/mac-set-jvm";
    try {
      logger.info("running: " + command);
      Process proc = Runtime.getRuntime().exec(command);
      proc.waitFor();

      if (proc.exitValue() == 0) {
        String message = "Set Apollo to use JDK1.5+.  When you click \"Ok\", Apollo will exit\nand you will need to restart it.\n\nYou shouldn't see this message again.";
        JOptionPane pane = new JOptionPane(message);
        JDialog dialog = pane.createDialog(null, "Please Confirm");
        dialog.show();
        System.exit(0);
      }
      else if (proc.exitValue() == 1) {
        logger.debug("JVM setting ok--Info.plist not changed.");
        return;
      }
      
      // If mac-set-jvm was not happy, then report what it printed out.
      InputStream errors = proc.getErrorStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(errors));
      String line;
      while ((line = reader.readLine()) != null)
        logger.debug("message from mac-set-jvm: " + line);

      if (proc.exitValue() == 2) {
        String message = "Since you are not running an installed version of Apollo, I can't change\nwhich JVM it uses.  Hopefully you have JDK1.5 or 1.6 set up as your default JVM.\n";
        //        if (doPopups)
        //          JOptionPane.showMessageDialog(null,message, "Warning", JOptionPane.WARNING_MESSAGE);
        logger.warn(message);
      }
      else if (proc.exitValue() == 3) {
        String message = "You are running an old version of Mac OS X that does not support JDK1.4.\nYou probably will not be able to run Apollo.\nWe recommend upgrading your Mac operating system.\n";
        if (doPopups)
          JOptionPane.showMessageDialog(null,message, "Warning", JOptionPane.WARNING_MESSAGE);
        logger.warn(message);
      }

      else {
        String message = "You may be running incompatible versions of Mac OS X and the JDK.\nRunning Apollo with this configuration could crash your Mac.\nWe recommend updating to JDK1.5 or 1.6 before trying to run Apollo.\nSee http://www.apple.com/macosx/upgrade/softwareupdates.html for instructions.\n";
        if (doPopups)
          JOptionPane.showMessageDialog(null,message, "Warning", JOptionPane.WARNING_MESSAGE);
        logger.warn(message);
      }
    } catch (Exception e) {
      logger.error("exception running " + command, e);
    }
  }
}



  /* This is not used anymore - delete? and what replaced it? I think DataAdapter
      Chooser does this automatically? */
//   private void writeAdapterHistory(
//     Vector history,
//     String region,
//     ApolloDataAdapterI adapter,
//     String filename
//   ){
                                   
//     if (Config.getAdapterHistoryFile() == null) {
//       return;
//     }//end if
    
//     try {
//       Properties adapterProperties = adapter.getStateInformation();
//       adapterProperties.put("region", region);
//       adapterProperties.put("adapter",adapter.getClass().getName());
//       MultiProperties allProperties = new MultiProperties();
//       int openHistoryLength = 0;
//       if (filename == null) {
//         logger.warn("writeAdapterHistory: warning--no filename.");
//         return;
//       }

//       // Read old history from history file, if it exists
//       File in = new File(filename);
//       if (in.exists()) {
//         allProperties.load(new FileInputStream(in));
//         String openHistoryLengthStr =
//           allProperties.getProperty("openHistoryLength");
//         if (openHistoryLengthStr != null) {
//           try {
//             openHistoryLength =
//               Integer.parseInt(openHistoryLengthStr);
//           } catch (NumberFormatException e) {
//             if (openHistoryLengthStr != null) {
//               logger.error("Can't parse " + openHistoryLengthStr);
//             }
//           }
//         }
//       }

//       history = new Vector();
//       String regionb = adapterProperties.
//                        getProperty("region");
//       String adapterb = adapterProperties.
//                         getProperty("adapter");
//       for(int i=0; i < openHistoryLength; i++) {
//         Properties p = allProperties.
//                        getProperties("openHistory"+i);
//         if (p != null) {
//           String regiona = p.getProperty("region");
//           String adaptera = p.getProperty("adapter");
//           if (!(regiona.equals(regionb) &&
//                 adaptera.equals(adapterb)))
//             history.addElement(p);
//         }
//       }
//       history.insertElementAt(adapterProperties,0);
//       int i;
//       for(i=0; i < history.size() &&
//           i < Config.getMaxHistoryLength(); i++) {
//         allProperties.setProperties("openHistory"+i,
//                                     (Properties) history.elementAt(i));
//       }
//       allProperties.setProperty("openHistoryLength",i+"");
//       allProperties.store(new FileOutputStream(new File(filename)),
//                           "Apollo data adapter history file");
//     } catch (Exception e) {
//       logger.error("Couldn't store state information for "+
//                    "data adapter "+adapter, e);
//     }
//   }//end writeAdapterHistory
