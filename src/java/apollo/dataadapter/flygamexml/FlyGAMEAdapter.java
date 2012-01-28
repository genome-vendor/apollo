/* Written by Berkeley Drosophila Genome Project.
   Copyright (c) 2000 Berkeley Drosophila Genome Center, UC Berkeley. */

package apollo.dataadapter.flygamexml;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileReader;
import java.io.File;
import java.io.BufferedReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.apache.log4j.*;

import org.bdgp.io.DataAdapterUI;
import org.bdgp.io.IOOperation;
import org.bdgp.util.ProgressEvent;
import org.bdgp.xml.XMLElement;

import apollo.config.ApolloNameAdapterI;
import apollo.config.Config;
import apollo.config.Style;
import apollo.dataadapter.AbstractApolloAdapter;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.DataInput;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.DataInputType.UnknownTypeException;
import apollo.dataadapter.Region;
import apollo.dataadapter.StateInformation;
import apollo.dataadapter.TransactionOutputAdapter;
import apollo.dataadapter.chado.ChadoTransactionTransformer;
import apollo.dataadapter.chadoxml.ChadoTransactionXMLWriter;
import apollo.dataadapter.gamexml.*;
import apollo.datamodel.AnnotatedFeature;
import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.Comment;
import apollo.datamodel.CurationSet;
import apollo.datamodel.DbXref;
import apollo.datamodel.Exon;
import apollo.datamodel.ExonI;
import apollo.datamodel.FeaturePair;
import apollo.datamodel.FeaturePairI;
import apollo.datamodel.FeatureSet;
import apollo.datamodel.FeatureSetI;
import apollo.datamodel.Protein;
import apollo.datamodel.RangeI;
import apollo.datamodel.SeqFeature;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.Sequence;
import apollo.datamodel.SequenceI;
import apollo.datamodel.StrandedFeatureSet;
import apollo.datamodel.StrandedFeatureSetI;
import apollo.datamodel.Synonym;
import apollo.datamodel.Transcript;
import apollo.datamodel.seq.GAMESequence;
import apollo.editor.Transaction;
import apollo.editor.TransactionManager;
import apollo.main.Version;
import apollo.util.DateUtil;
import apollo.util.FastaHeader;
import apollo.util.HTMLUtil;
import apollo.util.IOUtil;
import apollo.main.*;

/**
 * Reader for Drosophila GAME XML files.
 * Inherits from GAMEAdapter.
 *
 */
public class FlyGAMEAdapter extends GAMEAdapter {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(FlyGAMEAdapter.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private String NAME_LABEL_FOR_WRITING = "Fly annotations (GAME XML format)";
  private String NAME_LABEL_FOR_READING = "Fly annotations (GAME XML format)";

  public FlyGAMEAdapter() {
    // Set the name that is used in the Data Adapter Chooser dropdown list.
    // Can be ovverridden by an optional third arg in the DataAdapterInstall
    // line in apollo.cfg, e.g.
    // DataAdapterInstall	"apollo.dataadapter.flygamexml.FlyGAMEAdapter" "fly.style" "D. pseudoobscura (GAME XML format)"
    setName(NAME_LABEL_FOR_READING);
  }

  public FlyGAMEAdapter(DataInputType inputType, String input) {
    setInputType(inputType);
    setInput(input);
  }

  public FlyGAMEAdapter(DataInputType inputType, String input, boolean noGUI) {
    NO_GUI = noGUI;
    setInputType(inputType);
    setInput(input);
  }

  public void init() {}

  public DataAdapterUI getUI(IOOperation op) {
    if (!super.operationIsSupported(op))
      return null; // shouldnt happen
    DataAdapterUI ui = super.getCachedUI(op);
    if (ui == null) {
      ui = new FlyGAMEAdapterGUI(op);
      super.cacheUI(op,ui);
    }
    if (op.equals(OP_WRITE_DATA))
      // It would be great to put the GAME version string in the name, but
      // at the time the GAME adapter GUI is set up, we don't yet know which
      // GAME version we'll be using (because Config hasn't yet read
      // DO-ONE-LEVEL-ANNOTS from apollo.cfg)
      super.setName(NAME_LABEL_FOR_WRITING); // + " (version " + GAMESave.gameVersion  + ")");
    else
      super.setName(NAME_LABEL_FOR_READING);
    return ui;
  }

  /** Tests all the input types - can enter filename as arg */
  public static void main(String [] args) throws ApolloAdapterException {
    FlyGAMEAdapter databoy;

    // So I mistakedly made input type here FILE and it caused an endless loop
    // probably in getStreamFromFile (recursive call there)
    String url = "http://www.fruitfly.org/annot/gbunits/xml/AE003650.xml";
    databoy = new FlyGAMEAdapter(DataInputType.URL,url);
    testAdapter(databoy);

    databoy = new FlyGAMEAdapter(DataInputType.GENE,"cact");//args[0]);
    testAdapter(databoy);

    databoy = new FlyGAMEAdapter(DataInputType.CYTOLOGY,"34A");
    testAdapter(databoy);

    databoy = new FlyGAMEAdapter(DataInputType.SCAFFOLD,"AE003490");
    testAdapter(databoy);

    String file = "/users/mgibson/cvs/apollo/dev/sanger/data/josh";
    if (args.length > 0) file = args[0];
    databoy = new FlyGAMEAdapter(DataInputType.FILE,file);
    testAdapter(databoy);

    String seq = "actggcgtgctgtgttattagtgatgatgtcgcaatcgtgaatcgatgcatgcacacatcgtgtgtgtggtctgcgaatatggcattccgtaaagtgccgcgcgtatgtcgcgcgattatgatgtatgctgctgatgtagctgtgatattctaatgagtgctgatcgtgatgtagtcgtagtctagctagctagtcgatcgtagctacgtagctagctagcttgtgtgcgcgcgctg";
    databoy = new FlyGAMEAdapter(DataInputType.SEQUENCE,seq);
    testAdapter(databoy);
  }
  private static void testAdapter(FlyGAMEAdapter databoy) {
    try {
      CurationSet curation = databoy.getCurationSet();
      apollo.dataadapter.debug.DisplayTool.showFeatureSet(curation.getResults());
    } catch ( ApolloAdapterException ex ) {
      logger.error("No data to read", ex);
    }
  }

}
