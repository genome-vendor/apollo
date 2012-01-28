package apollo.util;

import java.io.*;
import javax.swing.JOptionPane;
import java.net.URL;

import apollo.dataadapter.ApolloAdapterException;

import org.apache.log4j.*;

/** I can't believe these basic IO utilities didn't already exist somewhere,
 *  but I didn't see them.
 *  Methods to read a text file, write a text file, and copy a text file.
 *  readFile and writeFile throw IOExceptions.  copyFile returns true or false
 *  depending on whether it succeeded in copying the file.
 *  4/2006: Moved getStreamFromUrl here (from GAMEAdapter) so that ChadoXMLAdapter
 *  can use it too.
 */

public class IOUtil {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(IOUtil.class);
  private static String            rootdir;

  /** Apollo root directory is defined by system property APOLLO_ROOT; if that's
   *  not defined, try . (see if there's a conf directory in .). 
   *  Failing that, use ~/.apollo
   *  NOTE that this could be problematic if we're running in webstart mode
   *  and the user has just a few things in their config (e.g. FrameOrientation "horizontal")
   *  but not everything--we'll say "oh, cool, we found apollo.cfg,", and not realize
   *  that it's not really the whole thing and we need to unjar the default apollo.cfg.
   *  Maybe we need an "are we in webstart?" test.  */
  public static String getRootDir() {
    if (rootdir == null) {
      rootdir = System.getProperty("APOLLO_ROOT");
      if (rootdir == null) {
        //check if it's running in web start mode
        if (apollo.config.Config.isJavaWebStartApplication()) {
          rootdir = System.getProperty("user.home") + "/apollo-webstart";
        }
        /*
        else {
          // See if . has a conf directory, suggesting that . is an apollo directory
          rootdir = ".";
          String tmp = rootdir + "/conf";
          File handle = new File(tmp);
          if (!handle.exists()) {
            String home = System.getProperty("user.home");
            if (home == null)
              home = "/tmp";  // last resort
            rootdir =  home + "/apollo-webstart";
          }
        }
        */
      }
      logger.info("APOLLO_ROOT: " + rootdir);
    }
    return rootdir;
  }

  /** Any IO errors will be thrown back to the caller to catch */
  public static String readFile(String fileName) 
    throws IOException {
    String text = "";
    BufferedReader in;

    in = new BufferedReader(new FileReader(fileName));

    /** 6/06: It would be faster to use a StringBuffer and then convert to a String
        at the end, but since at the moment this is only used to read smallish style files
        it doesn't matter much. */
    String line = "";
    while ((line = in.readLine()) != null)
      text = text + line + "\n";

    in.close();
    return text;
  }

  /** Any IO errors will be thrown back to the caller to catch */
  public static void writeFile(String fileName, String text)
    throws IOException {
    PrintWriter outStream;
    outStream = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
    outStream.write(text, 0, text.length());
    outStream.close();
    logger.debug("wrote " + text.length() + " bytes to " + fileName);
  }

  public static boolean copyFile(String from, String to) {
    String text;
    try {
      text = readFile(from);
    } catch ( Exception ex ) {
      logger.error("Caught exception opening " + from + " for reading", ex);
      return false;
    }
    try {
      writeFile(to, text);
    } catch ( Exception ex ) {
      logger.error("Caught exception writing to " + to, ex);
      return false;
    }
    return true;
  }

  public static String stripControlChars(String s) {
    StringBuffer stripped = new StringBuffer();
    for (int i=0; i<s.length(); i++) {
      char c = s.charAt(i);
      if (!Character.isISOControl(c)) 
        stripped.append(c);
    }
    return new String(stripped);
  }

  /** Look in various sensible places for the file with a given name (which may be a full path,
   *  a relative path, or just a filename). */
  public static String findFile(String name) {
    return findFile(name, false);
  }

  public static String findFile(String name, boolean canCreate) {
    if (name == null)
      return null;

    name = name.trim(); // take out leading & trailing whitespace

    /* also check for empty string because some of the
       panel interfaces use the empty string in the text field */
    if (name == null || name.equals(""))
      return null;

    name = expandSquiggle(name);
    
    // Maybe the given filename/path is already ok
    File handle = new File(name);
    if (handle.exists()) {
      logger.debug("findFile: orig name is ok: " + name);
      return name;  // already ok
    }

    // If it's already an absolute path,
    // and we're not allowed to create a new file,
    // then return null--we failed to find it.
    if (handle.isAbsolute() && !canCreate) {
      logger.debug("findFile: handle is absolute, can't find file, not allowed to create: " + name);
      return null;
    }

    if (canCreate) {
      // Check that path doesn't use a nonexistent directory
      String dirstring = "";
      if (name.lastIndexOf("/") < 0 &&
          name.lastIndexOf("\\") < 0) {
        // This test shouldn't really be needed because the
        // definition of absolute is that (on Unix) the name
        // begins with a '/'
        if (handle.isAbsolute()) {
          logger.warn ("No directory separator in absolute file name " + name);
          return null;
        }
      }
      else {
        if (name.lastIndexOf("/") >= 0)
          dirstring = name.substring(0, name.lastIndexOf("/"));
        else if (name.lastIndexOf("\\") >= 0)  // Windows--but sometimes it's / on Windows
          dirstring = name.substring(0, name.lastIndexOf("\\"));
      }

      if (dirstring.equals("")) {
        dirstring = ".";
        name = dirstring + "/" + name;
      }

      File dir = new File(dirstring);
      if (dir.canRead() && dir.canWrite()) {
        logger.debug("findFile: can write to dir " + dirstring + "--returning " + name);
	return name;
      }
      // It's an absolute handle and we can't read and/or write to it, so we lose.
      else if (handle.isAbsolute()) {
        logger.error("findFile: can't write to directory " + dir); // DEL
        return null;
      }
      logger.error("findFile: can't write to dir " + dirstring);
      // Otherwise, we have a relative path.
      // Ok, we failed to find a valid directory under ., but
      // don't give up yet--we might find it under APOLLO_ROOT.
    }

    // We still haven't found what we're looking for.
    // Try user's .apollo directory.
    String homeDotApolloDir = System.getProperty("user.home") + "/.apollo/";
    String path = homeDotApolloDir + name;
    handle = new File(path);
    if (handle.exists()) {
      logger.debug("Found " + name + " in " + homeDotApolloDir);
      return path;
    }

    // these are debug messages because we don't necessarily expect to find
    // the file in the first place we look
    logger.debug("findFile didn't find " + name + " in .apollo", new Throwable());

    // If name was a relative path, try looking under APOLLO_ROOT/conf
    path = getRootDir() + "/conf/" + name;
    handle = new File(path);
    if (handle.exists()) {
      logger.debug("findFile: found " + path + " under rootDir/conf");
      return path;
    }

    logger.debug("findFile: didn't find " + name + " in conf");

    // Try looking directly under APOLLO_ROOT (getRootDir())
    path = getRootDir() + "/" + name;
    handle = new File(path);
    if (handle.exists()) {
      logger.debug("findFile: found " + path + " under APOLLO_ROOT");
      return path;
    }

    // Adding looking under APOLLO_ROOT/obo-files for ontology files
    path = getRootDir() + "/obo-files/" + name;
    handle = new File(path);
    if (handle.exists()) {
      logger.debug("findFile: found " + path + " under APOLLO_ROOT");
      return path;
    }    
    
    // If we're allowed to create a new file, this is where it should go
    if (canCreate) {
      if (canWriteToDirectory(path)) {
        logger.debug("findFile: can write to dir for path " + path + "--returning that as place to create " + name);
        return path;
      }
      logger.debug("findFile: can't write to dir for path " + path);
    }
    logger.debug("findFile: didn't find " + name + " in " + path);

    // Last thing to try: rootdir/data
    path = getRootDir() + "/data/" + name;
    handle = new File(path);
    if (handle.exists()) {
      logger.debug("findFile: found " + path + " under APOLLO_ROOT/data");
      return path;
    }

    logger.debug("findFile: didn't find " + name + " in " + path);

    // We failed
    if (canCreate)
      logger.error("Warning: couldn't find or create file " + name + " anywhere");
    //    else
    //      logger.error("Warning: couldn't find file " + name + " anywhere");

    return null;
  }

  public static String expandSquiggle(String name) {
    if (name.startsWith("~/")) {  // Unix abbreviation for home dir
      name = name.substring(2);
      name = System.getProperty("user.home") + "/" + name;
    }
    else if (name.startsWith("~")) {  // e.g. ~joe/
      String users = System.getProperty("user.home");
      users = users.substring(0, users.lastIndexOf("/"));
      name = users + "/" + name.substring(1);
    }
    return name;
  }

  /** Check whether specified directory is writeable */
  public static boolean canWriteToDirectory(String path) {
      String dirstring = "";
      if (path.lastIndexOf("/") < 0 &&
          path.lastIndexOf("\\") < 0)
        dirstring = ".";
      else {
        if (path.lastIndexOf("/") >= 0)
          dirstring = path.substring(0, path.lastIndexOf("/"));
        else if (path.lastIndexOf("\\") >= 0)
          dirstring = path.substring(0, path.lastIndexOf("\\"));
      }

      File dir = new File(dirstring);
      if (dir.canRead() && dir.canWrite())
        return true;
      else
        return false;
  }

  public static boolean isWindows() {
    String osname = System.getProperty("os.name");
    if (osname.startsWith("Windows"))
      return true;
    else
      return false;
  }
  
  public static boolean isMac() {
    String osname = System.getProperty("os.name");
    if (osname.startsWith("Mac"))
      return true;
    else
      return false;
  }

  public static boolean isUnix() {
    String osname = System.getProperty("os.name");
    // Any other cases??  What about Alphas?
    if (osname.startsWith("Linux") || osname.startsWith("Solaris") || osname.startsWith("Sun") || osname.startsWith("Mac OS X") ||
	osname.indexOf("ix") > 0)
      return true;
    else
      return false;
  }

  /** Try to determine whether we're running as a webstart application */
  public static boolean isWebStart() {
    if (getRootDir().indexOf("webstart") > 0)
      return true;
    else
      return false;
  }

  /** Put up a dialog box if we're not running in batch mode or in headless mode.
   *  If we are, then just print the message to stdout. */
  public static void errorDialog(String msg) {
    if (!apollo.main.CommandLine.isInCommandLineMode() &&
    		!apollo.config.Config.isHeadlessApplication())
      JOptionPane.showMessageDialog(null,msg,"Warning",JOptionPane.WARNING_MESSAGE);
    logger.warn(msg);
  }

  /** Display an information dialog box (if not running in headless mode or command line
   *  mode and outputs information to logger).
   *  
   * @param msg - Message to be displayed
   */
  public static void informationDialog(String msg) {
    if (!apollo.main.CommandLine.isInCommandLineMode() &&
        !apollo.config.Config.isHeadlessApplication())
      JOptionPane.showMessageDialog(null, msg, "Information", JOptionPane.INFORMATION_MESSAGE);
    logger.info(msg);
  }
  
  /** Makes InputStream from URL - throws DataAdapterException with
    * badUrlMessage if URL is not found, or with notFoundMessage if stream is empty
    * (i.e., requested region is not found). */
  public static InputStream getStreamFromUrl(URL url, String badUrlMessage, String notFoundMessage)
    throws ApolloAdapterException {

    InputStream stream=null;
    try {
      logger.info("Trying to open URL " + url);
      stream = url.openStream();
      logger.debug("Succesfully opened URL "+url);
    } catch (IOException e) {
      ApolloAdapterException aae = new ApolloAdapterException(badUrlMessage);
      logger.error(badUrlMessage, aae);
      stream = null;
      throw aae;
    }
    // Make sure the stream isn't empty--if it is, print the notFoundMessage
    try {      // available() can throw an exception
      // Give it some time to respond--sometimes the stream says
      // nothing is available when it's first opened.
      int tries = 1500;
      do {
        Thread.sleep(10);  // 10 ms
        tries--;
      } while (stream.available() <= 1 && tries > 0);
      
      //      logger.debug("Tried " + (1500-tries)+"" + " times"); // DEL
      // If there's still nothing, then it must have returned an empty page
      // (requested region not found).  (Or maybe the server's just really slow.)
      if (stream.available() <= 1) {
        ApolloAdapterException aae = new ApolloAdapterException(notFoundMessage);
        logger.error(notFoundMessage, aae);
        stream = null;
        throw aae;
      }
    } catch (Exception e) {
        ApolloAdapterException aae = new ApolloAdapterException(notFoundMessage);
        logger.error(notFoundMessage, aae);
        stream = null;
        throw aae;
    }
    return stream;
  }

  /* If notFoundMessage is not supplied, make and pass along a generic one. */
  public static InputStream getStreamFromUrl(URL url, String badUrlMessage) 
    throws ApolloAdapterException {
    return getStreamFromUrl(url, badUrlMessage, "Requested region not found");
  }

}
