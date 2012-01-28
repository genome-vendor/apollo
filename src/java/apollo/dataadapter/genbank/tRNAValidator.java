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

public class tRNAValidator implements FeatureValidatorI {
  private final static int tRNA_MAX = 86;
  private final static int tRNA_MIN = 70; 

    public tRNAValidator() {
    }

  public String validateFeature (FeatureSetI feature, CurationSet curation,
                                 String prefix, String suffix) {
	StringBuffer buf = new StringBuffer();
        AnnotatedFeature gene = (feature instanceof AnnotatedFeature ? 
                                 (AnnotatedFeature) feature : null);
        if (gene == null)
            return "";
	if (gene.getFeatureType().equalsIgnoreCase ("trna")) {
            String aa = gene.getProperty ("aminoacid");
            if (aa == null || aa.equals(""))
                buf.append(prefix + " missing amino acid" + suffix);
	    if (!gene.getName().startsWith ("CR"))
		buf.append (prefix + " is incorrectly named " + suffix);
	    // if (gene.length() > 151)
	    if (gene.length() > tRNA_MAX)
		buf.append (prefix + " is > " + tRNA_MAX +
			    " bases, it is " + gene.length() + 
			    " bases " +suffix);
	    if (gene.length() < tRNA_MIN)
		buf.append (prefix + " is < " + tRNA_MIN +
			    " bases, it is " + gene.length() + 
			    " bases " +suffix);
	}
	return buf.toString();
    }


}

