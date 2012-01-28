package apollo.analysis;

import apollo.datamodel.*;

import org.bdgp.io.*;

import java.util.*;

/**
 * DataAdapterI interface
 *
 * Used to load annotated and non-annotated data from
 * whatever datasource
 */
public interface AnalysisDataAdapterI extends VisualDataAdapter {

  public static final IOOperation OP_ANALYZE_DATA =
    new IOOperation("Analyze data");

}
