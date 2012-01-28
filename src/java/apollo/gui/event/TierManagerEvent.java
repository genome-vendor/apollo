package apollo.gui.event;

import apollo.gui.TierManager;
import java.util.EventObject;

public class TierManagerEvent extends EventObject {
  public static final int LAYOUT_CHANGED = 1;

  int          type;
  TierManager  tierManagerSource;

  public TierManagerEvent(Object source,TierManager tierManagerSource, int type) {
    super(source);

    this.tierManagerSource = tierManagerSource;
    this.type              = type;
  }

  public int getType() {
    return type;
  }

  public TierManager getTierManagerSource() {
    return tierManagerSource;
  }

  public Object getSource() {
    return source;
  }
}
