package jalview.io;

import jalview.datamodel.*;
import jalview.gui.*;

import java.util.*;

public class FormatFactoryDriver extends Driver {

  public static void main(String [] argv) {
    AlignSequenceI [] s = new AlignSequence[1];
    for (int i=0; i<=FormatProperties.JNET; i++) {
      System.out.println("i = " + i + ": " + FormatFactory.get(i,s));
    }
    System.out.println("TEST: Invalid index: " + FormatFactory.get(FormatProperties.JNET+1,s));
    Vector names = FormatProperties.getFormatNames();
    for (int i=0; i<names.size(); i++) {
      System.out.println("i = " + i + ": " + FormatFactory.get((String)names.elementAt(i),s));
    }
    System.out.println("TEST: null name: " + FormatFactory.get((String)null,s));
  }
}
