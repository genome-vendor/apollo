package apollo.gui;

// Class defining a status bar (modified from "Beginning Java 2").
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import apollo.config.Config;

/**
 * Shows the status bar at the bottom of the main display.
 */
public class StatusBar extends JPanel {
  private StatusPane actionPane   = new StatusPane("Action","");
  private StatusPane featurePane  = new StatusPane("Feature","");
  private StatusPane positionPane = new StatusPane("Position","");

  // Constructor
  public StatusBar() {
    setLayout(new FlowLayout(FlowLayout.LEFT, 10, 3));
    setBackground(Color.lightGray);
    setBorder(BorderFactory.createLineBorder(Color.darkGray));
    add(positionPane);
    add(featurePane);
    if (Config.isEditingEnabled())
      add(actionPane);
    setFont(new Font("Serif", 0, 16));
  }

  public void setFont(Font font) {
    super.setFont(font);
    if (actionPane != null)
      actionPane.setFont(font);
    if (featurePane != null)
      featurePane.setFont(font);
    if (positionPane != null)
      positionPane.setFont(font);
  }

  public void setActionPane(String text) {
    actionPane.setText(text);
  }

  public void setFeaturePane(String text) {
    featurePane.setText(text);
  }

  public void setPositionPane(String text) {
    positionPane.setText(text);
  }
}
