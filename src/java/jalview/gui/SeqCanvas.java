package jalview.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import jalview.gui.event.*;
import jalview.util.*;

public class SeqCanvas extends ControlledCanvas implements AlignViewportListener,
                                                           ControlledObjectI,
                                                           FontChangeListener {

  Image             img;
  Graphics          gg;
  int               imgWidth;
  int               imgHeight;

  AlignViewport     av;

  int pady = 2;

  int oldstartx;
  int oldstarty;
  int oldendx;
  int oldendy;

  boolean paintFlag = false;
  boolean showScores = false;

  int chunkHeight;
  int chunkWidth;

  Graphics debugG;

  public SeqCanvas(AlignViewport av,Controller c) {
    this.av         = av;

    setController(c);
   
    //addMouseMotionListener(new MouseMotionAdapter() { public void mouseDragged(MouseEvent evt) { System.out.println("SC drag"); } });
  }

  public boolean handleAlignViewportEvent(AlignViewportEvent e) {
    paintFlag = false;
    if (e.getType() == AlignViewportEvent.COLOURING ||
        e.getType() == AlignViewportEvent.DELETE || 
        e.getType() == AlignViewportEvent.ORDER || 
        e.getType() == AlignViewportEvent.SHOW || 
	e.getType() == AlignViewportEvent.WRAP) {
      paintFlag = true;
    }
    paint(this.getGraphics());

    return true;
  }

  public void drawScale(Graphics g,int startx, int endx,int charWidth, int charHeight,int ypos) {
      int scalestartx = startx - startx%10 + 10;
      //      Enumeration e = seqPanel.alignPanel.selectedColumns.elements();
      
      //while (e.hasMoreElements()) {
	  
      //	  int sel  = ((Integer)e.nextElement()).intValue();
      //	  if ( sel >= startx  && sel <= endx) {
	      
      //	      gg.setColor(Color.red);
      //	      gg.fillRect((sel-startx)*charWidth,17-charHeight+ypos-(2)*charHeight,charWidth,charHeight);
      //	  }
      //}
      
      gg.setColor(Color.black);
      
      for (int i=scalestartx;i < endx;i+= 10) {
	  String string = String.valueOf(i);
	  gg.drawString(string,(i-startx-1)*charWidth,ypos+15 - charHeight*(2));
      }
  }


/**
 * Definitions of startx and endx (hopefully):
 * SMJS This is what I'm working towards!
 *   startx is the first residue (starting at 0) to display.
 *   endx   is the last residue to display (starting at 0).
 *   starty is the first sequence to display (starting at 0).
 *   endy   is the last sequence to display (starting at 0).
 * NOTE 1: The av limits are set in setFont in this class and  
 * in the adjustment listener in SeqPanel when the scrollbars move.
 */

  public void paint(Graphics g) {
    debugG = g;
    DrawableAlignment da = av.getAlignment();

    if (img == null ||
        imgWidth  != size().width  ||
        imgHeight != size().height ||
        paintFlag == true) {

      imgWidth  = (size().width > 0 ? size().width : 1);
      imgHeight = (size().height > 0 ? size().height : 1);

      img = createImage(imgWidth,imgHeight);
      gg  = img.getGraphics();

// SMJS I added this in to update the AV when the size changes
//      until I figure out how this should be done
      setFont(av.getFont());

      gg.setFont(av.getFont());

      paintFlag = false;

      oldstartx = -1;
      oldendx   = -1;
      oldstarty = -1;
      oldendy   = -1;
    }

    int startx = av.getStartRes();
    int starty = av.getStartSeq();

    int endx   = av.getEndRes();
    int endy   = av.getEndSeq();

    int charWidth  = av.getCharWidth();
    int charHeight = av.getCharHeight();

    chunkWidth = size().width/charWidth;
    chunkHeight =  (da.getHeight() + 2)*charHeight;

    av.setChunkHeight(chunkHeight);
    av.setChunkWidth(chunkWidth);

    int offy = av.getStartSeq();

    if (oldendx == -1) {
      fillBackground(gg,Color.white,0,0,imgWidth,imgHeight);

      if (av.getWrapAlignment() == true) {
	  startx = (int)(offy/chunkWidth)*chunkWidth;
	  endx   = startx + chunkWidth;
	  starty = offy%chunkHeight;
	  endy   = starty + da.getHeight();

	  int ypos     = 0;
	  int rowstart = starty;

	  if (starty == 0) {
	      ypos = 2*charHeight;
	  } else if (starty == 1) {
	      starty = 0;
	      ypos = charHeight;
	  }

	  if (endy > da.getHeight()) {
	      endy = da.getHeight();
	  }
	  
	  if (endx > da.getWidth()) {
	      endx = da.getWidth();
	  }

	  if (rowstart < 2) {
	      drawScale(gg,startx,endx,charWidth,charHeight,ypos);
	  }


	  drawPanel(gg,startx,endx,starty,endy,startx,starty,ypos);

	  if (rowstart == 0) {
	      ypos = ypos + chunkHeight;
	  } else if (rowstart == 1) {
	      ypos = ypos + chunkHeight;
	  } else {
	      ypos   = ypos + chunkHeight - rowstart*charHeight;
	  }

	  startx += chunkWidth;
	  endx   = startx + chunkWidth;
	  starty = 0;

	  if (endx > da.getWidth()) {
	      endx = da.getWidth();
	  }
	  // Draw the rest of the panels

	  while (ypos <= size().height) {
	      drawScale(gg,startx,endx,charWidth,charHeight,ypos);
	      drawPanel(gg,startx,endx,0,da.getHeight(),startx,starty,ypos);
	      
	      ypos   += chunkHeight;
	      startx += chunkWidth;
	      endx   = startx + chunkWidth;
	      
	      if (endy > da.getHeight()) {
		  endy = da.getHeight();
	      }
	      
	      if (endx > da.getWidth()) {
		  endx = da.getWidth();
	      }
	      
	  }
      } else {
	  drawPanel(gg,startx,endx,starty,endy,startx,starty,0);

	  oldstartx = startx;
	  oldendx   = endx;
	  oldstarty = starty;
	  oldendy   = endy;

      }
    

    }  else if (oldstartx < startx) {

      int delx  = (startx - oldstartx) * charWidth;
      int delx2 = (oldendx - startx + 1)   * charWidth;

      gg.copyArea(delx,0,delx2,da.getPixelHeight(starty,endy,charHeight),-delx,0);
      drawPanel(gg,oldendx+1,endx,starty,endy,startx,starty,0);

      oldstartx = startx;
      oldendx   = endx;

    } else if (oldstartx > startx) {

/*
      System.out.println("oldstartx > startx");
*/
      int delx  = (oldstartx - startx) * charWidth;
      int delx2 = (endx - oldstartx +1)   * charWidth;

/*
      System.out.println("Copying area between " + (oldstartx - startx) + "(" + 0 + ") and " + (endx - oldstartx) + "(" +
                         delx2 + ")");
      System.out.println("   startx  = " + startx + " oldstartx = " + oldstartx + " endx = " + endx + " delx = " + delx);
      System.out.println("   starty  = " + starty + " oldstarty = " + oldstarty + " endy = " + endy);
      System.out.println("   charWidth = " + charWidth + "  charHeight = " + charHeight);
      System.out.println("   da.getPixelHeight = " + da.getPixelHeight(starty,endy,charHeight));
*/

      gg.copyArea(0,0,delx2,da.getPixelHeight(starty,endy,charHeight),delx,0);
      drawPanel(gg,startx,oldstartx-1,starty,endy,startx,starty,0);

      oldstartx = startx;
      oldendx   = endx;

    }  else if (oldstarty < starty) {

      int dely  = da.getPixelHeight(oldstarty,starty,charHeight);
      int dely2 = da.getPixelHeight(starty,oldendy,charHeight);

      gg.copyArea(0,dely,(endx-startx+1)*charWidth,dely2,0,-dely);
      drawPanel(gg,startx,endx,oldendy,endy,startx,starty,0);

      oldstarty = starty;
      oldendy   = endy;

    } else if (oldstarty > starty) {

      int dely  = da.getPixelHeight(endy,oldendy,charHeight);
      int dely2 = da.getPixelHeight(oldstarty,endy,charHeight);

      gg.copyArea(0,0,(endx-startx+1)*charWidth,dely2,0,dely);
      drawPanel(gg,startx,endx,starty,oldstarty,startx,starty,0);

      oldstarty = starty;
      oldendy   = endy;
    }

    if ((oldendy -oldstarty) > (size().width / av.getCharWidth())) {
      System.out.println("LIMITS ERROR LIMITS ERROR");
    }

    g.drawImage(img,0,0,this);

  }

  private void tidyEdges(Graphics g) {
    int sidex = size().width / av.getCharWidth() * av.getCharWidth();
    int boty   = size().height / av.getCharHeight() * av.getCharHeight();
    fillBackground(g,
                   Color.white,
                   0,boty,size().width,size().height - boty);
    fillBackground(g,
                   Color.white,
                   sidex,0,size().width-sidex,size().height);
  }
                   


  public void drawPanel(Graphics g,int x1,int x2, int y1, int y2,int startx, int starty,int offset) {

      /**
    System.out.println("drawPanel called with g      = " + g);
    System.out.println("                      x1     = " + x1);
    System.out.println("                      x2     = " + x2);
    System.out.println("                      y1     = " + y1);
    System.out.println("                      y2     = " + y2);
    System.out.println("                      startx = " + startx);
    System.out.println("                      starty = " + starty);
      */

    g.setFont(av.getFont());

    DrawableAlignment da         = av.getAlignment();
    int               charWidth  = av.getCharWidth();
    int               charHeight = av.getCharHeight();

    //System.out.println("End x coord = " + (startx+charWidth-1) + " " + getWidth()); 

    if (y2 > starty && y1 < av.getEndSeq()) {

    fillBackground(g,
                   Color.white,
                   (x1-startx)*charWidth,
                   offset + da.getPixelHeight(starty,y1,av.getCharHeight()),
                   (x2-x1+1)*charWidth,
                   offset + da.getPixelHeight(y1,y2,av.getCharHeight()));


    if (Debug.getDebugLevel() > 0) {
      try {
        Thread.currentThread().sleep(200);
      } catch(Exception e) {
      }
    }

    for (int i = y1 ; i < y2 ;i++) {
     /**
	  if (Debug.getDebugLevel() > 0) {
        try {
          Thread.currentThread().sleep(100);
        } catch(Exception e) {
        }
        debugG.drawImage(img,0,0,this);
      }
     */
      ((DrawableSequence)(av.getAlignment().getSequenceAt(i))).drawSequence(g,
          x1,
          x2,
          (x1-startx)*charWidth,
	  offset + da.getPixelHeight(starty,i,av.getCharHeight()),charWidth,
          charHeight,showScores);


    }
    }

  }


  public void fillBackground(Graphics g,Color c, int x1,int y1,int width,int height) {
    g.setColor(c);
    g.fillRect(x1,y1,width,height);
  }

  public int getChunkWidth() {
    return chunkWidth;
  }
  public void update(Graphics g) {
    System.out.println("canvas update");
    paint(g);
  }

  public Dimension minimumSize() {
    return new Dimension(100,100);
  }

  public Dimension preferredSize() {
    return minimumSize();
  }

  public void repaint() {
    //System.out.println("repaint called");
    super.repaint();
  }
  public boolean handleFontChangeEvent(FontChangeEvent e) {
    setFont(e.getFont());
    paintFlag = true;
    repaint();
    return true;
  }

  public void setFont(int style, int size) {
    setFont(new Font(av.getFont().getName(),style,size));
  }

  public void setFont(Font f) {
    if (av == null) {
      return;
    }

    av.setFont(f);

    if (getGraphics() != null) {

      FontMetrics fm = getGraphics().getFontMetrics(f);

      int charWidth  = fm.charWidth('W');
      int charHeight = fm.getHeight();

      av.setCharWidth(charWidth);
      av.setCharHeight(charHeight);

    } else {
      av.setCharWidth(10);
      av.setCharHeight(11);

    }

    int startres = av.getStartRes();
    int startseq = av.getStartSeq();

// SMJS Was +2
    int endres   = startres + size().width/av.getCharWidth()-1;
    int endseq   = startseq + size().height/av.getCharHeight();

    if (endres > av.getAlignment().getWidth()) {
      endres = av.getAlignment().getWidth();

      startres = endres - size().width/av.getCharWidth();
      endres   = av.getAlignment().getWidth();

      if (startres < 0) {
        startres = 0;
      }
    }

    if (endseq > av.getAlignment().getHeight()) {
      endseq = av.getAlignment().getHeight();
      startseq = endseq - size().width/av.getCharWidth();

      if (startseq < 0) {
        startseq = 0;
      }

    }

    av.setStartRes(startres);
    av.setStartSeq(startseq);
    av.setEndRes(endres-1);
    av.setEndSeq(endseq);
    av.setPixelBounds(getBounds());

    //av.setFont(f);

    controller.handleAlignViewportEvent(new AlignViewportEvent(this,av,AlignViewportEvent.FONT));
  }

}
