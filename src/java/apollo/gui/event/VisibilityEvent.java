package apollo.gui.event;

import java.util.EventObject;

public class VisibilityEvent extends EventObject
{

  private boolean removeElement;
  
  public VisibilityEvent(Object source)
  {
    this(source, false);
  }
  
  public VisibilityEvent(Object source, boolean removeElement)
  {
    super(source);
    this.removeElement = removeElement;
  }
  
  public boolean isRemoveElement()
  {
    return removeElement;
  }
}
