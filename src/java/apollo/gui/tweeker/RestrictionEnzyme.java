package apollo.gui.tweeker;

import java.awt.Color;

import apollo.datamodel.SequenceI;

public class RestrictionEnzyme {
  private String name;
  private String cut_site;
  private SequenceI refSequence;
  //  private Vector forward_positions;
  // private Vector reverse_positions;
  private Color color;

  public RestrictionEnzyme(String name,
                           String cut_site, 
                           SequenceI refSequence,
                           Color color) {
    this.name = name;
    this.cut_site = cut_site;
    this.refSequence = refSequence;
    this.color = color;
  }
  
  public String toString() {
    return name + " [" + cut_site + "]";
  }

  public String getName() {
    return name;
  }

  public String getCutSite() {
    return this.cut_site;
  }

  public SequenceI getRefSequence() {
    return refSequence;
  }

  public Color getColor() {
    return color;
  }
}
