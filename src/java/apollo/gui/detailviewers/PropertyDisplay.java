package apollo.gui.detailviewers;

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import apollo.datamodel.*;
import apollo.gui.Selection;
import apollo.gui.Controller;
import apollo.util.FeatureList;

/** Display the properties belonging to a result.
 *  FOR NOW, just print them to stdout--eventually, this will be a popup panel. */
public class PropertyDisplay {
  public PropertyDisplay(Selection selection) {
    FeatureList all_features = selection.getConsolidatedFeatures();
    for (int i=0; i < all_features.size(); i++) {
      SeqFeatureI feat = all_features.getFeature(i);
      writeProperties(feat);
    }
  }

  /** For debugging */
  public static void writeProperties(SeqFeatureI feat) {
    Hashtable props = feat.getPropertiesMulti();
    if (props == null || props.isEmpty()) {
      System.out.println(feat.getName() + " has no properties.");
      return;
    }

    System.out.println("Properties for " + feat.getName() + ":");
    Enumeration e = props.keys();
    while (e.hasMoreElements()) {
      String type = (String) e.nextElement();
      Vector values = feat.getPropertyMulti(type);
      if (values == null)
        continue;
          
      for (int i = 0; i < values.size(); i++) {
        String value = (String) values.elementAt(i);
        writeProperty(type, value, i);
      }
    }
  }

  private static void writeProperty(String prop, String value, int rank) {
    if (value == null || value.equals(""))
      return;

    if (rank > 0)
      System.out.println("  and also (value #" +(rank+1+"") + ") " + prop + " = " + value);
    else
      System.out.println("  " + prop + " = " + value);
  }

}
