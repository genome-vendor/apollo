/* Jalview - a java multiple alignment editor
 * Copyright (C) 1998  Michele Clamp
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

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

public class EditMenu extends FrameMenu {

  UndoAction           undo;
  RedoAction           redo;
  GroupEditAction      groupEdit;
  GroupsAction         groups;
  SelectAllSeqAction   selectAllSeq;
  DeselectAllSeqAction deselectAllSeq;
  InvertSeqSelAction   invertSeqSelection;
  DeleteSelSeqAction   deleteSelectedSeq;
  MoveSelSeqAction     cutSelectedSeq;
  CopySelSeqAction     copySelectedSeq;
  DeselectColsAction   deselectAllCol;
  TrimLeftAction       trimLeft;
  TrimRightAction      trimRight;
  RemoveGapColAction   removeGapCols;
  GapCharAction        gapChar;

  public EditMenu(AlignFrame frame,AlignViewport av,Controller c) {
    super("Edit",frame,av,c);
  }

  protected void init() {
    undo               = new UndoAction("Undo");
    redo               = new RedoAction("Redo");
    groupEdit          = new GroupEditAction("Group editing mode",false);
    groups             = new GroupsAction("Groups...");
    selectAllSeq       = new SelectAllSeqAction("Select all sequences");
    deselectAllSeq     = new DeselectAllSeqAction("Deselect all sequences");
    invertSeqSelection = new InvertSeqSelAction("Invert sequence selection");
    deleteSelectedSeq  = new DeleteSelSeqAction("Delete selected sequences");
    cutSelectedSeq     = new MoveSelSeqAction("Move selected sequences to new alignment");
    copySelectedSeq    = new CopySelSeqAction("Copy selected sequences to new alignment");
    deselectAllCol     = new DeselectColsAction("Deselect all columns");
    trimLeft           = new TrimLeftAction("Remove sequence <- left of selected columns");
    trimRight          = new TrimRightAction("Remove sequence -> right of selected columns");
    removeGapCols      = new RemoveGapColAction("Remove gapped columns");
    gapChar            = new GapCharAction("Set gap character to .","Set gap character to -",
                                           av.getGapCharacter().equals("."));

    if (frame.showEverything()) {
      add(undo);
      add(redo);
      addSeparator();
      add(groups);
    }
    if (!frame.getReadOnlyMode()) add(groupEdit);
    addSeparator();
    add(selectAllSeq);
    add(deselectAllSeq);
    add(invertSeqSelection);
    addSeparator();
    add(deleteSelectedSeq);
    if (frame.showEverything()) {
      // These have a null pointer that was proabbly introduced by the apollo linking
      // Hope to fix them soon as its a nice thing to have
      add(cutSelectedSeq);
      add(copySelectedSeq);
    }
    addSeparator();
    add(deselectAllCol);
    add(trimLeft);
    add(trimRight);
    addSeparator();
    add(removeGapCols);
    add(gapChar);
  }

  class UndoAction extends JalAction {
    public UndoAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
    }
  }
  class RedoAction extends JalAction {
    public RedoAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
    }
  }
  class GapCharAction extends JalTwoStringToggleAction {
    public GapCharAction(String name,String altName,boolean state) {
      super(name,altName,state);
    }
    public void applyAction(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"Changing gap character...",StatusEvent.INFO));
      
      if (getState()) {
        av.setGapCharacter("."); 
      } else {
        av.setGapCharacter("-"); 
      }
    }
  }

  class GroupEditAction extends JalToggleAction {
    public GroupEditAction(String name,boolean state) {
      super(name,state);
    }
    public void applyAction(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"Setting group edit mode...",StatusEvent.INFO));

      av.setGroupEdit(getState());

      controller.handleStatusEvent(new StatusEvent(this,"done",StatusEvent.INFO));
    }
  }

  class GroupsAction extends JalAction {
    public GroupsAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {

      controller.handleStatusEvent(new StatusEvent(this,"Creating group edit window...",StatusEvent.INFO));
 
      GroupPopup gp = new GroupPopup(frame,av,controller,"Group properties",av.getAlignment());
 
      // controller.handleStatusEvent(new StatusEvent(this,"done",StatusEvent.INFO));
    }
  }

  class SelectAllSeqAction extends JalAction {
    public SelectAllSeqAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"Selecting all sequences...",StatusEvent.INFO));
 
      Selection sel = av.getSelection();
      for (int i=0; i<av.getAlignment().getSequences().size(); i++) {
        sel.addElement( av.getAlignment().getSequenceAt(i));
      }

      fireSequenceSelectionEvent(new SequenceSelectionEvent(this,av.getSelection()));

      controller.handleStatusEvent(new StatusEvent(this,"Selected all sequences",StatusEvent.INFO));
    }
  }

  class DeselectAllSeqAction extends JalAction {
    public DeselectAllSeqAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"Deselecting all sequences...",StatusEvent.INFO));
 
      av.getSelection().clear();

      fireSequenceSelectionEvent(new SequenceSelectionEvent(this,av.getSelection()));

      controller.handleStatusEvent(new StatusEvent(this,"Deselected all sequences",StatusEvent.INFO));
    }
  }

  class InvertSeqSelAction extends JalAction {
    public InvertSeqSelAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"Inverting sequence selection...",StatusEvent.INFO));
 
      Selection sel = av.getSelection();
      for (int i=0; i<av.getAlignment().getSequences().size(); i++) {
        if (sel.contains( av.getAlignment().getSequenceAt(i))) {
          sel.removeElement(av.getAlignment().getSequenceAt(i));
        } else {
          sel.addElement( av.getAlignment().getSequenceAt(i));
        }
      }
      fireSequenceSelectionEvent(new SequenceSelectionEvent(this,av.getSelection()));

      controller.handleStatusEvent(new StatusEvent(this,"Inverted sequence selection",StatusEvent.INFO));
    }
  }

  protected void fireSequenceSelectionEvent(SequenceSelectionEvent evt) {
    controller.handleSequenceSelectionEvent(evt);
  }

  abstract class ProcessSelSeqAction extends JalAction {

    public ProcessSelSeqAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      if (av.getSelection().size() > 0) {
        controller.handleStatusEvent(new StatusEvent(this,getStatusText(),StatusEvent.INFO));
   
        processSelection(evt);

        controller.handleStatusEvent(new StatusEvent(this,"done",StatusEvent.INFO));
      } else {
        controller.handleStatusEvent(new StatusEvent(this,"No sequences selected...",StatusEvent.ERROR));
      }
    }
    protected abstract void processSelection(ActionEvent evt);
    protected abstract String getStatusText();
  }
  class DeleteSelSeqAction extends ProcessSelSeqAction {
    public DeleteSelSeqAction(String name) {
      super(name);
    }
    protected void processSelection(ActionEvent evt) {
 
      deleteSelection();

      controller.handleAlignViewportEvent(new AlignViewportEvent(this,av,AlignViewportEvent.DELETE));
    }

    protected void deleteSelection() {
      for (int i=0;i < av.getSelection().size(); i++) {
        av.getAlignment().deleteSequence(av.getSelection().sequenceAt(i));
      }
      av.getSelection().clear();
      av.resetSeqLimits();
    }
    protected AlignSequenceI [] copySelection() {
      AlignSequenceI[] s = new DrawableSequence[av.getSelection().size()];
      for (int i=0; i < av.getSelection().size(); i++) {
        s[i] = new DrawableSequence(av.getSelection().sequenceAt(i));
      }
      return s;
    }

    protected String getStatusText() {
      return ("Deleting selected sequences...");
    }
  }

  class MoveSelSeqAction extends DeleteSelSeqAction {
    public MoveSelSeqAction(String name) {
      super(name);
    }
    protected void processSelection(ActionEvent evt) {
 
      AlignSequenceI[] s = copySelection();
      deleteSelection();

      controller.handleStatusEvent(new StatusEvent(this,"Creating new alignment window...",StatusEvent.INFO));
 
      AlignFrame af = new AlignFrame(this,new Alignment(s));
//      Font f = av.getFont();
//      af.setFont(f);
      af.resize(700,500);
      af.show();
    }
    protected String getStatusText() {
      return ("Moving selected sequences...");
    }
  }

  class CopySelSeqAction extends MoveSelSeqAction {
    public CopySelSeqAction(String name) {
      super(name);
    }

    protected void processSelection(ActionEvent evt) {
 
      AlignSequenceI[] s = copySelection();

      controller.handleStatusEvent(new StatusEvent(this,"Creating new alignment window...",StatusEvent.INFO));
 
      AlignFrame af = new AlignFrame(this,new Alignment(s));
//      Font f = av.getFont();
//      af.setFont(f);
      af.resize(700,500);
      af.show();
    }
    protected String getStatusText() {
      return ("Copying selected sequences...");
    }
  }

  class DeselectColsAction extends JalAction {
    public DeselectColsAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {

      controller.handleStatusEvent(new StatusEvent(this,"Deselecting all columns...",StatusEvent.INFO));

      av.getColumnSelection().clear();
      controller.handleColumnSelectionEvent(new ColumnSelectionEvent(this,av.getColumnSelection()));

      controller.handleStatusEvent(new StatusEvent(this,"Deselected all columns",StatusEvent.INFO));
    }
  }

  class TrimRightAction extends JalAction {
    public TrimRightAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      ColumnSelection colSel = av.getColumnSelection();

      if (colSel.size() > 0) {
        controller.handleStatusEvent(new StatusEvent(this,"Trimming sequences right...",StatusEvent.INFO));
 
        int max = colSel.getMax();
 
        if (max >= 0) {
 
          av.getAlignment().trimRight(max);
          controller.handleAlignViewportEvent(new AlignViewportEvent(this,av,AlignViewportEvent.DELETE));
 
// SMJS Why???
//          for (int i=0; i < ap.selectedColumns.size();i++) {
//            int temp = ((Integer)ap.selectedColumns.elementAt(i)).intValue();
//            if (temp > max) {
//              ap.selectedColumns.removeElementAt(i);
//            }
//          }
          controller.handleStatusEvent(new StatusEvent(this,"Trimmed right of " + max,StatusEvent.INFO));
        }
      } else {
        controller.handleStatusEvent(new StatusEvent(this,"No columns selected",StatusEvent.ERROR));
      }
    }
  }
  class TrimLeftAction extends JalAction {
    public TrimLeftAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      ColumnSelection colSel = av.getColumnSelection();

      if (colSel.size() > 0) {

        controller.handleStatusEvent(new StatusEvent(this,"Trimming sequences left...",StatusEvent.INFO));
 
        int min = colSel.getMin();
 
        if (min < av.getAlignment().getWidth()) {
 
          av.getAlignment().trimLeft(min);

          colSel.compensateForEdit(0,min);
          controller.handleAlignViewportEvent(new AlignViewportEvent(this,av,AlignViewportEvent.DELETE));

          controller.handleStatusEvent(new StatusEvent(this,"Trimmed left of " + min,StatusEvent.INFO));
        }
      } else {
        controller.handleStatusEvent(new StatusEvent(this,"No columns selected",StatusEvent.ERROR));
      } 
    }
  }

  class RemoveGapColAction extends JalAction {
    public RemoveGapColAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"Removing gaps...",StatusEvent.INFO));
 
      av.getAlignment().removeGappedColumns();

      controller.handleAlignViewportEvent(new AlignViewportEvent(this,av,AlignViewportEvent.DELETE));
 
      controller.handleStatusEvent(new StatusEvent(this,"done",StatusEvent.INFO));
    }
  }
}
