package apollo.gui;

import apollo.datamodel.CompositeDataHolder;
import apollo.dataadapter.*;
import apollo.datamodel.CurationSet;
import apollo.main.Apollo;
import apollo.gui.synteny.CurationManager;

import org.apache.log4j.*;

public class AutoSaveThread extends Thread {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(AutoSaveThread.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  //private CurationSet data;
  //private ApolloFrame apolloFrame;
  private String autosaveFile;
  private boolean halt;
  private long saveInterval;
  private ApolloDataAdapterI adapter;

  public AutoSaveThread(String autosaveFile, int saveIntervalMinutes) {
    //apolloFrame = af;
    // It then proceedes to ignore autosaveFile, but that's ok, I changed ApolloFrame to use it
    // when it calls SerialDiskAdapter.
    this.autosaveFile = autosaveFile;
    this.saveInterval = 60*1000*saveIntervalMinutes;
    setDaemon(true);
  }

  public void setDataAdapter(ApolloDataAdapterI adapter) {
    this.adapter = adapter;
  }

  public void halt() {
    halt = true;
    interrupt();
  }

  /** Queries CurationManager for active curation set. For single cur set this
      is the only cur set. for multi curset this is the last one se*/
  public void run() {
    Apollo.setLog4JDiagnosticContext();
    while(!halt) {
      try {
        sleep(saveInterval);
        logger.info("autosaving to " + autosaveFile);
        /*
        ApolloDataAdapterI saver = (ApolloDataAdapterI)
            AdapterRegistry.
            getSerialDiskAdapter(null, autosaveFile);
        */
        //CurationSet curationSet = apolloFrame.getCurationSet();
        CompositeDataHolder cdh = 
          CurationManager.getCurationManager().getCompositeDataHolder();
        if (cdh != null)
          adapter.commitChanges(cdh);
        logger.info("autosave done");
      } catch (InterruptedException e) {}
      catch (ApolloAdapterException e) {
        Exception realException = 
          (Exception)apollo.dataadapter.ApolloAdapterException.getRealException(e);
        logger.error("encountered exception while trying to autosave: "+realException.toString(), realException);
        realException.printStackTrace();
      }
    }
    Apollo.clearLog4JDiagnosticContext();
  }
}
