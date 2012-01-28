package apollo.dataadapter;

import apollo.datamodel.*;
import java.util.*;

/**
 * A simple bucket-of-info implementation of SyntenyAdapterI, to be fed forward
 * to FullEnsJDBCSyntenyPanel
**/

public class SyntenyAdapter implements SyntenyAdapterI{
  private  String type;
  private  String name;
  //  private  HashMap karyotypes = new HashMap();
  private  KaryotypeAdapterI karyotypeAdapter;
  private  HashMap syntenyRegions = new HashMap();

  public String getName() {
    return name;
  }//end getName

  public void setName(String name) {
    this.name = name;
  }//end setName

  public String getType() {
    return type;
  }//end getType

  public void setType(String type) {
    this.type = type;
  }//end setType

  /**
   * This has to be stamped into me beforehand.
  **/
  public KaryotypeAdapterI getKaryotypeAdapter(){
    return karyotypeAdapter;
  }//end getKaryotypeAdapter

  /**
   * This has to be stamped into me beforehand.
  **/
  public void setKaryotypeAdapter(KaryotypeAdapterI karyotypeAdapter){
    this.karyotypeAdapter = karyotypeAdapter;
  }//end setKaryotypeAdapter
  
  /**
   * Returns a vector's worth of Apollo.datamodel.SyntenyRegion objects for
   * the input chromosome.
  **/
  public Vector getSyntenyRegionsByChromosome(Chromosome chr){
    Vector regions = (Vector)getSyntenyRegions().get(chr.getDisplayId());
    if(regions != null){
      return regions;
    }else{
      return new Vector();
    }//end if
  }//end getSyntenyRegionsByChromosome
  
  public void addSyntenyRegionsForChromosome(String chromosome, Vector regions){
    getSyntenyRegions().put(chromosome, regions);
  }//end addSyntenyRegionsForChromosome
  
  public void addSyntenyRegionForChromosome(String chromosome, SyntenyRegion region){
    Vector regions = (Vector)getSyntenyRegions().get(chromosome);
    if(regions == null){
      regions = new Vector();
      getSyntenyRegions().put(chromosome, regions);
    }//end if
    regions.add(region);
  }//end addSyntenyRegionsForChromosome
  
  private HashMap getSyntenyRegions(){
    return syntenyRegions;
  }//end getSyntenyRegions
}//end SyntenyAdapter
