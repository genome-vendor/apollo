package apollo.dataadapter;

import java.util.*;
import java.io.*;

import apollo.seq.io.*;
import apollo.datamodel.*;
import apollo.config.Config;

import org.bdgp.io.*;
//import org.bdgp.io.DataAdapterException;
import org.bdgp.util.*;
import org.bdgp.swing.widget.*;

public class GFFSyntenyAdapter implements SyntenyAdapterI {
  Hashtable chrRegionHash = new Hashtable();
  KaryotypeAdapterI ka;
  String org1;
  String org2;
  String filePref;
  String fileSuff;

  public String getType() {
    return "Synteny  File";
  }

  public String getName() {
    return "Synteny GFF file format";
  }


  public GFFSyntenyAdapter(String filePref,String fileSuff,
                           KaryotypeAdapterI ka,
                           String org1, String org2) {
    this.filePref = filePref;
    this.fileSuff = fileSuff;

    this.ka = ka;

    this.org1 = org1;
    this.org2 = org2;
  }

  public KaryotypeAdapterI getKaryotypeAdapter() {
    return ka;
  }

  private FeatureSetI getDataFromFile(String fileName) 
    throws ApolloAdapterException {
    try {
      GFFFile gff  = new GFFFile(fileName,"File");

      FeatureSet fset = new FeatureSet();

      for (int i=0; i < gff.seqs.size() ; i++) {
        SeqFeatureI sf = (SeqFeatureI) gff.seqs.elementAt(i);
        fset.addFeature(sf);
      }

      return fset;
    } catch (Exception e) {
      e.printStackTrace();
      throw new ApolloAdapterException(e);
    }
  }

  public Vector getSyntenyRegions(String chr) {
     
    if (chrRegionHash.containsKey(chr)) {
      return (Vector)chrRegionHash.get(chr);
    }
    try {
      String fileName = filePref + chr + fileSuff;
      FeatureSetI fset = getDataFromFile(fileName);

      Vector regions = new Vector();

      for (int i = 0; i < fset.size(); i++) {
        SeqFeatureI sf = fset.getFeatureAt(i);

        if (sf instanceof FeaturePair) {
          FeaturePair fp = (FeaturePair)sf;

          String chrid1 = fp.getName();
          String chrid2 = fp.getHname();

          Chromosome chr1 = null;
          Chromosome chr2 = null;

          for (int j = 0; j < ka.getKaryotypes().size(); j++) {
            Karyotype k = (Karyotype)ka.getKaryotypes().elementAt(j);
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
                                                 (int)fp.getLow(),
                                                 (int)fp.getHigh(),
                                                 chr2,
                                                 (int)fp.getHlow(),
                                                 (int)fp.getHhigh(),
                                                 fp.getStrand());
            regions.addElement(sr);
          } else {
            System.out.println("Can't find chromosomes " + fp.getName() + " " 
                               + fp.getHname());
          }
        }
      }
      chrRegionHash.put(chr,regions);
      return regions;
    } catch (ApolloAdapterException e) {
      System.out.println("Can't read region " + e);
    }
    return null;
  }

  public Vector getSyntenyRegionsByChromosome(Chromosome chr) {
    String chrName = chr.getDisplayId();

    System.out.println("Chromosome " + chrName);

    Vector regions     = getSyntenyRegions(chrName);

    return regions;
  }

  public void addSyntenyRegionForChromosome(String chromosome, SyntenyRegion region){
    System.out.println("addSyntenyRegionsForChromosome not implemented for GFFSyntenyAdapter");
  }//end addSyntenyRegionsForChromosome

  public void setName(String name) {
    System.out.println("setName not implemented for GFFSyntenyAdapter");
  }

  public void setType(String type) {
    System.out.println("setName not implemented for GFFSyntenyAdapter");
  }
}
