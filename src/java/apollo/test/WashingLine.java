package apollo.test;

// import apollo.gui.*;
import apollo.gui.ApolloLoaderI;
import apollo.main.DataLoader;
import apollo.config.Config;
import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.dataadapter.gamexml.*;
import apollo.dataadapter.genbank.*;
import apollo.dataadapter.chadoxml.*;
import apollo.seq.io.*; // might not need this

import org.bdgp.swing.widget.DataAdapterChooser;
import org.bdgp.io.*;  // contains DataAdapterRegistry.class

import java.io.*;
import java.util.Vector;

public class WashingLine implements ApolloLoaderI {
    /**
     * wrapper class that works on command line
     * takes -x|s|c|etc <xml|serial|corba etc file> -gb <genbank dir>
     * as command line
     * arguments and converts file to genbank format for sequin validation
     * TO DO: bit that works out whether to save the genomic -
     *see GenbankAdapter.java.
     */
    CurationSet curation;
    ApolloDataAdapterI load_adapter;
    private Vector history = new Vector();

    public void setCurationSet (CurationSet curation) {
	this.curation = curation;
    }

    public CurationSet getCurationSet() {
	return this.curation;
    }

    public void setAdapter (ApolloDataAdapterI data_adapter) {
	this.load_adapter = data_adapter;
    }

    public Vector getHistory() {
	return history;
    }

    public static void main(String[] args) {
	WashingLine wash = new WashingLine();
	DataLoader loader = new DataLoader();
	String usage = "Bad arguments: " + args + ".\nEnter input file type and name followed by " +
	    "the output file type and name.\n" + 
	    "FOR EXAMPLE:\n\t" + 
	    "java apollo.test.WashingLine -x myfile.xml -gb mygbdir [-valid myvalidation.conf]\n" +
	    "\tor java apollo.test.WashingLine -x myfile.xml -x output.xml";

	ApolloDataAdapterI dataout = null;
	if (args.length  < 4) {
          // There should be at least 4 arguments on command line
          System.out.println (usage);
	}
	else {
          // parse out the save arguments here
	  // add do the same
	  DataAdapterRegistry adapterRegistry = Config.getAdapterRegistry();
          boolean acc = false;
          String gb_dir = null;
          String validation_config = (Config.getRootDir() + 
				      "/conf/validation.conf");
	  String game_output = null;
          String chado_xml_output = null;
          for (int i = 2; i < args.length - 1; i+=2) {
            if ( args[i].equalsIgnoreCase("-gb") ) { //save in GenBank format
              gb_dir = args[i+1];
              System.out.println ("Will save genbank files in " + gb_dir);
            }
            else if (args[i].equalsIgnoreCase("-acc")) {
              char c = args[i+1].charAt(0);
              acc = (c == 't' || c == 'T' ||
                     c == 'y' || c == 'Y');
            }
            else if (args[i].equalsIgnoreCase("-valid")) {
              validation_config = args[i+1];
            }
            else if (args[i].equalsIgnoreCase("-x")) {
              game_output = args[i+1];
            }
            else if (args[i].equalsIgnoreCase("-cx")) {
              chado_xml_output = args[i+1];
            }
            else
              System.err.println("WashingLine: don't understand argument " + args[i]);
          }

          if (gb_dir != null) {
            dataout = (GenbankAdapter)
		adapterRegistry.getAdapter("apollo.dataadapter.genbank.GenbankAdapter");
	    if (dataout == null) {
		System.out.println ("Not configured with GenbankAdapter");
		System.exit (-1);
	    }
            // let the user know by sending to stderr
            System.out.println("reading from = "+ args[1]);
            System.out.println("writing to genbank dir " + gb_dir +
                               " using " + dataout.getClass().getName());

            /* input from user -> datasource is a GenbankAdapter method
               Need to set the type before setting the input, otherwise
               there may be a null pointer exception
            */
            ((GenbankAdapter) dataout).setInputType (DataInputType.DIR);
            ((GenbankAdapter) dataout).setInput (gb_dir);
            
            // why do you have to cast datasource?
            // is it not really a GenbankAdapter???

            //whether to write .tbl and .pep OR accessions
            ((GenbankAdapter) dataout).commitAccessions(acc);

            // see if there is a file of validated genes to read
            // in, these fail the validation tests, but have been
            // approved by the curators
            if (validation_config != null) {
              ((GenbankAdapter) dataout).setValidationFile (validation_config);
            }
	  }
          // Wait a minute!  This seems to be assuming that if the INPUT format is game xml, then
          // so is the OUTPUT format!  I thought I fixed that!
	  else if (args[0].equalsIgnoreCase("-x")) {
            // true is for NO_GUI
            dataout = new GAMEAdapter(DataInputType.FILE, args[1], true);
	    if (dataout == null) {
              System.err.println("Not configured with GAMEAdapter");
              System.exit(-1);
	    }

            System.out.println("Reading from GAME XML file "+ args[1]);
	  }
	  else if (args[0].equalsIgnoreCase("-cx")) {
            dataout = new ChadoXmlAdapter();
	    if (dataout == null) {
              System.err.println("Not configured with ChadoXmlAdapter");
              System.exit(-1);
	    }
            dataout.setInput(args[1]);
            System.out.println("Reading from Chado XML file "+ args[1]);
          }
	  else
	      return;

          CurationSet main_curation = null;
	  //ApolloDataI main_curation = null;

          // Flag all peptides for retranslation, 
          // in order to fix any that might be wrong.
          Config.setRefreshPeptides(true);

	  // first read the input file
	  try {
            //main_curation = loader.getCurationSet(wash, args, null);
            CompositeDataHolder c = loader.getCompositeDataHolder(/*wash,*/args, null);
            main_curation = c.getCurationSet(0);
            wash.setCurationSet (main_curation);
          }
          catch (Exception e) {
            System.out.println("Caught exception reading " + args[1] + ": " + e);
            System.exit(-1);
          }
          
          if (main_curation != null) {
            // write the output using datasource
            // and the annotations (main_curation)
            if (game_output != null) {
              System.out.println("Writing to GAME XML file " + game_output);
              // Tell it that its input was game_output so it'll
              // write its output there (rather than overwriting
              // the input file).
              ((GAMEAdapter)dataout).setInput(game_output);
              loader.putCurationSet(dataout, main_curation.getCurationSet());
            }
            else if (chado_xml_output != null) {
              System.out.println("Writing to Chado XML file " + chado_xml_output);
              // Tell it that its input was game_output so it'll
              // write its output there (rather than overwriting
              // the input file).
              ((ChadoXmlAdapter)dataout).setInput(chado_xml_output);
              loader.putCurationSet(dataout, main_curation.getCurationSet());
            }
            else
              ((GenbankAdapter) dataout).commitChanges(main_curation.getCurationSet(), true);
          }
	}
	System.exit (0);
    }
}






