/*
	Copyright (c) 1997
	University of California Berkeley
*/

package apollo.analysis.filter;

import java.io.*;
import java.util.*;

import apollo.datamodel.*;

public class Coverage {

  public static Vector sortRegions (FeatureSetI forward_analysis, 
				    FeatureSetI reverse_analysis) {
	
    Vector remainders = new Vector();
    if (forward_analysis != null)
      remainders.addAll (forward_analysis.getFeatures());
    if (reverse_analysis != null)
      remainders.addAll (reverse_analysis.getFeatures());
    Vector regions = new Vector ();
    //    System.out.println ("Coverage.sortRegions: hits on forward: " + forward_analysis.size() +
    //			"; hits on reverse: " + reverse_analysis.size() +
    //			"; Total = " + remainders.size());  // DEL
	
    while (remainders.size () > 0) {
      FeatureSetI hit = (FeatureSetI) remainders.elementAt (0);
      Vector region = new Vector();
      region.addElement (hit);
      regions.addElement (region);
      remainders.removeElement (hit);
      boolean added = true;
      while (added) {
	added = false;
	for (int i = 0; i < region.size(); i++) {
	  hit = (FeatureSetI) region.elementAt (i);
	  int hit_length = getHitLength(hit);
	  for (int j = remainders.size() - 1; j >= 0; j--) {
	    FeatureSetI check_hit 
	      = (FeatureSetI) remainders.elementAt (j);
	    boolean found_region = regionsOverlap (hit, check_hit, hit_length);
	    if (found_region) {
	      // add the new hit at the appropriate spot 
	      //in the list
	      // strongest hits to weakest hits
	      added |= true;
	      addToRegion (region, check_hit);
	      remainders.removeElement (check_hit);
	    }
	  }
	}
      }
    }
    return regions;
  }

  public static void cleanUp (Vector regions, 
			      FeatureSetI forward_analysis,
			      FeatureSetI reverse_analysis,
			      int max, 
			      AnalysisFilter filter) {
    Vector region;
    FeatureSetI hit;
    int del;

    for (int i = 0; i < regions.size (); i++) {
      region = (Vector) regions.elementAt (i);
      Vector forward_region = new Vector();
      Vector reverse_region = new Vector();
      for (int j = 0; j < region.size(); j++) {
	hit = (FeatureSetI) region.elementAt (j);
	if (hit.getStrand() == 1)
	  forward_region.addElement(hit);
	else
	  reverse_region.addElement(hit);
      }
      region.removeAllElements();
      testForMax (forward_region, forward_analysis, max, filter);
      testForMax (reverse_region, reverse_analysis, max, filter);
    }
    System.gc();
  }

  public static void testForMax (Vector region, 
				 FeatureSetI analysis,
				 int max,
				 AnalysisFilter filter) {
    while (region.size() > max) {
      FeatureSetI hit = (FeatureSetI) region.elementAt (max);
      region.removeElementAt (max);
      analysis.deleteFeature (hit);
      filter.debugFeature (hit, "Exceeded coverage ");
    }
  }

  private static int getHitLength (FeatureSetI hit) {
    int hit_length = 0;
    for (int i = 0; i < hit.size(); i++) {
      SeqFeatureI span = hit.getFeatureAt(i);
      hit_length += span.length();
    }
    return hit_length;
  }

  private static boolean regionsOverlap (FeatureSetI hit,
					 FeatureSetI region_hit,
					 int hit_length) {
    int region_length = getHitLength(region_hit);
    // must be at least 50% of the shorter one
    int min_overlap = (int) (Math.min(hit_length, region_length) * 0.5);
    int total_overlap = 0;
    for (int i = 0; i < hit.size(); i++) {
      SeqFeatureI span = hit.getFeatureAt(i);
      for (int j = 0; j < region_hit.size(); j++) {
	SeqFeatureI region_span	= region_hit.getFeatureAt(j);
	if (span.overlaps(region_span)) {
	  int total_length = span.length() + region_span.length();
	  int extent = (Math.max (span.getHigh(), region_span.getHigh()) -
			Math.min (span.getLow(), region_span.getLow()));
	  
	  total_overlap += total_length - extent;
	}
      }
    }
    return (total_overlap >= min_overlap);
  }

  private static void addToRegion (Vector region, FeatureSetI hit) {
    int i = 0;
    boolean added = false;
    double hit_score = hit.getScore();
    double hit_expect = hit.getScore("expect");
    while (!added && i < region.size()) {
      FeatureSetI check_hit = (FeatureSetI) region.elementAt (i);
      double check_score = check_hit.getScore();
      double check_expect = check_hit.getScore("expect");
      if (hit_score > check_score
	  || (hit_score == check_score
	      && hit_expect != -1 &&
	      hit_expect < check_expect)) {
	region.insertElementAt (hit, i);
	added = true;
      }
      else {
	i++;
      }
    }
    if (!added)
      region.addElement (hit);
  }
}


