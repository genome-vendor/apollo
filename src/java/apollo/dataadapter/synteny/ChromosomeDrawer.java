package apollo.dataadapter.synteny;

import java.util.*;
import java.awt.*;
import javax.swing.*;

import apollo.datamodel.*;

public class ChromosomeDrawer {

  private Chromosome chromosome;
  private Vector     regions;
  private Hashtable  colours;
  private boolean    hit;
  private int        xstart;
  private int        ystart;
  private int        width;
  private int        height;

  public ChromosomeDrawer(Chromosome chromosome, Vector regions,Hashtable colours,boolean hit) {
    this.chromosome = chromosome;
    this.regions    = regions;
    this.colours    = colours;
    this.hit        = hit;
  }

  public void draw(Graphics g, int x, int y, int width, int height) {
    this.xstart = x;
    this.ystart = y;
    this.width = width;
    this.height = height;

    int  yoffset = 20;

    g.setColor(Color.black);
    g.setFont(new Font("Helvetica",Font.PLAIN,8));

    String label    = "Chr " + chromosome.getDisplayId();
    int    strwidth = g.getFontMetrics().stringWidth(label);

    g.drawString(label,x + width/2 - strwidth/2,y+15);

    int  ypos      = y + yoffset;
    int  tmpheight = (height - yoffset);

    // We save 10% of the height for the top and bottom pieces of chromosome

    g.drawArc(x,ypos+tmpheight/40,width,tmpheight/20,0,180);

    ypos = ypos + tmpheight/20;

    // P arm
    float frac = (float)(1.0*chromosome.getPlength()/chromosome.getLength());
    int   len  = (int)(frac*9*tmpheight/10);


    g.drawLine(x,      ypos,x,      ypos+len);
    g.drawLine(x+width,ypos,x+width,ypos+len);

    ypos += len;

    // Centromere
    frac = (float)(1.0*chromosome.getCentroLength()/chromosome.getLength());
    len  = (int)(frac*9*tmpheight/20);


    g.drawLine(x,      ypos,x+width/2,ypos+len);
    g.drawLine(x+width,ypos,x+width/2,ypos+len);

    ypos += len;

    g.drawLine(x,    ypos+len,x+width/2,    ypos);
    g.drawLine(x+width,ypos+len,x+width/2,ypos);

    ypos += len;

    // Q arm
    frac = (float)(1.0*chromosome.getQlength()/chromosome.getLength());
    len  = (int)(frac*9*tmpheight/10);


    g.drawLine(x,      ypos,x,      ypos+len);
    g.drawLine(x+width,ypos,x+width,ypos+len);

    ypos += len;

    // Bottom curve
    g.drawArc(x,ypos-tmpheight/40,width,tmpheight/20,180,180);

    // Now the regions;

    for (int i=0; i < regions.size(); i++) {
      SyntenyRegion reg = (SyntenyRegion)regions.elementAt(i);

      Color  col;
      float tmp1;
      float tmp2;

      col = (Color)colours.get(reg.getChromosome2());

      if (hit) {
        tmp1  = (float)(1.0*reg.getStart2()/reg.getChromosome2().getLength());
        tmp2  = (float)(1.0*(reg.getEnd2()-reg.getStart2()+1)/reg.getChromosome2().getLength());
      } else {
        tmp1  = (float)(1.0*reg.getStart1()/chromosome.getLength());
        tmp2  = (float)(1.0*(reg.getEnd1()-reg.getStart1()+1)/chromosome.getLength());
      }

      ypos = y + yoffset + tmpheight/20 + (int)(9*tmp1*tmpheight/10);

      len  = (int)(tmp2*9*tmpheight/10);

      g.setColor(col);
      g.fillRect(x+1,ypos,width-2,len);
      g.setColor(col.darker());
      g.drawRect(x+1,ypos,width-2,len);
    }
  }

  public int pixelToBase(int x, int y) {
    if (x >= xstart && x <= (xstart+width)) {
      int tmpystart = ystart + 20 + height/20;
      int tmpyend   = tmpystart + 9*(height-20)/10;

      if (y >= tmpystart && y <= tmpyend) {
        int base = (int)(1.0*(y-tmpystart)*chromosome.getLength()/(tmpyend-tmpystart+1));
        return base;
      }
    }
    return -1;
  }

  public SyntenyRegion pixelToRegion(int xpixel, int ypixel) {
    int base = pixelToBase(xpixel,ypixel);
    System.out.println(" Base " + base);
    if (base != -1) {
      for (int i = 0; i < regions.size(); i++) {
        SyntenyRegion sr = (SyntenyRegion)regions.elementAt(i);
        if (hit) {
          System.out.println("Region hit " + sr.getStart2() + " " + sr.getEnd2());
          if (base >= sr.getStart2() && base <= sr.getEnd2()) {
            return sr;
          }
        } else {
          System.out.println("Region " + sr.getStart1() + " " + sr.getEnd1());
          if (base >= sr.getStart1() && base <= sr.getEnd1()) {
            return sr;
          }
        }
      }
    }
    return null;
  }

  public SyntenyRegion pixelBoxToRegion(int xpixel, int ypixel) {
    int base1 = pixelToBase(xpixel,ypixel-1);
    int base2 = pixelToBase(xpixel,ypixel);
    // System.out.println(" Base1 " + base1);
    // System.out.println(" Base2 " + base2);
    if (base1 > base2) {
      int tmp = base1;
      base1 = base2;
      base2 = tmp;
    }
    if (base1 != -1 && base2 != -1) {
      for (int i = 0; i < regions.size(); i++) {
        SyntenyRegion sr = (SyntenyRegion)regions.elementAt(i);
        if (hit) {
          // System.out.println("Region hit " + sr.getStart2() + " " + sr.getEnd2());
          if (base2 >= sr.getStart2() && base1 <= sr.getEnd2()) {
            return sr;
          }
        } else {
          // System.out.println("Region " + sr.getStart1() + " " + sr.getEnd1());
          if (base2 >= sr.getStart1() && base1 <= sr.getEnd1()) {
            return sr;
          }
        }
      }
    }
    return null;
  }

  public Rectangle regionToRectangle(SyntenyRegion reg) {
    for (int i = 0; i < regions.size(); i++) {
      SyntenyRegion sr = (SyntenyRegion)regions.elementAt(i);

      int tmpheight = height-20;

      if (sr == reg) {
        float tmp1;
        float tmp2;

        if (hit) {
          tmp1  = (float)(1.0*reg.getStart2()/reg.getChromosome2().getLength());
          tmp2  = (float)(1.0*(reg.getEnd2()-reg.getStart2()+1)/reg.getChromosome2().getLength());
        } else {
          tmp1  = (float)(1.0*reg.getStart1()/chromosome.getLength());
          tmp2  = (float)(1.0*(reg.getEnd1()-reg.getStart1()+1)/chromosome.getLength());
        }

        int ypos = ystart + 20 + tmpheight/20 + (int)(9*tmp1*tmpheight/10);

        int len  = (int)(tmp2*9*tmpheight/10);


        return new Rectangle(xstart+1,ypos,xstart + width-1,ypos + len);
      }
    }
    return null;
  }
}
