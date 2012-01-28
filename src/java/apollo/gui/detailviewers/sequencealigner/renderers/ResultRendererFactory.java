package apollo.gui.detailviewers.sequencealigner.renderers;

public class ResultRendererFactory {

  private boolean easyRead;
  
  public ResultRendererFactory(boolean easyRead) {
    this.easyRead = easyRead;
  }
  
  public BaseRendererI makeRenderer() {
    return new DNAResultRenderer(easyRead);
  }
  
  public boolean getEasyRead() {
    return easyRead;
  }
  
  public void setEasyRead(boolean easyRead) {
    this.easyRead = easyRead;
  }
  
}
