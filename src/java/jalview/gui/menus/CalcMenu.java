package jalview.gui.menus;

import jalview.io.*;
import jalview.datamodel.*;
import jalview.gui.popups.*;
import jalview.analysis.*;
import jalview.util.*;
import jalview.gui.AlignFrame;
import jalview.gui.event.*;
import jalview.gui.*;

import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.io.*;

public class CalcMenu extends FrameMenu {

  AutoConsensusAction autoconsensus;
  ConsensusAction     consensus;
  SortAction          pairIdSort;
  SortAction          idSort;
  SortAction          groupSort;
  SortAction          treeSort;
  RedundancyAction    remRedund;
  PairAlignAction     pairAlign;
  PCAAction           pca;
  AvTreeAction        averTree;
  NeighbTreeAction    neighbTree;
  AvTreeAction        averTree2;
  NeighbTreeAction    neighbTree2;
  ConservationAction  conservation;
  RunCommandAction    runCommands;

  public CalcMenu(AlignFrame frame,AlignViewport av, Controller c) {
    super("Calc",frame,av,c);
  }

  protected void init() {

    if (frame.showEverything()) {
      // These dont seem to be doing anything in apollo mode as far as i can tell
      consensus    = new ConsensusAction("Consensus");
      add(consensus);
      autoconsensus = new AutoConsensusAction("Autocalculate consensus",false);
      add(autoconsensus);
    }

    pairIdSort   = new SortAction("Sort by pairwise identity");
    idSort       = new SortAction("Sort by ID");
    groupSort    = new SortAction("Sort by group");
    treeSort     = new SortAction("Sort by tree order");
    remRedund    = new RedundancyAction("Remove redundancy");
    pairAlign    = new PairAlignAction("Pairwise alignments");
    pca          = new PCAAction("Principal component analysis");
    averTree     = new AvTreeAction("Average distance tree using PID");
    neighbTree   = new NeighbTreeAction("Neighbour joining tree using PID");
    averTree2    = new AvTreeAction("Average distance tree using BLOSUM62");
    neighbTree2  = new NeighbTreeAction("Neighbour joining tree using BLOSUM62");

    averTree2.setScoreType("BL");
    neighbTree2.setScoreType("BL");

    if (frame.showEverything()) {
      // doesnt seem to do anything
      conservation = new ConservationAction("Conservation");
      // what is this for - how do you use it - does it work?
      runCommands  = new RunCommandAction("Run commands");
    }
    addSeparator();

    add(pairIdSort);
    add(idSort);
    add(groupSort);
    add(treeSort);
    add(remRedund);

    addSeparator();

    add(pairAlign);
    add(pca);

    addSeparator();

    add(averTree);
    add(neighbTree);

    add(averTree2);
    add(neighbTree2);

    if (frame.showEverything()) {
      addSeparator();
      // doesnt seem to do anything
      add(conservation);
      
      addSeparator();

      // what is this for - how do you use it - does it work?
      add(runCommands);
    }
  }
  class AutoConsensusAction extends JalToggleAction {
    public AutoConsensusAction(String name, boolean state) {
      super(name,state);
    }
    public void applyAction(ActionEvent evt) {
    }
  }

  class RunCommandAction extends JalAction {
    public RunCommandAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      new CommandPopup(frame,av,controller,getName());
    }
  }

  class ConsensusAction extends JalAction {
    public ConsensusAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
    }
  }

  class SortAction extends JalAction {
    public SortAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      boolean error = false;
      if (name.equals("Sort by pairwise identity")) {
        controller.handleStatusEvent(new StatusEvent(this,"Sorting sequences by PID...",StatusEvent.INFO));
 
        System.out.println("NOTE: Sorts relative to FIRST sequence");
        AlignmentSorter.sortByPID(av.getAlignment(),av.getAlignment().getSequenceAt(0));

      } else if (name.equals("Sort by ID")) {
        controller.handleStatusEvent(new StatusEvent(this,"Sorting sequences by ID...",StatusEvent.INFO));
 
        AlignmentSorter.sortByID(av.getAlignment());

      } else if (name.equals("Sort by group")) {
        controller.handleStatusEvent(new StatusEvent(this,"Sorting sequences by group...",StatusEvent.INFO));
 
        AlignmentSorter.sortGroups(av.getAlignment());
        AlignmentSorter.sortByGroup(av.getAlignment());
      } else if (name.equals("Sort by tree order")) {
        if (av.getCurrentTree() != null) {
          controller.handleStatusEvent(new StatusEvent(this,"Sorting sequences by tree...",StatusEvent.INFO));
   
          AlignmentSorter.sortByTree(av.getAlignment(),av.getCurrentTree());
        } else {
          controller.handleStatusEvent(new StatusEvent(this,"No tree defined",StatusEvent.ERROR));
          error = true;
        }
      }
      if (!error) {
        controller.handleAlignViewportEvent(new AlignViewportEvent(this,av,AlignViewportEvent.ORDER));
        controller.handleStatusEvent(new StatusEvent(this,"done",StatusEvent.INFO));
      }
    }
  }

  class RedundancyAction extends JalAction {
    public RedundancyAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"Creating redundancy chooser...",StatusEvent.INFO));
 
      RedundancyPopup rp = new RedundancyPopup(frame,av,controller,"Redundancy threshold selection","Percent identity",0,100,100);
      controller.handleStatusEvent(new StatusEvent(this,"Redundant sequences removed",StatusEvent.INFO));
    }
  }

  class PairAlignAction extends JalAction {
    public PairAlignAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"Aligning selected sequences",StatusEvent.INFO));
 
      PairAlignThread pat = new PairAlignThread(av,controller);
      pat.start();
    }
  }

  class PCAAction extends JalAction {
    public PCAAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"Starting PCA calculation...",StatusEvent.INFO));
 
      PCAThread pcat = new PCAThread(frame,av,controller,av.getAlignment().getSequences());
      pcat.start();
    }
  }

  class AvTreeAction extends TreeAction {
    public AvTreeAction(String name) {
      super(name);
    }
    protected  String getTitle() {
        return "Average distance tree using PID";
    }
    protected  String getType() {
      return "AV";
    }
  }

  class NeighbTreeAction extends TreeAction {
    public NeighbTreeAction(String name) {
      super(name);
    }
    protected  String getTitle() {
        return "Neighbour joining tree using PID";
    }
    protected  String getType() {
      return "NJ";
    }
  }

  abstract class TreeAction extends JalAction {
      String type      = "AV";
      String scoreType = "PID";

    public TreeAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
	TreeThread tt;

	if (av.getSelection() != null && av.getSelection().size() > 3) {
	    Vector seqs = av.getSelection().asVector();
	    tt = new TreeThread(controller,av,seqs, getType(),getScoreType());
	} else {
	    tt = new TreeThread(controller,av,av.getAlignment().getSequences(), getType(), getScoreType());
      }
      controller.handleStatusEvent(new StatusEvent(this,"Calculating " + getType() + " tree",StatusEvent.INFO));
      tt.start(); 
    }

    protected abstract String getTitle();
    protected abstract String getType();
      protected void setType(String type) {
	  this.type = type;
      }
      protected void setScoreType(String type) {
	  this.scoreType = type;
      }
      protected String getScoreType() {
	  return scoreType;
      }
  }
 

  class ConservationAction extends JalAction {
    public ConservationAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
    }
  }
}
