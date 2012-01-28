package apollo.analysis;

import java.io.FileInputStream;

import apollo.datamodel.StrandedFeatureSet;

import junit.framework.TestCase;

public class PrimerBlastHtmlParserTest extends TestCase {

  public void testParse() throws Exception
  {
    PrimerBlastHtmlParser parser = new PrimerBlastHtmlParser();
    FileInputStream fis = new FileInputStream("/Users/elee/blah/primer_blast_results.html");
    StrandedFeatureSet results = new StrandedFeatureSet();
    parser.parse(fis, results);
  }
  
}
