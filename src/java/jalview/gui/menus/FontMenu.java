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
import jalview.gui.event.*;
import jalview.gui.*;

import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.io.*;

public class FontMenu extends FrameMenu {

  //FastDrawAction fastDraw;

  /** FontFaceAction is an inner class */
  FontFaceAction helvAction;
  FontFaceAction courAction;
  FontFaceAction timesAction;
  JalToggleActionGroup faceGroup;

  FontStyleAction plain;
  FontStyleAction bold;

  static int []   sizes = {1,2,4,6,8,10,12,14,16,20,24};

  FontSizeActionVector sizeActions;

  static Hashtable styleHash = new Hashtable();
  static {
    styleHash.put("Plain",new Integer(Font.PLAIN));
    styleHash.put("Bold",new Integer(Font.BOLD));
  }

  public FontMenu(AlignFrame frame,AlignViewport av,Controller c) {
    super("Font",frame,av,c);
  }

  protected void init() {
    // fast draw is now done automatic by checking for fixed width font 
    // in DrawableSequence
    //fastDraw  = new FastDrawAction("Fast Draw",true);

    courAction  = new FontFaceAction("Courier(faster)","Courier",false);
    helvAction  = new FontFaceAction("Helvetica(slower)","Helvetica",false);
    timesAction = new FontFaceAction("Times-Roman(slower)","TimesRoman",false);

    //add(fastDraw);
    addSeparator();

    add(helvAction);
    add(courAction);
    //add(helvAction);
    add(timesAction);
    addSeparator();

    int curSize = av.getFont().getSize();
    sizeActions = new FontSizeActionVector(sizes,curSize);

    for (int i=0; i<sizeActions.size(); i++) {
      add(sizeActions.get(i));
    }

    faceGroup = new JalToggleActionGroup();
    faceGroup.add(courAction); // first added is default
    faceGroup.add(helvAction);
    faceGroup.add(timesAction);

    faceGroup.setEnabled(false);
    // This doesnt work - default goes to first in group
    faceGroup.setDefault(courAction);

    addSeparator();
    plain = new FontStyleAction("Plain",true);
    bold  = new FontStyleAction("Bold",false);

    add(plain);
    add(bold);

    JalToggleActionGroup styleGroup = new JalToggleActionGroup();
    styleGroup.add(bold);
    styleGroup.add(plain);
  }

  public class FontStyleAction extends JalToggleAction {
    public FontStyleAction(String name,boolean state) {
      super(name,state);
      if (!styleHash.containsKey(name)) {
        System.err.println("Error: Unknown font style " + name + ". This could cause problems");
      }
    }
    public void applyAction(ActionEvent evt) {
      Font f   = new Font(av.getFont().getName(),
                          ((Integer)styleHash.get(name)).intValue(),
                          av.getFont().getSize());

      av.setFont(f);
      controller.handleFontChangeEvent(new FontChangeEvent(this,f));
    }
  }

  // This is now done by checking for fixed width in DrawableSequence
//   public class FastDrawAction extends JalToggleAction {
//     public FastDrawAction(String name, boolean state) {
//       super(name,state);
//     }
//     public void applyAction(ActionEvent evt) {
//       faceGroup.setEnabled(!fastDraw.getState());
//       av.getAlignment().setFastDraw(fastDraw.getState());
//       Font f   = new Font("Courier",
//                           av.getFont().getStyle(),
//                           av.getFont().getSize());

//       av.setFont(f);
//       controller.handleFontChangeEvent(new FontChangeEvent(this,f));
//     }
//   }
  public class FontFaceAction extends JalToggleAction {
    String fontName;
/**
 * Note there are two constructors - one for when the fontName is NOT
 * the same as the menu item name and one for when it IS.
 */
    public FontFaceAction(String name,boolean state) {
      this(name,name,state);
    }
    public FontFaceAction(String name, String fontName, boolean state) {
      super(name,state);
      this.fontName = new String(fontName);
      //name += isFixedWidth() ? " (faster)" : " (slower)";
      //setName(name);
    }
    public void applyAction(ActionEvent evt) {
      Font f   = new Font(fontName,
                          av.getFont().getStyle(),
                          av.getFont().getSize());

      av.setFont(f);
      controller.handleFontChangeEvent(new FontChangeEvent(this,f));
    }
    
    // This doesnt work - graphics is null - how do i get a font metrics?
    private boolean isFixedWidth() {
      Graphics g = getGraphics(); // returns null
      Font currentFont = g.getFont();
      Font thisFont = new Font(fontName,
                          av.getFont().getStyle(),
                          av.getFont().getSize());
      g.setFont(thisFont);
      boolean isFixed = DrawableSequence.isFixedWidthFont(g.getFontMetrics());
      g.setFont(currentFont);
      return isFixed;
    }

  }


  public class FontSizeAction extends JalToggleAction {
    int fontSize;

    public FontSizeAction(String name, int size, boolean state) {
      super(name,state);
      this.fontSize = size;
    }

    public void applyAction(ActionEvent evt) {

      // av is AlignViewport
      Font f   = new Font(av.getFont().getName(),
                          av.getFont().getStyle(),
                          fontSize);

      av.setFont(f);
      controller.handleFontChangeEvent(new FontChangeEvent(this,f));
    }
  }

  class FontSizeActionVector {
    Vector actions = new Vector();
  
    public FontSizeActionVector(int [] sizes,int curSize) {
      JalToggleActionGroup sizeGroup = new JalToggleActionGroup();
      for (int i=0; i<sizes.length; i++) {
        FontSizeAction action = new FontSizeAction("Size = " + sizes[i], sizes[i],(sizes[i] == curSize));
        actions.addElement(action);
        sizeGroup.add(action);
      }
    }
  
    public int size() {
      return actions.size();
    }
  
    public FontSizeAction get(int i) {
      return (FontSizeAction)actions.elementAt(i);
    }
  
    public boolean contains(Object obj) {
      return actions.contains(obj);
    }
  }
}

