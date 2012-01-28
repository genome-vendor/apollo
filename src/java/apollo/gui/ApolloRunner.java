/**
   This used to be the main class to run apollo. All of its functionality now resides
   in apollo.main.Apollo. This is merely a wrapper of Apollo for backwards compatibility.
   At some point this should be deleted. Please replace any script references to this 
   class to apollo.main.Apollo
**/
package apollo.gui;

import apollo.main.Apollo;
import apollo.main.*;

public class ApolloRunner {
  public static void main(String[] argsStrings) {
    Apollo.main(argsStrings);
  }
}
