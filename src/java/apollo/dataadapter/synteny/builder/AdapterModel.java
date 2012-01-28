package apollo.dataadapter.synteny.builder;
import java.util.*;

public class AdapterModel {
  private String type;
  private String name;
  private String adapterClassName;
  private Properties properties;

  public static String MAIN_TYPE = "MAIN_TYPE";
  public static String CHILD_TYPE = "CHILD_TYPE";

  public AdapterModel(String name, String adapterClass, String type) {
    this.name = name;
    adapterClassName = adapterClass;
    this.type = type;
    properties = new Properties();
  }

  public String getName(){
    return name;
  }

  public void setName(String newValue){
    name = newValue;
  }
  
  public String getType(){
    return type;
  }

  public void setType(String type){
    this.type = type;
  }

  public String getAdapterClassName(){
    return adapterClassName;
  }

  public void setAdapterClassName(String newValue){
    adapterClassName = newValue;
  }
  
  public Properties getProperties(){
    return properties;
  }
  
  public void setProperties(Properties newValue){
    properties = newValue;
  }
  
  public void addProperty(String key, String value){
    getProperties().setProperty(key, value);
  }
  
  public String getProperty(String key){
    return getProperties().getProperty(key);
  }

  public String toXML(String indent){
    StringBuffer buffer = new StringBuffer();
    Iterator keys;
    String key;
    
    buffer
      .append(indent).append("<adapter>\n")
      .append(indent).append("\t<name>").append(getName()).append("</name>\n")
      .append(indent).append("\t<class>").append(getAdapterClassName()).append("</class>\n")
      .append(indent).append("\t<type>").append(getType()).append("</type>\n");
    
    if(getProperties().size()>0){
      keys = getProperties().keySet().iterator();
      while(keys.hasNext()){
        key = (String)keys.next();
        buffer
          .append(indent).append("\t<property>\n")
          .append(indent).append("\t\t<key>").append(key).append("</key>\n")
          .append(indent).append("\t\t<value>").append(getProperty(key)).append("</value>\n")
          .append(indent).append("\t</property>\n");
      }
    }
    
    buffer.append(indent).append("</adapter>\n");
    return buffer.toString();
  }
}
