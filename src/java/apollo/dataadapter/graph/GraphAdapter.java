package apollo.dataadapter.graph;

import apollo.dataadapter.*;
import apollo.datamodel.*;

import apollo.gui.synteny.CurationManager;
import apollo.gui.*;
import apollo.gui.genomemap.*;

import apollo.util.FileUtil;

import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;

import java.awt.Color;

import java.util.List;
import java.util.LinkedList;

import org.bdgp.io.IOOperation;
import org.bdgp.io.DataAdapterUI;

import org.apache.log4j.*;

/** This is the adapter class for handling graph data
 * 
 * @author elee
 *
 */

public class GraphAdapter extends AbstractApolloAdapter {

  //class variables
  protected final static Logger logger = LogManager.getLogger(GraphAdapter.class);

  //instance variables
  private String graphFilename;

  /** Constructor
   */
  public GraphAdapter()
  {
    setName("Graph data");
  }

  /** Get supported operations (append)
   * 
   * @return IOOperation[] - supported operations
   */
  public IOOperation[] getSupportedOperations()
  {
    return new IOOperation[] { ApolloDataAdapterI.OP_APPEND_DATA };
  }

  /** Get the UI for the data adapter
   * 
   * @param op - the IOOperation requested
   * @return the UI for the data adapter
   */
  public DataAdapterUI getUI(IOOperation op)
  {
    return new GraphAdapterGUI(op);
  }

  /** Add graph data
   * 
   * @return true if successful
   * @throws ApolloAdapterException if there is a problem parsing the data
   */
  public Boolean addToCurationSet(Color color) throws ApolloAdapterException
  {
    try {
      String ext = FileUtil.getExtension(getGraphFilename());
      List<ScoreCalculator> sc = new LinkedList<ScoreCalculator>();
      if (ext.equals("sgr")) {
        sc.add(new SgrScoreCalculator(getGraphFilename(), 100, curation_set));
      }
      else if (ext.equals("wig")) {
        BufferedReader br = new BufferedReader(new FileReader(getGraphFilename()));
        while (br.ready()) {
          sc.add(new WiggleScoreCalculator(br, 100, curation_set));
        }
      }
      for (ScoreCalculator s : sc) {
        if (s instanceof WiggleScoreCalculator) {
          WiggleScoreCalculator w = (WiggleScoreCalculator)s;
          if (w.getColor() != null) {
            color = w.getColor();
          }
        }
        CurationManager.getActiveCurationState().getSZAP().addGraph(s, true, color);
      }
    }
    catch (IOException e) {
      throw new ApolloAdapterException("Error parsing graph data: " + e.getMessage());
    }
    return true;
  }

  /** Get the filename for the graph data
   * 
   * @return the filename for the graph data
   */
  public String getGraphFilename()
  {
    return graphFilename;
  }

  /** Set the filename for the graph data
   * 
   * @param graphFilename - the filename for the graph data
   */
  public void setGraphFilename(String graphFilename)
  {
    this.graphFilename = graphFilename;
  }
}
