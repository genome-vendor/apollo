package apollo.dataadapter.sample;
import apollo.datamodel.*;
import apollo.dataadapter.*;
import org.bdgp.io.IOOperation;
import org.bdgp.io.DataAdapterUI;
import org.bdgp.util.*;
import java.util.*;

/**
 * I am a simple adapter gui that serves as a minimal sample that people can
 * use to understand how a data adapter works, and 
 * cut-and-paste from, if neccessary.
**/
public class SampleAdapter extends AbstractApolloAdapter {

  //People can use this constant to use for a key in the properties
  //that are passed in to set state (see setStateInformation).
  public static final String NUMBER_OF_FEATURES = "NUMBER_OF_FEATURES";

  //State as passed in by GUI.
  private Properties stateInformation;
  
  //My real internal state.
  private int numberOfFeatures = 0;
  
  public SampleAdapter(){
    // From bdgp DataAdapter. This is the name that will be shown in the chooser drop-down.
    // (Name can be changed by later calls to setName, which is in AbstractApolloAdapter.)
    setName("Sample");
  }

  /**
   * From bdgp DataAdapter. No idea where this is used.
  **/
  public String getType() {
    return "Sample Adapter";
  }

  /**
   * From bdgp DataAdapter. 
   * If you want the adapter to WRITE stuff, it needs to add OP_WRITE_DATA to the list.
  **/
  public IOOperation [] getSupportedOperations() {
    return new IOOperation[] {
       ApolloDataAdapterI.OP_READ_DATA
    };
  }

  /**
   * From bdgp DataAdapter. This method traditionally creates a new UI for the adapter,
   * based on the operation - read or write (each creates a different UI instance).
  **/
  public DataAdapterUI getUI(IOOperation op) {
    return new SampleAdapterGUI(op);
  }

  /**
   * This is a convenient way of passing in the adapter's state: each part of the
   * state is a key-value pair in the input Properties. Use the static constant(s)
   * on this dataadapter for the keys - it's just safer!
  **/
  public void setStateInformation(Properties properties) {
    stateInformation = properties;
    String numberString = properties.getProperty(NUMBER_OF_FEATURES);
    int number = 0;
    try{
      number = Integer.valueOf(numberString).intValue();
    }catch(NumberFormatException exception){
      throw new apollo.dataadapter.NonFatalDataAdapterException(
        "Number of properties - "+
        numberString+
        " - is not a valid integer"
      );
    }//end try
    
    setNumberOfFeatures(number);
  }//end setStateInformation

  /**
   * From org.bdgp.DataAdapter interface. This is called when the adapter is
   * created and added to the registry (see org.bdgp.io.DataAdapterChooser).
  **/
  public void init() {
  }

  /**
   * This is the main method: we create some fake features, add them to the
   * CurationSet's results, and create a fake sequence for them.
  **/
  public CurationSet getCurationSet() throws ApolloAdapterException{
    //
    //Superclass magic:
    clearOldData();

    CurationSet curationSet = new CurationSet();
    StringBuffer buffer = new StringBuffer();
    int spacing = 0;
    Sequence sequence = null;
    FeatureSet featureSet = null;
    SeqFeature feature = null;
    StrandedFeatureSet results = 
      new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
    StrandedFeatureSet annotations = 
      new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
    int currentPosition = 0;
    int strand = 1;
    int featureSize = 0;
    int start = 0;
    int end = 0;

    
    //Creates a silly sequence for the range of 
    //the features (which is 1Mb on Chr 1), and attach to the curation set.
    for(int i = 0; i<1000000; i++){
      buffer.append("ACTGACTGAA");
    }//end for
    sequence = new Sequence("sequence 1", buffer.toString());
    curationSet.setRefSequence(sequence);

    //Create as many feature pairs as the user asked for, distributed over
    //Chr 1, 1-1Mb.
    spacing = 2*1000000/getNumberOfFeatures();
    featureSize = spacing / 10;
    
    for(int i = 0; i < getNumberOfFeatures(); i+=2){
      featureSet = new FeatureSet();

      //Add two features to a feature set.
      start = currentPosition+featureSize;
      end = currentPosition+2*featureSize;
      feature = new SeqFeature(start, end, "sample_type", strand);
      feature.setName("Feature"+String.valueOf(start));
      featureSet.addFeature(feature);
      
      start = currentPosition+3*featureSize;
      end = currentPosition+4*featureSize;
      feature = new SeqFeature(start, end, "sample_type", strand);
      feature.setName("Feature"+String.valueOf(start));
      featureSet.addFeature(feature);
      
      featureSet.setStrand(strand);
      featureSet.setFeatureType("sample_group");

      //featureSet probably needs to have a ref sequence
      featureSet.setRefSequence(sequence);

      results.addFeature(featureSet);
      
      currentPosition += spacing;
      strand = -1 * strand;
    }//end for
    
    curationSet.setStart(1);
    curationSet.setEnd(1000000);
    curationSet.setChromosome("1");
    
    curationSet.setResults(results);
    curationSet.setAnnots(annotations);
    
    return curationSet;
  }//end getCurationSet

  /** 
   * This is queried by Apollo's FileMenu
  **/
  public DataInputType getInputType(){
    return DataInputType.FILE;
  }
  
  /**
   * When the data load is done, this adapter is queried for any state information:
   * this information is then written to a text file which stores a history of this
   * adapter's state (see ApolloRunner.writeAdapterHistory). HOWEVER the really useful
   * information (that passed to the GUI to allow it to set up history lists etc) is
   * gathered in the SampleAdapterGUI's  getProperties() method. So I'm not sure what
   * the information here actually achieves...
  **/
  public Properties getStateInformation() {
    return stateInformation;
  }

  /**
   * The number of features the user wanted to display
  **/
  private void setNumberOfFeatures(int number){
    numberOfFeatures = number;
  }//end setNumberOfFeatues
  
  /**
   * The number of features the user wanted to display
  **/
  private int getNumberOfFeatures(){
    return numberOfFeatures;
  }//end getNumberOfFeatures
}
