package apollo.seq;

import java.util.*;

/** DELETE THIS - was only used by apollo.gui.SeqMatchPanel, but that now uses
    org.bdgp.util.DNAUtils which is redundant with this class, and now takes 
    into account genetic code (which this doesnt), so this class has been
    basically replaced by DNAUtils */

public class ResidueProperties {

  public static Hashtable codonHash = new Hashtable();

  public static Hashtable compressedCodonHash = new Hashtable();

  protected static Hashtable pamHash = new Hashtable();

  public static final int UNRECOGNIZED_VALUES = -100;
  public static final int NO_AFFINITY = -1;
  public static final int SOME_AFFINITY = 0;
  public static final int HIGH_AFFINITY = 1;
  public static final int PERFECT_AFFINITY = 2;

  public static Vector Lys = new Vector();
  public static Vector Asn = new Vector();
  public static Vector Gln = new Vector();
  public static Vector His = new Vector();
  public static Vector Glu = new Vector();
  public static Vector Asp = new Vector();
  public static Vector Tyr = new Vector();
  public static Vector Thr = new Vector();
  public static Vector Pro = new Vector();
  public static Vector Ala = new Vector();
  public static Vector Ser = new Vector();
  public static Vector Arg = new Vector();
  public static Vector Gly = new Vector();
  public static Vector Trp = new Vector();
  public static Vector Cys = new Vector();
  public static Vector Ile = new Vector();
  public static Vector Met = new Vector();
  public static Vector Leu = new Vector();
  public static Vector Val = new Vector();
  public static Vector Phe = new Vector();
  public static Vector STOP = new Vector();

  static {
    codonHash.put("K",Lys);
    codonHash.put("N",Asn);
    codonHash.put("Q",Gln);
    codonHash.put("H",His);
    codonHash.put("E",Glu);
    codonHash.put("D",Asp);
    codonHash.put("Y",Tyr);
    codonHash.put("T",Thr);
    codonHash.put("P",Pro);
    codonHash.put("A",Ala);
    codonHash.put("S",Ser);
    codonHash.put("R",Arg);
    codonHash.put("G",Gly);
    codonHash.put("W",Trp);
    codonHash.put("C",Cys);
    codonHash.put("I",Ile);
    codonHash.put("M",Met);
    codonHash.put("L",Leu);
    codonHash.put("V",Val);
    codonHash.put("F",Phe);
    codonHash.put("*",STOP);

    compressedCodonHash.put("K","AAR");
    compressedCodonHash.put("N","AAY");
    compressedCodonHash.put("Q","CAR");
    compressedCodonHash.put("H","CAY");
    compressedCodonHash.put("E","GAR");
    compressedCodonHash.put("D","GAY");
    compressedCodonHash.put("Y","TAY");
    compressedCodonHash.put("T","ACN");
    compressedCodonHash.put("P","CCN");
    compressedCodonHash.put("A","GCN");
    compressedCodonHash.put("S","WSN");
    compressedCodonHash.put("R","MGN");
    compressedCodonHash.put("G","GGN");
    compressedCodonHash.put("W","TGG");
    compressedCodonHash.put("C","TGY");
    compressedCodonHash.put("I","ATH");
    compressedCodonHash.put("M","ATG");
    compressedCodonHash.put("L","YTN");
    compressedCodonHash.put("V","GTN");
    compressedCodonHash.put("F","TTY");
    compressedCodonHash.put("*","TRR");
    compressedCodonHash.put("-","---");
  }

  public static String translate(String dna,int phase) {
    int    i    = phase;
    String prot = "";

    while (i < dna.length()-2) {
      String codon   = dna.substring(i,i+3);
      String residue = codonTranslate(codon);

      prot += residue;
      i += 3;
    }
    return prot;
  }

  public static boolean isStop(String codon) {
    return STOP.contains(codon.toUpperCase());
  }

  public static String reverseComplement(String dna) {
    dna = dna.toUpperCase();

    dna = dna.replace('A','J');
    dna = dna.replace('T','A');
    dna = dna.replace('J','T');
    dna = dna.replace('C','J');
    dna = dna.replace('G','C');
    dna = dna.replace('J','G');

    StringBuffer newdna = new StringBuffer();
    int i = dna.length()-1;
    while (i>=0) {
      newdna.append(dna.substring(i,i+1));
      i--;
    }
    return newdna.toString();
  }

  public static Vector getCodons(String res) {
    if (codonHash.containsKey(res))
      return (Vector)codonHash.get(res);
    return null;
  }

  public static String getCodon(String res) {
    return (String) compressedCodonHash.get(res);
  }

  public static String codonTranslate(String codon) {
    Enumeration e = codonHash.keys();
    while (e.hasMoreElements()) {
      String key = (String)e.nextElement();
      Vector tmp = (Vector)codonHash.get(key);
      if (tmp.contains(codon)) {
        return key;
      }
    }
    System.out.println("Unknown codon " + codon);
    return "X";
  }

  public static Integer getDistance(String acid1, String acid2) {
    Hashtable hash = (Hashtable) pamHash.get(acid1);
    if (hash == null)
      return null;
    return (Integer) hash.get(acid2);
  }

  public static int getAARelationshipType(String acid1, String acid2) {
    if (acid1.equalsIgnoreCase(acid2))
      return PERFECT_AFFINITY;
    Hashtable hash = (Hashtable) pamHash.get(acid1);
    if (hash == null)
      return UNRECOGNIZED_VALUES;
    Integer temp = (Integer) hash.get(acid2);
    Integer temp2 = (Integer) hash.get("MAX");
    if (temp == null || temp2 == null)
      return UNRECOGNIZED_VALUES;

    int bindVal = temp.intValue();
    int maxVal = temp2.intValue();
    if (bindVal == maxVal)
      return HIGH_AFFINITY;
    else if (bindVal >= 0)
      return SOME_AFFINITY;
    else
      return NO_AFFINITY;
  }

  protected static void buildPAM() {
    Hashtable c_hash = new Hashtable();
    Hashtable s_hash = new Hashtable();
    Hashtable t_hash = new Hashtable();
    Hashtable p_hash = new Hashtable();
    Hashtable a_hash = new Hashtable();
    Hashtable g_hash = new Hashtable();
    Hashtable n_hash = new Hashtable();
    Hashtable d_hash = new Hashtable();
    Hashtable e_hash = new Hashtable();
    Hashtable q_hash = new Hashtable();
    Hashtable h_hash = new Hashtable();
    Hashtable r_hash = new Hashtable();
    Hashtable k_hash = new Hashtable();
    Hashtable m_hash = new Hashtable();
    Hashtable i_hash = new Hashtable();
    Hashtable l_hash = new Hashtable();
    Hashtable v_hash = new Hashtable();
    Hashtable f_hash = new Hashtable();
    Hashtable y_hash = new Hashtable();
    Hashtable w_hash = new Hashtable();

    pamHash.put("C", c_hash);
    pamHash.put("S", s_hash);
    pamHash.put("T", t_hash);
    pamHash.put("P", p_hash);
    pamHash.put("A", a_hash);
    pamHash.put("G", g_hash);
    pamHash.put("N", n_hash);
    pamHash.put("D", d_hash);
    pamHash.put("E", e_hash);
    pamHash.put("Q", q_hash);
    pamHash.put("H", h_hash);
    pamHash.put("R", r_hash);
    pamHash.put("K", k_hash);
    pamHash.put("M", m_hash);
    pamHash.put("I", i_hash);
    pamHash.put("L", l_hash);
    pamHash.put("V", v_hash);
    pamHash.put("F", f_hash);
    pamHash.put("Y", y_hash);
    pamHash.put("W", w_hash);

    c_hash.put("C", new Integer(12));
    c_hash.put("S", new Integer(0));
    c_hash.put("T", new Integer(-2));
    c_hash.put("P", new Integer(-3));
    c_hash.put("A", new Integer(-2));
    c_hash.put("G", new Integer(-3));
    c_hash.put("N", new Integer(-4));
    c_hash.put("D", new Integer(-5));
    c_hash.put("E", new Integer(-5));
    c_hash.put("Q", new Integer(-5));
    c_hash.put("H", new Integer(-3));
    c_hash.put("R", new Integer(-4));
    c_hash.put("K", new Integer(-5));
    c_hash.put("M", new Integer(-5));
    c_hash.put("I", new Integer(-2));
    c_hash.put("L", new Integer(-6));
    c_hash.put("V", new Integer(-2));
    c_hash.put("F", new Integer(-4));
    c_hash.put("Y", new Integer(0));
    c_hash.put("W", new Integer(-8));

    s_hash.put("C", new Integer(0));
    s_hash.put("S", new Integer(2));
    s_hash.put("T", new Integer(1));
    s_hash.put("P", new Integer(1));
    s_hash.put("A", new Integer(1));
    s_hash.put("G", new Integer(1));
    s_hash.put("N", new Integer(1));
    s_hash.put("D", new Integer(0));
    s_hash.put("E", new Integer(0));
    s_hash.put("Q", new Integer(-1));
    s_hash.put("H", new Integer(-1));
    s_hash.put("R", new Integer(0));
    s_hash.put("K", new Integer(0));
    s_hash.put("M", new Integer(-2));
    s_hash.put("I", new Integer(-1));
    s_hash.put("L", new Integer(-3));
    s_hash.put("V", new Integer(-1));
    s_hash.put("F", new Integer(-3));
    s_hash.put("Y", new Integer(-3));
    s_hash.put("W", new Integer(-2));

    t_hash.put("C", new Integer(-2));
    t_hash.put("S", new Integer(1));
    t_hash.put("T", new Integer(3));
    t_hash.put("P", new Integer(0));
    t_hash.put("A", new Integer(1));
    t_hash.put("G", new Integer(0));
    t_hash.put("N", new Integer(0));
    t_hash.put("D", new Integer(0));
    t_hash.put("E", new Integer(0));
    t_hash.put("Q", new Integer(-1));
    t_hash.put("H", new Integer(-1));
    t_hash.put("R", new Integer(-1));
    t_hash.put("K", new Integer(0));
    t_hash.put("M", new Integer(-1));
    t_hash.put("I", new Integer(0));
    t_hash.put("L", new Integer(-2));
    t_hash.put("V", new Integer(0));
    t_hash.put("F", new Integer(-3));
    t_hash.put("Y", new Integer(-3));
    t_hash.put("W", new Integer(-5));

    p_hash.put("C", new Integer(-3));
    p_hash.put("S", new Integer(1));
    p_hash.put("T", new Integer(0));
    p_hash.put("P", new Integer(6));
    p_hash.put("A", new Integer(1));
    p_hash.put("G", new Integer(-1));
    p_hash.put("N", new Integer(-1));
    p_hash.put("D", new Integer(-1));
    p_hash.put("E", new Integer(-1));
    p_hash.put("Q", new Integer(0));
    p_hash.put("H", new Integer(0));
    p_hash.put("R", new Integer(0));
    p_hash.put("K", new Integer(-1));
    p_hash.put("M", new Integer(-2));
    p_hash.put("I", new Integer(-2));
    p_hash.put("L", new Integer(-3));
    p_hash.put("V", new Integer(-1));
    p_hash.put("F", new Integer(-5));
    p_hash.put("Y", new Integer(-5));
    p_hash.put("W", new Integer(-6));

    a_hash.put("C", new Integer(-2));
    a_hash.put("S", new Integer(1));
    a_hash.put("T", new Integer(1));
    a_hash.put("P", new Integer(1));
    a_hash.put("A", new Integer(2));
    a_hash.put("G", new Integer(1));
    a_hash.put("N", new Integer(0));
    a_hash.put("D", new Integer(0));
    a_hash.put("E", new Integer(0));
    a_hash.put("Q", new Integer(0));
    a_hash.put("H", new Integer(-1));
    a_hash.put("R", new Integer(-2));
    a_hash.put("K", new Integer(-1));
    a_hash.put("M", new Integer(-1));
    a_hash.put("I", new Integer(-1));
    a_hash.put("L", new Integer(-2));
    a_hash.put("V", new Integer(0));
    a_hash.put("F", new Integer(-4));
    a_hash.put("Y", new Integer(-3));
    a_hash.put("W", new Integer(-6));

    g_hash.put("C", new Integer(-3));
    g_hash.put("S", new Integer(1));
    g_hash.put("T", new Integer(0));
    g_hash.put("P", new Integer(-1));
    g_hash.put("A", new Integer(1));
    g_hash.put("G", new Integer(5));
    g_hash.put("N", new Integer(0));
    g_hash.put("D", new Integer(1));
    g_hash.put("E", new Integer(0));
    g_hash.put("Q", new Integer(-1));
    g_hash.put("H", new Integer(-2));
    g_hash.put("R", new Integer(-3));
    g_hash.put("K", new Integer(-2));
    g_hash.put("M", new Integer(-3));
    g_hash.put("I", new Integer(-3));
    g_hash.put("L", new Integer(-4));
    g_hash.put("V", new Integer(-1));
    g_hash.put("F", new Integer(-5));
    g_hash.put("Y", new Integer(-5));
    g_hash.put("W", new Integer(-7));

    n_hash.put("C", new Integer(-4));
    n_hash.put("S", new Integer(1));
    n_hash.put("T", new Integer(0));
    n_hash.put("P", new Integer(-1));
    n_hash.put("A", new Integer(0));
    n_hash.put("G", new Integer(0));
    n_hash.put("N", new Integer(2));
    n_hash.put("D", new Integer(2));
    n_hash.put("E", new Integer(1));
    n_hash.put("Q", new Integer(1));
    n_hash.put("H", new Integer(2));
    n_hash.put("R", new Integer(0));
    n_hash.put("K", new Integer(1));
    n_hash.put("M", new Integer(-2));
    n_hash.put("I", new Integer(-2));
    n_hash.put("L", new Integer(-3));
    n_hash.put("V", new Integer(-2));
    n_hash.put("F", new Integer(-3));
    n_hash.put("Y", new Integer(-2));
    n_hash.put("W", new Integer(-4));

    d_hash.put("C", new Integer(-5));
    d_hash.put("S", new Integer(0));
    d_hash.put("T", new Integer(0));
    d_hash.put("P", new Integer(-1));
    d_hash.put("A", new Integer(0));
    d_hash.put("G", new Integer(1));
    d_hash.put("N", new Integer(2));
    d_hash.put("D", new Integer(4));
    d_hash.put("E", new Integer(3));
    d_hash.put("Q", new Integer(2));
    d_hash.put("H", new Integer(1));
    d_hash.put("R", new Integer(-1));
    d_hash.put("K", new Integer(0));
    d_hash.put("M", new Integer(-3));
    d_hash.put("I", new Integer(-2));
    d_hash.put("L", new Integer(-4));
    d_hash.put("V", new Integer(-2));
    d_hash.put("F", new Integer(-6));
    d_hash.put("Y", new Integer(-4));
    d_hash.put("W", new Integer(-7));

    e_hash.put("C", new Integer(-5));
    e_hash.put("S", new Integer(0));
    e_hash.put("T", new Integer(0));
    e_hash.put("P", new Integer(-1));
    e_hash.put("A", new Integer(0));
    e_hash.put("G", new Integer(0));
    e_hash.put("N", new Integer(1));
    e_hash.put("D", new Integer(3));
    e_hash.put("E", new Integer(4));
    e_hash.put("Q", new Integer(2));
    e_hash.put("H", new Integer(1));
    e_hash.put("R", new Integer(-1));
    e_hash.put("K", new Integer(0));
    e_hash.put("M", new Integer(-2));
    e_hash.put("I", new Integer(-2));
    e_hash.put("L", new Integer(-3));
    e_hash.put("V", new Integer(-2));
    e_hash.put("F", new Integer(-5));
    e_hash.put("Y", new Integer(-4));
    e_hash.put("W", new Integer(-7));

    q_hash.put("C", new Integer(-5));
    q_hash.put("S", new Integer(-1));
    q_hash.put("T", new Integer(-1));
    q_hash.put("P", new Integer(0));
    q_hash.put("A", new Integer(0));
    q_hash.put("G", new Integer(-1));
    q_hash.put("N", new Integer(1));
    q_hash.put("D", new Integer(2));
    q_hash.put("E", new Integer(2));
    q_hash.put("Q", new Integer(4));
    q_hash.put("H", new Integer(3));
    q_hash.put("R", new Integer(1));
    q_hash.put("K", new Integer(1));
    q_hash.put("M", new Integer(-1));
    q_hash.put("I", new Integer(-2));
    q_hash.put("L", new Integer(-2));
    q_hash.put("V", new Integer(-2));
    q_hash.put("F", new Integer(-5));
    q_hash.put("Y", new Integer(-4));
    q_hash.put("W", new Integer(-5));

    h_hash.put("C", new Integer(-3));
    h_hash.put("S", new Integer(-1));
    h_hash.put("T", new Integer(-1));
    h_hash.put("P", new Integer(0));
    h_hash.put("A", new Integer(-1));
    h_hash.put("G", new Integer(-2));
    h_hash.put("N", new Integer(2));
    h_hash.put("D", new Integer(1));
    h_hash.put("E", new Integer(1));
    h_hash.put("Q", new Integer(3));
    h_hash.put("H", new Integer(6));
    h_hash.put("R", new Integer(2));
    h_hash.put("K", new Integer(0));
    h_hash.put("M", new Integer(-2));
    h_hash.put("I", new Integer(-2));
    h_hash.put("L", new Integer(-2));
    h_hash.put("V", new Integer(-2));
    h_hash.put("F", new Integer(-2));
    h_hash.put("Y", new Integer(0));
    h_hash.put("W", new Integer(-3));

    r_hash.put("C", new Integer(-4));
    r_hash.put("S", new Integer(0));
    r_hash.put("T", new Integer(-1));
    r_hash.put("P", new Integer(0));
    r_hash.put("A", new Integer(-2));
    r_hash.put("G", new Integer(-3));
    r_hash.put("N", new Integer(0));
    r_hash.put("D", new Integer(-1));
    r_hash.put("E", new Integer(-1));
    r_hash.put("Q", new Integer(1));
    r_hash.put("H", new Integer(2));
    r_hash.put("R", new Integer(6));
    r_hash.put("K", new Integer(3));
    r_hash.put("M", new Integer(0));
    r_hash.put("I", new Integer(-2));
    r_hash.put("L", new Integer(-3));
    r_hash.put("V", new Integer(-2));
    r_hash.put("F", new Integer(-4));
    r_hash.put("Y", new Integer(-4));
    r_hash.put("W", new Integer(2));

    k_hash.put("C", new Integer(-5));
    k_hash.put("S", new Integer(0));
    k_hash.put("T", new Integer(0));
    k_hash.put("P", new Integer(-1));
    k_hash.put("A", new Integer(-1));
    k_hash.put("G", new Integer(-2));
    k_hash.put("N", new Integer(1));
    k_hash.put("D", new Integer(0));
    k_hash.put("E", new Integer(0));
    k_hash.put("Q", new Integer(1));
    k_hash.put("H", new Integer(0));
    k_hash.put("R", new Integer(3));
    k_hash.put("K", new Integer(5));
    k_hash.put("M", new Integer(0));
    k_hash.put("I", new Integer(-2));
    k_hash.put("L", new Integer(-3));
    k_hash.put("V", new Integer(-2));
    k_hash.put("F", new Integer(-5));
    k_hash.put("Y", new Integer(-4));
    k_hash.put("W", new Integer(-3));

    m_hash.put("C", new Integer(-5));
    m_hash.put("S", new Integer(-2));
    m_hash.put("T", new Integer(-1));
    m_hash.put("P", new Integer(-2));
    m_hash.put("A", new Integer(-1));
    m_hash.put("G", new Integer(-3));
    m_hash.put("N", new Integer(-2));
    m_hash.put("D", new Integer(-3));
    m_hash.put("E", new Integer(-2));
    m_hash.put("Q", new Integer(-1));
    m_hash.put("H", new Integer(-2));
    m_hash.put("R", new Integer(0));
    m_hash.put("K", new Integer(0));
    m_hash.put("M", new Integer(6));
    m_hash.put("I", new Integer(2));
    m_hash.put("L", new Integer(4));
    m_hash.put("V", new Integer(2));
    m_hash.put("F", new Integer(0));
    m_hash.put("Y", new Integer(-2));
    m_hash.put("W", new Integer(-4));

    i_hash.put("C", new Integer(-2));
    i_hash.put("S", new Integer(-1));
    i_hash.put("T", new Integer(0));
    i_hash.put("P", new Integer(-2));
    i_hash.put("A", new Integer(-1));
    i_hash.put("G", new Integer(-3));
    i_hash.put("N", new Integer(-2));
    i_hash.put("D", new Integer(-2));
    i_hash.put("E", new Integer(-2));
    i_hash.put("Q", new Integer(-2));
    i_hash.put("H", new Integer(-2));
    i_hash.put("R", new Integer(-2));
    i_hash.put("K", new Integer(-2));
    i_hash.put("M", new Integer(2));
    i_hash.put("I", new Integer(5));
    i_hash.put("L", new Integer(2));
    i_hash.put("V", new Integer(4));
    i_hash.put("F", new Integer(1));
    i_hash.put("Y", new Integer(-1));
    i_hash.put("W", new Integer(-5));

    l_hash.put("C", new Integer(-6));
    l_hash.put("S", new Integer(-3));
    l_hash.put("T", new Integer(-2));
    l_hash.put("P", new Integer(-3));
    l_hash.put("A", new Integer(-2));
    l_hash.put("G", new Integer(-4));
    l_hash.put("N", new Integer(-3));
    l_hash.put("D", new Integer(-4));
    l_hash.put("E", new Integer(-3));
    l_hash.put("Q", new Integer(-2));
    l_hash.put("H", new Integer(-2));
    l_hash.put("R", new Integer(-3));
    l_hash.put("K", new Integer(-3));
    l_hash.put("M", new Integer(4));
    l_hash.put("I", new Integer(2));
    l_hash.put("L", new Integer(6));
    l_hash.put("V", new Integer(2));
    l_hash.put("F", new Integer(2));
    l_hash.put("Y", new Integer(-1));
    l_hash.put("W", new Integer(-2));

    v_hash.put("C", new Integer(-2));
    v_hash.put("S", new Integer(-1));
    v_hash.put("T", new Integer(0));
    v_hash.put("P", new Integer(-1));
    v_hash.put("A", new Integer(0));
    v_hash.put("G", new Integer(-1));
    v_hash.put("N", new Integer(-2));
    v_hash.put("D", new Integer(-2));
    v_hash.put("E", new Integer(-2));
    v_hash.put("Q", new Integer(-2));
    v_hash.put("H", new Integer(-2));
    v_hash.put("R", new Integer(-2));
    v_hash.put("K", new Integer(-2));
    v_hash.put("M", new Integer(2));
    v_hash.put("I", new Integer(4));
    v_hash.put("L", new Integer(2));
    v_hash.put("V", new Integer(4));
    v_hash.put("F", new Integer(-1));
    v_hash.put("Y", new Integer(-2));
    v_hash.put("W", new Integer(-6));

    f_hash.put("C", new Integer(-4));
    f_hash.put("S", new Integer(-3));
    f_hash.put("T", new Integer(-3));
    f_hash.put("P", new Integer(-5));
    f_hash.put("A", new Integer(-4));
    f_hash.put("G", new Integer(-5));
    f_hash.put("N", new Integer(-3));
    f_hash.put("D", new Integer(-6));
    f_hash.put("E", new Integer(-5));
    f_hash.put("Q", new Integer(-5));
    f_hash.put("H", new Integer(-2));
    f_hash.put("R", new Integer(-4));
    f_hash.put("K", new Integer(-5));
    f_hash.put("M", new Integer(0));
    f_hash.put("I", new Integer(1));
    f_hash.put("L", new Integer(2));
    f_hash.put("V", new Integer(-1));
    f_hash.put("F", new Integer(9));
    f_hash.put("Y", new Integer(7));
    f_hash.put("W", new Integer(0));

    y_hash.put("C", new Integer(0));
    y_hash.put("S", new Integer(-3));
    y_hash.put("T", new Integer(-3));
    y_hash.put("P", new Integer(-5));
    y_hash.put("A", new Integer(-3));
    y_hash.put("G", new Integer(-5));
    y_hash.put("N", new Integer(-2));
    y_hash.put("D", new Integer(-4));
    y_hash.put("E", new Integer(-4));
    y_hash.put("Q", new Integer(-4));
    y_hash.put("H", new Integer(0));
    y_hash.put("R", new Integer(-4));
    y_hash.put("K", new Integer(-4));
    y_hash.put("M", new Integer(-2));
    y_hash.put("I", new Integer(-1));
    y_hash.put("L", new Integer(-1));
    y_hash.put("V", new Integer(-2));
    y_hash.put("F", new Integer(7));
    y_hash.put("Y", new Integer(10));
    y_hash.put("W", new Integer(0));

    w_hash.put("C", new Integer(-8));
    w_hash.put("S", new Integer(-2));
    w_hash.put("T", new Integer(-5));
    w_hash.put("P", new Integer(-6));
    w_hash.put("A", new Integer(-6));
    w_hash.put("G", new Integer(-7));
    w_hash.put("N", new Integer(-4));
    w_hash.put("D", new Integer(-7));
    w_hash.put("E", new Integer(-7));
    w_hash.put("Q", new Integer(-5));
    w_hash.put("H", new Integer(-3));
    w_hash.put("R", new Integer(2));
    w_hash.put("K", new Integer(-3));
    w_hash.put("M", new Integer(-4));
    w_hash.put("I", new Integer(-5));
    w_hash.put("L", new Integer(-2));
    w_hash.put("V", new Integer(-6));
    w_hash.put("F", new Integer(0));
    w_hash.put("Y", new Integer(0));
    w_hash.put("W", new Integer(17));

    Enumeration pHashVals = pamHash.elements();
    while(pHashVals.hasMoreElements()) {
      Hashtable hash = (Hashtable) pHashVals.nextElement();
      Enumeration hashVals = hash.elements();
      int maxVal = Integer.MIN_VALUE;
      while(hashVals.hasMoreElements()) {
        int currentVal = ((Integer) hashVals.nextElement()).intValue();
        if (currentVal > maxVal)
          maxVal = currentVal;
      }
      hash.put("MAX", new Integer(maxVal));
    }
  }

  static {
    Lys.addElement("AAA");
    Lys.addElement("AAG");
    Asn.addElement("AAC");
    Asn.addElement("AAT");

    Gln.addElement("CAA");
    Gln.addElement("CAG");
    His.addElement("CAC");
    His.addElement("CAT");

    Glu.addElement("GAA");
    Glu.addElement("GAG");
    Asp.addElement("GAC");
    Asp.addElement("GAT");

    Tyr.addElement("TAC");
    Tyr.addElement("TAT");

    Thr.addElement("ACA");
    Thr.addElement("ACG");
    Thr.addElement("ACC");
    Thr.addElement("ACT");

    Pro.addElement("CCA");
    Pro.addElement("CCG");
    Pro.addElement("CCC");
    Pro.addElement("CCT");

    Ala.addElement("GCA");
    Ala.addElement("GCG");
    Ala.addElement("GCC");
    Ala.addElement("GCT");

    Ser.addElement("TCA");
    Ser.addElement("TCG");
    Ser.addElement("TCC");
    Ser.addElement("TCT");
    Ser.addElement("AGC");
    Ser.addElement("AGT");

    Arg.addElement("AGA");
    Arg.addElement("AGG");
    Arg.addElement("CGA");
    Arg.addElement("CGG");
    Arg.addElement("CGC");
    Arg.addElement("CGT");

    Gly.addElement("GGA");
    Gly.addElement("GGG");
    Gly.addElement("GGC");
    Gly.addElement("GGT");

    STOP.addElement("TGA");
    STOP.addElement("TAA");
    STOP.addElement("TAG");

    Trp.addElement("TGG");

    Cys.addElement("TGC");
    Cys.addElement("TGT");

    Ile.addElement("ATA");
    Ile.addElement("ATC");
    Ile.addElement("ATT");

    Met.addElement("ATG");

    Leu.addElement("CTA");
    Leu.addElement("CTG");
    Leu.addElement("CTC");
    Leu.addElement("CTT");
    Leu.addElement("TTA");
    Leu.addElement("TTG");

    Val.addElement("GTA");
    Val.addElement("GTG");
    Val.addElement("GTC");
    Val.addElement("GTT");

    Phe.addElement("TTC");
    Phe.addElement("TTT");

    buildPAM();
  }

}
