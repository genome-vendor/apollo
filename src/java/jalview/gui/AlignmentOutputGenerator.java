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

import jalview.io.*;
import jalview.datamodel.*;

import java.util.*;
import java.io.*;

public class AlignmentOutputGenerator implements OutputGenerator {

  // Output properties
  MailProperties mp;
  FileProperties fp;
  PostscriptProperties pp;

  AlignViewport av;

  public AlignmentOutputGenerator(AlignViewport av) {
    this.av = av;

    propertiesInit();
  }

  public void propertiesInit() {
    this.mp = new MailProperties();
    mp.server = "mailserv.ebi.ac.uk";
    this.pp = new PostscriptProperties();
    this.fp = new FileProperties();
  }

  // The properties stuff for the OutputGenerator interface
  public MailProperties getMailProperties() {
    return mp;
  }

  public PostscriptProperties getPostscriptProperties() {
    return pp;
  }

  public FileProperties getFileProperties() {
    return fp;
  }

  public void setMailProperties(MailProperties mp) {
    this.mp = mp;
  }

  public void setPostscriptProperties(PostscriptProperties pp) {
    this.pp = pp;
  }

  public void setFileProperties(FileProperties fp) {
    this.fp = fp;
  }

  public String getText(String format) {
    if (FormatProperties.contains(format)) {
      return FormatAdapter.get(format,av.getAlignment().getSequences());
    } else {
      return null;
    }
  }

  public void getPostscript(PrintWriter bw) {
    Postscript p  = new Postscript(this,bw);
    p.generate();
  }

  public StringBuffer getPostscript() {
    Postscript p = new Postscript(this,true);
    p.generate();
    return p.getOut();
  }
  public void getPostscript(PrintStream ps) {
    Postscript p = new Postscript(this,ps);

    p.generate();
    ps.flush();
    ps.close();
  }

  public AlignViewport getViewport() {
    return av;
  }
}
