package apollo.gui.menus;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.event.*;
import java.util.Hashtable;

import apollo.util.HTMLUtil;

import apollo.gui.synteny.CurationManager;

/** A menu of web analysis links--should be specified in style. */

public class LinksMenu extends JMenu implements ActionListener {
  private Hashtable namesToURLs = new Hashtable();
  private Hashtable menuItemsToNames = new Hashtable();

  private JCheckBoxMenuItem igbLink;

  private static LinksMenu singletonInstance;

  /** singleton access */
  public static LinksMenu getLinksMenu() {
    if (singletonInstance == null)
      singletonInstance = new LinksMenu();
    return singletonInstance;
  }

  private LinksMenu() {
    super("Links");
    menuInit();
    // for enabling linking to igb - for here for now
    addMenuListener(new IgbMenuListener());
  }

  private void menuInit() {
    // Links are defined here just for testing.
    // The list of links should be in the style file.
    makeLink("Primer3 (pick primers)", "http://frodo.wi.mit.edu/primer3/input.htm");
    makeLink("NCBI BLAST", "http://www.ncbi.nlm.nih.gov/blast/Blast.cgi?CMD=Web&LAYOUT=TwoWindows&AUTO_FORMAT=Semiauto&ALIGNMENTS=50&ALIGNMENT_VIEW=Pairwise&CLIENT=web&DATABASE=chromosome&DESCRIPTIONS=100&ENTREZ_QUERY=%28none%29&EXPECT=10&FILTER=L&FORMAT_OBJECT=Alignment&FORMAT_TYPE=Text&NCBI_GI=on&PAGE=Nucleotides&PROGRAM=blastn&SERVICE=plain&SET_DEFAULTS.x=34&SET_DEFAULTS.y=8&SHOW_OVERVIEW=on&END_OF_HTTPGET=Yes&SHOW_LINKOUT=yes&GET_SEQUENCE=yes");
    makeLink("Fly BLAST", "http://flybase.net/blast/");
    makeLink("Genscan (gene predictor)", "http://genes.mit.edu/GENSCAN.html");
    makeLink("FgenesH (gene predictor)", "http://www.softberry.com/berry.phtml?topic=fgenesh&group=programs&subgroup=gfind");
    makeLink("tRNAscan-SE (tRNA gene finder)", "http://lowelab.ucsc.edu/tRNAscan-SE/");
    makeLink("CMS Molecular Biology Resource", "http://mbcf.dfci.harvard.edu/cmsmbr/");
    makeLink("BCM Search Launcher", "http://searchlauncher.bcm.tmc.edu/");

    // TODO - introduce a parameter that controls this feature, instead of using global DEBUG (deprecated)
    //        (it should be the same parameter that gets used for apollo.external.IgbBridge)
    if (apollo.config.Config.DEBUG) {
      String u = "http://localhost:7085/UnibrowControl?seqid=chr4&start=136015"
        +"&end=152756&version=D_melanogaster_Apr_2004 ";
      makeLink("IGB test",u);
    }
  }

  private void makeLink(String name, String url) {
    namesToURLs.put(name, url);
    JMenuItem item = new JMenuItem(name);
    item.addActionListener(this);
    add(item);
    menuItemsToNames.put(item, name);
  }

  public void actionPerformed(ActionEvent e) {
    JMenuItem item = (JMenuItem)e.getSource();
    String name = (String)menuItemsToNames.get(item);
    if (name == null) {
      return;
    }
    String url = (String)namesToURLs.get(name);
    if (url == null) {
      return;
    }
    HTMLUtil.loadIntoBrowser(url);
  }


  public static boolean igbLinksEnabled() {
    return getLinksMenu().getIgbLinkState();
  }

  private boolean getIgbLinkState() {
    if (!igbConfigged())
      return false;
    if (igbLink == null)
      return false;
    return igbLink.getState();
  }

  private boolean igbConfigged() {
    return CurationManager.getActiveStyle().igbHttpConnectionEnabled();
  }

  private class IgbMenuListener implements MenuListener {

    public void menuSelected(MenuEvent e) {
      
      if (igbLink != null)
        remove(igbLink);

      // if not configged for igb links return
      if (!igbConfigged())
        return;

      if (igbLink == null)
        igbLink = new JCheckBoxMenuItem("Enable Igb Selection");
      add(igbLink); // just add to bottom
    }

    public void menuDeselected(MenuEvent e) { }
    public void menuCanceled(MenuEvent e) { }
  }

}
