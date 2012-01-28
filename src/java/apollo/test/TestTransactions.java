/*
 * Created on Oct 28, 2004
 *
 */
package apollo.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import apollo.dataadapter.TransactionOutputAdapter;
import apollo.dataadapter.chado.ChadoTransactionTransformer;
import apollo.dataadapter.chadoxml.ChadoTransactionXMLWriter;
import apollo.datamodel.CurationSet;
import apollo.datamodel.SequenceI;


/**
 * 
 * @author wgm

 This is testing chado trasaction xml writing, and xort validating and xort commiting
 of chado trans. this should be renamed TestChadoXmlTransactions. this does not
 test out jdbc.

 from curation set from ApolloTest, gets its transactions and writes out chado
 transaction xml.
 then validates chado xml
 there is code to then commit to db but it is commented out
 
 chado trans get written to the file /home/wgm/xort/FB_XORT/chadoOutput.xml
 */
public class TestTransactions extends ApolloTest {
  private final String XORT_ROOT = "/home/wgm/xort/FB_XORT/";
  
  public TestTransactions(String name) {
    super(name);
  }
  
  public void testChadoTransactions() {
    TransactionOutputAdapter output = new ChadoTransactionXMLWriter();
    ChadoTransactionTransformer transformer = new ChadoTransactionTransformer();
    output.setTransformer(new ChadoTransactionTransformer());
    String fileName = ROOT + "chadoOutput.xml";
    output.setTarget(fileName);
    
    // curationSet from ApolloTest
    if (!curationSet.isChromosomeArmUsed()) {
      output.setMapID(curationSet.getChromosome());
      output.setMapType("chromosome");
    }
    else {
      output.setMapID(curationSet.getChromosome());
      output.setMapType("chromosome_arm");
    }
    try {
      output.commitTransactions(curationSet.getTransactionManager());
      // Use System call to run xort_validator
      boolean succeed = false;
      String tnFileName = ROOT + "chadoOutput.ctn";
      if(validateChaodXML(tnFileName));
        succeed = openLogFile("chadoOutput.ctn");
      //if (succeed) {
        // Commit the change directly to the database
        //commitTransactions(tnFileName);
      //}
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private void commitTransactions(String file) throws Exception {
    String xort = XORT_ROOT + "bin/xort_loader.pl";
    xort += " -d chado";
    xort += " -f ";
    xort += file;
    xort += " -i 0";
    xort += " -b 1";
    outputParaMsg("Start Loading Transaction XML"); 
    System.out.println("xort: " + xort);
    Process process = Runtime.getRuntime().exec("perl " + xort);
    int code = process.waitFor();
    InputStream is = null;
    if (code != 0) // Error happend
      is = process.getErrorStream(); 
    else
      is = process.getInputStream();
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    String line = null;
    while ((line = br.readLine()) != null)
      System.out.println(line);
    process.destroy();
    outputParaMsg("End Loading Transaction XML");
  }
  
  private boolean openLogFile(String chadoFileName) throws Exception {
    String logFileName = XORT_ROOT + "tmp/validator_" + chadoFileName + ".log";
    FileReader fr = new FileReader(logFileName);
    BufferedReader reader = new BufferedReader(fr);
    String line = null;
    outputParaMsg("Validator Log File");
    StringBuffer buffer = new StringBuffer();
    while ((line = reader.readLine()) != null) {
      buffer.append(line.trim());
      buffer.append("\n");
    }
    System.out.println(buffer.toString());
    outputParaMsg("Validator Log File");
    return buffer.length() == 0 ? true : false;
  }
  
  private boolean validateChaodXML(String chadoFileName) throws Exception {
      String xort = XORT_ROOT + "bin/xort_validator.pl";
      xort += " -d chado";
      xort += " -f ";
      xort += chadoFileName;
      xort += " -v 1";
      Process process = Runtime.getRuntime().exec("perl " + xort);
      int code = process.waitFor();
      InputStream is = null;
      if (code != 0) // Error happend
        is = process.getErrorStream(); 
      else
        is = process.getInputStream();
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      String line = null;
      outputParaMsg("Validator Process Message");
      while ((line = br.readLine()) != null)
        System.out.println(line);
      process.destroy();
      outputParaMsg("Validator Process Message");
      return code == 0 ? true : false;
  }
  
  private void outputParaMsg(String msg) {
    System.out.println("------- " + msg + " --------");
  }
  
}
