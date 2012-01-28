package apollo.dataadapter.synteny;
//package apollo.gui.synteny;

import javax.swing.*;
import apollo.dataadapter.*;
import apollo.datamodel.*;
import apollo.config.Config;
import apollo.gui.StatusBar;
import apollo.gui.event.*;
import java.awt.*;
import java.util.*;
//import apollo.dataadapter.synteny.*;
import org.bdgp.swing.AbstractDataAdapterUI;


public class FullEnsJDBCSyntenyPanel extends JPanel {
  String org1;
  String org2;
  JPanel chrPanel = new JPanel();
  StatusBar sb = new StatusBar();
  SyntenyAdapterI sa;
  KaryotypeAdapterI ka;
  AbstractDataAdapterUI callback;
  String logicalQuerySpecies;
  String logicalHitSpecies;

  public FullEnsJDBCSyntenyPanel(
    String org1, 
    String org2, 
    KaryotypeAdapterI ka,
    SyntenyAdapterI sa,
    AbstractDataAdapterUI theCallback
  ){
    logicalQuerySpecies = org1;
    logicalHitSpecies = org2;
    this.org1 = convertLogicalNameToSpeciesName(logicalQuerySpecies);
    this.org2 = convertLogicalNameToSpeciesName(logicalHitSpecies);
    this.ka = ka;
    this.sa = sa;
    callback = theCallback;
  }

  private AbstractDataAdapterUI getCallback(){
    return callback;
  }//end getCallback

  public void init() {
    int count = 1;
    Vector panels = new Vector();

    setLayout(new BorderLayout());

    Karyotype org1Kary = ka.getKaryotypeBySpeciesName(getLogicalQuerySpecies());
    Karyotype org2Kary = ka.getKaryotypeBySpeciesName(getLogicalHitSpecies());

    Vector org1chrs = org1Kary.getChromosomes();

    int nRow = org1chrs.size()/6;
    if (nRow*6 != org1chrs.size()) {
      nRow++;
    }
    chrPanel.setLayout(new GridLayout(org1chrs.size()/6,6));
    chrPanel.setBackground(Color.white);

    for (int cc = 0; cc < org1chrs.size(); cc++) {
      Chromosome chr1 = (Chromosome)org1chrs.elementAt(cc);

      String chr = chr1.getDisplayId();

      Vector regions = sa.getSyntenyRegionsByChromosome(chr1);

      ChromosomePanel cp = null;

      if (regions.size() != 0) {
        cp    = 
          new ChromosomePanel(
            chr1, 
            regions, 
            null, 
            sb, 
            getCallback(), 
            getLogicalQuerySpecies(),
            getLogicalHitSpecies()
          );

        panels.addElement(cp);
        chrPanel.add(cp);

      } else {
        Vector tmpreg = new Vector();

        cp    = 
          new ChromosomePanel(
            chr1,
            tmpreg,
            null, 
            sb, 
            getCallback(),
            getLogicalQuerySpecies(),
            getLogicalHitSpecies()
          );

        panels.addElement(cp);
        chrPanel.add(cp);
      }
      count++;
    }
    add(chrPanel,BorderLayout.CENTER);
    add(sb,BorderLayout.SOUTH);
  }

  /**
   * Use the synteny style to convert from logical to actual species names.
  **/
  private String convertLogicalNameToSpeciesName(String logicalName){
    HashMap speciesNames = 
      Config
        .getStyle("apollo.dataadapter.synteny.SyntenyAdapter")
        .getSyntenySpeciesNames();
    
    Iterator logicalNames = speciesNames.keySet().iterator();
    int index;
    String longName;
    String shortName = null;
    
    while(logicalNames.hasNext()){
      longName = (String)logicalNames.next();
      
      //
      //Convert Name.Human to Human
      index = longName.indexOf(".");
      shortName = longName.substring(index+1);
      
      if(shortName.equals(logicalName)){
        return (String)speciesNames.get(longName);
      }//end if
      
    }//end while
    
    if(true){
      throw new IllegalStateException("No logical species name matches the name input:"+shortName);
    }//end if
      
    return null;
  }//end convertLogicalNameToSpeciesName
  
  private String getLogicalQuerySpecies(){
    return logicalQuerySpecies;
  }//end getLogicalQuerySpecies
  
  private String getLogicalHitSpecies(){
    return logicalHitSpecies;
  }//end getLogicalHitSpecies
  
/*  
  public static void main(String[] args){
    FullEnsJDBCSyntenyPanel thePanel =  
      new FullEnsJDBCSyntenyPanel();

    thePanel.init();
    JFrame frame = new JFrame("range chooser");
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add("Center",thePanel);

    frame.setSize(700,700);
    frame.show();
  }
*/
}

