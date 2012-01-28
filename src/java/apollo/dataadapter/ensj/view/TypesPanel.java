package apollo.dataadapter.ensj.view;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import apollo.util.GuiUtil;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.controller.*;

public class TypesPanel extends JPanel{
  
  private View _view;
  public static final String GENE_LABEL = "Genes";
  public static final String DNA_PROTEIN_ALIGN_LABEL = "Dna Protein Alignments";
  public static final String DNA_DNA_ALIGN_LABEL = "Dna Dna Alignments";
  public static final String PROTEIN_ANNOTATIONS_LABEL = "Protein Annotations";
  public static final String SIMPLE_FEATURE_LABEL = "Simple Features";
  public static final String REPEAT_LABEL = "Repeats";
  public static final String AB_INITIO_LABEL = "Ab Initio Predictions";
  public static final String DITAG_FEATURE_LABEL = "Ditag Features";
  public static final String CONTIG_LABEL = "Contigs";
  
  private JCheckBox _genesCheckBox = new JCheckBox(GENE_LABEL);
  private JButton _geneTypesButton = new JButton("Gene Types...");
  
  private JCheckBox _dnaProteinAlignmentsCheckBox = new JCheckBox(DNA_PROTEIN_ALIGN_LABEL);
  private JButton _dnaProteinAlignmentTypesButton = new JButton("Protein Align Types...");
  
  private JCheckBox _dnaDnaAlignmentsCheckBox = new JCheckBox(DNA_DNA_ALIGN_LABEL);
  private JButton _dnaDnaAlignmentTypesButton = new JButton("Dna Align Types...");
  
  private JCheckBox _simpleFeaturesCheckBox = new JCheckBox(SIMPLE_FEATURE_LABEL);
  private JButton _simpleFeaturesTypesButton = new JButton("Simple Feature Types...");
  
  private JCheckBox _ditagFeaturesCheckBox = new JCheckBox(DITAG_FEATURE_LABEL);
  private JButton _ditagFeaturesTypesButton = new JButton("Ditag Feature Types...");

  private JCheckBox _proteinAnnotationsCheckBox = new JCheckBox(PROTEIN_ANNOTATIONS_LABEL);
  
  private JCheckBox _repeatsCheckBox = new JCheckBox(REPEAT_LABEL);
  
  private JCheckBox _contigsCheckBox = new JCheckBox(CONTIG_LABEL);

  private JCheckBox _abInitioPredictionsCheckBox = new JCheckBox(AB_INITIO_LABEL);
  private JButton _predictionTypesButton = new JButton("Ab Initio Types...");
  
  private JList _geneTypesList = new JList();
  private JList _dnaTypesList = new JList();
  private JList _proteinTypesList = new JList();
  private JList _simpleTypesList = new JList();
  private JList _abInitioTypesList = new JList();
  private JList _ditagTypesList = new JList();
  
  private JScrollPane _geneTypesPane = new JScrollPane(_geneTypesList);
  private JScrollPane _dnaTypesPane = new JScrollPane(_dnaTypesList);
  private JScrollPane _proteinTypesPane = new JScrollPane(_proteinTypesList);
  private JScrollPane _simpleTypesPane = new JScrollPane(_simpleTypesList);
  private JScrollPane _abInitioTypesPane = new JScrollPane(_abInitioTypesList);
  private JScrollPane _ditagTypesPane = new JScrollPane(_ditagTypesList);

  public TypesPanel(View view){
    _view = view;
    initialiseGUI();
  }

  private View getView(){
    return _view;
  }

  public void initialiseGUI(){
    setLayout(new java.awt.GridBagLayout());
    Dimension listSize = new Dimension(150,160);
    
    add(getGenesCheckBox(), GuiUtil.makeConstraintAt(0, 0, 10, true));
    add(getGeneTypesButton(), GuiUtil.makeConstraintAt(1, 0, 10, true));
    getView().getAdapterGUI().addActionRouter(getGeneTypesButton(), Controller.SHOW_GENE_COUNTS_BY_TYPE);
    
    add(getDnaProteinAlignmentsCheckBox(), GuiUtil.makeConstraintAt(0, 1, 10, true));
    add(getDnaProteinAlignmentTypesButton(), GuiUtil.makeConstraintAt(1, 1, 10, true));
    getView().getAdapterGUI().addActionRouter(getDnaProteinAlignmentTypesButton(), Controller.SHOW_DNA_PROTEIN_COUNTS_BY_TYPE);

    add(getDnaDnaAlignmentsCheckBox(), GuiUtil.makeConstraintAt(0, 2, 10, true));
    add(getDnaDnaAlignmentTypesButton(), GuiUtil.makeConstraintAt(1, 2, 10, true));
    getView().getAdapterGUI().addActionRouter(getDnaDnaAlignmentTypesButton(), Controller.SHOW_DNA_DNA_COUNTS_BY_TYPE);

    add(getSimpleFeaturesCheckBox(), GuiUtil.makeConstraintAt(0, 3, 10, true));
    add(getSimpleFeaturesTypesButton(), GuiUtil.makeConstraintAt(1, 3, 10, true));
    getView().getAdapterGUI().addActionRouter(getSimpleFeaturesTypesButton(), Controller.SHOW_SIMPLE_FEATURE_COUNTS_BY_TYPE);

    add(getDitagFeaturesCheckBox(), GuiUtil.makeConstraintAt(0, 4, 10, true));
    add(getDitagFeaturesTypesButton(), GuiUtil.makeConstraintAt(1, 4, 10, true));
    getView().getAdapterGUI().addActionRouter(getDitagFeaturesTypesButton(), Controller.SHOW_DITAG_FEATURE_COUNTS_BY_TYPE);

    add(getProteinAnnotationsCheckBox(), GuiUtil.makeConstraintAt(0, 5, 10, true));

    add(getRepeatsCheckBox(), GuiUtil.makeConstraintAt(0,6, 10, true));

    add(getContigsCheckBox(), GuiUtil.makeConstraintAt(0,7, 10, true));

    add(getAbInitioPredictionsCheckBox(), GuiUtil.makeConstraintAt(0, 8, 10, true));
    add(getPredictionTypesButton(), GuiUtil.makeConstraintAt(1, 8, 10, true));
    getView().getAdapterGUI().addActionRouter(getPredictionTypesButton(), Controller.SHOW_AB_INITIO_COUNTS_BY_TYPE);

    GridBagConstraints constraints = GuiUtil.makeConstraintAt(2,0,1,true);
    constraints.gridheight = 8;

    // SMJS Commented these lines out, because setting preferred size on the JLists was causing the 
    //      scrollbars on the JScrollPane to be incorrectly sized, making it impossible to see
    //      some of the list elements
    //getGeneTypesList().setPreferredSize(listSize);
    //getDnaTypesList().setPreferredSize(listSize);
    //getProteinTypesList().setPreferredSize(listSize);
    //getSimpleTypesList().setPreferredSize(listSize);
    //getAbInitioTypesList().setPreferredSize(listSize);
    
    getGeneTypesPane().setPreferredSize(listSize);
    getDnaTypesPane().setPreferredSize(listSize);
    getProteinTypesPane().setPreferredSize(listSize);
    getSimpleTypesPane().setPreferredSize(listSize);
    getAbInitioTypesPane().setPreferredSize(listSize);
    getDitagTypesPane().setPreferredSize(listSize);
    
    getGeneTypesPane().setMinimumSize(listSize);
    getDnaTypesPane().setMinimumSize(listSize);
    getProteinTypesPane().setMinimumSize(listSize);
    getSimpleTypesPane().setMinimumSize(listSize);
    getAbInitioTypesPane().setMinimumSize(listSize);
    getDitagTypesPane().setMinimumSize(listSize);
    
    add(getGeneTypesPane(), constraints);
    add(getDnaTypesPane(), constraints);
    add(getProteinTypesPane(), constraints);
    add(getSimpleTypesPane(), constraints);
    add(getDitagTypesPane(), constraints);
    add(getAbInitioTypesPane(), constraints);
    
    getGeneTypesPane().setVisible(false);
    getDnaTypesPane().setVisible(false);
    getProteinTypesPane().setVisible(false);
    getSimpleTypesPane().setVisible(false);
    getDitagTypesPane().setVisible(false);
    getAbInitioTypesPane().setVisible(false);
  }

  public void update(Model model){
    TypesModel myModel = model.getTypesModel();
    
    myModel.setIncludeGenes(getCheckBoxString(getGenesCheckBox()));
    
    myModel.setIncludeDnaProteinAlignments(getCheckBoxString(getDnaProteinAlignmentsCheckBox()));
    
    myModel.setIncludeDnaDnaAlignments(getCheckBoxString(getDnaDnaAlignmentsCheckBox()));
    
    myModel.setIncludeProteinAnnotations(getCheckBoxString(getProteinAnnotationsCheckBox()));
    
    myModel.setIncludeSimpleFeatures(getCheckBoxString(getSimpleFeaturesCheckBox()));
    
    myModel.setIncludeDitagFeatures(getCheckBoxString(getDitagFeaturesCheckBox()));

    myModel.setIncludeRepeats(getCheckBoxString(getRepeatsCheckBox()));
    
    myModel.setIncludeContigs(getCheckBoxString(getContigsCheckBox()));

    myModel.setIncludeAbInitioPredictions(getCheckBoxString(getAbInitioPredictionsCheckBox()));

    if(myModel.isGeneTypeCountInitialised()){
      myModel.setSelectedGeneTypes(Arrays.asList(getGeneTypesList().getSelectedValues()));
    }
    if(myModel.isDnaProteinAlignmentTypeCountInitialised()){
      myModel.setSelectedDnaProteinAlignTypes(Arrays.asList(getProteinTypesList().getSelectedValues()));
    }
    if(myModel.isDnaDnaAlignmentTypeCountInitialised()){
      myModel.setSelectedDnaDnaAlignTypes(Arrays.asList(getDnaTypesList().getSelectedValues()));
    }
    if(myModel.isSimpleFeatureTypeCountInitialised()){
      myModel.setSelectedSimpleFeatureTypes(Arrays.asList(getSimpleTypesList().getSelectedValues()));
    }
    if(myModel.isDitagFeatureTypeCountInitialised()){
      myModel.setSelectedDitagFeatureTypes(Arrays.asList(getDitagTypesList().getSelectedValues()));
    }
    if(myModel.isAbInitioTypeCountInitialised()){
      myModel.setSelectedPredictionTypes(Arrays.asList(getAbInitioTypesList().getSelectedValues()));
    }
  }

  public void read(Model model){
    TypesModel myModel = model.getTypesModel();
    setCheckBoxFromString(getGenesCheckBox(), myModel.includeGenes());
    getGenesCheckBox().setText(GENE_LABEL+" ("+myModel.getGeneCount()+")");

    setCheckBoxFromString(getDnaProteinAlignmentsCheckBox(), myModel.includeDnaProteinAlignments());
    getDnaProteinAlignmentsCheckBox().setText(DNA_PROTEIN_ALIGN_LABEL+" ("+myModel.getDnaProteinAlignmentCount()+")");
    

    setCheckBoxFromString(getDnaDnaAlignmentsCheckBox(), myModel.includeDnaDnaAlignments());
    getDnaDnaAlignmentsCheckBox().setText(DNA_DNA_ALIGN_LABEL+" ("+myModel.getDnaDnaAlignmentCount()+")");

    setCheckBoxFromString(getSimpleFeaturesCheckBox(), myModel.includeSimpleFeatures());
    getSimpleFeaturesCheckBox().setText(SIMPLE_FEATURE_LABEL+" ("+myModel.getSimpleFeatureCount()+")");

    setCheckBoxFromString(getDitagFeaturesCheckBox(), myModel.includeDitagFeatures());
    getDitagFeaturesCheckBox().setText(DITAG_FEATURE_LABEL+" ("+myModel.getDitagFeatureCount()+")");

    setCheckBoxFromString(getProteinAnnotationsCheckBox(), myModel.includeProteinAnnotations());
    getProteinAnnotationsCheckBox().setText(PROTEIN_ANNOTATIONS_LABEL+" ("+myModel.getProteinAnnotationCount()+")");

    setCheckBoxFromString(getRepeatsCheckBox(), myModel.includeRepeats());
    getRepeatsCheckBox().setText(REPEAT_LABEL+" ("+myModel.getRepeatCount()+")");

    setCheckBoxFromString(getContigsCheckBox(), myModel.includeContigs());
    getContigsCheckBox().setText(CONTIG_LABEL+" ("+myModel.getContigCount()+")");

    setCheckBoxFromString(getAbInitioPredictionsCheckBox(), myModel.includeAbInitioPredictions());
    getAbInitioPredictionsCheckBox().setText(AB_INITIO_LABEL+" ("+myModel.getAbInitioPredictionCount()+")");

    if(myModel.isGeneTypeCountInitialised()){
      getGeneTypesList().setListData(new Vector(myModel.getGeneTypes()));
      selectValuesInList(getGeneTypesList(), myModel.getGeneTypes(), myModel.getSelectedGeneTypes());
    }
    
    if(myModel.isDnaProteinAlignmentTypeCountInitialised()){
      getProteinTypesList().setListData(new Vector(myModel.getDnaProteinAlignTypes()));
      selectValuesInList(getProteinTypesList(), myModel.getDnaProteinAlignTypes(), myModel.getSelectedDnaProteinAlignTypes());
    }
    
    if(myModel.isDnaDnaAlignmentTypeCountInitialised()){
      getDnaTypesList().setListData(new Vector(myModel.getDnaDnaAlignTypes()));
      selectValuesInList(getDnaTypesList(), myModel.getDnaDnaAlignTypes(), myModel.getSelectedDnaDnaAlignTypes());
    }
    
    if(myModel.isSimpleFeatureTypeCountInitialised()){
      getSimpleTypesList().setListData(new Vector(myModel.getSimpleFeatureTypes()));
      selectValuesInList(getSimpleTypesList(), myModel.getSimpleFeatureTypes(), myModel.getSelectedSimpleFeatureTypes());
    }
     
    if(myModel.isDitagFeatureTypeCountInitialised()){
      getDitagTypesList().setListData(new Vector(myModel.getDitagFeatureTypes()));
      selectValuesInList(getDitagTypesList(), myModel.getDitagFeatureTypes(), myModel.getSelectedDitagFeatureTypes());
    }
     
    if(myModel.isAbInitioTypeCountInitialised()){
      getAbInitioTypesList().setListData(new Vector(myModel.getPredictionTypes()));
      selectValuesInList(getAbInitioTypesList(), myModel.getPredictionTypes(), myModel.getSelectedPredictionTypes());
    }
      
    getGeneTypesPane().setVisible(false);
    getProteinTypesPane().setVisible(false);
    getDnaTypesPane().setVisible(false);
    getSimpleTypesPane().setVisible(false);
    getDitagTypesPane().setVisible(false);
    getAbInitioTypesPane().setVisible(false);
    
    if(myModel.getTypePanelToShow().equals(myModel.GENE)){
      getGeneTypesPane().setVisible(true);
    }else if(myModel.getTypePanelToShow().equals(myModel.DNAPROTEIN)){
      getProteinTypesPane().setVisible(true);
    }else if(myModel.getTypePanelToShow().equals(myModel.DNADNA)){
      getDnaTypesPane().setVisible(true);
    }else if(myModel.getTypePanelToShow().equals(myModel.SIMPLEFEATURE)){
      getSimpleTypesPane().setVisible(true);
    }else if(myModel.getTypePanelToShow().equals(myModel.DITAGFEATURE)){
      getDitagTypesPane().setVisible(true);
    }else if(myModel.getTypePanelToShow().equals(myModel.ABINITIO)){
      getAbInitioTypesPane().setVisible(true);
    }
  }
  
  private void selectValuesInList(JList list, java.util.List allValues, java.util.List selectionList){
    Iterator values = selectionList.iterator();
    Object selectedValue;
    ArrayList selectedPositions = new ArrayList();
    int position;
    int counter = 0;
    boolean foundASelection = (selectionList.size() > 0);
    int[] selectionArray = null;
    
    //
    //This asinine copying back-and-forth to an ArrayList because I can't dynamically allocate an
    //simple array of int's up-front
    while(values.hasNext()){
      selectedValue = values.next();
      position = allValues.indexOf(selectedValue);
      selectedPositions.add(new Integer(position));
    }
    
    if(foundASelection){
      selectionArray = new int[selectedPositions.size()];
      for(int i=0; i<selectedPositions.size(); i++){
        selectionArray[i] = ((Integer)selectedPositions.get(i)).intValue();
      }
      
      list.setSelectedIndices(selectionArray);
    }else{
      list.clearSelection();
    }
  }

  private String getCheckBoxString(JCheckBox box){
    if(box.isSelected()){
      return Boolean.TRUE.toString();
    }else{
      return Boolean.FALSE.toString();
    }
  }

  private void setCheckBoxFromString(JCheckBox box, String value){
    if(Boolean.TRUE.toString().equals(value)){
      box.setSelected(true);
    }else{
      box.setSelected(false);
    }
  }

  private JCheckBox getGenesCheckBox() {
    return _genesCheckBox;
  }
  
  private JCheckBox getDnaProteinAlignmentsCheckBox() {
    return _dnaProteinAlignmentsCheckBox;
  }
  
  private JCheckBox getDnaDnaAlignmentsCheckBox() {
    return _dnaDnaAlignmentsCheckBox;
  }
  
  private JCheckBox getSimpleFeaturesCheckBox() {
    return _simpleFeaturesCheckBox;
  }
  
  private JCheckBox getDitagFeaturesCheckBox() {
    return _ditagFeaturesCheckBox;
  }

  private JCheckBox getRepeatsCheckBox() {
    return _repeatsCheckBox;
  }

  private JCheckBox getContigsCheckBox() {
    return _contigsCheckBox;
  }
  
  private JCheckBox getProteinAnnotationsCheckBox() {
    return _proteinAnnotationsCheckBox;
  }
  
  private JCheckBox getAbInitioPredictionsCheckBox() {
    return _abInitioPredictionsCheckBox;
  }

  private JButton getGeneTypesButton() {
    return _geneTypesButton;
  }
  
  private JButton getDnaProteinAlignmentTypesButton() {
    return _dnaProteinAlignmentTypesButton;
  }
  
  private JButton getDnaDnaAlignmentTypesButton() {
    return _dnaDnaAlignmentTypesButton;
  }
  
  private JButton getSimpleFeaturesTypesButton() {
    return _simpleFeaturesTypesButton;
  }

  private JButton getDitagFeaturesTypesButton() {
    return _ditagFeaturesTypesButton;
  }
  
  private JButton getPredictionTypesButton() {
    return _predictionTypesButton;
  }

  private JList getGeneTypesList(){
    return _geneTypesList;
  }

  private void setGeneTypesList(JList list){
    _geneTypesList = list;
  }
  
  private JScrollPane getGeneTypesPane(){
    return _geneTypesPane;
  }
  
  private JList getDnaTypesList(){
    return _dnaTypesList;
  }

  private void setDnaTypesList(JList list){
    _dnaTypesList = list;
  }
  
  private JScrollPane getDnaTypesPane(){
    return _dnaTypesPane;
  }
  
  private JList getProteinTypesList(){
    return _proteinTypesList;
  }

  private void setProteinTypesList(JList list){
    _proteinTypesList = list;
  }
  
  private JScrollPane getProteinTypesPane(){
    return _proteinTypesPane;
  }
  
  private JList getSimpleTypesList(){
    return _simpleTypesList;
  }

  private void setSimpleTypesList(JList list){
    _simpleTypesList = list;
  }
  
  private JScrollPane getSimpleTypesPane(){
    return _simpleTypesPane;
  }

  private JList getDitagTypesList(){
    return _ditagTypesList;
  }

  private void setDitagTypesList(JList list){
    _ditagTypesList = list;
  }
  
  private JScrollPane getDitagTypesPane(){
    return _ditagTypesPane;
  }
  
  private JList getAbInitioTypesList(){
    return _abInitioTypesList;
  }

  private void setAbInitioTypesList(JList list){
    _abInitioTypesList = list;
  }
  
  private JScrollPane getAbInitioTypesPane(){
    return _abInitioTypesPane;
  }
}
