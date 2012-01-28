package apollo.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import apollo.datamodel.FeatureSet;
import apollo.datamodel.FeatureSetI;
import apollo.datamodel.SeqFeature;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.StrandedFeatureSetI;
import apollo.util.FeatureList;

public class PrimerBlastHtmlParser {

  private static final String TYPE = "primer_blast";
  
  public class PrimerBlastHtmlParserException extends Exception
  {
    private static final long serialVersionUID = 1L;
    
    public PrimerBlastHtmlParserException(String msg)
    {
      super(msg);
    }
  }
  
  public PrimerBlastHtmlParser() throws IOException
  {
  }
  
  public String parse(InputStream is, StrandedFeatureSetI results) throws IOException, PrimerBlastHtmlParserException {
    return parse(is, results, 0, null);
  }
  
  public String parse(InputStream is, StrandedFeatureSetI results, FeatureList fl) throws IOException, PrimerBlastHtmlParserException {
    return parse(is, results, 0, fl);
  }
  
  public String parse(InputStream is, StrandedFeatureSetI results, int offset) throws IOException, PrimerBlastHtmlParserException {
    return parse(is, results, offset, null);
  }
  
  public String parse(InputStream is, StrandedFeatureSetI results, int offset, FeatureList fl) throws IOException, PrimerBlastHtmlParserException
  {
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    String line;
    int primerNum = 1;
    FeatureSetI primerPlus = new FeatureSet();
    primerPlus.setStrand(1);
    primerPlus.setFeatureType(TYPE);
    primerPlus.setName(TYPE);
    FeatureSetI primerMinus = new FeatureSet();
    primerMinus.setStrand(-1);
    primerMinus.setFeatureType(TYPE);
    primerMinus.setName(TYPE);
    FeatureList leaves = fl != null ? fl.getAllLeaves() : null;
    while ((line = br.readLine()) != null) {
      
      // check for errors
      if (line.contains("class=\"error\"")) {
        String []tokens = line.split("<|>");
        throw new PrimerBlastHtmlParserException(tokens[6].replaceAll("&quot;", "'"));
      }
      
      if (line.contains("<th>Forward primer</th>")) {
        SeqFeatureI fPrimerPlus = processData(line, primerNum, offset, true);
        SeqFeatureI fPrimerMinus = processData(line, primerNum, offset, false);
        line = br.readLine();
        if (!line.contains("<th>Reverse primer</th>")) {
          apollo.util.IOUtil.errorDialog("Forward primer missing reverse primer");
          continue;
        }
        SeqFeatureI rPrimerPlus = processData(line, primerNum, offset, true);
        SeqFeatureI rPrimerMinus = processData(line, primerNum, offset, false);
        if (leaves != null) {
          SeqFeatureI fPrimer;
          SeqFeatureI rPrimer;
          if (leaves.getFeature(0).getStrand() == 1) {
            fPrimer = fPrimerPlus;
            rPrimer = rPrimerPlus;
          }
          else {
            fPrimer = fPrimerMinus;
            rPrimer = rPrimerMinus;
          }
          if (!isValid(fPrimer, rPrimer, leaves)) {
            continue;
          }
        }
        primerPlus.addFeature(createPrimerPair(fPrimerPlus, rPrimerPlus, 1, primerNum));
        primerMinus.addFeature(createPrimerPair(fPrimerMinus, rPrimerMinus, -1, primerNum++));
      }
    }
    results.addFeature(primerPlus);
    results.addFeature(primerMinus);
    return TYPE;
  }

  private SeqFeatureI processData(String line, int primerNum, int offset, boolean plus)
  {
    String []tokens = line.split("(</*t[rhd]>)+");
    int strand = tokens[3].equals("Plus") ? 1 : -1;
    //int strand = plus ? 1 : -1;
    int start = Integer.parseInt(tokens[5]) + offset;
    int stop = Integer.parseInt(tokens[6]) + offset;
    if (strand == -1) {
      int tmp = start;
      start = stop;
      stop = tmp;
    }

    //SeqFeatureI feat = new SeqFeature(start, stop, "primer", strand);
    SeqFeatureI feat = new SeqFeature(start, stop, TYPE, plus ? 1 : -1);
    String id = "primer-" + primerNum + "-" + (strand == 1 ? "forward" : "reverse") + (plus ? "-plus" : "-minus");
    feat.setId(id);
    feat.setName(id);
    
    return feat;
  }
  
  private FeatureSetI createPrimerPair(SeqFeatureI forwardPrimer, SeqFeatureI reversePrimer, int strand, int primerNum)
  {
    forwardPrimer.setStrand(strand);
    reversePrimer.setStrand(strand);
    FeatureSetI primerPair = new FeatureSet();
    primerPair.addFeature(forwardPrimer);
    primerPair.addFeature(reversePrimer);
    primerPair.setStrand(strand);
    String id = "primer-pair-" + primerNum + (strand == 1 ? "-plus" : "-minus");
    primerPair.setId(id);
    primerPair.setName(id + " (length = " + (reversePrimer.getLow() - forwardPrimer.getHigh() +
        forwardPrimer.length() + reversePrimer.length() - 1) + ")");
    primerPair.setFeatureType(TYPE);

    return primerPair;
  }
  
  private boolean isValid(SeqFeatureI fPrimer, SeqFeatureI rPrimer, FeatureList leaves)
  {
    SeqFeatureI fPrimerExonOverlap = null;
    SeqFeatureI rPrimerExonOverlap = null;
    for (int i = 0; i < leaves.size(); ++i) {
      SeqFeatureI exon = leaves.getFeature(i);
      if (exon.isExon()) {
        if (isContained(exon, fPrimer)) {
          fPrimerExonOverlap = exon;
        }
        else if (isContained(exon, rPrimer)) {
          rPrimerExonOverlap = exon;
        }
      }
    }
    return fPrimerExonOverlap != null && rPrimerExonOverlap != null &&
        !fPrimerExonOverlap.getId().equals(rPrimerExonOverlap.getId());
  }
  
  private boolean isContained(SeqFeatureI feat1, SeqFeatureI feat2)
  {
    return feat2.getLow() >= feat1.getLow() && feat2.getHigh() <= feat1.getHigh();
  }
  
}
