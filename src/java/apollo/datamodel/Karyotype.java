package apollo.datamodel;

import java.util.*;

public class Karyotype {

  private String species_name;
  private Vector chromosomes;

  public Karyotype (String name,Vector chromosomes) {
    this.species_name = name;
    this.chromosomes = chromosomes;
  }

  public String getSpeciesName() {
    return species_name;
  }
  public Vector getChromosomes() {
    return chromosomes;
  }
  public Chromosome getChromosomeByName(String name) {
    for (int i = 0;i < chromosomes.size(); i++) {
      Chromosome chr = (Chromosome)chromosomes.elementAt(i);
      if (chr.getDisplayId().equals(name)) {
        return chr;
      }
    }
    return null;
  }
}

