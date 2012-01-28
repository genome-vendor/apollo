package jalview.analysis;

import jalview.io.*;
import jalview.datamodel.*;
import jalview.gui.*;

import java.awt.*;

public class PCADriver {

  public static void printMemory(Runtime rt) {
    System.out.println("Free memory = " + rt.freeMemory());
  }

  public static void main(String[] args) {
    long tstart = System.currentTimeMillis();
    Runtime rt = Runtime.getRuntime();
    printMemory(rt);
    try {
      // Read the sequence file
      long tend = System.currentTimeMillis();
      System.out.println("Reading file " + (tend-tstart) + "ms");
      tstart = System.currentTimeMillis();
      MSFfile msf = new MSFfile(args[0],"File");
      tend = System.currentTimeMillis();
      System.out.println("done " + (tend-tstart) + "ms");
      System.out.println("Creating sequences");
      tstart = System.currentTimeMillis();
      DrawableSequence[] s = new DrawableSequence[msf.getSeqs().size()];
      for (int i=0;i < msf.getSeqs().size();i++) {
        s[i] = new DrawableSequence((AlignSequence)msf.getSeqs().elementAt(i));
      }
      tend = System.currentTimeMillis();
      System.out.println("done " + (tend-tstart) + "ms");
      System.out.println("Diagonalizing matrix");
      tstart = System.currentTimeMillis();
      PCA pca = new PCA(s);
      pca.run();
      tend = System.currentTimeMillis();
      System.out.println("done " + (tend-tstart) + "ms");
      System.out.println("Finding component coords");
      tstart = System.currentTimeMillis();

      // Now find the component coordinates
      double[][] comps = new double[msf.getSeqs().size()][msf.getSeqs().size()];

      for (int i=0; i < msf.getSeqs().size(); i++ ) {
        if (pca.eigenvector.d[i] > 1e-4) {
          comps[i]  = pca.component(i);
        }

      }
      tend = System.currentTimeMillis();
      System.out.println("done " + (tend-tstart) + "ms");
      System.out.println("Creating frame");
      tstart = System.currentTimeMillis();

      // for (int j = 0; j < s.length; j++ ){
      //	Format.print(System.out,"%20s",s[j].name + " ");
      //	int i = s.length-1;
      //	while (i >= 0 ) {
      //	  Format.print(System.out,"%13.4e",comps[i][j]);
      //	  i--;
      //	}
      //	System.out.println();
      //}
      //System.out.println();



      Frame f = new Frame();
      f.setLayout(new BorderLayout());
//MG      PCAPanel p  = new PCAPanel(f,pca,s);
//MG      f.add("Center",p);
      f.resize(400,400);

//MG      AlignFrame af = new AlignFrame(p,s);//
//MG      af.resize(700,300);
//MG      af.show();
      f.show();
      //System.exit(0);
    }
    catch (java.io.IOException e) {
      System.out.println("IOException : " + e);
    }
  }
}
