package apollo.gui;

import javax.swing.JSplitPane;
import java.awt.Component;

public class UnBorderedSplitPane extends JSplitPane {
  public UnBorderedSplitPane() {
    super();
  }
  public UnBorderedSplitPane(int newOrientation,
                    boolean newContinuousLayout,
                    Component newLeftComponent,
                    Component newRightComponent){
    super(newOrientation,newContinuousLayout,newLeftComponent,newRightComponent);
    resetDecorations();
  }
  public UnBorderedSplitPane(int newOrientation,
                    Component newLeftComponent,
                    Component newRightComponent){
    super(newOrientation,newLeftComponent,newRightComponent);
    resetDecorations();
  }

  public void updateUI() {
//    System.out.println("updateUI called");
    super.updateUI();
    resetDecorations();
  }

  private void resetDecorations() {
    setDividerSize(3);
    setBorder(null);
  }
}
