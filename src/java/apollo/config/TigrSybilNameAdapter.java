package apollo.config;

import apollo.datamodel.CurationSet;
import apollo.datamodel.AnnotatedFeature;
import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.SequenceI;

import apollo.config.Config;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.log4j.*;

/**
 * TIGR-specific apollo name adapter for Chado.  It appears to be necessary to have one 
 * of these in order to link to outside URLs from the genes/transcripts in Apollo (despite 
 * the  specification of a gene URL in the tiers file.)  Annotated features appear to be 
 * handled differently than computed features (called "Results" in Apollo) in this 
 * respect.
 *
 * @author Jonathan Crabtree
 * @version $Revision: 1.2 $ $Date: 2007/01/03 13:50:07 $ $Author: jcrabtree $
 */
public class TigrSybilNameAdapter extends DefaultNameAdapter {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------
  
  protected final static Logger logger = LogManager.getLogger(TigrSybilNameAdapter.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------
  
  // TO DO - make this class more generic; currently it only handles a specific type of gene 
  // id used in TIGR's Chado-migrated eukaryotic annotation databases

  /**
   * Regular expression pattern used to parse a gene's ID from the chado uniquename.
   */
  protected Pattern geneIdPattern;
  
  // -----------------------------------------------------------------------
  // Constructor
  // -----------------------------------------------------------------------
  
  public TigrSybilNameAdapter() {
    // TIGR-specific
    geneIdPattern = Pattern.compile("(\\S+)\\.(\\d+\\.\\S+)_gene");
  }
  
  // -----------------------------------------------------------------------
  // ApolloNameAdapterI
  // -----------------------------------------------------------------------
  
  public String generateURL(AnnotatedFeatureI g) {
    if (g instanceof AnnotatedFeature) {
      String geneUrl = Config.getGeneUrl();
      
      // TIGR-specific; parses the gene ID to get the eukaryotic annotation database name and transcript name
      Matcher m = geneIdPattern.matcher(g.getId());
      if (m.matches()) {
        String geneDb = m.group(1);
        String geneId = m.group(2);
        
        // TO DO - the username and password are embedded in the URL and we should be using
        // the same username and password that were specified when the data were loaded.
        return geneUrl + "&db=" + geneDb + "&orf=" + geneId;
      } else {
        logger.warn("failed to parse TIGR eukaryotic annotation database name and transcript id from gene name '" + g.getId() + "'");
      }
    }
    return null;
  }
  
}
