package apollo.util;

import java.util.Date;
import java.util.Locale;
import java.lang.String;
import java.text.SimpleDateFormat;
import java.util.TimeZone;


public class DateUtil
{ 
//   protected static String [] date_formats = {
//     "dd-MMM-yyyy",
//     "d-MMM-yy",
//     "yyyy-M-d",
//     "EEE MMM dd HH:mm:ss zzz yyyy",
//     "EEE MMM dd HH:mm:ss yyyy",
//     "EEE MMM dd HH:mm:sszzz yyyy"
//   };

  // Try different possible date formats to see if any of them works
  protected static SimpleDateFormat [] date_formats = {
    new SimpleDateFormat("dd-MMM-yyyy"),  // this is the one we usually see
    new SimpleDateFormat("d-MMM-yy"),
    new SimpleDateFormat("yyyy-M-d"),
    new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy"),
    new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy"),
    new SimpleDateFormat("EEE MMM dd HH:mm:sszzz yyyy"),
    // Default date format (whatever that may be)
    new SimpleDateFormat ()
  };

  private static SimpleDateFormat dateFormat 
    = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);

  public static Date makeADate (String date_str) {
    if (date_str == null || date_str.equals(""))
      return null;

    Date date = null;
    SimpleDateFormat df;

    if (date == null) {
      int i = 0;
      while (date == null && i < date_formats.length) {
        df = date_formats[i];
        try {
          date = df.parse(date_str);
        }
        catch (Exception e) {}
        i++;
      }
    }
    //    if (date == null) {  // DEL
    //      System.out.println ("makeADate: could not parse date " + date_str);  // DEL
    //    }  // DEL
    return date;
  }

  /** The opposite of makeADate: converts a Date object to a String in
   *  the current format.  (This is needed in case the user wants to change the
   *  tiers and save as a tiers file.) */
  public static String formatDate(Date date) {
    return dateFormat.format(date);
  }

  /** Convert a Date to GMT.  This is useful because otherwise we get local timezones
      that, in some cases, java.util.Date can write but can't parse (!). */
  public static String toGMT(Date date) {
    SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    return(sdf.format(date));
  }

  /** Return a date in the format 18-JAN-2004 
   *  (Used by GenbankReport) */
  public static String dateNoTime(Date date) {
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
    return(sdf.format(date).toUpperCase());
  }
}
