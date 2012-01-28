package apollo.datamodel;

import org.bdgp.util.DNAUtils;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A class to represent SeqFeatures which have been mapped to a static golden path-
 * the usual start/end is assumed to be in chromosome coords, but the assembly
 * start/end specific to this subclass are in coords relative to the feature.
 */
public class AssemblyFeature extends AnnotatedFeature
  implements AssemblyFeatureI {

  private int fragmentOffset;
  private String chromosome;
  private String accession;
  private int version;
  private Vector keywords;
  private Vector remarks;
  
  public AssemblyFeature() {
  }//end AssemblyFeature

  public AssemblyFeature(int theFragmentOffset, String theChromosome) {
    fragmentOffset = theFragmentOffset;
    chromosome = theChromosome;
  }//end AssemblyFeature

  public int getFragmentOffset(){
    return fragmentOffset;
  }//end getFragmentOffset
  
  public void setFragmentOffset(int start){
    fragmentOffset = start;
  }//end setFragmentOffset

  public int getVersion(){
    return version;
  }//end getVersion
  
  public void setVersion(int ver){
    version = ver;
  }//end setVersion

  public String getAccession(){
    return accession;
  }//end getAccession
  
  public void setAccession(String acc){
    accession = acc;
  }//end setAccession
  
  public String getChromosome(){
    return chromosome;
  }//end getChromosome
  
  public void setChromosome(String chr){
    chromosome = chr;
  }//end setChromosome

  public Vector getKeywords(){
    return keywords;
  }//end getKeywords
  
  public void addKeyword(String keyword){
    if (keywords == null) {
      keywords = new Vector();
    }
    keywords.addElement(keyword);
  }//end addKeyword

  public Vector getRemarks(){
    return remarks;
  }//end getRemarks
  
  public void addRemark(String remark){
    if (remarks == null) {
      remarks = new Vector();
    }
    remarks.addElement(remark);
  }//end addRemark


  
  public void accept(apollo.util.Visitor visitor){
    visitor.visit(this);
  }//end accept
}
