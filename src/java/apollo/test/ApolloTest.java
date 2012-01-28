/*
 * Created on Oct 28, 2004
 *
 */
package apollo.test;

import apollo.config.Config;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.gamexml.GAMEAdapter;
import apollo.datamodel.CurationSet;
import apollo.gui.Controller;
import junit.framework.TestCase;

/**
 * 
 * @author wgm
 */
public class ApolloTest extends TestCase {
  protected final String ROOT = "/home/wgm/apolloTemp/";
  // Default for testing
  private String fileName = ROOT + "example_test1.xml";
  protected CurationSet curationSet = null;

  /**
   * This is for test case.
   * @param name
   */
  public ApolloTest(String name) {
    super(name);
  }
  
  protected void setUp() throws Exception {
    super.setUp();
    Config.initializeConfiguration();
    //String fileName = ROOT + "example_test1.xml";
    //String fileName = ROOT + "Rice17100000-17200000.xml";
    curationSet = new CurationSet();
    GAMEAdapter gameAdapter = new GAMEAdapter();
    gameAdapter.setInput(fileName);
    gameAdapter.setInputType(DataInputType.FILE);
    try {
      curationSet = gameAdapter.getCurationSet();
      Controller controller = new Controller();
      controller.addListener(Controller.getMasterController());
      curationSet.getTransactionManager().setAnnotationChangeListener(controller);
    }
    catch (ApolloAdapterException e) {
      e.printStackTrace();
    }
  }

}
