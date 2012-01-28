  /*
	 Copyright (c) 1997
	 University of California Berkeley
 */

package apollo.analysis.filter;

import java.lang.*;
import java.util.*;

import org.apache.log4j.*;

public class Compress {
  
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(Compress.class);

  //  public static double compress (String str, int wordsize) {
  public static double compress (String str, int wordsize, boolean debug) {
    int by_word = getBits (str, wordsize);
    // return ((double) (by_word * wordsize * 100) / (double) (au_natural));
    double alphabet_size = (str.length() / wordsize);
    int bits_for_alphabet = (alphabet_size < 5 ? 2 : 
			     alphabet_size < 9 ? 3 :
			     alphabet_size < 17 ? 4 : 
			     alphabet_size < 33 ? 5 :
			     alphabet_size < 65 ? 7 : 8);

    logger.debug("str length=" + str.length() + 
                 " alphabet_size= " + alphabet_size +
                 " bits_for_alphabet=" + bits_for_alphabet +
                 " multipled=" + alphabet_size * bits_for_alphabet +
                 " bits per char=" + (2*str.length()) +
                 " wordsize=" + wordsize + 
                 " bits by_words=" + by_word);

    return ((double) (by_word * 100) / 
	    (double) (alphabet_size * bits_for_alphabet));
  }

  protected static int getBits (String str, int wordsize) {
    Hashtable nodes = new Hashtable ();
    Hashtable leaves = new Hashtable ();
    HuffNode node;
	
    /*
      The first step is to count the frequency of each 
      base or amino acid in the sequence to be encoded
	  
      /* Count the frequency of each word in the string */
    for (int i = 0; i < str.length(); i += wordsize) {
      String word;
      if ((i + wordsize) < str.length())
	word = str.substring(i, i + wordsize);
      else
	word = str.substring(i);
	    
      node = (HuffNode) nodes.get (word);
      if (node == null) {
	/* create a tree node for each substring of wordsize
	   that is found in the string at least once */
	node = new HuffNode (word);
	nodes.put (word, node);
	leaves.put (word, node);
      }
      node.addOccurance();
    }
	
    /*
      build a huffman encoding tree from the bottom up according to
      the frequencies.
      Its a binary tree with the frequencies/occurances stored in each node
      Iteratively find the two nodes with the smallest number of occurances.
      Combine these two into a single new node with these two as children.
      The occurance count for the new node is the sum of the values of the
      two children. Repeat until all the nodes are combined into a single
      tree. Note that the words that occur infrequently end up at the
      bottom of the tree and words that occur often are near the root.
      The Huffman code is derived by simply viewing this tree as the
      encoder with a left node corresponding to a code bit of 0 and a 
      right node corresponding to a code bit of 1. Thus the words with
      the most frequent occurance are encoded with the fewest number of bits
    */
	
    /* 
       P.S. a better sort algorithm would be useful here
    */
    while (nodes.size() > 1) {
      HuffNode child_0 = null;
      HuffNode child_1 = null;
	    
      Enumeration e = nodes.elements();
	    
      while (e.hasMoreElements()) {
	node = (HuffNode) e.nextElement();
	if (child_1 == null) {
	  child_1 = node;
	}
	else if (node.getOccurance() < child_1.getOccurance()) {
	  if (child_0 == null ||
	      child_1.getOccurance() < child_0.getOccurance()) {
	    child_0 = child_1;
	  }
	  child_1 = node;
	}
	else if (child_0 == null) {
	  child_0 = node;
	}
	else if (node.getOccurance() < child_0.getOccurance()) {
	  child_0 = node;
	}
      }
	    
      nodes.remove (child_0.getWord());
      nodes.remove (child_1.getWord());
	    
      node = new HuffNode (child_0, child_1);
      nodes.put (node.getWord(), node);
    }
	
    /*
      Now that the tree is built use it to encode the sequence string
    */
    String encoded = "";
    int bit_total = 0;
	
    for (int i = 0; i < str.length(); i += wordsize) {
      String c;
      if ((i + wordsize) < str.length())
	c = str.substring(i, i + wordsize);
      else
	c = str.substring(i);
	    
      node = (HuffNode) leaves.get (c);
      HuffNode parent = node.getParent();
      int word_code = 0;
      int bit_count = 0;
      while (parent != null) {
	word_code <<= 1;
	bit_count++;
	if (node == parent.getChild_1()) {
	  word_code |= 1;
	}
	node = parent;
	parent = node.getParent();
      }
      encoded = encoded + String.valueOf (word_code);
      bit_total += bit_count;
    }
    return (bit_total);
  }

  public static boolean isPolyATail (String dna, 
				     int intron_length,
				     String name) {
    return isPolyATail(dna, intron_length, name, false);
  }

  // TODO - ignoring debug flag in favor of log4j config.
  public static boolean isPolyATail (String dna, 
				     int intron_length,
				     String name, 
                                     boolean debug) {
    boolean polyA = false;

    // get exact substring of this span
    if (dna != null && dna.length() > 0 && dna.length() < 40) {	
      // 1 is the wordsize

      int bit_total = getDNABits (dna);
      // if the intron is 7500 bp or less then
      // more than 10-fold compression is unacceptable
      // however if the intron is > 7500 then increase
      // the limit for compression even more
      // double identity= (90 + Math.min (intron_length / 7500, 10.0)) / 100.0;
      double identity = (90 + intron_length / 7500) / 100.0;
      double mixed = 2 * dna.length();
      long lower_limit = Math.round(mixed * identity);
      if (bit_total < lower_limit) {
	polyA = true;
      }
      logger.debug("intron_length=" + intron_length +
                   " identity=" + identity +
                   " dna_length=" + dna.length() +
                   " if random bits=" + mixed +
                   " bit_total=" + bit_total +
                   " lower_limit=" + lower_limit);
      if (!polyA)
        logger.debug("KEEPER!! span " + name + " dna=" + dna);
      else
        logger.debug("Deleting span " + name + " dna=" + dna);
    }
    return polyA;
  }

  private static int getDNABits(String dna) {
    char[] bases = dna.toCharArray();
    int [] totals = new int [4];
    int A_count = 0;
    int T_count = 1;
    int G_count = 2;
    int C_count = 3;
    totals[T_count] = 0;
    totals[A_count] = 0;
    totals[G_count] = 0;
    totals[C_count] = 0;
    int N_count = 0;
    for (int i = 0; i < bases.length; i++) {
      switch (bases[i]) {
      case 'A':
      case 'a':
        totals[A_count]++;
        break;
      case 'T':
      case 't':
        totals[T_count]++;
        break;
      case 'G':
      case 'g':
        totals[G_count]++;
        break;
      case 'C':
      case 'c':
        totals[C_count]++;
        break;
      default:
        N_count++;
        break;
      }
    }

    double std_dev = getStandardDev(totals);
    double quarter = Math.round(std_dev * 1.44);
    double expected = dna.length() / 4;
    double max_expected = expected + quarter;
    char base = 'A';
    logger.debug(" expected count of bases = " + expected +
                 " std_dev=" + std_dev +
                 " quarter=" + quarter +
                 " max_expected=" + max_expected);
    for (int i = 0; i < 4; i++) {
      if (totals[i] <= max_expected) {
        logger.debug("Using 2 bits for " + base + " " +
                     " base count=" + totals[i]);
        totals[i] *= 2;
      }
      else {
          logger.debug("Using 1 bits for " + base + " " +
                       " base count=" + totals[i]);
      }
      base = (i == 0 ? 'T' : (i == 1 ? 'G' : 'C'));
    }

    N_count *= 3;
    int bits = (totals[A_count] + totals[T_count] + 
                totals[C_count] + totals[G_count] + N_count);

    logger.debug("for dna: " + dna + "\n" +
                 "A="+totals[A_count] + ", T=" + totals[T_count] +
                 ", C=" + totals[C_count] + ", G=" + totals[G_count] +
                 ", N=" + N_count + ". Total bits = " + bits);
    return bits;
  }

  private static double getStandardDev(int [] nums) {
    double var = 0;
    double mean = getMean(nums);
    for (int i = 0; i < nums.length; i++) {
      var += (nums[i] - mean) * (nums[i] - mean);
    }
    var = var / nums.length;
    return Math.sqrt(var);
  }

  private static double getMean(int [] nums) {
    int sum = 0;
    for (int i = 0; i < nums.length; i++) {
      sum += nums[i];
    }
    return ((double) (sum / nums.length));
  }
}
