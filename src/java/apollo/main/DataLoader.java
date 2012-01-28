package apollo.main;

import java.lang.reflect.*;
import java.io.*;
import java.awt.Color;
import javax.swing.*;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.*;

import org.bdgp.io.DataAdapter;
import org.bdgp.io.DataAdapterRegistry;
import org.bdgp.io.IOOperation;
import org.bdgp.io.VisualDataAdapter;
import org.bdgp.util.*;
import org.bdgp.swing.widget.DataAdapterChooser;

import apollo.config.Config;
import apollo.datamodel.*;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.GFFAdapter;
import apollo.dataadapter.NotImplementedException;
import apollo.dataadapter.StateInformation;
import apollo.dataadapter.SerialDiskAdapter;
import apollo.seq.io.*;
import apollo.gui.synteny.CurationManager;
import apollo.gui.ApolloFrame;
import apollo.gui.CheckMemoryThread; // move to main?

/**
 * This class handles loading of data into Apollo.
 * used by Apollo and LoadUtil
 * Keeps instance of CompositeDataHolder that it creates, used to get sequence from
 * (should probably have cdh passed in for getSeq)
 * should probably make singleton
 * maybe this belongs in apollo.dataadapter?
 */
public class DataLoader {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(DataLoader.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------
  
  private ApolloDataAdapterI data_adapter;
  //private ApolloDataI apolloData;
  /** If single species put curation set in composite data holder anyways */
  private CompositeDataHolder compositeDataHolder;

  /** return_value is set by the thread inner classes, which the outer class then
      retrieves - a little confusing - its either a CurSet or a CDH */
  private Object return_value;

  public DataLoader() {}

  /** Load data using props.
      gets dataadapter from configs adapter registry using props adapter property,
      sets data adapters state information and region with props and calls getData */
  public CompositeDataHolder getCompositeDataHolder(//ApolloLoaderI frame,
                                                    Properties props,
                                                    ProgressListener listener)
  throws ApolloAdapterException {
    
    try {
      
      DataAdapterRegistry adapterRegistry = Config.getAdapterRegistry();
      
      String adapterClass = props.getProperty(StateInformation.DATA_ADAPTER);
      ApolloDataAdapterI datasource = 
        (ApolloDataAdapterI)adapterRegistry.getAdapter(adapterClass);
      
      if (datasource == null) {
        
        logger.warn(adapterClass+" not found in registry. " +
                    "this is what's available:");
        
        // this doesnt seem to work...
        String[] names = adapterRegistry.getAdapterNames(IOOperation.READ);
        
        for (int i=0; i<names.length; i++) {
          logger.warn("  "+names[i]);
        }
        
      } 
      else {
        
        // clean up old curation set (remove as listener) -> regChangeEvent
        //if (compositeDataHolder!=null) compositeDataHolder.cleanup();
        
        // now sets state info before set region
        datasource.setStateInformation(props);
        datasource.setRegion(props.getProperty("region"));
        compositeDataHolder = getData(/*frame,*/datasource, listener);
        
      }
    } 
    catch (Exception ie) {
      throw new apollo.dataadapter.ApolloAdapterException(ie.getMessage(), ie);
    }
    return compositeDataHolder;
  }

  /** String[0] in the args tells
   * which data adapter to use: -g GFF, -x GAME, -b backup (serial), -gb GenBank/EMBL
   * followed by the filename
   * e.g., apollo -x data/example.xml
   quitOnCancel is false
   */
  public CompositeDataHolder getCompositeDataHolder(
    //ApolloLoaderI frame,
    String[] args,
    ProgressListener listener
  )
  throws apollo.dataadapter.ApolloAdapterException {
    // Last arg means DON'T exit if user hits cancel
    //return getCurationSet(frame, args, listener, false);
    return getCompositeDataHolder(/*frame,*/args, listener, false); // quitOnCancel false
  }

  public CompositeDataHolder getCompositeDataHolder(CommandLine commandLine) 
    throws ApolloAdapterException {

    ApolloDataAdapterI datasource = null;

    // check if data adapter specified on command line...
    if (commandLine.readIsSpecified()) {
      try {
        datasource = commandLine.getReadAdapter();
      } catch (Exception e) {
        throw new ApolloAdapterException(e);
      }
    }
    
    // if no datasource is specified by cmdline, datasouce is null and getData
    // brings up DataAdapterChooser gui.
    compositeDataHolder = getData(datasource,null,true); // throws AAE
    
    return compositeDataHolder;
  }

  /** String[0] in the args tells
   * which data adapter to use: -g GFF, -x GAME, -b backup (serial), -gb GenBank/EMBL
   * followed by the filename
   * e.g., apollo -x data/example.xml
   * (Called by ApolloRunner.mainRun.)
   After resolving datasource from args and retrieving dataadapter calls getData
   */
  private CompositeDataHolder getCompositeDataHolder(String[] args,
                                                    ProgressListener listener,
                                                    boolean quitOnCancel)
    throws apollo.dataadapter.ApolloAdapterException {

    ApolloDataAdapterI datasource = null;
    
    // This is a very rigid method of dealing with arguments.  Need a more
    // general, flexible approach.
    if (args.length >= 1) {
      DataAdapterRegistry adapterRegistry = Config.getAdapterRegistry();

      // should i keep this old stuff around for backwards compatibility?
      // should it be encoded in CommandLine?
      try {
        // can throw exception
        CommandLine commandLine = CommandLine.getCommandLine();
        commandLine.setArgs(args); //new CommandLine(args,adapterRegistry);
        if (commandLine.readIsSpecified())
          datasource = commandLine.getReadAdapter();
        if (datasource != null) {
          compositeDataHolder = getData(datasource,listener,quitOnCancel);
          return compositeDataHolder;
        }

        if (args[0].startsWith("-h")) {
          commandLine.printHelp();
          System.exit(0);
        } 
        // If the -g (gff) file argument is specified, 
        // user may also use -s fastafile.
        // The -g and -s files can be in either order.
        // Use equals, not startsWith because other formats
        // (like genbank) also start with a g 
        // -gb for genbank
        if ((args[0].equals("-g") || args[0].equals("-gff")) ||
            (args.length > 2 &&
             (args[2].equals("-g") || args[2].equals("-gff")))) {
          String gffFile = null;
          String fastaFile = null;
          // apollo -g foo.gff [-s foo.fasta]
          if (args[0].startsWith("-g")) {
            gffFile = args[1];
            if (args.length > 2 && args[2].startsWith("-s"))
              fastaFile = args[3];
            else if (args.length > 2) {
              commandLine.printHelp();
              logger.warn("Ignoring unknown arguments: " + args[2] + " " + args[3]);
            }
          }
          // apollo -s foo.fasta -g foo.gff
          else {
            gffFile = args[3];
            if (args[0].startsWith("-s"))
              fastaFile = args[1];
            else {
              commandLine.printHelp();
              logger.warn("Ignoring unknown arguments: " + args[0] + " " + args[1]);
            }
          }

          // Load the GFFAdapter
          datasource = (GFFAdapter)
                       adapterRegistry.getAdapter("apollo.dataadapter.GFFAdapter");
          ((GFFAdapter) datasource).setFilename(gffFile);
          //((GFFAdapter) datasource).setRegion(gffFile); // pase'
          if (fastaFile != null)
            ((GFFAdapter) datasource).setSequenceFilename(fastaFile);
        } 
        else if (args[0].equals("-b")) {
          // Load the SerialDiskAdapter (backup file)
          datasource = (SerialDiskAdapter)
                       adapterRegistry.getAdapter("apollo.dataadapter.SerialDiskAdapter");
          ((SerialDiskAdapter) datasource).setFilename(args[1]);
        } 
        else if (args[0].equals("-x")) {
          // Load the GAMEAdapter
          datasource = (apollo.dataadapter.gamexml.GAMEAdapter)
                       adapterRegistry.getAdapter("apollo.dataadapter.gamexml.GAMEAdapter");
          if (datasource == null) {
            logger.error ("Unable to create a GAME data adapter");
          } else {
            // This may not be working.
            DataInputType inputType = DataInputType.FILE;
            if (args[1].startsWith("http:") ||
                args[1].startsWith("file:")) {
              inputType = DataInputType.URL;
            }
            ((apollo.dataadapter.gamexml.GAMEAdapter) datasource).setInputType(inputType);
            ((apollo.dataadapter.gamexml.GAMEAdapter) datasource).setInput(args[1]);
          }
        }
        else if (args[0].equals("-gb")) {
          datasource = (apollo.dataadapter.genbank.GenbankAdapter)
                       adapterRegistry.getAdapter("apollo.dataadapter.genbank.GenbankAdapter");
          if (datasource == null) {
            logger.error ("Unable to create a Genbank data adapter");
          } else {
            ((apollo.dataadapter.genbank.GenbankAdapter) datasource).setInputType(DataInputType.FILE);
            ((apollo.dataadapter.genbank.GenbankAdapter) datasource).setInput(args[1]);
          }
        }
        else if (args[0].equals("-cx") || args[0].equals("-chado")) {
          datasource = (apollo.dataadapter.chadoxml.ChadoXmlAdapter)
                       adapterRegistry.getAdapter("apollo.dataadapter.chadoxml.ChadoXmlAdapter");
          if (datasource == null) {
            logger.error ("Unable to create chadoXML data adapter");
          } else {
            ((apollo.dataadapter.chadoxml.ChadoXmlAdapter) datasource).setInput(args[1]);
          }
        }
        else if (args[0].startsWith("-")) {
          commandLine.printHelp();
          throw new apollo.dataadapter.ApolloAdapterException("Unrecognized command line argument: "+args[0]);
        }
        else {
          /* If a file is specified with no arguments,
             assume it's GAME (for now).
             (This is so that we can webstart on GAME files without
             passing command-line args.) !! Should check whether
             file is really GAME.  But we can't just try to
             open it now, because it might be a relative pathname 
             that the GAMEAdapter is cleverly going to complete.
             Seems harmless enough to try opening the file as GAME,
             since otherwise we'd definitely fail. */
          logger.error("No filetype argument specified--assuming " + 
                       args[0] + " is GAME XML.");
          datasource = (apollo.dataadapter.gamexml.GAMEAdapter)
          adapterRegistry.getAdapter("apollo.dataadapter.gamexml.GAMEAdapter");
          if (datasource == null) {
            logger.error ("Unable to create a GAME data adapter");
            return null;
          }
          else {
            DataInputType inputType = DataInputType.FILE;
            if (args[0].startsWith("http:") ||
                args[0].startsWith("file:"))
              inputType = DataInputType.URL;
            ((apollo.dataadapter.gamexml.GAMEAdapter) datasource).setInputType(inputType);
            ((apollo.dataadapter.gamexml.GAMEAdapter) datasource).setInput(args[0]);
          }
        }
        
      } catch (Exception ie) {
        logger.error(ie.getMessage(), ie);
        throw new apollo.dataadapter.ApolloAdapterException(ie.getMessage());
      }
      
      if (datasource != null) {
        compositeDataHolder = getData (datasource, listener, quitOnCancel);
      }
      
    } else if (args.length == 0) {
      // datasource is null - will bring up data chooser
      compositeDataHolder = getData(datasource, listener, quitOnCancel);
    } else { // this is for args.length < 0 - how the heck is that possible????
      CommandLine.getCommandLine().printHelp();
      StringBuffer arglist = new StringBuffer();
      for (int i = 0; i < args.length; i++)
        arglist.append("arg " + i + ": " + args[i] + " ");
      throw new apollo.dataadapter.ApolloAdapterException("Invalid command line: " + arglist);
    }
    return compositeDataHolder;
  }

  private CompositeDataHolder getData(
    //final ApolloLoaderI frame,
    ApolloDataAdapterI datasource,
    ProgressListener listener
  )
  throws apollo.dataadapter.ApolloAdapterException {
    // Last arg is whether to exit if user hits cancel
    // (true the first time this is called; false subsequently)
    return getData(datasource, listener, false);
  }

  
  /** if DataAdapter datasource is null, brings up DataAdapterChooser to get one.
      Changed: Returns CompositeDataHolder. If its just one species, returns
      CompositeDataHolder that only has one CurationSet.
   */
  private CompositeDataHolder getData(
    //final ApolloLoaderI frame,
    ApolloDataAdapterI datasource,
    ProgressListener listener,
    final boolean quitOnCancel
  )
  throws apollo.dataadapter.ApolloAdapterException {

    //CurationSet out = null;
    CompositeDataHolder out = null;

    //    long lasttime = System.currentTimeMillis();

    // if datasource is null get an adapter from data adapter chooser gui
    if (datasource == null) {
      boolean appendData = false;
      DataChooserThread chooserRun = new DataChooserThread(quitOnCancel,appendData);
      chooserRun.run();
      
      if (return_value == null)
        return null;

      ApolloDataI ad = (ApolloDataI) return_value;
      // convert single species to CompositeDataHolder - easier that way
      // this actually makes ApolloDataI a lot less relevant, irrelevant?
      if (ad.isCurationSet())
        out = new CompositeDataHolder(ad.getCurationSet());
      else if (ad.isCompositeDataHolder())
        out = ad.getCompositeDataHolder();
      return_value = null;

      // Do gc here to stop initial pause on first scroll
      System.gc();
    } 

    // datasource is nonnull. dont need data adapter chooser. use dataadapter directly.
    // I believe this can only happen with single species adapter - no way to do
    // this with synteny - so datasource.getCurationSet is ok
    // (e.g. provided on command line)
    else { 
      // datasource is nonnull
      data_adapter = datasource;

      if (listener != null) {
        if (datasource instanceof VisualDataAdapter) {
          ((VisualDataAdapter)datasource).addProgressListener(listener);
        }
      }
      
      try {
        // Its a single species adapter - or at least it better be!
         CurationSet cs = datasource.getCurationSet();
         out = new CompositeDataHolder(cs);
      }
      catch (apollo.dataadapter.ApolloAdapterException e) {
        JOptionPane msg = new JOptionPane();
        logger.error("format error", e);
        msg.showMessageDialog(
          ApolloFrame.getFrame(),
          e.toString(),
          "Format error",
          JOptionPane.WARNING_MESSAGE
        );
      }
    }

    //    long curtime = System.currentTimeMillis();
    Config.getController().curationSetIsLoaded (out != null);
    // getSeq used to be called by outside caller - seems more convenient to put here
    getSequence(out);
    return out;
  }

  /** Inner class that runs DataChooser on separate thread.
      sets instance Object return_value 
    Why is this a separate thread - is it so you can still interact with
    the gui while the chooser is up?
    Steve Searle in cvs comment of 8.23.01 wrote:
     Put DataChooser code into a Runnable to run on the AWTEvent dispatch
    thread. I don't think this should be necessary but Exceptions
    were being thrown on Alphas. */
  private class DataChooserThread implements Runnable {
    
    private boolean quitOnCancel;
    private boolean appendData;

    private DataChooserThread(boolean quitOnCancel, boolean appendData) {
      this.quitOnCancel = quitOnCancel;
      this.appendData = appendData;
    }

    public void run() {
      IOOperation op = appendData ? 
        ApolloDataAdapterI.OP_APPEND_DATA : ApolloDataAdapterI.OP_READ_DATA;
      String title = appendData ? "Apollo: adding data" : "Apollo: load data";
      // Adding ApolloFrame first arg in an attempt 
      // to keep this window always on top
      ApolloAdapterChooser chooser = 
        new ApolloAdapterChooser(
          ApolloFrame.getFrame(),
          Config.getAdapterRegistry(),
          op,
          title,
          null, // input
          true // failfast
          );

      chooser.setBackground(Config.getDataLoaderBackgroundColor());
      chooser.setForeground(Config.getDataLoaderLabelColor());
      chooser.setLabelColor(Config.getDataLoaderTitleColor());

      String historyFile = Config.getAdapterHistoryFile();
          
      if (historyFile != null && historyFile.length() > 0)
        chooser.setPropertiesFile(new File(historyFile));
          
      chooser.show();
      chooser.invalidate();
      chooser.validate();

      // SUCCESS
      if (!chooser.isCancelled() && !chooser.isFailure()) {
        DataAdapter adapter = chooser.getDataAdapter();
            
        if (adapter instanceof ApolloDataAdapterI) {
          data_adapter = (ApolloDataAdapterI) adapter;
        }
        return_value = chooser.getOutput();
      }
      // FAILURE
      else if (chooser.isFailure()) {
        org.bdgp.io.DataAdapterException ex = chooser.getException();
        String message = ex.getMessage();
        JOptionPane msg = new JOptionPane();
        logger.error(message);
        msg.showMessageDialog(
          ApolloFrame.getFrame(),
          message,
          "Read error",
          JOptionPane.WARNING_MESSAGE);

        return_value = null;
      } 
      // CANCEL
      else if (chooser.isCancelled()) {
            
        if (quitOnCancel) {
          logger.info ("nothing loaded so quitting");
          System.exit(0);
        }
        return_value = null;
            
      }

      // This dispose is critical. Without it the DataAdapterChooser
      // lingers as its a top level window(JDialog) and it keeps
      // a reference to the curation set - major mem leak.
      chooser.dispose();
      chooser = null;
    }
  } // end of DataChooserThread inner class


  public boolean putCurationSet (ApolloDataAdapterI datasource,
                                 CurationSet curation_set){
    try {
      datasource.commitChanges(curation_set);
      Config.getController().setAnnotationChanged (false);
      return true;
      
    } catch (org.bdgp.io.DataAdapterException ex) {
      logger.error(ex.getMessage(), ex);
      JOptionPane.showMessageDialog(
        null,
        "Could not save because of " +
        ex.toString()
      );
      return false;
      
    }
  }

  public boolean saveCompositeDataHolder(ApolloDataAdapterI datasource,
                                         CompositeDataHolder cdh) {
    try {
      datasource.commitChanges(cdh);
      Config.getController().setAnnotationChanged (false);
      return true;
    } catch (org.bdgp.io.DataAdapterException ex) {
      logger.error(ex.getMessage(), ex);
      JOptionPane.showMessageDialog(
        null,
        "Could not save because of " +
        ex.toString()
      );
      return false;
    }
  }

  /** This should have the simgle species cdh or cur set passed in */
  //public void getSequence() throws apollo.dataadapter.ApolloAdapterException {
  public void getSequence(CompositeDataHolder compositeDataHolder) 
    throws apollo.dataadapter.ApolloAdapterException {
    if (compositeDataHolder.isMultiSpecies()) 
      return; // for now
    CurationSet curation_set = compositeDataHolder.getCurationSet(0);
    if (data_adapter != null && curation_set != null) {
      SequenceI seq = curation_set.getRefSequence();
      if (seq == null || seq.getLength() == 0) {
        try {
          DbXref    dbx = new DbXref(curation_set.getName(),
                                     curation_set.getName(),
                                     curation_set.getName());
          logger.info("trying to get sequence for region " +
                      dbx.getIdValue());
          seq = data_adapter.getSequence(dbx);
          curation_set.setRefSequence(seq);
        } catch (NotImplementedException e) {
          logger.warn("Didn't set sequence because methods were "+
                      "not implemented", e);
          seq = null;
        }
      }
      if (seq != null && seq.getLength() > 0) {
        if (curation_set.getResults() == null)
          logger.warn("No results!");
        else
          curation_set.getResults().setRefSequence(seq);

        if (curation_set.getAnnots() == null)
          logger.warn("No curated annotations!");
        else
          curation_set.getAnnots().setRefSequence(seq);
      }
    }
  }
  
  public ApolloDataAdapterI getDataAdapter(){
    return data_adapter;
  }//end getDataAdapter

  /** Layer data onto curationset (if multi species active species cur set) */
  public void addToCurationSet(/*ApolloLoaderI frame*/) throws apollo.dataadapter.ApolloAdapterException {
    try {
      
      DataAdapterRegistry registry = Config.getAdapterRegistry();
      DataAdapter [] loaders = registry.getAdapters(ApolloDataAdapterI.OP_APPEND_DATA, false);
      //      CurationSet set = frame.getCurationSet();
      // set curation set for all append data adapters
      for (int i = 0; i < loaders.length; i++) {
        ApolloDataAdapterI adapter = (ApolloDataAdapterI) loaders[i];
        //adapter.setCuration (frame.getCurationSet());
        CurationManager cm = CurationManager.getCurationManager();
        adapter.setCuration(cm.getActiveCurState().getCurationSet());
      }
      
      addData();//frame);
      
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new apollo.dataadapter.ApolloAdapterException(e.getMessage(), e);
    }
  }

  /** Helper function for addToCurationSet for adding data to cur set */
  private boolean addData() throws apollo.dataadapter.ApolloAdapterException {
    
    boolean out = false;

    //    long lasttime = System.currentTimeMillis();

    // Why is this a separate thread - is it so you can still interact with
    // the gui while the chooser is up?
    // Steve Searle in cvs comment of 8.23.01 wrote:
    //  Put DataChooser code into a Runnable to run on the AWTEvent dispatch
    // thread. I don't think this should be necessary but Exceptions
    // were being thrown on Alphas.
    boolean exitOnCancel = false;
    boolean appendData = true;
    Runnable chooserRun = new DataChooserThread(exitOnCancel,appendData);
//       new Runnable() {
//         public void run() {
          
//           // Adding ApolloFrame first arg in an attempt 
//           // to keep this window always on top
//           ApolloAdapterChooser chooser = 
//             new ApolloAdapterChooser(
//               ApolloFrame.getFrame(),
//               Config.getAdapterRegistry(),
//               ApolloDataAdapterI.OP_APPEND_DATA,
//               "Apollo: adding data",
//               null,
//               // last arg is "failfast"
//               true
//             );
          
//           chooser.setBackground(Config.getDataLoaderBackgroundColor());
//           chooser.setForeground(Config.getDataLoaderLabelColor());

//           String historyFile = Config.getAdapterHistoryFile();
          
//           if (historyFile != null && historyFile.length() > 0)
//             chooser.setPropertiesFile(new File(historyFile));
          
//           chooser.show();
//           chooser.invalidate();
//           chooser.validate();
          
//           // DataAdapterChooser ignores setLocation, for some reason
//           if (!chooser.isCancelled() && !chooser.isFailure()) {
            
//             DataAdapter adapter = chooser.getDataAdapter();
//             if (adapter instanceof ApolloDataAdapterI) {
//               data_adapter = (ApolloDataAdapterI) adapter;
//               //Config.setDataAdapterType(data_adapter.getClass().getName());
//             }
    // dont think cast to Boolean matters - return_value is an Object
//             return_value = (Boolean) chooser.getOutput(); 
            
//           }
//           else if (chooser.isFailure()) {
            
//             org.bdgp.io.DataAdapterException ex = chooser.getException();
//             String message = ex.getMessage();
//             JOptionPane msg = new JOptionPane();
//             //JFrame parent = (frame instanceof JFrame ?(JFrame) frame : null);
//             msg.showMessageDialog(ApolloFrame.getFrame(),
//                 message,
//                 "Read error",
//                 JOptionPane.WARNING_MESSAGE);
//             return_value = null;
            
//           }
//           else if (chooser.isCancelled()) {
            
//             return_value = null;
            
//           }

//           // This dispose is critical. Without it the DataAdapterChooser
//           // lingers as its a top level window(JDialog) and it keeps
//           // a reference to the curation set - major mem leak.
//           chooser.dispose();
//           chooser = null;
//         }
//       }; // end of Runnable
      
    chooserRun.run();
    if (return_value != null)
      out = ((Boolean) return_value).booleanValue();

    return_value = null;

    // Do gc here to stop initial pause on first scroll
    System.gc();
    return out;
  }

  public void saveFileDialog(ApolloDataI apolloData) { // CompositeDataHolder?
    String historyFile = Config.getAdapterHistoryFile();
    if(apolloData instanceof CompositeDataHolder){
      CompositeDataHolder cdh = (CompositeDataHolder) apolloData; 
      for (int i=0; i<cdh.numberOfSpecies(); i++) {
        SaveAdapterChooser chooser = new SaveAdapterChooser(cdh.getCurationSet(i));
        if (historyFile != null && historyFile.length() > 0)
          chooser.setPropertiesFile(new File(historyFile));
        chooser.show();
      }
    } else {
      SaveAdapterChooser chooser = new SaveAdapterChooser(apolloData);
      if (historyFile != null && historyFile.length() > 0)
        chooser.setPropertiesFile(new File(historyFile));
      chooser.show();
    }
    Config.getController().setAnnotationChanged (false);
  }


/** This nullifies choosers output reference, preventing a java1.3.1 ui mem leak
    dangling down to the curation set - i think this mem leak is fixed in 1.4
    and if so this class can be discarded, cant make inner class cos inner class
    keeps ref to outer class */
class ApolloAdapterChooser extends DataAdapterChooser {
  ApolloAdapterChooser(java.awt.Frame parent,
                       DataAdapterRegistry registry,
                       IOOperation op,
                       String title,
                       Object input,
                       boolean failfast) {
    super(parent, registry, op, title, input, failfast);
  }

  /** This nullifies choosers output reference, preventing a java1.3.1 
      ui mem leak dangling down to the curation set - i think this 
      mem leak is fixed in 1.4 and if so this class can be discarded, 
      this is also presumptious in that you can only call getOutput 
      once, if more calls are needed a nullifyOutput method would need 
      to be added and called. */
  public Object getOutput() {
    Object returnedOutput = output;
    output = null;
    return returnedOutput;
  }
  
  /**
   * An attempt at allowing the user to fix problems with the data they've already
   * entered, instead of having to re-type everything. Logic put here - to allow the
   * user to keep staring at the same GUI, to fix their problems. failed -> false after
   * the dialog to stop repucussions later in ApolloRunner.
  **/
  public void doCommit(){
    Apollo.setLog4JDiagnosticContext();
    try {
      super.doCommitWithExceptions();
    }catch(org.bdgp.io.DataAdapterException exception){
      logger.error(exception.getMessage());
      javax.swing.JOptionPane.showMessageDialog(this, exception.getMessage());
      failed = false;
    }
    Apollo.clearLog4JDiagnosticContext();
  }
}

  /** SaveAdapterChooser inner class: Nulls out input after commit as workaround
      for java popup mem leak */
  class SaveAdapterChooser extends DataAdapterChooser {
    SaveAdapterChooser(Object input) {
      super(Config.getAdapterRegistry(),ApolloDataAdapterI.OP_WRITE_DATA,
      //            "Save data",input,false);
            "Write data",input,false);
    }
    public void doCommit() {
      Apollo.setLog4JDiagnosticContext();
      // Check available memory before we try to save
      CheckMemoryThread cmt = new CheckMemoryThread(Config.getMemoryAllocation());
      cmt.checkFreeMemory();
      cmt.halt();
      cmt = null;

      super.doCommit();
      input = null; // mem leak workaround
      Apollo.clearLog4JDiagnosticContext();
    }
  }
}
