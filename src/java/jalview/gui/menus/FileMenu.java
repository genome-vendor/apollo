package jalview.gui.menus;

import jalview.io.*;
import jalview.datamodel.*;
import jalview.gui.popups.*;
import jalview.analysis.*;
import jalview.gui.AlignFrame;
import jalview.util.*;
import jalview.gui.event.*;
import jalview.gui.*;

import java.awt.event.*;
import java.awt.*;
import java.applet.Applet;
import java.util.*;
import java.io.*;

public class FileMenu extends FrameMenu {

  OpenFileAction openFile;
    OpenTreeAction openTree;
  SaveFileAction  saveFile;
  SavePSFileAction     savePSFile;
  InputAlignBoxAction  inpAlignBox;
  OutputAlignBoxAction outAlignBox;
    //  MailAlignAction      mailAlign;
    //  MailPSAction         mailPS;
  CloseAction          close;
  QuitAction           quit;

  public FileMenu(AlignFrame frame,AlignViewport av,Controller c) {
    super("File",frame,av,c);
  }

  protected void init() {

    if (!(frame.getAFParent() instanceof Applet)) {
	openFile  = new OpenFileAction("Open...");
	add(openFile);
	//      inpAlignURL   = new InputAlignURLAction("Input alignment as URL");
	//      add(inpAlignURL);
	//      addSeparator();
	openTree = new OpenTreeAction("Open tree...");
	add(openTree);
    }

    saveFile = new SaveFileAction("Save as...");
    add(saveFile);
    savePSFile    = new SavePSFileAction("Print to file...");
    add(savePSFile);

    addSeparator();

    inpAlignBox   = new InputAlignBoxAction("Input alignment via text box");
    add(inpAlignBox);
    outAlignBox   = new OutputAlignBoxAction("Output alignment via text box");
    add(outAlignBox);

    addSeparator();

    //    mailAlign     = new MailAlignAction("Mail alignment...");
    //    add(mailAlign);
    //    mailPS        = new MailPSAction("Mail postscript...");
    //    add(mailPS);

    addSeparator();
    close        = new CloseAction("Close");
    add(close);
    quit         = new QuitAction("Quit");
    add(quit);
  }


    class InputAlignBoxAction extends JalAction {
	public InputAlignBoxAction(String name) {
	super(name);
    }

	public void applyAction(ActionEvent evt) {
	    StatusEvent info =
		new StatusEvent(this,"Creating input textbox...",StatusEvent.INFO);
	    controller.handleStatusEvent(info);

	    String message = "Input alignment via text box";

	    InputPopup ip = new InputPopup(frame,av,controller,message);

	}
    }


  class OutputAlignBoxAction extends JalAction {
    public OutputAlignBoxAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"Creating output textbox...",StatusEvent.INFO));

      OutputPopup ip = new OutputPopup(frame,av,controller,"Alignment " + frame.getTitle());

      // controller.handleStatusEvent(new StatusEvent(this,"done",StatusEvent.INFO));
    }
  }

  class SaveFileAction extends JalAction {
    public SaveFileAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"Creating file chooser...",StatusEvent.INFO));

      System.out.println("parent = " + frame.getAFParent());
      if (frame.getAFParent() instanceof Applet) {
        AppletFilePopup afp = new AppletFilePopup(frame,av,controller,"Save alignment to local file");
      } else {
        OutputFilePopup ofp = new OutputFilePopup(frame,av,controller,"Save alignment to local file");
      }

      // controller.handleStatusEvent(new StatusEvent(this,"done",StatusEvent.INFO));
    }
  }

  class SavePSFileAction extends JalAction {
    public SavePSFileAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"Creating file chooser...",StatusEvent.INFO));

      if (frame.getAFParent() instanceof Applet) {
        AppletPostscriptPopup app = new AppletPostscriptPopup(frame,av,controller,"Save postscript to local file",
                                                              av.getOutputGenerator());
      } else {
        PostscriptFilePopup pfp = new PostscriptFilePopup(frame,av,controller,"Save postscript to local file",
                                                          av.getOutputGenerator());
      }

      // controller.handleStatusEvent(new StatusEvent(this,"done",StatusEvent.INFO));
    }
  }

  class MailPSAction extends JalAction {
    public MailPSAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"Creating mail window...",StatusEvent.INFO));

  //    MailPostscriptPopup mp = new MailPostscriptPopup(frame,av,controller,"Mail postscript",av.getOutputGenerator());

      // controller.handleStatusEvent(new StatusEvent(this,"done",StatusEvent.INFO));
    }
  }

  class MailAlignAction extends JalAction {
    public MailAlignAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"Creating mail window...",StatusEvent.INFO));

    //  MailTextPopup mp = new MailTextPopup(frame,av,controller,"Mail text alignment",av.getOutputGenerator());

      // controller.handleStatusEvent(new StatusEvent(this,"done",StatusEvent.INFO));
    }
  }

  class CloseAction extends JalAction {
    public CloseAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      getAlignFrame().dispose();
      if (getAlignFrame().exitOnClose()) System.exit(0);
    }
  }

  class QuitAction extends JalAction {
    public QuitAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      getAlignFrame().dispose();
      if (getAlignFrame().exitOnClose()) System.exit(0);
    }
  }

  class OpenFileAction extends JalAction {
    public OpenFileAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"Creating file chooser...",StatusEvent.INFO));

      FilePopup fp = new FilePopup(frame,av,controller,"Input alignment from local file");

      // controller.handleStatusEvent(new StatusEvent(this,"done",StatusEvent.INFO));
    }
  }

  class OpenTreeAction extends JalAction {
    public OpenTreeAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"Creating file chooser...",StatusEvent.INFO));
      FileDialog fd = new FileDialog(frame,"Tree file");
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

      fd.setLocation((screenSize.width - fd.getSize().width) / 2,
		     (screenSize.height - fd.getSize().height) / 2);


      fd.show();
      String dir  = "";
      String file = "";

      if (fd.getDirectory() != null) {
        dir = fd.getDirectory();
      }
      if (fd.getFile() != null) {
        file = fd.getFile();
      }
      try {
	  TreeFile tf = new TreeFile(fd.getDirectory() + "/" + fd.getFile(),"File");

	  NJTree tree = tf.getTree();

	  TreePanel treePanel = new TreePanel(frame,av,controller ,tree);
	  TreeFrame treeFrame = new TreeFrame(frame,treePanel);

	  treeFrame.setSize(500,500);


	  screenSize = Toolkit.getDefaultToolkit().getScreenSize();

	  treeFrame.setLocation((screenSize.width - treeFrame.getSize().width) / 2,
		      (screenSize.height - treeFrame.getSize().height) / 2);


	  treeFrame.show();
      } catch (IOException e) {
	  System.out.println("Exception " + e);
      }
      // controller.handleStatusEvent(new StatusEvent(this,"done",StatusEvent.INFO));
    }
  }

  class InputAlignURLAction extends JalAction {
    public InputAlignURLAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      controller.handleStatusEvent(new StatusEvent(this,"Creating file chooser...",StatusEvent.INFO));

      URLPopup up = new URLPopup(frame,av,controller,"Input alignment from local file");

      // controller.handleStatusEvent(new StatusEvent(this,"done",StatusEvent.INFO));
    }
  }
}
