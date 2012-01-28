package jalview.gui;

import jalview.io.*;
import jalview.gui.schemes.*;
import jalview.datamodel.*;
import jalview.gui.popups.*;
import jalview.analysis.*;
import jalview.gui.event.*;
import jalview.gui.menus.*;
import MCview.*;

import apollo.util.FeatureList;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.FeaturePairI;

import java.awt.*;
import java.awt.event.*;
import java.applet.Applet;
import java.util.*;
import java.net.*;
import java.io.*;

import javax.swing.*;


public final class AlignFrame extends JFrame implements OutputGenerator {
  //MenuBar        mb;


  AlignmentPanel ap;
  Object         parent;

  StatusBar      status;
  ErrorPopup     error;
  Label          redraw;
  JPanel          labelPanel;

  DrawableAlignment drawableAlignment;
  AlignViewport     av;
  Controller        controller;

  PostscriptProperties pp;
  MailProperties       mp;
  MenuManager       menuManager;

  private CondensedAlignment ca;
  private boolean readOnlyMode = false;
  /** show all menu items and such, even if not fully implemented */
  private boolean showEverything = true;

  /** Whether to System.exit java on window closing */
  private static boolean exitOnClose=false;

  static int defaultWidth  = 700;
  static int defaultHeight = 500;

  /** If parent is null then we exit java on closing window */
  public AlignFrame(Object parent) {
    this.parent = parent;
    if (parent == null) exitOnClose = true;
    myFrameInit();
  }

  public AlignFrame(Object parent, Alignment al) {
    this.parent = parent;
    if (parent==null) exitOnClose = true;
    drawableAlignment = new DrawableAlignment(al);
    init();
  }

  public AlignFrame(DrawableSequence[] s, boolean exitOnClose) {
    this.exitOnClose = exitOnClose;
    drawableAlignment = new DrawableAlignment(s);
    System.out.println("Created alignment");
    init();
  }
  public AlignFrame(DrawableSequence[] s) {
    this(s,true);
  }
  public AlignFrame(Object parent, String input, String type, String format) {
    this.parent = parent;
    if (parent==null) exitOnClose = true;
    DrawableSequence[] s = null;
    s = FormatAdapter.toDrawableSequence(FormatAdapter.read(input,type,format));

    drawableAlignment = new DrawableAlignment(s);
    init();
  }

  /** Put up a FeatureList from apollo. FeatureList is a list of apollo's SeqFeatureI's.*/
  public AlignFrame(FeatureList featureList, boolean exitOnClose,boolean readOnly) {
    setReadOnlyMode(readOnly);
    this.exitOnClose = exitOnClose;

    ca       = new CondensedAlignment(featureList);
    AlignSequenceI []  seqarr   = ca.createSequences();
    DrawableSequence[] drawSeqs = sequenceToDrawableSequences(seqarr);

    Alignment align  = new Alignment();

    SequenceGroup dnag = ca.getDnaSequences();
    SequenceGroup pepg = ca.getPeptideSequences();

    //align.addGroup(dnag);
    //align.addGroup(pepg);

    drawableAlignment = new DrawableAlignment(align);

    drawableAlignment.addGroup(dnag);
    drawableAlignment.addGroup(pepg);

    ResidueColourScheme dnacs = new ZappoColourScheme();
    ResidueColourScheme pepcs = new TaylorColourScheme();

    dnacs.setThreshold(50);
    pepcs.setThreshold(50);

    drawableAlignment.setColourScheme(dnag,dnacs);
    drawableAlignment.setColourScheme(pepg,pepcs);

    init();
    ap.setMapper(ca.getMapper());
  }

  public void setReadOnlyMode(boolean readOnly) {
    this.readOnlyMode = readOnly;
    // If readOnly mode(eg apollo) dont show all the stuff that is not fully
    // implemented. We can make an explicit method if need be.
    showEverything = !readOnly;
  }
  public boolean getReadOnlyMode() { return readOnlyMode; }

  public boolean showEverything() { return showEverything; }

  public static boolean exitOnClose() { return exitOnClose; }

  public AlignViewport getAlignViewport() {
    return av;
  }


  private DrawableSequence[] sequenceToDrawableSequences(AlignSequenceI[] seqs)
  {
    if (seqs.length==0) return new DrawableSequence[]{};

    ArrayList drawSeqList = new ArrayList();

    for (int i=0; i < seqs.length; i++) {
      DrawableSequence ds = new DrawableSequence(seqs[i]);
      drawSeqList.add(ds);
    }

    return (DrawableSequence[])drawSeqList.toArray(new DrawableSequence[]{});
  }

  public void init() {
    //System.out.println("Calling init");
    av         = new AlignViewport(drawableAlignment,false,true,true,false);
    //System.out.println("Created viewport");
    controller = new Controller();
    //System.out.println("Created controller");
    ap         = new AlignmentPanel(this,av,controller);
    ap.setReadOnlyMode(readOnlyMode);
    //System.out.println("Created alignment panel");
    // DEBUG
    new ControllerDebugListener(controller);

    myFrameInit();

    drawableAlignment.setColourScheme(new ZappoColourScheme());

  }


  boolean firstShow = true;
  public void show() {
    super.show();
    ap.setFont(new Font("Courier",Font.PLAIN,10));
    AlignFrameWindowListener afwl = new AlignFrameWindowListener(this);
    if (!firstShow) {
      System.err.println("WARNING: Unexpected show");
    }
    firstShow = false;
  }

  public void myFrameInit() {
    //System.out.println("Java version = " + System.getProperty("java.version"));
    //System.out.println("av = " + av);
    menuManager = new MenuManager(this,av,controller);

    getContentPane().setLayout(new BorderLayout());

    error  = new ErrorPopup(this,av,controller,"Error dialog");
    status = new StatusBar("Status : ",controller,Label.LEFT);
    redraw = new Label(" ",Label.RIGHT);

    status.setHorizontalAlignment(JLabel.LEFT);
    labelPanel  = new JPanel();
    labelPanel.setLayout(new GridLayout(1,2));
    getContentPane().add("South",labelPanel);
    labelPanel.add(status);
    labelPanel.add(redraw);

    getContentPane().add("Center",ap);
    //resize(defaultWidth,defaultHeight);
    //show();

//    updateFont();
//    System.out.println("three");
//    updateFont();
//    System.out.println("four");

  }

  class AlignFrameWindowListener extends WindowAdapter {
    AlignFrame af;
    public AlignFrameWindowListener(AlignFrame f) {
      af = f;
      af.addWindowListener(this);
    }
    public void windowClosing(WindowEvent evt) {
      Controller controller = af.getController();
      if (exitOnClose) {
        controller.handleStatusEvent(new StatusEvent(this,"Quiting application",StatusEvent.INFO));

        System.exit(0);

      } else {
        controller.handleStatusEvent(new StatusEvent(this,"Closing window",StatusEvent.INFO));

        af.hide();
        af.dispose();
      }
    }
  }

  public void setFont(String fontName) {
    Font f = av.getFont();
    Font newf = new Font(fontName,f.getStyle(),f.getSize());

    av.setFont(newf);
    controller.handleAlignViewportEvent(new AlignViewportEvent(this,av,AlignViewportEvent.FONT));

    updateFont();
    controller.handleStatusEvent(new StatusEvent(this,"Font changed to " + fontName,StatusEvent.INFO));
  }

  public void setFont(int style,int size) {
    Font f = av.getFont();
    Font newf = new Font(f.getName(),style,size);

    av.setFont(newf);
    controller.handleAlignViewportEvent(new AlignViewportEvent(this,av,AlignViewportEvent.FONT));

    updateFont();

    controller.handleStatusEvent(new StatusEvent(this,"Font changed to " + style + " " + size, StatusEvent.INFO));
  }

  public void updateFont() {
    super.repaint();
    System.out.println("Frame size = " + getSize());
  }
    public Dimension getMinimumSize() {
	return new Dimension(10,10);
    }
  public static void main(String[] args) {
    MSFfile msf;

    DrawableSequence[] s = null;

    try {
      msf = new MSFfile(args[0], "File");
      s   = new DrawableSequence[msf.getSeqs().size()];

      for (int i=0;i < msf.getSeqs().size();i++) {
        s[i] = new DrawableSequence((AlignSequence)msf.getSeqs().elementAt(i));
      }
    } catch (java.io.IOException e) {
      System.out.println("Exception : " + e);
    }

    if (s != null) {
      AlignFrame af = new AlignFrame(s,true);
      System.out.println("AlignFrame made");
      af.resize(defaultWidth,defaultHeight);

      af.show();
    }
  }

  public  Object getAFParent() {
    return parent;
  }

  /**
   * This is for the applet.
   */
  public AlignViewport getViewport() {
    return av;
  }

  public Controller getController() {
    return controller;
  }
  public void setPostscriptProperties(PostscriptProperties pp) {
    this.pp = pp;
  }
  public PostscriptProperties getPostscriptProperties() {
     return pp;
  }
  public MailProperties getMailProperties() {
     return mp;
  }

  //    public FileProperties getFileProperties();
  //    public void setFileProperties(FileProperties fp);
  //public void setMailProperties(MailProperties mp);

  public String getText(String format)  {
     return "";
  }

  public void getPostscript(PrintWriter bw) {
  }
  public void getPostscript(PrintStream ps) {
  }
  public StringBuffer getPostscript() {
     return new StringBuffer();
  }

  public Mapper getMapper() { return ca.getMapper(); }
}
