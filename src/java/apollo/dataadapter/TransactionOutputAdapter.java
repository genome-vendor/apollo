/*
 * Created on Oct 8, 2004
 *
 */
package apollo.dataadapter;

import java.util.List;

import apollo.editor.TransactionManager;

/**
 * A class used to handle Transaction objects output.
 * @author wgm
 */
public abstract class TransactionOutputAdapter {

  protected TransactionTransformer transformer;
  // target: it can be a database target, file system, or a url
  protected Object target;
  // For map position
  protected String mapID;
  protected String mapType; // chromosome or chromosome_arm
  
  /**
   * @return Returns the mapID.
   */
  public String getMapID() {
    return mapID;
  }

  /**
   * Set the id for map_position. Usually it should be a name of a chromosome
   * or chromosome_arm
   * @param mapID The mapID to set.
   */
  public void setMapID(String mapID) {
    this.mapID = mapID;
  }
  
  public String getMapType() {
    return mapType;
  }
  
  /**
   * Set the type used for map_position.
   * @param type The type to set.
   */
  public void setMapType(String type) {
    this.mapType = type;
  }
  
  /**
   * Set the TransactionTransformer to be used to transform Apollo Transaction objects.
   * @param transformer
   */
  public void setTransformer(TransactionTransformer transformer) {
    this.transformer = transformer;
  }
  
  public TransactionTransformer getTransformer() {
    return this.transformer;
  }

  /**
   * Set the output target for Transaction objects. A target can be a database connection,
   * a file name, or a URL.
   * @param target
   */
  public void setTarget(Object target) {
    this.target = target;
  }
  
  public Object getTarget() {
    return this.target;
  }
  
  /**
   * The client to this class should call this method to commit (save or write
   * changes to the database) transactions.
   * @param transctions
   */
  //public void commit(List apolloTransactions) throws Exception {
  public void commitTransactions(TransactionManager transManager) throws Exception {
    // Mark sure the target is not null
    if (target == null)
      throw new IllegalStateException("TransctionOutputAdapter.commit(): no target specified!");
    List transformedTns = transManager.getTransactions();
    if (transformer != null) {
      // throws TransactionTransformException
      try {
        transformedTns = transformer.transform(transManager);
      } catch (TransactionTransformException e) {
        System.out.println("\nTransforming of transactions has failed.");
        throw e;
      }
    }
    commitTransformedTransactions(transformedTns);
  }
  
  /**
   * A subclass to this class should implement this method to provide its own behaviors.
   * @param transformedTn a list of Transaction objects transformed by transactions.
   * These are no longer apollo.editor.Transactions, but transactions that are specific
   * to the data adapter (eg ChadoTransactions)
   I would argue this should be a HASA just like the transaction transformer. so
   the transaction output adapter is just a generic object that gets a transformer &
   a writer and just calls both - its the old HASA vs ISA i guess.
   */
  protected abstract void commitTransformedTransactions(List transformedTn) throws Exception;
}
