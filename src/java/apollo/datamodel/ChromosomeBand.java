package apollo.datamodel;


public class ChromosomeBand {

  private String display_id;
  private String chr_name;
  private int    chr_start;
  private int    chr_end;
  private String stain;


  public ChromosomeBand(String id,String chr_name, int chr_start, int chr_end, String stain) {

    this.display_id = id;
    this.chr_name   = chr_name;
    this.chr_start  = chr_start;
    this.chr_end    = chr_end;
    this.stain      = stain;

  }

  public String getDisplayId() {
    return display_id;
  }
  public String getChrName() {
    return chr_name;
  }
  public int getChrStart() {
    return chr_start;
  }
  public int getChrEnd() {
    return chr_end;
  }
  public String getStain() {
    return stain;
  }
}
