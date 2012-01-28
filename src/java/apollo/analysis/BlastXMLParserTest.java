package apollo.analysis;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import apollo.datamodel.StrandedFeatureSet;

import junit.framework.TestCase;

public class BlastXMLParserTest extends TestCase {

  public void testParse() throws Exception
  {
    FileInputStream fis = new FileInputStream("/Users/elee/blah/blast_3.xml");
    BlastXMLParser parser = new BlastXMLParser();
    StrandedFeatureSet results = new StrandedFeatureSet();
    parser.parse(fis, results);
  }
  
}
