package apollo.gui.event;


import java.awt.event.*;
import javax.swing.JButton;

import apollo.gui.ApolloFrame;
import apollo.main.DataLoader;
import apollo.main.LoadUtil;

import org.apache.log4j.*;

public class BasicWindowListener extends WindowAdapter {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------
  
  private static final Logger logger = LogManager.getLogger(BasicWindowListener.class);


  private static Object[] options = { "Quit anyway",
                                      "Save first, then quit",
                                      "Cancel" };

  public void windowClosed(WindowEvent e) {
    System.exit(0);
  }

  public void windowClosing(WindowEvent e) {
    boolean close_up = ! (e.getWindow() instanceof ApolloFrame);

    if (!close_up) {
      DataLoader loader = new DataLoader();
      close_up = (LoadUtil.confirmSaved(options, loader));
    }
    if (close_up) {
      if (e.getWindow() instanceof ApolloFrame)
        logger.info("closing apollo, cheerio!");
      e.getWindow().setVisible(false);
      e.getWindow().dispose();
    }
  }
}
