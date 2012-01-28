/*****
 * Display tool. Contains various methods
 * for printing out apollo datatypes
 */
package apollo.dataadapter.debug;

import apollo.datamodel.*;

import java.io.*;
import java.util.*;

// TODO - these methods should return Strings (or append to StringBuffers) so that 
// the *caller* can decide whether the info. should be logged or printed.

public class DisplayTool {

  private static String getRangeDirection(int in) {
    if (in == 1)
      return "+";
    if (in == -1)
      return "-";
    if (in == 0)
      return "n";
    return "?";
  }

  private static String getGeneTypeName(String in) {
    if (in.equals("gene"))
      return "PROTEIN CODING GENE";
    if (in.equals("tRNA"))
      return "TRNA";
    if (in.equals ("transposon"))
      return "TRANSPOSON";
    return "Annotation";
  }

  public static void showGene(AnnotatedFeatureI g) {
    System.err.println("Gene : ("+
                       g.getName()+", "+
                       g.getSynonyms().toString()+")");
    for(int i=0; i < g.size(); i++) {
      Transcript t = (Transcript) g.getFeatureAt(i);
      System.err.println("  transcript "+(i+1)+": ("+
                         t.getName()+", "+
                         t.getSynonyms().toString()+")");
      for(int j=0; j < t.getExons().size(); j++) {
        ExonI e = (ExonI) t.getExons().elementAt(j);
        System.err.println("    exon "+(j+1)+": "+e.getName()+
                           (e.getStrand() == 1 ? "+" : "-")
                           +" ("+e.getStart()+", "+e.getEnd()+")");
      }
    }
  }

  public static void showExon(ExonI e) {
    System.err.println("    exon : "+e.getName()+
                       (e.getStrand() == 1 ? "+" : "-")
                       +" ("+e.getStart()+", "+e.getEnd()+") "+e.getFeatureType());
  }

  public static void showFeatureSet(FeatureSetI fs) {
    showFeatureSet(fs,0);
  }
  
  public static void showFeatureSet(FeatureSetI fs,int indent) {
    String indentString = "";
    int tempIndent;
    String tempIndentString;
    
    for(int i=0; i<indent*8; i++){
      indentString+=" ";
    }

    System.err.println(indentString +
		       "("+String.valueOf(indent)+"-set) " +
		       getStringForSeqFeature(fs));
    
    for(int i=0; i < fs.size(); i++){
      SeqFeatureI sf = fs.getFeatureAt(i);
      
      if(sf instanceof FeatureSetI){
        System.err.println("");
        showFeatureSet((FeatureSetI)sf,indent+1);
        System.err.println("");
      }else{
        tempIndent = indent+1;
        tempIndentString = indentString+"        ";
        System.err.println("");
        System.err.println(tempIndentString +
			   "("+String.valueOf(tempIndent)+"-leaf) " +
			   getStringForSeqFeature(sf));
        System.err.println("");
      }
    }
  }
  public static void outputSpaces(int nSpace) {
    for (int i=0;i<nSpace;i++) {
      System.err.print(" ");
    }
  }

  public static String getStringForSeqFeature(SeqFeatureI sf) {
    if (sf instanceof FeaturePair) {
      return "\""+sf.getName()+"\"-"+sf.getFeatureType()+" ("+sf.getStart()+", "+sf.getEnd()+")"+(sf.getStrand() == 1 ? "+" : "-")+" {"+sf.getClass().getName()+"} " + getScoresString(sf) + " Pair info: " + ((FeaturePair)sf).getHname() + " " + ((FeaturePair)sf).getHstart() + "-" + ((FeaturePair)sf).getHend();
    } else {
      return "\""+sf.getName()+"\"-"+sf.getFeatureType()+" ("+sf.getStart()+", "+sf.getEnd()+")"+(sf.getStrand() == 1 ? "+" : "-")+" {"+sf.getClass().getName()+"} " + getScoresString(sf);
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

  public static void checkRefFeatures(FeatureSetI fs) {
    Vector children = fs.getFeatures();
    for (int i=0; i<fs.size(); i++) {
      SeqFeatureI sf = (SeqFeatureI)children.elementAt(i);
      if (sf.getRefFeature() != fs) {
        System.out.println("RefFeature not parent for " + sf);
      }
      if (sf instanceof FeatureSetI) {
        checkRefFeatures((FeatureSetI)sf);
      }
    }
  }
}
