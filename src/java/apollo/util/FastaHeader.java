package apollo.util;

import java.util.Vector;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.Iterator;

import apollo.datamodel.SequenceI;
import apollo.datamodel.Sequence;
import apollo.seq.io.FastaFile;

import org.apache.log4j.*;

public class FastaHeader {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(FastaHeader.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private StringBuffer description;
  private String seq_id;
  private String db;
  private String organism;
  private HashMap xrefs;

  public FastaHeader (String fasta_header) {
    try {
      parseHeader (fasta_header);
    }
    catch (Exception e) {
      logger.error ("Error parsing " + fasta_header, e);
      if (seq_id == null)
	seq_id = "unknown";
      db = "";
    }
  }

  public String getSeqId () {
    return seq_id;
  }

  public String getDescription() {
    if (description != null)
      return description.toString();
    else
      return "";
  }

  public String getDatabase() {
    return db;
  }

  public HashMap getDbXrefs() {
    return xrefs;
  }

  public String getOrganism() {
    return organism;
  }

  public SequenceI generateSequence() {
    SequenceI sequence = new Sequence(getSeqId(), "");
    sequence.setDescription(getDescription());
    sequence.setOrganism(getOrganism());
    if (!getDatabase().equals("")) {
      sequence.addDbXref(getDatabase(), getSeqId());
    }
    if (xrefs != null) {
      Iterator keys = xrefs.keySet().iterator();
      while (keys.hasNext()) {
        String xref_db = (String) keys.next();
        String acc = (String) xrefs.get(xref_db);
        sequence.addDbXref(xref_db, acc);
      }
    }
    return sequence;
  }

  private void parseHeader (String fasta_header) {
    if (fasta_header == null)
      return;
      
    if (fasta_header.length() > 1 && fasta_header.charAt(0) == '>')
      fasta_header = fasta_header.substring(1);

    description = new StringBuffer();
    seq_id = "";
    db = "";
    organism = null;
    xrefs = null;

    int index = fasta_header.indexOf(' ');
    if (index > 0) {
      description.append ((fasta_header.substring (index)).trim());
      fasta_header = fasta_header.substring (0, index);
    }

    // A | at the end of the sequence name, e.g. emb||DM_ROO|, is a problem
    while (fasta_header.lastIndexOf('|') == 
	   (fasta_header.length() - 1)) {
      fasta_header = fasta_header.substring(0, fasta_header.length() - 1);
    }

    Vector words = new Vector();
    while (fasta_header.length () > 0) {
      index = fasta_header.indexOf('|');
      if (index < 0) {
        logger.debug("FastaHeader: Pushing last word " + fasta_header);
	addWord (fasta_header, words);
	fasta_header = "";
      }
      else {
	if (index > 0) {
          logger.debug("FastaHeader: Pushing next word " + 
                       fasta_header.substring(0, index));
	  addWord (fasta_header.substring (0, index), words);
	}
	index++;
	if (fasta_header.length() > index) {
	  fasta_header = fasta_header.substring (index);
	}
      }
    }

    logger.debug("FastaHeader: found " +  words.size() + " words");

    if (words.size() == 1) {
      seq_id = (String) words.elementAt(0);
      if (seq_id.startsWith("BcDNA:"))
        seq_id = seq_id.substring("BcDNA:".length());
    }
    else if (words.size() == 2) {
      db = (String) words.elementAt(0);
      seq_id = (String) words.elementAt(1);
    }
    else {
      index = 0;
      int word_count = words.size();
      /* I hate hard-coding all of this in here, but these
         db names are not necessarily found as the first 
         word in the header. So try these first and if they
         aren't found then assume the first word is the DB
         and the second is the ID */
      for (int i = 0; i < word_count; i++) {
        String word_1 = (String) words.elementAt(i);
        String word_2 = ((i + 1) < word_count ?
                         (String) words.elementAt(i + 1) : null);
        logger.debug("FastaHeader: word_1 = " + word_1 + ", word_2 = " + word_2);
        if (isDatabaseName(word_1) &&
            word_2 != null &&
            !isDatabaseName(word_2)) {
          if (seq_id.equals("")) {
            if (!word_1.equalsIgnoreCase ("bdgp")) {
              db = (word_1.equalsIgnoreCase ("embl") ? "emb" : word_1);
            }
            index = i + 1;
            seq_id = word_2;
          } else {
            if (!word_1.equalsIgnoreCase ("bdgp")) {
              String xref_db = (word_1.equalsIgnoreCase ("embl") ?
                                "emb" : word_1);
              index = i + 1;
              String acc = word_2;
              if (xrefs == null)
                xrefs = new HashMap(1);
              xrefs.put(xref_db, acc);
            } else {
              if (xrefs == null)
                xrefs = new HashMap(1);
              xrefs.put(db, seq_id);
              db = "";
              index = i + 1;
              seq_id = word_2;
            }
          }
          logger.debug("FastaHeader: seq_id is " +  seq_id + " for db " + db);
          // go back and get the words that preceded this
          // May 2004, we may now miss a few things like locus name,
          // but since we're collecting names and accessions now its
          // too much trouble to try and salvage this info (I hope)
          // for (int j = 0; j < i; j++)
          // description.append (" " +(String) words.elementAt (j));
        }
      }
      if (seq_id.equals("")) {
        /* just guessing that it is safer to take the 2nd word
           for the ID rather than the first if the number of
           words is even, otherwise take the first */
        index = 0;
        if ((word_count % 2) == 0) {
          db = (String) words.elementAt(index++);
          seq_id = (String) words.elementAt(index);
        } else {
          seq_id = (String) words.elementAt(index);
        }
      }
      for (int i = index+1; i < word_count; i++)
        description.append (" " +(String) words.elementAt (i));
    }
    /* This extremely ugly bit of code is to replace SGML encodings
       for greek letters with the greek letters themselves. This is
       (so far) just a FlyBase hack because, for unknown reasons, they
       actually try to use the markup language (useful for display
       only) as an inherent part of the name */
    seq_id = HTMLUtil.replaceSGMLWithGreekLetter(seq_id);
    String prior_desc = description.toString();
    String cleaned_up = HTMLUtil.replaceSGMLWithGreekLetter(prior_desc);
    boolean changed = false;
    while (!cleaned_up.equals(prior_desc)) {
      prior_desc = cleaned_up;
      cleaned_up = HTMLUtil.replaceSGMLWithGreekLetter(prior_desc);
      changed = true;
    }
    if (changed)
      description = new StringBuffer (cleaned_up);

    /* Just for kicks lets see if the organism can be teased
       out of the header line as well */
    String info = description.toString();
    int index_first = info.toLowerCase().indexOf("organism:\"");
    if (index_first >= 0) {
      index_first += "organism:\"".length();
      int index_last = info.indexOf("\"", index_first);
      if (index_last > index_first) {
        organism = info.substring (index_first, index_last);
      }
    }

    logger.debug("parseHeader: seq_id = " + seq_id + " for fasta_header " + description);
  }

  private boolean isDatabaseName(String word) {
    return ((word != null) &&
            (word.equalsIgnoreCase ("bdgp") ||
             word.equalsIgnoreCase ("gb") ||
             word.equalsIgnoreCase ("ref") ||
             word.equalsIgnoreCase ("emb") ||
             word.equalsIgnoreCase ("embl") ||
             word.equalsIgnoreCase ("ug") ||
             word.equalsIgnoreCase ("sptr") ||
             word.equalsIgnoreCase ("sp") ||
             word.equalsIgnoreCase ("sreal") ||
             word.equalsIgnoreCase ("fb")));
  }

  private static void addWord (String word, Vector words) {
    if (! word.equalsIgnoreCase ("gi") &&
	! word.equalsIgnoreCase ("gnl")) {
      if (word.charAt(word.length() - 1) == ';') {
        word = word.substring(0, word.length() - 1);
      }
      words.addElement (word);
    }
  }  

}
