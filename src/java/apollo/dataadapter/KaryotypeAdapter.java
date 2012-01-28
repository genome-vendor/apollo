package apollo.dataadapter;

import java.util.*;
import apollo.datamodel.*;

/**
 * A simple implementation of KaryotypeAdapterI. I am a bucket
 * of information passed forward to the FullEnsjSyntenyPanel.
**/
public class KaryotypeAdapter implements KaryotypeAdapterI {

  String type;
  String name;
  HashMap karyotypes = new HashMap();

  public String getName() {
    return name;
  }//end getName

  public String setName(String name) {
    return this.name = name;
  }//end setName

  public String getType() {
    return type;
  }//end getType

  public void setType(String type) {
    this.type = type;
  }//end setType

  public Vector getKaryotypes(){
    Iterator values = karyotypes.values().iterator();
    Vector returnVector = new Vector();
    while(values.hasNext()){
      returnVector.add(values.next());
    }
    
    return returnVector;
  }//end getKaryotype
  
  public Karyotype getKaryotypeBySpeciesName(String name){
    return (Karyotype)getKaryotypeHash().get(name);
  }//end getKarotypeBySpeciesName
  
  public void addKaryotype(Karyotype karyotype){
    getKaryotypeHash().put(karyotype.getSpeciesName(), karyotype);
  }//end setKaryotype
    
  private HashMap getKaryotypeHash(){
    return karyotypes;
  }//end getKaryotypeHash
}//end KaryotypeAdapter



