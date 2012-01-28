package apollo.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.Vector;
import apollo.gui.genomemap.*;
import apollo.config.Config;

/** PrefsPanel goes inside a PreferencesDialog window.
 *  4/08/04: Since now your style file only needs to contain the DIFFS
 *  from the default one, it's often rather empty.
 *  Show, in another (unpersonal) text window, the contents of the default
 *  style file, so user can see what they can change. */

public class PrefsPanel extends JPanel {
  JTextArea defaultTextArea;
  JTextArea personalTextArea;
  Color bgColor;
  JLabel defaultHeader;
  JLabel personalHeader;

  public PrefsPanel(PreferencesDialog parent,
		    Color bgColor) {
    this.bgColor = bgColor;
    initGUI();
  }

  private void initGUI() {
    setLayout(new BorderLayout(10,10));
    setBackground(bgColor);

    setupTextArea("default");
    setupTextArea("personal");
  }

  private void setupTextArea(String which) {
    JTextArea textArea;
    JLabel header;
    Panel panel = new Panel();
    panel.setLayout(new BorderLayout(10,5));

    boolean editable = true;
    if (which.equals("default")) {
      defaultTextArea = new JTextArea();
      textArea = defaultTextArea;
      textArea.setRows(19);
      editable = false;
      textArea.setForeground(Color.gray);
      defaultHeader = new JLabel("");
      header = defaultHeader;
    }
    else {
      personalTextArea = new JTextArea();
      textArea = personalTextArea;
      textArea.setRows(11);
      personalHeader = new JLabel("");
      header = personalHeader;
    }
    header.setForeground(Color.black);
    JScrollPane textScroll = new JScrollPane();
    textScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    textScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

    textArea.setLineWrap(false);
    textArea.setWrapStyleWord(true);
    textArea.setEditable(editable);
    textScroll.getViewport().add(textArea, null);

    panel.add(header, BorderLayout.NORTH);
    panel.add(textScroll, BorderLayout.CENTER);

    if (which.equals("default"))
      add(panel, BorderLayout.NORTH);
    else
      add(panel, BorderLayout.SOUTH);
  }

  public void setText(String text, String which) {
    JTextArea textArea;
    if (which.equals("personal"))
      textArea = personalTextArea;
    else
      textArea = defaultTextArea;
    textArea.setText(text);
    textArea.setCaretPosition(0);  // Scroll text box up to top (default is scrolled to bottom)
  }

  /** It's only ever relevant to get text from the personal text area */
  public String getText() {
    return personalTextArea.getText();
  }

  public void addMessage(String text, String which) {
    JLabel header;
    if (which.equals("default"))
      header = defaultHeader;
    else
      header = personalHeader;
    header.setText("<HTML><FONT FACE=Geneva,Dialog,Helvetica color=black><B>"+text+"</B></FONT></HTML>");
  }

}
