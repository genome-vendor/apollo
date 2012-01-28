package apollo.datamodel;

import java.util.*;

public class Chromosome {

  private String display_id;
  private int    length;
  private int    plength;
  private int    qlength;
  private int    centrolength;
  private Vector bands;
  private String species_name;
  private String coord_system;

  public Chromosome(String species_name,String id,Vector bands) {
    this.display_id = id;
    this.species_name = species_name;

    parse_bands(bands);
  }
  public Chromosome(String species_name,String id,int length) {
    this.display_id   = id;
    this.length       = length;
    this.species_name = species_name;
  }

  public Chromosome(String species_name,String id,int plength, int centrolength, int qlength) {
    this(species_name,id,plength+qlength+centrolength);

    this.plength = plength;
    this.qlength = qlength;
    this.centrolength = centrolength;
  }

  public String getSpeciesName() {
    return species_name;
  }
  private void parse_bands(Vector tmpbands) {

    int qstart = 1000000000;
    int qend   = 0;
    int pstart = 1000000000;
    int pend   = 0;
    int cstart = 1000000000;
    int cend   = 0;

    for (int i=0; i < tmpbands.size(); i++) {
      ChromosomeBand band = (ChromosomeBand)tmpbands.elementAt(i);

      if (band.getStain().equals("acen")) {
        if (band.getChrEnd() > cend) {
          cend = band.getChrEnd();
        }
        if (band.getChrStart() < cstart) {
          cstart = band.getChrStart();
        }
      } else if (band.getDisplayId().indexOf("p")== 0) {
        if (band.getChrEnd() > pend) {
          pend = band.getChrEnd();
        }
        if (band.getChrStart() < pstart) {
          pstart = band.getChrStart();
        }
      } else if (band.getDisplayId().indexOf("q") == 0) {
        if (band.getChrEnd() > qend) {
          qend = band.getChrEnd();
        }
        if (band.getChrStart()  < qstart) {
          qstart = band.getChrStart();
        }
      } else {
        if (band.getChrEnd() > qend) {
          qend = band.getChrEnd();
        }
        if (band.getChrStart() < qstart) {
          qstart = band.getChrStart();
        }
      }
    }
    
    if (qend > 0) {
      if (pend > qstart) {
        //System.out.println("Error in bands coords parm ends after q begins " + pend + " " + qstart);
        //do nothing: p- and q- staining might not even be done.
      } else {
        cstart = pend+1;
        cend   = qstart-1;
      }
    } else {
      pstart = cstart-1;
      pend   = cstart-1;
    }
    this.bands   = tmpbands;
    plength      = pend;
    centrolength = (cend - cstart + 1);
    qlength      = (qend - qstart + 1);

    length       = plength + centrolength + qlength;

  }

  public String getDisplayId() {
    return display_id;
  }
  public int getLength() {
    return length;
  }
  public int getPlength() {
    return plength;
  }
  public int getQlength() {
    return qlength;
  }
  public int getCentroLength() {
    return centrolength;
  }
  public Vector getBands() {
    return bands;
  }
  
  public void setLength(int length){
    this.length = length;
  }
  
  public void setPLength(int length){
    this.plength = length;
  }
  
  public void setQLength(int length){
    this.qlength = length;
  }
  
  public void setCentroLength(int length){
    this.centrolength = length;
  }

  public void setCoordSystem(String coordSys) {
    this.coord_system = coordSys;
  }
  public String getCoordSystem() {
    return this.coord_system;
  }
}

