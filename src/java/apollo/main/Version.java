package apollo.main;

import apollo.config.Config;

/**
 * Separate class for setting the version string (used to be in ApolloFrame)
 */

public class Version {
  public static String version = "Apollo Genome Annotation and Curation Tool, version 1.11.1";
  public static String getVersion() {
    String timestamp = Config.getTimestamp();
    if (timestamp == null)
      return(version);
    else
      return(version + ", last updated " + timestamp);
  }
}
