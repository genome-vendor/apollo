package apollo.dataadapter.ensj;

import apollo.datamodel.*;
import apollo.datamodel.seq.*;

import java.util.*;
import java.sql.*;


import org.ensembl.*;
import org.ensembl.driver.*;
import org.ensembl.datamodel.*;
import org.ensembl.util.*;

public class DataModelConversionUtil {

  // Recursively descend a feature tree and reset the start and stop codon positions,
  // doing a bit of position modification and missing terminus flagging along the way.
  // The main use of this function is to convert from ensembls representation of start
  // and stop to the apollo one. Also it allows the starts and stops to be reset after
  // the correct sequence has been set as the ref sequence of the features (often
  // when the features are initially loaded they come from a db with no sequence and
  // a reference sequence is attached to them from another db).

  // Differences between ensembl and apollo in representing starts and stops
  //   All ensembl genes with translations have a start of translation set, but 
  //   this doesn't signify that its an ATG - to know that we have to look at 
  //   the sequence
  //   All ensembl genes with translations have an end of translation set, but
  //   this isn't necessarily a stop codon (TAA, TAG, TGA). Also ensembl sets
  //   its stop position to include the stop codon whereas apollo wants it to
  //   be set as the first base of the stop codon. Again to figure out what
  //   change needs to be made we have to get the sequence
  public static void resetTranslationStartsAndStops(SeqFeatureI sf) {
    if (sf.getNumberOfChildren() != 0) {
      FeatureSetI fs = (FeatureSetI)sf;
      if (fs.getTranslationStart() != 0) {
        fs.setTranslationStart(fs.getTranslationStart());
        if (fs.getStartCodon() != null && !fs.getStartCodon().equals("ATG") && fs.getFeaturePosition(fs.getTranslationStart()) < 4) {
          fs.setMissing5prime(true);
        }
        String stop_codon = fs.getRefSequence().getResidues(fs.getTranslationEnd()-(2*fs.getStrand()), fs.getTranslationEnd());
        if (stop_codon != null) {
          if (stop_codon.equals("TGA") ||
              stop_codon.equals("TAG") ||
              stop_codon.equals("TAA")) {
            // System.out.println("stop = " + stop_codon);
            fs.setTranslationEnd(fs.getTranslationEnd()-(2*fs.getStrand()));
          } else {
            // System.out.println("Missed stop = " + stop_codon);
            fs.setMissing3prime(true);
          }
        }
      } else {
        Vector feats = fs.getFeatures();
        for (int i=0;i<feats.size();i++) {
          resetTranslationStartsAndStops((SeqFeatureI)feats.get(i));
        }
      }
    }
  }
}
