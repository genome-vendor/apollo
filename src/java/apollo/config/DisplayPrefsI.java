package apollo.config;

import java.util.Date;
import java.util.Vector;
import apollo.datamodel.*;

public interface DisplayPrefsI {

  /** generate a title for this curation */
  public String getTitle (CurationSet curation);

  //generates a URL
  public String generateURL (SeqFeatureI f);

  // get the display name of a seqfeature, short if needed
  public String getDisplayName (RangeI sf);

  // get a feature name appropriate for use in a FASTA sequence header
  public String getHeader (RangeI sf);

  // Return a string to display the biotype of the feature
  public String getBioTypeForDisplay(SeqFeatureI sf);

  public String getPublicName(SeqFeatureI origSF);

}
