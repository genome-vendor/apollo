package jalview.gui.menus;

import jalview.io.*;
import jalview.datamodel.*;
import jalview.gui.popups.*;
import jalview.analysis.*;
import jalview.gui.AlignFrame;
import jalview.gui.schemes.*;
import jalview.util.*;
import jalview.gui.event.*;
import jalview.gui.*;
import jalview.analysis.*;

import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.io.*;

public class ColourMenu extends FrameMenu  {

  ColourAction [] schemeActions;
  ColourAction    conservation;
  //FetchSeqAction  features;
  //FetchPDBAction  structures;
  //ConsIncAction   incitem;
  PIDThreshAction PIDthreshold;
  UserColoursAction userColours;
  JalToggleActionGroup colourGroup;

  public ColourMenu(AlignFrame frame,AlignViewport av,Controller c) {
    super("Colour",frame,av,c);
  }

  /**
   * Factory method to create MenuItems for colour schemes
   */
  protected void createSchemeActions() {
    Vector schemeNames = ColourProperties.getColourSchemeNames();
  
    ColourAction [] tmpActions = new ColourAction[schemeNames.size()];
    colourGroup                = new JalToggleActionGroup();

    int nAdded = 0;

    try {
      for (int i=0; i < schemeNames.size(); i++) {

	String menuStr = ColourProperties.getMenuString(i);

	//System.out.println("Menu string " + menuStr);

  
	if (menuStr != null) {
	  String classStr     = ColourProperties.getClassName(i);
	  String schemeName   = (String)schemeNames.elementAt(i);
	  ColourSchemeI dummy = ColourSchemeFactory.get(schemeName);

	  Class  superClassName = Class.forName(classStr).getSuperclass();

	  //System.out.println("super name " + superClassName);

	  if (superClassName.equals(ScoreColourScheme.class)) {
	    tmpActions[nAdded] = new ScoreColourAction(menuStr, 
						       schemeName,
						       false);

	  } else if (superClassName.equals(ResidueColourScheme.class)) {
	    //System.out.println("Found residue colour scheme");
	    tmpActions[nAdded] = new ResidueColourAction(menuStr, 
							 schemeName,
							 false);
	    //System.out.println("Added residueColourAction");

	  } else {
	    System.out.println("NOTE: Basic colour action for " + classStr);
	    tmpActions[nAdded] = new ColourAction(menuStr, 
						  schemeName,
						  false);
	    
	  }

	  if (dummy.canThreshold()) {
	    PIDthreshold.addEnabler(tmpActions[nAdded]);
	  } else {
	    PIDthreshold.addDisabler(tmpActions[nAdded]);
	  }

	  if (dummy.isUserDefinable()) {
	    userColours.addEnabler(tmpActions[nAdded]);
	  } else {
	    userColours.addDisabler(tmpActions[nAdded]);
	  }

          
	  add(tmpActions[nAdded]);
	  colourGroup.add(tmpActions[nAdded]);
	  nAdded++;
	}
      }

      schemeActions = new ColourAction[nAdded];
      System.arraycopy(tmpActions,0,schemeActions,0,nAdded);
    } catch (Exception e) {
      System.err.println("Failed creating ColourScheme menu options");
      e.printStackTrace();
    }
  }
    
  protected void init() {

    // MUST BE CREATED BEFORE createSchemeActions

    PIDthreshold = new PIDThreshAction         ("Set minimum percent identity for match...");
    userColours  = new UserColoursAction       ("Define new colour scheme...");
    //incitem      = new ConsIncAction           ("Conservation colour increment...");
    //conservation = new ConservationColourAction("By conservation","conservation",false);
    //features     = new FetchSeqAction          ("Fetch sequence features");
    //structures   = new FetchPDBAction          ("Fetch PDB structure");

    createSchemeActions();

    //addSeparator();

    //add(conservation);
    //conservation.setState(false);

    //incitem.addEnabler(conservation);
    //PIDthreshold.addDisabler(conservation);
    //userColours.addDisabler(conservation);

    addSeparator();

    add(PIDthreshold);

    //add(incitem);
    //incitem.setEnabled(false);

    add(userColours);

    //add(features);
    //add(structures);
  }


  public class ColourAction extends JalToggleAction {
    String schemeName;
    
    public ColourAction(String name,String schemeName,boolean state) {
      super(name,state);
      this.schemeName = new String(schemeName);
    }
    public void applyAction(ActionEvent evt) {
      setColourScheme(ColourSchemeFactory.get(schemeName));
    }
    public void setColourScheme(ColourSchemeI cs) {
      av.getAlignment().setColourScheme(cs);
      controller.handleAlignViewportEvent(new AlignViewportEvent(this,av,AlignViewportEvent.COLOURING));
    }
  }

  
  public class ScoreColourAction extends ColourAction {
    public ScoreColourAction(String name,String schemeName,boolean state) {
      super(name,schemeName,state);
    }
    
    public void applyAction(ActionEvent evt) {
      setColourScheme(ColourSchemeFactory.get(schemeName));
    }
  }

  public class ResidueColourAction extends ColourAction {
    public ResidueColourAction(String name,String schemeName,boolean state) {
      super(name,schemeName,state);
    }
    public void applyAction(ActionEvent evt) {
      setColourScheme(ColourSchemeFactory.get(schemeName));
    }
    
  }

  public class PIDThreshAction extends JalAction implements JalActionListener {

    public PIDThreshAction(String name) {
      super(name);
    }

    public void applyAction(ActionEvent evt) {
      if (Config.DEBUG) System.out.println("Got a PIDThreshAction");

      StatusEvent sevt = new StatusEvent(this,"Creating PID threshold chooser",StatusEvent.INFO);

      controller.handleStatusEvent(sevt);
 

      PercentIdentityPopup pip = new PercentIdentityPopup(frame,
							  av,
							  controller,
							  "PID threshold selection",
							  "Percent identity",
							  0,
							  100,
							  0);

    }
  }

  public class UserColoursAction extends JalAction {
    public UserColoursAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"Creating user colour chooser...",StatusEvent.INFO));
 
// was ap.color instead of ResidueProperties.color
      ColourChooserFrame ccf = new ColourChooserFrame(frame,av,controller,ResidueProperties.color);
      ccf.show();

      controller.handleStatusEvent(new StatusEvent(this,"done",StatusEvent.INFO));
    }
  }
}
