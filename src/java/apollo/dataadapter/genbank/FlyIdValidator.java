/* Written by Berkeley Drosophila Genome Project.
   Copyright (c) 2000 Berkeley Drosophila Genome Center, UC Berkeley. */

package apollo.dataadapter.genbank;

import java.net.*;
import java.util.*;
import java.io.*;
import javax.swing.*;

import apollo.datamodel.*;
import apollo.config.Config;
import apollo.seq.io.FastaFile;
import apollo.dataadapter.*;

import org.bdgp.io.*;
import org.bdgp.util.*;

public class FlyIdValidator implements FeatureValidatorI {

  public String validateFeature (FeatureSetI fs, CurationSet curation,
				 String prefix, String suffix) {
    StringBuffer buf = new StringBuffer();

    if (fs instanceof AnnotatedFeature) {
      AnnotatedFeatureI gene = (AnnotatedFeatureI) fs;
      buf.append (validateGeneID(gene, prefix, suffix));
      buf.append (validateGeneName(gene, prefix, suffix));
      buf.append (validateFBid (gene, prefix, suffix));
      buf.append (validateCyto (gene, prefix, suffix));
      buf.append (validateSynonyms (gene, prefix, suffix));
      if (gene.isProteinCodingGene()) {
	int count = gene.size();
	for (int i = 0; i < count; i++) {
	  Transcript sf = (Transcript) gene.getFeatureAt(i);
	  buf.append (validateTranscriptID(sf, prefix, suffix));
	}
      }
    }
    return buf.toString();
  }

  private String validateGeneID (AnnotatedFeatureI fs,
                                 String prefix, 
                                 String suffix) {
    StringBuffer buf = new StringBuffer();
    String id = fs.getId();
    String type = fs.getFeatureType();
    String id_type = (type.equalsIgnoreCase("gene") ?
		      "CG" :
		      (type.equals ("transposable_element") ?
		       "TE" :
		       "CR"));
    if (id == null || id.length() == 0) {
      buf.append( prefix + " ID missing " + suffix);
    }
    else if (id.length() < 3) {
      buf.append( prefix + " ID too short for " + id + " " + suffix);
    }
    else if ( ! id.startsWith(id_type) ) {
      if (! id_type.equals ("TE") ||
	  (id_type.equals ("TE") && (id.indexOf("{}") < 0)) )
	buf.append( prefix + " incorrect " + type + 
		    " ID prefix for " + id + " " + suffix);
    }
    else {
      try {
	String numstr = id.substring(2);
	int idnum = Integer.parseInt(numstr);
      }
      catch (NumberFormatException e) {
	buf.append (prefix + " incorrect ID syntax in " + id + " " + suffix);
      }
    }
    return buf.toString();
  }

  private String validateGeneName (AnnotatedFeatureI fs,
                                   String prefix, 
                                   String suffix) {
    StringBuffer buf = new StringBuffer();
    String name = fs.getName();
	
    if (name == null || name.length() == 0) {
      buf.append( prefix + " symbol is missing " + suffix);
    }
    else if (name.length() > 5 &&
	     name.charAt(0) == 'D' &&
	     name.charAt(4) == '\\') {
      buf.append( prefix + " non D. melanogaster symbol " + name + " " +
		  suffix);
    }
    else if (name.indexOf ("gr") > 0 && name.length() > 3) {
      buf.append( prefix + " possible bad greek " + name + " " +
		  suffix);
    }
    else if (name.startsWith (fs.getId().substring (0, 2)) &&
	     ! name.equals (fs.getId())) {
      buf.append( prefix + " name-ID mismatch " + 
		  name + "-" + fs.getId() + " " +
		  suffix);
    }
    return buf.toString();
  }

  private String validateFBid (AnnotatedFeatureI gene,
                               String prefix, 
                               String suffix) {
    Vector xrefs = gene.getDbXrefs();
    String FBid = null;
    String id_type = (gene.getFeatureType().equals ("transposable_element") ?
		      "FBti" : "FBgn");
    String tmp = "";
    for (int i = 0; i < xrefs.size() && FBid == null; i++) {
      DbXref xref = (DbXref) xrefs.elementAt (i);
      tmp = xref.getIdValue();
      if (tmp.startsWith(id_type) && tmp.length() == 11)
	FBid = tmp;
    }
    if (FBid == null)
      return (prefix + " Missing or incorrect FBid " + tmp + " " + suffix);
    else
      return "";
  }

  private String validateTranscriptID (Transcript fs,
				       String prefix, String suffix) {
    StringBuffer buf = new StringBuffer();
    String id = fs.getId();
    int len = id.length();
    if (id == null || len == 0) {
      buf.append( prefix + " transcript ID missing " + suffix);
    }
    if (len < 6) {
      buf.append( prefix + " transcript ID too short for " + id + " " + suffix);
    }
    else if ( ! id.startsWith ("CG") ||
	      (! id.substring(len - 3, len - 1).equals("-R"))) {
      buf.append( prefix + " incorrect transcript ID syntax in " + id + " " + suffix);
    }
    else {
      try {
	String numstr = id.substring(2, len - 3);
	int idnum = Integer.parseInt(numstr);
      }
      catch (NumberFormatException e) {
	buf.append (prefix + " incorrect transcript ID syntax in " + id + suffix);
      }
    }
    return buf.toString();
  }

  private String validateCyto (AnnotatedFeatureI gene,
                               String prefix,
                               String suffix) {
    String cyto = (String) gene.getProperty("cyto_range");
    if (cyto == null || cyto.equals (""))
      return (prefix + " missing cytology " + suffix);
    else
      return "";
  }

  private String validateSynonyms (AnnotatedFeatureI gene,
                                   String prefix, 
                                   String suffix) {
    StringBuffer buf = new StringBuffer();
    Vector syns = gene.getSynonyms();
    for (int i = 0; i < syns.size(); i++) {
      Synonym synObject = (Synonym) syns.elementAt (i);
      String syn = synObject.getName();
      if (syn.equals("") ||
	  syn.indexOf ("temp") >= 0 ||
	  syn.indexOf ("tmp") >= 0 ||
	  (syn.equals (gene.getName()) && syn.equals (gene.getId())))
	buf.append (prefix + " invalid synonym " + syn + " " + suffix);
    }
    return buf.toString();
  }

}

