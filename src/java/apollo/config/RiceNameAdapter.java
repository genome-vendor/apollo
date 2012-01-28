package apollo.config;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.SeqFeatureI;
import apollo.editor.CompoundTransaction;

public class RiceNameAdapter extends GmodNameAdapter {




  /** The prefix to use if idFormat is not specified in tiers file for a type */
  protected String getDefaultIDPrefix() {
    return "RICE";
  }

//   /** no-oped. Rice doesnt need to change prot/seq names. apollo never displays seq
//       names and rice triggers automatically change prot names on transcript name change
//       This is called by FlyNameAdapter setTranscriptNameFromAnnot(). used to be called
//       by FED as well - no longer.*/
//   protected CompoundTransaction changeSeqNames(AnnotatedFeatureI trans) {
//     return null;
//   }
}
