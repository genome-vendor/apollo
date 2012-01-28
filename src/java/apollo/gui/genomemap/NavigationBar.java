package apollo.gui.genomemap;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Properties;

import apollo.util.NumericKeyFilter;
import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.seq.*;
import apollo.gui.ApolloFrame;
import apollo.gui.ChromosomeField;
import apollo.config.Config;
import apollo.gui.ControlledObjectI;
import apollo.gui.ControlledPanel;
import apollo.gui.Controller;
import apollo.main.LoadUtil;
import apollo.gui.synteny.GuiCurationState;
import apollo.gui.event.*;
import apollo.gui.menus.*;

import org.apache.log4j.*;

/** This panel appears in the apollo frame and
 *  lets the user navigate to a new region, going left or right or zooming out from 
 *  current coordinates, and selecting new chromosomes. You could think of it as another
 *  gui for the data adapter, that is displayed inside the ApolloFrame. (one could
 *  even imagine this class being moved to dataadapter? except that it is displayed
 *  in the main frame)
 *  This is displayed if "EnableNavigationManager" in style file is set to true.
 *  Should only be enabled for dataadapters that can handle locations.

 *  MovementPanel is currently for one curation/CurationState. One could imagine
 *  either working with the "active" curaiton set, or even having a drop down
 *  list of curation names so as to only have one of these and save real estate.
*/

public class NavigationBar extends ControlledPanel
  implements ActionListener,  ControlledObjectI, DataLoadListener {
 
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(NavigationBar.class);
  
  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  //CurationSet       set;
  //Controller        controller;
  Font              font;
  //ApolloFrame       frame;
  private GuiCurationState curationState;
  JButton           leftButton;
  JButton           rightButton;
  JTextField        startField;
  JTextField        endField;
  //JTextField        chrField;
  ChromosomeField chrField; // inner class - can be JComboBox or JTextField
  JButton           loadButton;
  JButton           expandButton;
  private NavigatorManager navigatorManager; // inner class w/ action for menu

  public NavigationBar(GuiCurationState cs) {
    curationState = cs;
    curationState.getController().addListener(this); // region change events
    initGui();
    updateFields(); // put in chrom, start, end from cur set
  }

  private void initGui() {
    this.removeAll();
    setLayout(new BorderLayout());

    leftButton = new JButton("<");
    rightButton = new JButton(">");
    JLabel startLabel = new JLabel("Start");
    startField = new JTextField();
    // NumericKeyFilter filters out all non numeric chars
    startField.addKeyListener(NumericKeyFilter.getFilter());
    startField.setPreferredSize(new Dimension(80,25));
    JLabel endLabel = new JLabel("End");
    endField = new JTextField();
    endField.addKeyListener(NumericKeyFilter.getFilter());
    endField.setPreferredSize(new Dimension(80,25));
    JLabel chrLabel = new JLabel("Chromosome");
    chrField = new ChromosomeField();//jcombo or text field depending if chroms in style
    loadButton = new JButton("Load");
    expandButton = new JButton("Expand");
    JPanel pan1 = new JPanel();
    pan1.setLayout(new FlowLayout());
    // DatabaseList databaseList = new DatabaseList();???
    // if (databaseList.hasMoreThanOneDatabase()) { add(databaseList.getComboBox()); }
    pan1.add(chrLabel);
    pan1.add(chrField.getComponent());
    pan1.add(leftButton);
    pan1.add(startLabel);
    pan1.add(startField);
    pan1.add(endLabel);
    pan1.add(endField);
    pan1.add(rightButton);
    pan1.add(expandButton);
    pan1.add(loadButton);

    //JLabel label = new JLabel("Press buttons to load adjacent regions");
    //add(leftButton, BorderLayout.WEST); // moved left/right next to start/end
    //add(rightButton, BorderLayout.EAST);
    add(pan1, BorderLayout.CENTER);
    leftButton.addActionListener(this);
    rightButton.addActionListener(this);
    loadButton.addActionListener(this);
    expandButton.addActionListener(this);
    startField.addActionListener(this);
    endField.addActionListener(this);
    chrField.addActionListener(this);

    loadButton.setEnabled(false);  // Load button is disabled until user does something to change the requested region
    // If the user types something in the start or end field, enable the Load button
    startField.addKeyListener(new KeyAdapter() {
                                public void keyReleased(KeyEvent e) {
                                  loadButton.setEnabled(true);
                                }
                              }
                             );
    endField.addKeyListener(new KeyAdapter() {
                              public void keyReleased(KeyEvent e) {
                                loadButton.setEnabled(true);
                              }
                            }
                           );
    // Listener for chrField depends on whether it has a JComboBox or a JTextField
    JComponent chrChooser = chrField.getComponent();
    if (chrChooser instanceof JComboBox) {
      ((JComboBox)chrChooser).addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              loadButton.setEnabled(true);
            }
          }
                                               );
    } else if (chrChooser instanceof JTextField) {
      ((JTextField)chrChooser).addKeyListener(new KeyAdapter() {
                                                public void keyReleased(KeyEvent e) {
                                                  loadButton.setEnabled(true);
                                                }
                                              }
                                             );
    }

    setMinimumSize(new Dimension(150,30));
    setPreferredSize(new Dimension(300,30));
    navigatorManager = new NavigatorManager();
    navigatorManager.setEnabled(Config.isNavigationManagerEnabled());
  }

  public void setEnabled(boolean enable)
  {
    navigatorManager.setEnabled(enable);
  }
  
//   public void addNotify() {
//    frame = (ApolloFrame) SwingUtilities.getAncestorOfClass(ApolloFrame.class, this);
//     super.addNotify();
//   }

  private void updateFields() {
    // if chrom field changed (combobox textfield switch) rebuild gui
    if (chrField.styleChanged())
      initGui();
    if (getCurSet() == null) { // shouldnt happen
      logger.error("Programmer error. NavigationBar CurSet is null.");
      return;
    }
    chrField.setChromosome(getCurSet().getChromosome());
    startField.setText("" +getCurSet().getStart());
    endField.setText("" +getCurSet().getEnd());
  }

  public void actionPerformed(ActionEvent evt) {
    //if (frame==null) return; // i dont think apollo frame can be null

    final StateInformation props = new StateInformation();

    // why "old"? isnt this the current adapter
    ApolloDataAdapterI oldAdapter = curationState.getDataAdapter();//frame.getAdapter();
    if (oldAdapter == null) {
      // Why would it be null?
      JOptionPane.showMessageDialog(getFrame(),"Can't load requested region","Warning",
                                    JOptionPane.WARNING_MESSAGE);
      return;
    }
    Properties oldAdapterProps = oldAdapter.getStateInformation();
    Enumeration enu = oldAdapterProps.propertyNames();
    for (; enu.hasMoreElements(); ) {
      String key = (String)enu.nextElement();
      props.put(key,oldAdapterProps.getProperty(key));
    }

    if (getCurSet() == null) { // is this possible?
       return;
    }

    String chr = getCurSet().getChromosome();
    int start = readNumberField(startField);
    int end = readNumberField(endField);
    //int width = getCurSet().getEnd() - getCurSet().getStart();
    int width = end - start;
    boolean doLoad = false;
    // Taking out minus 1 as i think its better to have overlap with previous by one
    // base - this means left of 30000 to 40000 will return 20000 to 30000 (with the
    // overlap of the 30000 base) instead of 19999 to 29999
    if (evt.getSource() == leftButton) {
      end = start;
      start -= width;
    } else if (evt.getSource() == rightButton) {
      // took out + 1 for same reasons as above with -1
      start = end;
      end += width;  // should there be end of chrom check?
    }
    else if (evt.getSource() == loadButton ||
             evt.getSource() == chrField.getComponent() ||
             evt.getSource() == startField ||
             evt.getSource() == endField) {
      chr = chrField.getChromosome();
      start  = readNumberField(startField);
      end    = readNumberField(endField);
      doLoad = true;

    } else if (evt.getSource() == expandButton) {
      // changed from width/2 -> width/4
      start -= width/4;
      end += width/4;
    }

    // make 0 and negatives 1
    if (start < 1)
      start = 1;
    if (end < 1)
      end = 1; // 2?
    // disable leftButton if start==1
    leftButton.setEnabled(start!=1);

    // set fields in gui
    chrField.setChromosome(chr);
    startField.setText(String.valueOf(start));
    endField.setText(String.valueOf(end));

    loadButton.setEnabled(true);  // Now you can load

    if (doLoad) {
      String error=null;
      // check if the query is the same as the present set
      if (getCurSet()!=null && 
          getCurSet().getChromosome()!=null && 
          getCurSet().getChromosome().equals(chr) &&
          getCurSet().getStart()==start && 
          getCurSet().getEnd()==end) {
        error = "Region already loaded";
      } else if (chr == null || chr.equals("")) {
        error = "No chromosome selected";
      } else if (start >= end) {
        error = "Start >= End";
      }
      if (error!=null) {
        error += "\nLoad cancelled";
        JOptionPane.showMessageDialog(getFrame(),error,"Warning",
                                      JOptionPane.WARNING_MESSAGE);
      } else {
        loadButton.setEnabled(false);  // No more loading for now
        String rangeString = "Chr "+chr+" "+start+" "+end;
        props.put(StateInformation.REGION,rangeString);
	//        logger.debug(props.getProperty("region"));
        // this is needed?
        props.put(StateInformation.DATA_ADAPTER,oldAdapter.getClass().getName());
        props.put(StateInformation.INPUT_TYPE,DataInputType.BASEPAIR_RANGE.toString());
        props.put(StateInformation.INPUT_STRING,rangeString);

        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        LoadUtil.loadWithProgress(getFrame(),props,true);
	// Cursor will get set back to default when load is finished
	//	frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }
    }
  }

  private int readNumberField (JTextField field) {
    // parseInt throws runtime error NumberFormatException - should this be caught
    // is it possible to have non ints in there? - its ok because theres a number
    // filter on the field
    int val = (field.getText() == null || field.getText().equals("")
               ? 1 : Integer.parseInt(field.getText()));
    return val;
  }

  private CurationSet getCurSet() {
    //return set;
    return curationState.getCurationSet();
  }

  public void setFont(Font f) {
    this.font = f;
  }

  public Font getFont() {
    return font;
  }

  /** Part of ControlledObjectI and ControlledPanel, which ensure removal as listener
      on window closing, but controller comes from CurationState so this is a no-op 
      to satisfy the interface */
  public void setController(Controller controller) {
    //this.controller = controller;
    //controller.addListener(this);
  }

  /** from ControlledObjectI interface. just returns curationState controller */
  public Controller getController() {
    return curationState.getController();//controller;
  }

  // --> handleDataLoadEvent
//   public void setCurationSet(CurationSet set) {
//     this.set = set; // setCurationSet(null) works for nullifying
//     if (set != null) {
//       if (getCurSet().getStart() == 1) {
//         leftButton.setEnabled(false);
//       } else {
//         leftButton.setEnabled(true);
//       }
//       updateFields();
//       loadButton.setEnabled(false);  // Load button is disabled until user does something to change the requested region
//     }
//   }

  public boolean handleDataLoadEvent(DataLoadEvent evt) {
    if (!evt.dataRetrievalDone()) 
      return false; // only interested if cur state got the latest data
    
    if (getCurSet().getStart() == 1) {
      leftButton.setEnabled(false);
    } else {
      leftButton.setEnabled(true);
    }
    updateFields();
    // Load button is disabled until user does something to change the requested region
    loadButton.setEnabled(false);  
    
    return true;
  }

  private ApolloFrame getFrame() { return ApolloFrame.getApolloFrame(); }

  /** apollo.gui.menu.ViewMenu uses this */
  public Action getNavigationAction() {
    return navigatorManager.getAction();
  }

  /** NavigatorManager inner class, for navigating up and down chromosome
   Just wraps MovementPanel movementPanel, which is the gui for navigation*/
  private class NavigatorManager {

    private NavigationAction action;
    private NavigatorManager() {
      action = new NavigationAction(getNavigationMenuString());
      action.setEnabled(true);
    }

    private Action getAction() {
      return action;
    }

    private String getNavigationMenuString() {
      if (isVisible() == true) {
        return "Hide navigation bar";
      } else {
        return "Show navigation bar";
      }
    }

    private void setNavVisible(boolean state) {
      setVisible(state);
      action.putValue(Action.NAME,getNavigationMenuString());
    }

    private void setEnabled(boolean state) {
      if (state == true) {
        action.setEnabled(true);
        setNavVisible(true);
      } else {
        setNavVisible(false);
        action.setEnabled(false);
      }
    }

    /** inner inner class for action passed to navigation manager */
    private class NavigationAction extends AbstractAction {
      private NavigationAction(String name)  {
        super(name);
      }
      public void actionPerformed(ActionEvent evt) {
        if (isVisible()) {
          setNavVisible(false);
        } else {
          setNavVisible(true);
        }
      }
    }
  }

}
