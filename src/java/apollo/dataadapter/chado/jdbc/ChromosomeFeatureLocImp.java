/*
 * Created on Nov 4, 2004
 *
 */
package apollo.dataadapter.chado.jdbc;

import java.sql.Connection;

import apollo.dataadapter.Region;

/**
 * A special FeatureLocImplementation for chromosome location query.
 * @author wgm
 not sure we actually need a special subclass for this - a lot of this stuff
 is already in FeatureLocImp
 this should be renamed TopLevelFeatLocImp
 */
class ChromosomeFeatureLocImp extends FeatureLocImplementation {
  private int interbaseStart;
  private int interbaseEnd;
  
  protected ChromosomeFeatureLocImp(long featureId, Region baseOrientedRegion, Connection conn)
  {
    super.featId = featureId;
    this.interbaseStart = baseOrientedRegion.getStart() - 1;
    this.interbaseEnd = baseOrientedRegion.getEnd();
    this.conn = conn;
  }
  
  /** Region is base oriented NOT interbase (from user/gui) */
  protected ChromosomeFeatureLocImp(long featureId, Region baseOrientedRegion) {
    this(featureId, baseOrientedRegion, null);
  }
  
  int getBaseOrientedMinWithPadding() {
    return interbaseStart + 1;
  }


  long getContainingFeatureId() {
    return featId;
  }

  String getContainingFeatureWhereClause(String featLocTableName) {
    /*
    int start = interbaseStart - flankLength;
    int end = interbaseEnd + flankLength;
    if (start < 0) {
      start = 0;
    }
    if (max > 0 && start > max) {
      end = max;
    }
    */
    int start = getCoordinateWithFlankUpstream(interbaseStart);
    int end = getCoordinateWithFlankDownstream(interbaseEnd);
    //this can be really slow when there are many featurelocs to filter
    /*
    return " AND "+ featLocTableName +".fmax > " + interbaseStart
      +" AND "+featLocTableName+".fmin < "+ interbaseEnd +" ";
    */
    //instead we set the WHERE clause to return data contained within the region
    //of interest plus some flank and then filter out the result
    //this is much more efficient, especially when looking at smaller ranges
    return " AND " + featLocTableName + ".fmax BETWEEN " + start + " AND " + end + " AND " +
    featLocTableName + ".fmin BETWEEN " + start + " AND " + end + " AND " +
    featLocTableName + ".fmin < " + interbaseEnd + " AND " +
    featLocTableName + ".fmax > " + interbaseStart;
    
  }

  int getLengthWithPadding() {
    return interbaseEnd - interbaseStart;
  }

  int getMaxWithPadding() {
    return interbaseEnd;
  }

  // yikes!
//   String getSrcFeatureType() {
//     return "chromosome";
//   }

  // replaces getSrcFeatureType().equals("chromosome") in FlyChadInst
  boolean hasTopLevelName() { return true; }

  /** Returns false - chrom feat locs have a sub range that is different than
      the range of the chromosome itself */
  protected boolean featLocSameAsFeatRange() {
    return false;
  }
}
