package apollo.gui.evidencepanel;

import java.awt.Dimension;
import java.util.Vector;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import misc.JIniFile;

import apollo.gui.Orientations;

/** JPanel that holds evidence panels for all of the szaps/species,
 handle orientation change. This should be made a singleton. 
 Should this be merged in with CurationManager? Should Species hold the ev panels?*/
public class EvidencePanelContainer extends JPanel {

  private static EvidencePanelContainer evidencePanelContainerSingleton;
  private Vector evidencePanels = new Vector(3);
  private int orientation = Orientations.VERTICAL;
    
  public static EvidencePanelContainer getSingleton() {
    if (evidencePanelContainerSingleton == null) 
      evidencePanelContainerSingleton = new EvidencePanelContainer();
    return evidencePanelContainerSingleton;
  }

  private EvidencePanelContainer() {
    // BoxLayout dont have to specify # of panels (unlike GridLayout)
    setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
  }
    
  /** Min size 0,0 - allow the user to shrink the ev panel down to 0 */
  public Dimension getMinimumSize() {
    return new Dimension(0,0);
  }
  public Dimension getPreferredSize() {
    return new Dimension(200,100);
  }

  public void clear() { 
    // perhaps we should just clear out the evidence panels and keep them around
    // for each ev panel evPan.clear()
    evidencePanels.clear();
    removeAll();
    repaint();
  }

  public void addEvidencePanel(EvidencePanel evPan) {
    add(evPan);
    setEvidencePanelOrientation(evPan);
    evidencePanels.add(evPan);
  }

  private void setEvidencePanelOrientation(EvidencePanel evPan) {
    if(getOrientation() == Orientations.VERTICAL) {
      evPan.setOrientation(JSplitPane.VERTICAL_SPLIT);
    } else {
      evPan.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
    }
  }

  int getOrientation(){
    return orientation;
  }
    
  private Vector getEvidencePanels(){
    return evidencePanels;
  }

  private EvidencePanel getEvidencePanel(int i) {
    return (EvidencePanel)evidencePanels.get(i);
  }
    
  public void setOrientation(int orientation){
    //if (orientation == this.orientation) { return; } // ??
    this.orientation = orientation;
    // this.removeAll();//its sufficient to just change layout manager
    if(getOrientation() == Orientations.VERTICAL){ 
      setLayout(new javax.swing.BoxLayout(this,BoxLayout.Y_AXIS));
    }else{
      setLayout(new javax.swing.BoxLayout(this,BoxLayout.X_AXIS));
    }
      
    for(int i=0; i< getEvidencePanels().size(); i++){
      setEvidencePanelOrientation(getEvidencePanel(i));
    }
  }

  public void saveLayout(JIniFile iniFile) {
    for(int i=0; i< getEvidencePanels().size(); i++){
      String section = "EvidencePanel" + i;
      getEvidencePanel(i).saveLayout(iniFile,section);
    }
  }

  public void applyLayout(JIniFile iniFile, boolean updateBaseLocation) {
    for(int i=0; i< getEvidencePanels().size(); i++){
      String section = "EvidencePanel" + i;
      getEvidencePanel(i).applyLayout(iniFile, section, updateBaseLocation);
    }
  }
} 

