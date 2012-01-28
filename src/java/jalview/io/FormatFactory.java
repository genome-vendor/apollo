package jalview.io;

import jalview.datamodel.*;

import java.lang.reflect.*;
import java.util.*;

public class FormatFactory {

  public static AlignFile get(int index, AlignSequenceI [] s) {
    try {
      Class c = Class.forName(FormatProperties.getClassName(index));
      Class [] paramTypes = new Class[1];
      paramTypes[0] = AlignSequenceI[].class; 
      Constructor cons = c.getConstructor(paramTypes);
    
      Object [] params = new Object[1];
      params[0] = s;
      return (AlignFile)cons.newInstance(params);
    } catch (Exception e) {
      System.err.println(e);
      return null;
    }
  }

  public static AlignFile get(int index,String inStr) {
    try {
      Class c = Class.forName(FormatProperties.getClassName(index));
      Class [] paramTypes = new Class[1];
      paramTypes[0] = String.class; 
      Constructor cons = c.getConstructor(paramTypes);
    
      Object [] params = new Object[1];
      params[0] = inStr;
      return (AlignFile)cons.newInstance(params);
    } catch (Exception e) {
      System.err.println(e);
      return null;
    }
  }

  public static AlignFile get(int index,String inFile,String type) {
    try {
      Class c = Class.forName(FormatProperties.getClassName(index));
      System.out.println("Class = " + c);
      Class [] paramTypes = new Class[2];
      paramTypes[0] = String.class; 
      paramTypes[1] = String.class; 
      Constructor cons = c.getConstructor(paramTypes);

      System.out.println("Constructor = " + cons);
    
      Object [] params = new Object[2];
      params[0] = inFile;
      params[1] = type;
      return (AlignFile)cons.newInstance(params);
    } catch (Exception e) {
      System.err.println(e);
      return null;
    }
  }

  public static AlignFile get(String format, AlignSequenceI [] s) {
    return get(FormatProperties.indexOf(format),s);
  }

  public static AlignFile get(String format,String inStr) {
    return get(FormatProperties.indexOf(format),inStr);
  }
  public static AlignFile get(String format,String inFile,String type) {
    return get(FormatProperties.indexOf(format),inFile,type);
  }
}
