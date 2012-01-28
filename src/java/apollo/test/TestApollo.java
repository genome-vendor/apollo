package apollo.test;

import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.bdgp.io.DataAdapterUI;

import apollo.config.Config;
import apollo.config.ApolloNameAdapterI;
import apollo.config.PropertyScheme;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.DataInput;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.gamexml.GAMEAdapter;
import apollo.dataadapter.gamexml.GAMEAdapterGUI;
import apollo.dataadapter.synteny.SyntenyAdapter;
import apollo.dataadapter.synteny.SyntenyAdapterGUI;
import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.ApolloDataI;
import apollo.datamodel.CompositeDataHolder;
import apollo.datamodel.CurationSet;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.StrandedFeatureSetI;
import apollo.datamodel.Synonym;
import apollo.editor.AnnotationEditor;
import apollo.editor.TransactionManager;
import apollo.gui.ApolloFrame;
import apollo.gui.synteny.CompositeApolloFrame;
import apollo.gui.synteny.CurationManager;
import apollo.gui.synteny.GuiCurationState;
import apollo.gui.Controller;
import apollo.gui.Selection;
import apollo.gui.annotinfo.FeatureEditorDialog;
import apollo.gui.genomemap.AnnotationView;
import apollo.gui.genomemap.FeatureTierManager;
import apollo.gui.genomemap.StrandedZoomableApolloPanel;
import apollo.gui.TierManagerI;
import apollo.main.CommandLine;
import apollo.main.DataLoader;
import apollo.util.FeatureList;


public class TestApollo extends TestCase {

  /** Whether to test multi species/synteny or not - configurable? */
  //private final static boolean MULTI_SPECIES = false;
  private final static boolean MULTI_SPECIES = true;

  private CurationSet curationSet;
  private CompositeApolloFrame apolloFrame;
  private CompositeDataHolder compositeDataHolder;

  private DataLoader loader;

  private SyntenyAdapter syntenyAdapter;

  // Always required. Omitted from documentation.
  public TestApollo(String s) {
    super(s);
  }

  // Note, we should not do these initializations with the declarations.
  // This method can be called again for each test, making the tests "memoryless" so they
  // don't depend on each other.
  // setUp is autmoatically called by JUnit
  protected void setUp() {
    Config.initializeConfiguration();

    //Controller controller = Config.getController();
    // no longer necasary
    //Config.getAnnotationChangeLog().setController(controller);

    //apolloFrame = new ApolloFrame(controller);
    // this is wrong - just here to compile - fix
    //apolloFrame = new apollo.gui.synteny.CompositeApolloFrame(null,null);
    apolloFrame = ApolloFrame.getApolloFrame();
  }

  private void loadExampleData() {
    CompositeDataHolder cdh = getCompositeDataHolder();
    loadCompositeDataHolder(cdh);
  }

  private void loadCompositeDataHolder(CompositeDataHolder cdh) {
    loadCompositeDataHolder(cdh,getDataAdapter());
  }

  boolean firstLoad = true;

  private void loadCompositeDataHolder(CompositeDataHolder cdh,
                                       ApolloDataAdapterI adap) {
    curationSet = cdh.getCurationSet(0);
    // setCurationSet is private - either change to public or do through main or run
    // just commenting out for now as TestApollo is not being used yet and jbuilder
    // chokes on the compile
    //apolloFrame.setCurationSet (curationSet);
    apolloFrame.loadData(adap,cdh);
    if (firstLoad) {
      apolloFrame.completeGUIInitialization();
      firstLoad = false;
    }
    //getSequence();
  }

  private CompositeDataHolder getCompositeDataHolder() {
    if (compositeDataHolder == null) {
      if (MULTI_SPECIES)
        compositeDataHolder =  getMultiSpecies();
      else
        compositeDataHolder = getSingleSpecies();
    }
    return compositeDataHolder;
  }

  private ApolloDataAdapterI getDataAdapter() {
    if (MULTI_SPECIES)
      return syntenyAdapter;
    else
      return getDataLoader().getDataAdapter();
  }

//   private void getSequence() {
//     if (MULTI_SPECIES)
//       return;
//     try {
//       //getDataLoader().getSequence(); --> data loader
//     } catch (apollo.dataadapter.ApolloAdapterException e) {
//       e.printStackTrace();
//       throw new RuntimeException(e); // ???
//     }
//   }

  
  //private ApolloDataAdapterI gameAdapter;

  private ApolloDataAdapterI getGameWriteAdapter() {
    ApolloDataAdapterI gameAdap = getDataAdapter();
    if (MULTI_SPECIES)
      gameAdap = syntenyAdapter.getChildAdapter(0);
    //gameAdapUI.setIOOperation(ApolloDataAdapterI.OP_WRITE_DATA);
    String file = "test-save.game";
    gameAdap.setDataInput(new DataInput(DataInputType.FILE,file));
    return gameAdap;
  }

  private DataLoader getDataLoader() {
    if (loader == null)
      loader = new DataLoader();
    return loader;
  }

  private CompositeDataHolder getSingleSpecies() {
    // Read the features from somewhere
    //loader = new DataLoader();

    // example2 puts out this msg:  "annotation: Either intentionally
    //    ignoring or inadvertantly forgetting to parse property"
    // maybe should just use Example. arg? config param?
    String gameFile = exampleFileName();
    //String gameFile = System.getProperty ("APOLLO_ROOT")+"/data/annot-types.xml";
    String[] gameArgs = new String[] {"-x",gameFile}; // -x -> game adapter
     //curationSet = loader.getCurationSet(apolloFrame,gameArgs,null);
    try {
      CompositeDataHolder cdh = getDataLoader().getCompositeDataHolder(gameArgs,null);
      return cdh;
    } catch (apollo.dataadapter.ApolloAdapterException e) {
      throw new RuntimeException(e.toString()); // ???
    }
  }

  private CompositeDataHolder getMultiSpecies() {
    syntenyAdapter = new SyntenyAdapter();
    // synteny adapter calls doOperation on guis, SAG constructor hooks up
    // guis with data adapters
    //new SyntenyAdapterGUI(ApolloDataAdapterI.OP_READ_DATA,syntenyAdapter);
    DataInput di1 = new DataInput(DataInputType.FILE,exampleFileName());
    System.out.println("Input 1 "+di1);
    syntenyAdapter.getChildAdapter(0).setDataInput(di1);

    setUIDataAdapter(syntenyAdapter.getChildAdapter(0),di1);
    DataInput di2 = new DataInput(DataInputType.FILE,dpseExampleFileName());
    System.out.println("Input 2 "+di2);
    syntenyAdapter.getChildAdapter(1).setDataInput(di2);
    setUIDataAdapter(syntenyAdapter.getChildAdapter(1),di2);
    int numOfSpec = 2;
    try {
      CompositeDataHolder cdh = syntenyAdapter.loadSpeciesThatContainLinks(numOfSpec);
      return cdh;
    } catch (org.bdgp.io.DataAdapterException e) {
      e.printStackTrace();
      //} catch (apollo.dataadapter.ApolloAdapterException e) {
      throw new RuntimeException(e); // ???
    }
  }

  /** Normally done by SyntenyAdapterGUI */
  private void setUIDataAdapter(ApolloDataAdapterI adap, DataInput di) {
    //adap.setDataInput(di); // actually gets wiped out by game adapter gui.doOP
    // hacking this till theres a gui-free way of doing synteny
    GAMEAdapterGUI gameGui = (GAMEAdapterGUI)getReadUI(adap);
    // hacky - 1st/current panel happens to be the file panel
    //    gameGui.getCurrentGamePanel().setCurrentInput(di.getInputString());
    gameGui.setCurrentInput(di.getInputString());
    gameGui.setDataAdapter(adap);
  }
    
  private DataAdapterUI getReadUI(ApolloDataAdapterI adap) {
    return adap.getUI(ApolloDataAdapterI.OP_READ_DATA);
  }

  private static String exampleFileName() {
    return apolloRoot()+"/data/example.xml";
  }
  private static String dpseExampleFileName() {
    return apolloRoot()+"/data/dpse-example.xml";
  }
  private static String apolloRoot() {
    return System.getProperty("APOLLO_ROOT");
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    // Actually nothing is needed here for this example.
    // In general, you may need to release resources (files, say) here.
  }


  public static Test suite() {
    TestSuite suite = new TestSuite();
    // TestApollo constructor takes string that are method names to test
    //suite.addTest(new TestApollo("testEquals"));
    //suite.addTest(new TestApollo("testUndo"));
    //suite.addTest(new TestApollo("testSplitNaming"));
    suite.addTest(new TestApollo("test"));
    return suite;
  }
  // TestCases and TestSuites implement the Composite desgin pattern. A TestSuite contains
  // TestCases, but it is also, itself, a TestCase. Therefore suites can contain other suites.

  public static void main(String [] args) // The batch interface:
  {
    junit.textui.TestRunner.run(suite());
    System.exit(0);
  }

  /* Usage: Assuming that the junit directory is junit3.2 (at root level),
     the run command for the batch (textual) system here is
     java -cp .;/junit3.2/junit.jar orderprocessing.TestApollo
     or make the above class your "main" class in KAWA or other
     project mgr. Alternatively, to use the GUI interface to the
     test infrastructure use the batch command
     java -cp .;/junit3.2/junit.jar junit.ui.TestRunner
  */

  public void test() {
    // loads its own data
    //testTiersChange();
    // the rest of the tests use [synteny] example data
    loadExampleData();
    addDeleteCoalesceTest(false,true); // add save del
    addDeleteCoalesceTest(false,false); // add del
    //addDeleteCoalesceTest(false,true); // add save del
    addDeleteCoalesceTest(true,false); // add del add
    testUndo();
    testSplitNaming();
    testPeptideAlignment();
    if (MULTI_SPECIES)
      testSynteny();
  }


  public void testUndo() {
    System.out.println("----testUndo");
    FeatureEditorDialog.testNameUndo(this);
  }

  public void testSplitNaming() {
    System.out.println("--- Split Naming");
    SeqFeatureI annot = getFirstAnnot();

    // hopefully first annot has 1 trans with multiple exons...(easy split)
    if (annot.getNumberOfChildren() > 1) {
      System.out.println("simple split wont work, >1 transcript");
      return;
    }
    SeqFeatureI transcript = annot.getFeatureAt(0);
    if (transcript.getNumberOfChildren() <= 1) {
      System.out.println("cant split "+transcript.getName()+" only has 1 exon");
      return;
    }
    SeqFeatureI firstExon = transcript.getFeatureAt(0);
    SeqFeatureI secondExon = transcript.getFeatureAt(1);
    
    // record what split name should be
    ApolloNameAdapterI na = getNameAdapter(annot);
    // this wont effect future gen split name cos we aint assigning it
    String genSplitName = na.generateAnnotSplitName(annot,getAnnots(),curName());
    
    // do split
    Vector annots = new Vector(2);
    annots.add(firstExon);
    annots.add(secondExon);
    getEditor().setSelections(null,annots,null,null); // null?
    getEditor().splitTranscript();
    //System.out.println(genSplitName+annot+annot.getName()+annot.getId());
    //System.out.println(annot+" fa()  "+getFirstAnnot()+annot.getName()+getFirstAnnot().getName());
    // i think this is a java bug???? if i say just annot.getName() it gives the old
    // name - isnt that totally wierd - am i missing something?? im pretty sure
    // its the same object (split keeps the 1st annot the same) - even stranger if
    // i uncomment the print statement above annot gets the new name - wierd!
    //assertEquals(genSplitName,annot.getName());
    assertEquals(genSplitName,getFirstAnnot().getName());
    checkForRedundantSplitSynonym(getFirstAnnot());
    // splitting assigns temp id & name - should be same - is this presumptious?
    // this is true for gmod/rice/fly at least
    //assertEquals(genSplitName,getFirstAnnot().getId());
  }

  private void checkForRedundantSplitSynonym(SeqFeatureI sf) {
    assertTrue(sf.hasAnnotatedFeature());
    checkForRedundantSplitSynonym(sf.getAnnotatedFeature(),true);
  }

  private void checkForRedundantSplitSynonym(AnnotatedFeatureI annot, boolean checkKids) {
    for (int i = 0; i < annot.getSynonymSize() - 1; i++) {
      String syn1 = annot.getSynonym(i).getName();
      for (int j=i; j < annot.getSynonymSize(); j++) {
        String syn2 = annot.getSynonym(j).getName();
        assertTrue(!syn1.equals(syn2));
      }
    }
    // Transcripts...
    for (int i=0; i<annot.size(); i++) {
      AnnotatedFeatureI transcript = annot.getFeatureAt(i).getAnnotatedFeature();
      // false - dont check kids - exons dont have syns
      checkForRedundantSplitSynonym(transcript,false);
    }
  }
    

  private SeqFeatureI getFirstAnnot() {
    assertNotNull(curationSet);
    return curationSet.getAnnots().getFeatureAt(0);
  }
  private SeqFeatureI getFirstExon() {
    return getFirstAnnot().getFeatureAt(0).getFeatureAt(0);
  }

  private SeqFeatureI getLastAnnot() {
    int last = getCurationSet().getAnnots().size() - 1;
    return getCurationSet().getAnnots().getFeatureAt(last);
  }
  private SeqFeatureI getLastExon() {
    return getLastAnnot().getFeatureAt(0).getFeatureAt(0);
  }

  private CurationSet getCurationSet() { return curationSet; }

  private StrandedFeatureSetI getAnnots() { return curationSet.getAnnots(); }
  private String curName() { return curationSet.getName(); }

  private ApolloNameAdapterI getNameAdapter(SeqFeatureI annot) {
    return getCurationState().getNameAdapter(annot.getAnnotatedFeature());
  }
  private GuiCurationState getCurationState() {
    return CurationManager.getCurationManager().getActiveCurState();
  }

  private StrandedZoomableApolloPanel getSZAP() {
    return getCurationState().getSZAP();
  }

  private AnnotationView getForwardAnnotView() {
    return getSZAP().getAnnotView(1);
  }
  
  private TierManagerI getForwardAnnotTierManager() {
    return getForwardAnnotView().getTierManager();
  }

  private TransactionManager getTranManager() {
    return getCurationSet().getTransactionManager();
  }
  private int getTransNum() { return getTranManager().size(); }

  private AnnotationEditor getEditor() {
    boolean forwardStrand = true; // yikes - presumptioous
    return getCurationState().getAnnotationEditor(forwardStrand);
  }

  
  private void testPeptideAlignment() {
    SeqFeatureI blastxResult = getBlastXResult();
    assertTrue(blastxResult.alignmentIsPeptide());
    System.out.println("peptide alignment test passed");
  }

  private SeqFeatureI getBlastXResult() {
    String blastxHitName = "Q9VNA4";
    FeatureList fl = curationSet.getResults().findFeaturesByName(blastxHitName);
    assertNotNull(fl.getFeature(0));
    return fl.getFeature(0);
  }

  public void testSynteny() {
    System.out.println("Testing synteny for links...");
    assertTrue(getCompositeDataHolder().hasNonEmptyLinkSet());
    System.out.println("Synteny has links");
  }

  
  private void addDeleteCoalesceTest(boolean doSecondAdd,boolean doInterimSave) {
    print("\n\nTESTING add del coalesce... secondAdd "+doSecondAdd+" interim save "
          +doInterimSave);
    getTranManager().clear();
    int low =  1178050;
    int high = 1178100;
    int strand = 1;
    //Selection emptySelect = new Selection();
    //getEditor().setSelections(null,emptySelect,null,basepair,strand);
    //getEditor().createAnnotation();
    getEditor().createAnnotation(low,high,strand,"gene");
    
    SeqFeatureI addedGene = getTranManager().getTransaction(0).getSeqFeature();

    print("added "+addedGene.getName()+" # trans "+getTransNum());

    if (doInterimSave) {
      print("\nDoing interim save to test-save.game");
      ApolloDataAdapterI gameAd = getGameWriteAdapter();
      try {
        gameAd.commitChanges(getCurationSet());
      } catch (ApolloAdapterException e) {
        print("got exception on save"+e);
      }
      print("saved");
    }

    Vector annots = new Vector();
    SeqFeatureI addedExon = addedGene.getFeatureAt(0).getFeatureAt(0);
    annots.add(addedExon);
    print("deleting exon "+addedExon+" "+addedExon.getName());
    getEditor().deleteSelectedFeatures(annots,null);
    print("deleted feat, # trans "+getTransNum());
    

    if (doSecondAdd) {
      print("doing second add...");
      getEditor().createAnnotation(low,high,strand,"gene");
      SeqFeatureI addedGene2 = getTranManager().getTransaction(0).getSeqFeature();
      print("added "+addedGene2.getName()+" # trans "+getTransNum());
    }
    
    getTranManager().coalesce();
    print("coalesced - # trans "+getTransNum());
    // add del should coalesce to 0 transactions
    boolean numTransOK = getTransNum() == 0;
    if (doSecondAdd)
      numTransOK = getTransNum() == 1;

    assertTrue("coalesce transactions",numTransOK);

    print("add del coalesced - pass ");

   
  }
  

  /** test that prop scheme changes with style/data adapter change 
   this test passes even when it shouldnt - darn. when guiCurationStates
   firing of prop scheme change event is disabled/commented out normally the tiers in 
   tier manager fails to get updated. this is not so in TestApollo. funny gui
   exceptions fly (not sure why) which seem to cause layouts & such which 
   causes the prop scheme to update properly - so i cant replicate the bug here
   bummer as this bug has cropped up twice */
  private void testTiersChange() {
    print("Testing prop scheme change with gff to game style change");
    // load gff
    CommandLine cmdLine = CommandLine.getCommandLine();
    String[] args = new String[] {"-i","gff","-f","chr2.200000-400000.gff"};
    try {
      cmdLine.setArgs(args);
      CompositeDataHolder c = getDataLoader().getCompositeDataHolder(cmdLine);
      loadCompositeDataHolder(c,getDataLoader().getDataAdapter());
      // get gffPropScheme
      //PropertyScheme gffPropScheme = Config.getPropertyScheme();
      // kindof a cheesy way to do this, but the tier manager string describes the
      // contents of the tiers (otherwise would need to make a public method to 
      // get FeatureTierManagers vector of tiers - this avoids that)
      Vector gffTierProps = getTierManagerProps();
      assertNotNull(gffTierProps);
      
      //load game
      args = new String[] {"-x","tiny.xml"};
      cmdLine.setArgs(args);
      CompositeDataHolder cdh = getDataLoader().getCompositeDataHolder(cmdLine);
      loadCompositeDataHolder(cdh,getDataLoader().getDataAdapter()); // -> gui

      //PropertyScheme gamePropScheme = Config.getPropertyScheme();
      Vector gameTierProps = getTierManagerProps();

      // test that game prop scheme != gff prop scheme
      String m ="game != gff prop scheme";
      assertTrue(m,!gameTierProps.equals(gffTierProps));
      print("\ngame & gff prop scheme test passed, !=");

    } catch (ApolloAdapterException e) {
      throw new RuntimeException(e);
    }
  }

  private Vector getTierManagerProps() {
    FeatureTierManager f = (FeatureTierManager)getForwardAnnotTierManager();
    return (Vector)f.getTierProperties().clone();
  }


  private void print(String m) { System.out.println(m); }




  // Delete?
  // need to have a doc/example.xml file for this to fly - not using for now
  public void testEquals() {
    // assert(!o123.equals(null)); Not needed. The system catches null errors.
    // assertEquals ("OrderItem equality test failed", o123, new OrderItem(10, 123, 2.34));
    // The string will be printed only if the test fails.
    // this fails - name is the arm 2R.3?? - fix this
    //assert("Name equality test failed "+curationSet.getName()+"!="+"AE003799.Feb",
    //	 curationSet.getName().equals("AE003799.Feb"));
    // this fails as we havent dealt with start and end yet
    //assert("region inequality test failed",
    //curationSet.getStart() == 13376150 &&
    // curationSet.getEnd() == 13693110);
    System.out.println("----Test start");
    String fileName = "doc/example.xml";
    CurationSet set = new CurationSet();
    GAMEAdapter gameAdapter = new GAMEAdapter();
    gameAdapter.setInput(fileName);
    gameAdapter.setInputType(DataInputType.FILE);
    try {
      set = gameAdapter.getCurationSet();
    }
    catch (ApolloAdapterException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    assertNotNull(set);
    assertNotNull(set.getTransactionManager().getTransactions());
    System.out.println("----Test end");
  }
}

