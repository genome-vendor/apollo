package apollo.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.*;

/** Value class for synteny link types (used by game synteny) 
 if we go to java 1.5 this should be replaced by an enumeration */
public class LinkType implements Cloneable {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(LinkType.class);


  private String PARENT_LEVEL = "PARENT";
  private String CHILD_LEVEL = "CHILD";

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private String linkLevel = CHILD_LEVEL;

  private boolean matchOnId = false; // default true?? for now false for game

  private String typeString;
  
  public LinkType(String typeString) {
  	this.typeString=typeString;				
  }
  /** Link to transcript in transcript coords. */
  public boolean isPeptide() {
  	return typeString.equals("PEPTIDE"); 
  } 
  /** Link to a transcript feature with peptide coords */ 
  public boolean isTranscript() {
  	return typeString.equals("TRANSCRIPT"); 
  } 
  /** Link to features of same type (eg syntenic block) */
  public boolean isSelf() {
  	return typeString.equals("SELF"); 
  }  
  public boolean isNoLink() {
  	return typeString.equals("NO_LINK"); 
  }  

  /** Valid link level values are CHILD & PARENT, this says whether link
      leaves(children) or leaf parents(transcript like things */
  void setSyntenyLinkLevel(String level) {
    if (level.equals(PARENT_LEVEL))
      this.linkLevel = PARENT_LEVEL;
    else if (level.equals(CHILD_LEVEL))
      this.linkLevel = CHILD_LEVEL;
    else
      logger.error("synteny_link_level "+level+" is invalid. Must be "+
                   PARENT_LEVEL+" or "+CHILD_LEVEL);
  }


  public boolean isParentLevel() {
    return linkLevel == PARENT_LEVEL;
  }

  void setSyntenyLinkMatchOn(String matchOn) {
    if (matchOn.equalsIgnoreCase("ID"))
      matchOnId = true;
    else if (matchOn.equalsIgnoreCase("RANGE"))
      matchOnId = false;
    else
      logger.warn(matchOn+" is invalid value for synteny_link_match_on in "
                  +" tiers file, ignoring");
  }

  public boolean isLinkById() {
    return matchOnId;
  }

  public String toString() { return "LinkType: "+typeString; }

}
