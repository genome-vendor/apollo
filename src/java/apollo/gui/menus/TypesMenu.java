/* Offers a pull-right menu of annotation types that user can choose from
   when creating a new annotation.
   Ideally, the appropriate type for the currently selected result should
   appear first in list. */

package apollo.gui.menus;

import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.config.PropertyScheme;
import apollo.datamodel.SeqFeatureI;
import apollo.editor.AnnotationEditor;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.util.Iterator;
import java.util.Vector;
import java.awt.event.*;

public class TypesMenu extends JMenu implements ActionListener {
  AnnotationEditor editor;

  public TypesMenu(AnnotationEditor editor) {
    super("Create new annotation");
    this.editor = editor;
    //    setEnabled(false);

    PropertyScheme scheme = Config.getPropertyScheme();
    Vector annotFeatureProps = scheme .getAnnotationFeatureProps();
    Iterator props = annotFeatureProps.iterator();
    while (props.hasNext()) {
      String type = ((FeatureProperty)props.next()).getDisplayType();
      JMenuItem typeItem = new JMenuItem(type);
      add(typeItem);
      typeItem.addActionListener(this);
      typeItem.setBackground(scheme.getFeatureProperty(type).getColour());
    }
  }

  public void actionPerformed(ActionEvent e) {
    //      System.out.println("Calling editor.addNewAnnot(" + e.getActionCommand() + ")"); // DEL
    // getActionCommand returns the type the user selected from the menu
    editor.addNewAnnot(e.getActionCommand());
  }
}
