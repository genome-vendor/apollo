package apollo.dataadapter.genbank;

/**
 * GenbankFormatter.java
 *
 *
 * Created: Mon Sep  1 18:32:20 2003
 *
 * @author <a href="mailto:shiranp@bcm.tmc.edu">Shiran Pasternak</a>
 * @author <a href="mailto:rlozado@bcm.tmc.edu">Ryan Lozado</a>
 * @version 1.0
 * $Header: /cvsroot/gmod/apollo/src/java/apollo/dataadapter/genbank/GenbankFormatter.java,v 1.8 2004/06/23 00:00:01 nomi Exp $
 */
public class GenbankFormatter {
  private static final int LINE_LENGTH = 80;
  /* if a space occurs within SPACE_TOLERANCE of the end of
   * the line, split the line at the space.
   */
  private static final int SPACE_TOLERANCE = 20;
  private static final int HEAD_INDENT = 12;
  private static final String SUBHEAD_PAD = "  ";
  private static final int FEATURE_INDENT = 21;
  private static final String SUBFEATURE_PAD = "     ";
  private static final int SEQ_LINE_LENGTH = 60;

  private static String indentString(String str, int indent) {
    if (str == null)
      str = "";

    StringBuffer b = new StringBuffer(str);
    for (int i = str.length(); i < indent; i++) {
      b.append(" ");
    }
    return b.toString();
  }

  private static String getInsert(int indent) {
    StringBuffer b = new StringBuffer("\n");
    for (int i = 0; i < indent; i++) {
      b.append(" ");
    }
    return b.toString();
  }

  /** Pad with spaces between str1 and str2 to attain total width */
  public static String padBetweenWithSpaces(String str1, String str2, int width) {
    if (str1.length() + str2.length() >= width)
      return str1+str2;  // truncate?

    StringBuffer b = new StringBuffer(str1);
    for (int i = 0; i < width-(str1.length()+str2.length()); i++)
      b.append(" ");
    b.append(str2);
    return b.toString();
  }

  /**
   * Formats a string so that it does not run beyond a specified line length.
   * if the string is longer than that, it is split up, and proper indentations
   * are inserted to maintain the expected format.
   *
   * @return if longer than the line length, returns a multi-line, formatted string
   */
  public static String breakSingle(String str) {
    return breakSingle(str, FEATURE_INDENT);
  }

  public static String breakSingle(String str, int indent) {
    if (str.length() <= LINE_LENGTH) {
      return str;
    }
    StringBuffer b = new StringBuffer(str);
    String insert = getInsert(indent);
    int idx = getSplitIndex(b, 0);
    do {
      // System.out.println("["+idx+":"+b.length()+"]\n"+b);
      b.insert(idx, insert);
      idx = getSplitIndex(b, idx);
    } while (idx < b.length());
	
    return b.toString();
  }

  /* Determine exactly where to split the line. if a space is found
   * within a tolerance of the expected line length, then at that space.
   * Otherwise, the split is done at the line length.
   */
  private static int getSplitIndex(StringBuffer b, int last) {
    int idx = 0;
    int nextSpace = -1;
    try {
      nextSpace =
        b.substring(1, last+LINE_LENGTH).lastIndexOf(' ');
    } catch (IndexOutOfBoundsException e) {
    }
    if (nextSpace > last + LINE_LENGTH - SPACE_TOLERANCE && 
        nextSpace < last + LINE_LENGTH) {
      idx = nextSpace + 1;
      b.deleteCharAt(idx);
    } else {
      idx = last == 0 ? LINE_LENGTH : last + LINE_LENGTH + 1;
    }
    return idx;
  }

  public static String getHeading(String heading) {
    return indentString(heading, HEAD_INDENT);
  }

  public static String getSubHeading(String sub) {
    return indentString(SUBHEAD_PAD+(sub==null?"":sub), HEAD_INDENT);
  }

  public static String getFeature(String feature) {
    return indentString(feature, FEATURE_INDENT);
  }

  public static String getFeatureHeading(String sub, String info) {
    return indentString(SUBFEATURE_PAD+sub, FEATURE_INDENT) + info;
  }
    
  public static String getFeatureItem(String name, String value) {
    if (value == null)
      value = "";  // Should we just return empty string if we don't have a value?
    return breakSingle(indentString("", FEATURE_INDENT) +
                       "/"+name+"=\""+value+"\"", FEATURE_INDENT);
  }

  /** Return GenBank-formatted sequence, e.g.
        1 GCTCCTAGGC ATCTGCCTAG TCACCCAAAT CATCACAGGC CTTCTCCTAG CTATGCACTA CACAGCAGAC
       61 CACAGCAGAC ACCTCCCTAG CCTTCACCTC CGTAGCCCAC ACCTGCCGAA ACGTCCAATT CGGCTGACTC
  */
  public static String formatSequence(String residues) {
    if (residues == null || residues.equals(""))
      return "";

    try {
      StringBuffer b = new StringBuffer();

      int LABEL_LENGTH = 9;
      int numRes = residues.length();
      for (int resNum = 1; resNum < numRes; resNum += SEQ_LINE_LENGTH) {
        String label = resNum + "";
        while (label.length() < LABEL_LENGTH)
          label = " " + label;
        String line = label;
        // Want to print 1-based labels, but sequence is of course 0-based
        for (int i = resNum-1; i < resNum+SEQ_LINE_LENGTH-1 && i < numRes; i += 10)
          line += " " + residues.substring(i, Math.min(i+10,numRes));
        if (resNum+SEQ_LINE_LENGTH < numRes)
          b.append(line + "\n");
        else
          b.append(line);
      }
      return b.toString();
    }
    catch (Exception e) {
      System.out.println("formatSequence: caught error " + e);
      return "";
    }
  }

} // GenbankFormatter
