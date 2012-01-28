package apollo.gui.event;
import java.util.EventObject;

public class ChainedRepaintEvent extends EventObject {
  public ChainedRepaintEvent(Object source){
    super(source);
  }
}
