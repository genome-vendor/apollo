package apollo.gui.menus;

import apollo.config.Config;
import apollo.gui.*;
import apollo.gui.drawable.DrawableSeqFeature;
import apollo.gui.event.BaseFocusEvent;
import apollo.gui.event.FeatureSelectionEvent;
import apollo.editor.FeatureChangeListener;
import apollo.editor.FeatureChangeEvent;
import apollo.dataadapter.DataLoadEvent;
import apollo.dataadapter.DataLoadListener;
import apollo.gui.synteny.CurationManager;
import apollo.gui.synteny.GuiCurationState;
import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.util.*;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

import org.bdgp.util.VectorUtil;
import org.bdgp.util.*;

/** This menu allows for the bookmarking of annots. The user can then return to this
    annot (select it) at another time by selecting the bookmark added previously.
    For multi-curSet currently you can add bookmarks from the active curSet, but
    when a bookmark is selected it sends the select to all cur sets. (this can be
    ammended if so desired) */
public class BookmarkMenu extends JMenu {

  //private ApolloFrame frame;
  private JMenuItem addBookmark;
  private JMenuItem removeBookmark;
  FeatureComparator featureComparator = new FeatureComparator();

  private Vector bookmarks;

  BookmarkMenu() {//ApolloFrame frame) {
    super("Bookmarks");
    //this.frame = frame;
    //frame.getController().addListener(new FeatureDelListener());
    Controller.getMasterController().addListener(new FeatureDelListener());
    // clear out bookmarks when a new region is loaded
    Controller.getMasterController().addListener(new DataLoadListener() {
        public boolean handleDataLoadEvent(DataLoadEvent e) {
          bookmarks.clear();
          removeAll();
          return true; // remove as listener?
        } } );
    bookmarks = new Vector();
    addBookmark = new JMenuItem("Add bookmark");
    removeBookmark = new JMenuItem("Remove bookmark");
    try {
      addBookmark.addActionListener(
        new AddBookmarkListener (this));
      removeBookmark.addActionListener(
        new RemoveBookmarkListener(this));
    } catch (Exception e) {
      e.printStackTrace();
    }
    addMenuListener(new BookmarkMenuListener());
    getPopupMenu().addPropertyChangeListener(new PopupPropertyListener());
  }

  private class BookmarkMenuListener implements MenuListener {
    public void menuCanceled(MenuEvent e) {}

    public void menuDeselected(MenuEvent e) {}

    public void menuSelected(MenuEvent e) {
      menuInit();
    }
  }

  private class FeatureComparator implements org.bdgp.util.Comparator {
    public int compare(Object a, Object b) {
      SeqFeatureI sf1 = (SeqFeatureI) a;
      SeqFeatureI sf2 = (SeqFeatureI) b;
      if (sf1.getLow() < sf2.getLow())
        return -1;
      else if (sf1.getLow() > sf2.getLow())
        return 1;
      else
        return 0;
    }
  }

  /** INNER CLASS for centering, selecting, and zooming. Doesnt the zooming
      cover the centering? */
  private class FocusMenuListener implements ActionListener {
    SeqFeatureI feature;

    public FocusMenuListener(SeqFeatureI feature) {
      this.feature = feature;
    }

    public void actionPerformed(ActionEvent e) {
      // do we need centering if we have the zooming below?
      // what if wrong species/curSet - will it still center way off bounds?
//       frame.getController().handleBaseFocusEvent(
//          new BaseFocusEvent(this,
//                             feature.getLow()+
//                             (feature.getHigh() -
//                              feature.getLow())/2,
//                             feature));

      //frame.getSelectionManager().select(feature,this);
      //frame.getOverviewPanel().zoomToSelectionWithWindow(Config.getGeneWindow());
      // just select and zoom all curations. if feature not in curation presumably
      // it wont select nor zoom.
      CurationManager cm = CurationManager.getCurationManager();
      for (int i=0; i< cm.numberOfCurations(); i++) {
        GuiCurationState cs = cm.getCurationState(i);
        cs.getSelectionManager().select(feature,this);
        cs.getSZAP().zoomToSelectionWithWindow(Config.getGeneWindow());
      }
    }
  }

  /** INNER CLASS FeatureDelListener - listens for annot deletes */
  private class FeatureDelListener implements FeatureChangeListener {
    public boolean handleFeatureChangeEvent(FeatureChangeEvent evt) {
      if (evt.isDelete()) {
        if (evt.getChangeTop() instanceof AnnotatedFeatureI)
          bookmarks.removeElement(evt.getChangeTop());
      }
      return true;
    }
  }


  /** INNER CLASS AddBookmarkListener */
  private class AddBookmarkListener implements ActionListener {
    BookmarkMenu bookmark_menu;

    public AddBookmarkListener(BookmarkMenu bmm) {
      this.bookmark_menu = bmm;
    }

    public void actionPerformed(ActionEvent e) {
      FeatureList genes = bookmark_menu.getSelectedGenes();
      for(int i=0; i < genes.size(); i++)
        bookmarks.addElement(genes.getFeature(i));
      VectorUtil.sort(bookmarks, featureComparator);
    }
  }

  /** INNER CLASS RemoveBookmarkListener */
  private class RemoveBookmarkListener implements ActionListener {
    BookmarkMenu bookmark_menu;

    public RemoveBookmarkListener(BookmarkMenu bmm) {
      this.bookmark_menu = bmm;
    }

    public void actionPerformed(ActionEvent e) {
      FeatureList genes = bookmark_menu.getSelectedGenes();
      for(int i=0; i < genes.size(); i++)
        bookmarks.removeElement(genes.getFeature(i));
    }
  }

  /** Should this get selected genes from active cur set or all cur sets? for now
      just do active cur set */
  private FeatureList getSelectedGenes() {
    //FeatureList selSpans=frame.getOverviewPanel().getApolloPanel().getSelection();
    GuiCurationState cs = CurationManager.getCurationManager().getActiveCurState();
    FeatureList selectedSpans = cs.getSelectionManager().getSelection();
    //Hashtable uniqueFeatures = new Hashtable();
    FeatureList uniqueAnnots = new FeatureList(); // FeatureList checks unique
    for(int i=0; i < selectedSpans.size(); i++) {
      SeqFeatureI sf = selectedSpans.getFeature(i);

      if (sf instanceof ExonI) {
        AnnotatedFeatureI g
          = (AnnotatedFeatureI) sf.getRefFeature().getRefFeature();
        //uniqueFeatures.put(g,g);
        uniqueAnnots.addFeature(g);
      }
      else if (sf instanceof Transcript)
        uniqueAnnots.addFeature(sf.getRefFeature());
        //uniqueFeatures.put(sf.getRefFeature(), sf.getRefFeature());
      else if (sf.hasAnnotatedFeature())
        uniqueAnnots.addFeature(sf.getAnnotatedFeature());
    }
    //Enumeration keys = uniqueFeatures.keys();
    //Vector out = new Vector();
    //while(keys.hasMoreElements()) out.addElement(keys.nextElement());
    //return out;
    return uniqueAnnots;
  }

  public void menuInit() {
    FeatureList selected = getSelectedGenes();
    boolean allowRemove = false;
    boolean allowAdd = false;
    for(int i=0; i < selected.size(); i++) {
      AnnotatedFeatureI g = (AnnotatedFeatureI) selected.getFeature(i);
      if (bookmarks.contains(g)) {
        allowRemove = true;
        if (allowAdd)
          break;
      } else {
        allowAdd = true;
        if (allowRemove)
          break;
      }
    }

    addBookmark.setEnabled(allowAdd);
    removeBookmark.setEnabled(allowRemove);

    removeAll();
    add(addBookmark);
    add(removeBookmark);
    addSeparator();
    /*
    Vector features = SeqFeatureUtil.getFeaturesOfClass(
                            frame.getCurationSet().getAnnots(),
                            Gene.class,
                            false);
    */
    if (bookmarks.size() > 0) {
      for(int i=0; i < bookmarks.size(); i++) {
        SeqFeatureI feature = (SeqFeatureI) bookmarks.elementAt(i);

        String name = feature.getName();

        // strand
        name += " "+(feature.getStrand() != -1 ? "(+)" : "(-)");

      JMenuItem menuItem = new JMenuItem(name);
        menuItem.addActionListener(new FocusMenuListener(feature));
        add(menuItem);
      }
    } else {
      JMenuItem menuItem = new JMenuItem("<no bookmarks>");
      menuItem.setEnabled(false);
      add(menuItem);
    }
  }
}
