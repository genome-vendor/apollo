package apollo.dataadapter;

import java.util.*;
import java.io.*;

import apollo.seq.io.*;
import apollo.datamodel.*;
import apollo.config.Config;

import org.bdgp.io.*;
import org.bdgp.util.*;
import org.bdgp.swing.widget.*;

public class SyntenyGFFAdapter extends GFFAdapter
  implements ApolloDataAdapterI {
  Vector      regions;
  Chromosome  chr1 = null;
  KaryotypeAdapter ca;
  String org1;
  String org2;

  IOOperation [] supportedOperations = {
                                         ApolloDataAdapterI.OP_READ_DATA,
                                       };

  public void init() {}

  public String getType() {
    return "Synteny  File";
  }

  public IOOperation [] getSupportedOperations() {
    return supportedOperations;
  }

  public DataAdapterUI getUI(IOOperation op) {
    //return new SyntenyGFFAdapterGUI(op);
    return null;
  }


  public String getName() {
    return "Synteny GFF file format";
  }


  // Must be instantiated with the name of a GFF file
  public SyntenyGFFAdapter(String filename,String org1, String org2) {
    this.filename = filename;

    ca = new KaryotypeAdapter();

    this.org1 = org1;
    this.org2 = org2;
  }
  public KaryotypeAdapter getKaryotypeAdapter() {
    return ca;
  }
  public SyntenyGFFAdapter() {}
  public CurationSet getCurationSet() throws ApolloAdapterException {
    CurationSet curationSet = new CurationSet();
    curationSet.setAnnots(new StrandedFeatureSet(new FeatureSet(),new FeatureSet()));
    curationSet.setResults(getAnalysisRegion());
    //curationSet.setAnnotationChangeLog(new apollo.gui.AnnotationChangeLog());
    //curationSet.setPropertyScheme(apollo.config.Config.getPropertyScheme());
    //curationSet.setRegion(region);
    return curationSet;
  }

  // extID is ignored here, all data from file is returned
  public StrandedFeatureSetI getAnalysisRegion() throws ApolloAdapterException {
    try {

      fireProgressEvent(new ProgressEvent(this, new Double(0.0),
                                          "Beginning parse..."));

      GFFFile gff  = new GFFFile(filename,"File");

      StrandedFeatureSet fset = new StrandedFeatureSet(new FeatureSet(),new FeatureSet());

      for (int i=0; i < gff.seqs.size() ; i++) {
        SeqFeatureI sf = (SeqFeatureI) gff.seqs.elementAt(i);
        fset.addFeature(sf);
      }

      fireProgressEvent(new ProgressEvent(this, new Double(50.0),
                                          "Populating data structures..."));

      fireProgressEvent(new ProgressEvent(this, new Double(100.0),
                                          "Done..."));

      //apollo.dataadapter.debug.DisplayTool.showFeatureSet(fset);
      return fset;
    } catch (Exception e) {
      e.printStackTrace();
      throw new ApolloAdapterException(e);
    }
  }

  public Vector getSyntenyRegions() {
    if (regions != null) {
      return regions;
    }
    try {
      StrandedFeatureSetI fset = getAnalysisRegion();
      regions = new Vector();


      for (int i = 0; i < fset.size(); i++) {
        SeqFeatureI sf = fset.getFeatureAt(i);

        if (sf instanceof FeaturePair) {
          FeaturePair fp = (FeaturePair)sf;

          String chrid1 = fp.getName();
          String chrid2 = fp.getHname();

          Chromosome chr2 = null;

          for (int j = 0; j < ca.getKaryotypes().size(); j++) {
            Karyotype k = (Karyotype)ca.getKaryotypes().elementAt(j);
            if (k.getSpeciesName().equals(org1)) {
              Chromosome tmpchr = k.getChromosomeByName(chrid1);
              if (tmpchr  != null) {
                chr1 = tmpchr;
              }
            }
            if (k.getSpeciesName().equals(org2)) {
              Chromosome tmp = k.getChromosomeByName(chrid2);
              if (tmp  != null) {
                chr2 = tmp;
              }
            }
          }
          if (chr1 != null && chr2 != null) {
            SyntenyRegion sr = new SyntenyRegion(chr1,
						 fp.getLow(),
						 fp.getHigh(),
						 chr2,
						 fp.getHlow(),
						 fp.getHhigh(),
						 fp.getStrand());

            regions.addElement(sr);
          } else {
            System.out.println("Can't find chromosomes " + fp.getName() + " " + fp.getHname());
          }
        }
      }
      return regions;
    } catch (ApolloAdapterException e) {
      System.out.println("Can't read region " + e);
    }
    return null;
  }

  public Vector getSyntenyRegionsByChromosome(String chr) {
    if (chr.indexOf("chr") == 0) {
      chr = chr.substring(3);
    }

    System.out.println("Chromosome " + chr);

    Vector regions     = getSyntenyRegions();
    Vector newRegions  = new Vector();

    for (int i = 0; i < regions.size(); i++) {
      SyntenyRegion reg = (SyntenyRegion)regions.elementAt(i);

      String chr1 = reg.getChromosome1().getDisplayId();

      if (chr1.equals(chr)) {
        newRegions.addElement(reg);
      }
    }

    return newRegions;
  }

  public Chromosome getChromosome1() {
    return chr1;
  }

}









