package jalview.gui;

import jalview.datamodel.*;
import jalview.analysis.*;
import jalview.gui.schemes.*;
import jalview.gui.event.*;

import java.awt.*;
import java.net.*;
import java.io.*;
import java.applet.*;
import MCview.*;

public class SequenceFeatureThread extends Thread {
  Object parent;
  AlignViewport av;
  Controller controller;
  int find = -1;


  public SequenceFeatureThread(AlignViewport av, Controller c,Object parent) {
    this.parent = parent;
    this.av = av;
    this.controller = c;
    System.out.println("pog");
  }

  public void run() {
    FeatureFrame ff = null;

    System.out.println("wog");
    if (parent instanceof AlignFrame) {
      AlignFrame af = (AlignFrame)parent;
      try {
        // Test the server can be accessed
        String tmp = Config.getSRSServer();
        String page;
        String server;
        int port;
        page = "/";
        if (tmp.indexOf("/") != -1) {
          page = tmp.substring(tmp.indexOf("/"));
        }
        if (tmp.indexOf(":") >= 0) {
          System.out.println("server = " + tmp);
          port = Integer.parseInt(tmp.substring(tmp.indexOf(":")+1, tmp.indexOf("/")));
          server = tmp.substring(0,tmp.indexOf(":"));
        } else {
          port = 80;
          server = tmp.substring(0,tmp.indexOf("/"));
        }
        System.out.println("eek");

        // Check if applet and if the server is allowed
        if (af.getAFParent() instanceof Applet) {
          Applet app = (Applet)af.getAFParent();
          if (! app.getCodeBase().getHost().equals(server)) {
            controller.handleStatusEvent(new StatusEvent(this,"SRS server must be the applet host",StatusEvent.ERROR));
          }
        }

        System.out.println("ook");
        if (CGI.test(server,port,5000)) {
          System.out.println("ork");
          if (av.getSelection().size() != 0) {
            if (ff == null || ff.isVisible() == false) {
              ff = new FeatureFrame(av,controller,af,this,"Sequence feature console",15,72,"");
              ff.setTextFont(new Font("Courier",Font.PLAIN,12));
              ff.resize(500,400);
              ff.show();
            }
            find = 0;
            //av.getAlignment().getFeatures(ff.ta,av.getSelection(),Config.getSRSServer(),Config.getDatabase());
            find = 1;
          } else {
            if (ff == null || ff.isVisible() == false) {
              ff = new FeatureFrame(av,controller,af,this,"Sequence feature console",15,72,"");
              ff.setTextFont(new Font("Courier",Font.PLAIN,12));
              ff.resize(500,400);
              ff.show();
            }
            find = 0;
            System.out.println("Fetching features");
            //av.getAlignment().getFeatures(ff.ta,Config.getSRSServer(),Config.getDatabase());
            System.out.println("Fetched features");
            find = 1;
          }
          controller.handleStatusEvent(new StatusEvent(this,"Setting feature colours",StatusEvent.INFO));

	  //          FeatureColourScheme ftcs = new FeatureColourScheme();
          System.out.println("Setting feature colour scheme");
          //av.getAlignment().setColourScheme(ftcs);
          System.out.println("Done colour Scheme");

          controller.handleStatusEvent(new StatusEvent(this,"done",StatusEvent.INFO));

          // 	  boolean found = false;
          // 	  for (int i=0; i < af.ap.seqPanel.align.ds.length; i++) {

          // 	    DrawableSequence seq = (DrawableSequence)af.ap.seqPanel.align.ds[i];
          // 	    if (seq.pdbcode.size() > 0) {
          // 	      found = true;
          // 	      break;
          // 	    }
          // 	  }
          // 	  if (found) {
          // 	    af.structures.enable();
          // 	  }

        }
        else {
          controller.handleStatusEvent(new StatusEvent(this,"SRS server cannot be reached",StatusEvent.ERROR));
        }
      } catch (StringIndexOutOfBoundsException ex) {
        controller.handleStatusEvent(new StatusEvent(this,"ERROR: invalid SRS server name \"" + Config.getSRSServer() + "\"",StatusEvent.ERROR));
      }
    }
  }
}
