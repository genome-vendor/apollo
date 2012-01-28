package jalview.gui;

import java.awt.*;

import jalview.gui.event.*;
import jalview.util.*;
import jalview.datamodel.*;

import javax.swing.*;

public class IdCanvas extends ControlledCanvas implements AlignViewportListener,
                                                ControlledObjectI,
                                                SequenceSelectionListener {
  protected Image    img;
  protected Graphics gg;

  protected int imgWidth;
  protected int imgHeight;

  protected AlignViewport av;

  protected boolean paintFlag = false;
  protected boolean showScores = true;

  protected int idWidth = 120;
  protected int maxIdLength = -1;
  protected String maxIdStr = null;

  protected DrawableAlignment da;
  public IdCanvas(AlignViewport av, Controller c) {
    this.av         = av;

    setController(c);
  }


  public boolean handleAlignViewportEvent(AlignViewportEvent e) {
      if (e.getType() == AlignViewportEvent.HSCROLL) {
	  paintFlag = false;
      }
    paint(this.getGraphics());
    return true;
  }

  public boolean handleSequenceSelectionEvent(SequenceSelectionEvent e) {
    paint(this.getGraphics());
    return true;
  }

  public void drawIdString(Graphics g,DrawableSequence ds,int i, int starty, int ypos) {
      int charHeight = av.getCharHeight();

      if (av.getSelection().contains(ds)) {
	  gg.setColor(Color.gray);
	  gg.fillRect(0,da.getPixelHeight(starty,i,charHeight)+ ypos,size().width,charHeight);
	  gg.setColor(Color.white);
      } else {
	  gg.setColor(ds.getColor());
	  gg.fillRect(0,da.getPixelHeight(starty,i,charHeight)+ ypos,size().width,charHeight);
	  gg.setColor(Color.black);
      }
	    
      String string = ds.getName() + "/" + ds.getStart() + "-" + ds.getEnd();

      if (showScores) {
	  gg.drawString(string,0,da.getPixelHeight(starty,i,charHeight)+ ypos+charHeight/2);
      } else {
	  gg.drawString(string,0,da.getPixelHeight(starty,i,charHeight) + ypos + charHeight);
      }
  }
  public void paint(Graphics g) {
    da         = av.getAlignment();
    int          charWidth  = av.getCharWidth();
    int               charHeight = av.getCharHeight();
    Font              f          = av.getFont();

    if (img == null ||
        imgWidth != size().width   ||
        imgHeight != size().height ||
        paintFlag == true) {
      imgWidth = size().width;
      idWidth  = size().width;
      //System.out.println("Repainting");
      FontMetrics fm = g.getFontMetrics(f);

      idWidth   = fm.stringWidth(maxId(fm));
      imgHeight = size().height;

      if (imgWidth <= 0 )  {
        imgWidth  = 700;
      }
      if (imgHeight <= 0 ) {
        imgHeight = 500;
      }

      img = createImage(imgWidth,imgHeight);

      gg = img.getGraphics();
      gg.setColor(Color.white);
      gg.fillRect(0,0,imgWidth,imgHeight);

      gg.setFont(f);

      fm = gg.getFontMetrics(f);

      paintFlag = false;

    }

    //Fill in the background
    gg.setColor(Color.white);
    gg.fillRect(0,0,imgWidth,imgHeight);

    Color currentColor     = Color.white;
    Color currentTextColor = Color.black;

    //Which ids are we printing
    int starty = av.getStartSeq();
    int endy   = av.getEndSeq();


    if (av.getWrapAlignment()) {
	starty = starty%av.getChunkHeight();

	int ypos     = 0;
	int rowstart = starty;

	if (starty == 0) {
	    ypos = 2*charHeight;
	} else if (starty == 1) {
	    starty = 0;
	    ypos = charHeight;
	}

	endy   = starty + da.getHeight();
	
	if (endy > da.getHeight()) {
	    endy = da.getHeight();
	}	
	
	for (int i = starty; i < endy; i++) {
	    DrawableSequence ds = da.getDrawableSequenceAt(i);
	    drawIdString(gg,ds,i,starty,ypos);
	}
	if (rowstart == 0) {
	    ypos   = ypos +  av.getChunkHeight();
	} else if (rowstart == 1) {
	    ypos   = ypos +  av.getChunkHeight();
	} else {
	    ypos   = ypos +  av.getChunkHeight() - rowstart*charHeight;
	}
	
	starty = 0;
	
	int chunkwidth = av.getChunkWidth();
	int startx = (int)(av.getEndSeq()/chunkwidth)*chunkwidth;
	int endx   = startx + chunkwidth;
	
	while (ypos <= size().height && endx < da.getWidth()) {

	    //System.out.println("IdPanel start/endy ypos " + starty + " " + endy + " " + ypos);
	    //System.out.println("IdCanvas start/endy ypos " +  starty + " " + endy + " " + ypos);
	    
	    for (int i = starty; i < endy; i++) {
		DrawableSequence ds = da.getDrawableSequenceAt(i);
		drawIdString(gg,ds,i,starty,ypos);
	    }
	    
	    ypos   += av.getChunkHeight();
	    startx += chunkwidth;
	    endx = startx + chunkwidth;
	    
	    if (endx > da.getWidth()) {
		endx = da.getWidth();
	    }
	    
	    starty = 0;
	    
	    if (endy > da.getHeight()) {
		endy = da.getHeight();
	    }
	    
	}
    } else {

    //Now draw the id strings

    for (int i = starty; i < endy; i++) {

      // Selected sequence colours

      if (av.getSelection().contains(da.getDrawableSequenceAt(i))) {
        if (currentColor != Color.gray) {
          currentColor     = Color.gray;
          currentTextColor = Color.white;
        }
      } else if (da.getDrawableSequenceAt(i).getColor() != null) {
        Color newcol = da.getDrawableSequenceAt(i).getColor();
        if (newcol != currentColor) { 
          currentColor = newcol;
          if (newcol == Color.black) {
            currentTextColor = Color.white;
          } else {
            currentTextColor = Color.black;
          }
        }
      }

      gg.setColor(currentColor);

      if (currentColor != Color.white) {
        gg.fillRect(0,
                    da.getPixelHeight(starty,i,charHeight),
                    size().width,
                    charHeight);
      }

      gg.setColor(currentTextColor);
        
      String string = da.getSequenceAt(i).getName();

      gg.drawString(string,0,da.getPixelHeight(starty,i,charHeight) + charHeight - 2);
    }
    }

    g.drawImage(img,0,0,this);
  }

  public void update(Graphics g) {
    paint(g);
  }

  public Dimension minimumSize() {
    if (idWidth != 0) {
      return new Dimension(idWidth + 20,size().height);
    } else {
      return new Dimension(100,100);
    }
  }

  public Dimension preferredSize() {
    return minimumSize();
  }

  public int maxIdLength() {
    if (maxIdLength == -1) {
      System.out.println("Calculating maxIdLength");
      int max = 0;
      DrawableAlignment al = av.getAlignment();
      
      int i   = 0;

      while (i < al.getHeight() && al.getSequenceAt(i) != null) {
        if (al.getSequenceAt(i).getName().length() > max) {
          max = al.getSequenceAt(i).getName().length();
        }
        i++;
       }
       maxIdLength = max;
    }
    return maxIdLength;
  }
  public String maxId(FontMetrics fm) {
    if (maxIdStr == null) {
      //System.out.println("Calculating maxIdStr");
      DrawableAlignment al = av.getAlignment();

    int    max = 0;
    String maxStr = "";
    int i   = 0;

    while (i < al.getHeight() && al.getSequenceAt(i) != null) {
      AlignSequenceI s   = al.getSequenceAt(i);
      String str   = s.getName();
      if (fm.stringWidth(str) > max) {
        max = fm.stringWidth(str);
        maxStr = str;
      }
      i++;
    }
    maxIdStr = maxStr;
    }
    return maxIdStr;
  }
}
