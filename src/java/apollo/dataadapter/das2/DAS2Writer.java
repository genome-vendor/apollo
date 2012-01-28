package apollo.dataadapter.das2;

import java.io.*;
import java.util.Vector;
import java.util.Date;
import java.util.Hashtable;
import java.util.Enumeration;
import javax.swing.JOptionPane;

import apollo.dataadapter.*;
import apollo.datamodel.*;
import apollo.editor.UserName;
import apollo.util.DateUtil;

import org.apache.log4j.*;

/** writeXML: writes DAS2XML to a file */

public class DAS2Writer {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(DAS2Writer.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  static String TAB = "  ";
  static String DAS2_XML_VERSION = "v?";

  public static boolean writeXML(CurationSet curation, 
                                 String file_str,
                                 // Apollo version
                                 String version) {
    String filename = apollo.util.IOUtil.findFile(file_str, true);
    if (filename == null) {
      String message = "Failed to open file for writing: " + filename;
      logger.error(message);
      JOptionPane.showMessageDialog(null,
                                    message,
                                    "Warning",
                                    JOptionPane.WARNING_MESSAGE);
      return false;
    }

    FileWriter fileWriter = null;
    try {
      fileWriter = new FileWriter(file_str);
    } 
    catch ( Exception ex ) {
      logger.error("writeXML: caught exception opening " + file_str + 
                   " (" + filename + ")", ex);
      return false;
    }
    if (fileWriter == null) 
      return false;

    boolean success = writeXML(curation, fileWriter, version);

    try {
      fileWriter.close();
    }
    catch ( Exception ex ) {
      logger.error("writeXML: caught exception closing " + filename, ex);
      return false;
    }
    return success;
  }

  public static boolean writeXML(CurationSet curation,
                                 FileWriter fileWriter,
				 String version) {
    //    initConstants();
    String startingIndent = TAB;
    // Do the actual writing
    BufferedWriter bw = new BufferedWriter(fileWriter);
    PrintWriter pw = new PrintWriter(bw);
    try {
      pw.print(writeBegin(version, curation));
      //      writeGenomePosition(curation, startingIndent, pw);  // includes residues
      //      if (saveAnnots)
      //        writeAnnotations(curation, startingIndent, pw);
      //      if (saveResults)
        writeResults(curation, startingIndent, pw);
      pw.print(writeEnd());
      pw.close();
    } 
    catch ( Exception ex ) {
      logger.error("Caught exception committing XML", ex);
      return false;
    }
    return true;
  }

  /** Write the ChadoXML header */
  private static String writeBegin(String ApolloVersion, CurationSet curation) {
    StringBuffer buf = new StringBuffer();
    /** Version of XML itself, not of ChadoXML or this writer or Apollo */
    String XML_VERSION = "1.0";
    buf.append("<?xml version=\"" + XML_VERSION + "\" standalone=\"no\"?>\n");
    buf.append("<!DOCTYPE DAS2FEATURE SYSTEM \"http://www.biodas.org/dtd/das2feature.dtd\">\n");
    buf.append("<!-- DAS2XML file (" + DAS2_XML_VERSION + ") created by " + UserName.getUserName() + " -->\n");
    buf.append("<!-- " + ApolloVersion + " -->\n");
    buf.append("<!-- date=\"" + DateUtil.toGMT(new Date()) + "\" -->\n");
    buf.append("<FEATURELIST\n");
    // !! Need to get these from properties stored in curation.
    // Hardcode for now.
    buf.append(TAB + " xmlns=\"http://www.biodas.org/ns/das/2.00\"\n");
    buf.append(TAB + " xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n");
    buf.append(TAB + " xml:base=\"http://127.0.0.1:9021/das2/genome/D_melanogaster_Apr_2004/feature\" >\n");
    return buf.toString();
  }

  private static String writeEnd() {
    return "</FEATURELIST>\n";
  }

  private static void writeResults(CurationSet curation, String indent, PrintWriter pw) {
    StrandedFeatureSetI analyses = curation.getResults();
    if (analyses == null) {
      logger.debug("No results to save.");
      return;
    }
    logger.debug("Saving " + analyses.size() + " types of results");

    String chrom = curation.getChromosome();
    for (int i = 0; i < analyses.size(); i++) {
      FeatureSetI analysis = (FeatureSetI) analyses.getFeatureAt(i);
      if (analysis.getFeatureType() != null)
        writeAnalysisResults(analysis, indent, chrom, pw);
    }
  }

  /** Write out all the results of this particular analysis type */
  private static void writeAnalysisResults(FeatureSetI analysis, String indent, String chrom, PrintWriter pw) {
    String program = analysis.getProgramName(); 
    /* If we started with GFF data, 
       we won't have a program name in the SeqFeature,
       so use the type instead */
    if (program == null || 
        program.equals("") || 
        program.equals(RangeI.NO_TYPE))
      program = analysis.getProperty ("type");
    if (program.equals("") ||
        program.equals(RangeI.NO_TYPE))
      program = analysis.getTopLevelType();

    // In the GAME writer, the results can be FeatureSets, FeaturePairs, or SeqFeatures.
    // Here, they always seem to be FeatureSets, though that is presumably just a result
    // of how they're saved when they're read in.
    Vector results = analysis.getFeatures();
    logger.debug("Writing " + results.size()+1 + " results (program = " + program + ")");
    for (int i = 0; i < results.size(); i++) {
      if (results.elementAt(i) instanceof FeatureSetI) {
        FeatureSetI result = (FeatureSetI) results.elementAt (i);
        writeResult(result, indent, chrom, pw);
      } 
      else {
        logger.error ("writeAnalysisResults: don't know what to do to save non-FeatureSet class " +
                      results.elementAt(i).getClass().getName());
      }
    }
  }
  
  private static void writeResult(FeatureSetI result, String indent, String chrom, PrintWriter pw) {
    pw.print(indent + "<FEATURE id=\"" +
             result.getId() + "\" type=\"" +
             result.getTopLevelType() + "\"");
    if (!result.getProperty("created").equals(""))
      pw.print("\n" + indent + "         created=\"" + result.getProperty("created") + "\"");
    if (!result.getProperty("modified").equals(""))
      pw.print("\n" + indent + "         modified=\"" + result.getProperty("modified") + "\"");
    pw.print(" >\n");
    writeLocation(indent+TAB, result, chrom, pw);

    // Write PART records for child result spans
    Vector spans = result.getFeatures();
    for (int i = 0; i < spans.size (); i++) {
      SeqFeatureI span = (SeqFeatureI) spans.elementAt (i);
      pw.print(indent + TAB + "<PART id=\"" +
               span.getId() + "\" />\n");
    }

    // Write ALIGN
    writeAlign(indent+TAB, result, pw);

    // !! Write STYLE

    // Write properties
    writeProperties(indent+TAB, result, pw);

    // Close feature tag
    pw.print(indent + "</FEATURE>\n");

    // Write child result spans
    for (int i = 0; i < spans.size (); i++) {
      SeqFeatureI span = (SeqFeatureI) spans.elementAt (i);
      writeSpan(indent, span, result, chrom, pw);
    }
  }

  private static void writeLocation(String indent, SeqFeatureI result, String chrom, PrintWriter pw) {
    int strand = result.getStrand();
    int low;
    if (strand == -1)
      low = result.getEnd();
    else
      low = result.getStart();

    int high;
    if (strand == -1)
      high = result.getStart();
    else
      high = result.getEnd();

    pw.print(indent + "<LOC pos=\"region/" + chrom + "/" +
             low + ":" + high + ":" +
             strand + "\" />\n");
  }

  /** Write a result span */
  private static void writeSpan(String indent, SeqFeatureI span, FeatureSetI parent, String chrom, PrintWriter pw) {
    pw.print(indent + "<FEATURE id=\"" +
             span.getId() + "\" type=\"" +
             span.getFeatureType() + "\" >\n");

    writeLocation(indent+TAB, span, chrom, pw);
    pw.print(indent+TAB + "<PARENT id=\"" +
             parent.getId() + "\" />\n");

    // Write ALIGN, STYLE
    // Write properties
    writeProperties(indent+TAB, span, pw);

    pw.print(indent + "</FEATURE>\n");
  }

  public static void writeProperties(String indent, SeqFeatureI feat, PrintWriter pw) {
    Hashtable props = feat.getPropertiesMulti();
    if (props == null)
      return;

    Enumeration e = props.keys();
    while (e.hasMoreElements()) {
      String type = (String) e.nextElement();
      Vector values = feat.getPropertyMulti(type);
      if (values == null)
        continue;
          
      for (int i = 0; i < values.size(); i++) {
        String value = (String) values.elementAt(i);
        if (!isSpecialProperty(type))
          writeProperty(indent, type, value, i, pw);
      }
    }
  }

  private static void writeProperty(String indent, String prop, String value, int rank, PrintWriter pw) {
    if (value == null || value.equals(""))
      return;

    pw.print(indent + "<PROP key=\"" + prop + "\" ");
    if (value.startsWith("href:"))
      pw.print("href=\"" + value.substring("href:".length() + 1) + "\"");
    else
      pw.print("value=\"" + value + "\"");
    pw.print(" />\n");
  }

  /** Identify "special" properties that are explicitly written out elsewhere
   *  so we don't also write them out as generic properties. */
  public static boolean isSpecialProperty(String prop) {
    if (prop.equals("created"))
      return true;
    if (prop.equals("modified"))
      return true;
    if (prop.equals("subject_id"))
      return true;
    if (prop.equals("subject_range"))
      return true;

    return false;
  }

  private static void writeAlign(String indent, SeqFeatureI result, PrintWriter pw) {
    if (!(result.getProperty("subject_id").equals(""))) {
      pw.print(indent + "<ALIGN target_id=\"" + result.getProperty("subject_id") + "\"");
      if (!(result.getProperty("subject_range").equals("")))
        pw.print(" range=\"" + result.getProperty("subject_range") + "\"");
      pw.print(" />\n");
    }
  }
}
