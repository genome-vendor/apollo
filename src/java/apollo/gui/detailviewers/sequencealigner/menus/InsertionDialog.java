package apollo.gui.detailviewers.sequencealigner.menus;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import apollo.gui.detailviewers.sequencealigner.GoToDialog;

public class InsertionDialog extends JDialog {
  
  /** Logger */
  protected final static Logger logger = 
    LogManager.getLogger(InsertionDialog.class);
  
  private JLabel label;
  private JTextField textField;
  
  public InsertionDialog(String insertion) {
    super();
    label = new JLabel("Insertion:");
    textField = new JTextField(insertion);
    init();
  }
  
  private void init() {
    Box posBox = new Box(BoxLayout.X_AXIS);
    posBox.add(Box.createHorizontalStrut(5));
    posBox.add(label);
    posBox.add(Box.createHorizontalStrut(10));
    posBox.add(textField);
    posBox.add(Box.createHorizontalStrut(15));
    getContentPane().add(posBox);
    setTitle("Insertion");
    setSize(500,200);
  }

}
