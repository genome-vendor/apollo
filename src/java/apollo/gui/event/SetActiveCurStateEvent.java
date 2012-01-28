package apollo.gui.event;

import apollo.gui.synteny.GuiCurationState;
import java.util.EventObject;

public class SetActiveCurStateEvent extends EventObject {

  GuiCurationState oldActiveCurState;
  GuiCurationState newActiveCurState;

  public SetActiveCurStateEvent(Object source,GuiCurationState newACS, GuiCurationState oldACS) {
    super(source);

    this.oldActiveCurState = oldACS;
    this.newActiveCurState = newACS;
  }

  public GuiCurationState getNewActiveCurState() {
    return newActiveCurState; 
  }

  public GuiCurationState getOldActiveCurState() {
    return oldActiveCurState;
  }

  public Object getSource() {
    return source;
  }
}
