package apollo.gui.annotinfo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;

import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JComponent;
import javax.swing.text.Document;

import apollo.config.Config;

/** Field that is a JLabel if ReadOnly, if not ReadOnly is an editable component.
 * Convenience class to deal with the 2 cases of ReadOnly and editable. 
 * Editable component is either a JComboBox or a JTextField.
 * This was an inner class of FeatureEditorDialog, but it turns out TranscriptEditPanel
 * needs to use it as well.
 */
class ReadWriteField {

  private final static Dimension fieldSize = new Dimension(200,21);
  private final static Font fieldFont = new Font("Dialog",Font.PLAIN,12);
  private boolean isList;
  private JComboBox list;
  private JTextField jTextField;
  private JComponent jComponent;
  private boolean isReadOnly = !Config.isEditingEnabled();

  ReadWriteField() {
    this(false);
  }

  ReadWriteField(boolean isList) {
    this.isList = isList;
    if (isList) {
      list = new JComboBox();
      list.setEditable(false);
      list.setBackground(Config.getAnnotationBackground());
      jComponent = list;
    } else {
      jTextField = new JTextField();
      jComponent = jTextField;
      jTextField.setEditable(!isReadOnly);
    }
    jComponent.setMinimumSize(fieldSize);
    jComponent.setPreferredSize(fieldSize);
    jComponent.setFont(fieldFont);
  }

  void setEditable(boolean editable) {
    this.isReadOnly = !editable;
    if (jTextField != null)
      jTextField.setEditable(editable);
  }

  void setEnabled(boolean enabled) {
    if (jComponent != null)
      jComponent.setEnabled(enabled);
  }  

  void setValue(String value) {
    if (isList)
      list.setSelectedItem(value);
    else
      jTextField.setText(value);
  }
  String getValue() {
    if (isList)
      return (String)list.getSelectedItem();
    else
      return jTextField.getText();
  }
  JComponent getComponent() {
    return jComponent;
  }
  void addFocusListener(FocusListener fl) {
    jComponent.addFocusListener(fl);
  }

  void setListBackground(Color bgColor) {
    if (isList)
      list.setBackground(bgColor);
  }

  /** List functions */
  void addItem(String listItem) {
    if (isReadOnly || !isList)
      return; // only for editable lists
    list.addItem(listItem);
  }
  void setMaximumRowCount(int max) {
    if (isReadOnly || !isList)
      return; // only for editable lists
    list.setMaximumRowCount(max);
  }
  void removeAllItems() {
    if (isReadOnly || !isList)
      return; // only for editable lists
    list.removeAllItems();
  }

  void addKeyListener(KeyListener kl) {
    getComponent().addKeyListener(kl);
  }

  Document getDocument() {
    if (!isList)
      return jTextField.getDocument();
    return null; // lists have no document
  }
}
