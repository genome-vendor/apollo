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

import jalview.analysis.*;

import java.awt.*;

public class ProgressPanelDriver extends Driver {
  public static void main(String[] args) {
    Frame f = new Frame("Testy");
    ClustalwThread ct = new ClustalwThread("pog.msf","pog.aln");
    ProgressPanel pp = new ProgressPanel(f,ct);
    Thread t = new Thread(pp);

    t.start();

    f.add(pp);
    f.resize(350,150);
    f.show();
    ct.start();
  }
}
