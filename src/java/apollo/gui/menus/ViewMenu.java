package apollo.gui.menus;

import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.config.Config;
import apollo.gui.*;
import apollo.gui.drawable.DrawableAnnotationConstants;
import apollo.gui.genomemap.StrandedZoomableApolloPanel;
import apollo.gui.detailviewers.seqexport.SeqExport;
import apollo.gui.evidencepanel.EvidencePanelOrientationManager;
import apollo.gui.synteny.CurationManager;
import apollo.gui.synteny.GuiCurationState;

import java.io.*;
import java.util.Vector;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import apollo.gui.featuretree.*;

public class ViewMenu extends JMenu implements ActionListener,
  DrawableAnnotationConstants {

  JMenuItem            orientation;
  JMenuItem            navigator;
  /** Index of navigator menu item */
  private int navigatorPosition;
  JMenuItem            invertcolours;
  JMenuItem            zoom;
  JMenuItem            flip;
  JMenuItem            resetViews;
  JCheckBoxMenuItem    forward;
  JCheckBoxMenuItem    reverse;
  JCheckBoxMenuItem    annotations;
  JCheckBoxMenuItem    controls;
  JCheckBoxMenuItem    results;
  private JCheckBoxMenuItem showSites;
  JCheckBoxMenuItem    scale;
  JCheckBoxMenuItem    reverseComplement;
  JCheckBoxMenuItem    textAvoid;
  public static JCheckBoxMenuItem tree;  // Public so when tree is killed, checkbox can be unchecked
  JCheckBoxMenuItem    guide;
  //  JCheckBoxMenuItem    threeD;
  //  JCheckBoxMenuItem    outline;
  JCheckBoxMenuItem    showEdgeMatches;
  private JMenuItem    newSequenceWindowMenuItem;

  //  JMenuItem         dogc;

  ApolloFrame   frame;

  public ViewMenu(ApolloFrame frame) {
    super("View");
    this.frame = frame;
    addMenuListener(new ViewMenuListener());
    getPopupMenu().addPropertyChangeListener(new PopupPropertyListener());
    menuInit();  // Need?  Or is it called automatically?
  }

  public void menuInit() {
    flip              = new JMenuItem("Flip strands");
    forward           = new JCheckBoxMenuItem("Show forward strand");
    reverse           = new JCheckBoxMenuItem("Show reverse strand");
    reverseComplement = new JCheckBoxMenuItem("Reverse complement");
    results           = new JCheckBoxMenuItem("Show results");
    annotations       = new JCheckBoxMenuItem("Show annotations");
    showSites         = new JCheckBoxMenuItem("Show starts/stops (when zoomed in)");
    scale             = new JCheckBoxMenuItem("Show axis");
    showEdgeMatches   = new JCheckBoxMenuItem("Show edge matches");
    textAvoid         = new JCheckBoxMenuItem("Avoid text overlaps");
    guide             = new JCheckBoxMenuItem("Show guide line");
    tree              = new JCheckBoxMenuItem("Show annotation tree");
    newSequenceWindowMenuItem = new JMenuItem("Open new sequence window");
    invertcolours     = new JMenuItem("Invert result background color");
    //    threeD            = new JCheckBoxMenuItem("Draw 3D rectangles");
    //    outline           = new JCheckBoxMenuItem("Draw outline rectangles");
    zoom              = new JMenuItem("Zoom to selected");
    resetViews        = new JMenuItem("Reset views");

    controls          = new JCheckBoxMenuItem("Show control panel");

    guide            .setState(false);

    // These get set by ViewMenuListener
//     forward          .setState(true);
//     reverse          .setState(true);
//     results          .setState(true);
//     showSites        .setState(Config.getStyle().getInitialSitesVisibility());
//     scale            .setState(true);
//     if(Config.getShowAnnotations()){
//       annotations.setState(true);
//     }else{
//       annotations.setState(true); // shouldnt this be false???
//     }
//    reverseComplement.setState(false);
    
    textAvoid        .setState(true);
    //    threeD           .setState(Config.getDraw3D());
    //    outline          .setState(Config.getDrawOutline());

    showEdgeMatches  .setState(true);
    showEdgeMatches  .setEnabled(true);

    //newSequenceWindowMenuItem.setEnabled(frame.haveSequence());
    newSequenceWindowMenuItem.setEnabled(getActiveCurState().haveSequence());

    add(flip);
    add(reverseComplement);
    add(forward);
    add(reverse);
    addSeparator();
    add(results);
    add(annotations);
    add(scale);
    add(showSites);
    addSeparator();

    // This locks navigator menu item in with a particular szap - need to change 
    // this - it makes ViewMenu unreusable
    navigatorPosition = getItemCount();
    //navigator = add(frame.getOverviewPanel().getNavigationAction());
    navigator = add(getActiveNavigationAction());
    add(controls);
    add(tree);
    add(newSequenceWindowMenuItem);
    addSeparator();

    add(showEdgeMatches);
    add(textAvoid);
    add(guide);
    //orientation = add(frame.getOrientationAction());
    orientation = add(EvidencePanelOrientationManager.getSingleton().getAction());
    add(invertcolours);
    addSeparator();

    //    add(threeD);
    //    add(outline);
    // add(dogc);
    //    addSeparator();

    add(zoom);
    add(resetViews);

    flip             .addActionListener(this);
    tree             .addActionListener(this);
    guide            .addActionListener(this);
    orientation      .addActionListener(this);
    forward          .addActionListener(this);
    reverse          .addActionListener(this);
    annotations      .addActionListener(this);
    results          .addActionListener(this);
    scale            .addActionListener(this);
    showSites        .addActionListener(this);
    reverseComplement.addActionListener(this);
    textAvoid        .addActionListener(this);
    showEdgeMatches  .addActionListener(this);
    invertcolours    .addActionListener(this);
    //    threeD           .addActionListener(this);
    //    outline          .addActionListener(this);
    zoom             .addActionListener(this);
    resetViews       .addActionListener(this);

    controls.addActionListener(this);

    newSequenceWindowMenuItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            Selection sel = getActiveCurState().getSelectionManager().getSelection();
            new SeqExport(sel,getActiveCurState().getController());
          }
        }
                                               );

    zoom    .setMnemonic('Z');
    forward .setMnemonic('F');
    reverse .setMnemonic('R');
    orientation.setMnemonic('V');
    flip.    setMnemonic('B');

    zoom    .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                            ActionEvent.CTRL_MASK));

    forward .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,
                            ActionEvent.CTRL_MASK));

    reverse .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,
                            ActionEvent.CTRL_MASK));

    orientation.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,
                               ActionEvent.CTRL_MASK));

    flip.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B,
                        ActionEvent.CTRL_MASK));
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == zoom) {
      getSZAP().zoomToSelection();
    } else if (e.getSource() == resetViews) {
      getSZAP().resetViews();
    } else if (e.getSource() == forward) {
      getSZAP().setForwardVisible(forward.getState());
    } else if (e.getSource() == reverse) {
      getSZAP().setReverseVisible(reverse.getState());
    }else if(e.getSource() == annotations){
      getSZAP().setAnnotationViewsVisible(annotations.getState());
    }else if(e.getSource() == controls){
      getSZAP().setControlPanelVisibility(controls.getState());
    }else if(e.getSource() == results){
      getSZAP().setResultViewsVisible(results.getState());
    }else if(e.getSource() == scale){
      getSZAP().setScaleViewVisible(scale.getState());
      frame.repaint();
    } else if (e.getSource() == showSites) {
      getSZAP().setSiteViewVisibleOnZoom(showSites.getState());
    }else if (e.getSource() == reverseComplement) {
      revComp();
    } else if (e.getSource() == flip) {
      revComp(true);
      // If only one strand was showing, show the other one where the
      // first one was.  e.g., if forward strand was showing, now show
      // revcomp of reverse strand on top of axis.
      if (forward.getState() && !reverse.getState()) {
        getSZAP().setForwardVisible(false);
        forward.setState(false);
        getSZAP().setReverseVisible(true);
        reverse.setState(true);
      } else if (!forward.getState() && reverse.getState()) {
        getSZAP().setForwardVisible(true);
        forward.setState(true);
        getSZAP().setReverseVisible(false);
        reverse.setState(false);
      }
    } else if (e.getSource() == textAvoid) {
      getSZAP().setTextAvoidance(textAvoid.getState());
    } else if (e.getSource() == guide) {
      getSZAP().setGuideLine(guide.getState());
    } else if (e.getSource() == tree) {
      //FeatureTreeFrame treeFrame = frame.getTreePanel();
      FeatureTreeFrame treeFrame = 
        CurationManager.getCurationManager().getActiveCurState().getFeatureTreeFrame();
      treeFrame.setVisible(tree.getState());
      if (tree.getState() == false)
        treeFrame.removeNotify();
      else 
        treeFrame.addWindowListener(new ItemWindowListener(tree));
        //treeFrame.setApolloFrame(frame);
      // fixes iconifying bug on linux
      if (treeFrame.getState()==Frame.ICONIFIED)
        treeFrame.setState(Frame.NORMAL);
    } else if (e.getSource() == showEdgeMatches) {
      getSZAP().setEdgeMatching(showEdgeMatches.getState());
    } else if (e.getSource() == invertcolours) {
      invertResultBackgroundColor();
    }
  }

  // This method, which inverts ALL the colors, is no longer in use--
  // it's not linked to anything on the View menu, which now calls
  // InvertResultBackgroundColor.
  private void invertColors() {
    // Invert colors for axis

    // If we're in revcomp mode, first revert axis color
    if (reverseComplement.getState()) {
      Color tmp = Config.getCoordForeground();
      (Config.getStyle()).setCoordForeground(Config.getCoordRevcompColor());
      (Config.getStyle()).setCoordRevcompColor(tmp);
    }

    Color tmp = Config.getCoordForeground();
    (Config.getStyle()).setCoordForeground(Config.getCoordBackground());
    (Config.getStyle()).setSequenceColor(Config.getCoordBackground());
    //(Config.getStyle()).setEdgematchColor(Config.getCoordBackground());
    (Config.getStyle()).setCoordBackground(tmp);

    // If we're in revcomp mode, change axis color back
    if (reverseComplement.getState()) {
      tmp = Config.getCoordForeground();
      (Config.getStyle()).setCoordForeground(Config.getCoordRevcompColor());
      (Config.getStyle()).setCoordRevcompColor(tmp);
    }

    // Invert colors for Annotation panel
    Color annotColor = (Config.getStyle()).getAnnotationBackground();
    annotColor = oppositeColor(annotColor);
    (Config.getStyle()).setAnnotationBackground(annotColor);

    Color annotLabelColor = (Config.getStyle()).getAnnotationLabelColor();
    annotLabelColor = oppositeColor(annotLabelColor);
    (Config.getStyle()).setAnnotationLabelColor(annotLabelColor);

    // Invert colors for Feature panel
    Color featureColor = (Config.getStyle()).getFeatureBackground();
    featureColor = oppositeColor(featureColor);
    (Config.getStyle()).setFeatureBackground(featureColor);

    Color featureLabelColor = (Config.getStyle()).getFeatureLabelColor();
    featureLabelColor = oppositeColor(featureLabelColor);
    (Config.getStyle()).setFeatureLabelColor(featureLabelColor);

    // Invert edgematch color
    Color edgematchColor = (Config.getStyle()).getEdgematchColor();
    (Config.getStyle()).setEdgematchColor(oppositeColor(edgematchColor));

    getSZAP().setViewColours();
    frame.repaint();
  }

  private void invertResultBackgroundColor() {
    // Invert colors for Feature (result) panel
    Color featureColor = (Config.getStyle()).getFeatureBackground();
    featureColor = oppositeColor(featureColor);
    (Config.getStyle()).setFeatureBackground(featureColor);

    Color featureLabelColor = (Config.getStyle()).getFeatureLabelColor();
    featureLabelColor = oppositeColor(featureLabelColor);
    (Config.getStyle()).setFeatureLabelColor(featureLabelColor);

    // Invert edgematch color
    Color edgematchColor = (Config.getStyle()).getEdgematchColor();
    (Config.getStyle()).setEdgematchColor(oppositeColor(edgematchColor));

    getSZAP().setViewColours();
    frame.repaint();
  }

  // Should this go elsewhere, so others can use it?
  public Color oppositeColor(Color orig) {
    int red = orig.getRed();
    int green = orig.getGreen();
    int blue = orig.getBlue();
    Color opposite = new Color(255 - red, 255 - green, 255 - blue);
    return(opposite);
  }

  public void revComp() {
    revComp(false);
  }

  public void revComp(boolean toggleReverseComplementCheckbox) {

    //This might not have been set up (if there are multiple szap's
    //- do it here if necessary.
    if(getSZAP().getRevCompListener() == null){
      getSZAP().setRevCompListener(
        new apollo.gui.genomemap.RevCompListener() {
          public void updateRevComp(boolean isRevComp) {
            reverseComplement.setState(isRevComp);
            if (reverseComplement.getState() == true){
              reverseComplement.setForeground(Color.red);
            }else{
              reverseComplement.setForeground(Color.black);
            }
          }//end updateRevComp
        }
      );
    }//end if
    
    if (toggleReverseComplementCheckbox)
      reverseComplement.setState(!reverseComplement.getState());

    if (reverseComplement.getState() == true)
      reverseComplement.setForeground(Color.red);
    else
      reverseComplement.setForeground(Color.black);

    getSZAP().setReverseComplement(reverseComplement.getState());
  }

  void setHaveSequence(boolean haveSequence) {
    newSequenceWindowMenuItem.setEnabled(haveSequence);
  }

  /** sets menu item state to false on closing of window */
  private class ItemWindowListener extends WindowAdapter {
    JCheckBoxMenuItem item;

    public ItemWindowListener(JCheckBoxMenuItem item) {
      this.item = item;
    }

    public void windowClosing(WindowEvent e) {
      item.setState(false);
      ((Window)e.getSource()).removeWindowListener(this);
    }
  }

  //private StrandedZoomableApolloPanel getSZAP(){return frame.getOverviewPanel();}
  private StrandedZoomableApolloPanel getSZAP() { 
    return getActiveCurState().getSZAP(); 
  }

  /** Return navigation action for active szap/movementPanel */
  private Action getActiveNavigationAction() {
    return getActiveCurState().getNavigationBar().getNavigationAction();
  }

  private GuiCurationState getActiveCurState() {
    return CurationManager.getCurationManager().getActiveCurState();
  }

  /** This queries for menu state before menu comes up. Presently this does not do
      strand, but this could replace the StrandVisibilityListeners. This is an 
      easier way of going about the same task. The sole reason for 
      StrandVisibilityListeners is for the ViewMenu, so they could be tossed. */
  private class ViewMenuListener implements MenuListener {
    public void menuSelected(MenuEvent e) {
      // this should be reworked - quickie pre-release solution
      // quick fix of mem leak/bug
      // have to get navigator hooked up with latest szap 
      // dont like the 10 hardwiring - could easily trip things
      // i do like dynamically getting the szap/action
      // apparetnly ya cant just reset the action for a menu item
      remove(navigatorPosition);
      JMenuItem i = new JMenuItem(getActiveNavigationAction());
      navigator = insert(i,navigatorPosition);

      // Enable the "Show annotation tree" menu item only if we have annotations
      tree.setEnabled(getActiveCurState().haveAnnots());
      // should szap implement some sort of view visibility interface?
      annotations.setState(getSZAP().areAnnotationViewsVisible());
      controls.setState(getSZAP().isControlPanelVisible());
      results.setState(getSZAP().areResultViewsVisible());
      scale.setState(getSZAP().isScaleViewVisible());
      showSites.setState(getSZAP().getSiteViewVisibleOnZoom());
      forward.setState(getSZAP().isForwardStrandVisible());
      reverse.setState(getSZAP().isReverseStrandVisible());
      showEdgeMatches.setState(getSZAP().isShowingEdgeMatches());
      reverseComplement.setState(getSZAP().isReverseComplement());
      Color rc = reverseComplement.getState() ? Color.red : Color.black;
      reverseComplement.setForeground(rc);
    }

    public void menuDeselected(MenuEvent e) { }
    public void menuCanceled(MenuEvent e) { }
  }

}
