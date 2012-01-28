/*********
 * SerialDiskAdapter
 *
 * Reads data from a serialized java class.  Used for autosaving backups
 * of Apollo data and allowing user to restore from backup.
 *
 */

package apollo.dataadapter;

import apollo.datamodel.*;
import apollo.config.Config;

import java.util.*;
import java.io.*;

import org.apache.log4j.*;

import org.bdgp.io.ProgressableFileInputStream;
import org.bdgp.io.AbstractDataAdapter;
import org.bdgp.io.IOOperation;
import org.bdgp.io.DataAdapterUI;
import org.bdgp.util.*;
import java.util.Properties;

public class SerialDiskAdapter extends AbstractApolloAdapter {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(SerialDiskAdapter.class);

  String filename;

  IOOperation [] supportedOperations = {
                                         ApolloDataAdapterI.OP_READ_DATA,
                                         ApolloDataAdapterI.OP_WRITE_DATA,
                                         ApolloDataAdapterI.OP_READ_SEQUENCE
                                       };

  public SerialDiskAdapter() {
    setName("Apollo backup file");
  }

  public void init() {}

  public String getType() {
    return "Choose backup file";
  }

  public DataInputType getInputType() {
    return DataInputType.FILE;
  } // ??
  public String getInput() {
    return filename;
  }

  public IOOperation [] getSupportedOperations() {
    return supportedOperations;
  }

  public DataAdapterUI getUI(IOOperation op) {
    if (op.equals(ApolloDataAdapterI.OP_READ_DATA) ||
        op.equals(ApolloDataAdapterI.OP_WRITE_DATA))
      return new SerialAdapterGUI(op);
    else
      return null;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  protected String getFilename() {
    if (filename == null)
      filename = super.getFilename();
    return filename;
  }

  public SerialDiskAdapter(String filename) {
    this.filename = filename;
  }

  public Properties getStateInformation() {
    Properties props = new Properties();
    props.put("filename", filename);
    return props;
  }

  public void setStateInformation(Properties props) {
    filename = props.getProperty("filename");
  }

  public void commitChanges(CurationSet curationSet) throws ApolloAdapterException {
    commitChanges(curationSet,filename);
  }

  private void commitChanges(CurationSet curationSet,String filename) 
    throws ApolloAdapterException {
    try {
      FileOutputStream istream 
        = new FileOutputStream(apollo.util.IOUtil.findFile(getFilename(), true));
      ObjectOutputStream p = new ObjectOutputStream(istream);
      p.writeObject(curationSet);
      // AnnotChangeLog replaced by TransactionManager which is part of CurSet now
      //if (curationSet.hasTransactions())
      //p.writeObject(curationSet.getAnnotationChangeLog());
    } catch (UTFDataFormatException e) {
      logger.error("SerialDiskAdapter.commitChanges() broken string = "+e.getMessage());
      throw new ApolloAdapterException(e);
    }
    catch (Exception e) {
      throw new ApolloAdapterException(e);
    }
  }

  /** If multi cur set, appends curset name to filename - or should it appends the
      cur set # so its retrievable? */
  public void commitChanges(CompositeDataHolder cdh) throws ApolloAdapterException {
    if (!cdh.isMultiSpecies())
      commitChanges(cdh.getCurationSet(0));
    else {
      for (int i=0; i<cdh.getNumberOfSpecies(); i++) 
        // append curset/species name to file name
        commitChanges(cdh.getCurationSet(i),filename+cdh.getSpecies(i));
    }
  }

  /** What about getCompositeDataHolder? */
  public CurationSet getCurationSet() throws ApolloAdapterException {
    try {
      ProgressableFileInputStream istream =
        new ProgressableFileInputStream(apollo.util.IOUtil.findFile(getFilename(), false));
      super.clearOldData();
      ProgressListener pl = new ProgressListener() {
                              public void progressMade(ProgressEvent e) {
                                fireProgressEvent(e);
                              }
                            };
      istream.addProgressListener(pl);
      ObjectInputStream p = new ObjectInputStream(istream);
      //super.notifyLoadingDone();
      //return (CurationSet) p.readObject();
      // Reading follows the order of writing in commitChanges
      CurationSet cs = (CurationSet) p.readObject();
      //AnnotationChangeLog acl = (AnnotationChangeLog) p.readObject();
      // yuck - ACL has nothing to do with config - change this
      //Config.setAnnotationChangeLog(acl); 
      // AnnotationChangeLog.setAnnotationChangeLog(acl); //??
      // I think there shouldnt be a public ACL setter, only ACL can change
      // the singleton instance - thus the deserialize method.
      //AnnotationChangeLog.deserializeAnnotationChangeLog(p); 
      //AnnotationChangeLog a = (AnnotationChangeLog)p.readObject();
      // or should it deserialize and set some sort of TransactionList?
      //cs.setAnnotationChangeLog(a);
      // cur set has transaction manager now!

      return cs;
    } catch (Exception e) {
      //super.notifyLoadingDone();
      logger.error("SerialDiskAdapter.getCurationSet() deserialization exception", e);
      throw new ApolloAdapterException(e);
    }
  }

  public void setRegion(String extId) throws ApolloAdapterException {
    // region is ignored
  }

}
