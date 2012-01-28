package org.bdgp.swing;

import javax.swing.JPanel;
import org.bdgp.io.DataAdapter;
import org.bdgp.io.DataAdapterUI;

public abstract class AbstractDataAdapterUI extends JPanel
  implements DataAdapterUI {

  protected DataAdapter driver;

  public void setDataAdapter(DataAdapter driver) {
    this.driver = driver;
  }

  /** setInput from DataAdapterUI is a no-op here. If a subclass actually
   * wants to use the Input object it just needs to override this.
   */
  public void setInput(Object input) {}

}
