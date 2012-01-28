package apollo.gui.detailviewers.sequencealigner;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;
import apollo.gui.Selection;
import apollo.gui.detailviewers.sequencealigner.AAPanel.AAMultiSequenceAlignerPanel;
import apollo.gui.detailviewers.sequencealigner.DNAPanel.DNAMultiSequenceAlignerPanel;
import apollo.gui.synteny.GuiCurationState;

public class MultiSequenceAlignerFrame extends JFrame {

  private MultiSequenceAlignerPanel panel;

  public static MultiSequenceAlignerFrame makeAligner(
      GuiCurationState curationState, int strand, 
      Selection selection, String type) {
    
   AnnotatedFeatureI feature = BaseFineEditor.getFirstAnnot(selection);
   ReadingFrame frame = ReadingFrame.ONE;
   if (feature != null) {
     frame = ReadingFrame.valueOf(feature.getFrame());
   }
   
   

    MultiSequenceAlignerPanel msaP = MultiSequenceAlignerPanel.makeAligner(
        type, curationState, strand, frame);

    MultiSequenceAlignerFrame msaF = new MultiSequenceAlignerFrame(msaP);
    
    msaF.setPreferredSize(new Dimension(1000, 800));
    msaF.pack();
    msaF.setVisible(true);
    
    
    AnnotatedFeatureI gai = getFirstAnnot(selection);
    
    if (gai != null) {
       msaF.getPanel().scrollToBase(gai.getStart());
       msaF.getPanel().setSelection(gai);
    }
    
    return msaF;
  }

  private MultiSequenceAlignerFrame() {
    super();
  }
  
  private MultiSequenceAlignerFrame(MultiSequenceAlignerPanel panel) {
    super();
    this.panel = panel;
    init();
  }
  
  public void init() {
    
    addMenu();
    add(panel);
    panel.init();
    attachListeners();
  }

  protected void validateTree() {
    if (!panel.isValid()) {
      panel.validate();
    }
    super.validateTree();
  }
  
  public MultiSequenceAlignerPanel getPanel() {
    return panel;
  }
  
  public void validate() {
    Dimension size = this.getSize();
    // I think the size includes the bar on the top (maybe thats just for macs?)
    size.height = size.height - 50;
    size.height = size.height - getRootPane().getJMenuBar().getSize().height;
    if (!panel.getPreferredSize().equals(size)) {
      panel.setPreferredSize(size);
      panel.invalidate();
    }
    super.validate();
  }
  
  public void attachListeners() {
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        panel.cleanUp();
        panel = null;
      }
    }
   );
  }
  
  private void addMenu() {
    JMenuBar menuBar = this.getRootPane().getJMenuBar();
    JMenu menu;
    JMenuItem menuItem;
    
    menuBar = new JMenuBar();
    this.getRootPane().setJMenuBar(menuBar);
    
    //Build the first menu.
    menu = new JMenu("Main Menu");
    //menu.setMnemonic(KeyEvent.VK_A);
    menu.getAccessibleContext().setAccessibleDescription(
            "The main menu for this panel");
    menuBar.add(menu);
    
    String switchText = "Switch Display";
    
    if (panel instanceof DNAMultiSequenceAlignerPanel) {
      switchText = "Switch to AA view";
    } else {
      switchText = "Switch to DNA view";
    }
    
    menuItem = new JMenuItem(new AbstractAction(switchText) {
      public void actionPerformed(ActionEvent arg0) {
        switchDisplay();
      }
    });
    menuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_SPACE, ActionEvent.ALT_MASK));
    menuItem.getAccessibleContext().setAccessibleDescription(
            "Updates the reults view to display data from the opposite strand");
    menu.add(menuItem);
  }

  private void switchDisplay() {
    
    MultiSequenceAlignerPanel p = panel;
    
    String type = panel.getType();
    GuiCurationState curationState = panel.getCurationState();
    Strand strand = panel.getStrand();
    int base = panel.getVisibleBase();
    Dimension size = panel.getSize();
    AnnotatedFeatureI selection = panel.getSelection();
    Color color = panel.getIndicatorColor();
    ReadingFrame frame = ReadingFrame.ONE;
    if (selection != null && selection.getFrame() > 0) {
      frame = ReadingFrame.valueOf(selection.getFrame());
    }
    
    getRootPane().getJMenuBar().remove(panel.getMenu());
    remove(panel);
    panel.cleanUp();
    
    if (SequenceI.DNA.equals(type)) {
      type = SequenceI.AA;
    } else if (SequenceI.AA.equals(type)) {
      type = SequenceI.DNA;
    }
    
    panel = MultiSequenceAlignerPanel.makeAligner(
        type, curationState, strand.toInt(), frame);
    
    addMenu();
    add(panel);
    panel.init();
    panel.invalidate();
    panel.setIndicatorColor(color);
    
    this.setPreferredSize(this.getSize());
    pack();
    setVisible(true);
    validateTree();
    
    panel.setSelection(selection);
    panel.scrollToBase(base);
    
  }
  
  /** Returns first AnnotatedFeatureI in Selection on strand clicked on,
  null if there is none.
  Exon, Transcript, and Gene are all AnnotatedFeatureI's */
  public static AnnotatedFeatureI getFirstAnnot(Selection selection) {
    for (int i=0; i < selection.size() ;i++) {
      SeqFeatureI feat = selection.getSelectedData(i);
      if (feat instanceof AnnotatedFeatureI ) {
        // If this is an exon, get the parent transcript
        // XXX: uncoment if you want to start at the beginning of the transcript
        //if (feat instanceof Exon)
          //feat = feat.getRefFeature();
        return (AnnotatedFeatureI)feat;
      }
    }
    return null;
  }
  
}
