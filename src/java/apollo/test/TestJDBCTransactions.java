/*
 * Created on Nov 19, 2004
 *
 */
package apollo.test;

import apollo.dataadapter.chado.ChadoTransactionTransformer;
import apollo.dataadapter.chado.jdbc.JDBCTransactionWriter;

/**
 * 
 * @author wgm
 */
public class TestJDBCTransactions extends ApolloTest{
  
  public TestJDBCTransactions(String name) {
    super(name);
  }
  
  public void testChadoTransactions() {
    JDBCTransactionWriter processor = new JDBCTransactionWriter("brebiou",
                                                                  "rice",
                                                                  "wgm",
                                                                  null,
                                                                  5432);
    ChadoTransactionTransformer transformer = new ChadoTransactionTransformer();
    processor.setTransformer(new ChadoTransactionTransformer());
    if (!curationSet.isChromosomeArmUsed()) {
      processor.setMapID(curationSet.getChromosome());
      processor.setMapType("chromosome");
    }
    else {
      processor.setMapID(curationSet.getChromosome());
      processor.setMapType("chromosome_arm");
    }
    try {
      processor.commitTransactions(curationSet.getTransactionManager());
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }  
    
}
