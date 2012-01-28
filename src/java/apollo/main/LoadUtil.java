package apollo.main;

import apollo.config.Config;
import apollo.editor.ResultChangeEvent;
import apollo.gui.*;
import apollo.gui.event.*;
import apollo.gui.synteny.CompositeApolloFrame;
import apollo.gui.synteny.GuiCurationState;
import apollo.gui.synteny.CurationManager;
import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.util.*;
import apollo.gui.genomemap.StrandedZoomableApolloPanel;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.*;

import org.apache.log4j.*;

import org.bdgp.io.*;
import org.bdgp.swing.widget.*;
import java.util.Properties;


/** LoadUtil calls DataLoader to do loading, puts up progress, catches exceptions
    and puts up popups, and adds data. methods are static. 
    merge or groove with ApolloRunner somehow? both are used to load data - both
    call DataLoader
*/
public class LoadUtil {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(LoadUtil.class);

  private static final String loadUtilSource = "LOAD_UTIL";

  private static Object[] load_options = { "Load anyway",
                                           "Save first, then load",
                                           "Cancel" };

  // it would be nice if we just converted args to props before calling and not
  // have this version of the method
  public static void loadWithProgress(final ApolloFrame frame,
                                      final String [] args,
                                      final boolean new_curation) {
    loadWithProgress(frame,true,args,null,new_curation);
  }

  public static void loadWithProgress(final ApolloFrame frame,
                                      final Properties props,
                                      final boolean new_curation) {
    loadWithProgress(frame,false,null,props,new_curation);
  }

  private static boolean finished = false;
  
  private static void loadWithProgress(final ApolloFrame frame,
                                       final boolean isArgs,
                                       final String[] args,
                                       final Properties props,
                                       final boolean new_curation) {
                                         
    final DataLoader loader = new DataLoader();
    
    if (new_curation) {
      if(!confirmSaved(load_options, loader)) {
        return;
      }
    }
    
    final ProgressFrame pf = new ProgressFrame(frame,"Load progress");

    pf.setVisible(false);

    finished = false;
    
    apollo.gui.SwingWorker worker = new apollo.gui.SwingWorker() {
      public Object construct() {
        Vector results = new Vector(1);
        //        CurationSet curationSet = null;
        CompositeDataHolder compDataHolder = null;

        try {
          if (!new_curation && isArgs) {
            // layer data onto active species curation set
            loader.addToCurationSet();
            // do we have to set compDataHolder for layover? who cares right?
            //cs = frame.getCurationSet(); //CurationSet implements ApolloDataI
            // or should we be getting the active species cur set?
            compDataHolder = getCurationManager().getCompositeDataHolder();
          }
          else if (isArgs) {
            compDataHolder = loader.getCompositeDataHolder(args,pf.getListener());
          }
          else {
            compDataHolder = loader.getCompositeDataHolder(props,pf.getListener());
          }
          // Species should take care of this now
        //if(loader.getDataAdapter()!=null)frame.setAdapter(loader.getDataAdapter());


        } catch (Exception ex) {
          if(ex instanceof org.bdgp.io.DataAdapterException){
            logger.error(ex.getMessage(), ex);
            JOptionPane.showMessageDialog(
              frame,
              "This region couldn't be loaded because of the following problem: \n"+
              ex.getMessage(),
              "Warning",
              JOptionPane.WARNING_MESSAGE
            );
          }
          else{
            logger.error(ex.getMessage(), ex);
            JOptionPane.showMessageDialog(
              frame,
              "Apollo has experienced an unexpected problem: \n"+
              ex.getMessage(),
              "Warning",
              JOptionPane.WARNING_MESSAGE
            );
          }
          logger.error(ex.getMessage(), ex);
        }

        if (compDataHolder != null) 
          results.addElement(compDataHolder);

        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        logger.debug("loadWithProgress.construct (new_curation = " + new_curation +"): returning " + results.size() + " results");
        return results;
      }
      
      public void finished() {
        if (get() != null && ((Vector) get()).size() > 0) {
          CompositeDataHolder compDataHolder = (CompositeDataHolder)((Vector) get()).elementAt (0);
          try {
            doLoad(frame, loader, compDataHolder, new_curation);
          } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            showFailDialog();
          }
        } // moved to here
        
        pf.dispose();
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        frame.invalidate(); // force cursor to be redrawn
        finished = true;
      }
    
      private void showFailDialog() {
        JOptionPane.showMessageDialog(
          frame,
          "Couldn't load region",
          "Warning",
          JOptionPane.WARNING_MESSAGE
        );
      }
    }; //end of swingworker definition
      
    worker.start();
  }

  /** Fires DataLoadEvent - should this be moved to AbstractApolloAdapter and take the
      place of notifyNewData?
      Loads new curation set into gui(frame.loadData) */
  private static void doLoad(ApolloFrame frame,
                             DataLoader loader,
                             CompositeDataHolder compDataHolder,
                             boolean new_curation) 
    throws org.bdgp.io.DataAdapterException {

//     if (new_curation) {
//       frame.clearData(); // now done by DataLoadEvents
//     }

    if (compDataHolder != null) {
      // If the refsequence for the curation set has not been set by
      // the dataadapter then this will take care of it.  This will
      // also make sure that all of the results and annotations have
      // this same reference sequence.

      if (new_curation) {
        //loader.getSequence(); DataLoader now does this

        // temp - eventually CAF will be merged with AF
        // this could happen via the above region change event as well? 
        CompositeApolloFrame caf = (CompositeApolloFrame)frame;
	logger.trace("calling loadData on CompositeApolloFrame for new_curation");
        caf.loadData(loader.getDataAdapter(),compDataHolder);

        // this should probably go to species controller?
        Config.getController().setAnnotationChanged(false);

      } // end of if (new_curation)

      // Not new_curation. layover. send out result change events for ResultView
      else { //if (apolloData.isCurationSet()) { // only do for single species?
        fireResultChangeEvents();//frame);
      }
    }
    
    logger.debug("LoadUtil load done totalMemory: " + java.lang.Runtime.getRuntime().totalMemory());
    logger.debug("LoadUtil.load: garbage collecting...");
    System.gc(); // clean up previous data set
    // There was also a System.gc() call in AnalysisAdapter.addToCurationSet, which I commented out
    logger.debug("Post LoadUtil Garbage collection totalMemory = " + java.lang.Runtime.getRuntime().totalMemory());
    frame.getGlassPane().setVisible(false);
    return;
  }

  /** fire at active species for now. repair and redraw results.
      the load is an append/layover - add in new results */
  private static boolean fireResultChangeEvents() {//ApolloFrame frame){
    GuiCurationState curationState = getCurationManager().getActiveCurState();
    StrandedZoomableApolloPanel szap = curationState.getSZAP();

    // Force a redraw by calling szap.setFeatureSet.  Maybe this is overkill, but it WORKS,
    // which the ResultChangeEvent throwing did not.
    szap.setFeatureSet(szap.getCurationSet());
    // Also redraw annotations, in case layered data included annotations.
    szap.setAnnotations(szap.getCurationSet());
    szap.putVerticalScrollbarsAtStart();

    return true;
  }

  private static void fireResultChangeEvent(ResultChangeEvent evt) {
    Config.getController().handleResultChangeEvent(evt);
  }

  private static CurationManager getCurationManager() { 
    return CurationManager.getCurationManager(); 
  }

  /** should this be done on a per curationState basis? */
  public static boolean confirmSaved (Object[] options, DataLoader loader) {
    //Needs to confirm saving if the annotation has changed and the transaction stack is not empty
    boolean okay = !Controller.getMasterController().isAnnotationChanged() ||
      CurationManager.getActiveCurationState().getTransactionManager().numberOfTransactions() == 0;
    boolean rollbackResult;
    ApolloDataAdapterI adap = getCurationManager().getDataAdapter();
    CompositeDataHolder cdh = getCurationManager().getCompositeDataHolder();

    if (!okay) {
      JOptionPane pane
      = new JOptionPane ("Annotations are changed",
                         JOptionPane.QUESTION_MESSAGE,
                         JOptionPane.YES_NO_CANCEL_OPTION,
                         null, // icon
                         options, // all choices
                         options[1]); // initial value
      JDialog dialog = pane.createDialog(ApolloFrame.getFrame(),"Please Confirm");
      // This colors the perimeter but not the inside, which stays gray.
      // Better to have it all gray, because it looks weird this way.
      //       pane.setBackground (Config.getAnnotationBackground());
      //       dialog.setBackground (Config.getAnnotationBackground());
      dialog.show();
      Object result = pane.getValue();
      if (result != null) {
        //If there is an array of option buttons:
        if (options[0].equals(result)){
          okay = true;
	  // This seems to happen when user made some changes, 
	  // tried to load another dataset,
	  // the load failed, and now we've asked again "Save your changes?" 
	  // and the user said no but we can't get the adapter.
	  // LoadUtil was setting adapter to null on failed loads - 
	  // doesnt do this anymore - DataLoader only sets adapter 
	  // if it gets a valid one - MG
	  // if (getAdapter() == null) { return true; }
          // Annotations are willfully discarded. However, the user
          // may still have outstanding locks. We message the adapter 
	  // to roll the locks back.
          //rollbackResult = getAdapter().rollbackAnnotations(getCurationSet());
          rollbackResult = adap.rollbackAnnotations(cdh);
          if(rollbackResult = false){
            JOptionPane.showConfirmDialog(ApolloFrame.getFrame(), "Attempted to roll back your annotation locks and failed");
          }//end 
        }else if (options[1].equals(result)){
          // save it first and then proceed
          okay = loader.saveCompositeDataHolder(adap,cdh);
        }
      }
    }else{
      //
      // No annotations were made - so movement should be possible. 
      // However, the user may still have outstanding locks. 
      // We message the adapter to roll the locks back.
      //if (getAdapter() != null) {
      //rollbackResult = getAdapter().rollbackAnnotations(getCurationSet());
      rollbackResult = adap.rollbackAnnotations(cdh);
      if(rollbackResult = false){
        JOptionPane.showConfirmDialog(ApolloFrame.getFrame(), "Attempted to roll back your annotation locks and failed");
      }
    }//end if
    return okay;
  }


  /** Puts up a dialog asking user the question provided (which usually
      is some variant on "Are you sure?").  Returns their answer (true
      or false). If they don't answer, it returns false. */
  public static boolean areYouSure(String question) {
    if (question == null || question.equals(""))
      question = "Are you sure?";

    JOptionPane pane
      = new JOptionPane (question,
                         JOptionPane.QUESTION_MESSAGE,
                         JOptionPane.YES_NO_OPTION);
    JDialog dialog = pane.createDialog(ApolloFrame.getFrame(), "Please Confirm");
    dialog.show();
    Object result = pane.getValue();
    if (result != null) {
      if (result instanceof Integer) {
        int answer = ((Integer)result).intValue();
        if (answer == JOptionPane.YES_OPTION)
          return true;
      }
    }
    return false;
  }
}
