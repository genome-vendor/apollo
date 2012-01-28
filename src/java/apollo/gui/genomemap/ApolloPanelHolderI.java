package apollo.gui.genomemap;

import java.awt.Component;
import java.awt.Dimension;
import apollo.datamodel.CurationSet;
import apollo.gui.SelectionManager;
import apollo.gui.StatusBar;

/** An interface that holds ApolloPanels. Currently the only implementation is 
    StrandedZoomableApolloPanel. The idea is to be able to plug in a different
    ApolloPanelHolder into ApolloFrame, for example for the synteny viewer. We 
    have decided not to go that route for now, but we may in the future, and its
    nice to have an interface to abstract the concept anyways. */
public interface ApolloPanelHolderI {

  public void setCurationSet(CurationSet curationSet);
  /** I think this will need to change to:
      ApolloPanel[] getApolloPanels() */
  public ApolloPanel getApolloPanel();
  public boolean selectFeaturesByName(String name, int window);
  public void setSelectionManager(SelectionManager sm);
  public void setStatusBar(StatusBar sb);
  //public void setRealRemove(boolean state);
  public void putVerticalScrollbarsAtStart();
  public void setZoomFactor(double factor);
  /** Single stranded holder return false? */
  public boolean isReverseComplement();
  public void setReverseComplement(boolean revcomp);
  public void clearData();
  //public void setLoadInProgress(boolean inProgress);
  //public ControlledPanel getControlledPanel();??
  public Component getComponent();
  /** These could be taken out of interface. sugar for getComponent().setPreferredSize() */
  public void setPreferredSize(Dimension d);
  public void setVisible(boolean state);
}
