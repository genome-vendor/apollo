package jalview.util;

import java.io.*;

public class ErrorLog {
  static PrintStream errStream = System.err;
  static PrintStream teeFile = null;

  private ErrorLog() {}

  public static void println(String str) {
    errStream.println("Error Logger: " + str);
  }

  public static void main(String [] argv) {
    ErrorLog.println("ERROR");
  }
}
