package jalview.gui.schemes;

import java.util.*;

public class ColourProperties {

  public static final int ZAPPO        = 0;
  public static final int TAYLOR       = 1;
  public static final int PID          = 2;
  public static final int BLOSUM62     = 3;
  public static final int SECONDARY    = 4;
  public static final int USER         = 5;
  public static final int HYDROPHOBIC  = 6;
  public static final int CONSERVATION = 7;
  public static final int HELIX        = 8;
  public static final int STRAND       = 9;
  public static final int TURN         = 10;
  public static final int BURIED       = 11;
  public static final int FEATURES     = 12;
  public static final int CLUSTALX     = 13;

  static ColourSchemePropertyVector colourSchemes = new ColourSchemePropertyVector();

  static {
    String prefix = getDefaultClassPrefix();

    // MC 27/03/02 These should probably be read in from a file

    colourSchemes.add(new ColourSchemeProperty("Zappo",
					       prefix + "ZappoColourScheme",
					       "Zappo colours"));

    colourSchemes.add(new ColourSchemeProperty("Taylor", 
					       prefix + "TaylorColourScheme",  
					       "Taylor colours"));

    colourSchemes.add(new ColourSchemeProperty("PID", 
					       prefix + "PIDColourScheme",  
					       "Match/mismatch (by percent identity)"));

//     colourSchemes.add(new ColourSchemeProperty("BLOSUM62",
// 					       prefix + "Blosum62ColourScheme", 
// 					       "By BLOSUM62 Score"));

//     colourSchemes.add(new ColourSchemeProperty("Secondary structure",
// 					       prefix + "SecondaryColourScheme", 
// 					       "By Secondary Structure"));

//     colourSchemes.add(new ColourSchemeProperty("User defined",
// 					       prefix + "ZappoColourScheme",  
// 					       "User defined colours"));

//     colourSchemes.add(new ColourSchemeProperty("Hydrophobic", 
// 					       prefix + "HydrophobicColourScheme",
// 					       "By Hydrophobicity"));

//     colourSchemes.add(new ColourSchemeProperty("Helix",    
// 					       prefix + "HelixColourScheme",  
// 					       "Helix propensity"));

//     colourSchemes.add(new ColourSchemeProperty("Strand",
// 					       prefix + "StrandColourScheme", 
// 					       "Strand propensity"));

//     colourSchemes.add(new ColourSchemeProperty("Turn",   
// 					       prefix + "TurnColourScheme",   
// 					       "Turn propensity"));

//     colourSchemes.add(new ColourSchemeProperty("Buried", 
// 					       prefix + "BuriedColourScheme",  
// 					       "Buried index"));

  }

  static int indexOf(String scheme) {
    if (colourSchemes.contains(scheme)) {
      return colourSchemes.indexOf(scheme);
    } else {
      return -1;
    }
  }

  static int indexOfClass(ColourSchemeI scheme) {
    return colourSchemes.indexOfClass(scheme);
  }

  public static String getClassName(int index) {
    return colourSchemes.getClassName(index);
  }

  public static String getMenuString(int index) {
    return colourSchemes.getMenuString(index);
  }
    
  static boolean contains(String scheme) {
    return colourSchemes.contains(scheme);
  }

  public static Vector getColourSchemeNames() {
    return colourSchemes.getColourSchemeNames();
  }

  protected static String getDefaultClassPrefix() {
    return "jalview.gui.schemes.";
  }
}



