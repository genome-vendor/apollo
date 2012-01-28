/*
 * Created on Nov 23, 2004
 *
 */
package apollo.test;

import java.util.HashMap;
import java.util.Map;

import apollo.config.Config;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.chado.ChadoTransactionTransformer;
import apollo.dataadapter.chado.jdbc.JDBCTransactionWriter;
import apollo.dataadapter.gamexml.GAMEAdapter;
import apollo.datamodel.CurationSet;
import apollo.gui.Controller;

/**
 * 
 * @author wgm
 */
public class JDBCTransactionLoader {

  private CurationSet curationSet;
  // All properties
  private String dbHost;
  private String dbName;
  private String dbUser;
  private String dbPwd;
  private int port;
  private String fileName;
  
  public JDBCTransactionLoader(String dbHost,
                               String dbName,
                               String dbUser,
                               String dbPwd,
                               int port) {
    this.dbHost = dbHost;
    this.dbName = dbName;
    this.dbUser = dbUser;
    this.dbPwd = dbPwd;
    this.port = port;
  }
  
  public void setGAMEFile(String fileName) {
    this.fileName = fileName;
  }
  
  protected void setUp() throws ApolloAdapterException {
    Config.initializeConfiguration();
    curationSet = new CurationSet();
    curationSet = new CurationSet();
    GAMEAdapter gameAdapter = new GAMEAdapter();
    gameAdapter.setInput(fileName);
    gameAdapter.setInputType(DataInputType.FILE);
    curationSet = gameAdapter.getCurationSet();
    Controller controller = new Controller();
    controller.addListener(Controller.getMasterController());
    curationSet.getTransactionManager().setAnnotationChangeListener(controller);
  }

  public void loadTransactions() throws Exception {
    JDBCTransactionWriter processor = new JDBCTransactionWriter(dbHost, dbName, dbUser, dbPwd, port);
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
    processor.commitTransactions(curationSet.getTransactionManager());
  }    
  
  public static void main(String[] args) {
    if (args.length < 8) {
      argsFail();
    }
    int length = args.length;
    if (length % 2 != 0) {
      argsFail();
    }
    Map map = new HashMap();
    String key = null;
    String value = null;
    for (int i = 0; i < length / 2; i++) {
      key = args[i * 2];
      value = args[i * 2 + 1];
      if (!key.startsWith("-")) {
        argsFail();
      }
      map.put(key, value);
    }
    String fileName = (String) map.get("-f");
    if (fileName == null) {
      System.err.println("Game file name is not specified!");
      argsFail();
    }
    String dbHost = (String) map.get("-h");
    if (dbHost == null) {
      System.err.println("Database host is not specified!");
      argsFail();
    }
    String dbName = (String) map.get("-d");
    if (dbName == null) {
      System.err.println("Database name is not specified!");
      argsFail();
    }
    String dbUser = (String) map.get("-u");
    if (dbUser == null) {
      System.err.println("Database user is not specified");
      argsFail();
    }
    String dbPwd = (String) map.get("-p");
    String portStr = (String) map.get("-port");
    int port = 5432;
    if (portStr != null) 
      port = Integer.parseInt(portStr);
    JDBCTransactionLoader loader = new JDBCTransactionLoader(dbHost,
                                                             dbName,
                                                             dbUser,
                                                             dbPwd,
                                                             port);
    loader.setGAMEFile(fileName);
    try {
      loader.setUp();
      loader.loadTransactions();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private static void argsFail() {
    String usage = "Usage: java TestJDBCTransactions -h dbHost -d dbName " +
                   "-u dbUser [-p dbPwd] [-port dbPort] -f gameFileName";
    System.err.println(usage);
    System.exit(1);
  }

}
