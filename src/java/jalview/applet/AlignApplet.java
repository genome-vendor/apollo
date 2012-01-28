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

import jalview.gui.*;
import jalview.gui.schemes.*;
import jalview.gui.event.*;
import jalview.datamodel.*;

import java.applet.Applet;
import java.awt.*;
import java.util.StringTokenizer;

public class AlignApplet extends Applet {
  String input;
  String type;
  AlignFrame af;
  String fontSize = "10";
  String format = "MSF";
  int noGroups;

  String consString = "*";
  String local="";
  // ConsThread ct;
  String mailServer;
  String clustalServer;
  String srsServer;
  String database;

  AlignViewport av;
  Controller    controller;

  public void init() {
    input = getParameter("input");
    type = getParameter("type");
    fontSize = getParameter("fontsize");

    mailServer = getParameter("mailServer");
    clustalServer = getParameter("clustalServer");
    srsServer = getParameter("srsServer");
    database = getParameter("database");

    local = getParameter("local");

    try {
      noGroups = (Integer.valueOf(getParameter("groups"))).intValue();
      System.out.println("Number of groups = " + noGroups);
    } catch (Exception e) {
      //      System.out.println("Exception in nogroups : " + e);
    }

    format = getParameter("format");
    if (format == null || format.equals("")) {
      format = "MSF";
    }

    format = format.toUpperCase();

    System.out.println("Format = " + format);
    consString =  getParameter("Consensus");

    if (consString == null) {
      consString = "*";
    }

    componentInit();

  }
  public void componentInit() {
    makeFrame();
  }

  private void setFrameValues() {
    av = af.getViewport();
    controller = af.getController();

    af.setSize(700,500);
    af.show();
  }

  public void makeFrame() {
    try {
      String s = getParameter("numseqs");
      if (getParameter("numseqs") == null) {
        af = new AlignFrame(this,input,type,format);
      } else {
        int num = (Integer.valueOf(getParameter("numseqs"))).intValue();
        int i = 0;
        int count = 0;
        DrawableSequence[] seqs = new DrawableSequence[num];
        while (i < num) {
          String s2 = getParameter("seq" + (i+1));
          if (s2 != null) {
            String id = getParameter("id" + (i+1));
            if (id == null) {
              id = "Seq_" + i;
            }
            seqs[count] = new DrawableSequence(id,s2,1,s2.length());
            count++;
          } else {
            System.out.println("Can't read sequence " + (i+1));
          }
          i++;
        }
        if (count > 0) {
          af = new AlignFrame(this,new Alignment(seqs));
        } else {
          System.out.println("No sequences found");
          this.stop();
        }
      }
      setFrameValues();

      af.setTitle("Jalview alignment editor");

      if (mailServer != null) {
        if (!(mailServer.equals(""))) {
          av.getOutputGenerator().getMailProperties().setServer(mailServer);
        } else {
          av.getOutputGenerator().getMailProperties().setServer("");
        }
      }

      if (srsServer != null) {
        if (!(srsServer.equals(""))) {

          if (!srsServer.substring(srsServer.length()-1).equals("/")) {
            srsServer = srsServer + "/";
          }
          Config.setSRSServer(srsServer);
          System.out.println("Srs server = " + Config.getSRSServer());
        }
      }
      if (database != null) {
        if (!(database.equals(""))) {
          Config.setDatabase(database);
          System.out.println("Srs database = " + Config.getDatabase());
        }
      }

      try {
        int fs = (Integer.valueOf(fontSize)).intValue();
        av.setFont(new Font("Courier",Font.PLAIN,fs));
      } catch (Exception ex) {
        System.out.println("Exception in font size : " + ex);
        //af.ap.seqPanel.seqCanvas.f = new Font("Courier",Font.PLAIN,10)
        av.setFont(new Font("Courier",Font.PLAIN,10));
      }
      System.out.println("Consensus string " + consString);
      if (consString.equals("*")) {
        av.getSelection().selectAll(av.getAlignment());
      } else {
        int seqs[] = selectSeqs(consString);
        int i = 0;
        av.getSelection().clear();
        while (i < seqs.length && seqs[i] != -1) {
          av.getSelection().addElement(av.getAlignment().getSequenceAt(seqs[i]));
          i++;
        }
      }

// SMJS TODO
//      af.getStatusBar().setText("Calculating consensus...");
//      af.ap.seqPanel.align.percentIdentity(af.ap.sel);
//      af.ap.seqPanel.align.percentIdentity2();
//      af.ap.seqPanel.align.findQuality();
//      af.cons = af.ap.seqPanel.align.cons;
//      af.getStatusBar().setText("done");

      //ScoreSequence[] sseq = new ScoreSequence[1];

      //sseq[0] = af.ap.seqPanel.align.qualityScore;
      //af.bp.setScorePanel(new ScorePanel(af,sseq));

      if (noGroups > 0) {
        for (int i = 0; i < noGroups; i++) {
          controller.handleStatusEvent(new StatusEvent(this,"Parsing group " + (i+1),StatusEvent.INFO));

          String gs = getParameter("group" + (i+1));
          System.out.println("Group = " + gs);

          parseGroup(av.getAlignment(),gs);
        }
      } else {
        controller.handleStatusEvent(new StatusEvent(this,"Setting colour scheme...",StatusEvent.INFO));

        //	PIDColourScheme pidcs = new PIDColourScheme(af.ap.seqPanel.align.cons);
        //ClustalxColourScheme pidcs = new ClustalxColourScheme(af.ap.seqPanel.align.cons2,af.ap.seqPanel.align.size());
        //af.ap.seqPanel.align.setColourScheme(pidcs);
      }
      controller.handleStatusEvent(new StatusEvent(this,"done",StatusEvent.INFO));

      av.getSelection().clear();
      controller.handleAlignViewportEvent(new AlignViewportEvent(this,av,AlignViewportEvent.COLOURING));

    }
    catch (Exception ex) {
      System.out.println("Exception in applet : " + ex);
    }
  }

  private boolean parseTrueFalse(String str) {
    if (str.equalsIgnoreCase("TRUE")) {
      return true;
    } else if (str.equalsIgnoreCase("FALSE")) {
      return false;
    }
    return false;
  }


  public void parseGroup(DrawableAlignment al, String gs) {
    int col = 0;
    boolean boxes = true;
    boolean text = true;
    boolean colourText = false;

    StringTokenizer st = new StringTokenizer(gs,":");
    try {
      int seqs[] = selectSeqs(st.nextToken());

      String tmp = st.nextToken();
      col = 0;
      tmp.toUpperCase();
      System.out.println(tmp);

      if (tmp.equals("SECONDARY")) {
        tmp = "Secondary structure";
      }
      if (tmp.equals("ZAPPO")) {
        tmp = "Zappo";
      }
      if (tmp.equals("TAYLOR")) {
        tmp = "Taylor";
      }
      if (tmp.equals("HYDROPHOBIC")) {
        tmp = "Hydrophobic";
      }
      if (tmp.equals("USERDEFINED")) {
        tmp = "User defined";
      }
      if (tmp.equals("HELIX")) {
        tmp = "Helix";
      }
      if (tmp.equals("STRAND")) {
        tmp = "Strand";
      }
      if (tmp.equals("TURN")) {
        tmp = "Turn";
      }
      if (tmp.equals("CLUSTALX")) {
        tmp = "Clustalx";
      }

      ColourSchemeI cs = ColourSchemeFactory.get(tmp); //MG ColourScheme->ColourSchemeI

      if (cs == null) {
        cs = new TaylorColourScheme();
      }

      System.out.println("Colour scheme = " + cs);
      tmp = st.nextToken();
      tmp = tmp.toUpperCase();

      boxes = parseTrueFalse(tmp);

      tmp = st.nextToken();
      tmp = tmp.toUpperCase();

      text = parseTrueFalse(tmp);

      tmp = st.nextToken();
      tmp = tmp.toUpperCase();

      colourText = parseTrueFalse(tmp);

      System.out.println("boxes = " + boxes);
      System.out.println("text = " + text);

//MG      SequenceGroup sg = new SequenceGroup(cs,true,boxes,text,colourText,true);


//MG      System.out.println("Sequence group " + sg);
//MG      av.getAlignment().addGroup(sg);

      int i = 0;
      while(seqs[i] != -1) {
        if (av.getAlignment().getSequenceAt(seqs[i]) != null) {
          AlignSequenceI seq = av.getAlignment().getSequenceAt(seqs[i]);
          if (av.getAlignment().findGroup(seqs[i]) != null) {
            av.getAlignment().removeFromGroup(av.getAlignment().findGroup(seqs[i]),
                                              seq);
          }
//MG          sg.addSequence(seq);
        }
        i++;
      }

// SMJS TODO
//      System.out.println("Colourscheme is " + cs);
//      if ((af.cons != null) && (cs instanceof ResidueColourScheme)) {
//        System.out.println("Setting colour scheme " + cs);
//        ((ResidueColourScheme)sg.colourScheme).setCons(af.cons);
//      }
//      if ((af.cons != null) && (cs instanceof ClustalxColourScheme)) {
//        System.out.println("Setting colour scheme " + cs);
//        ((ClustalxColourScheme)sg.colourScheme).setCons(af.cons);
//        System.out.println("Consensus is" + af.cons);
//      }

//MG      av.getAlignment().displayText(sg);
//MG      av.getAlignment().displayBoxes(sg);
//MG      av.getAlignment().colourText(sg);
//MG      av.getAlignment().setColourScheme(sg);

    } catch (Exception e) {
      System.out.println("Exception : " + e);
    }
  }

  public int[] selectSeqs(String s) {
    StringTokenizer st = new StringTokenizer(s,",");

    int[] seqs = new int[2000];
    int count = 0;

    while (st.hasMoreTokens()) {
      String tmp = st.nextToken();
      if (tmp.equals("*")) {
        for (int i=0; i < av.getAlignment().getHeight(); i++) {
          seqs[i] = i;
        }
      } else  if (tmp.indexOf("-") >= 0) {
        try {
          StringTokenizer st2 = new StringTokenizer(tmp,"-");

          int start = (Integer.valueOf(st2.nextToken())).intValue();
          int end = (Integer.valueOf(st2.nextToken())).intValue();

          if (end > start) {
            for (int i = start; i <= end; i++) {
              //	      System.out.println("Adding " + i + " to group");
              seqs[count] = i-1;
              count++;
            }
          }
        } catch (Exception e) {
          System.out.println("Exception : " + e);
        }
      } else {
        try {
          seqs[count] = (Integer.valueOf(tmp)).intValue()-1;
          System.out.println("Adding " + seqs[count] + " to group");
          count++;
        } catch (Exception e) {
          System.out.println("Exception : " + e);
        }
      }
    }
    seqs[count] = -1;

    return seqs;
  }
}
