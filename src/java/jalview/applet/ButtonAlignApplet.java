

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
package jalview.applet;

import java.awt.*;
import java.awt.event.*;

public class ButtonAlignApplet extends AlignApplet implements ActionListener {
  Button b;

  public void componentInit() {
    b = new Button("JalView");
    add(b);
    b.addActionListener(this);
  }
  public void actionPerformed(ActionEvent evt) {
    if (evt.getSource() == b) {
      b.requestFocus();
      makeFrame();
    }
    return;
  }
}
