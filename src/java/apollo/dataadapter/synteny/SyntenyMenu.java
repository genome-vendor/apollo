package apollo.dataadapter.synteny;

import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.dataadapter.synteny.ChromosomePanel;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import org.bdgp.swing.AbstractDataAdapterUI;

public class SyntenyMenu extends JPopupMenu
                         implements ActionListener {

  JMenuItem region1;
  JMenuItem region2;

  /** ChromosomePanel not actually used - delete? */
  ChromosomePanel cp;
  SyntenyRegion   region;

  String species1;
  String species2;

  private AbstractDataAdapterUI callback;
  String logicalQuerySpecies;
  String logicalHitSpecies;

  static JFrame frame;

  public SyntenyMenu(ChromosomePanel cp, SyntenyRegion region) {
    this(cp, region, null, null, null);
  }

  public SyntenyMenu(
    ChromosomePanel cp,
    SyntenyRegion region,
    AbstractDataAdapterUI theCallback,
    String querySpecies,
    String hitSpecies
  ) {

    super("Synteny");

    this.cp = cp;
    this.region  = region;

    this.species1 = region.getChromosome1().getSpeciesName();
    this.species2 = region.getChromosome2().getSpeciesName();
    callback = theCallback;
    logicalQuerySpecies = querySpecies;
    logicalHitSpecies = hitSpecies;

    menuInit();
  }

  private AbstractDataAdapterUI getCallback(){
    return callback;
  }

  public void menuInit() {

    region1    = new JMenuItem("this one please");

    add(region1);
    region1  .addActionListener(this);
  }

  public void actionPerformed(ActionEvent e) {
    HashMap theInput = new HashMap();
    theInput.put("region", region);
    theInput.put("logicalQuerySpecies", getLogicalQuerySpecies());
    theInput.put("logicalHitSpecies", getLogicalHitSpecies());
    getCallback().setInput(theInput);
  }

  private String getLogicalQuerySpecies(){
    return logicalQuerySpecies;
  }

  private String getLogicalHitSpecies(){
    return logicalHitSpecies;
  }

}
