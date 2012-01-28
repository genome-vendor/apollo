
/*****
 * Drawable Display tool. Contains various methods
 * for printing out apollo drawable hierarchies
 */
package apollo.gui.drawable;

import apollo.datamodel.*;

import java.io.*;
import java.util.*;

// TODO - these methods should return Strings (or append to StringBuffers) so that 
// the *caller* can decide whether the info. should be logged or printed.

public class DrawableDisplayTool {

  public static void showDrawableSet(DrawableSetI ds) {
    showDrawableSet(ds,0);
  }
  
  public static void showDrawableSet(DrawableSetI ds,int indent) {
    String indentString = "";
    int tempIndent;
    String tempIndentString;
    
    for(int i=0; i<indent*8; i++){
      indentString+=" ";
    }

    System.err.println(indentString +
		       "("+String.valueOf(indent)+"-set) " +
		       getStringForDrawable(ds));
    
    for(int i=0; i < ds.size(); i++){
      Drawable d = ds.getDrawableAt(i);
      
      if(d instanceof DrawableSetI){
        System.err.println("");
        showDrawableSet((DrawableSetI)d,indent+1);
        System.err.println("");
      }else{
        tempIndent = indent+1;
        tempIndentString = indentString+"        ";
        System.err.println("");
        System.err.println(tempIndentString +
			   "("+String.valueOf(tempIndent)+"-leaf) " +
			   getStringForDrawable(d));
        System.err.println("");
      }
    }
  }

  public static String getStringForDrawable(Drawable d) {
    SeqFeatureI sf = d.getFeature();

    
    if (sf instanceof FeaturePair) {
      FeaturePair fp = (FeaturePair)sf;
      return d.getClass().getName() + " \""+fp.getName()+"\"-"+fp.getFeatureType()+" ("+fp.getStart()+", "+fp.getEnd()+")"+(fp.getStrand() == 1 ? "+" : "-")+" {"+fp.getClass().getName()+"} " + getScoresString(fp) + " Pair info: " + fp.getHname() + " " + fp.getHstart() + "-" + fp.getHend();
    } else {
      return d.getClass().getName() + " \""+sf.getName()+"\"-"+sf.getFeatureType()+" ("+sf.getStart()+", "+sf.getEnd()+")"+(sf.getStrand() == 1 ? "+" : "-")+" {"+sf.getClass().getName()+"} " + getScoresString(sf);
    }
  }

  public static String getScoresString(SeqFeatureI sf) {
    StringBuffer buffer;
    Enumeration keys = sf.getScores().keys();
    if (keys.hasMoreElements())
      buffer = new StringBuffer("Scores: ");
    else
      buffer = new StringBuffer("No Scores");
    while (keys.hasMoreElements()) {
      Object key = keys.nextElement();
      buffer.append(" " + ((Score)sf.getScores().get(key)).toString());
    }
    return buffer.toString();
  }
}
