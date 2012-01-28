package apollo.gui.menus;

import apollo.config.Config;
import apollo.gui.*;
import apollo.gui.genomemap.ApolloPanel;
import apollo.gui.event.*;
import apollo.gui.synteny.CompositeApolloFrame;
import apollo.gui.synteny.CurationManager;
import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.util.*;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

import org.bdgp.io.*;
import org.bdgp.swing.widget.*;
import java.util.Properties;

import java.util.*;

import gov.sandia.postscript.*;

/** This is the "Synteny" menu at the top of the synteny apollo frame */
public class SyntenyChoiceMenu extends JMenu {

  Set activationItems = new HashSet();
  static HashMap logicalNamesAndSpecies;
  CompositeApolloFrame frame;
  //String lastSpeciesSelected = null;
  boolean useOpaqueLinks = Config.getStyle().useOpaqueLinks();
  boolean shadeByPercId = false;
  /** Lock scrolling initial value from style. Not picking up lock scrolling
      change on style change - this is ok for now because presently synteny's
      style cant change. When synteny style can change, will have to either listen
      for style changes or query an szap. */
  private boolean lockScrolling = Config.getStyle().initialLockedScrolling();
  /** Whether shift needed for loack zooming - same issues as lockScrolling on
      style change */
  private boolean shiftLockZoom = Config.getStyle().initialShiftForLockedZooming();
  JCheckBoxMenuItem useOpaqueLinksItem;
  JCheckBoxMenuItem shadeLinksByPercIdItem;
  JCheckBoxMenuItem lockScrollingItem;
  private JCheckBoxMenuItem shiftLockZoomMenuItem;

  public SyntenyChoiceMenu(CompositeApolloFrame frame) {
    super("Synteny");
    this.frame = frame;
    addMenuListener(new SyntenyMenuListener());
    getPopupMenu().addPropertyChangeListener(new PopupPropertyListener());
    menuInit();
  }

  /** INNER CLASS menu listener. calls menuInit on menu selection */
  private class SyntenyMenuListener implements MenuListener {
    public void menuCanceled(MenuEvent e) {}
    public void menuDeselected(MenuEvent e) {}
    public void menuSelected(MenuEvent e) {
      menuInit();
    }
  }

  public void menuInit() {
    removeAll();
    
    ActionListener actionListener = new SyntenyMenuActionListener();
    //
    //If we're dealing with a multi species, then we add switches
    //to allow the user to choose which is the 'active' stranded-zoomable-apollo-panel.
    //if(frame.getCompositeAdapter() != null) {
    if (getCurationManager().isSingleCuration())
      return;
    //Iterator names=Config.getStyle(SyntenyAdapter.class.getName()
    //).getSyntenySpeciesOrder().iterator();
    String name = null;
    JMenuItem item = null;
    for (int i=0; i<getCurationManager().numberOfCurations(); i++) {
      //name = (String)names.next();
      name = getCurationManager().getCurationName(i);
      //name = name.substring(5); // ??
      item = new JCheckBoxMenuItem(name +" panel active");
      getActivationItems().add(item);
      item.addActionListener(actionListener);
      add(item);
      item.setSelected(getCurationManager().isActiveCurName(name));
//    if((getLastSpeciesSelected() != null && name.equals(getLastSpeciesSelected())) ||
//        (i == 0 && getLastSpeciesSelected() == null) )
//         item.setSelected(true);
//       else  item.setSelected(false);
    } //end for
      
    addSeparator();
    useOpaqueLinksItem = new JCheckBoxMenuItem("Use opaque links");
    useOpaqueLinksItem.setSelected(getUseOpaqueLinks());
    useOpaqueLinksItem.addActionListener(actionListener);
    add(useOpaqueLinksItem);
      
    shadeLinksByPercIdItem = new JCheckBoxMenuItem("Shade links by % identity");
    shadeLinksByPercIdItem.addActionListener(actionListener);
    shadeLinksByPercIdItem.setSelected(getShadeByPercId());
    add(shadeLinksByPercIdItem);
      
    lockScrollingItem = new JCheckBoxMenuItem("Lock scrolling");
    lockScrollingItem.addActionListener(actionListener);
    lockScrollingItem.setSelected(getLockScrolling());
    lockScrollingItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
                                                 ActionEvent.CTRL_MASK));
    add(lockScrollingItem);

    shiftLockZoomMenuItem = new JCheckBoxMenuItem("Shift for locked zooming");
    shiftLockZoomMenuItem.addActionListener(actionListener);
    shiftLockZoomMenuItem.setSelected(getShiftZoomLock());
    add(shiftLockZoomMenuItem);
      //}
  }
    
  private class SyntenyMenuActionListener implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      int panelIndex;
      //      boolean useOpaqueLinks;
      //      boolean shadeByPercId;
    
      if(getActivationItems().contains(e.getSource())){
      
        String name = (((JCheckBoxMenuItem)e.getSource()).getText());
        panelIndex = name.indexOf(" panel");
        name = name.substring(0, panelIndex);
        //frame.setInstanceVariablesForSpecies(name);
        CurationManager.getCurationManager().setActiveCurState(name);
        //setLastSpeciesSelected(name);
      
      }else if(e.getSource().equals(getUseOpaqueLinksItem())){
      
        frame.setUseOpaqueLinks(getUseOpaqueLinksItem().isSelected());
        setUseOpaqueLinks(getUseOpaqueLinksItem().isSelected());
      
      }else if(e.getSource().equals(getShadeByPercIdItem())){
      
        frame.setShadeByPercId(getShadeByPercIdItem().isSelected());
        setShadeByPercId(getShadeByPercIdItem().isSelected());
      
      }else if(e.getSource().equals(getLockScrollingItem())){

        frame.setLockScrolling(getLockScrollingItem().isSelected());
        setLockScrolling(getLockScrollingItem().isSelected());

      } else if (e.getSource().equals(getShiftLockZoomMenuItem())) {
        frame.setShiftZoomLock(getShiftLockZoomMenuItem().isSelected());
        setShiftZoomLock(getShiftLockZoomMenuItem().isSelected());
      }else if(e.getSource() instanceof String){ // what is this???
      
        //setLastSpeciesSelected((String)e.getSource());
      
      }//end if
    } // end actionPerformed

  } // end of SyntenyMenuActionListener inner class

  private JCheckBoxMenuItem getLockScrollingItem(){
    return lockScrollingItem;
  }
    
  private JCheckBoxMenuItem getShiftLockZoomMenuItem() { 
    return shiftLockZoomMenuItem; 
  }
  
  private JCheckBoxMenuItem getUseOpaqueLinksItem(){
    return useOpaqueLinksItem;
  }
  
  private JCheckBoxMenuItem getShadeByPercIdItem(){
    return shadeLinksByPercIdItem;
  }
  
  private Set getActivationItems(){
    return activationItems;
  }
  
  private static HashMap getLogicalNamesAndSpecies(){
    return logicalNamesAndSpecies;
  }
  
//   private String getLastSpeciesSelected(){return lastSpeciesSelected; }
//   public void setLastSpeciesSelected(String name){lastSpeciesSelected = name;}
  
  public void setUseOpaqueLinks(boolean value){
    useOpaqueLinks = value;
  }
  
  public void setShadeByPercId(boolean value){
    shadeByPercId = value;
  }

  public boolean getUseOpaqueLinks(){
    return useOpaqueLinks;
  }
  
  public boolean getShadeByPercId(){
    return shadeByPercId;
  }

  public boolean getLockScrolling(){
    return lockScrolling;
  }
  
  public void setLockScrolling(boolean value){
    lockScrolling = value;
  }

  private boolean getShiftZoomLock() { return shiftLockZoom; }
  private void setShiftZoomLock(boolean b) { shiftLockZoom = b; }

  private CurationManager getCurationManager() {
    return CurationManager.getCurationManager();
  }

}


