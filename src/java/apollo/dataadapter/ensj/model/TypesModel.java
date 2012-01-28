package apollo.dataadapter.ensj.model;
import java.util.*;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.view.*;
import apollo.dataadapter.ensj.controller.*;

public class TypesModel {

  private String _includeGenes;
  private String _includeDnaProteinAlignments;
  private String _includeDnaDnaAlignments;
  private String _includeSimpleFeatures;
  private String _includeProteinAnnotations;
  private String _includeRepeats;
  private String _includeContigs;
  private String _includeAbInitioPredictions;
  private String _includeVariations;
  private String _includeDitagFeatures;

  private boolean _geneTypeCountInitialised;
  private boolean _dnaProteinAlignmentTypeCountInitialised;
  private boolean _dnaDnaAlignmentTypeCountInitialised;
  private boolean _simpleFeatureTypeCountInitialised;
  private boolean _abInitioTypeCountInitialised;
  private boolean _simplePeptideTypeCountInitialised;
  private boolean _ditagFeatureTypeCountInitialised;
  
  public static final String NONE = "NONE";
  public static final String GENE = "GENE";
  public static final String DNAPROTEIN = "DNAPROTEIN";
  public static final String DNADNA = "DNADNA";
  public static final String SIMPLEFEATURE = "SIMPLEFEATURE";
  public static final String ABINITIO = "ABINITIO";
  public static final String SIMPLEPEPTIDE = "SIMPLEPEPTIDE";
  public static final String DITAGFEATURE = "DITAGFEATURE";
  private String _typePanelToShow = NONE; 

  private HashMap _geneTypeCounts;
  private HashMap _dnaProteinAlignmentTypeCounts;
  private HashMap _dnaDnaAlignmentTypeCounts;
  private HashMap _simpleFeatureTypeCounts;
  private HashMap _abInitioTypeCounts;
  private HashMap _simplePeptideTypeCounts;
  private HashMap _ditagFeatureTypeCounts;

  private String _geneCount;
  private String _dnaProteinAlignmentCount;
  private String _dnaDnaAlignmentCount;
  private String _simpleFeatureCount;
  private String _proteinAnnotationCount;
  private String _repeatCount;
  private String _contigCount;
  private String _abInitioPredictionCount;
  private String _variationCount;
  private String _ditagFeatureCount;
  
  private List _geneTypes = new ArrayList();
  private List _dnaProteinAlignTypes = new ArrayList();
  private List _dnaDnaAlignTypes  = new ArrayList();
  private List _simpleFeatureTypes = new ArrayList();
  private List _predictionTypes = new ArrayList();
  private List _ditagFeatureTypes = new ArrayList();

  private List _selectedGeneTypes  = new ArrayList();
  private List _selectedDnaProteinAlignTypes  = new ArrayList();
  private List _selectedDnaDnaAlignTypes  = new ArrayList();
  private List _selectedSimpleFeatureTypes  = new ArrayList();
  private List _selectedPredictionTypes = new ArrayList();
  private List _selectedDitagFeatureTypes  = new ArrayList();
  

  public String includeGenes(){
    return _includeGenes;
  }
  
  public void setIncludeGenes(String newValue){
     _includeGenes = newValue;
  }
  
  public String getGeneCount(){
    return _geneCount;
  }
  
  public void setGeneCount(String value){
    _geneCount = value;
  }

  public String includeDnaProteinAlignments(){
    return _includeDnaProteinAlignments;
  }
  
  public void setIncludeDnaProteinAlignments(String newValue){
     _includeDnaProteinAlignments = newValue;
  }
  
  public void setDnaProteinAlignmentCount(String newValue){
     _dnaProteinAlignmentCount = newValue;
  }
  
  public String getDnaProteinAlignmentCount(){
    return _dnaProteinAlignmentCount;
  }
  
  public String includeDnaDnaAlignments(){
    return _includeDnaDnaAlignments;
  }

  public void setIncludeDnaDnaAlignments(String newValue){
    _includeDnaDnaAlignments  = newValue;
  }
  
  public void setDnaDnaAlignmentCount(String newValue){
     _dnaDnaAlignmentCount= newValue;
  }
  
  public String getDnaDnaAlignmentCount(){
    return _dnaDnaAlignmentCount;
  }
  
  public String includeSimpleFeatures(){
    return _includeSimpleFeatures;
  }
  
  public void setIncludeSimpleFeatures(String newValue){
    _includeSimpleFeatures  = newValue;
  }

  public String getSimpleFeatureCount(){
    return _simpleFeatureCount;
  }
  
  public void setSimpleFeatureCount(String newValue){
    _simpleFeatureCount  = newValue;
  }

  public String includeDitagFeatures(){
    return _includeDitagFeatures;
  }
  
  public void setIncludeDitagFeatures(String newValue){
    _includeDitagFeatures  = newValue;
  }

  public String getDitagFeatureCount(){
    return _ditagFeatureCount;
  }
  
  public void setDitagFeatureCount(String newValue){
    _ditagFeatureCount  = newValue;
  }

  public String includeProteinAnnotations(){
    return _includeProteinAnnotations;
  }
  
  public void setIncludeProteinAnnotations(String newValue){
    _includeProteinAnnotations  = newValue;
  }

  public String getProteinAnnotationCount(){
    return _proteinAnnotationCount;
  }
  
  public void setProteinAnnotationCount(String newValue){
    _proteinAnnotationCount  = newValue;
  }

  public String includeRepeats(){
    return _includeRepeats;
  }
  
  public void setIncludeRepeats(String newValue){
     _includeRepeats = newValue;
  }
  
  public String getRepeatCount(){
    return _repeatCount;
  }

  public void setRepeatCount(String newValue){
     _repeatCount = newValue;
  }
  
  public String includeContigs(){
    return _includeContigs;
  }

  public void setIncludeContigs(String newValue){
     _includeContigs = newValue;
  }
  
  public String getContigCount(){
    return _contigCount;
  }
  
  public void setContigCount(String newValue){
     _contigCount = newValue;
  }
  
  public String includeAbInitioPredictions(){
    return _includeAbInitioPredictions;
  }
  
  public void setIncludeAbInitioPredictions(String newValue){
    _includeAbInitioPredictions  = newValue;
  }

  public String getAbInitioPredictionCount(){
    return _abInitioPredictionCount;
  }
  
  public void setAbInitioPredictionCount(String newValue){
    _abInitioPredictionCount  = newValue;
  }

  public String includeVariations(){
    return _includeVariations;
  }
  
  public void setIncludeVariations(String newValue){
     _includeVariations = newValue;
  }
  
  public List getGeneTypes(){
    return _geneTypes;
  }
  
  public void setGeneTypes(List newValue){
    _geneTypes = newValue;
  }
  
  public List getDnaProteinAlignTypes(){
    return _dnaProteinAlignTypes;
  }
  
  public void setDnaProteinAlignTypes(List newValue){
    _dnaProteinAlignTypes = newValue;
  }
  
  public List getDnaDnaAlignTypes(){
    return _dnaDnaAlignTypes;
  }
  
  public void setDnaDnaAlignTypes(List newValue){
    _dnaDnaAlignTypes = newValue;
  }
  
  public List getSimpleFeatureTypes(){
    return _simpleFeatureTypes;
  }
  
  public void setSimpleFeatureTypes(List newValue){
    _simpleFeatureTypes = newValue;
  }
  
  public List getDitagFeatureTypes(){
    return _ditagFeatureTypes;
  }
  
  public void setDitagFeatureTypes(List newValue){
    _ditagFeatureTypes = newValue;
  }
  
  public List getPredictionTypes(){
    return _predictionTypes;
  }
  
  public void setPredictionTypes(List newValue){
    _predictionTypes = newValue;
  }

  public List getSelectedGeneTypes(){
    return _selectedGeneTypes;
  }
  
  public void setSelectedGeneTypes(List newValue){
    _selectedGeneTypes = newValue;
  }
  
  public List getSelectedDnaProteinAlignTypes(){
    return _selectedDnaProteinAlignTypes;
  }
  
  public void setSelectedDnaProteinAlignTypes(List newValue){
    _selectedDnaProteinAlignTypes = newValue;
  }
  
  public List getSelectedDnaDnaAlignTypes(){
    return _selectedDnaDnaAlignTypes;
  }
  
  public void setSelectedDnaDnaAlignTypes(List newValue){
    _selectedDnaDnaAlignTypes = newValue;
  }
  
  public List getSelectedSimpleFeatureTypes(){
    return _selectedSimpleFeatureTypes;
  }
  
  public void setSelectedSimpleFeatureTypes(List newValue){
    _selectedSimpleFeatureTypes = newValue;
  }
  
  public List getSelectedDitagFeatureTypes(){
    return _selectedDitagFeatureTypes;
  }
  
  public void setSelectedDitagFeatureTypes(List newValue){
    _selectedDitagFeatureTypes = newValue;
  }
  
  public List getSelectedPredictionTypes(){
    return _selectedPredictionTypes;
  }
  
  public void setSelectedPredictionTypes(List newValue){
    _selectedPredictionTypes = newValue;
  }

  public boolean isGeneTypeCountInitialised(){
    return _geneTypeCountInitialised;
  }
  
  public void setGeneTypeCountInitialised(boolean value){
    _geneTypeCountInitialised = value;
  }
  
  public boolean isDnaProteinAlignmentTypeCountInitialised(){
    return _dnaProteinAlignmentTypeCountInitialised;
  }
  
  public void setDnaProteinAlignmentTypeCountInitialised(boolean value){
    _dnaProteinAlignmentTypeCountInitialised = value;
  }
  
  public boolean isDnaDnaAlignmentTypeCountInitialised(){
    return _dnaDnaAlignmentTypeCountInitialised;
  }
  
  public void setDnaDnaAlignmentTypeCountInitialised(boolean value){
    _dnaDnaAlignmentTypeCountInitialised = value;
  }
  
  public boolean isSimpleFeatureTypeCountInitialised(){
    return _simpleFeatureTypeCountInitialised;
  }
  
  public void setSimpleFeatureTypeCountInitialised(boolean value){
    _simpleFeatureTypeCountInitialised = value;
  }
  
  public boolean isDitagFeatureTypeCountInitialised(){
    return _ditagFeatureTypeCountInitialised;
  }
  
  public void setDitagFeatureTypeCountInitialised(boolean value){
    _ditagFeatureTypeCountInitialised = value;
  }

  public boolean isAbInitioTypeCountInitialised(){
    return _abInitioTypeCountInitialised;
  }
  
  public void setAbInitioTypeCountInitialised(boolean value){
    _abInitioTypeCountInitialised = value;
  }
  
  public boolean isSimplePeptideTypeCountInitialised(){
    return _simplePeptideTypeCountInitialised;
  }
  
  public void setSimplePeptideTypeCountInitialised(boolean value){
    _simplePeptideTypeCountInitialised = value;
  }

  public HashMap getGeneTypeCounts(){
    return _geneTypeCounts;
  }
  
  public void setGeneTypeCounts(HashMap value){
    _geneTypeCounts = value;
  }
  
  public HashMap getDnaProteinAlignmentTypeCounts(){
    return _dnaProteinAlignmentTypeCounts;
  }
  
  public void setDnaProteinAlignmentTypeCounts(HashMap value){
    _dnaProteinAlignmentTypeCounts = value;
  }
  
  public HashMap getDnaDnaAlignmentTypeCounts(){
    return _dnaDnaAlignmentTypeCounts;
  }
  
  public void setDnaDnaAlignmentTypeCounts(HashMap value){
   _dnaDnaAlignmentTypeCounts  = value;
  }
  
  public HashMap getSimpleFeatureTypeCounts(){
    return _simpleFeatureTypeCounts;
  }
  
  public void setSimpleFeatureTypeCounts(HashMap value){
     _simpleFeatureTypeCounts = value;
  }
  
  public HashMap getDitagFeatureTypeCounts(){
    return _ditagFeatureTypeCounts;
  }
  
  public void setDitagFeatureTypeCounts(HashMap value){
     _ditagFeatureTypeCounts = value;
  }
  
  public HashMap getAbInitioTypeCounts(){
    return _abInitioTypeCounts;
  }
  
  public void setAbInitioTypeCounts(HashMap value){
   _abInitioTypeCounts  = value;
  }
  
  public HashMap getSimplePeptideTypeCounts(){
    return _simplePeptideTypeCounts;
  }
  
  public void setSimplePeptideTypeCounts(HashMap value){
    _simplePeptideTypeCounts = value;
  }
  
  public String getTypePanelToShow(){
    return _typePanelToShow; 
  }
  
  public void setTypePanelToShow(String value){
    _typePanelToShow = value;
  }
}
