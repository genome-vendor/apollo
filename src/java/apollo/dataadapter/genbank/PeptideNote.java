package apollo.dataadapter.genbank;

public class PeptideNote {
  String peptide;
  String note;
  String accession;
  String peptide_id;
  String gene_name;
  String reference;
  int ref_count;

  public String getPeptide (String current_peptide) {
    if (peptide != null && !peptide.equals ("-") && !peptide.equals ("")) {
      System.out.println ("Using patch for peptide of " + 
			  gene_name + "::" + peptide_id);
      
      return peptide;
    }
    else
      return current_peptide;
  }

  public void setPeptide(String peptide) {
    this.peptide = peptide;
  }

  public int getReferenceNumber () {
    return ref_count;
  }

  public String getReference () {
    System.out.println ("For " + 
			gene_name + "::" + peptide_id +
			" returning reference " + reference);
    return reference;
  }

  public void setReference (String ref, int count) {
    if (ref == null && reference != null) {
      try {
	throw new Exception ();
      }
      catch (Exception e) {
	e.printStackTrace();
      }
    }
    this.reference = ref;
    this.ref_count = count;
    if (reference != null)
      System.out.println ("Set reference of " +
			  gene_name + "::" + peptide_id +
			  " to " + reference);
  }

  public String getNote () {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public String getAccession () {
    return accession;
  }

  public void setAccession(String acc) {
    this.accession = acc;
  }

  public String getPeptideId () {
    return peptide_id;
  }

  public void setPeptideId(String peptide_id) {
    this.peptide_id = peptide_id;
  }

  public String getGeneName () {
    return gene_name;
  }

  public void setGeneName(String gene_name) {
    this.gene_name = gene_name;
  }

}
