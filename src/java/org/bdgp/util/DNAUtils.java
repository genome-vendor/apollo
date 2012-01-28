/*
**  DNAUtils
**  (c) Copyright 1997, Neomorphic Sofware, Inc.
**  All Rights Reserved
**
**  CONFIDENTIAL
**  DO NOT DISTRIBUTE
**
**  File: DNAUtils.java
**
*/

package org.bdgp.util;

import java.util.List;
import java.util.ArrayList;

/**
 * a collection of constants and static methods
 * useful in manipulating ascii representations of DNA sequences.
 */
public class DNAUtils {

  public static final int NUCLEOTIDES = 0;
  public static final int COMPLEMENT = 1;
  public static final int FRAME_ONE = 2;
  public static final int FRAME_TWO = 3;
  public static final int FRAME_THREE = 4;
  public static final int FRAME_NEG_ONE = 5;
  public static final int FRAME_NEG_TWO = 6;
  public static final int FRAME_NEG_THREE = 7;

  public static final int FORWARD_SPLICED_TRANSLATION = 8;
  public static final int REVERSE_SPLICED_TRANSLATION = 9;

  public static final int[] FRAME_MAPPING = { 0, 0, 0, 1, 2, -0, -1, -2 };

  public static final int ONE_LETTER_CODE = 100;
  public static final int THREE_LETTER_CODE = 101;

  // need to add full IUPAC!!!
  static char[] dna_chars = { 'A', 'C', 'G', 'T', 'N', ' ', 
                              'a', 'c', 'g', 't', 'n' };

  private static String[] threeNucleotideStopCodons;
  private static List stopCodonCharArrayList;

  private static int A = 0;
  private static int C = 1;
  private static int G = 2;
  private static int T = 3;
  private static int N = 4;

  private static int currentGeneticCodeNumber = 1;

  //private static void synchGeneticCodeWithConfig() ???

  /** Set genetic code via int. 1 is the "nnormal" one, 6 is for cilliates.
      currently only 1 & 6 are supported. todo add the rest of them... */
  public static void setGeneticCode(int geneticCodeNumber) {
    if (geneticCodeNumber == currentGeneticCodeNumber)
      return; // already set as such
    currentGeneticCodeNumber = geneticCodeNumber;
    aa1 = aa1Default; // genetic code 1 is the default
    aa3 = aa3Default;
    if (geneticCodeNumber == 1) {
      return; // already set with default
    }
    else if (geneticCodeNumber == 6) {
      // just change a few things from genetic code 1
      aa1[T][A][A] = "Q"; // Gln
      aa3[T][A][A] = "Gln";
      aa1[T][A][G] = "Q";
      aa3[T][A][G] = "Gln";
    }

    threeNucleotideStopCodons = null;
    stopCodonCharArrayList = null;
  }


  /** default is for genetic code 1 (normal) */
  private static String aa1Default[][][] = new String[5][5][5];
  private static String aa3Default[][][] = new String[5][5][5];
  
  static {

    // starting with AAA, AAC, ... to TTG, TTT
    aa3Default[A][A][A] = "Lys";  // AAA, K, Lys, Lysine
    aa3Default[A][A][C] = "Asn";  // AAC, N, Asn, Asparagine
    aa3Default[A][A][G] = "Lys";  // AAG, K, Lys, Lysine
    aa3Default[A][A][T] = "Asn";  // AAT, N, Asn, Asparagine
    aa3Default[A][C][A] = "Thr";  // ACA, T, Thr, Threonine
    aa3Default[A][C][C] = "Thr";  // ACC, T, Thr, Threonine
    aa3Default[A][C][G] = "Thr";  // ACG, T, Thr, Threonine
    aa3Default[A][C][T] = "Thr";  // ACT, T, Thr, Threonine
    aa3Default[A][G][A] = "Arg";  // AGA, R, Arg, Arginine
    aa3Default[A][G][C] = "Ser";  // AGC, S, Ser, Serine
    aa3Default[A][G][G] = "Arg";  // AGG, R, Arg, Arginine
    aa3Default[A][G][T] = "Ser";  // AGT, S, Ser, Serine
    aa3Default[A][T][A] = "Ile";  // ATA, I, Ile, Isoleucine
    aa3Default[A][T][C] = "Ile";  // ATC, I, Ile, Isoleucine
    aa3Default[A][T][G] = "Met";  // ATG, M, Met, Methionine
    aa3Default[A][T][T] = "Ile";  // ATT, I, Ile, Isoleucine

    aa3Default[C][A][A] = "Gln";  // CAA, Q, Gln, Glutamine
    aa3Default[C][A][C] = "His";  // CAC, H, His, Histidine
    aa3Default[C][A][G] = "Gln";  // CAG, Q, Gln, Glutamine
    aa3Default[C][A][T] = "His";  // CAT, H, His, Histidine
    aa3Default[C][C][A] = "Pro";  // CCA, P, Pro, Proline
    aa3Default[C][C][C] = "Pro";  // CCC, P, Pro, Proline
    aa3Default[C][C][G] = "Pro";  // CCG, P, Pro, Proline
    aa3Default[C][C][T] = "Pro";  // CCT, P, Pro, Proline
    aa3Default[C][G][A] = "Arg";  // CGA, R, Arg, Arginine
    aa3Default[C][G][C] = "Arg";  // CGC, R, Arg, Arginine
    aa3Default[C][G][G] = "Arg";  // CGG, R, Arg, Arginine
    aa3Default[C][G][T] = "Arg";  // CGT, R, Arg, Arginine
    aa3Default[C][T][A] = "Leu";  // CTA, L, Leu, Leucine
    aa3Default[C][T][C] = "Leu";  // CTC, L, Leu, Leucine
    aa3Default[C][T][G] = "Leu";  // CTG, L, Leu, Leucine
    aa3Default[C][T][T] = "Leu";  // CTT, L, Leu, Leucine

    aa3Default[G][A][A] = "Glu";  // GAA, E, Glu, Glutamate
    aa3Default[G][A][C] = "Asp";  // GAC, D, Asp, Aspartate
    aa3Default[G][A][G] = "Glu";  // GAG, E, Glu, Glutamate
    aa3Default[G][A][T] = "Asp";  // GAT, D, Asp, Aspartate
    aa3Default[G][C][A] = "Ala";  // GCA, A, Ala, Alanine
    aa3Default[G][C][C] = "Ala";  // GCC, A, Ala, Alanine
    aa3Default[G][C][G] = "Ala";  // GCG, A, Ala, Alanine
    aa3Default[G][C][T] = "Ala";  // GCT, A, Ala, Alanine
    aa3Default[G][G][A] = "Gly";  // GGA, G, Gly, Glycine
    aa3Default[G][G][C] = "Gly";  // GGC, G, Gly, Glycine
    aa3Default[G][G][G] = "Gly";  // GGG, G, Gly, Glycine
    aa3Default[G][G][T] = "Gly";  // GGT, G, Gly, Glycine
    aa3Default[G][T][A] = "Val";  // GTA, V, Val, Valine
    aa3Default[G][T][C] = "Val";  // GTC, V, Val, Valine
    aa3Default[G][T][G] = "Val";  // GTG, V, Val, Valine
    aa3Default[G][T][T] = "Val";  // GTT, V, Val, Valine

    aa3Default[T][A][A] = "***";  // TAA, *, ***, Stop
    aa3Default[T][A][C] = "Tyr";  // TAC, Y, Tyr, Tyrosine
    aa3Default[T][A][G] = "***";  // TAG, *, ***, Stop
    aa3Default[T][A][T] = "Tyr";  // TAT, Y, Tyr, Tyrosine
    aa3Default[T][C][A] = "Ser";  // TCA, S, Ser, Serine
    aa3Default[T][C][C] = "Ser";  // TCC, S, Ser, Serine
    aa3Default[T][C][G] = "Ser";  // TCG, S, Ser, Serine
    aa3Default[T][C][T] = "Ser";  // TCT, S, Ser, Serine
    aa3Default[T][G][A] = "***";  // TGA, *, ***, Stop
    aa3Default[T][G][C] = "Cys";  // TGC, C, Cys, Cysteine
    aa3Default[T][G][G] = "Trp";  // TGG, W, Trp, Tryptophan
    aa3Default[T][G][T] = "Cys";  // TGT, C, Cys, Cysteine
    aa3Default[T][T][A] = "Leu";  // TTA, L, Leu, Leucine
    aa3Default[T][T][C] = "Phe";  // TTC, F, Phe, Phenylalanine
    aa3Default[T][T][G] = "Leu";  // TTG, L, Leu, Leucine
    aa3Default[T][T][T] = "Phe";  // TTT, F, Phe, Phenylalanine

    //    for (int i=0; i<4; i++) {
    //      for (int j=0; j<4; j++) {
    // BUG FIX 10-27-98 GAH -- need to fill out last row (i/j = 4) !!!
    for (int i=0; i<=4; i++) {
      for (int j=0; j<=4; j++) {
	aa3Default[N][i][j] = "???"; // N**, ?, ???, Unkown
	aa3Default[i][N][j] = "???"; // N**, ?, ???, Unkown
	aa3Default[i][j][N] = "???"; // N**, ?, ???, Unkown
      }
    }

    // setting up one letter code
    // starting with AAA, AAC, ... to TTG, TTT
    aa1Default[A][A][A] = "K";  // AAA, K, Lys, Lysine
    aa1Default[A][A][C] = "N";  // AAC, N, Asn, Asparagine
    aa1Default[A][A][G] = "K";  // AAG, K, Lys, Lysine
    aa1Default[A][A][T] = "N";  // AAT, N, Asn, Asparagine
    aa1Default[A][C][A] = "T";  // ACA, T, Thr, Threonine
    aa1Default[A][C][C] = "T";  // ACC, T, Thr, Threonine
    aa1Default[A][C][G] = "T";  // ACG, T, Thr, Threonine
    aa1Default[A][C][T] = "T";  // ACT, T, Thr, Threonine
    aa1Default[A][G][A] = "R";  // AGA, R, Arg, Arginine
    aa1Default[A][G][C] = "S";  // AGC, S, Ser, Serine
    aa1Default[A][G][G] = "R";  // AGG, R, Arg, Arginine
    aa1Default[A][G][T] = "S";  // AGT, S, Ser, Serine
    aa1Default[A][T][A] = "I";  // ATA, I, Ile, Isoleucine
    aa1Default[A][T][C] = "I";  // ATC, I, Ile, Isoleucine
    aa1Default[A][T][G] = "M";  // ATG, M, Met, Methionine
    aa1Default[A][T][T] = "I";  // ATT, I, Ile, Isoleucine

    aa1Default[C][A][A] = "Q";  // CAA, Q, Gln, Glutamine
    aa1Default[C][A][C] = "H";  // CAC, H, His, Histidine
    aa1Default[C][A][G] = "Q";  // CAG, Q, Gln, Glutamine
    aa1Default[C][A][T] = "H";  // CAT, H, His, Histidine
    aa1Default[C][C][A] = "P";  // CCA, P, Pro, Proline
    aa1Default[C][C][C] = "P";  // CCC, P, Pro, Proline
    aa1Default[C][C][G] = "P";  // CCG, P, Pro, Proline
    aa1Default[C][C][T] = "P";  // CCT, P, Pro, Proline
    aa1Default[C][G][A] = "R";  // CGA, R, Arg, Arginine
    aa1Default[C][G][C] = "R";  // CGC, R, Arg, Arginine
    aa1Default[C][G][G] = "R";  // CGG, R, Arg, Arginine
    aa1Default[C][G][T] = "R";  // CGT, R, Arg, Arginine
    aa1Default[C][T][A] = "L";  // CTA, L, Leu, Leucine
    aa1Default[C][T][C] = "L";  // CTC, L, Leu, Leucine
    aa1Default[C][T][G] = "L";  // CTG, L, Leu, Leucine
    aa1Default[C][T][T] = "L";  // CTT, L, Leu, Leucine

    aa1Default[G][A][A] = "E";  // GAA, E, Glu, Glutamate
    aa1Default[G][A][C] = "D";  // GAC, D, Asp, Aspartate
    aa1Default[G][A][G] = "E";  // GAG, E, Glu, Glutamate
    aa1Default[G][A][T] = "D";  // GAT, D, Asp, Aspartate
    aa1Default[G][C][A] = "A";  // GCA, A, Ala, Alanine
    aa1Default[G][C][C] = "A";  // GCC, A, Ala, Alanine
    aa1Default[G][C][G] = "A";  // GCG, A, Ala, Alanine
    aa1Default[G][C][T] = "A";  // GCT, A, Ala, Alanine
    aa1Default[G][G][A] = "G";  // GGA, G, Gly, Glycine
    aa1Default[G][G][C] = "G";  // GGC, G, Gly, Glycine
    aa1Default[G][G][G] = "G";  // GGG, G, Gly, Glycine
    aa1Default[G][G][T] = "G";  // GGT, G, Gly, Glycine
    aa1Default[G][T][A] = "V";  // GTA, V, Val, Valine
    aa1Default[G][T][C] = "V";  // GTC, V, Val, Valine
    aa1Default[G][T][G] = "V";  // GTG, V, Val, Valine
    aa1Default[G][T][T] = "V";  // GTT, V, Val, Valine

    aa1Default[T][A][A] = "*";  // TAA, *, ***, Stop
    aa1Default[T][A][C] = "Y";  // TAC, Y, Tyr, Tyrosine
    aa1Default[T][A][G] = "*";  // TAG, *, ***, Stop
    aa1Default[T][A][T] = "Y";  // TAT, Y, Tyr, Tyrosine
    aa1Default[T][C][A] = "S";  // TCA, S, Ser, Serine
    aa1Default[T][C][C] = "S";  // TCC, S, Ser, Serine
    aa1Default[T][C][G] = "S";  // TCG, S, Ser, Serine
    aa1Default[T][C][T] = "S";  // TCT, S, Ser, Serine
    aa1Default[T][G][A] = "*";  // TGA, *, ***, Stop
    aa1Default[T][G][C] = "C";  // TGC, C, Cys, Cysteine
    aa1Default[T][G][G] = "W";  // TGG, W, Trp, Tryptophan
    aa1Default[T][G][T] = "C";  // TGT, C, Cys, Cysteine
    aa1Default[T][T][A] = "L";  // TTA, L, Leu, Leucine
    aa1Default[T][T][C] = "F";  // TTC, F, Phe, Phenylalanine
    aa1Default[T][T][G] = "L";  // TTG, L, Leu, Leucine
    aa1Default[T][T][T] = "F";  // TTT, F, Phe, Phenylalanine

    //    for (int i=0; i<4; i++) {
    //      for (int j=0; j<4; j++) {
    // BUG FIX 10-27-98 GAH -- need to fill out last row (i/j = 4) !!!
    for (int i=0; i<=4; i++) {
      for (int j=0; j<=4; j++) {
	aa1Default[N][i][j] = "X"; // N**, ?, ???, Unknown
	aa1Default[i][N][j] = "X"; // N**, ?, ???, Unknown
	aa1Default[i][j][N] = "X"; // N**, ?, ???, Unknown
      }
    }

  }

  /** Genetic Code in 1-character amino acid codes. by default set to default
   genetic code 1 */
  protected static String aa1[][][] = aa1Default;
  /** Genetic Code in 3-character amino acid codes, default set to gen code 1 */
  protected static String aa3[][][] = aa3Default;

  /** Return array of all stop codons for the configured genetic code - stop codons
      as 3 letter nucleotide  letters eg "TGA" */
  public static String[] get3NucleotideStopCodons() {
    //synchGeneticCodeWithConfig();

    if (threeNucleotideStopCodons == null) {
      createStopCodons();
    }
    return threeNucleotideStopCodons;
  }

  private static String get3LetterCodeFromInts(int i, int j, int k) {
    return "" + getResidueChar(i) + getResidueChar(j) + getResidueChar(k);
  }

  private static char[] getCharArrayCodonFromInts(int i, int j, int k) {
    return new char[] {getResidueChar(i),getResidueChar(j),getResidueChar(k)};
  }

  public static List getStopCodonsAsCharArrayList() {
    if (stopCodonCharArrayList == null)
      createStopCodons();
    return stopCodonCharArrayList;
  }

  private static void createStopCodons() {
    List stopList = new ArrayList(5);
    stopCodonCharArrayList = new ArrayList(5);
    for (int i=0; i<4; i++) {
      for (int j=0; j<4; j++) {
        for (int k=0; k<4; k++) {
          if (aa1[i][j][k].equals("*")) {
            String codon = get3LetterCodeFromInts(i,j,k);
            stopList.add(codon);
            char[] charCodon = getCharArrayCodonFromInts(i,j,k);
            stopCodonCharArrayList.add(charCodon);
          }
        }
      }
    }
    String[] threeNucleotideStopCodons = new String[stopList.size()];
    for (int i=0; i<stopList.size(); i++)
      threeNucleotideStopCodons[i] = (String)stopList.get(i);

  }

  //private static String nucleotideStringForInt(int i) {}

  /**
   * determines the complement of a sequence of nucleotides.
   *
   * @param s a string of nucleotide codes.
   * @return the complementary codes.
   */
  public static String complement(String s) {
    if (s == null)  { return null; }
    StringBuffer buf = new StringBuffer(s);
    DNAUtils.complementBuffer(buf);
    return buf.toString();
  }
  
  /**
   * determines the reverse complement of a sequence of nucleotides.
   *
   * @param s a string of nucleotide codes.
   * @return the complementary codes in reverse order.
   */
  public static String reverseComplement(String s) {
    if (s == null) { return null; }
    StringBuffer buf = new StringBuffer(s.length());
    int j=0;
    for (int i=s.length()-1; i>=0; i--) {
      buf.append(s.charAt(i));
      j++;
    }
    complementBuffer(buf);
    return buf.toString();
  }

  /**
   * determines the reverse of a sequence of nucleotides.
   *
   * @param s a string of nucleotide codes.
   * @return the codes in reverse order.
   */
  public static String reverse(String s) {
    if (s == null) { return null; }
    StringBuffer buf = new StringBuffer(s.length());
    //    int j=0;
    for (int i=s.length()-1; i>=0; i--) {
      buf.append(s.charAt(i));
      //      j++;
    }
    return buf.toString();
  }

  /**
   * determines the reverse of a part of a sequence of nucleotides.
   *
   * @param s a string of nucleotide codes.
   * @param offset the number of characters to skip
   *               at the beginning of s.
   * @param chunk_size the number of characters in the portion
   *                   to be reversed
   * @return the codes of the specified chunk, in reverse order.
   */
  public static String chunkReverse(String s, int offset, int chunk_size) {
    if (s == null) { return null; }
    int reverse_offset = (s.length()-offset) % chunk_size;
    //    System.out.println(reverse_offset);
    StringBuffer buf = new StringBuffer(s.length());
    for (int i = 0; i < reverse_offset; i++) {
      buf.append(' ');
    }
    int max = s.length() - reverse_offset - chunk_size;
    //    System.out.println("Max: " + max);
    String chunk;
    for (int i = max; i >= 0; i -= chunk_size) {
      //      System.out.println(i + ", " + chunk_size);
      //      chunk = s.substring(i-chunk_size+1, i+1);
      chunk = s.substring(i, i+chunk_size);
      buf.append(chunk);
    }
    int end_spaces = s.length() - buf.length();
    for (int i = 0; i < end_spaces; i++) {
      buf.append(' ');
    }
    return buf.toString();
  }

  /**
   * determines the complement of a sequence of nucleotides.
   *
   * @param buf a string of nucleotide codes
   *            each of which is replaced
   *            with it's complementary code.
   * @see #complement
   */
  protected static void complementBuffer(StringBuffer buf) {
    char base;
    for (int i=0; i<buf.length(); i++) {
      base = buf.charAt(i);
      buf.setCharAt(i, complement(base));
    }
  }
  
  /**
   * determines the complement of a nucleotide
   * 
   * @param base a character reperesenting a nucleotide
   * @return the character which represents the complement to the input base
   */
  public static char complement(char base) {
    char complement = base;
    if      (base == 'a') { complement = 't'; }
    else if (base == 'c') { complement = 'g'; }
    else if (base == 'g') { complement = 'c'; }
    else if (base == 't') { complement = 'a'; }
    else if (base == 'A') { complement = 'T'; }
    else if (base == 'C') { complement = 'G'; }
    else if (base == 'G') { complement = 'C'; }
    else if (base == 'T') { complement = 'A'; }
    
    if ((complement == base) && (base != 'n') && (base != 'N'))
      throw new IllegalArgumentException(
          "Could not find complement for '" + base + "'");

    return complement;
  }

  /**
   * gets a representation of the genetic code.
   * The three dimensions of the array returned correspond
   * to the three nucleotides in a codon.
   * Each dimension ranges from 0 to 4
   * representing bases A, C, G, T, and N respectively.
   * Prefer the constants A, C, G, T, and N to the integers
   * when subscripting the array.
   *
   * @return the genetic code
   *         expressed in three-character amino acid codes.
   */
  public static String[][][] getGeneticCodeThree() {
    return aa3;
  }

  /**
   * gets a representation of the genetic code.
   * The three dimensions of the array returned correspond
   * to the three nucleotides in a codon.
   * Each dimension ranges from 0 to 4
   * representing bases A, C, G, T, and N respectively.
   * Prefer the constants A, C, G, T, and N to the integers
   * when subscripting the array.
   *
   * @return the genetic code
   *         expressed in one-character amino acid codes.
   */
  public static String[][][] getGeneticCodeOne() {
    return aa1;
  }

  /** Says whether a single letter represents a valid one-letter amino acid code
      @param aa String (should be single char)
      @return true or false */
  public static boolean isValidOneLetterAA(String aa) {
    if (aa.length() != 1)
      return false;

    String one_letter_codes = "ACDEFGHIKLMNPQRSTVWY";
    if (one_letter_codes.indexOf(aa.toUpperCase()) >= 0)
      return true;
    else
      return false;
  }


  /**
   * gets a translation into amino acids of a string of nucleotides.
   *
   * @param s represents the string of nucleotides.
   * @param frametype FRAME_ONE, FRAME_TWO, or FRAME_THREE.
   *                  For reverse strand frames, 
   *                  translate the reverse complement.
   *                  Then reverse that result.
   * @param codetype ONE_LETTER_CODE, or THREE_LETTER_CODE
   *                 indicating how many letters should encode each amino acid.
   * @return a representation of the amino acid sequence
   *         encoded by the given nucleotide sequence.
   */
  public static String translate(String s, int frametype, int codetype) {
    return translate(s, frametype, codetype, null, null, null);
  }
    
  /**
   * gets a translation into amino acids of a string of nucleotides.
   *
   * @param s represents the string of nucleotides.
   * @param frametype FRAME_ONE, FRAME_TWO, or FRAME_THREE.
   *                  For reverse strand frames, 
   *                  translate the reverse complement.
   *                  Then reverse that result.
   * @param codetype ONE_LETTER_CODE, or THREE_LETTER_CODE
   *                 indicating how many letters should encode each amino acid.
   * @param initial_string what goes at front of entire translation
   * @param pre_string what goes before every amino acid
   * @param post_string what goes after every amino acid
   * @return a representation of the amino acid sequence
   *         encoded by the given nucleotide sequence.
   */
  public static String translate(String s, int frametype, int codetype, 
				 String initial_string, 
				 String pre_string, String post_string) {
    String result = null;
    if (codetype == ONE_LETTER_CODE || codetype == 1) {
      result = 
       translate(s, frametype, getGeneticCodeOne(), 
		 initial_string, pre_string, post_string);
    }
    else if (codetype == THREE_LETTER_CODE || codetype == 3) {
      result = 
       translate(s, frametype, getGeneticCodeThree(), 
		 initial_string, pre_string, post_string);
    }
    return result;
  }

  /**
   * gets a translation into amino acids of a string of nucleotides.
   *
   * @param s represents the string of nucleotides.
   * @param frametype FRAME_ONE, FRAME_TWO, or FRAME_THREE.
   *                  For reverse strand frames, 
   *                  translate the reverse complement.
   *                  Then reverse that result.
   * @param genetic_code the result of one of the getGeneticCode methods
   *                     of this class.
   * @param initial_string what goes at front of entire translation
   * @param pre_string what goes before every amino acid
   * @param post_string what goes after every amino acid
   * @return a representation of the amino acid sequence
   *         encoded by the given nucleotide sequence.
   * @see #getGeneticCodeOne
   * @see #getGeneticCodeThree
   */
  // currently only translates in +1, +2, +3 
  // for -1, -2, -3: translate reverse complement, then reverse result
  // initial_string is what goes at front of entire translation
  // pre_string is what goes before every amino acid
  // post_string is what goes after every amino acid
  public static String translate(String s, int frametype, 
				 String[][][] genetic_code, 
				 String initial_string,
				 String pre_string, String post_string) {
    int frame = FRAME_MAPPING[frametype];

    int length = s.length();
    byte[] basenums = new byte[length];
    for (int i=0; i<length; i++) {
      switch (s.charAt(i)) {
      case 'A':
      case 'a':
	basenums[i] = 0;
	break;
      case 'C':
      case 'c':
	basenums[i] = 1;
	break;
      case 'G':
      case 'g':
	basenums[i] = 2;
	break;
      case 'T':
      case 't':
	basenums[i] = 3;
	break;
      default:
	basenums[i] = 4;
	break;
      }
    }
    
    String residue;
    //    int residue_charsize = 3;
    int residue_charsize = genetic_code[0][0][0].length();
    if (pre_string != null) { residue_charsize += pre_string.length(); }
    if (post_string != null) { residue_charsize += post_string.length(); }

    StringBuffer amino_acids = new StringBuffer(length);
    // StringBuffer amino_acids = 
    //new StringBuffer(((int)(length-(int)Math.abs(frame))/3)*residue_charsize;

    if (initial_string != null)
      amino_acids.append(initial_string);

    // checking for no spaces, can build non-spaced faster by avoiding 
    //     amino_acids.append("") calls
    int extra_bases = (length-(int)Math.abs(frame)) % 3;
    int k = 0;
    if (pre_string == null && post_string == null) {
      for (int i = frame; i < length-2; i += 3, k = i) {
	residue = genetic_code[basenums[i]][basenums[i+1]][basenums[i+2]];
	amino_acids.append(residue);

      }
      for (int i = 0; i < extra_bases; i++) {
	amino_acids.append(" ");
      }
    }
    else {
      if (pre_string == null) { pre_string = ""; }
      if (post_string == null) { post_string = ""; }
      for (int i = frame; i< length-2; i+=3) {
	residue = genetic_code[basenums[i]][basenums[i+1]][basenums[i+2]];
	amino_acids.append(pre_string);
	amino_acids.append(residue);
	amino_acids.append(post_string);
      }
      for (int i = 0; i < extra_bases; i++) {
	amino_acids.append(" ");
      }
    }
    return amino_acids.toString();
  }

  /** number of "letters" that are valid in a string of nucleotide codes. */
  public static final int LETTERS = 6;

  /**
   * ordinal numbers of nucleotides
   * associated with each possible ascii character code.
   * Unused characters are associated with the integer -1.
   */
  protected static int[] letter_to_id = new int[256];
  /** ascii character codes for each nucleotide (or set of nucleotides). */
  protected static char[] id_to_letter = new char[LETTERS];

  static {
    for (int i=0; i<letter_to_id.length; i++) {
      letter_to_id[i] = -1;
    }

    letter_to_id['A'] = 0;
    letter_to_id['C'] = 1;
    letter_to_id['G'] = 2;
    letter_to_id['T'] = 3;
    letter_to_id['N'] = 4;
    letter_to_id['a'] = 0;
    letter_to_id['c'] = 1;
    letter_to_id['g'] = 2;
    letter_to_id['t'] = 3;
    letter_to_id['n'] = 4;
    letter_to_id['*'] = 5;
    letter_to_id[' '] = 5;
    letter_to_id['-'] = 5;

    id_to_letter[0] = 'A';
    id_to_letter[1] = 'C';
    id_to_letter[2] = 'G';
    id_to_letter[3] = 'T';
    id_to_letter[4] = 'N';
    id_to_letter[5] = '*';
    //    id_to_letter[6] = ' ';
  }

  /**
   * gets an index into an array of codes for nucleotides.
   * 
   * @param residue_letter letter representation of a nucleotide.
   * @return ordinal of nucleotide letter code.
   * @see #getResidueChar
   */
  public static int getResidueID(char residue_letter) {
    return letter_to_id[residue_letter];
  }

  /**
   * gets a nucleotide code.
   * 
   * @param residue_id ordinal of nucleotide letter code.
   * @return letter representation of a nucleotide.
   * @see #getResidueChar
   */
  public static char getResidueChar(int residue_id) {
    return id_to_letter[residue_id];
  }

  /**
   * gets a map from letters to numbers
   * each representing nucleotides.
   */
  public static int[] getNACharToIdMap() {
    return letter_to_id;
  }

  /**
   * gets a map from numbers to letters
   * each representing nucleotides.
   */
  public static char[] getNAIdToCharMap() {
    return id_to_letter;
  }

  /**
   *  return an array of all allowed characters used to represent nucleotides
   *  This _should_ follow IUPAC spec, but doesn't yet
   */
  public static char[] getAllowedDNACharacters() {
    return dna_chars;
  }

  public static double GCcontent (String dna)
  {
    return GCcontent (new StringBuffer (dna));
  }

  public static double GCcontent (StringBuffer dna)
  {
    int gc_count = 0;
    for (int i = 0; i < dna.length(); i++)
      {
	if (dna.charAt(i) == 'G' || 
	    dna.charAt(i) == 'g' ||
	    dna.charAt(i) == 'C' ||
	    dna.charAt(i) == 'c')
	  gc_count++;
      }
    return ((gc_count * 100) / dna.length());
  }

}
