package apollo.editor;

/** This event indicates that the editor is done with a series of edits. The view
    uses this as a signal to resynch and redraw itself. */

public class AnnotSessionDoneEvent extends AnnotationChangeEvent {
  
  /** do we need annot top or changed feature? */
  public AnnotSessionDoneEvent(Object source) {
    super(source);
  }

  public boolean isEndOfEditSession() { return true; }
}
