package apollo.dataadapter.ensj;


import java.util.Properties;
import apollo.dataadapter.*;
import java.util.*;

public class EnsJAdapterUtil {

  private static final int MAX_ENSJ_HISTORY_LENGTH = 20;  

  public static java.util.List getPrefixedProperties(
    Properties settings, 
    String prefix
  ){
    java.util.List returnList = new ArrayList();
    int i=0;
    String value = "";
    
    while(i < settings.size() && value != null){
      value = settings.getProperty(prefix + i);
      
      if(value != null){
        if(value.equals("END_OF_LIST")){
          return returnList;
        }
        returnList.add(value);
      }
      i++;
    }
    return returnList;
  }

  public static void putPrefixedProperties(
    Properties settings,
    java.util.List values,
    String prefix
  ){
    int i = 0;
    values.size();
    while(i < values.size() && i < MAX_ENSJ_HISTORY_LENGTH) {
      settings.put(prefix+i, (String)values.get(i));
      i++;
    }
    settings.put(prefix+i, "END_OF_LIST");
  }
}
