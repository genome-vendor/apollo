package apollo.util;

import java.util.*;
import java.io.IOException;
import java.io.*;
import java.lang.String;
//import java.net.*;
import javax.swing.JOptionPane;
import java.net.URL;
import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;

import edu.stanford.ejalbert.*;

import apollo.config.Config;

import org.apache.log4j.*;

public class HTMLUtil {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(HTMLUtil.class);

  private HTMLUtil() { }

  public static String removeHtml(String line) {
    StringBuffer output = new StringBuffer();
    int i=0;
    String curChar;
    if (line.length() == 0)
      return line;
    curChar = line.substring(i,i+1);

    while (i<line.length()) {
      if (curChar.equals("<") && isHtmlToken(line,i)) {
        i = skipAngled(line,i);
      } else {
        logger.trace("added to output: " + curChar);
        output.append(curChar);
        i++;
      }
      logger.trace("getting next char");
      if (i < line.length()) {
        curChar = line.substring(i,i+1);
      }
    }
    logger.debug("returning from removeHtml");
    return output.toString();
  }

  private static boolean isHtmlToken(String line, int pos) {
    if (line.indexOf("<A ",pos) == pos) {
      return true;
    }
    if (line.indexOf("</A>",pos) == pos) {
      return true;
    }
    if (line.indexOf("<pre>",pos) == pos) {
      return true;
    }
    if (line.indexOf("<PRE>",pos) == pos) {
      return true;
    }
    if (line.indexOf("<br>",pos) == pos) {
      return true;
    }
    if (line.indexOf("<BR>",pos) == pos) {
      return true;
    }
    if (line.indexOf("</br>",pos) == pos) {
      return true;
    }
    if (line.indexOf("</BR>",pos) == pos) {
      return true;
    }
    if (line.indexOf("</pre>",pos) == pos) {
      return true;
    }
    if (line.indexOf("</PRE>",pos) == pos) {
      return true;
    }
    return false;
  }
  private static int skipAngled(String line, int start) {
    logger.debug("skipAngled called at " + start);
    int pos = start+1;
    String curChar = line.substring(pos,pos+1);
    while (pos<line.length() && !curChar.equals(">")) {
      logger.trace("skipping " + curChar);
      if (curChar.equals("<") && isHtmlToken(line,pos)) {
        pos = skipAngled(line,pos);
      } else {
        pos++;
      }
      curChar = line.substring(pos,pos+1);
    }
    logger.debug("return from skipAngled");
    return ++pos;
  }

  public static void loadIntoBrowser(String url) {
    logger.debug("URL string = " + url);
    String browser = apollo.config.Config.getBrowserProgram();
    try {
      if (apollo.util.IOUtil.isWebStart()) {
        logger.debug("loadIntoBrowser--calling BasicService.showDocument on " + url);
        BasicService bs = (BasicService)ServiceManager.lookup("javax.jnlp.BasicService");
        if (bs == null || !bs.isWebBrowserSupported()) {
          JOptionPane.showMessageDialog(null, "Can't show " + url + "\nno web browser supported by your JNLP client");
          return;
        }
        bs.showDocument(new URL(url));
      }
      else {
        if (browser!=null)
          BrowserLauncher.setBrowser(browser);
        BrowserLauncher.openURL(url);
      }
    } catch (Exception err) {
      logger.error("error showing url " + url + " " + err, err);
      String m = "Error opening browser " + browser + ". BrowserProgram can be specified in config file (" + Config.getFileName() + ")";
      JOptionPane.showMessageDialog(null,m);
    }
  }

  public static String replaceSGMLWithGreekLetter (String sgml) {
    int greek_begin = (sgml != null ? sgml.indexOf ("&") : -1);
    if (greek_begin >= 0 && sgml.indexOf ("gr") > greek_begin) {
      String greek = (sgml.indexOf ("gr;") > 0) ? "gr;" : "gr";
      String english = greek;
      int greek_end = sgml.indexOf (greek) + greek.length();
      String prefix = (greek_begin > 0 ?
                       sgml.substring (0, greek_begin) :
                       "");
      String suffix = (greek_end < sgml.length() ?
                       sgml.substring (greek_end) :
                       "" );
      greek = sgml.substring (greek_begin, greek_end);

      if (greek.startsWith ("&agr"))
        english = "alpha";
      else if (greek.startsWith ("&Agr"))
        english = "Alpha";
      else if (greek.startsWith ("&bgr"))
        english = "beta";
      else if (greek.startsWith ("&Bgr"))
        english = "Beta";
      else if (greek.startsWith ("&ggr"))
        english = "gamma";
      else if (greek.startsWith ("&Ggr"))
        english = "Gamma";
      else if (greek.startsWith ("&dgr"))
        english = "delta";
      else if (greek.startsWith ("&Dgr"))
        english = "Delta";
      else if (greek.startsWith ("&egr"))
        english = "epsilon";
      else if (greek.startsWith ("&Egr"))
        english = "Epsilon";
      else if (greek.startsWith ("&zgr"))
        english = "zeta";
      else if (greek.startsWith ("&Zgr"))
        english = "Zeta";
      else if (greek.startsWith ("&eegr"))
        english = "eta";
      else if (greek.startsWith ("&EEgr"))
        english = "Eta";
      else if (greek.startsWith ("&thgr"))
        english = "theta";
      else if (greek.startsWith ("&THgr"))
        english = "Theta";
      else if (greek.startsWith ("&igr"))
        english = "iota";
      else if (greek.startsWith ("&Igr"))
        english = "Iota";
      else if (greek.startsWith ("&kgr"))
        english = "kappa";
      else if (greek.startsWith ("&Kgr"))
        english = "Kappa";
      else if (greek.startsWith ("&lgr"))
        english = "lambda";
      else if (greek.startsWith ("&Lgr"))
        english = "Lambda";
      else if (greek.startsWith ("&mgr"))
        english = "mu";
      else if (greek.startsWith ("&Mgr"))
        english = "Mu";
      else if (greek.startsWith ("&ngr"))
        english = "nu";
      else if (greek.startsWith ("&Ngr"))
        english = "Nu";
      else if (greek.startsWith ("&xgr"))
        english = "xi";
      else if (greek.startsWith ("&Xgr"))
        english = "Xi";
      else if (greek.startsWith ("&ogr"))
        english = "omicron";
      else if (greek.startsWith ("&Ogr"))
        english = "Omicron";
      else if (greek.startsWith ("&pgr"))
        english = "pi";
      else if (greek.startsWith ("&Pgr"))
        english = "Pi";
      else if (greek.startsWith ("&rgr"))
        english = "rho";
      else if (greek.startsWith ("&Rgr"))
        english = "Rho";
      else if (greek.startsWith ("&sgr"))
        english = "sigma";
      else if (greek.startsWith ("&Sgr"))
        english = "Sigma";
      else if (greek.startsWith ("&sfgr"))
        english = "sigma";
      else if (greek.startsWith ("&tgr"))
        english = "tau";
      else if (greek.startsWith ("&Tgr"))
        english = "Tau";
      else if (greek.startsWith ("&ugr"))
        english = "upsilon";
      else if (greek.startsWith ("&Ugr"))
        english = "Upsilon";
      else if (greek.startsWith ("&phgr"))
        english = "phi";
      else if (greek.startsWith ("&PHgr"))
        english = "Phi";
      else if (greek.startsWith ("&khgr"))
        english = "chi";
      else if (greek.startsWith ("&KHgr"))
        english = "Chi";
      else if (greek.startsWith ("&psgr"))
        english = "psi";
      else if (greek.startsWith ("&PSgr"))
        english = "Psi";
      else if (greek.startsWith ("&ohgr"))
        english = "omega";
      else if (greek.startsWith ("&OHgr"))
        english = "Omega";
      else
        logger.error("unable to translate " + greek);
      return (prefix + english + suffix);
    } else {
      // not SGML, just a name or id
      return sgml;
    }
  }

}
