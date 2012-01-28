package apollo.editor;

import apollo.datamodel.Comment;
import apollo.datamodel.RangeI;
import apollo.datamodel.SeqFeatureI;

/** Update events have a lot of information associated with them. This interface
    captures that. 
    Should this be generalized to EventDetailI for all events?
*/

public interface UpdateDetailsI {
  /** isMove means parent is updated, ie isParent */
  public boolean isMove();
  public SeqFeatureI getOldParent();
  public boolean isRangeUpdate();
  public RangeI getOldRange();
  public boolean isStringUpdate();
  public String getOldString();
  public boolean isIntUpdate();
  public int getOldInt();

  public boolean isCommentUpdate();
  public Comment getOldComment();

  // public SeqEdit getOldSeqEdit();
  
  /** More details that are needed */
  public int getSubpartRank();
  public int getNewSequencingErrorPosition();
}
