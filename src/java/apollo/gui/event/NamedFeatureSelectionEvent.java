package apollo.gui.event;

import java.util.EventObject;
import apollo.datamodel.SeqFeatureI;
import apollo.util.FeatureIterator;
import apollo.gui.Selection;
import apollo.gui.SelectionItem;
import java.util.*;

/**
 * <p> I am fired when the system needs to select a feature by name (e.g.
 * gene stable id) instead of by SeqFeature. The notion is that
 * the name is all you have - the actual object will be found when the even
 * is handled. </p>
 *
 * <p> I was created to ease transmission of selection events (which in the end select
 * genes and exons) between the SyntenyLinkPanel and the two Apollo panels which
 * the link panel knows about. </p>
**/
public class NamedFeatureSelectionEvent extends EventObject {

    String[] names;
  
    public NamedFeatureSelectionEvent(Object source, String[] theNames) {
        super(source);
        names = theNames;
    }//end NamedFeatureSelectionEvent

    public String[] getNames(){
        return names;
    }//end getNames

    public Object getSource(){
        return source;
    }//end getSource
    
}//end NamedFeatureSelectionEvent 
