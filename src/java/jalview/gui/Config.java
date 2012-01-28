package jalview.gui;

import java.io.*;
import java.net.*;
import java.util.*;

public class Config {

  public final static boolean DEBUG = false; 

  private static String            database    = "";
  private static String            srsServer   = "";
  private static String            layout      = "strip";
  private static boolean           initialized = false;

  // Don't allow Configs to be instantiated.
  private Config() {
  }

  private static void readConfig(File confFile) {

    System.out.println("Reading configuration file: "  + confFile);

    try {
      StreamTokenizer tokenizer = 
	new StreamTokenizer(new BufferedReader(new FileReader(confFile)));
      tokenizer.eolIsSignificant(true);
      tokenizer.slashStarComments(true);

      boolean EOF     =  false;
      int     tokType =  0;
      Vector  words   =  new Vector();
      while (!EOF) {
	if ((tokType = tokenizer.nextToken()) == StreamTokenizer.TT_EOF){
	  EOF = true;
	}
	else if (tokType != StreamTokenizer.TT_EOL) {
	  if (tokenizer.sval != null) {
	    words.addElement(tokenizer.sval);
	  }
	}
	else {
	  if (words.size() == 2) {
	    String key   = (String)words.elementAt(0);
	    String value = (String)words.elementAt(1);
	    if (key.equals("SRSServer")) {
	      srsServer = new String(value);
	    } else if (key.equals("Database")) {
	      database = new String(value);
	    } else if (key.equals("Layout")) {
	      layout = new String(value);
	    } else if (key.equals("AutosaveInterval")) {
	      if (value.equals("none")) {
		setAutosaveInterval(-1);
	      } else {
		try {
		  setAutosaveInterval(Integer.parseInt(value));
		} catch (NumberFormatException e) {
		  System.err.println("Can't parse number: " + value);
		}
	      }
	    } else if (key.equals("ColourSchemeInstall")) {
	      try {
		String installString = value;
		int breakIndex = installString.indexOf(":");

		if (breakIndex < 0) {
		  //adapterRegistry.installDataAdapter(installString);
		} else {
		  String driverName =
		    installString.substring(0,breakIndex);
		  String driverDesc =
		    installString.substring(breakIndex+1);
		  //adapterRegistry.installDataAdapter(driverName);
		}
	      } catch (Throwable e) {
		System.err.println("Could not install driver "+value+" because of "+e);
	      }
	    } else if (key.equals("FormatAdapterInstall")) {
	      try {
		String installString = value;
		int breakIndex = installString.indexOf(":");

		if (breakIndex < 0) {
		  //adapterRegistry.installDataAdapter(installString);
		} else {
		  String driverName =
		    installString.substring(0,breakIndex);
		  String driverDesc =
		    installString.substring(breakIndex+1);
		  //adapterRegistry.installDataAdapter(driverName);
		}
	      } catch (Throwable e) {
		System.err.println("Could not install driver "+value+" because of "+e);
	      }
	    } else {
	      System.out.println("Unknown config key " + key);
	    }
	  } else {
	    if (words.size() != 0) {
	      System.out.println("Too many words on line beginning " + 
				 (String)words.elementAt(0) + 
				 " in config file");
	    }
	  }
                
	  words.removeAllElements();
	}
      }
      return;

    } catch (Exception ex) {
      System.out.println(ex);
      return;
    }
  }

  public static String getLayout() {
    init();
    return layout;
  }

  public static String getSRSServer() {
    init();
    return srsServer;
  }
   
  public static String getDatabase() {
    init();
    return database;
  }

  private static void init() {
    if(initialized == false) {
      initialized = true;
      System.out.println("initializing");
      String dataDir = System.getProperty("user.home") + "/.jalview";
      String dotFileName =  dataDir + "/.jalview.cfg";
      File configFile = new File(dotFileName);
      if (configFile.exists()) {
        try {
          readConfig(configFile);
        } catch (Exception e) {
           System.err.println(
                  "Failed reading config file " + configFile +
                  ": " + e.getMessage()
           );
        }
      } else {
        setDefaults();
      }
    }
  }

  private static void setDefaults() {
    setSRSServer("srs6.ebi.ac.uk/srs6bin/cgi-bin/");
    setDatabase("swall");
  }

  public static void setSRSServer(String server) {
    srsServer = server;
  }

  public static void setDatabase (String db) {
    database = db;
  }

  private static void setAutosaveInterval(int in) {
    //autosaveInterval = in;
  }

  public static void ensureExists(File thing, String resource) {
    System.err.println("configfile = "+thing);
    if(!thing.exists()) {
      try {
        System.err.println("Creating: " + thing + " from " + resource);
        if(resource.indexOf("/") != 0) {
          resource = "/" + resource;
        }
        
        InputStream is = Config.class.getResourceAsStream(resource);
        if(is == null) {
          throw new NullPointerException("Can't find resource: " + resource);
        }
        getParentFile(thing).mkdirs();
        OutputStream os = new FileOutputStream(thing);
        
        for(int next = is.read(); next != -1; next = is.read()) {
          os.write(next);
        }
        
        os.flush();
        os.close();
      } catch (FileNotFoundException fnfe) {
        throw new Error("Can't create resource: " + fnfe.getMessage());
      } catch (IOException ioe) {
        throw new Error("Can't create resource: " + ioe.getMessage());
      }
    }
  }
    public static File getParentFile(File thing) {
        String p = thing.getParent();
        if (p == null) return null;
        return new File(p);
    }     
}
