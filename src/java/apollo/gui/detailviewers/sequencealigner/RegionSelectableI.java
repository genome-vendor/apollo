package apollo.gui.detailviewers.sequencealigner;

public interface RegionSelectableI {
  
  public void setRegionStart(int pos);
  
  public void setRegionEnd(int pos);
  
  public int getRegionStart();
  
  public int getRegionEnd();
  
  public int getRegionLow();
  
  public int getRegionHigh();

}
