package apollo.analysis;

import java.util.*;
import java.io.*;

import apollo.main.Apollo;

public class HashDB {
  DataInputStream dis;
  int             wordsize = 5;
  Hashtable       hash;
  boolean         overlapping = false;

  public HashDB(DataInputStream dis) {
    this.dis = dis;
  }

  public void setWordSize(int size) {
    this.wordsize = size;
  }
  public int getWordSize() {
    return wordsize;
  }

  public boolean isOverlapping() {
      return overlapping;
  }

  public void setOverlapping(boolean o) {
    this.overlapping = o;
  }

  public void run() {
    Apollo.setLog4JDiagnosticContext();
     if (hash == null) {
       hash = new Hashtable();
       if (overlapping == false) {
	   byte[] bytestr = new byte[wordsize];
	   int count = 1;

	   try {
	       while (dis.read(bytestr) > 0) {
		   String str = new String(bytestr);
		   
		   if (hash.get(str) == null) {
		       Vector v = new Vector();
		       v.add(new Integer(count));
		       hash.put(str,v);
		   } else {
                       System.out.println("Found existing string " + str + " " + count);
		       Vector v = (Vector)hash.get(str);
		       v.add(new Integer(count));
		   }
		   count += wordsize;
	       }
	   } catch (IOException e) {
	       System.out.println("Error parsing file : " + e);
	   }
       } else {
	   try {
	       byte[] bytestr = new byte[wordsize];
	       dis.read(bytestr);
	       String str = new String(bytestr);
	       Vector v   = new Vector();

	       v.add(new Integer(1));
	       hash.put(str.toString(),v);

	       byte[] newbyte = new byte[1];
	       int count = 1;

	       while (dis.read(newbyte) > 0) {
		   str = str.substring(1);
		   str = str + new String(newbyte);

		   if (hash.get(str) == null) {
		       Vector v2 = new Vector();
		       v2.add(new Integer(count));
		       hash.put(str,v2);
		   } else {
		       Vector v2 = (Vector)hash.get(str);
		       v2.add(new Integer(count));
		   }
		   count++;
	       }
	   } catch (IOException e) {
	       System.out.println("Exception " + e);
	   }
       }
     }
    Apollo.clearLog4JDiagnosticContext();
  }

  public Vector getPositions(String str) {
     Vector vec = (Vector)hash.get(str);
     if (vec != null) {
        System.out.println("Size " + str + " " + vec.size());
     }
     if (vec != null && vec.size() >= 10) {
        return new Vector();
     } else {
        return vec;
     }
  }

  public String toString() {
     Enumeration  en  = hash.keys();
     StringBuffer str = new StringBuffer();

     while (en.hasMoreElements()) {
        String key = (String)en.nextElement();

        str.append( key);
        str.append(" : ");
        str.append(hash.get(key));
        str.append(" : ");
        str.append(((Vector)hash.get(key)).size());
        str.append("\n");
     }
     return str.toString();
  }
  public static void main(String args[]) {
      try {
      DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(args[0])));

      HashDB hashdb = new HashDB(dis);
      hashdb.setWordSize(Integer.parseInt(args[1]));
      long start = System.currentTimeMillis();
      hashdb.run();
      long end   = System.currentTimeMillis();

      System.out.println("Time for hash " + ((end-start)+1)/1000);
      System.out.println(hashdb);
      } catch (FileNotFoundException e) {
         System.out.println("Exception " + e);
      }
  }
}
