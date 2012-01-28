package jalview.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import jalview.gui.*;
import jalview.gui.event.*;
import jalview.datamodel.*;
import jalview.gui.menus.*;

import apollo.datamodel.SequenceI;
import apollo.datamodel.SequenceEdit;
import MCview.*;

public class SeqPanel extends ControlledPanel implements AdjustmentListener,
                                               AlignViewportListener,
                                               MouseListener,
                                               MouseMotionListener {
  public    DrawableAlignment align;
  public    SeqCanvas         seqCanvas;
  public    ScalePanel        scalePanel;

  protected JScrollBar hscroll;
  protected JScrollBar vscroll;

  protected int oldoffx;
  protected int oldoffy;

  protected int maxoffx = 0;
  protected int maxoffy = 0;

  protected int startres;
  protected int lastres;
  protected int endres;


  protected int startseq;
  protected int padseq;

  public    boolean fastDraw = true;
  public    boolean editFlag;
  public    boolean showScale = true;
  private   boolean readOnlyMode=false;

  protected AlignViewport av;

  public SeqPanel(AlignViewport av,Controller c) {
    this.av         = av;
    this.align      = av.getAlignment();

    setController(c);

    componentInit();

    addMouseMotionListener(this);
    addMouseListener(this);
  }

  private void componentInit() {
    seqCanvas  = new SeqCanvas(av,controller);
    scalePanel = new ScalePanel(av,controller);

    hscroll = new JScrollBar(Scrollbar.HORIZONTAL);
    vscroll = new JScrollBar(Scrollbar.VERTICAL);

    hscroll.addAdjustmentListener(this);
    vscroll.addAdjustmentListener(this);

    setLayout(new BorderLayout());

    if (showScale) {
      JPanel sh = new JPanel();
      sh.setLayout(new BorderLayout());
      sh.add("Center",scalePanel);
      sh.add("West",new SeqFillPanel(SeqFillPanel.LEFT,av,controller));
      JPanel sh2 = new JPanel();
      sh2.setLayout(new BorderLayout());
      sh2.add("Center",sh);
      sh2.add("East",new ScrollFillPanel(vscroll));
     
      add("North",sh2);
    }

    JPanel sch = new JPanel();
    sch.setLayout(new BorderLayout());
    sch.add("Center",seqCanvas);
    sch.add("West",new SeqFillPanel(SeqFillPanel.LEFT,av,controller));
    sch.add("North",new SeqFillPanel(SeqFillPanel.TOP,av,controller));

    JPanel hh = new JPanel();
    hh.setLayout(new BorderLayout());
    hh.add("Center",hscroll);
    hh.add("West",new SeqFillPanel(SeqFillPanel.LEFT,av,controller));
    JPanel hh2 = new JPanel();
    hh2.setLayout(new BorderLayout());
    hh2.add("Center",hh);
    hh2.add("East",new ScrollFillPanel(vscroll,SystemColor.scrollbar));

    Panel vh = new Panel();
    vh.setLayout(new BorderLayout());
    vh.add("Center",vscroll);
    vh.add("North",new SeqFillPanel(SeqFillPanel.TOP,av,controller));

    add("Center",sch);
    add("East",vh);
    add("South",hh2);

    setScrollValues(0,0);

    seqCanvas.addMouseListener(this);
    seqCanvas.addMouseMotionListener(this);

    //System.out.println("SeqPanel initialized");
  }

  void setMapper(Mapper mapper) { scalePanel.setMapper(mapper); }

  void setReadOnlyMode(boolean readOnly) { this.readOnlyMode = readOnly; }

  public void setFont(Font f) {
    if (seqCanvas != null) {
      seqCanvas.setFont(f);
    }
  }

  public void setScrollValues(int offx, int offy) {
    // System.out.println("Set scroll values called");
    int width;
    int height;

    //Brings up error in netscape
    if (seqCanvas.size().width > 0) {
      width  = seqCanvas.size().width;
      height = seqCanvas.size().height;
    } else {
      width  = seqCanvas.preferredSize().width;
      height = seqCanvas.preferredSize().height;
    }

    //Make sure the maxima are right
    if (maxoffx != (align.getWidth())) {
      maxoffx = (align.getWidth());
    }
    if (maxoffy != (align.getHeight())) {
      maxoffy = (align.getHeight());
    }

    //The extra 1 is to make sure all the last character gets printed
    // SMJS Removed the extra one

    //hscroll.setValues(offx,width/av.getCharWidth(),1,maxoffx);
    //vscroll.setValues(offy,height/av.getCharHeight(),1,maxoffy);
    if (av.getWrapAlignment() == false) {
	showScale = false;

	hscroll.setValues(offx,width/av.getCharWidth(),0,maxoffx);
	vscroll.setValues(offy,height/av.getCharHeight(),0,maxoffy);
	
	hscroll.setUnitIncrement(1);
	vscroll.setUnitIncrement(1);
	
	int hpageinc = av.getEndRes() - av.getStartRes();
	int vpageinc = av.getEndSeq() - av.getStartSeq();
	
	if (hpageinc < 1) {
	    hpageinc = 1;
	}
	if (vpageinc < 1) {
	    vpageinc = 1;
	}

	hscroll.setBlockIncrement(hpageinc);
	vscroll.setBlockIncrement(vpageinc);
    } else {
	showScale = true;
	maxoffy = ((align.getWidth()/seqCanvas.getChunkWidth()) + 2)*(align.getHeight()+1);
	vscroll.setValues(offy,height/av.getCharHeight(),0,maxoffy);
	
	hscroll.setUnitIncrement(1);
	vscroll.setUnitIncrement(1);
    }
    // System.out.println("Done set scroll values");
  }

  public int getScaleHeight() {
    if (showScale) {
      return scalePanel.size().height;
    } else {
      return 0;
    }

  }
  public void adjustmentValueChanged(AdjustmentEvent evt) {

    if (evt.getSource() == hscroll) {
      int offx = hscroll.getValue();
      if (offx != oldoffx) {
        av.setStartRes(offx);
        av.setEndRes(offx + seqCanvas.size().width/av.getCharWidth()-1);
        controller.handleAlignViewportEvent(new AlignViewportEvent(this,av,AlignViewportEvent.HSCROLL));
        hscroll.validate();
      }
      oldoffx = offx;
    }

    if (evt.getSource() == vscroll) {
      int offy = vscroll.getValue();
      if (oldoffy != offy) {
        av.setStartSeq(offy);
        av.setEndSeq(offy + seqCanvas.size().height/av.getCharHeight());
        controller.handleAlignViewportEvent(new AlignViewportEvent(this,av,AlignViewportEvent.LIMITS));
      }
      oldoffy = offy;
    }
  }

  public boolean handleAlignViewportEvent(AlignViewportEvent e) {
    hscroll.setValue(av.getStartRes());
    vscroll.setValue(av.getStartSeq());
    hscroll.paint(hscroll.getGraphics());
    vscroll.paint(vscroll.getGraphics());
    repaint();
    return true;
  }

  public void reshape(int x, int y, int width, int height) {
    super.reshape(x,y,width,height);
    setScrollValues(av.getStartRes(),av.getStartSeq());
  }

  public Dimension minimumSize() {
    return new Dimension(700,500);
  }

  public Dimension preferredSize() {
    return minimumSize();
  }

  /*
  public class EditSequenceAction extends JalAction {
    public EditSequenceAction(String name) {
      super(name);
      addRequiredArg("sequence");
      addRequiredArg("position");
      addRequiredArg("length");
    }
    public void applyAction(ActionEvent evt) {
      if (evt.getSource() == this) {
      } else {
      }
    }
  }
  */
  public void mouseEntered(MouseEvent evt) { }
  public void mouseExited(MouseEvent evt) { }
  public void mouseClicked(MouseEvent evt) { }

  public void mouseReleased(MouseEvent evt) {
    int x = evt.getX();
    int y = evt.getY();

    if (editFlag) {
      System.out.println("End edit");
    }

    try {
      int res = (x)/av.getCharWidth() + av.getStartRes();
      int seq = av.getIndex(y);

      char resstr = align.getDrawableSequenceAt(seq).getBaseAt(res);

      endres = res;
      
      int pos = align.getDrawableSequenceAt(seq).findPosition(res);
      
      // This is to detect edits - we're at the end of an edit if mouse is up
      editFlag = false;
      startseq = -1;
      startres = -1;
      lastres = -1;
    } catch (Exception e) {
    }
    return;
  }

  /**
   * NOTE: This routine is here because getWidth (used to be maxLength)
   *       method in DrawableAlignment does much more than just getting
   *       the maximum alignment length. This method replaces places
   *       where the maxLength VARIABLE (not method) was used.
   */
  private int getMaxAlignLength() {
    return align.getWidth()-1;
  }

  public void mousePressed(MouseEvent evt) {
    int seq;
    int res;
    int pos;
    int x = evt.getX();
    int y = evt.getY();
    //System.out.println("mousePressed");

    res = (x)/av.getCharWidth() + av.getStartRes();
    seq = (y)/av.getCharHeight() + av.getStartSeq();
    
    if (seq < align.getHeight() && res < getMaxAlignLength() && res >= 0) {
      char resstr = align.getDrawableSequenceAt(seq).getBaseAt(res);
      
      // Find the residue's position in the sequence (res is the position
      // in the alignment
      
      pos = align.getDrawableSequenceAt(seq).findPosition(res);
      startseq = seq;
      
      if (startseq == (align.getHeight()-1)) {
        padseq = 1;
      } else {
        padseq = 1;
      }
      startres = res;
      lastres = res;
      
      controller.handleStatusEvent(new StatusEvent(this,"Sequence ID : " + align.getDrawableSequenceAt(seq).getName() +    
                                   " (" + seq + ") Residue = "     +  resstr + " (" + pos + ") ",StatusEvent.INFO));
    } else {
      startseq = -1;
      startres = -1;
      lastres = -1;
    }
    return;
  }

  public void mouseMoved(MouseEvent evt) { }

  /** Mouse dragging on sequence widens and shrinks gaps. If in read only
      mode this is disabled. */
  public void mouseDragged(MouseEvent evt) {

    if (readOnlyMode) return; 

    // If we're dragging we're editing
    editFlag = true;
  
    int x = evt.getX();
    int y = evt.getY();

    int res = (x)/av.getCharWidth() + av.getStartRes();
    if (res < 0) {res = 0;}
    
    if (res  != lastres) {
      if (startseq != -1) {
	char resstr = align.getDrawableSequenceAt(startseq).getBaseAt(res);
	
	// Group editing
	if (av.getGroupEdit()) {
	  int start = lastres;
	  SequenceGroup sg = align.findGroup(startseq);
	  
	  if (res < getMaxAlignLength() && res < lastres) { 
	    boolean flag = false;
	    for (int i= 0 ; i < sg.getSize(); i++) {
	      DrawableSequence s = (DrawableSequence)sg.getSequenceAt(i);
	      for (int j=lastres-1; j >= res; j--) {
		if (!flag) {
		  if (!s.residueIsSpacer(j)) {
		    res = j+1;
		    System.out.print("\07");
		    System.out.flush();
		    flag = true;
		  }
		}
	      }
	    }
	  }
	  
	  for (int i= 0 ; i < sg.getSize(); i++) {
	    DrawableSequence s = (DrawableSequence)sg.getSequenceAt(i);
	    boolean found = false;
	    int sno = -1;
	    for (int k = 0; k < align.getHeight(); k++) {
	      if (align.getDrawableSequenceAt(k) == s) {
		found = true;
		sno = k;
		break;
	      }
	    }
	    if (found && sno != -1) {
	      if (res < getMaxAlignLength() && res > lastres) {
		for (int j = lastres; j < res; j++) {
		  insertChar(j,sno);
		}

		//		drawChars(i,i+1,lastres,align.maxLength);
		int index = align.findIndex(s);
		if (index != -1) {
		  drawChars(index,index+1,lastres,getMaxAlignLength());
		}
		
	      } else if (res < getMaxAlignLength() && res < lastres) { 
		for (int j = res; j < lastres; j++) { 
		  deleteChar(j,res,sno);
		  startres = res;
		}
		int index = align.findIndex(s);
		if (index != -1) {
		  drawChars(index,index+1,res,getMaxAlignLength());
		}

		//		drawChars(i,i+1,res,getMaxAlignLength());
	      }
	    }
	    
	  }
	  lastres = res;
	} else {
	  //  System.out.println("res " + res + " " + lastres);
	  if (res < getMaxAlignLength() && res > lastres) {
	    //  System.out.println("icky");
	    for (int j = lastres; j < res; j++) {
	      insertChar(j,startseq);
	    }
	    drawChars(startseq,startseq+1,lastres,getMaxAlignLength());
	    
	  } else if (res < getMaxAlignLength() && res < lastres) { 
	    for (int j = res; j < lastres; j++) { 
	      deleteChar(j,res,startseq);
	      startres = res;
	    }
	    drawChars(startseq,startseq+1,res,getMaxAlignLength());
	  }
	}
      }
      lastres = res;
    }
    return;
  }
  public void drawChars(int seqstart, int seqend, int start, int end) {
      /**
    System.out.println("DrawChars with seqstart = " + seqstart);
    System.out.println("               seqend   = " + seqend);
    System.out.println("               start    = " + start);
    System.out.println("               end      = " + end);
      */
    seqCanvas.drawPanel(seqCanvas.getGraphics(),
                        start,end,
                        seqstart,seqend,
                        av.getStartRes(),av.getStartSeq(),
                        0);
    seqCanvas.drawPanel(seqCanvas.gg, 
                        start,end,
                        seqstart,seqend,
                        av.getStartRes(),av.getStartSeq(),
                        0);
  }
 
  private void insertChar(int j, int seq) {
    String insertion = av.getGapCharacter();
    SequenceI refSeq = align.getSequenceAt(seq);
    ((AlignSequenceI) refSeq).insertCharAt(j, insertion.charAt(0));
    align.getWidth();

    controller.handleEditEvent(new EditEvent(this,
                                             new SequenceEdit(refSeq,
                                                              SequenceI.INSERTION, 
                                                              j,
                                                              insertion)));

    // SMJS CHECK (was offx,offy)
    setScrollValues(av.getStartRes(),av.getStartSeq());
 
  }
 
  private void deleteChar(int j, int res, int sno) {
    /* why does deleteChar call getDrawableSequenceAt, but
       insertChar calls getSequenceAt???? */
    if (align.getDrawableSequenceAt(sno).residueIsSpacer(j)) {
      align.getDrawableSequenceAt(sno).deleteCharAt(j);
    }
    align.getWidth();
    controller.handleEditEvent(new EditEvent(this,
					     new SequenceEdit(align.getSequenceAt(sno),
                                                              SequenceI.DELETION,
                                                              j,
                                                              null)));
    // SMJS CHECK
    setScrollValues(av.getStartRes(),av.getStartSeq());
  }

  public JScrollBar getHScroller() {
    return hscroll;
  }
}




