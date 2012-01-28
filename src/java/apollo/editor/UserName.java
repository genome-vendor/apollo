package apollo.editor;

import javax.swing.JOptionPane;

public class UserName {

  /** This is the sole public method. The only way this class is used is
      UserName.getUserName() */
  public static String getUserName() {
    return getSingleton().getName();
  }

  /** perhaps singleton pattern is overkill here, but what the hell
      it is gonna bring up a gui at some point */
  private static UserName singleton;
  private static UserName getSingleton() {
    if (singleton == null)
      singleton = new UserName();
    return singleton;
  }

  private String userName;

  private UserName() {
    userName = retrieveUserName();
  } 

  private String getName() { return userName; }

  /** First tries to get user name from system. If this fails, as it does for windows
      and probably most non-unix systems, user is queried for name */
  private String retrieveUserName() {
    String retrievedName = getNameFromSystem();
    //if (AnnotationEditor.DEBUG) retrievedName = null; // testing
    // not in system property (windows)
    if (retrievedName == null) {
      //System.out.println("Failed to get user name from system property");
      // bring up popup window querying user for name
      retrievedName = getNameFromUser();
    }
    return retrievedName;
  }
  
  /** I think this only works for unix. doesnt work for windows */
  private String getNameFromSystem() {
    return System.getProperty("user.name");
  }

  /** Queries user for name with a dialog box. Returns user input. */
  private String getNameFromUser() {
    return JOptionPane.showInputDialog("Enter Name: ");
  }

}
