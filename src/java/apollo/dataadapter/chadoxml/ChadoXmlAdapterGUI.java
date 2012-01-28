package apollo.dataadapter.chadoxml;

import java.util.Properties;
import java.util.Vector;

import java.awt.BorderLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.apache.log4j.*;

import org.bdgp.io.IOOperation;
import org.bdgp.io.DataAdapter;
import apollo.datamodel.ApolloDataI;
import apollo.datamodel.CurationSet;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.chadoxml.ChadoXmlAdapter;
import apollo.gui.GenericFileAdapterGUI;

public class ChadoXmlAdapterGUI extends GenericFileAdapterGUI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ChadoXmlAdapterGUI.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private ChadoXmlAdapter driver;

  private JCheckBox saveAnnots = new JCheckBox("Save annotations");
  private JCheckBox saveResults = new JCheckBox("Save evidence (computational results)");

  ChadoXmlAdapterGUI() {
  }

  public void init() {}

  private void addCheckboxes() {
    JPanel checkboxPanel = new JPanel();
    checkboxPanel.setLayout(new BorderLayout());
    checkboxPanel.add("North", saveAnnots);
    checkboxPanel.add("South", saveResults);
    panel.add("South", checkboxPanel);
    saveAnnots.setEnabled(true);
    saveAnnots.setSelected(true);
    saveResults.setEnabled(true);
    saveResults.setSelected(true);
  }

  void setIOOperation(IOOperation op) {
    if (!(op.equals(this.op))) {
      this.op = op;
      removeAll();
      super.initGUI();
      super.buildGUI();
      super.attachListeners();
      if (this.op.equals(ApolloDataAdapterI.OP_WRITE_DATA))
        addCheckboxes();
    }
  }

//   public void setProperties(Properties in) {
//     super.setProperties(in);
//     if (ioOperation.equals(ApolloDataAdapterI.OP_WRITE_DATA))
//       addAnnotsCheckbox();
//   }

  public Object doOperation(Object values) throws ApolloAdapterException {
    driver.setInput(getSelectedPath());
    if (this.op.equals(ApolloDataAdapterI.OP_READ_DATA)) {
      logger.debug("Reading chado xml data from " + driver.getInput());
      return driver.getCurationSet();
    }
    else if (this.op.equals(ApolloDataAdapterI.OP_APPEND_DATA)) {
      logger.debug("Reading chado xml data from " + driver.getInput());
      return driver.addToCurationSet();
    }
    else if (this.op.equals(ApolloDataAdapterI.OP_WRITE_DATA)) {
      ApolloDataI apolloData = (ApolloDataI)values;
      CurationSet curSet=null;
      if (apolloData.isCurationSet()) {
        curSet = apolloData.getCurationSet();
      } else if (apolloData.isCompositeDataHolder()) {
        curSet = apolloData.getCompositeDataHolder().getCurationSet(0);
      }
      driver.commitChanges(curSet, saveAnnots.isSelected(), saveResults.isSelected());
      return null;
    } else {
      logger.error("doOperation: unknown operation " + this.op);
      return null;
    }
  }
  
  public void setDataAdapter(DataAdapter driver) {
    super.setDataAdapter(driver); // not sure if this is needed
    if (driver instanceof ChadoXmlAdapter) {
      this.driver = ((ChadoXmlAdapter)driver);
      logger.debug("Set data adapter to " + driver);
    } else {
      logger.error("ChadoXmlAdapterGUI not compatible with adapter " + driver + " ( class " + driver.getClass().getName() + ")");
    }
  }
}
