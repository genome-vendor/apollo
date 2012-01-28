package apollo.gui.genomemap;

// import java.awt.MouseInputListener;
import java.awt.Event;
import java.awt.event.MouseEvent;
import apollo.util.FeatureList;
import apollo.gui.SelectionManager;
import apollo.gui.Selection;
import apollo.gui.synteny.GuiCurationState;
import apollo.gui.synteny.CurationManager;

/**
 * MouseManager should handle all the mouse logic for ApolloPanel. This is to
 * separate that out from ApolloPanel and make it less cluttered.
 *
 * Just adding a little bit for now, eventually should have all the mouse listener
 * methods
 Rename ApolloPanelMouseManager to make it clear that its ApolloPanel's mouse manager?
 */

class MouseManager /*implements MouseInputListener*/ {

  private ApolloPanel apolloPanel;
  private SelectionManager selectionManager;
  private GuiCurationState curationState;

  MouseManager(ApolloPanel ap,GuiCurationState cs) {
    apolloPanel = ap;
    curationState = cs;
    selectionManager = curationState.getSelectionManager();
  }

  /** A Left click is a selection */
  void doLeftClick(MouseEvent evt) {

    PickViewI pickViewFocus;
    if (!apolloPanel.focusIsSelectable())
      return;

    pickViewFocus = apolloPanel.getPickViewFocus();

    CurationManager.getCurationManager().setActiveCurState(getCurationState());
    
    boolean exclusiveSelection = false;
    // if no shift key then it's an exclusive selection
    // (everything else deselects)
    if ((evt.getModifiers() & Event.SHIFT_MASK) == 0) {
      exclusiveSelection = true;
    }

    boolean selectParents = false;
    if (evt.getClickCount() == 2) {
      selectParents = true;
    }

    /* have to return Selection, to get SelectionItems with model and
       drawable listeners have to do selectParents here to get the 
       drawable parents in there */
    Selection newSelection =
      pickViewFocus.findFeaturesForSelection(evt.getPoint(),selectParents);

    /* changing source to apolloPanel for handleFeatureSelEvent - 
       dont think source needs fine granularity of view */
    selectionManager.select(newSelection,exclusiveSelection,apolloPanel);

//     //handle the change of active species for synteny view:
//     java.awt.Window possibleFrame 
//       = javax.swing.SwingUtilities.windowForComponent(apolloPanel);
//     if(possibleFrame != null && 
//        possibleFrame instanceof apollo.gui.synteny.CompositeApolloFrame){
//       ((apollo.gui.synteny.CompositeApolloFrame)possibleFrame)
//         .handlePanelSelection(apolloPanel.getStrandedZoomableApolloPanel());
//     }
  }

  private GuiCurationState getCurationState() { return curationState; }
}
