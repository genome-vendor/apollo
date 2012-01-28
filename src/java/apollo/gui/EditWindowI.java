package apollo.gui;


/** This was used solely by FeatureEditorDialog but no longer - once its established
that the FED is not going back to commit/cancel/clone/replace this interface should
be deleted */
public interface EditWindowI {
  public boolean isChanged ();
  public void close();
  public void save();

}
