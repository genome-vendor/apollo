package apollo.dataadapter.gamexml;

/** This is the adapter GUI for reading generic (non-fly) GAME XML files.
 *  It lets you specify a file or URL only.
 *  The FlyGAMEAdapterGUI has the tabbed panels to allow you to fetch fly
 *  data from the server by gene/cytology/etc.
 *  Although we'll probably disable even that when the Indiana server switches
 *  over completely to ChadoXML. */

import java.util.Properties;
import java.util.Vector;

import java.awt.BorderLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.apache.log4j.*;
import org.bdgp.io.IOOperation;
import org.bdgp.io.DataAdapter;
import org.bdgp.io.DataAdapter;
import apollo.datamodel.ApolloDataI;
import apollo.datamodel.CurationSet;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.DataInput;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.chadoxml.ChadoXmlAdapter;
import apollo.gui.GenericFileAdapterGUI;

public class GAMEAdapterGUI extends GenericFileAdapterGUI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(GAMEAdapterGUI.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private GAMEAdapter driver;

  private JCheckBox saveAnnots = new JCheckBox("Save annotations");
  private JCheckBox saveResults = new JCheckBox("Save evidence (computational results)");

  public GAMEAdapterGUI() {
  }

  public GAMEAdapterGUI(IOOperation op) {
    //super(op);
    setIOOperation(op);
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

  /** This is used only by TestApollo at the moment */
  public void setCurrentInput(String file) {
    driver.setInput(file);
  }

  /** need? */
  public DataInputType getInputType() {
    logger.info("GAMEAdapterGUI.getInputType: input = " + getSelectedPath());
    if (getSelectedPath().startsWith("http"))
      return DataInputType.URL;
    else
      return DataInputType.FILE;
  }

//   public void setProperties(Properties in) {
//     super.setProperties(in);
//     if (ioOperation.equals(ApolloDataAdapterI.OP_WRITE_DATA))
//       addAnnotsCheckbox();
//   }

//   /** public for testing synteny - tied in w gui unfortunately 
//    this isnt just for tetsting synteny - used to get input - crucial!
//    copied from flygameadapterGui - i have no idea why its not here???? */
//   public GuiTabPanel getCurrentGamePanel() {
//     return gamePanels[tabbedPane.getSelectedIndex()];
//   }
//   /** copied from flygameadapterGui - i have no idea why its not here???? */
//   private DataInput getDataInput() throws ApolloAdapterException {
//     try {
//       return getCurrentGamePanel().getDataInput();
//     }
//     catch (RuntimeException e) { throw new ApolloAdapterException(e.getMessage()); }
//   }

  public Object doOperation(Object values) throws ApolloAdapterException {
//     if (getDataInput()==null || !getDataInput().hasInput())
//       throw new ApolloAdapterException("null input");
//     driver.setDataInput(getDataInput());
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
    if (driver instanceof GAMEAdapter) {
      this.driver = ((GAMEAdapter)driver);
      logger.debug("Set data adapter to " + driver);
    } else {
      logger.error("GAMEAdapterGUI not compatible with adapter " + driver + " ( class " + driver.getClass().getName() + ")");
    }
  }
}
