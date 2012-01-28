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
package jalview.gui;

import java.awt.*;
import java.util.*;

public class ColourPanelDriver extends Driver {
  public static void main(String[] args) {
    Frame f = new Frame();
    Panel p = new Panel();
    ColourPanel[] cp = new ColourPanel[4];
    cp[0] = new ColourPanel(Color.red,"ILV");
    cp[1] = new ColourPanel(Color.pink,"FWY");
    cp[2] = new ColourPanel(Color.orange,"ST");
    cp[3] = new ColourPanel(Color.yellow,"GP");

    ColorSelectPanel cs = new ColorSelectPanel();

    p.setLayout(new GridLayout(5,1));
    p.add(cp[0]);
    p.add(cp[1]);
    p.add(cp[2]);
    p.add(cp[3]);

    f.setLayout(new BorderLayout());

    f.add("North",p);
    f.add("South",cs);

    f.resize(cp[0].getPreferredSize().width,cp.length*cp[0].getPreferredSize().height+25 + cs.getPreferredSize().height);
    f.show();
  }
}

