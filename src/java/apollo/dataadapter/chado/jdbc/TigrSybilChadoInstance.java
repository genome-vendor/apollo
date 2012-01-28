package apollo.dataadapter.chado.jdbc;

/**
 * Implementation of AbstractChadoInstance for TIGR's "Sybil" comparative chado databases.  
 * At TIGR we distinguish between "annotation" or "project" databases, which are strictly
 * single-genome, and "Sybil" comparative databases, which contain multiple genomes and 
 * comparative analyses that relate them.
 *
 * TODO - it remains to be seen whether there are any differences between this class and
 * TigrChadoInstance; if there are none then this class can be removed.
 *
 * @author Jonathan Crabtree
 * @version $Revision: 1.34 $ $Date: 2006/07/04 18:33:36 $ $Author: jcrabtree $
 */
public class TigrSybilChadoInstance extends TigrChadoInstance {

  // -----------------------------------------------------------------------
  // Constructors
  // -----------------------------------------------------------------------

  public TigrSybilChadoInstance() {}
  
  TigrSybilChadoInstance(JdbcChadoAdapter jdbcChadoAdapter) {
    super(jdbcChadoAdapter);
  }
  
}
