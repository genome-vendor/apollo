package apollo.gui.featuretree;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

//import apollo.gui.*;
import apollo.gui.FeatureNavigationI;
import apollo.gui.drawable.DrawableSeqFeature;
import apollo.gui.menus.*;
import apollo.gui.event.*;
import apollo.gui.synteny.GuiCurationState;
import apollo.datamodel.*;
import apollo.util.*;

// for test main
import apollo.seq.io.*;

/**
 * A scrollpane containing a SWING tree of the annotations (not related to the
 * FeatureTree or FeatureTreeNode). This should be in apollo.gui.featuretree package
 */
public class FeatureTreePanel extends JScrollPane
  implements MouseListener {
  protected JTree        tree = null;
  protected FeatureSetI  feature_set;
  protected FeatureNavigationI parent;
  protected boolean      remote_update = false;
  protected Hashtable    treeHash;
  protected boolean      endToggle     = true;
  private GuiCurationState curationState;

  public FeatureTreePanel(FeatureNavigationI parent,GuiCurationState curationState) {
    this.parent = parent;
    this.curationState = curationState;
  }

  public FeatureTreeFrame getFeatureTreeFrame() {
    if (parent instanceof FeatureTreeFrame)
      return (FeatureTreeFrame)parent;
    else
      return null;
  }

  public void setFeatureSet(FeatureSetI fs) {
    this.feature_set = fs;
    updateFeatureSet ();
  }

  protected void updateFeatureSet() {
    if (tree != null) {
      tree.clearSelection();
      tree.setModel(null);
      tree.setUI(null);
      getViewport().remove(tree);
    }

    tree = null;
    if (feature_set != null) {
      makeTree(feature_set, null);
    }

    // If there are no annotations at all init an empty tree
    treeHash = new Hashtable();
    if (tree == null) {
      initTree("No annotations");
    } else {
      DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
      Enumeration df
        = ((DefaultMutableTreeNode)model.getRoot()).depthFirstEnumeration();
      while (df.hasMoreElements()) {
        DefaultMutableTreeNode dmtn 
          = (DefaultMutableTreeNode)(df.nextElement());
        treeHash.put(dmtn.getUserObject(), dmtn);
      }
    }
    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
    TreePath         rootp = new TreePath(model.getRoot());
    tree.expandPath(rootp);
  }

  public void mouseClicked(MouseEvent evt) {
    //    System.out.println("FeatureTreePanel.mouseClicked: evt = " + evt); // DEL
    // Only enable right mouse menu if we're in a standalone tree panel,
    // not one that's embedded in an Annotation Info editor.
    // The way to discern this is to see whether our FeatureTreeFrame is set.
    if (MouseButtonEvent.isRightMouseClick(evt) &&
        getFeatureTreeFrame() != null) {
      JPopupMenu popup = new TreeMenu(this,evt.getPoint(),curationState);  
      popup.show((Component)evt.getSource(),evt.getX(),evt.getY());  
    } 
    else if (MouseButtonEvent.isLeftMouseClick(evt)) {
      TreePath selPath = tree.getPathForLocation(evt.getX(), evt.getY());
      if(selPath != null) {
        // already handled by valueChanged, which will have been triggered by
        // the user moving to this node before they clicked
        //        if(evt.getClickCount() == 1) {
        //          singleClick(selPath);
        //        } else 
          if(evt.getClickCount() == 2) {
          doubleClick(selPath);
        }
      }
    }
  }

  public void mousePressed(MouseEvent evt) {}

  public void mouseReleased(MouseEvent evt) {}

  public void mouseEntered(MouseEvent evt) {}

  public void mouseExited(MouseEvent evt) {}

  public void selectionInit() {
    TreeSelectionListener listen;
    listen = new TreeSelectionListener() {
        // This is triggered if user moves the mouse to a different node
        // (e.g. with arrow keys)
        public void valueChanged(TreeSelectionEvent e) {
          //          System.out.println("FeatureTreePanel.valueChanged: evt = " + e); // DEL
          singleClick(e.getPath());
          endToggle = true;
        }
      };
    tree.addTreeSelectionListener(listen);
  }

  private void singleClick(TreePath path) {
    if (!remote_update) {
      SeqFeatureI sf = getSelectedFeature();
      if (sf != null) {
        parent.featureSelected(sf);
      }
      endToggle = !endToggle;
    }
  }

  protected boolean endToggling() {
    return endToggle;
  }

  protected boolean remoteUpdate() {
    return remote_update;
  }

  private void doubleClick(TreePath path) {
    singleClick(path);
  }

  private void makeTree(SeqFeatureI sf,
                        DefaultMutableTreeNode parent_node) {
    if (sf.hasKids()) {
      if (sf.getNumberOfChildren() > 0) {
        // Sort them by order in the genome (if 0 strand by low, else 5'->3')
        Vector sorted_set = SeqFeatureUtil.getSortedKids(sf);

        DefaultMutableTreeNode newnode 
          = addFeatureNode (sf, parent_node, "Annotations");

        // getSortedFeatures seems to return them in REVERSE order,
        // so go through the vector in reverse.
        // getSortedKids now returns them in correct order so go
        // through vector forwards
        //for (int i = sorted_set.size()-1; i >= 0 ; i--) {
        for (int i = 0; i <sorted_set.size(); i++) {
          SeqFeatureI sf2 = (SeqFeatureI) sorted_set.elementAt(i);

          makeTree(sf2, newnode);
        }
      }
    } else {
      DefaultMutableTreeNode newnode
        = addFeatureNode (sf, parent_node,
                          "Annotations (a feature - shouldn't happen!)");
      if (parent_node != null) {
        if (sf instanceof AnnotatedFeatureI) {
          AnnotatedFeatureI gi = (AnnotatedFeatureI)sf;
          if (gi.getEvidenceFinder() != null) {
            // System.out.println(" has an EvidenceFinder");
            Vector evidence = gi.getEvidence();
            for (int i=0; i<evidence.size(); i++) {
              String evidenceId 
                = ((Evidence)evidence.elementAt(i)).getFeatureId();
              SeqFeatureI evidenceSF;
              evidenceSF = gi.getEvidenceFinder().findEvidence(evidenceId);
              if (evidenceSF != null) {
                String label = getEvidenceLabel (evidenceSF);
                DefaultMutableTreeNode evidence_node 
                  = new DefaultMutableTreeNode (label);
                newnode.add(evidence_node);
              }
            }
          }
        }
      }
    }
  }

  private DefaultMutableTreeNode addFeatureNode (SeqFeatureI sf,
                                                 DefaultMutableTreeNode parent_node,
                                                 String title) {
    DefaultMutableTreeNode newnode;

    if (parent_node == null) {
      newnode = initTree(title);
    } else {
      newnode = new DefaultMutableTreeNode (sf);
      parent_node.add(newnode);
    }
    return newnode;
  }

  private DefaultMutableTreeNode initTree(String topName) {
    DefaultMutableTreeNode newnode = new DefaultMutableTreeNode(topName);
    tree = new JTree(newnode);

    int placeholderHeight = (tree.getFontMetrics(tree.getFont())).getHeight();

    Icon blank = new BlankIcon(1, placeholderHeight);

    /* get rid of silly folder/file icons and draw connecting lines */
    DefaultTreeCellRenderer rend = new DefaultTreeCellRenderer();
    rend.setClosedIcon(blank);
    rend.setOpenIcon(blank);
    rend.setLeafIcon(blank);
    tree.setCellRenderer(rend);

    tree.putClientProperty("JTree.lineStyle", "Angled");

    // get the tree ui
    javax.swing.plaf.TreeUI ui = tree.getUI();

    // only set the icon if we are using a ui that supports it
    if (ui instanceof javax.swing.plaf.basic.BasicTreeUI) {
      javax.swing.plaf.basic.BasicTreeUI treeUI =
        (javax.swing.plaf.basic.BasicTreeUI) ui;
      Color iconColor = Color.gray;

      // set expanded and collapsed icons to whatever icon you like
      treeUI.setExpandedIcon(new TreeIcon(TreeIcon.MINUS,
                                          iconColor));
      treeUI.setCollapsedIcon(new TreeIcon(TreeIcon.PLUS,
                                           iconColor));
    } else
      System.out.println ("Icons not set because BasicTreeUI unsupported");
    getViewport().add(tree);
    tree.addMouseListener(this);
    selectionInit();

    return newnode;
  }

  public DefaultMutableTreeNode getSelectedNode() {
    TreePath   selPath = tree.getSelectionPath();
    if (selPath != null) {
      return (DefaultMutableTreeNode)selPath.getLastPathComponent();
    } else {
      return null;
    }
  }

  public SeqFeatureI getSelectedFeature() {
    TreePath path = tree.getSelectionPath();
    return getSelectedFeature(path);
  }
    
  protected SeqFeatureI getSelectedFeature(TreePath path) {
    SeqFeatureI sf = null;
    if (path != null) {
      DefaultMutableTreeNode dmtn
        = (DefaultMutableTreeNode) path.getLastPathComponent();
      Object selected = dmtn.getUserObject();
      if (selected instanceof SeqFeatureI) {
        sf = (SeqFeatureI) selected;
      }
    }
    return sf;
  }

  private String getEvidenceLabel (SeqFeatureI sf) {
    String label = "";
    FeatureSetI fs = (FeatureSetI) sf.getRefFeature();
    if (fs != null && (fs instanceof FeaturePair)) {
      SequenceI seq = fs.getHitSequence();
      SeqFeatureI hit = ((FeaturePair)fs).getHitFeature();
      if (seq == null) {
        seq = hit.getRefSequence();
      }
      if (seq != null) {
        label = sf.getFeatureType() +
          (seq.getName() != null ? ": " + seq.getName() : "");
      } else {
        label = sf.getFeatureType() + 
          (hit.getName() != null ? ": " + hit.getName() : "");
      }
    } else { // Not a FeaturePair
      label = sf.getFeatureType() + (sf.getName() != null ? ":  " +sf.getName() : "");
    }
    return label;
  }

  protected DefaultMutableTreeNode findTreeNode (Object obj) {
    return ((treeHash == null || obj == null) ?
            null : (DefaultMutableTreeNode)treeHash.get(obj));
  }

  /** If object found, collapses tree, selects path to object and scrolls to it */
  public DefaultMutableTreeNode findObject (Object obj) {
    remote_update = true;
    DefaultMutableTreeNode dmtn = null;
    if (obj != null) {
      dmtn  = findTreeNode(obj);
      if (dmtn!=null) {
        DefaultTreeModel       model = (DefaultTreeModel)tree.getModel();
        collapseAll();  // Only collapse if we actually found something
        TreeNode [] objs = model.getPathToRoot(dmtn);
        
        TreePath treep = new TreePath(objs);
        
        tree.paintImmediately(0,0,tree.getSize().width,tree.getSize().height);
        
        TreeSelectionModel selModel = tree.getSelectionModel();
        selModel.addSelectionPath(treep);
        
        tree.scrollPathToVisible(treep);
      } 
    } else {
      tree.setSelectionPath(null);
    }
    remote_update = false;
    return dmtn;
  }

  public void collapseAll() {
    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
    TreePath         rootp = new TreePath(model.getRoot());
    Enumeration expanded;
    while ((expanded = tree.getExpandedDescendants(rootp))!=null) {
      for (; expanded.hasMoreElements() ;) {
        TreePath expp = (TreePath)expanded.nextElement();
        tree.collapsePath(expp);
      }
    }
  }

  class TreeIcon implements Icon {

    public  final static int PLUS = 1;
    public  final static int MINUS = 2;

    private final static int width = 8;
    private final static int height = 8;

    private int type;
    private Color foreground;
    private Color background;

    public TreeIcon(int type, Color foreground) {
      this.type = type;
      this.background = Color.white;
      this.foreground = foreground;
    }

    public int getIconHeight() {
      return width;
    }

    public int getIconWidth() {
      return height;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
      g.setColor(Color.white);
      g.fillRect(x,y,width,height);
      g.setColor(foreground);
      g.drawRect(x,y,width,height);
      g.drawLine(x+2, y+(height/2), x+6, y+(height/2));
      if (type == PLUS)
        g.drawLine(x+(width/2), y+2, x+(width/2), y+6);
    }
  }

  class BlankIcon implements Icon {
    private int width;
    private int height;

    public BlankIcon(int width, int height) {
      this.width = width;
      this.height = height;
    }

    public int getIconHeight() {
      return height;
    }

    public int getIconWidth() {
      return width;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {}
  }
}
