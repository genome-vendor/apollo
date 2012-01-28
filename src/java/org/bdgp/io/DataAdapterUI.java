package org.bdgp.io;

import java.util.*;
import org.bdgp.util.*;

/**
 * An abstract representation of a DataAdapter user interface. These interfaces
 * are meant to be used in conjunction with a some sort of data adapter
 * selection widget, like {link org.bdgp.swing.widget.DataAdapterChooser}
 */
public interface DataAdapterUI {
    /**
     * Sets the data adapter for this UI. Since the UI provides it's
     * own implementation of the IOOperation via the 
     * {link #doOperation(Object)}, the DataAdapterUI can decide whether or
     * not to actually use the DataAdapter to perform the operation. However,
     * a correct implementation of this interface will use the DataAdapter
     * given in setDataAdapter.
     */
    public void setDataAdapter(DataAdapter in);

    /**
     * Sets UI properties for this DataAdapterUI. The source of these
     * properties is up to the widget that displays the DataAdapterUI. Most of
     * the time these properties will contain some sort of history information
     * and will be loaded from a file. The Properties object may be null if
     * there are no properties to set.
     */
    public void setProperties(Properties in);

    /**
     * Returns the UI properties for this DataAdapterUI. These properties
     * may be recorded by the widget that displays the DataAdapterUI and
     * provided to the setDataAdapter method of later instances of the
     * DataAdapterUI. This method may return null if there are no properties
     * to record.
     */
    public Properties getProperties();

    /**
     * The method that actually performs the data adapter operation
     * (presumably by delegating to a data adapter). The input and outputs
     * to this method are entirely dependant on the operation the data adapter
     * is supposed to perform. The input or output of this method may be null.
     */
    public Object doOperation(Object values) throws DataAdapterException;

  
  /**
   * Set the input, if there is any. This input is the same as the Object
   * passed into doOperation. A DataAdapterUI may need to get a hold of
   * its input before doOperation time and this allows for it. For instance
   * if the UI wants to display input related stuff it needs it before doOp
   */
  public void setInput(Object input);
}
