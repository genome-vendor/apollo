package apollo.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import apollo.datamodel.GenomicRange;

//import org.bdgp.util.*;
// this class functions migrated to dataadapter.Region object - delete?
// if delete wont be able to name another class SequenceUtil because of cvs
// limitation

public class SequenceUtil {

// this is now in the Region class
//   /** I think this should be incorporated into apollo.dataadapter.Region class
//       that does this kind of stuff...
//       Parses chrom loc strings with spaces. eg: "Chr 2L 10000 20000" and makes
//     a GenomicRange from chrom start and end */
//   public static GenomicRange parseChrStartEndString(String str) {
//     //if ( str==null || str.equals("") )  return null;
//     nullCheck(str); // throws runtime exception

//     StringTokenizer tokenizer = new StringTokenizer(str); // space default delim
//     if (!tokenizer.nextToken().equals("Chr")) {

//       throw new RuntimeException("Failed parsing location string " + str);
//     }
//     String chr = tokenizer.nextToken();
//     int start, end;
//     start = stringToInt(tokenizer.nextToken());
//     end = stringToInt(tokenizer.nextToken());
//     return new GenomicRange(chr, start, end);
//   }

//   public static void nullCheck(String chromString) {
//     if (chromString == null)
//       throw new RuntimeException("Chromosome string is null");
//     if (chromString.equals(""))
//       throw new RuntimeException("Chromosome string is empty");
//   }

//   public static int stringToInt(String intString) {
//     int intValue;

//     String errorMsg = "Positive integer value should be input. "
//       +intString+" is invalid";

//     intString = takeOutCommas(intString);

//     if (intString.length() == 0)
//       throw new RuntimeException(errorMsg);

//     try {
//       intValue = Integer.parseInt(intString);
//     }
//     catch (java.lang.NumberFormatException e) {
//       throw new RuntimeException(errorMsg + e.getMessage());
//     }
//     return intValue;
//   }

//   private static String takeOutCommas(String intString) {
//     intString = intString.trim();
//     int index = intString.indexOf(',');
//     if (index < 0)
//       return intString;
//     else {
//       return intString.replaceAll(",", "");
//     }
//   }

}
