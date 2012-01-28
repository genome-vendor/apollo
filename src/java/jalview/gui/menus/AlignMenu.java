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

import java.awt.*;
import java.awt.event.*;
import java.applet.Applet;
import java.util.*;
import java.io.*;

public class AlignMenu extends FrameMenu {

  LocalAlignAction  local;
  RemoteAlignAction alignEBI;
  PostalAnalAction  postalEBI;
  JNetAction        jnetEBI;

  public AlignMenu(AlignFrame frame,AlignViewport av,Controller c) {
    super("Align",frame,av,c);
  }

  protected void init() {
    String server = "jura.ebi.ac.uk";
    if (getFrameParent() instanceof Applet) {
      Applet app = (Applet)getFrameParent();
      if (app.getCodeBase().getHost().equals("jura.ebi.ac.uk")) {
        alignEBI  = new RemoteAlignAction("Remote alignment at EBI",6543,server);
        postalEBI = new PostalAnalAction("Remote postal analysis at EBI",6543,server);
        jnetEBI   = new JNetAction("Remote Jnet analysis at EBI",6543,server);
        add(alignEBI);
        add(postalEBI);
        add(jnetEBI);
      } else {
        disable();
      }
    } else {
      local     = new LocalAlignAction("Local alignment");
      alignEBI  = new RemoteAlignAction("Remote alignment at EBI",6543,server);
      postalEBI = new PostalAnalAction("Remote postal analysis at EBI",6543,server);
      jnetEBI   = new JNetAction("Remote Jnet analysis at EBI",6543,server);
      add(local);
      add(alignEBI);
      add(postalEBI);
      add(jnetEBI);
    }
  }

  abstract class RemoteAction extends AnalAction {
    String server;
    int    port;
    public RemoteAction(String name, int port, String server) {
      super(name);
      this.server = new String(server);
      this.port = port;
    }
  }


  abstract class AnalAction extends JalAction {

    public AnalAction(String name) {
      super(name);
    }

    public void applyAction(ActionEvent evt) {
      doAnalysis(evt);
    }

    public abstract void doAnalysis(ActionEvent evt);

    protected AlignSequenceI [] getNoGapSeqArray() {
      AlignSequenceI[] newseq = new AlignSequenceI[av.getAlignment().getSequences().size()];

      for (int i=0; i < newseq.length; i++) {
        AlignSequenceI seq = av.getAlignment().getSequenceAt(i);
        String newstr = AlignSeq.extractGaps(" ",seq.getResidues());
        newstr =  AlignSeq.extractGaps("-",newstr);
        newstr =  AlignSeq.extractGaps(".",newstr);
 
        newseq[i]  = new AlignSequence(seq.getName(),
                                  newstr,
                                  seq.getStart(),
                                  seq.getEnd());
      }
      return newseq;
    }

    protected AlignSequenceI [] getSeqArray() {
      AlignSequenceI[] newseq = new AlignSequenceI[av.getAlignment().getSequences().size()];
      for (int i=0; i < newseq.length; i++) {
        newseq[i]  = av.getAlignment().getSequenceAt(i);
      }
      return newseq;
    }
  }

  class LocalAlignAction extends AnalAction {

    public LocalAlignAction(String name) {
      super(name);
    }
    public void doAnalysis(ActionEvent evt) {
      if (!(frame.getAFParent() instanceof Applet)) {
        controller.handleStatusEvent(new StatusEvent(this,"Starting local alignment...",StatusEvent.INFO));

        AlignSequenceI [] newseq = getNoGapSeqArray();
 
        ClustalwThread ct = new ClustalwThread(newseq);
        ct.start();
 
      } else {
        controller.handleStatusEvent(new StatusEvent(this,"Can't run local process from applet",StatusEvent.ERROR));
      }
    }
  }

  class PostalAnalAction extends RemoteAction {
    public PostalAnalAction(String name, int port, String server) {
      super(name,port,server);
    }
    public void doAnalysis(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"Remote postal analysis...",StatusEvent.INFO));

      AlignSequenceI [] newseq = getSeqArray();
 
      controller.handleStatusEvent(new StatusEvent(this,"Creating progress frame",StatusEvent.INFO));

      ProgressFrame pf = new ProgressFrame("Postal progress...",this,null);
      TextAreaPrintStream taps = new TextAreaPrintStream(System.out,pf.getTextArea());
      PostalCGI cwcgi = new PostalCGI("jura.ebi.ac.uk",6543,"/cgi-bin/runpostal",newseq,taps);
      Thread th = new Thread(cwcgi);
      pf.setCommandThread(th);
 
      pf.show();
      Thread t = pf.createProgressThread();
 
      t.start();
      th.start();
 
      controller.handleStatusEvent(new StatusEvent(this,"Starting postal thread...",StatusEvent.INFO));
    }
  }

  class RemoteAlignAction extends RemoteAction {
    public RemoteAlignAction(String name, int port, String server) {
      super(name,port,server);
    }
    public void doAnalysis(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"Aligning sequences remotely...",StatusEvent.INFO));

      AlignSequenceI [] newseq = getNoGapSeqArray();
 
      controller.handleStatusEvent(new StatusEvent(this,"Creating progress frame",StatusEvent.INFO));

      ProgressFrame pf = new ProgressFrame("Clustalw progress...",this,null);
      TextAreaPrintStream taps = new TextAreaPrintStream(System.out,pf.getTextArea());
      ClustalwCGI cwcgi = new ClustalwCGI(server,port,"/cgi-bin/runclustal",newseq,taps);
      Thread th = new Thread(cwcgi);
      pf.setCommandThread(th);
 
      pf.show();
      Thread t = pf.createProgressThread();
 
      t.start();
      th.start();
 
      controller.handleStatusEvent(new StatusEvent(this,"Started clustal thread",StatusEvent.INFO));
    }
  }

  class JNetAction extends RemoteAction {
    public JNetAction(String name, int port, String server) {
      super(name,port,server);
    }
    public void doAnalysis(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"JNET action not implemeneted",StatusEvent.ERROR));
    }
  }
}
