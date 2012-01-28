package apollo.gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/** Class defining a status bar (modified from "Beginning Java 2"). */
public class StatusPane extends JPanel {

  JLabel     label = new JLabel();
  JTextField tb    = new JTextField();
  Color defaultColor = Color.lightGray;

  public StatusPane(String title,String text) {
    this(title,text,Color.lightGray);
  }

  /** The thing called "title" is actually the label for the field; "text" is
      what goes in the indented area to the right of the label! */
  public StatusPane(String title, String text, Color background) {
    setLayout(new FlowLayout(FlowLayout.LEFT, 3, 3));
    setBackground(defaultColor);  // Can be overridden by doing StatusPane.setBackground().
    label.setBackground(defaultColor);
    label.setForeground(Color.black);
    label.setHorizontalAlignment(JLabel.CENTER);
    label.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
    label.setPreferredSize(new Dimension(10,20));
    label.setText(text);

    tb.setText(title);
    tb.setBackground(background);
    tb.setBorder(null);

    label.setFont(getFont());
    tb.setFont(getFont());
    add(tb);
    add(label);
  }

  // this.setFont(StatusBar.this.getFont());
  public void setFont(Font font) {
    super.setFont(font);
    if (label != null)
      label.setFont(getFont());
    if (tb != null)
      tb.setFont(getFont());
  }

  public void setText(String text) {
    if (label.getFont() != null) {
      FontMetrics fm    = label.getFontMetrics(getFont());
      int         width = fm.stringWidth(text);

      label.setPreferredSize(new Dimension(width + 10,20));
      label.setText(text);
    }
  }
}
