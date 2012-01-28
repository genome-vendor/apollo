/* GeneSeen was written by Nomi Harris and Suzanna Lewis, Berkeley Drosophila Genome Project.
   Copyright (c) 2000 Berkeley Drosophila Genome Center, UC Berkeley. */

package org.bdgp.xml;

import org.xml.sax.*;

//////////////////////////////////////////////////////////////////////
    // Utility routines.
    //////////////////////////////////////////////////////////////////////

public class XML_util
{
  // transform content String to replace 
  //  "<" with "&lt;", ">" with "&gt;", "&" with "&amp;"
  public static String transformToPCData(String str) {
    return transformToPCData(str, false);
  }

  // may want to extend to take a flag for transforming for attributes, in 
  //     which case "&apos;" can replace ' and "&quot;" can replace ", 
  //     to allow attributes to contain single and double quotes
  // exnended for optional single and double quote transform -- GAH 5-19-98
  public static String transformToPCData(String str, boolean transQuotes) {
    if (str == null)
      return null;

    if (str.equals(""))
      return "";

    StringBuffer buf = new StringBuffer();
    int last_char = str.length() - 1;
    char cur_char;
    for (int i=0; i<=last_char; i++) {
      cur_char = str.charAt(i);
      switch (cur_char) {
      case '&':
          buf.append("&amp;");
	break;
      case '<':
	buf.append("&lt;");
	//	  System.err.println("filtering out <");
	break;
      case '>':
	buf.append("&gt;");
	break;
      case '\'':
	if (transQuotes)  { buf.append("&apos;"); }
	else  { buf.append(cur_char); }
	break;
      case '"':
	if (transQuotes)  { buf.append("&quot;"); }
	else  { buf.append(cur_char); }
	break;
      default:
	buf.append(cur_char);
	break;
      }
    }
    return buf.toString();
  }

  public static String filterWhiteSpace(char ch[], int start, int length) {
    int end = start + length - 1;
    StringBuffer buf = new StringBuffer();
    char cur_char;
    int index = start;
    while (index <= end) {
      cur_char = ch[index];
      switch (cur_char) {
      case '\n':
      case '\r':
      case '\t':
      case '\f':
      case ' ':
	break;
      default:
	buf.append(cur_char);
	break;
      }
      index++;
    }
    return buf.toString();
  }

  /**
   *  takes a start position and length within a character array, and
   *  returns a String corresponding to this range but with all white
   *  space before the first non-whitespace character and all whitespace
   *  after the last non-whitespace character removed
   *  GAH  5-16-98
   */
  public static String trimWhiteSpace(char ch[], int start, int length) {
    int end = start + length - 1;
  LEFT_TRIM_LOOP:
    while (start <= end) {
      char cur_char = ch[start];
      switch (cur_char) {
      case '\n':
      case '\r':
      case '\t':
      case '\f':
      case ' ':
	start++;
	break;
      default:
	break LEFT_TRIM_LOOP;
      }
    }
  RIGHT_TRIM_LOOP:
    while(start <= end) {
      char cur_char = ch[end];
      switch (cur_char) {
      case '\n':
      case '\r':
      case '\t':
      case '\f':
      case ' ':
	end--;
	break;
      default:
	break RIGHT_TRIM_LOOP;
      }
    }
    if (start > end) { return ""; }
    length = end - start + 1;
    return new String(ch, start, length);
  }

  //
  // Escape special characters for display.
  //
  public static String escapeCharacters(char ch[], int start, int length)
    {
      StringBuffer out = new StringBuffer();

      for (int i = start; i < start+length; i++) {
	if (ch[i] >= 0x20 && ch[i] < 0x7f) {
	  out.append(ch[i]);
	} else {
	  out.append("&#" + (int)ch[i] + ';');
	}
      }
      return out.toString();
    }

}
