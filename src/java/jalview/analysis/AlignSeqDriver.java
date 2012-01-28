package jalview.analysis;

import jalview.gui.schemes.*;
import jalview.gui.*;
import jalview.datamodel.*;
import jalview.util.*;
import jalview.io.*;

import java.util.*;
import java.io.*;
import java.awt.*;

public class AlignSeqDriver extends Driver {

  public static void main(String[] args) {

    try {
      if (args.length != 3) {
        System.out.println("args: <msffile> <File|URL> <pep|dna>");
        System.exit(0);
      }
      MSFfile msf = new MSFfile(args[0],args[1]);
      AlignSequenceI[] s = new AlignSequence[msf.getSeqs().size()];
      int scores[][] = new int[msf.getSeqs().size()][msf.getSeqs().size()];
      int totscore = 0;
      for (int i=0;i < msf.getSeqs().size();i++) {
        s[i] = (AlignSequenceI)msf.getSeqs().elementAt(i);
      }

      for (int i = 1; i < 2; i++) {
        for (int j = 0; j < i; j++) {
          AlignSeq as = new AlignSeq(s[i],s[j],args[2]);
          as.calcScoreMatrix();
          as.traceAlignment();
          as.printAlignment();

          //as.printScoreMatrix(as.score);
          //as.printScoreMatrix(as.E);
          //as.printScoreMatrix(as.F);
          //as.printScoreMatrix(as.traceback);
          scores[i][j] = as.maxscore;
          totscore = totscore + as.maxscore;
          System.out.println(as.output);
          Frame f = new Frame("Score matrix");
          Panel p = new Panel();
          p.setLayout(new BorderLayout());
          ick ic = new ick(as);
          p.add("Center",ic);
          f.setLayout(new BorderLayout());
          f.add("Center",p);
          f.resize(500,500);
          f.show();

        }
      }
      System.out.println();
      System.out.print("      ");
      for (int i = 1; i < msf.getSeqs().size(); i++) {
        Format.print(System.out,"%6d ",i);
      }
      System.out.println();
      for (int i = 1; i < msf.getSeqs().size(); i++) {
        Format.print(System.out,"%6d",i+1);
        for (int j = 0; j < i; j++) {
          Format.print(System.out,"%7.3f",(float)scores[i][j]/totscore);
        }
        System.out.println();
      }
    }
    catch (Exception e) {
      System.out.println("Exception : " + e);
    }

    //   AlignSeq as = new AlignSeq("GTGASAAATGGNNTGATTCTGTACCTTGTGGAGACTGGCGTGATGTGCAGCAACTATTCGANNGTGATCCAGTGGTTTTGTCGTTGAATCTGTCTTCGATGGTTCTCGGGTAAGCTATCACCAAGCATAGGTGGATTGGTTCATCTGAAGCAGCTGGATCTGTCATATAATGGGTTGTCAGNGGAAAATTCCTAAGGAAATTGGCAACTGTTCAAGCTTGGAGATTCTGAAACTAAACAATAACCAGTTTGATGGTGAGATACCTGTGGAAATAGGTAAGCTTGTGTCTTTGGAGAATCTGATCATATCAACAACAGAATCTCAGGGTCTCTCCCTGTGGAGATTGG","AAATGATTCTGTACCTTGTGGATGGACTGGCGTGATGTGCAGCAACTATTCGAGTGATCCAGAGGTTTTGTCCTTGAATCTGTCTTCGATGGTTCTCTCGGGTAAGATCCACCAAGCATA");
  }

}



class ick extends Canvas {
  AlignSeq fs;
  public ick(AlignSeq fs) {
    super();
    this.fs = fs;
  }

  public void paint(Graphics g) {
    fs.displayMatrix(g,fs.score,fs.seq1.length,fs.seq2.length,(int)500/fs.seq1.length);
  }

}
