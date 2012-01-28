package apollo.test;

import apollo.config.Config;
import apollo.gui.*;
import apollo.datamodel.*;
import apollo.dataadapter.*;

import org.bdgp.swing.widget.DataAdapterChooser;

import java.io.*;
import java.util.*;
import apollo.main.*;

public class ApolloLaundry implements ApolloLoaderI {
  CurationSet curation;
  ApolloDataAdapterI load_adapter;
  ApolloDataAdapterI save_adapter = null;
  private Vector history = new Vector();

  public void setCurationSet (CurationSet curation) {
    this.curation = curation;
  }

  public CurationSet getCurationSet() {
    return this.curation;
  }

  public void setAdapter (ApolloDataAdapterI data_adapter) {
    this.load_adapter = data_adapter;
  }

  public Vector getHistory() {
    return history;
  }

  public static void main(String[] args) {
    ApolloLaundry wash = new ApolloLaundry();
    DataLoader loader = new DataLoader();

    CurationSet main_curation = null;
    boolean more = false;
    try {
      //ApolloDataI apolloData = loader.getApolloData(wash, args, null);
      CompositeDataHolder cdh = loader.getCompositeDataHolder(args, null);
      main_curation = cdh.getCurationSet(0);
      wash.setCurationSet (main_curation);
    } catch (Exception e) {}

    more = (main_curation != null);

    while (more) {
      DataAdapterChooser chooser
      = new DataAdapterChooser(Config.getAdapterRegistry
                               (),
                               ApolloDataAdapterI.OP_WRITE_DATA,
                               "Save data",
                               wash.getCurationSet(),
                               false);
      String historyFile = Config.getAdapterHistoryFile();
      if (historyFile != null && historyFile.length() > 0)
        chooser.setPropertiesFile(new File(historyFile));
      chooser.show();

      main_curation = null;
      try {
        CompositeDataHolder cdh = loader.getCompositeDataHolder(new String [0],null);
        main_curation = cdh.getCurationSet(0);
        wash.setCurationSet (main_curation);
      } catch (Exception e) {
        more = false;
      }

      more &= (main_curation != null);

    }
    System.exit (0);
  }

}

