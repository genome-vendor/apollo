package apollo.gui;

import java.util.Vector;

import apollo.datamodel.CurationSet;
import apollo.dataadapter.ApolloDataAdapterI;

/**
   Interface for data loader to layer data on to curation set?
 */

public interface ApolloLoaderI {
  public void   setAdapter(ApolloDataAdapterI data_adapter);
  public Vector getHistory();
  public CurationSet getCurationSet();
}
