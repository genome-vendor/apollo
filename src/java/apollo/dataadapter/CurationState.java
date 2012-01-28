package apollo.dataadapter;

import java.util.HashMap;
import java.util.Map;

import apollo.datamodel.CurationSet;
import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.SequenceI;
import apollo.editor.AnnotationEditor;
import apollo.editor.TransactionManager;
import apollo.config.ApolloNameAdapterI; // move to editor
import apollo.config.FeatureProperty;
import apollo.config.PropertyScheme;

public class CurationState {

  /** curationNumber corresponds with # in speicesList and curation number 
      in compositeDataHolder */
  private int curationNumber; 
  private String curationName;
  private CurationSet curationSet;
  /** Theres an AnnotationEditor for each strand */
  private AnnotationEditor forwardAnnotEditor;
  private AnnotationEditor reverseAnnotEditor;
  private TransactionManager transactionManager;
  private ApolloDataAdapterI dataAdapter;
  /** maps data adapter strings to cur state specific ApolloNameAdapterIs */
  private Map stringToNameAdapters = new HashMap(3);
//private Controller controller;

  public CurationState(int i) {
    curationNumber = i;
  }

  public void setCurationName(String spStr) {
    curationName = spStr;
  }
  
  public String getCurationName() {
    return curationName;
  }

  /** CurationHolder goes through compDatHolder and sets CurationState cur set */
  public void setCurationSet(CurationSet cs) {
//    if (curationSet != null)// clean up old curationSet if there is one
//      curationSet.cleanup(); // no longer needed
    // set new curationSet
    curationSet = cs;
    if (cs.hasTransactionManager()) // hasAnnotChLog?
      //annotationChangeLog = cs.getAnnotationChangeLog();
      transactionManager = cs.getTransactionManager();
    else
      //cs.setAnnotationChangeLog(getAnnotationChangeLog());
      cs.setTransactionManager(getTransactionManager());
    // could send out DataLoadEvent here after curState gets its cur set?
    // and RCE just goes to its controller
    //getController().handleDataLoadEvent(new DataLoadEvent(this,cs));
    //getSZAP().setCurationSet(curationSet);
  }

  public CurationSet getCurationSet() { return curationSet; }

  public void setDataAdapter(ApolloDataAdapterI da) {
    if (da.isComposite()) { // shouldnt happen
      dataAdapter = da.getChildAdapter(curationNumber);
    } 
    else 
      dataAdapter = da;
    //da.setDataLoadListener(getController());
    //da.getStyle().getPropertyScheme().setPropSchemeChangeListener(getController());
  }
  
  public ApolloDataAdapterI getDataAdapter() {
    return dataAdapter;
  }

  public PropertyScheme getPropertyScheme() {
    if (getDataAdapter() == null) // hopefully doesnt happen
      return null;
    return getDataAdapter().getStyle().getPropertyScheme();
  }
  public void emptyNameAdapter() {
  	stringToNameAdapters.clear();  	
  }
  /** A style/property scheme can be used by multiple curations (which is often the
      case) BUT the name adapters proscribed by its feature properties are curation
      specific in that they require the curations transaction manager, as the
      TM is checked for past ids in making new temp ids. So CurationState holds
      the name adapters for the curation associated with its feat props. 
      af can be a Transcript or Exon as well as an top annot, as it queries for 
      biotype which gives the type of the top level annot for exon & trans. */
  public ApolloNameAdapterI getNameAdapter(AnnotatedFeatureI af) {
    // This occasionally happens with Harvard ChadoXML data (HP1470).
    // Remove this test if we figure out the problem.
    if (af == null)  
      return null;

    FeatureProperty fp = getPropertyScheme().getFeatureProperty(af.getTopLevelType());

    // check name adapter cache
    Object o = stringToNameAdapters.get(fp.getNameAdapterString());
    if (o != null)
      return (ApolloNameAdapterI)o;

    // not in cache -> create, add to cache
    ApolloNameAdapterI na = fp.createNameAdapter();
    // curation specific transaction manager
    na.setTransactionManager(getTransactionManager());
    na.setDataAdapter(dataAdapter);
    stringToNameAdapters.put(fp.getNameAdapterString(),na);
    return na;
  }
  
  /**
   * Get the TransactionManager used by the embed curationSet. This method call
   * is delegated to {@link apollo.datamodel.CurationSet#getTransactionManager() 
   * CurationSet.getTransactionManager}.
   * @return
   */
  public TransactionManager getTransactionManager() {
    if (transactionManager == null) {
      if (curationSet != null && curationSet.hasTransactionManager())
        transactionManager = curationSet.getTransactionManager();
      //TransactionManager manager = curationSet.getTransactionManager();
      //if (manager == null) {
      else {
        // A TransactionManager for database
        transactionManager = new TransactionManager();
        transactionManager.setCurationState(this);
        if (curationSet != null)
          curationSet.setTransactionManager(transactionManager);
      }
    }
    return transactionManager;
  }

  public void addAnnotationEditor(AnnotationEditor ae,boolean forwardStrand) {
    if (forwardStrand)
      forwardAnnotEditor = ae;
    else
      reverseAnnotEditor = ae;
  }

  public AnnotationEditor getAnnotationEditor(boolean forwardStrand) {
    return forwardStrand ? forwardAnnotEditor : reverseAnnotEditor;
  }

  /** Whether curationSet has ref sequence (formerly in ApolloFrame) */
  public boolean haveSequence() {
    SequenceI seq = curationSet.getRefSequence();
    if (seq == null)
      return false;
    if (seq.isLazy()) 
      return true;
    return seq.getResidues()!=null && !seq.getResidues().equals("");
  }

  /** Returns true if curSet have >0 annotations */
  public boolean haveAnnots() {
    if (curationSet == null || curationSet.getAnnots() == null)
      return false;
    return curationSet.getAnnots().size() > 0;
  }

}

// moved to gui curation state to link up with controller
//   /** Clear out TransactionManager transactions on data load (this shouldnt clear
//       on append loads - does DataLoadEvents happen on appends?) */
//   private class CurationDataLoadListener implements DataLoadListener {
//     public boolean handleDataLoadEvent(DataLoadEvent e) {
//       if (e.dataRetrievalBeginning())
//         getTransactionManager().clear();
//       return true;
//     }
//   }


//   public AnnotationChangeLog getAnnotationChangeLog() {
//     if (annotationChangeLog == null) {
//       // if curation set has an annotation change log grab that
//       if (curationSet != null && curationSet.getAnnotationChangeLog() != null)
//         annotationChangeLog = curationSet.getAnnotationChangeLog();
//       else 
//         annotationChangeLog = new AnnotationChangeLog();
//       // dont have cur set yet
//       //curationSet.setAnnotationChangeLog(annotationChangeLog);

//       //getController().addListener(annotationChangeLog); // annotChangeEvents
//       //getController().addListener(new CurationDataLoadListener()); //load clear
//     }
//     return annotationChangeLog;
//   }
