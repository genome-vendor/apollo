package apollo.dataadapter;

import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.io.File;

import org.bdgp.io.*;

import apollo.gui.GenericFileAdapterGUI;
import apollo.datamodel.ApolloDataI;
import apollo.datamodel.CurationSet;

public class SerialAdapterGUI extends GenericFileAdapterGUI {

  public SerialAdapterGUI(IOOperation op) {
    super(op);
  }

  public Object doOperation(Object values) throws ApolloAdapterException {
    ((SerialDiskAdapter) driver).
    setFilename(getSelectedPath());

    if (op.equals(ApolloDataAdapterI.OP_READ_DATA))
      //Config.newDataAdapter(driver); // to pick up new style? serial style??
      return ((ApolloDataAdapterI) driver).getCurationSet();
    else if (op.equals(ApolloDataAdapterI.OP_WRITE_DATA)) {
      ApolloDataI apolloData = (ApolloDataI)values;
      CurationSet curSet=null;
      if (apolloData.isCurationSet()) {
        curSet = apolloData.getCurationSet();
      } else if (apolloData.isCompositeDataHolder()) {
        // is it possible that a multi species could come in here and if
        // so should we be saving them all. I think serial adapter gui is
        // set up for one species, and there would be multiple adapters for multi
        // species - so i think we are ok. But I dont think serial adapter should
        // even recieve a compo data and have to get its cur set - it should
        // get just a CurationSet (or a niftier ApolloDataI)
        curSet = apolloData.getCompositeDataHolder().getCurationSet(0);
      }
      ((ApolloDataAdapterI) driver).commitChanges(curSet);
      return null;
    } else
      return null;
  }
}
