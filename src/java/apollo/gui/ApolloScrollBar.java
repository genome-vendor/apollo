package apollo.gui;

import javax.swing.*;

public class ApolloScrollBar extends JScrollBar {

  public ApolloScrollBar() {
    super();
    setUI(new ApolloMetalScrollBarUI());
  }

  public void updateUI() {}
}
