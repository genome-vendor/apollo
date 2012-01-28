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

public class PeptideValidator implements FeatureValidatorI {

  private static String [] specials = {"short CDS OK",
				       "Probable mutation",
				       "computed ORF is questionable",
				       "Probable pseudogene",
				       // these require patches
				       "Hand edit",
				       "Known mutation",
				       "CDS edit"};

  public String validateFeature (FeatureSetI fs, CurationSet curation,
				 String prefix, String suffix) {
    StringBuffer buf = new StringBuffer();

    if (fs instanceof AnnotatedFeature) {
      AnnotatedFeature gene = (AnnotatedFeature) fs;
      // if things have been validated against SwissProt
      // and match then there is no need to continue
      if (!okaySwissProt (gene)) {
	int count = gene.size();
	for (int i = 0; i < count; i++) {
	  Transcript transcript = (Transcript) gene.getFeatureAt(i);
	  SequenceI pep = transcript.getPeptideSequence();
	  if (pep != null) {
	    if (pep.getLength() <= 50) {
	      buf.append (knownProblem(gene, transcript,
				       prefix, 
				       (" is only " + pep.getLength() + "aa "),
				       suffix));
	    }
	    else {
	      buf.append (proportionallyShort(pep, gene, transcript,
					      prefix, suffix));
	    }
	    buf.append (validateStart(gene, transcript, pep, prefix, suffix));
	    buf.append (validatePepID (gene, transcript, pep, prefix, suffix));
	  }
	  else {
	    buf.append(prefix + " peptide not provided " + suffix);
	  }
	  if (transcript.getEnd() == transcript.getTranslationEnd()) {
	    buf.append(prefix + " missing stop codon on 3' end " + suffix);
	  }
	}
      }
    }
    return buf.toString();
  }

  private String getComment (AnnotatedFeatureI sf, String clue) {
    String text = "";
    clue = clue.toLowerCase();
    Vector comments = sf.getComments ();
    for (int i = 0; i < comments.size() && text.equals(""); i++) {
      Comment comment = (Comment) comments.elementAt (i);
      String tmp = comment.getText().toLowerCase();
      if ((tmp.indexOf (clue) >= 0)) {
	text = comment.getText();
      }
    }
    return text;
  }

  private void getAllComments (AnnotatedFeatureI sf,
				 StringBuffer buf) {
    Vector comments = sf.getComments ();
    for (int i = 0; i < comments.size(); i++) {
      Comment comment = (Comment) comments.elementAt (i);
      buf.append(buf.length() > 0 ? "--" : "");
      buf.append(comment.getText());
    }
  }

  private boolean okaySwissProt (AnnotatedFeatureI gene) {
    String sp_comment = gene.getProperty ("sp_status");
    boolean match = false;
    if (!sp_comment.equals(""))
      match = (// sp_comment.startsWith ("Not in ") ||
	       sp_comment.startsWith ("Perfect match to "));
    return match;
  
  }

  private String proportionallyShort (SequenceI pep, 
				      AnnotatedFeature gene,
				      Transcript transcript,
				      String prefix,
				      String suffix) {
    int t_length = -1;

    String text = getComment (transcript, "dicistronic");
    if (!text.equals("")) {
      int t_count = gene.size();
      int tss = transcript.getTranslationStart();
      int tes = transcript.getTranslationEnd();
      int strand = transcript.getStrand();
      String id = transcript.getId();
      boolean adjusted = false;
      for (int i = 0; i < t_count && !adjusted; i++) {
	Transcript alt = (Transcript) gene.getFeatureAt (i);
	text = getComment (transcript, "monocistronic");
	if (!alt.getId().equals (id)) {
	    // Only look at the dicistronic transcripts
	    if (text.equals ("")) {
		int alt_tss = alt.getTranslationStart();
		int alt_tes = alt.getTranslationEnd();
		// use to look and see if the two ORFs are disjoint
		// but changed that because of CG31305
		adjusted = true;
		if ((strand == 1 && alt_tss < tss) ||
		    (strand == -1 && alt_tss > tss)) {
		    // alt is the transcript with the upstream ORF
		    int upstream_index = transcript.getIndexContaining (tss);
		    t_length = transcript.getSplicedLength(upstream_index,
							   transcript.size());
		}
		else {
		    // alt is the transcript with the downstream ORF
		    int downstream_index = transcript.getIndexContaining (tes);
		    if (downstream_index < transcript.size())
			downstream_index++;
		    t_length = transcript.getSplicedLength(0,
							   downstream_index);
		}
	    }
	}
      }
      if (!adjusted)
	  return (prefix + " Unpartnered dicistronic transcript " + 
			      transcript.getName() + suffix);
    }
    else {
      text = getComment (transcript, "shared first exon");
      if (!text.equals("")) {
	t_length= transcript.getSplicedLength(1,transcript.size());
      }
    }
    if (t_length < 0) {
      t_length = transcript.getSplicedLength();
    }
    int orf_length = pep.getLength() * 3;			  
    String error_msg = "";
    if (orf_length <= (t_length * 0.20)) {
      error_msg = knownProblem(gene, 
			       transcript,
			       prefix,
			       (" peptide is " +
				pep.getLength() + "aa which is only " +
				((orf_length * 100) / t_length) +
				"% of transcript " +
				t_length + "bp (full-length=" +
				transcript.getSplicedLength() + ") "),
			       suffix);
    }
    return error_msg;
  }
      
  private String knownProblem (AnnotatedFeature gene,
			       Transcript transcript,
			       String prefix,
			       String error_msg,
			       String suffix) {
    if (!gene.isProblematic() && !transcript.isProblematic()) {
      String text = "";
      for (int i = 0; i < specials.length && text.equals(""); i++) {
	text = getComment (gene, specials[i]);
	if (text.equals(""))
	  text = getComment (transcript, specials[i]);
      }
      
      if (text.equals("")) {
	StringBuffer buf = new StringBuffer();
	getAllComments(gene, buf);
	getAllComments(transcript, buf);
	String sp_comment = gene.getProperty ("sp_status");
	if (!sp_comment.equals ("")) {
	  buf.append(buf.length() > 0 ? "--" : "");
	  buf.append(sp_comment);
	}
	return (prefix + error_msg + buf.toString() + suffix);
      }
      else
	return "";
    }
    return "";
  }

    private String validateStart (AnnotatedFeature gene, 
				  Transcript transcript,
				  SequenceI pep, 
				  String prefix, String suffix) {
      StringBuffer buf = new StringBuffer();
      String clue = "no ATG translation start identified";
      String comment = getComment(gene, clue);
      if (comment.equals ("")) {
	comment = getComment (transcript, clue);
      }
      if (comment.equals ("")) {
	clue = "Unconventional translation start";
	comment = getComment(gene, clue);
	if (comment.equals ("")) {
	  comment = getComment (transcript, clue);
	}
      }
      if (comment.equals ("") && pep.getResidues().charAt (0) != 'M') {
	buf.append(prefix + "  " + pep.getResidues().charAt(0) +
		   " is an unusual start codon " + suffix);
      }
      int tss = transcript.getTranslationStart();
      int gap = transcript.getFeaturePosition(tss) - 1;
      if ((((FeatureSetI) gene).isMissing5prime() ||
	   ((FeatureSetI) transcript).isMissing5prime())&& 
	  gap > 2) {
	buf.append(prefix + "  " + transcript.getId() +
		   " 5' partial has TSS at " +
		   tss + " beyond start by " + 
		   gap + " bases " + suffix);
      }
      return buf.toString();
    }

    private String validatePepID (AnnotatedFeature gene, 
				  Transcript transcript,
				  SequenceI pep, 
				  String prefix, String suffix) {
      StringBuffer buf = new StringBuffer();
      String mRNA_name = transcript.get_cDNASequence().getName();
      String pep_name = pep.getName();
      int indexR = mRNA_name.indexOf ("-R");
      int indexP = pep_name.indexOf ("-P");
      if (indexR < 0) {
	buf.append(prefix + "  " + mRNA_name +
		   " doesn't follow -RA syntax " + suffix);
      }
      else if (indexP < 0) {
	buf.append(prefix + "  " + pep_name +
		   " doesn't follow -PA syntax " + suffix);
      }
      else {
	if (mRNA_name.charAt (indexR + 2) != pep_name.charAt (indexP + 2)) {
	  buf.append(prefix + " mis-matched suffixes in " + 
		     mRNA_name + " and " + pep_name + "  " + suffix);
	}
	else {
	  String rna_prefix = mRNA_name.substring (0, indexR);
	  String pep_prefix = pep_name.substring (0, indexP);
	  String gene_id = gene.getId();
	  if (!gene_id.equals(rna_prefix) ||
	      !gene_id.equals(pep_prefix)) {
	    buf.append(prefix + " mis-matched IDs gene=" + gene_id +
		       " mRNA=" + mRNA_name + 
		       " peptide=" + pep_name + "  " + suffix);
	  }
	}
      }
      return buf.toString();
    }

}

