package apollo.gui.menus;

import apollo.config.Config;
import apollo.config.PropertyScheme;
import apollo.config.TierProperty;
import apollo.gui.*;
import apollo.gui.drawable.DrawableAnnotationConstants;
import apollo.gui.genomemap.StrandedZoomableApolloPanel;
import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.gui.synteny.CurationManager;
import apollo.gui.tweeker.Tweeker;
import apollo.analysis.AnalysisDataAdapterI;
import apollo.analysis.AnalysisGUI;
import apollo.analysis.BlastXMLParser;
import apollo.analysis.RemoteBlastNCBI;
import apollo.analysis.SeqAnalysisI;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;
import java.util.Vector;

import org.bdgp.swing.widget.DataAdapterChooser;
import org.bdgp.io.DataAdapter;
import org.bdgp.io.DataAdapterRegistry;


/** has menu items for gc plot and restriction enzyme. Works with "active" curation set
 */
// Show/hide GC plot checkbox is now in Tweeker.java

public class AnalysisMenu extends JMenu implements ActionListener,
  DrawableAnnotationConstants {
  JMenuItem    gcplot;
  JMenuItem    restriction;
  JMenuItem            analyze;

  private Tweeker tweekerFrame;

  //ApolloFrame   frame;
  
  public AnalysisMenu() { //ApolloFrame frame) {
    super("Analysis");
    //this.frame = frame;
    menuInit();
  }

  public void menuInit() {
    gcplot            = new JMenuItem("Show GC plot...");
    restriction       = new JMenuItem("Find restriction sites...");
    analyze           = new JMenuItem("Analyze sequence...");

    add(gcplot);
    add(restriction);
    addSeparator();
    add(analyze);  // Not working yet--don't add

    analyze.addActionListener(this);
    ActionListener al = new AnalysisActionListener();
    gcplot.addActionListener(al);
//  new ActionListener() {public void actionPerformed(ActionEvent e) { 
//   openTweeker(frame, "gc");frame.getOverviewPanel().setGraphVisibility(true);} } );
    restriction.addActionListener(al);
//   new ActionListener() {public void actionPerformed(ActionEvent e) { 
//     openTweeker(frame, "restriction"); } } );

    this.addMenuListener(new SequenceCheckMenuListener());
  }

  private class AnalysisActionListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      int type = -1;
      if (e.getSource() == gcplot) 
        type = Tweeker.GC;
      else if (e.getSource() == restriction)
        type = Tweeker.RESTRICTION;

      Tweeker.openTweeker(type);
    }
  }

  // Hafta check seq on fly. dont know about seq at init time and seq changes
  private class SequenceCheckMenuListener implements MenuListener {
    public void menuSelected(MenuEvent e) {
      gcplot.setEnabled(haveSequence());
      restriction.setEnabled(haveSequence());
      analyze.setEnabled(haveSequence());
      // analyze is not working yet
      //analyze.setEnabled(false);
    } 
    public void menuDeselected(MenuEvent e) {}
    public void menuCanceled(MenuEvent e) {}
  }

  /** returns true if active curation has sequence */
  private boolean haveSequence() {
    return CurationManager.getCurationManager().getActiveCurState().haveSequence();
  }

  public void actionPerformed(ActionEvent e) {
    //    if (e.getSource() == gcplot) {
    //       frame.getOverviewPanel().setGraphVisibility(gcplot.getState());
    //    } 
    if (e.getSource() == analyze) {
      //      SequenceI seq = frame.getSeqFromClipboard();
      /*
      DataAdapterChooser chooser 
	= new DataAdapterChooser(Config.getAdapterRegistry(),
				 AnalysisDataAdapterI.OP_ANALYZE_DATA,
				 "Analyze data",
				 null,
				 false);
      chooser.show();
      */
      new AnalysisGUI();
    }
  }

  class ItemWindowListener extends WindowAdapter {
    JCheckBoxMenuItem item;

    public ItemWindowListener(JCheckBoxMenuItem item) {
      this.item = item;
    }

    public void windowClosing(WindowEvent e) {
      item.setState(false);
      ((Window)e.getSource()).removeWindowListener(this);
    }
  }
}
  // Could this go somewhere else so that it was accessible in GraphView?
  // which is "gc" or "restriction"
//   private void openTweeker(ApolloFrame frame, String which) {
//     ScoreCalculator c = frame.getOverviewPanel().getGraphView().getScoreCalculator();
//     BoundedRangeModel m = ((WindowScoreCalculator)c).getModel();
//     if (tweekerFrame!=null) 
//       tweekerFrame.clear();
//     StrandedZoomableApolloPanel szap = frame.getOverviewPanel();
//     boolean revcomp = szap.isReverseComplement();
//     tweekerFrame = new Tweeker(m,szap,revcomp,which);

//     // clean up ref (mem leak) on tweeker closing
//     tweekerFrame.addWindowListener(new WindowAdapter() {
// 	public void windowClosed(WindowEvent e) { tweekerFrame = null;	}
//       });

//     tweekerFrame.setVisible(true);
//     tweekerFrame.toFront();
//     tweekerFrame.selectTab(which);
//     if (tweekerFrame.getState()==Frame.ICONIFIED) // linux iconifying issue
//       tweekerFrame.setState(Frame.NORMAL);
//   }


