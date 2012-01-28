package apollo.gui;

import java.util.*;
import java.awt.*;

public class URLQueryGenerator {

  private URLQueryGenerator() {}

  public static String getURL(String baseURL, String id) {
    int IDpos = -1;
    String idBit = id;
    String finalURL = "";
    String afterID = baseURL;

    if ( afterID.indexOf("__ID") != -1) {
      while ((IDpos = afterID.indexOf("__ID")) != -1) {
        finalURL += afterID.substring(0,IDpos);
        afterID = afterID.substring(IDpos+4);
        if (!afterID.startsWith("__")) {
          String separator = afterID.substring(0,1);
          int sepPos;
          if ((sepPos = idBit.indexOf(separator)) != -1) {
            finalURL += idBit.substring(0,sepPos);
            idBit = idBit.substring(sepPos+1);
            afterID = afterID.substring(3);
          } else {
            finalURL += idBit;
            afterID = afterID.substring(3);
            System.out.println("Warning: Failed finding ID separator " + 
                               separator + " in baseURL " + baseURL);
            break;
          }
        } else {
          finalURL += idBit;
          afterID = afterID.substring(2);
        }
      }
      finalURL += afterID;
    } else {
      finalURL = baseURL + id;
    }
    //    System.out.println("URL for baseURL " + baseURL + ", id " + id + " = " + finalURL);  // DEL
    return finalURL;
  }
}
