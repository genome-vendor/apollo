package apollo.analysis;

import java.util.*;
import java.io.*;

import apollo.datamodel.*;
import apollo.main.Apollo;

public class StreamMatch {
    HashDB          hash;
    DataInputStream dis;

    public StreamMatch(DataInputStream dis, HashDB hash) {
       this.hash = hash;
       this.dis  = dis;
    }
    
    public void run() {
      Apollo.setLog4JDiagnosticContext();

      long start = System.currentTimeMillis();
      hash.run();
      long end   = System.currentTimeMillis();

      System.out.println("Time for hash " + ((end-start)+1)/1000);

      streamDB();
      Apollo.clearLog4JDiagnosticContext();
    }

    public void streamDB() {
        long start    = System.currentTimeMillis();
        int  wordsize = hash.getWordSize();

	byte[] bytestr = new byte[wordsize];
	Vector hits    = new Vector();

	try {
	    while (dis.read(bytestr) > 0) {
		String str = new String(bytestr);
		Vector pos = (Vector)hash.getPositions(str);
		if (pos != null) {
		    hits.addElement(pos);
		}
 
	    }

        } catch (IOException e) {
	    System.out.println("Exception reading data stream " + e);
	}
        long end = System.currentTimeMillis();
        System.out.println("Time for stream " + ((end-start)+1)/1000.0);

	System.out.println("Number of hits " + hits.size());
	System.out.println("Hits " + hits);
    }

     public static void main(String args[])  {

     try {
      DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(args[0])));
  
      HashDB hashdb = new HashDB(dis);
      hashdb.setOverlapping(true);
      hashdb.setWordSize(Integer.parseInt(args[1]));
      hashdb.run();

      System.out.println(hashdb);

      DataInputStream stream = new DataInputStream(new BufferedInputStream(new FileInputStream(args[2])));

      StreamMatch sm = new StreamMatch(stream,hashdb);

      sm.run();

      } catch (FileNotFoundException e) {
         System.out.println("Exception " + e);
      }

   }
}
