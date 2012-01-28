package apollo.dataadapter.chado.jdbc;

import apollo.datamodel.SequenceI;
import apollo.datamodel.seq.AbstractLazySequence;
//import apollo.gui.Controller; // yuck -> move to apollo.controller? refactor!

class ChadoLazySequence extends AbstractLazySequence {

  private transient JdbcChadoAdapter adapter;

  /** should this work off feature_id or uniquename - probably uniquename if we want it
      to work with game as well */
  ChadoLazySequence(String uniquename,int length,JdbcChadoAdapter adap) {
    // first string (id+"") becomes seq name
    super(uniquename,null); // null controller - doesnt actually use one passed in - refactor!
    setLength(length);
    adapter = adap;
  }

  private String getUniqueName() { return super.getName(); }

  protected String getResiduesFromSourceImpl(int low, int high) {
    SequenceI seq = adapter.getResiduesSubstring(getUniqueName(),low,high);
    return seq.getResidues();
  }

  /** this actually isnt used in apollo - take out of SeqI? */
  public SequenceI getSubSequence(int start, int end) {
    return null;
  }
}
