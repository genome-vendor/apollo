package apollo.gui.annotinfo;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.List;
import java.util.Hashtable;

import apollo.datamodel.*;
import apollo.editor.AddTransaction;
import apollo.editor.AnnotationChangeEvent;
import apollo.editor.AnnotationAddEvent;
import apollo.editor.AnnotationDeleteEvent;
import apollo.editor.AnnotationUpdateEvent;
import apollo.editor.ChangeList;
import apollo.editor.DeleteTransaction;
import apollo.editor.TransactionSubpart;
import apollo.editor.TransactionUtil;
import apollo.editor.UserName;
import apollo.editor.UpdateTransaction;
import apollo.config.ApolloNameAdapterI;
import apollo.config.Config;
import apollo.config.Style;
import apollo.gui.DetailInfo;
import apollo.gui.event.*;
import apollo.util.GuiUtil;

import org.apache.log4j.*;

/**
 * A JPanel to display Transcript & Gene info. Used by FeatureEditorDialog.
 * Fields disabled if read only.
 * No CommentEditPanel if read only (transcript comments show up in
 * FeatureEditPanel's panel)
 * superclass of GeneEditPanel and TranscriptEditPanel
 * has all the stuff that is in both - like synonyms
 */

public abstract class FeatureEditPanel extends JPanel {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(FeatureEditPanel.class);

  // if doSynList is true then syns have JList instead of comma delim text box
  // if things dont test ok right away ill make this a configurable...
  private final static boolean doSynList = true;

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private AnnotatedFeatureI originalFeature;

  protected FeatureEditorDialog featureEditorDialog;

  /** box for whole panel */
  Box featureBox = new Box(BoxLayout.Y_AXIS);
  JCheckBox        isProblematicCheckbox = new JCheckBox();
  /** fields panel holds all the "fields" (check boxes, names,... */
  JPanel fieldsPanel = new JPanel();
  Color bgColor = new Color(176, 236, 248);  // turquoise
  TitledBorder     border;
  
  ReadWriteField   featureNameField      = new ReadWriteField();
  ReadWriteField   featureIDField        = new ReadWriteField();

  // phase this out for synonym list below
  ReadWriteField   featureSynonymField   = new ReadWriteField();

  private JList synGuiList = new JList();
  private DefaultListModel guiListModel = new DefaultListModel();
  private JScrollPane synScrollPane = new JScrollPane(synGuiList);
  private JButton delSynButton = new JButton("delete");
  private JButton addSynButton = new JButton("add");

  // used in focus listener
  boolean lockFocus = false;
  protected boolean goodUser = true;
  protected boolean isReadOnly;

  protected int row;

  private SynonymEditor synonymEditor; // inner class for syn editing (focus listener)

  FeatureEditPanel(FeatureEditorDialog featureEditorDialog,
       boolean isReadOnly) {
      this.featureEditorDialog = featureEditorDialog;
      this.isReadOnly = isReadOnly;
  }
    
  protected FeatureEditorDialog getFeatureEditorDialog() {
    return featureEditorDialog;
  }

  /** features is set to in, which is used for the JList.
      feature is set selected. 
      Also checks for & commits edits in focus driven editors (syn,name,id) */
  protected void loadAnnotation(AnnotatedFeatureI feature) {
    // Sort gene names (but not transcripts) in alphabetical order (?)
    // check if there is any hanging edits in old feature (from focus driven edits)
    // if there is an old feature
    if (originalFeature != null && !getFeatureEditorDialog().isUndoing())
      checkFocusDrivenEdits(); // syns, name, id
    originalFeature = feature;
    loadSelectedFeature();
  }

  protected void isGoodUser() {
     if(Config.getCheckOwnership() 
    	&& originalFeature != null 
	&& originalFeature.getOwner() != null
	&& !originalFeature.getOwner().equals(UserName.getUserName())) {
      goodUser = false;
    } else
      goodUser = true;
  }
  
  /** Focus driven edits occur in textboxes -> synonyms, names, and ids. If the user
      selects a new annot in annot tree the focus event comes too late AFTER the
      loading of new annot, so this checks for edits before loadAnnotation sets the
      new annotation in place. this just checks syns, gene & trans panels override 
      to check name & ids. */
  protected void checkFocusDrivenEdits() {
    synonymEditor.processSynonymChanges();
  }

  protected AnnotatedFeatureI getEditedFeature() {
    // im experimenting with replacing clone with the original feat
    //return cloned_feature;
    return originalFeature;
  }

  protected ApolloNameAdapterI getNameAdapter() {
    return getFeatureEditorDialog().getNameAdapter(getEditedFeature());
  }

  protected void jbInit() {
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    this.setBackground(bgColor);
    this.setForeground(Color.black);
    //setMinimumSize(new Dimension(350, 100));  // was 350,250

    // The only field now shared by Gene and Transcript panels is name (symbol)
    fieldsPanel.setBackground (bgColor);
    fieldsPanel.setLayout(new GridBagLayout());
    row = 0;
    addField ("Symbol", featureNameField.getComponent());

    featureBox.setBackground(bgColor);
    featureBox.setForeground(Color.black);

    add(Box.createHorizontalStrut(8));
    add(featureBox);
    add(Box.createHorizontalStrut(8));
    add(Box.createHorizontalGlue());

    addListeners();
  }
  
  protected Box getFeatureBox() { return featureBox; }

  protected void addIsProblematicCheckbox() {
    if (!Config.getStyle().showIsProbCheckbox())
      return;
    addField ("Is problematic?", isProblematicCheckbox);
    isProblematicCheckbox.setBackground(bgColor);
    isProblematicCheckbox.setEnabled(!isReadOnly);
  }


  protected JPanel getFieldsPanel() { return fieldsPanel; }

  /** pad out bottom of fields panel, for layout - otherwise syns can get huge */
  protected void addFieldsPanelBottomGlue() {
    Component glue = Box.createVerticalGlue(); // ??
    //addLeftField(glue); no weight
    int fill = GridBagConstraints.VERTICAL;
    fieldsPanel.add(glue,GuiUtil.makeConstraint(0,row,1,1,1.0,0,fill));
  }

  protected void addField(String label_text, Component c) {
    addField(initLabel(label_text), c);
  }

  protected void addField(JComponent left, Component right) {
    addLeftField(left);
    fieldsPanel.add(right, GuiUtil.makeWeightConst(1, row++, 0, true));// true->fill
  }

  private void addLeftField(Component leftComp) {
    // 0 -> x, row -> y , 3 -> padding
    fieldsPanel.add(leftComp, GuiUtil.makeWeightConst(0, row, 3, false));
  }

  protected ReadWriteField getNameField() {
    return featureNameField;
  }

  protected String getGuiName() {
    return getNameField().getValue().trim();
  }
    
  protected void setGuiName(String name) {
    getNameField().setValue(name);
  }

  protected String getModelName() {
    return getEditedFeature().getName();
  }

  protected void setGuiNameFromModel() {
    setGuiName(getEditedFeature().getName());
  }

  protected boolean nameHasChanged() {
    String guiName = getGuiName();
    return guiName != null && !guiName.equals("") &&
      !guiName.equals(getEditedFeature().getName());
  }


  void clearFields() {
    featureNameField.setValue("");
    featureIDField.setValue("");
    featureSynonymField.setValue("");
    isProblematicCheckbox.setSelected(false);
    if (doSynList)
      guiListModel.clear();
  }


  /**
   * changes a comma delimited list into a vector of strings
   * leading and trailing whitespace between strings is ignored
   */
  private static List tokenizeListing(String in) {
    int i = 0;
    int endIndex = -1;
    int len = in.length();
    Vector out = new Vector();
    do {
      while(i < len && Character.isWhitespace(in.charAt(i)))
        i++;
      endIndex = in.indexOf(',', i);
      if (endIndex == -1) {
        // dont add empty string(i==len) as element
        if (i!=len)
          out.addElement(in.substring(i, len));
        break;
      } 
      else {
        out.addElement(in.substring(i, endIndex));
        i = endIndex+1;
      }
    } while (i < len);
    return out;
  }

  protected JLabel initLabel (String text) {
    JLabel label = new JLabel(text);
    label.setFont(new Font("Dialog", Font.PLAIN, 12));
    label.setBackground(bgColor);
    label.setForeground(Color.black);
    return label;
  }

  /**
   * cuts the first and last character off a string
   */
  protected static String trimFirstAndLast(String in) {
    in = in.substring(1, in.length() - 1);
    if (in.startsWith(", "))
      in.replace (',', ' ');
    return in.trim();
  }

  protected Color getBackgroundColor() {
    return bgColor;
  }

  protected abstract void loadSelectedFeature();

  
  // exprerimenting - for now just firing out - if that doesnt work will collect til
  // commit time
  protected void fireAnnotEvent(AnnotationChangeEvent ace) {
    getFeatureEditorDialog().fireAnnotEvent(ace);
  }

  /** If guiState is different than getEditedFeatures prop state as dictated by 
      trueString and falseString, set editedFeat with new value and fire event 
      special stuff for props that are method calls: IS_PROBLEMATIC */
  protected void setBooleanSubpart(boolean guiState, TransactionSubpart ts) {

    boolean modelState = TransactionUtil.getBoolean(getEditedFeature(),ts);
    
    // if gui and model state are the same return - nothing to do
    if (guiState == modelState) 
      return;

    // flips boolean from true to false or vice versa
    TransactionUtil.flipBoolean(getEditedFeature(),ts);

    AnnotationUpdateEvent aue = makeUpdateEvent(ts);
    fireAnnotEvent(aue);
  }

  protected AnnotationUpdateEvent makeUpdateEvent(TransactionSubpart ts) {
    return makeUpdateEvent(getEditedFeature(),ts);
  }

  protected AnnotationUpdateEvent makeUpdateEvent(AnnotatedFeatureI a,
                                                  TransactionSubpart ts) {
    return new AnnotationUpdateEvent(getFeatureEditorDialog(),a,ts);
  }

  protected void errorPopup(String err) {
    err += "\nYou must fix this problem before changes can be committed.";
    logger.error(err);
    JOptionPane.showMessageDialog(null, err, "Name or ID problem",
                                  JOptionPane.WARNING_MESSAGE);
  }

  protected ChangeList createChangeList() {
    return new ChangeList(getFeatureEditorDialog().getController());
  }

  protected Style getStyle() { return getFeatureEditorDialog().getStyle(); }

  protected void addSynonymGui() {
    // old comma delimited field - take out? pase?
    if (!doSynList) {
      addField("Synonyms", featureSynonymField.getComponent());
      featureSynonymField.setEditable(!isReadOnly);
    }
    // new list gui
    else {
      Box synonymBox = new Box(BoxLayout.X_AXIS); // need box for border
      Border line = BorderFactory.createLineBorder(Color.gray);
      synonymBox.setBorder(BorderFactory.createTitledBorder(line,"Synonyms"));
      // add add/del buttons if we are in editing mode
      if (!isReadOnly) {
        synonymBox.add(makeSynButBox());
      }
      synonymBox.add(Box.createHorizontalStrut(51));
      synonymBox.add(synScrollPane);
      synonymBox.setPreferredSize(new Dimension(80,50)); // crucial!!
      synonymBox.setMaximumSize(new Dimension(150,100));
      // x,y,width,height,weighty,pad,fill
      int fill = GridBagConstraints.BOTH;
      fieldsPanel.add(synonymBox,GuiUtil.makeConstraint(0,row,2,2,1.0,0,fill));
      row += 2; // because taking up 2 vertical cells
      synGuiList.setVisibleRowCount(2);
      synScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }
  }

  /** returns Box with add and delete buttons */
  private Box makeSynButBox() {
    setupSynButton(delSynButton);
    setupSynButton(addSynButton);
    Box box = new Box(BoxLayout.X_AXIS);
    box.add(Box.createHorizontalGlue());
    box.add(addSynButton);
    box.add(Box.createHorizontalGlue());
    box.add(delSynButton);
    box.add(Box.createHorizontalGlue());
    return box;
    //addLeftField(addSynButton);
    //fieldsPanel.add(delSynButton,GuiUtil.makeConstraintAt(1,row,0,false));
  }

  protected void setupSynButton(JButton synBut) {
    //delSynButton.setMaximumSize(new Dimension(4,2)); // seems to be ignored
    //delSynButton.setSize(new Dimension(4,2)); // seems to be ignored
    synBut.setPreferredSize(new Dimension(50,20)); 
    synBut.setFont(new Font("Dialog",0,10));
    synBut.setMargin(new Insets(0,0,0,0)); // top left bot right
  }

  protected void loadSynonymGui() {
    // !apollo.config.Config.internalMode() is so that if we're NOT in internal mode,
    // the list of synonyms will exclude internal synonyms.
    boolean notInternal = !apollo.config.Config.internalMode();
    Vector syns = getEditedFeature().getSynonyms(notInternal);
    featureSynonymField.setValue(trimFirstAndLast(syns.toString()));
    
    if (doSynList) {
      //synGuiList.setListData(syns); immutable
      guiListModel.clear();
      for (int i=0; i<syns.size(); i++) {
        guiListModel.addElement(syns.get(i));
      }
      synGuiList.setModel(guiListModel);
      addSynButton.setEnabled(goodUser);
      delSynButton.setEnabled(goodUser);
    }
    
  }

  private void addListeners() {
    isProblematicCheckbox.addActionListener(new IsProbCheckBoxListener());
    synonymEditor = new SynonymEditor();
    featureSynonymField.addFocusListener(synonymEditor);
    if (doSynList) {
      addSynButton.addActionListener(new AddSynListener());
      delSynButton.addActionListener(new DelSynListener());
    }
  }

  /** Inner class for is prob check box listening */
  private class IsProbCheckBoxListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      boolean b = isProblematicCheckbox.isSelected(); 
      setBooleanSubpart(b,TransactionSubpart.IS_PROBLEMATIC);
    }
  }

  private final TransactionSubpart SYN_SUBPART = TransactionSubpart.SYNONYM;

  /** When synonym loses focus, if edited, changes model and fires add, and/or
      delete synonym events */
  private class SynonymEditor implements FocusListener {

    private List oldSyns;
    private List newSyns;
    private boolean modelUpdated = false;
    private boolean synonymGuiEdited = false;

    public void focusGained(FocusEvent e) {
      synonymGuiEdited = true;
    }

    public void focusLost(FocusEvent e) {
      processSynonymChanges();
      synonymGuiEdited = false;
    }

    private List getNewSyns() {
      if (newSyns == null)
        newSyns = tokenizeListing(featureSynonymField.getValue());
      return newSyns;
    }
    private String getNewSyn(int i) {
      return (String)getNewSyns().get(i);
    }

    private List getOldSyns() {
      if (oldSyns == null && getEditedFeature() != null) {
        // if not in internal mode dont get internal syns
        boolean excludeInternal = !Config.getStyle().internalMode();
        // why clone?
        oldSyns = (List)getEditedFeature().getSynonyms(excludeInternal).clone();
      }
      return oldSyns;
    }
    private Synonym getOldSyn(int i) {
      return (Synonym)getOldSyns().get(i);
    }

    private void processSynonymChanges() {

      // loadAnnotation calls this (on user selecting new annot). if synonyms havent
      // actually been edited then dont go forth as model & gui can be out of synch
      // due to gene name, id, or type change.
      if (!synonymGuiEdited) // if in focus
        return;

      newSyns = null; // invalidate cache
      oldSyns = null; 
      Object src = getFeatureEditorDialog();
      AnnotatedFeatureI ann = getEditedFeature();
      if (ann == null) // happens at initialization time
        return;

      // look for deletes
      for (int i=0; i<getOldSyns().size(); i++) {
        if (!getNewSyns().contains(getOldSyn(i).getName())) {
//           AnnotationDeleteEvent a = new AnnotationDeleteEvent(src,ann,SYN_SUBPART);
//           a.setSubpartRank(i);
//           a.setOldString(getOldSyn(i).getName());
          DeleteTransaction dt =
            new DeleteTransaction(src,ann,SYN_SUBPART,getOldSyn(i),i);
          dt.setSubpartRank(i);
          updateModel(); // dt.editModel(); ???
          fireAnnotEvent(dt.generateAnnotationChangeEvent());
//           // fire delete event
//           fireAnnotEvent(a);
        }
      }

      // look for adds
      for (int i=0; i < getNewSyns().size(); i++) {
        if (findSynonym(getNewSyn(i), false) == null) {  // false means don't create new syn if we don't find it in list--that will happen in updateModel
          updateModel();
          Synonym addedSyn = ann.getSynonym(i);
          // fire add event
//           AnnotationAddEvent a = new AnnotationAddEvent(src,ann,SYN_SUBPART);
//           a.setSubpartRank(i);
          AddTransaction at = new AddTransaction(src,ann,SYN_SUBPART,addedSyn,i);
          //fireAnnotEvent(a);
          fireAnnotEvent(at.generateAnnotationChangeEvent());
        }
      } 
      
      modelUpdated = false; // reset flag
    }
    
    
    /** oldSyns and newSyns are different. set model syns to new syns.
        If already updated ignore. */
    private void updateModel() {
      if (modelUpdated)
        return;
      AnnotatedFeatureI feat = getEditedFeature();
      feat.clearSynonyms();
      // getNewSyns just returns a list of strings.  Need to find the old synonym records
      // that we recognize and substitute them for the flat strings.
      // This means that if a synonym name has changed, we'll lose the properties
      // that were associated with it--i.e., it's effectively a new synonym, which
      // seems reasonable.
      // NOTE: this is a slow n2 process, as we potentially have to compare each
      // new synonym with each old one, but the total number of synonyms is never large.
      for (int i=0; i<getNewSyns().size(); i++)
        feat.addSynonym(findSynonym(getNewSyn(i), true));  // true means create if necessary

      modelUpdated = true;
    }


    /** Either find Synonym by name from list of old synonyms (to preserve its properties), 
        or create a new Synonym if we can't find it on the list and createIfMissing==true. */
    private Synonym findSynonym(String name, boolean createIfMissing) {
      for (int i=0; i<getOldSyns().size(); i++) {
        Synonym syn = getOldSyn(i);
        if (syn.getName().equals(name))
          return syn;
      }
      // Didn't find it in list
      logger.debug("Couldn't find synonym " + name + " in list--creating new one");
      if (createIfMissing) {
        Synonym syn = new Synonym(name);
        syn.addProperty("synonym_sgml", name);
        syn.addProperty("author", UserName.getUserName());
        return syn;
      }
      else
        return null;
    }

  } // end of SynonymEditor inner class
  
  
  /** AddSynListener inner class listens to add button, brings up add syn dialog and
   * adds new synonym */
  private class AddSynListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      String newSynString = getUserInput();
      if (newSynString == null) // user hit "Cancel"
        return;
      Synonym newSyn = makeNewSyn(newSynString);
      addSynToModel(newSyn);
      addSynToGui(newSyn);
    }

    /** returns null if user hits cancel */
    private String getUserInput() {
      String featName = getEditedFeature().getName();
      return JOptionPane.showInputDialog("Enter new synonym for "+featName);
    }
    
    private Synonym makeNewSyn(String name) {
      Synonym syn = new Synonym(name);
      syn.setOwner(UserName.getUserName());
      return syn;
    }

    private void addSynToModel(Synonym syn) {
      AnnotatedFeatureI an = getEditedFeature();
      int i = an.getSynonymSize();
      AddTransaction at = new AddTransaction(evtSrc(),an,SYN_SUBPART,syn,i);
      at.editModel();
      fireAnnotEvent(at.generateAnnotationChangeEvent());
    }

    private void addSynToGui(Synonym syn) {
      guiListModel.addElement(syn);
    }
  }



  private class DelSynListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      if (!synSelected())
        return;
      deleteSelSynFromModel();
      removeSynFromGui();
    }

    private boolean synSelected() {
      return getSelectedSynonym() != null;
    }

    private Synonym getSelectedSynonym() {
      return (Synonym)synGuiList.getSelectedValue();
    }

    private void deleteSelSynFromModel() {
      Synonym syn = getSelectedSynonym();
      int i = synGuiList.getSelectedIndex();
      DeleteTransaction dt =
        new DeleteTransaction(evtSrc(),getEditedFeature(),SYN_SUBPART,syn,i);
      dt.editModel();
      fireAnnotEvent(dt.generateAnnotationChangeEvent());
    }
    private void removeSynFromGui() {
      // fiddle
      //getEditedFeature().clearSynonyms();
      guiListModel.remove(synGuiList.getSelectedIndex());
    }
  }

  private Object evtSrc() { return getFeatureEditorDialog(); }
}
