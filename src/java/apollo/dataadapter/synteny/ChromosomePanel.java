package apollo.dataadapter.synteny;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.gui.StatusBar;
import apollo.dataadapter.synteny.*;
import apollo.gui.genomemap.*;
import apollo.util.*;

import org.bdgp.swing.AbstractDataAdapterUI;

public class ChromosomePanel extends JPanel implements MouseListener, MouseMotionListener {

  private Chromosome chr;
  private Vector     regions;
  private Hashtable  colours;
  private Vector     hitChrs;
  private Hashtable  hitRegions;
  private Vector     drawers;
  private Vector     hitOrder;
  private StatusBar  sb;
  private AbstractDataAdapterUI callback;
  String logicalQuerySpecies;
  String logicalHitSpecies;

  public ChromosomePanel(Chromosome chr,Vector regions) {
    this(chr,regions,null,null);
    setColours();
  }
  public ChromosomePanel(Chromosome chr,Vector regions,StatusBar sb) {
    this(chr,regions,null,sb, null, null, null);
    setColours();
  }
  public ChromosomePanel(
    Chromosome chr,
    Vector regions,
    Hashtable colours,
    StatusBar sb
  ){
    this(chr, regions, colours, sb, null, null, null);
  }

  public ChromosomePanel(
    Chromosome chr,
    Vector regions,
    Hashtable colours,
    StatusBar sb,
    AbstractDataAdapterUI theCallback,
    String querySpecies,
    String hitSpecies
  ){
    this.chr = chr;
    this.regions = regions;
    this.colours = colours;
    this.sb = sb;
    callback = theCallback;
    addMouseListener(this);
    addMouseMotionListener(this);

    if(colours == null){
      setColours();
    }//end if

    logicalQuerySpecies = querySpecies;
    logicalHitSpecies = hitSpecies;

    init();
  }

  private AbstractDataAdapterUI getCallback(){
    return callback;
  }

  public void init() {

    hitChrs    = new Vector();
    hitRegions = new Hashtable();
    hitOrder   = new Vector();

    regions = sortRegions(regions);

    for (int i = 0;i < regions.size(); i++) {

      SyntenyRegion reg = (SyntenyRegion)regions.elementAt(i);
      Chromosome    chr = reg.getChromosome2();

      if (!hitOrder.contains(chr)) {
        hitOrder.addElement(chr);
      }

      if (!hitRegions.containsKey(chr)) {
        Vector tmpreg = new Vector();
        tmpreg.addElement(reg);
        hitRegions.put(chr,tmpreg);
      } else {
        Vector tmpreg = (Vector)hitRegions.get(chr);
        tmpreg.addElement(reg);
      }
    }

  }
  public void setColours() {
    colours = new Hashtable();

    for (int i = 0; i < regions.size(); i++) {
      SyntenyRegion sr = (SyntenyRegion)regions.elementAt(i);

      if (!colours.containsKey(sr.getChromosome2())) {
        Color c = new Color((int)(255*(Math.random())),
                            (int)(255*(Math.random())),
                            (int)(255*(Math.random())));
        colours.put(sr.getChromosome2(),c.brighter());
      }
      if (!colours.containsKey(sr.getChromosome1())) {
        Color c = new Color((int)(255*(Math.random())),
                            (int)(255*(Math.random())),
                            (int)(255*(Math.random())));
        colours.put(sr.getChromosome1(),c.brighter());
      }
    }
  }
  public void paint(Graphics g) {

    drawers = new Vector();

    g.setColor(Color.white);
    g.fillRect(0,0,size().width,size().height);

    int xoffset = 10;
    int yoffset = 10;

    int block = size().width/12;

    // First of all draw the main chromosome
    ChromosomeDrawer cd  = new ChromosomeDrawer(chr,regions,colours,false);
    drawers.addElement(cd);
    cd.draw(g,size().width/2 - block,yoffset,2*block, size().height-20);

    // Now the smaller chromosomes

    // Number each side

    int sideCount = (int)((hitOrder.size()+1)/2);

    if (sideCount == 0) {
      return;
    }
    // calculate the height of each chromosome
    int hitHeight = (size().height-20)/sideCount;
    int count     = 0;
    int leftpos   = yoffset;
    int rightpos  = yoffset;

    for (int j = 0; j < hitOrder.size(); j++) {
      Chromosome chr     = (Chromosome)hitOrder.elementAt(j);
      Vector     regions = (Vector)hitRegions.get(chr);
      ChromosomeDrawer cd1  = new ChromosomeDrawer(chr,regions,colours,true);

      drawers.addElement(cd1);

      if (count%2 == 0) {
        cd1.draw(g,block,leftpos,block, hitHeight);
        leftpos += hitHeight;
      } else {
        cd1.draw(g,10*block,rightpos,block, hitHeight);
        rightpos += hitHeight;
      }

      // Now draw the connecting regions
      g.setColor(Color.black);

      for (int i=0; i < regions.size(); i++) {
        SyntenyRegion sr = (SyntenyRegion)regions.elementAt(i);

        Rectangle rect1 = cd1.regionToRectangle(sr);
        Rectangle rect2 = cd.regionToRectangle(sr);

        //System.out.println("Rect1 " + rect1);
        //System.out.println("Rect2 " + rect2);

        int tmpx1 = rect1.width+1;
        int tmpx2 = rect2.x;

        if (count%2 != 0) {
          tmpx1 = rect1.x-1;
          tmpx2 = rect2.width+1;
        }
        int tmpy1 = (rect1.y + rect1.height)/2;
        int tmpy2 = (rect2.y + rect2.height)/2;

        g.drawLine(tmpx1,tmpy1,tmpx2,tmpy2);
        ;

      }


      count++;
    }


  }
  private SyntenyRegion getRegion(int x,int y) {
    for (int i = 0; i < drawers.size(); i++) {
      ChromosomeDrawer cd = (ChromosomeDrawer)drawers.elementAt(i);
      //    SyntenyRegion    sr = cd.pixelToRegion(x,y);
      SyntenyRegion    sr = cd.pixelBoxToRegion(x,y);

      if (sr != null) {
        // System.out.println("Found region " + sr.getChromosome1().getDisplayId() + " " + sr.getStart1() + " " + sr.getEnd1());
        // System.out.println("Found region " + sr.getChromosome2().getDisplayId() + " " + sr.getStart2() + " " + sr.getEnd2());
        return sr;

      }
    }
    return null;
  }

  private int getBase(int x,int y) {
    for (int i = 0; i < drawers.size(); i++) {
      ChromosomeDrawer cd = (ChromosomeDrawer)drawers.elementAt(i);
      int base = cd.pixelToBase(x,y);
      if (base != -1) {
        return base;
      }
    }
    return -1;
  }

  public void mouseEntered(MouseEvent evt) {
    requestFocus();
  }
  public void mouseExited(MouseEvent evt) {}
  public void mouseClicked(MouseEvent evt) {}
  public void mousePressed(MouseEvent e) {
    if (!SwingUtilities.isLeftMouseButton(e)) {
      return;
    } else {
      SyntenyRegion reg = getRegion(e.getX(),e.getY());
      if (reg != null) {
        JPopupMenu popup =
          new SyntenyMenu(
            this,
            reg,
            getCallback(),
            getLogicalQuerySpecies(),
            getLogicalHitSpecies()
          );
        popup.show((Component)e.getSource(),e.getX(),e.getY());
      }
    }
  }

  public void mouseReleased(MouseEvent evt) {}

  public void mouseMoved(MouseEvent e) {
    SyntenyRegion reg = getRegion(e.getX(),e.getY());
    if (sb != null) {
      int base = getBase(e.getX(),e.getY());
      if (base != -1) {
        sb.setPositionPane(String.valueOf(base));
      } else {
        sb.setPositionPane("");
      }
      if (reg != null) {
        int start1 = (int)reg.getStart1()/100000;
        double s1 = start1/10.0;
        int start2 = (int)reg.getStart2()/100000;
        double s2 = start2/10.0;
        int end1   = (int)reg.getEnd1()/100000;
        double e1   = end1/10.0;
        int end2   = (int)reg.getEnd2()/100000;
        double e2   = end2/10.0;

        String mouseregion = new String(getLogicalHitSpecies() + reg.getChromosome2().getDisplayId() + " " + s2 + "M - " + e2 + "M");
        String humanregion = new String(getLogicalQuerySpecies() + reg.getChromosome1().getDisplayId() + " " + s1 + "M - " + e1 + "M");

        //String regString = reg.getChromosome1().getDisplayId() + " " + reg.getStart1() + " " +
        //                  reg.getEnd1() + " " + reg.getChromosome2().getDisplayId() + " " +
        //                  reg.getStart2() + " " + reg.getEnd2();
        sb.setFeaturePane(humanregion + " " + mouseregion);
      } else {
        sb.setFeaturePane("");
      }
    }
  }
  public void mouseDragged(MouseEvent e) {}

  public Vector sortRegions(Vector regions) {
    SyntenyRegion[] rarr = new SyntenyRegion[regions.size()];
    int[]           startarr = new int[regions.size()];

    for (int i=0; i < regions.size(); i++) {
      SyntenyRegion reg = (SyntenyRegion)regions.elementAt(i);

      rarr[i] = reg;
      startarr[i] = reg.getStart1();
    }

    QuickSort.sort(startarr,rarr);

    Vector newregions = new Vector();

    for (int i=0; i < rarr.length; i++) {
      newregions.addElement(rarr[i]);
    }
    return newregions;
  }

  public static void main(String[] args) {
    /*
    Properties props = new Properties();
    props.setProperty("jdbc_driver","org.gjt.mm.mysql.Driver");
    props.setProperty("host","ecs2d.sanger.ac.uk");
    props.setProperty("port","3306");
    props.setProperty("user","ensro");
    props.setProperty("ensembl_driver","org.ensembl.driver.plugin.standard.MySQLDriver");
    props.setProperty("database","homo_sapiens_core_11_31");

    org.ensembl.driver.Driver queryDriver = org.ensembl.driver.DriverManager.load(props);

    org.ensembl.driver.ChromosomeAdaptor queryChromosomeAdaptor = 
      (org.ensembl.driver.ChromosomeAdaptor)queryDriver.getAdaptor("chromosome");    

    props = new Properties();
    props.setProperty("jdbc_driver","org.gjt.mm.mysql.Driver");
    props.setProperty("host","ecs2d.sanger.ac.uk");
    props.setProperty("port","3306");
    props.setProperty("user","ensro");
    props.setProperty("ensembl_driver","org.ensembl.driver.plugin.standard.MySQLDriver");
    props.setProperty("database","mus_musculus_core_11_3");
    
    org.ensembl.driver.Driver hitDriver = org.ensembl.driver.DriverManager.load(props);

    org.ensembl.driver.ChromosomeAdaptor hitChromosomeAdaptor = 
      (org.ensembl.driver.ChromosomeAdaptor)queryDriver.getAdaptor("chromosome");    

    apollo.datamodel.Chromosome apolloChromosome;
    org.ensembl.datamodel.Chromosome ensjChromosome;
    
    apollo.datamodel.ChromosomeBand apolloChromosomeBand;
    org.ensembl.datamodel.ChromosomeBand ensjChromosomeBand;
    Vector apolloChromosomes = new Vector();
    Vector apolloChromosomeBands = new Vector();
    
    ensjChromosome = (org.ensembl.datamodel.Chromosome)queryChromosomeAdaptor.fetch("1");

    Iterator bands = ensjChromosome.getBands().iterator();

    while(bands.hasNext()){

      ensjChromosomeBand = (org.ensembl.datamodel.ChromosomeBand)bands.next();
      apolloChromosomeBand = 
        new apollo.datamodel.ChromosomeBand(
          ensjChromosomeBand.getBand(),
          ensjChromosome.getName(),
          ensjChromosomeBand.getChrStart(),
          ensjChromosomeBand.getChrEnd(),
          ensjChromosomeBand.getStain()
        );

      apolloChromosomeBands.add(apolloChromosomeBand);

    }//end while

    apolloChromosome = 
      new apollo.datamodel.Chromosome(
        "homo_sapiens", 
        ensjChromosome.getName(),
        apolloChromosomeBands
      );

    apolloChromosome.setLength(new Long(ensjChromosome.getLength()).intValue());

    //
    //if you have no bands, the calculations for p-length etc will be stuffed, so
    //just dummy these up based on full chromosome length. That way we get a picture
    //instead of nothing.
    if(apolloChromosome.getBands().size()<= 0){
      apolloChromosome.setPLength(apolloChromosome.getLength() / 2);
      apolloChromosome.setQLength(apolloChromosome.getLength() / 2);
      apolloChromosome.setCentroLength(apolloChromosome.getLength() / 100);
    }

    apolloChromosomes.add(apolloChromosome);
    
    
    //ChromosomePanel cp = new ChromosomePanel(chromosome,regions);
    
    JFrame frame = new JFrame(args[0]);

    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add("Center",cp);

    frame.setSize(300,500);
    frame.setLocation(100,100);
    frame.show();
     */
  }

  private String getLogicalQuerySpecies(){
    return logicalQuerySpecies;
  }//end getLogicalQuerySpecies

  private String getLogicalHitSpecies(){
    return logicalHitSpecies;
  }//end getLogicalHitSpecies
}









