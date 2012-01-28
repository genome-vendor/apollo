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
import jalview.gui.AlignFrame;
import jalview.util.*;
import jalview.gui.event.*;
import jalview.gui.*;

import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.io.*;

public class ViewMenu extends FrameMenu {

  JalToggleAction colourText;
  JalToggleAction blackText;
  JalToggleAction boxes;
  JalToggleAction text;
  JalToggleAction scores;
  JalToggleAction wrap;
  JalToggleActionGroup textGroup;

  public ViewMenu(AlignFrame frame,AlignViewport av,Controller c) {
    super("View",frame,av,c);
  }

  protected void init() {
    //    frame.setShowScores(false);

    boxes = new ShowAction("Boxes",av.getShowBoxes());
    add(boxes);
    
    if (frame.showEverything()) {
      // I gather this toggles whether text is displayed but it doesnt seem to be working
      text = new ShowAction("Text",av.getShowText());
      add(text);
    }

    if (!frame.getReadOnlyMode()) {
      // scores are not coming up in apollo mode which i think is what we want
      // should there be another flag frame.getApolloMode()?
      scores = new ShowAction("Scores",av.getShowScores());
      add(scores);
    }

    wrap  = new ShowAction("Wrap alignment",av.getWrapAlignment());
    add(wrap);

    addSeparator();

    colourText = new TextColourAction("Colour text",false);
    add(colourText);

    blackText = new TextColourAction("Black text",false);
    add(blackText);

    textGroup = new JalToggleActionGroup();
    textGroup.add(blackText);
    textGroup.add(colourText);
  }
  public class ShowAction extends JalToggleAction {
    public ShowAction(String name, boolean state) {
      super(name,state);
    }

    public void applyAction(ActionEvent evt) {
      if (name.equals("Scores")) {
        av.setShowScores(getState());
      } else if (name.equals("Text")) {
        av.setShowText(getState());
        av.getAlignment().setDisplayText(getState());
        if (!getState()) {
          textGroup.setDefault(textGroup.getCurrent());
        }
        textGroup.setEnabled(getState());
        if (!getState()) {
          textGroup.setNoSelection();
        }
      } else if (name.equals("Boxes")) {
        av.setShowBoxes(getState());
        av.getAlignment().setDisplayBoxes(getState());
	if (Config.DEBUG) System.out.println("Setting " + name + " to " + getState());
	controller.handleAlignViewportEvent(new AlignViewportEvent(this,av,AlignViewportEvent.SHOW));

      } else if (name.equals("Wrap alignment")) {
	  av.setWrapAlignment(getState());
	  if (Config.DEBUG) 
	    System.out.println("Setting " + name + " to " + getState());
	  controller.handleAlignViewportEvent(new AlignViewportEvent(this,av,AlignViewportEvent.WRAP));
      }
    }
  }
  public class TextColourAction extends JalToggleAction {
    public TextColourAction(String name,boolean state) {
      super(name,state);
    }

    public void applyAction(ActionEvent evt) {
      if (name.equals("Black text")) {
        setTextColouring(false);
      } else if (name.equals("Colour text")) {
        setTextColouring(true);
      }
      controller.handleAlignViewportEvent(new AlignViewportEvent(this,av,AlignViewportEvent.COLOURING));
    }
  }
  public void setTextColouring(boolean state) {
    if (Config.DEBUG)  System.out.println("Setting text colouring to " + state);
    av.getAlignment().setColourText(state);
  }

}
