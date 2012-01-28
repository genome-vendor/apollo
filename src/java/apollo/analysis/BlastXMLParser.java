package apollo.analysis;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import apollo.datamodel.FeaturePair;
import apollo.datamodel.FeaturePairI;
import apollo.datamodel.FeatureSet;
import apollo.datamodel.FeatureSetI;
import apollo.datamodel.SeqFeature;
import apollo.datamodel.Sequence;
import apollo.datamodel.SequenceI;
import apollo.datamodel.StrandedFeatureSetI;

public class BlastXMLParser {

  private DocumentBuilder db;
  
  public class BlastXMLParserException extends Exception
  {
    
    private final static long serialVersionUID = 1l;
    
    public BlastXMLParserException(String msg)
    {
      super(msg);
    }
  }
  
  public BlastXMLParser() throws ParserConfigurationException
  {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    db = dbf.newDocumentBuilder();
    db.setEntityResolver(new EntityResolver() {
      public InputSource resolveEntity(String publicId, String systemId)
      {
        return new InputSource(new StringReader(""));
      }
    });
  }
  
  public String parse(InputStream is, StrandedFeatureSetI results) throws SAXException, IOException, BlastXMLParserException
  {
    return parse(is, results, 1, 0, 0);
  }
  
  public String parse(InputStream is, StrandedFeatureSetI results, int strand, int genomicLength, int offset) throws
  SAXException, IOException, BlastXMLParserException
  {
    Document doc = db.parse(is);
    String blast = getTextContent(doc.getElementsByTagName("BlastOutput_program").item(0));
    String db = getTextContent(doc.getElementsByTagName("BlastOutput_db").item(0));
    String type = blast + ":" + db;
    
    // check for errors
    NodeList messages = doc.getElementsByTagName("Iteration_message");
    for (int i = 0; i < messages.getLength(); ++i) {
      String message = getTextContent(messages.item(i));
      int idx = message.indexOf("Error:");
      if (idx != -1) {
        throw new BlastXMLParserException(message.substring(idx));
      }
    }

    FeatureSet hitContainerPlus = new FeatureSet();
    hitContainerPlus.setFeatureType(type);
    hitContainerPlus.setStrand(1);
    FeatureSet hitContainerMinus = new FeatureSet();
    hitContainerMinus.setFeatureType(type);
    hitContainerMinus.setStrand(-1);
    
    NodeList hits = doc.getElementsByTagName("Hit");
    for (int i = 0; i < hits.getLength(); ++i) {
      FeatureSetI hitPlus = new FeatureSet();
      hitPlus.setStrand(1);
      FeatureSetI hitMinus = new FeatureSet();
      hitMinus.setStrand(-1);
      parseHit((Element)hits.item(i), type, i + 1, strand, genomicLength, offset, hitPlus, hitMinus);
      if (hitPlus.getFeatures().size() > 0) {
        hitContainerPlus.addFeature(hitPlus);
      }
      if (hitMinus.getFeatures().size() > 0) {
        hitContainerMinus.addFeature(hitMinus);
      }
    }

    if (hitContainerPlus.getFeatures().size() > 0) {
      results.addFeature(hitContainerPlus);
    }
    if (hitContainerMinus.getFeatures().size() > 0) {
      results.addFeature(hitContainerMinus);
    }

    return type;
  }
  
  private void parseHit(Element elt, String type, int hitNum, int strand, int genomicLength, int offset, FeatureSetI hitPlus, FeatureSetI hitMinus)
  {
    String hitId = getTextContent(elt.getElementsByTagName("Hit_id").item(0));
    String hitDef = getTextContent(elt.getElementsByTagName("Hit_def").item(0));
    String hitAcc = getTextContent(elt.getElementsByTagName("Hit_accession").item(0));
    hitPlus.setId(hitId);
    hitMinus.setId(hitId);
    SequenceI hitSequence = new Sequence(hitId, null);
    hitSequence.setDescription(hitDef);
    NodeList hsps = elt.getElementsByTagName("Hsp");
    for (int i = 0; i < hsps.getLength(); ++i) {
      FeaturePairI hsp = parseHsp((Element)hsps.item(i), type, hitSequence, hitNum, i + 1, strand, genomicLength, offset);
      if (hsp.getStrand() == 1) {
        hitPlus.addFeature(hsp, true);
      }
      else {
        hitMinus.addFeature(hsp, true);
      }
    }
    String id = type + ":" + hitNum;
    hitPlus.setId(id + "-plus");
    hitMinus.setId(id + "-minus");
    //String name = type + ":" + hitId;
    String name = hitAcc;
    hitPlus.setName(name);
    hitMinus.setName(name);
    hitPlus.setFeatureType(type);
    hitMinus.setFeatureType(type);
    hitPlus.setHitSequence(hitSequence);
    hitMinus.setHitSequence(hitSequence);
  }

  private FeaturePairI parseHsp(Element elt, String type, SequenceI hitSeq, int hitNum, int hspNum, int strand, int genomicLength, int offset)
  {
    int queryBegin; //= Integer.parseInt(getTextContent(elt.getElementsByTagName("Hsp_query-from").item(0))) + offset;
    int queryEnd; //= Integer.parseInt(getTextContent(elt.getElementsByTagName("Hsp_query-to").item(0))) + offset;
    int queryStrand = convertFrameToStrand(Integer.parseInt(getTextContent(elt.getElementsByTagName("Hsp_query-frame").item(0))));
    if (strand == 1) {
      queryBegin = Integer.parseInt(getTextContent(elt.getElementsByTagName("Hsp_query-from").item(0))) + offset;
      queryEnd = Integer.parseInt(getTextContent(elt.getElementsByTagName("Hsp_query-to").item(0))) + offset;
    }
    else {
      queryBegin = offset + genomicLength - Integer.parseInt(getTextContent(elt.getElementsByTagName("Hsp_query-to").item(0)));
      queryEnd = offset + genomicLength - Integer.parseInt(getTextContent(elt.getElementsByTagName("Hsp_query-from").item(0)));
      queryStrand *= -1;
    }
    int hitBegin = Integer.parseInt(getTextContent(elt.getElementsByTagName("Hsp_hit-from").item(0)));
    int hitEnd = Integer.parseInt(getTextContent(elt.getElementsByTagName("Hsp_hit-to").item(0)));
    int hitStrand = convertFrameToStrand(Integer.parseInt(getTextContent(elt.getElementsByTagName("Hsp_hit-frame").item(0))));
    double score = Double.parseDouble(getTextContent(elt.getElementsByTagName("Hsp_score").item(0)));
    double bitScore = Double.parseDouble(getTextContent(elt.getElementsByTagName("Hsp_bit-score").item(0)));
    double evalue = Double.parseDouble(getTextContent(elt.getElementsByTagName("Hsp_evalue").item(0)));
    int numIdentity = Integer.parseInt(getTextContent(elt.getElementsByTagName("Hsp_identity").item(0)));
    int alignLen = Integer.parseInt(getTextContent(elt.getElementsByTagName("Hsp_align-len").item(0)));

    // stupid hack to deal with the inconsistency on how NCBI's BLASTXML is produced
    // in blastn, if the strand is minus, the from/to are reversed, however in
    // in blastx, the from/to are not reversed
    if (type.startsWith("blastn:") && hitStrand == -1) {
      int tmp = hitBegin;
      hitBegin = hitEnd;
      hitEnd = tmp;
    }
    
    String queryAlign = getTextContent(elt.getElementsByTagName("Hsp_qseq").item(0));
    String hitAlign = getTextContent(elt.getElementsByTagName("Hsp_hseq").item(0));
    SeqFeature query = new SeqFeature(queryBegin, queryEnd, type, queryStrand);
    query.setExplicitAlignment(queryAlign);
    SeqFeature hit = new SeqFeature(hitBegin, hitEnd, type, hitStrand);
    hit.setExplicitAlignment(hitAlign);
    hit.addProperty("refId", hitSeq.getName());
    FeaturePair hsp = new FeaturePair(query, hit);
    hsp.setScore(score);
    hsp.addScore("expect", evalue);
    hsp.addScore("bits", bitScore);
    hsp.addScore("identity", ((double)numIdentity) / alignLen * 100);
    String id = type + ":" + hitNum + ":" + hspNum;
    hsp.setId(id);
    hsp.setName(id);
    hsp.setFeatureType(type);
    hsp.setHitSequence(hitSeq);
    return hsp;
  }
  
  private int convertFrameToStrand(int frame)
  {
    if (frame > 0) {
      return 1;
    }
    if (frame < 0) {
      return -1;
    }
    return 0;
  }
  
  private String getTextContent(Node node)
  {
    Node child = node.getFirstChild();
    if (child.getNodeType() != Node.TEXT_NODE) {
      return null;
    }
    return child.getNodeValue();
  }
}
