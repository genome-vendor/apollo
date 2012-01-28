package apollo.gui.tweeker;

import java.awt.Dimension;
import javax.swing.BoundedRangeModel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.BoxLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import apollo.util.GuiUtil;
import java.awt.GridBagLayout;

import javax.swing.JComponent;

//import apollo.datamodel.SeqFeatureI;
import apollo.gui.ControlledObjectI;
import apollo.gui.Controller;
import apollo.gui.SliderWindow;
import apollo.gui.genomemap.StrandedZoomableApolloPanel;
import apollo.dataadapter.DataLoadListener;
import apollo.dataadapter.DataLoadEvent;
import apollo.gui.synteny.CurationManager;
import apollo.gui.synteny.GuiCurationState;


/** Tweeker has a JFrame with a JTabbedPane. Each tab tweeks something.
 * GC window size for gc graph, restriction enzyme selection, result thresholds 
 * For now this works with the active cur set. Eventually there should be a drop down 
 * list of cur set names to choose from.  (Why?) */
public class Tweeker extends JFrame implements ActionListener {
  public static int frameWidth = 520;
  public static int frameHeight = 275;  // room for table
  public final static int GC = 0;
  public final static int RESTRICTION = 1;

  private final static Dimension frameSize = new Dimension(frameWidth, frameHeight);
  private JCheckBox gcplot = new JCheckBox("Show GC content graph");
  private JButton changeColour = new JButton("Change graph color");
  private JPanel gcSliderPanel;
  private RestrictionEnzymeSelector resEnzSel;
  private JTabbedPane tabbedPane;
  private TweekerWindowControl winControl;
  private static Tweeker tweekerSingleton;

  public static void openTweeker(int type) {
    if (tweekerSingleton == null)
      tweekerSingleton = new Tweeker();
    else
      tweekerSingleton.clear();

    tweekerSingleton.initGui(type);
  }

  public Tweeker() {
    super("Sequence Analysis Controls");
    //initGui(type);
    //winControl = new TweekerWindowControl(szap.getController());
  }
  
  private GuiCurationState getCurState() { 
    return CurationManager.getCurationManager().getActiveCurState(); 
  }
  private StrandedZoomableApolloPanel getSZAP() { return getCurState().getSZAP(); }

  private void initGui(int type) {
    tabbedPane = new JTabbedPane();

    //gcSliderPanel = new SliderWindow(gcModel);

    JPanel slider = new SliderWindow(getActiveBoundedRangeModel());

    /* Problem: after you choose a restriction enzyme, if you then go back
       to the GC Window Size pane, everything is next to each other on one
       line.  How to keep that from happening? */

    // Using an enclosing JPanel with GridBagLayout instead of a BorderLayout with
    // two SOUTH components seems to help
    gcSliderPanel = new JPanel();
    gcSliderPanel.setLayout(new GridBagLayout());

    gcSliderPanel.add(slider, GuiUtil.makeConstraintAt(0,0,0));
    gcSliderPanel.add(gcplot, GuiUtil.makeConstraintAt(0,1,0));
    gcplot.setSelected(getSZAP().getGraphVisibility());
    gcSliderPanel.add(changeColour, GuiUtil.makeConstraintAt(0,2,0));
    gcplot.addActionListener(this);


    changeColour.addActionListener(this);

    resEnzSel = new RestrictionEnzymeSelector(this);//,revComp);

    String tip = "Set size of window in calculating GC graph";
    if (type == RESTRICTION) {
      resEnzSel.addToTabbedPane(tabbedPane); // adds itself
      tabbedPane.addTab("GC Window Size",null,gcSliderPanel,tip);
    }
    else {
      tabbedPane.addTab("GC Window Size",null,gcSliderPanel,tip);
      resEnzSel.addToTabbedPane(tabbedPane); // adds itself
    }

    getContentPane().add(tabbedPane);
    setSize(frameSize);


    winControl = new TweekerWindowControl(getCurState().getController());

    setVisible(true);
    toFront();
    selectTab(type);

    if (getState()==Frame.ICONIFIED) // linux iconifying issue
      setState(Frame.NORMAL);
  }

  private BoundedRangeModel getActiveBoundedRangeModel() {
    return getSZAP().getGraphView().getScoreCalculator().getModel();
  }

  // Called when "show GC plot" is called from analysis menu
  private void selectTab(int type) {
    if (type == GC) {
      tabbedPane.setSelectedComponent(gcSliderPanel);
      gcplot.setSelected(true);
      getSZAP().setGraphVisibility(gcplot.isSelected());
      // Neither of these helps restore window to its smaller size, oh well
      //
      // Note the reason for this is that the tabbedPane is set to the size of its largest contained
      // panel which isn't necessarily the panel currently displayed. 
      //      pack();    
      //      tabbedPane.pack();
    }
    else {
      //      tabbedPane.setSelectedComponent(resEnzSel);  // can't do
      int which_is_gc = tabbedPane.indexOfComponent(gcSliderPanel);
      tabbedPane.setSelectedIndex(1 - which_is_gc);  // kludge
    }
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == gcplot) {
      getSZAP().setGraphVisibility(gcplot.isSelected());
    } 
    else if (e.getSource() == changeColour) {
      Color colour = JColorChooser.showDialog(this,
                                             "Change plot colour",
                                             getSZAP().getGraphView().getPlotColour());
      if (colour != null) {
	boolean visibleNow = getSZAP().getGraphVisibility();
	// to make the graph to repaint in the new colour
	if (visibleNow)
	  getSZAP().setGraphVisibility(false);
        getSZAP().getGraphView().setPlotColour(colour);
	if (visibleNow)
	  getSZAP().setGraphVisibility(true);
      }
    }
  }

  /** Remove as listener, dispose window */
    private void clear() { winControl.clear(); }

  /** ControlledObjectI class that gets tweeker in window list menu */
  private class TweekerWindowControl extends java.awt.event.WindowAdapter 
    implements ControlledObjectI, java.util.EventListener, DataLoadListener {
    private Controller controller;
    private TweekerWindowControl(Controller c) {
      setController(c);
      Tweeker.this.addWindowListener(this); // windowClosing events
    }
    /** Sets the Controller for the object - ControlledObjectI interface */
    public void setController(Controller controller) {
      this.controller = controller;
      controller.addListener(this);
    }
    /** Gets the Controller for the object */
    public Controller getController() {
      return controller;
    }
    public Object     getControllerWindow() {
      return Tweeker.this;
    }
    /** Returns true so controller will remove as listener on window closing */
    public boolean    needsAutoRemoval() {
      //return false;
      return true; 
    }

    /** DataLoadListener method. Region changing means a new data set is 
	being loaded. Get rid of the tweeker */
    public boolean handleDataLoadEvent(DataLoadEvent e) {
      getSZAP().setGraphVisibility(false);
      clear();
      return true;
    }

    private void clear() {

      // Fix - remove the existing tabbedPane, otherwise when
      // we bring the Tweeker back up we will have 2 tabbed panes
      // which sit on top of one another one with the old GC slider panel in
      // and the new one with both panels in. This lead to some interesting
      // drawing effects
      if (tabbedPane!=null) {
        getContentPane().remove(tabbedPane);
      }

      tabbedPane = null;

      // I hope it won't be a big memory leak not to null this out, but it's a problem
      // when user kills the tweeker window with the X and then tries to reinvoke it
      // using the Analysis window.
//      rootPane = null;
      if (resEnzSel != null)
	  resEnzSel.clear();
      resEnzSel = null;
      Tweeker.this.hide();
      Tweeker.this.dispose();
      controller.removeListener(this);
    }

    /** WindowListener/Adapter - clear on window closing */
    public void windowClosing(java.awt.event.WindowEvent e) {
      clear();
    }
  }
}
