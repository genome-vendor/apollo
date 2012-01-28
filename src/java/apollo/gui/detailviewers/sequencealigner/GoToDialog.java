package apollo.gui.detailviewers.sequencealigner;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import apollo.gui.BaseScrollable;

public class GoToDialog extends JDialog {

  /** Logger */
  protected final static Logger logger = 
    LogManager.getLogger(GoToDialog.class);
  
  /** Object that will be scrolled */
  BaseScrollable scrollableObject;
  
  /** Visual components */
  JLabel posLabel;
  JTextField posField;
  JButton goButton;

  public GoToDialog(BaseScrollable scrollableObject) {
    this.scrollableObject = scrollableObject;
    posLabel = new JLabel("Position");
    posField = new JTextField();
    goButton = new JButton("GoTo");
    init();
  }
  
  private void init() {
    Box posBox = new Box(BoxLayout.X_AXIS);
    posBox.add(Box.createHorizontalStrut(5));
    posBox.add(posLabel);
    posBox.add(Box.createHorizontalStrut(10));
    posBox.add(posField);
    posBox.add(Box.createHorizontalStrut(15));
    posBox.add(goButton);      
    getContentPane().add(posBox);
    setTitle("Exon Detail Editor GoTo");
    setSize(250,50);

    goButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        goTo();
      }
    });
  }    

  private void goTo() {
    String pos = posField.getText();
    if (pos.length() < 1)
      return;
    try {
      int position = Integer.parseInt(pos);
      this.scrollableObject.scrollToBase(position);
      /*
      scrollToBase(position);
      Vector zones = new Vector();
      int [] match_positions = new int[2];
      if (reverseStrand) {
        match_positions[0] = basePairToPos(position);
        match_positions[1] = basePairToPos((position-1));
      } else {
        match_positions[0] = basePairToPos(position);
        match_positions[1] = basePairToPos((position+1));
      }
      zones.add(match_positions);
      setHitZones(zones);
      setShowHitZones(true);
      baseFineEditor.clearFindsButton.setEnabled(true);
      */
      dispose();
    } catch (Exception e) {
      logger.error("Not an integer: " + pos);
    }
  }
}
