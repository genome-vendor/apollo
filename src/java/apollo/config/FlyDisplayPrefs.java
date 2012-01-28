package apollo.config;

import apollo.datamodel.*;
import apollo.config.Config;
import apollo.gui.URLQueryGenerator;
import org.bdgp.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

public class FlyDisplayPrefs extends DefaultDisplayPrefs {

  /** This is used for making (short) labels in the display. */
  public String getDisplayName (RangeI sf) {
    if (sf instanceof SeqFeatureI) 
      return getDisplayName((SeqFeatureI) sf);
    else
      return super.getDisplayName(sf);
  }
  
  private String getDisplayName (SeqFeatureI sf) {
    String display_name = "";

    // Annot
    if (sf instanceof AnnotatedFeatureI) {
      display_name = sf.getName();
    } 

    // Not Annot
    else {
      // FeatPair
      FeatureSetI fset = (sf.canHaveChildren() ? 
                          (FeatureSetI) sf : (FeatureSetI) sf.getRefFeature());
      // cant do as seqfeature as getHitSequence() is in FeatureSet and not 
      // SeqFeature and furthermore cant do a getHitFeature().getRefSequence()
      // as FeatureSet has no hit feature yet it has hit sequence
      //SeqFeatureI fset = sf.canHaveChildren() ? sf : sf.getRefFeature();
      SequenceI seq = (fset != null ? fset.getHitSequence() : null);
      SeqFeatureI featPair = null;   //FeaturePairI fp;

      if (fset != null &&
          fset.size() > 0 &&
          (fset.getFeatureAt(0).hasHitFeature())) {
        featPair = fset.getFeatureAt(0);
      } 
      else if (sf.hasHitFeature()) {
        featPair = sf;  //fp = (FeaturePairI) sf;
      } //else {//fp = null; } // not necasary - already null

      if (seq == null && featPair != null)
        seq = featPair.getHitFeature().getRefSequence();
      if (seq == null)
        seq = sf.getFeatureSequence();

      if (seq != null) {
        display_name = seq.getName() != null ? seq.getName() : "";
      } else if (featPair != null) {
        SeqFeatureI hit = featPair.getHitFeature();
        display_name = hit.getName() != null ? hit.getName() : "";
      }
      /* NOT FeaturePair
         this will only be reached if not an annot, not a FS w fps 
         and not a FeaturePair (or if fp hit name is "") 
         in otherwords a seqfeature result
         added getName in for chado gene predics which are leaf seq feats
      */
      // shouldnt we check getName != null before using biotype??
      else
        display_name = getBioTypeForDisplay(sf);
    }
    return display_name;
  }

  // Used by SeqExport for making nice name for features
  public String getHeader (RangeI sf) {
    if (sf instanceof FeaturePair) {
      return ((FeaturePair)sf).getDisplayId();
    } else {
      return getDisplayName(sf);
    }
  }

  public String generateURL (SeqFeatureI f) {
    if (f instanceof AnnotatedFeatureI)
      // For now, all annotations share a common base URL, defined in the style
      // file as ExternalRefURL.
      return generateAnnotURL((AnnotatedFeatureI) f, Config.getExternalRefURL());
    else 
      return generateAnalysisURL(f);
  }

  private String generateAnalysisURL(SeqFeatureI f) {
    // Figure out URL for getting more information about this feature.
    // If more than one feature was selected, use the LAST one.
    String id = getIdForURL(f);
    FeatureProperty prop
      = Config.getPropertyScheme().getFeatureProperty(f.getFeatureType());

    String urlPrefix = getURLPrefix(prop, f, id);

    if (urlPrefix==null) {
      String m = "Sorry, no URL registered for type " + f.getFeatureType() +
        "\nin tiers file " + Config.getStyle().getTiersFile();
      JOptionPane.showMessageDialog(null,m);
      return null;
    }
    return URLQueryGenerator.getURL(urlPrefix, id);
  }

  protected String getIdForURL (SeqFeatureI f) {
    /* 02/06/2004: If user selected a whole result (a FeatureSet),
     * neither the id nor the name is what we want.  
     * So just look at the first child of the FeatureSet 
     * (which is probably a FeaturePair).  Is this bad?  --NH */
    if (f instanceof FeatureSet) {
      f = ((FeatureSet)f).getFeatureAt(0);
    }
    String id;
    if (f instanceof FeaturePair) {
      FeaturePair fp = (FeaturePair)f;
      SeqFeatureI sbjct = (SeqFeatureI) fp.getHitFeature();
      id = sbjct.getName();
      if (id.equals("") || id.equals("no_name"))
        id = f.getName();
      //      System.out.println("getIdForURL: for feature pair " + f.getName() + ", subject name = " + id); // DEL
    } else {
      id = getDisplayName(f);
      //      System.out.println("getIdForURL: for feature " + f.getName() + ", display name = " + id); // DEL
    }
    // Special case:  BDGP EST IDs look like "LD18592.5prime" rather than "LD18592",
    // which isn't what we want.  Need to leave out the .[53]prime part.
    // (Sima says there aren't any cases where we'd want to preserve that part.)
    if (id.endsWith(".3prime") || id.endsWith(".5prime")) {
      //      System.out.println("Truncating EST/cDNA ID " + id + " to remove .[35]prime");
      id = id.substring(0, id.lastIndexOf("."));
    }
    // Some ESTs IDs have :contig1 at the end
    if (id.indexOf(":contig") > 0) {
      id = id.substring(0, id.indexOf(":contig"));
      // In r4.1, I see EST IDs like UNKNOWN_RE01983:contig1 and INVERTED_GH07123:contig1
      if (id.indexOf("_") > 0)
        id = id.substring(id.indexOf("_")+1);
    }
    // Some ESTs have _revcomp at the end, e.g. BE975849_revcomp
    if (id.indexOf("_revcomp") > 0)
      id = id.substring(0, id.indexOf("_revcomp"));

    return id;
  }

  /* Need to special-case some URLs.  It's yucky to do this,
   * but that's the only way this will work for some of the Drosophila 
   * result types.  The assumption in the code is that there is one URL 
   * per type, but for us it depends on which database the hit is on. */
  protected String getURLPrefix(FeatureProperty prop,
                                SeqFeatureI f, String id) {
    String defaultURL = prop.getURLString();
    
    if (!(f instanceof FeaturePair))
      return(defaultURL);
    
    FeaturePair fp = (FeaturePair)f;
    SeqFeatureI fs = (SeqFeatureI) fp.getRefFeature();
    String database = fs.getDatabase();
    
    String url = defaultURL;

    /* Here we go--a bunch of special cases.
     * It sucks that these are in the code--
     * these should be in the style file or something! */
    String nucleotide = "nucleotide";
    String protein = "protein";
    String ENTREZ_N = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=search&db=" + nucleotide + "&doptcmdl=GenBank&term=";
    String ENTREZ_P = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=search&db=" + protein + "&doptcmdl=GenBank&term=";
    // In most cases, the IDs for BLASTX Similarity to Fly seem to work better
    // with the default SWALL URL; however, the AA ones seem not to be in SWALL
    // and we have to look in Entrez.  Yuck.
    //    if (prop.typeEquals("BLASTX Similarity to Fly") && id.startsWith("AA")) {
    if (prop.getDisplayType().startsWith("BLASTX") && id.startsWith("AA")) {
      url = ENTREZ_P;
    }
    else if (prop.typeEquals("Rodent") && 
	     (database.indexOf("unigene") >= 0)) {
      url = ENTREZ_N;
    }
    else if (prop.typeEquals("Insect") && 
	     (database.indexOf("dbEST") >= 0)) {
      url = ENTREZ_N;
    }
    // This isn't working right yet.  The tRNA ids look like:
    // gb|AE002593|AE002593.trna5-ArgTCG
    // and we want just the part between the first two ||s.
    else {
      String acc;
      if (prop.typeEquals("tRNA-result") && 
          (database.indexOf("na_tRNA.dros") >= 0)) {
        acc = id.substring(id.indexOf("|")+1);
        acc = acc.substring(0, acc.indexOf("|"));
      } else if (prop.typeEquals("New Fly Sequence")) {
        // These accs look like gi||gb|AY135216|AY135216
        acc = id;
        if (acc.indexOf("|") >= 0)
          acc = acc.substring(acc.lastIndexOf("|")+1);

        /* We've already appended the correct acc, 
         * but the URL constructor will automatically
         * slap the uncorrected one at the end.
         * The last # is so that when the whole ID is appended at the end, it 
         * won't have any effect. */
        url = ENTREZ_N + acc + "#";
      }
    }
    return url;
  }

  /** For annotations, try to find the appropriate identifier to add to the base URL. */
  protected String generateAnnotURL (AnnotatedFeatureI g, String baseAnnotURL) {
    if (baseAnnotURL == null || baseAnnotURL.equals(""))
      return null;

    Hashtable id_hash = new Hashtable();

    String id;
    for(int i=0; i < g.getDbXrefs().size(); i++) {
      DbXref ref = (DbXref) g.getDbXrefs().elementAt(i);
      id = ref.getIdValue();
      // Apparently, if the id starts with FBan we want to replace "FBan" with "CG".
      if (id.startsWith ("FBan")) {
        id = id.substring ("FBan".length());
        try {
          int cg_num = Integer.parseInt (id);
          id = "CG" + cg_num;
          id_hash.put (id, id);
        } catch (Exception e) {}
      }
      else if (id.startsWith ("FB")) {  // FBgn, FBab, etc.
        id_hash.put (id, id);
      }
      else if (id.startsWith ("TE")) {  // FBgn, FBab, etc.
        id_hash.put (id, id);
      }
    }

    id = getCG (g.getId(), "CG");
    if (id != null)
      id_hash.put (id, id);
    else {
      id = getCG (g.getId(), "CR");
      if (id != null)
        id_hash.put (id, id);
      else {
        id = getCG (g.getId(), "FB");
        if (id != null)
          id_hash.put (id, id);
      }
    }
  
    id = getCG (g.getName(), "CG");
    if (id != null)
      id_hash.put (id, id);
    else {
      id = getCG (g.getName(), "CR");
      if (id != null)
        id_hash.put (id, id);
    }

    for(int i=0; i < g.getSynonyms().size(); i++) {
      Synonym syn = (Synonym)(g.getSynonyms().elementAt(i));
      id = getCG (syn.getName(), "CG");
      if (id != null)
        id_hash.put (id, id);
      else {
        id = getCG (syn.getName(), "CR");
        if (id != null)
          id_hash.put (id, id);
        else {
          id = getCG (g.getId(), "FB");
          if (id != null)
            id_hash.put (id, id);
        }
      }
    }

    // The code here was glomming together all the elements into one 
    // big |ed string, but that's not what the FlyBase annotation report CGI wants.  
    // An FB or CG or CR identifier should be sufficient.
    StringBuffer query_str = new StringBuffer();
    for (Enumeration e = id_hash.elements(); e.hasMoreElements() ;) {
      String namepart = (String) e.nextElement();
      //      System.out.println("Considering namepart " + namepart); // DEL
      if (namepart.indexOf("FB") == 0 ||
          namepart.indexOf("CG") == 0 ||
	  namepart.indexOf("CR") == 0) {
        if (query_str.length() > 0)
          query_str.replace(0, query_str.length(), namepart);
        else
          query_str.append(namepart);
        break;
      }
      if (query_str.length() > 0)
        query_str.append ('|');
      query_str.append (namepart);
    }

    // if not FlyBase ID, use the annotation's regular ID
    if (query_str.length() == 0) {
      query_str.append(g.getId());
    }
    
    String url = null;
    //    System.out.println("generateAnnotUrl: query = " + query_str); // DEL
    if (query_str.length() > 0) {
      // if a custom URL is setup for this type in the tiers file, use that - otherwise use the global
      // one setup in the style file
      FeatureProperty fp = Config.getPropertyScheme().getFeatureProperty(g.getTopLevelType());
      if (fp.getURLString() != null && fp.getURLString().length() > 0) {
        url = fp.getURLString() + g.getId();
      }
      else {
        url = baseAnnotURL + query_str.toString();
      }
    }
    
    return url;
  }

  /** Apparently this method gets the first portion of a name (up to the :, if any)
      and returns it IF it starts with the desired prefix. */
  private String getCG (String name, String prefix) {
    String cg_name = null;
    String tmp_name = name;
    if (tmp_name.startsWith (prefix)) {
      int index = tmp_name.indexOf (":");
      if (index > 0)
        tmp_name = tmp_name.substring (0, index);
      if (!tmp_name.equals (prefix)) {
        //        String num_str = tmp_name.substring(prefix.length());
        cg_name = tmp_name;
      }
    }
    return cg_name;
  }
  
  public String getTitle (CurationSet curation) {
    if (curation.getChildCurationSets().size() <= 0)
      return curation.toString();
    else
      return "composite";
  }

  /** This just returns sf.getBioType() (capitalized) unless its a transcript.
      For transcript it only returns "Transcript" if its part of a Gene.
      Otherwise it returns its parent's bio type + transcript. 
      (should it not tack on the transcript?)
      Thus a tRNA's transcript would display "tRNA transcript", where a gene's
      transcript would display just "Transcript". Should it actually display
      "Gene transcript"? 
      rename getTypeForDisplay?
  */
      
  public String getBioTypeForDisplay(SeqFeatureI sf) {
    String typeDisplay;
    if (sf instanceof AnnotatedFeatureI) {
      typeDisplay = sf.getTopLevelType();
      typeDisplay = capitalize(typeDisplay);
      if (!sf.getTopLevelType().equalsIgnoreCase(sf.getFeatureType()))
        typeDisplay += " " + capitalize(sf.getFeatureType());
    } else {
      String db = sf.getDatabase();
      // Don't bother showing db name if it is "dummy"
      if (db != null && !db.equals("") && !db.equals("dummy"))
        typeDisplay = sf.getProgramName()  + ":" + sf.getDatabase();
      // yet again, the users have requested a change
      // Now instead of an enumeration it is the range of the feature
      else {
        typeDisplay = (sf.getProgramName() + ":" +
                       sf.getStart() + "-" + sf.getEnd());
      }
    }
    return typeDisplay;
  }
  
  private String capitalize(String s) {
    if (s.endsWith("RNA"))
      return s;
    String first = s.substring(0,1);
    first = first.toUpperCase();
    return first + s.substring(1);
  }
  
  private void print(String m) { System.out.println(m); }

}
