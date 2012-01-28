package apollo.config;

import apollo.datamodel.*;
import apollo.config.Config;
import apollo.gui.URLQueryGenerator;
import org.bdgp.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

public class ParameciumDisplayPrefs extends DefaultDisplayPrefs {
  
  public String getDisplayName (RangeI sf) {
    
    if (sf instanceof SeqFeatureI) 
      return getDisplayName((SeqFeatureI)sf,false);
      
    return sf.getName();
  }
  
  protected String getIdForURL (SeqFeatureI sf) {
    return getDisplayName(sf,true);
  }
  
  private String getDisplayName (SeqFeatureI sf, boolean onlyName) {
    String display_name= null;
    
    if (sf instanceof AnnotatedFeatureI) {
    	display_name=sf.getName();
	
    } else {
    	
    	FeatureSetI fset = (sf.canHaveChildren() ? 
	                          (FeatureSetI) sf : (FeatureSetI) sf.getRefFeature());
       SeqFeatureI hit = fset.getFeatureAt(0).getHitFeature();

	if(fset.getId() != null) { // genePrediction
		display_name=fset.getName();
	} else if(hit != null)  { // hitFeatures
		SequenceI seq = hit.getRefSequence();
		display_name=hit.getName();
		if(!onlyName) {
			if(! fset.getName().equals("P.tetraurelia") & !fset.getName().equals(display_name))
				display_name+=" "+fset.getName();
			if(seq.getDescription()	!= null) 
				display_name+=" "+seq.getDescription();
		}
			
	} else {
		display_name="???";
	}
		  
    }
    return display_name;
  }

}
