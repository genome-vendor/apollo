package apollo.gui;

import javax.swing.*;
import apollo.dataadapter.*;
import apollo.dataadapter.synteny.ChromosomePanel;
import apollo.datamodel.*;
import apollo.gui.event.*;
import java.awt.*;
import java.util.*;
import java.io.*;
import gov.sandia.postscript.*;
import java.awt.event.*;
import java.awt.print.*;

/** This doesnt seem to be used anymore - delete? */

public class FullSyntenyPanel extends JPanel implements ActionListener {
  String org1;
  String org2;
  String fileStub = ".gff";
  JPanel chrPanel = new JPanel();
  StatusBar sb = new StatusBar();
  public Vector panels;

  public FullSyntenyPanel() {
    this("homo_sapiens","mus_musculus");
  }
  public FullSyntenyPanel(String org1, String org2) {
    this.org1 = org1;
    this.org2 = org2;
  }


  public void init() {
    int count = 1;
    panels = new Vector();
    setLayout(new BorderLayout());
    chrPanel.setLayout(new GridLayout(4,6));
    chrPanel.setBackground(Color.white);

    //KaryotypeFileAdapter ca = new KaryotypeFileAdapter();
    KaryotypeAdapter ca = new KaryotypeAdapter();

    Karyotype org1Kary = ca.getKaryotypeBySpeciesName(org1);
    Karyotype org2Kary = ca.getKaryotypeBySpeciesName(org2);

    Hashtable colours = new Hashtable();
    Vector chr1 = org1Kary.getChromosomes();

    for (int cc = 0; cc < chr1.size(); cc++) {
      Chromosome chrtmp = (Chromosome)chr1.elementAt(cc);
      String chr = chrtmp.getDisplayId();

      String filename = chr + fileStub;

      SyntenyGFFAdapter sga = new SyntenyGFFAdapter(filename,org1,org2);

      Vector regions       = sga.getSyntenyRegions();

      if (regions != null) {
        for (int i=0; i < regions.size(); i++) {
          SyntenyRegion reg = (SyntenyRegion)regions.elementAt(i);
          Chromosome tmpchr = reg.getChromosome2();

          if (!colours.containsKey(tmpchr)) {
            Color c = new Color((int)(255*(Math.random())),
                                (int)(255*(Math.random())),
                                (int)(255*(Math.random())));
            colours.put(tmpchr,c.brighter());
          }
        }
      }
    }

    for (int cc = 0; cc < chr1.size(); cc++) {
      String name = String.valueOf(cc+1);
      if (name.equals("23")) {
        name = "X";
      }
      System.out.println("name is " + name);
      Chromosome chrtmp =  org1Kary.getChromosomeByName(name);
      if (chrtmp != null) {
        //Chromosome chrtmp = (Chromosome)chr1.elementAt(cc);
        String chr = chrtmp.getDisplayId();

        String filename       = chr + fileStub;
        SyntenyGFFAdapter sga = new SyntenyGFFAdapter(filename,org1,org2);
        Vector regions        = sga.getSyntenyRegions();

        ChromosomePanel cp = null;

        if (sga.getChromosome1() != null) {
          System.out.println("Chromosome " + chr + " length " + sga.getChromosome1().getLength());
          cp    = new ChromosomePanel(sga.getChromosome1(),regions,sb);

          panels.addElement(cp);
          chrPanel.add(cp);

        } else {
          System.out.println("No hits " + chr + " Generating dummy chromosome");
          Chromosome tmpchr = org1Kary.getChromosomeByName(chr);
          Vector tmpreg = new Vector();

          cp    = new ChromosomePanel(tmpchr,tmpreg,sb);

          panels.addElement(cp);
          chrPanel.add(cp);
        }
      }
      count++;
    }
    add(chrPanel,BorderLayout.CENTER);
    add(sb,BorderLayout.SOUTH);
  }

  public void actionPerformed(ActionEvent e) {
     System.out.println("Got action event");
     //printPanel();
     psPrint();
  }
  public static void main(String[] args) {
    JFrame frame = new JFrame();

    JMenuBar menuBar = new JMenuBar();

    frame.setJMenuBar(menuBar);

    JMenu fileMenu = new JMenu("File");
    menuBar.add(fileMenu);
    fileMenu.setMnemonic('F');
    JMenuItem print = new JMenuItem("Save as postscript...");
    fileMenu.add(print);

    FullSyntenyPanel fsp;

    if (args.length == 3) {
      frame.setTitle(args[1] + "_" + args[2]);
      fsp = new FullSyntenyPanel(args[1],args[2]);
      fsp.fileStub = args[0];
    } else {
      fsp  = new FullSyntenyPanel();
    }
    fsp.init();
    print.addActionListener(fsp);
    frame.setJMenuBar(menuBar);
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.addWindowListener(new BasicWindowListener());
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add("Center",fsp);

    frame.setSize(700,700);
    frame.show();

  }

  public void psPrint() {
      Graphics psg = null;
      PrintWriter fw = null;
      double	   scaleVal = 1.0;
      double scale = 0.0;
      try {
        String startPath = System.getProperty("user.home");
   
        JFileChooser chooser = new JFileChooser(startPath);
  
        int returnVal = chooser.showSaveDialog(this);
  
        File file = chooser.getSelectedFile();
    
        if (returnVal != JFileChooser.APPROVE_OPTION) {
          return;
        }
	fw       = new PrintWriter(new FileWriter(file));
	psg      = new PSGr2(fw);
	scale    = PSGrBase.getPSScale(getSize(),"portrait");
	
       } catch (Exception e) {
        System.out.println("Got exception " + e);
       }	
	try {
	    scale = new Double(scale).doubleValue();
	} catch (Exception e) {
	    System.out.println("Invalid scale factor");
	    return;
	}
	
	fw.println("-30 30 translate");
	int yOffset = (int)(762.0 - 792.0*scaleVal );
	fw.println("30 " + yOffset + " translate");
	fw.println("" + scale + " " + scale + " scale");

        int width = 30;
        int height = 762;
        psg.translate(30,100);
        for (int i = 0; i < panels.size(); i++) {
          ChromosomePanel p = (ChromosomePanel)panels.elementAt(i);
	  p.paint(psg);
          psg.translate(p.getSize().width,0); 

          if ((i+1)%6 ==0) {
            psg.translate(-6*p.getSize().width,-1*p.getSize().height);
          }
        }
    }
}
