package apollo.analysis;

import java.util.*;
import java.io.*;

import apollo.main.Apollo;
import apollo.datamodel.*;
import apollo.seq.io.*;

import org.bdgp.util.DNAUtils;

public class HashMatch {
    HashDB   hash;
    SequenceI seq;

    public HashMatch(SequenceI seq, HashDB hash) {
       this.hash = hash;
       this.seq  = seq;
    }
    
    public void run() {
      Apollo.setLog4JDiagnosticContext();
      long start = System.currentTimeMillis();
      hash.run();
      long end   = System.currentTimeMillis();

      System.out.println("Time for hash " + ((end-start)+1)/1000);

      matchSeq(seq);
      Apollo.clearLog4JDiagnosticContext();
    }

    public void matchSeq(SequenceI seq) {
        long start = System.currentTimeMillis();

        int wordsize = hash.getWordSize() -1;
        
        for (int i = 1; i < seq.getLength(); i++) {
           int end = i + wordsize;

           if (end < seq.getLength()) {

              String str = seq.getResidues(i,end);
              Vector pos = hash.getPositions(str);

              if (pos != null) {


                for (int j = 0; j < pos.size(); j++) {
                  int hashpos = ((Integer)pos.elementAt(j)).intValue();
                  System.out.println(seq.getName() + "\thash\tsimilarity\t" + 
                  i + "\t" + (i+wordsize-1) + "\t100\t1\t.\t" + 
                  "hit\t" + hashpos + "\t" + (hashpos+wordsize-1) + "\n");
               }
              }
           }
        }

        String rev = DNAUtils.reverseComplement(seq.getResidues());

        for (int i = 0; i < rev.length(); i++) {
           int end = i + wordsize;
           if (end < rev.length()) {

              String str = rev.substring(i,end+1);
              Vector pos = hash.getPositions(str);


              if (pos != null) {
                for (int j = 0; j < pos.size(); j++) {
                  int hashpos = ((Integer)pos.elementAt(j)).intValue();
                  System.out.println(seq.getName() + "\thash\tsimilarity\t" + 
                  (seq.getLength()-i-wordsize+1) + "\t" + (seq.getLength()-i) + "\t100\t-1\t.\t" + 
                  "hit\t" + hashpos + "\t" + (hashpos+wordsize-1) + "\n");
               }
              }
           }
        }
        long end = System.currentTimeMillis();
        System.out.println("Time for match " + ((end-start)+1)/1000.0);
     }

     public static void main(String args[]) {

      try {
      DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(args[0])));

      HashDB hashdb = new HashDB(dis);
      hashdb.setWordSize(Integer.parseInt(args[1]));
      
      DataInputStream dis2 = new DataInputStream(new BufferedInputStream(new FileInputStream(args[2])));

      FastaFile ff = new FastaFile(dis2,false);
      for (int i = 0; i < ff.getSeqs().size(); i++) {
       SequenceI seq = (Sequence)ff.getSeqs().elementAt(0);
       HashMatch hm = new HashMatch(seq,hashdb);
       hm.run();
      }
      } catch (FileNotFoundException e) {
         System.out.println("Exception " + e);
      }

   }
}
